package com.example.mobile_smart_ssh.network.ssh

import android.util.Log
import com.example.mobile_smart_ssh.core.model.ProcessInfo
import com.example.mobile_smart_ssh.core.model.RemoteFileItem
import com.example.mobile_smart_ssh.core.model.SystemResources
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Vector
import kotlin.coroutines.coroutineContext

/**
 * Kluczowy serwis komunikacyjny z serwerem Raspberry Pi przez SSH/SFTP (OpenSSH).
 * Odpowiada za testowanie połączenia, uwierzytelnianie kluczem Ed25519,
 * wykonywanie poleceń powłoki, zbieranie metryk systemowych, SFTP oraz interaktywny terminal.
 */
object SSHManager {

    private const val TAG = "SSHManager"
    private const val CONNECT_TIMEOUT_MS = 6000

    /**
     * Szybki test dostępności portu 22 (TCP ping bez autoryzacji),
     * używany do wyświetlania wskaźnika [ONLINE] / [OFFLINE] w nagłówku Dashboardu.
     */
    suspend fun isHostOnline(host: String, port: Int = 22, timeoutMs: Int = 2500): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tworzy i konfiguruje sesję JSch z uwierzytelnieniem kluczem prywatnym Ed25519.
     */
    private fun createSession(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        timeoutMs: Int = CONNECT_TIMEOUT_MS
    ): Session {
        val jsch = JSch()
        val privKeyBytes = privateKeyPem.trim().toByteArray(Charsets.UTF_8)
        jsch.addIdentity("rpi_ed25519", privKeyBytes, null, null)

        val session = jsch.getSession(username, host, port)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey")
        session.connect(timeoutMs)
        return session
    }

    /**
     * Weryfikuje autoryzację kluczem SSH na Raspberry Pi.
     * Zwraca informację o systemie (np. wynik `uname -srm`) przy sukcesie.
     */
    suspend fun testConnection(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var session: Session? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            val output = executeCommandOnSession(session, "uname -srm; uptime -p")
            Result.success(output.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Test połączenia SSH nie powiódł się: ${e.message}", e)
            Result.failure(Exception("Błąd połączenia SSH (${e.javaClass.simpleName}): ${e.message}"))
        } finally {
            session?.disconnect()
        }
    }

    /**
     * Jednorazowa instalacja klucza publicznego na serwerze Raspberry Pi przy użyciu hasła
     * (odpowiednik uniksowego polecenia `ssh-copy-id`).
     */
    suspend fun deployPublicKeyWithPassword(
        host: String,
        port: Int,
        username: String,
        password: String,
        publicKeyString: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: Session? = null
        try {
            val jsch = JSch()
            session = jsch.getSession(username, host, port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive")
            session.connect(CONNECT_TIMEOUT_MS)

            val safePubKey = publicKeyString.trim()
            val command = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && " +
                    "touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && " +
                    "grep -q -F '$safePubKey' ~/.ssh/authorized_keys 2>/dev/null || echo '$safePubKey' >> ~/.ssh/authorized_keys"

            executeCommandOnSession(session, command)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wdrożenia klucza przez hasło: ${e.message}", e)
            Result.failure(Exception("Nie udało się wysłać klucza na Raspberry Pi: ${e.message}"))
        } finally {
            session?.disconnect()
        }
    }

    /**
     * Wykonuje pojedyncze polecenie powłoki przez kanał Exec i zwraca stdout.
     */
    suspend fun executeCommand(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        command: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var session: Session? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            val output = executeCommandOnSession(session, command)
            Result.success(output)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wykonania polecenia '$command': ${e.message}", e)
            Result.failure(e)
        } finally {
            session?.disconnect()
        }
    }

    private fun executeCommandOnSession(session: Session, command: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.inputStream = null

        val stdoutStream = ByteArrayOutputStream()
        val stderrStream = ByteArrayOutputStream()
        channel.outputStream = stdoutStream
        channel.setErrStream(stderrStream)

        channel.connect()

        val start = System.currentTimeMillis()
        while (!channel.isClosed && System.currentTimeMillis() - start < 15000) {
            Thread.sleep(100)
        }

        channel.disconnect()
        val stdout = stdoutStream.toString(Charsets.UTF_8.name())
        val stderr = stderrStream.toString(Charsets.UTF_8.name())

        if (channel.exitStatus != 0 && stdout.isBlank() && stderr.isNotBlank()) {
            throw Exception("Polecenie zwróciło błąd (${channel.exitStatus}): $stderr")
        }
        return stdout
    }

