package com.example.mobile_smart_ssh

import com.example.mobile_smart_ssh.core.security.KeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyManagerTest {

    @Test
    fun testGenerateEd25519KeyPair() {
        val keyPair = KeyManager.generateEd25519KeyPair("testuser")

        assertNotNull(keyPair)
        assertEquals("ed25519", keyPair.keyType)
        assertTrue("Klucz publiczny powinien zaczynać się od ssh-ed25519", keyPair.publicKey.startsWith("ssh-ed25519 "))
        assertTrue("Klucz publiczny powinien zawierać komentarz użytkownika", keyPair.publicKey.endsWith("testuser@smartssh-mobile"))
        assertTrue("Klucz prywatny powinien zaczynać się od nagłówka OpenSSH", keyPair.privateKeyPem.contains("-----BEGIN OPENSSH PRIVATE KEY-----"))
        assertTrue("Klucz prywatny powinien kończyć się stopką OpenSSH", keyPair.privateKeyPem.contains("-----END OPENSSH PRIVATE KEY-----"))
        assertTrue("Odcisk palca powinien zaczynać się od SHA256:", keyPair.fingerprint.startsWith("SHA256:"))
    }

    @Test
    fun testCalculateFingerprint() {
        val keyPair = KeyManager.generateEd25519KeyPair("minow")
        val computedFingerprint = KeyManager.calculateFingerprint(keyPair.publicKey)

        assertEquals("Wyliczony fingerprint powinien być identyczny z wygenerowanym", keyPair.fingerprint, computedFingerprint)
    }

    @Test
    fun testCalculateFingerprintInvalidInput() {
        val singleWord = KeyManager.calculateFingerprint("nieprawidlowy")
        assertEquals("Nieprawidłowy format", singleWord)

        val badBase64 = KeyManager.calculateFingerprint("ssh-ed25519 nieprawidlowy_base64")
        assertTrue(badBase64.startsWith("Błąd obliczania odcisku"))
    }

    @Test
    fun testIsValidPublicKey() {
        val validEd25519 = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGtY9k76PjHjU2Y730s034kF3g5r7Q7b1mP2k4w6e8t0 user@host"
        val validRsa = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC3 user@host"
        val invalidKey = "to_nie_jest_klucz_ssh"
        val emptyKey = ""

        assertTrue(KeyManager.isValidPublicKey(validEd25519))
        assertTrue(KeyManager.isValidPublicKey(validRsa))
        assertFalse(KeyManager.isValidPublicKey(invalidKey))
        assertFalse(KeyManager.isValidPublicKey(emptyKey))
    }

    @Test
    fun testIsValidPrivateKey() {
        val validOpenSsh = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()

        val validPkcs8 = """
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIP...
            -----END PRIVATE KEY-----
        """.trimIndent()

        val invalidKey = "to nie jest klucz prywatny"

        assertTrue(KeyManager.isValidPrivateKey(validOpenSsh))
        assertTrue(KeyManager.isValidPrivateKey(validPkcs8))
        assertFalse(KeyManager.isValidPrivateKey(invalidKey))
    }
}
