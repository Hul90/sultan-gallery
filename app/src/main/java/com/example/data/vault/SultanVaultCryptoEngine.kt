package com.example.data.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SultanVaultCryptoEngine {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SultanGalleryVaultMasterKey_v1"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts stream data into a secure encrypted file using AES-256-GCM.
     * Format on disk: [12-byte IV] + [Ciphertext with GCM Auth Tag]
     */
    suspend fun encryptStreamToFile(input: InputStream, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv

            FileOutputStream(destinationFile).use { fos ->
                // Write IV first
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    input.copyTo(cos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            if (destinationFile.exists()) destinationFile.delete()
            false
        }
    }

    /**
     * Decrypts encrypted file using AES-256-GCM and writes plaintext to output stream.
     */
    suspend fun decryptFileToStream(encryptedFile: File, output: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!encryptedFile.exists()) return@withContext false
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val bytesRead = fis.read(iv)
                if (bytesRead < GCM_IV_LENGTH) return@withContext false

                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

                CipherInputStream(fis, cipher).use { cis ->
                    cis.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts an encrypted image file directly into memory as a Bitmap without writing plaintext to disk.
     */
    suspend fun decryptFileToBitmap(encryptedFile: File): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!encryptedFile.exists()) return@withContext null
            val baos = ByteArrayOutputStream()
            val success = decryptFileToStream(encryptedFile, baos)
            if (success) {
                val bytes = baos.toByteArray()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a versioned, per-installation PIN verifier.
     * Format: v2$<hex-salt>$<hex-derived-key>
     */
    fun hashPin(pin: String, salt: String? = null): String {
        val saltBytes = if (salt != null) {
            hexToBytes(salt)
        } else {
            ByteArray(16).also { SecureRandom().nextBytes(it) }
        }
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, 120_000, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return "v2\$${bytesToHex(saltBytes)}\$${bytesToHex(derived)}"
    }

    fun verifyPin(pinInput: String, storedHash: String, salt: String? = null): Boolean {
        if (storedHash.isBlank()) return false

        if (storedHash.startsWith("v2$")) {
            val parts = storedHash.split('$')
            if (parts.size != 3) return false
            return try {
                val saltBytes = hexToBytes(parts[1])
                val expected = hexToBytes(parts[2])
                val spec = PBEKeySpec(pinInput.toCharArray(), saltBytes, 120_000, expected.size * 8)
                val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                MessageDigest.isEqual(actual, expected)
            } catch (_: Exception) {
                false
            }
        }

        // Legacy SHA-256 verifier for installations created by the older build.
        val legacySalt = salt ?: "SultanGallerySalt_9981"
        val digest = MessageDigest.getInstance("SHA-256")
        val legacy = digest.digest((legacySalt + pinInput).toByteArray(Charsets.UTF_8))
        val computed = bytesToHex(legacy)
        return MessageDigest.isEqual(computed.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0)
        return ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

}
