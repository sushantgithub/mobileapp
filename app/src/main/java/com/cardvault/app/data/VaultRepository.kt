package com.cardvault.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

class VaultRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val vaultFile = File(appContext.filesDir, VAULT_FILE)

    val isInitialized: Boolean
        get() = prefs.contains(KEY_WRAPPED_DEK) && vaultFile.exists()

    fun createVault(pin: CharArray): ByteArray {
        require(pin.size in 4..8 && pin.all { it.isDigit() }) { "PIN must be 4-8 digits" }
        val dek = CryptoEngine.generateDek()
        val wrapped = CryptoEngine.wrapDek(dek, pin)
        val empty = VaultPayload()
        persistVault(dek, empty)
        prefs.edit()
            .putString(KEY_WRAPPED_DEK, json.encodeToString(wrapped))
            .putInt(KEY_FAILED_UNLOCKS, 0)
            .apply()
        return dek
    }

    fun unlock(pin: CharArray): UnlockResult {
        val wrappedJson = prefs.getString(KEY_WRAPPED_DEK, null) ?: return UnlockResult.NotInitialized
        val wrapped = json.decodeFromString<WrappedKey>(wrappedJson)
        val dek = try {
            CryptoEngine.unwrapDek(wrapped, pin)
        } catch (_: Exception) {
            val fails = prefs.getInt(KEY_FAILED_UNLOCKS, 0) + 1
            prefs.edit().putInt(KEY_FAILED_UNLOCKS, fails).apply()
            return UnlockResult.InvalidPin(fails)
        }
        prefs.edit().putInt(KEY_FAILED_UNLOCKS, 0).apply()
        val payload = readVault(dek)
        return UnlockResult.Success(dek, payload)
    }

    fun load(dek: ByteArray): VaultPayload = readVault(dek)

    fun save(dek: ByteArray, payload: VaultPayload) {
        persistVault(dek, payload)
    }

    fun changePin(dek: ByteArray, newPin: CharArray) {
        require(newPin.size in 4..8 && newPin.all { it.isDigit() }) { "PIN must be 4-8 digits" }
        val wrapped = CryptoEngine.wrapDek(dek, newPin)
        prefs.edit().putString(KEY_WRAPPED_DEK, json.encodeToString(wrapped)).apply()
    }

    fun exportBackup(dek: ByteArray, backupPassword: CharArray): ByteArray {
        require(backupPassword.size >= 8) { "Backup password must be at least 8 characters" }
        val vaultBytes = vaultFile.readBytes()
        val wrapped = CryptoEngine.wrapDek(dek, backupPassword)
        val backup = EncryptedBackup(
            wrappedDek = wrapped,
            vaultIvB64 = CryptoEngine.b64(vaultBytes.copyOfRange(0, 12)),
            vaultCiphertextB64 = CryptoEngine.b64(vaultBytes.copyOfRange(12, vaultBytes.size)),
        )
        return json.encodeToString(backup).toByteArray(StandardCharsets.UTF_8)
    }

    fun importBackup(backupBytes: ByteArray, backupPassword: CharArray, newPin: CharArray): ByteArray {
        val backup = json.decodeFromString<EncryptedBackup>(
            String(backupBytes, StandardCharsets.UTF_8),
        )
        require(backup.magic == EncryptedBackup.MAGIC) { "Not a Card Vault backup" }
        val dek = CryptoEngine.unwrapDek(backup.wrappedDek, backupPassword)
        val ciphertext = CryptoEngine.fromB64(backup.vaultCiphertextB64)
        val iv = CryptoEngine.fromB64(backup.vaultIvB64)
        val plaintext = CryptoEngine.decrypt(ciphertext, dek, iv)
        json.decodeFromString<VaultPayload>(String(plaintext, StandardCharsets.UTF_8))
        writeRawVault(iv, ciphertext)
        changePin(dek, newPin)
        return dek
    }

    fun wipe() {
        prefs.edit().clear().apply()
        if (vaultFile.exists()) {
            vaultFile.writeBytes(ByteArray(vaultFile.length().toInt()))
            vaultFile.delete()
        }
    }

    private fun persistVault(dek: ByteArray, payload: VaultPayload) {
        val plaintext = json.encodeToString(payload).toByteArray(StandardCharsets.UTF_8)
        val sealed = CryptoEngine.encrypt(plaintext, dek)
        writeRawVault(sealed.iv, sealed.ciphertext)
        plaintext.fill(0)
    }

    private fun writeRawVault(iv: ByteArray, ciphertext: ByteArray) {
        val out = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(ciphertext, 0, out, iv.size, ciphertext.size)
        vaultFile.parentFile?.mkdirs()
        val tmp = File(appContext.filesDir, "$VAULT_FILE.tmp")
        tmp.writeBytes(out)
        if (!tmp.renameTo(vaultFile)) {
            vaultFile.writeBytes(out)
            tmp.delete()
        }
    }

    private fun readVault(dek: ByteArray): VaultPayload {
        val raw = vaultFile.readBytes()
        require(raw.size > 12) { "Vault file is corrupt" }
        val iv = raw.copyOfRange(0, 12)
        val ciphertext = raw.copyOfRange(12, raw.size)
        val plaintext = CryptoEngine.decrypt(ciphertext, dek, iv)
        return json.decodeFromString(String(plaintext, StandardCharsets.UTF_8))
    }

    companion object {
        private const val PREFS_NAME = "card_vault_secure_prefs"
        private const val VAULT_FILE = "vault.bin"
        private const val KEY_WRAPPED_DEK = "wrapped_dek"
        private const val KEY_FAILED_UNLOCKS = "failed_unlocks"
    }
}

sealed class UnlockResult {
    data class Success(val dek: ByteArray, val payload: VaultPayload) : UnlockResult()
    data class InvalidPin(val failedAttempts: Int) : UnlockResult()
    data object NotInitialized : UnlockResult()
}

fun CharArray.toUtf8Bytes(): ByteArray {
    val buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(this))
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

fun wipeChars(value: CharArray) {
    java.util.Arrays.fill(value, '\u0000')
}

fun ByteBuffer.wipe() {
    if (hasArray()) {
        java.util.Arrays.fill(array(), 0)
    }
}