    /**
     * Pobiera stan zasobów i kwot dyskowych z Raspberry Pi za pomocą jednego wywołania SSH:
     * - uptime
     * - df -h /
     * - free -m
     * - ps aux --sort=-%cpu | head -n 12
     * - quota -u <username> 2>/dev/null || du -sh /home/<username>
     */
    suspend fun fetchSystemResources(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String
    ): Result<SystemResources> = withContext(Dispatchers.IO) {
        val compositeCommand = """
            uptime; 
            echo "---DELIM_DF---"; df -h /; 
            echo "---DELIM_FREE---"; free -m; 
            echo "---DELIM_PS---"; ps aux --sort=-%cpu | head -n 13; 
            echo "---DELIM_QUOTA---"; quota -u $username 2>/dev/null || du -sh /home/$username 2>/dev/null || echo "Brak danych"
        """.trimIndent()

        val res = executeCommand(host, port, username, privateKeyPem, compositeCommand)
        res.map { output -> parseSystemResources(output, username) }
    }

    private fun parseSystemResources(output: String, username: String): SystemResources {
        val parts = output.split("---DELIM_")
        var uptimeStr = "Brak danych"
        var diskTotal = "0 GB"
        var diskUsed = "0 GB"
        var diskFree = "0 GB"
        var diskPercentage = 0f
        var ramUsed = "0 MB"
        var ramTotal = "0 MB"
        var ramPercentage = 0f
        var cpuSum = 0f
        var quotaUsed = "0 B"
        var quotaLimit = "Brak limitu"
        var quotaPercentage = 0f
        val processes = mutableListOf<ProcessInfo>()

        for (part in parts) {
            val trimmed = part.trim()
            when {
                trimmed.startsWith("DF---") -> {
                    val lines = trimmed.removePrefix("DF---").trim().lines()
                    if (lines.size >= 2) {
                        val row = lines[1].split("\\s+".toRegex())
                        if (row.size >= 5) {
                            diskTotal = row[1]
                            diskUsed = row[2]
                            diskFree = row[3]
                            diskPercentage = row[4].trimEnd('%').toFloatOrNull() ?: 0f
                        }
                    }
                }
                trimmed.startsWith("FREE---") -> {
                    val lines = trimmed.removePrefix("FREE---").trim().lines()
                    for (line in lines) {
                        if (line.startsWith("Mem:")) {
                            val row = line.split("\\s+".toRegex())
                            if (row.size >= 4) {
                                val totalMb = row[1].toFloatOrNull() ?: 1f
                                val usedMb = row[2].toFloatOrNull() ?: 0f
                                ramTotal = "${totalMb.toInt()} MB"
                                ramUsed = "${usedMb.toInt()} MB"
                                ramPercentage = (usedMb / totalMb * 100f).coerceIn(0f, 100f)
                            }
                        }
                    }
                }
                trimmed.startsWith("PS---") -> {
                    val lines = trimmed.removePrefix("PS---").trim().lines()
                    // Pomiń nagłówek (USER PID %CPU %MEM VSZ RSS TTY STAT START TIME COMMAND)
                    for (i in 1 until lines.size) {
                        val row = lines[i].trim().split("\\s+".toRegex())
                        if (row.size >= 11) {
                            val procUser = row[0]
                            val pid = row[1]
                            val cpu = row[2].toFloatOrNull() ?: 0f
                            val mem = row[3].toFloatOrNull() ?: 0f
                            val cmd = row.subList(10, row.size).joinToString(" ")
                            processes.add(ProcessInfo(procUser, pid, cpu, mem, cmd))
                            cpuSum += cpu
                        }
                    }
                }
                trimmed.startsWith("QUOTA---") -> {
                    val quotaBody = trimmed.removePrefix("QUOTA---").trim()
                    if (quotaBody.contains("Disk quotas for user") || quotaBody.lines().size > 2) {
                        // Parsowanie wyjścia polecenia quota -u
                        val lines = quotaBody.lines()
                        for (line in lines) {
                            if (line.contains("/") || line.contains("sda") || line.contains("mmcblk")) {
                                val cols = line.trim().split("\\s+".toRegex())
                                if (cols.size >= 4) {
                                    val usedBlocks = cols[1].replace("*", "")
                                    val quotaSoft = cols[2]
                                    quotaUsed = "$usedBlocks KB"
                                    quotaLimit = if (quotaSoft == "0") "Bez limitu" else "$quotaSoft KB"
                                    val usedVal = usedBlocks.toFloatOrNull() ?: 0f
                                    val limVal = quotaSoft.toFloatOrNull() ?: 0f
                                    if (limVal > 0) {
                                        quotaPercentage = (usedVal / limVal * 100f).coerceIn(0f, 100f)
                                    }
                                }
                            }
                        }
                    } else if (quotaBody.isNotEmpty()) {
                        // Fallback z du -sh /home/<username>
                        val cols = quotaBody.split("\\s+".toRegex())
                        if (cols.isNotEmpty()) {
                            quotaUsed = cols[0]
                            quotaLimit = "Katalog domowy (~$quotaUsed)"
                        }
                    }
                }
                else -> {
                    // Pierwsza część to uptime
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("---")) {
                        uptimeStr = trimmed.lines().firstOrNull() ?: trimmed
                    }
                }
            }
        }

