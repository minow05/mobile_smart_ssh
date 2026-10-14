package com.example.mobile_smart_ssh.core.security

import android.util.Base64
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Kontener na wygenerowaną lub zaimportowaną parę kluczy SSH.
 */
data class SshKeyPair(
    val publicKey: String,
    val privateKeyPem: String,
    val fingerprint: String,
    val keyType: String = "ed25519"
)

/**
 * Menadżer kryptografii SSH odpowiedzialny za:
 * - Generowanie par kluczy Ed25519
 * - Formatowanie do standardu OpenSSH (RFC 4253 / RFC 8709)
 * - Obliczanie skrótów SHA256 (fingerprint)
 * - Walidację poprawności kluczy
 */
object KeyManager {

    private const val KEY_TYPE = "ssh-ed25519"

    /**
     * Generuje parę kluczy Ed25519 przy użyciu BouncyCastle.
     * Zwraca gotowy obiekt [SshKeyPair] sformatowany dla serwera OpenSSH (Raspberry Pi).
     */
    fun generateEd25519KeyPair(username: String = "minow"): SshKeyPair {
        val random = SecureRandom()
        val keyGen = Ed25519KeyPairGenerator()
        keyGen.init(Ed25519KeyGenerationParameters(random))

        val keyPair = keyGen.generateKeyPair()
        val privParams = keyPair.private as Ed25519PrivateKeyParameters
        val pubParams = keyPair.public as Ed25519PublicKeyParameters

        // 1. Formatowanie klucza publicznego OpenSSH
        val pubBytes = OpenSSHPublicKeyUtil.encodePublicKey(pubParams)
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val comment = "$username@smartssh-mobile"
        val publicKeyString = "$KEY_TYPE $pubBase64 $comment"

        // 2. Formatowanie klucza prywatnego OpenSSH (nagłówek i stopka OpenSSH)
        val privBytes = OpenSSHPrivateKeyUtil.encodePrivateKey(privParams)
        val privBase64 = Base64.encodeToString(privBytes, Base64.NO_WRAP)
        val privateKeyPem = buildString {
            appendLine("-----BEGIN OPENSSH PRIVATE KEY-----")
            privBase64.chunked(70).forEach { appendLine(it) }
            appendLine("-----END OPENSSH PRIVATE KEY-----")
        }

        // 3. Obliczenie odcisku palca (SHA256 Fingerprint)
        val fingerprint = calculateFingerprintBytes(pubBytes)

        return SshKeyPair(
            publicKey = publicKeyString,
            privateKeyPem = privateKeyPem,
            fingerprint = fingerprint,
            keyType = "ed25519"
        )
    }

    /**
     * Oblicza odcisk palca SHA256 z zakodowanego klucza publicznego w formacie OpenSSH:
     * SHA256:<skrót base64 bez paddingu =>
     */
    fun calculateFingerprint(publicKeyString: String): String {
        return try {
            val parts = publicKeyString.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                val pubBytes = Base64.decode(parts[1], Base64.DEFAULT)
                calculateFingerprintBytes(pubBytes)
            } else {
                "Nieprawidłowy format"
            }
        } catch (e: Exception) {
            "Błąd obliczania odcisku: ${e.message}"
        }
    }

    private fun calculateFingerprintBytes(publicKeyBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(publicKeyBytes)
        val base64 = Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')
        return "SHA256:$base64"
    }

    /**
     * Weryfikuje czy podany łańcuch znaków reprezentuje prawidłowy klucz publiczny ssh-ed25519.
     */
    fun isValidPublicKey(key: String): Boolean {
        val trimmed = key.trim()
        if (!trimmed.startsWith("ssh-ed25519 ") && !trimmed.startsWith("ssh-rsa ") && !trimmed.startsWith("ecdsa-")) {
            return false
        }
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size < 2) return false
        return try {
            Base64.decode(parts[1], Base64.DEFAULT).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sprawdza czy tekst wygląda na klucz prywatny w formacie PEM (OpenSSH lub PKCS#8).
     */
    fun isValidPrivateKey(pem: String): Boolean {
        val trimmed = pem.trim()
        return (trimmed.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----") ||
                trimmed.startsWith("-----BEGIN PRIVATE KEY-----") ||
                trimmed.startsWith("-----BEGIN RSA PRIVATE KEY-----")) &&
                (trimmed.contains("-----END OPENSSH PRIVATE KEY-----") ||
                        trimmed.contains("-----END PRIVATE KEY-----") ||
                        trimmed.contains("-----END RSA PRIVATE KEY-----"))
    }
}