        return SystemResources(
            cpuPercentage = (cpuSum).coerceIn(0f, 100f),
            ramUsed = ramUsed,
            ramTotal = ramTotal,
            ramPercentage = ramPercentage,
            diskUsed = diskUsed,
            diskTotal = diskTotal,
            diskPercentage = diskPercentage,
            diskFree = diskFree,
            userQuotaUsed = quotaUsed,
            userQuotaLimit = quotaLimit,
            userQuotaPercentage = quotaPercentage,
            topProcesses = processes,
            uptime = uptimeStr,
            rawOutput = output
        )
    }

    // --- SFTP Operacje ---

    /**
     * Pobiera listę plików i katalogów w danej ścieżce za pośrednictwem SFTP.
     */
    suspend fun listSftpFiles(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        remotePath: String
    ): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()

            val normalizedPath = if (remotePath.isBlank()) "." else remotePath
            val vector = channel.ls(normalizedPath) as Vector<*>
            val items = mutableListOf<RemoteFileItem>()

            for (entry in vector) {
                if (entry is ChannelSftp.LsEntry) {
                    val name = entry.filename
                    if (name == "." || name == "..") continue
                    val attrs = entry.attrs
                    val isDir = attrs.isDir
                    val size = attrs.size
                    val mtime = attrs.mTime.toLong() * 1000L
                    val permissions = attrs.permissionsString ?: ""
                    val fullPath = if (normalizedPath.endsWith("/")) "$normalizedPath$name" else "$normalizedPath/$name"

                    items.add(
                        RemoteFileItem(
                            name = name,
                            path = fullPath,
                            isDirectory = isDir,
                            size = size,
                            lastModified = mtime,
                            permissions = permissions
                        )
                    )
                }
            }

            // Sortuj: najpierw foldery alfabetycznie, potem pliki
            items.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd pobierania listy plików SFTP: ${e.message}", e)
            Result.failure(e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    suspend fun downloadSftpFile(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        remotePath: String,
        destinationStream: OutputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.get(remotePath, destinationStream)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    suspend fun uploadSftpFile(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        sourceStream: InputStream,
        remotePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.put(sourceStream, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    suspend fun deleteSftpFile(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        remotePath: String,
        isDirectory: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            if (isDirectory) {
                channel.rmdir(remotePath)
            } else {
                channel.rm(remotePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    suspend fun createSftpDirectory(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String,
        remotePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: Session? = null
        var channel: ChannelSftp? = null
        try {
            session = createSession(host, port, username, privateKeyPem)
            channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.mkdir(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
    }

    // --- Interaktywny Terminal SSH ---

    /**
     * Klasa sesji terminala SSH utrzymująca stałe, połączone połączenie SSH (Session)
     * i wykonująca polecenia w czasie rzeczywistym z obsługą katalogu roboczego (cd)
     * oraz strumieniowaniem stdout i stderr.
     */
    class TerminalSession(
        private val host: String,
        private val port: Int,
        private val username: String,
        private val privateKeyPem: String
    ) {
        private var session: Session? = null
        private var activeChannel: ChannelExec? = null

        var currentDirectory: String = "~"
            private set

        suspend fun connect(): Result<String> = withContext(Dispatchers.IO) {
            try {
                disconnect()
                val s = createSession(host, port, username, privateKeyPem)
                session = s

                // Pobierz banner i początkowy katalog roboczy
                val info = runQuickCommand(s, "pwd; uname -srm; uptime -p 2>/dev/null || true")
                val lines = info.trim().lines()
                if (lines.isNotEmpty()) {
                    val pwd = lines[0].trim()
                    if (pwd.startsWith("/")) currentDirectory = pwd
                }
                val banner = if (lines.size > 1) lines.subList(1, lines.size).joinToString(" • ") else "Raspberry Pi Linux"
                Result.success(banner)
            } catch (e: Exception) {
                Log.e(TAG, "Błąd łączenia z sesją terminala: ${e.message}", e)
                Result.failure(e)
            }
        }

        suspend fun executeCommand(
            command: String,
            onLineReceived: (String, Boolean) -> Unit
        ): Result<Int> = withContext(Dispatchers.IO) {
            val s = session
            if (s == null || !s.isConnected) {
                return@withContext Result.failure(Exception("Brak aktywnego połączenia SSH."))
            }

            var channel: ChannelExec? = null
            try {
                channel = s.openChannel("exec") as ChannelExec
                activeChannel = channel

                val wrappedCommand = """
                    cd "$currentDirectory" 2>/dev/null || cd ~
                    $command
                    __CMD_EXIT__=$?
                    echo "___SMARTSSH_PWD___"
                    pwd
                    exit ${'$'}__CMD_EXIT__
                """.trimIndent()

                channel.setCommand(wrappedCommand)
                channel.inputStream = null

                val stdout = channel.inputStream
                val stderr = channel.errStream

                channel.connect(5000)

                val outReader = BufferedReader(InputStreamReader(stdout, Charsets.UTF_8))
                val errReader = BufferedReader(InputStreamReader(stderr, Charsets.UTF_8))

                var reachedMarker = false
                var updatedPwd = currentDirectory

                coroutineScope {
                    val stdoutJob = launch {
                        while (coroutineContext.isActive) {
                            val line = outReader.readLine() ?: break
                            if (line == "___SMARTSSH_PWD___") {
                                reachedMarker = true
                                continue
                            }
                            if (reachedMarker) {
                                val trimmed = line.trim()
                                if (trimmed.startsWith("/")) {
                                    updatedPwd = trimmed
                                }
                            } else {
                                onLineReceived(line, false)
                            }
                        }
                    }

                    val stderrJob = launch {
                        while (coroutineContext.isActive) {
                            val errLine = errReader.readLine() ?: break
                            onLineReceived(errLine, true)
                        }
                    }

                    stdoutJob.join()
                    stderrJob.join()
                }

                while (!channel.isClosed && coroutineContext.isActive) {
                    delay(50)
                }

                currentDirectory = updatedPwd
                Result.success(channel.exitStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Błąd wykonania polecenia: ${e.message}", e)
                Result.failure(e)
            } finally {
                activeChannel = null
                channel?.disconnect()
            }
        }

        fun cancelCurrentCommand() {
            try {
                activeChannel?.disconnect()
                activeChannel = null
            } catch (_: Exception) {}
        }

        private fun runQuickCommand(session: Session, command: String): String {
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.inputStream = null
            val baos = ByteArrayOutputStream()
            channel.outputStream = baos
            channel.connect(5000)
            val start = System.currentTimeMillis()
            while (!channel.isClosed && System.currentTimeMillis() - start < 5000) {
                Thread.sleep(50)
            }
            channel.disconnect()
            return baos.toString(Charsets.UTF_8.name())
        }

        fun isConnected(): Boolean = session?.isConnected == true

        fun disconnect() {
            try { activeChannel?.disconnect() } catch (_: Exception) {}
            activeChannel = null
            try { session?.disconnect() } catch (_: Exception) {}
            session = null
        }
    }

    fun createTerminalSession(
        host: String,
        port: Int,
        username: String,
        privateKeyPem: String
    ): TerminalSession {
        return TerminalSession(host, port, username, privateKeyPem)
    }
}
