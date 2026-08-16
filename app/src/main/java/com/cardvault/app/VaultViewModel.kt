package com.cardvault.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardvault.app.data.CardBrand
import com.cardvault.app.data.CardNumberFormatter
import com.cardvault.app.data.CardRecord
import com.cardvault.app.data.UnlockResult
import com.cardvault.app.data.VaultPayload
import com.cardvault.app.data.VaultRepository
import com.cardvault.app.data.wipeChars
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VaultUiState(
    val ready: Boolean = false,
    val initialized: Boolean = false,
    val unlocked: Boolean = false,
    val cards: List<CardRecord> = emptyList(),
    val message: String? = null,
    val busy: Boolean = false,
    val failedAttempts: Int = 0,
)

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {
    private val _state = MutableStateFlow(VaultUiState())
    val state: StateFlow<VaultUiState> = _state

    private var dek: ByteArray? = null
    private var autoLockJob: Job? = null

    init {
        _state.update {
            it.copy(ready = true, initialized = repository.isInitialized)
        }
    }

    fun createPin(pin: CharArray, confirm: CharArray) {
        if (!pin.contentEquals(confirm)) {
            wipeChars(pin)
            wipeChars(confirm)
            _state.update { it.copy(message = "PINs do not match") }
            return
        }
        if (pin.size !in 4..8 || pin.any { !it.isDigit() }) {
            wipeChars(pin)
            wipeChars(confirm)
            _state.update { it.copy(message = "PIN must be 4–8 digits") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                withContext(Dispatchers.Default) {
                    val key = repository.createVault(pin)
                    dek = key
                    repository.load(key)
                }
            }.onSuccess { payload ->
                _state.update {
                    it.copy(
                        busy = false,
                        initialized = true,
                        unlocked = true,
                        cards = payload.cards.sortedBy { card -> card.nickname.lowercase() },
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = error.message ?: "Could not create vault") }
            }
            wipeChars(pin)
            wipeChars(confirm)
        }
    }

    fun unlock(pin: CharArray) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val result = withContext(Dispatchers.Default) { repository.unlock(pin) }
            wipeChars(pin)
            when (result) {
                is UnlockResult.Success -> {
                    dek = result.dek
                    _state.update {
                        it.copy(
                            busy = false,
                            unlocked = true,
                            initialized = true,
                            failedAttempts = 0,
                            cards = result.payload.cards.sortedBy { card -> card.nickname.lowercase() },
                        )
                    }
                }
                is UnlockResult.InvalidPin -> {
                    _state.update {
                        it.copy(
                            busy = false,
                            failedAttempts = result.failedAttempts,
                            message = "Incorrect PIN (${result.failedAttempts} failed attempts)",
                        )
                    }
                }
                UnlockResult.NotInitialized -> {
                    _state.update { it.copy(busy = false, initialized = false, message = "Vault is not set up") }
                }
            }
        }
    }

    fun lock() {
        dek?.fill(0)
        dek = null
        autoLockJob?.cancel()
        _state.update { it.copy(unlocked = false, cards = emptyList()) }
    }

    fun scheduleAutoLock() {
        autoLockJob?.cancel()
        autoLockJob = viewModelScope.launch {
            delay(AUTO_LOCK_MS)
            lock()
        }
    }

    fun cancelAutoLock() {
        autoLockJob?.cancel()
    }

    fun upsertCard(
        existingId: String?,
        nickname: String,
        cardholderName: String,
        number: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String,
        billingZip: String,
        notes: String,
    ): Boolean {
        val digits = CardNumberFormatter.digitsOnly(number)
        if (nickname.isBlank()) {
            _state.update { it.copy(message = "Give this card a nickname") }
            return false
        }
        if (!CardNumberFormatter.luhnValid(digits)) {
            _state.update { it.copy(message = "Card number failed the checksum check") }
            return false
        }
        if (expiryMonth !in 1..12 || expiryYear < 2024) {
            _state.update { it.copy(message = "Expiry date is invalid") }
            return false
        }
        if (cvv.length !in 3..4 || cvv.any { !it.isDigit() }) {
            _state.update { it.copy(message = "CVV must be 3 or 4 digits") }
            return false
        }
        val key = dek ?: return false
        val now = System.currentTimeMillis()
        val current = _state.value.cards
        val record = CardRecord(
            id = existingId ?: UUID.randomUUID().toString(),
            nickname = nickname.trim(),
            cardholderName = cardholderName.trim(),
            number = digits,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            cvv = cvv,
            brand = CardBrand.detect(digits),
            billingZip = billingZip.trim(),
            notes = notes.trim(),
            createdAtEpochMs = current.firstOrNull { it.id == existingId }?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
        )
        val next = current.filterNot { it.id == record.id } + record
        persist(key, next)
        return true
    }

    fun deleteCard(id: String) {
        val key = dek ?: return
        persist(key, _state.value.cards.filterNot { it.id == id })
    }

    fun changePin(currentPin: CharArray, newPin: CharArray, confirm: CharArray) {
        if (!newPin.contentEquals(confirm)) {
            wipeChars(currentPin); wipeChars(newPin); wipeChars(confirm)
            _state.update { it.copy(message = "New PINs do not match") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            val unlock = withContext(Dispatchers.Default) { repository.unlock(currentPin) }
            wipeChars(currentPin)
            if (unlock !is UnlockResult.Success) {
                wipeChars(newPin); wipeChars(confirm)
                _state.update { it.copy(busy = false, message = "Current PIN is incorrect") }
                return@launch
            }
            runCatching {
                withContext(Dispatchers.Default) { repository.changePin(unlock.dek, newPin) }
            }.onSuccess {
                dek = unlock.dek
                _state.update { it.copy(busy = false, message = "PIN updated") }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = error.message) }
            }
            wipeChars(newPin)
            wipeChars(confirm)
        }
    }

    fun exportBackup(context: Context, password: CharArray): Uri? {
        val key = dek ?: return null
        if (password.size < 8) {
            wipeChars(password)
            _state.update { it.copy(message = "Backup password must be at least 8 characters") }
            return null
        }
        return try {
            val bytes = repository.exportBackup(key, password)
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(dir, "card-vault-backup.cvault")
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        } catch (error: Exception) {
            _state.update { it.copy(message = error.message ?: "Export failed") }
            null
        } finally {
            wipeChars(password)
        }
    }

    fun importBackup(bytes: ByteArray, password: CharArray, pin: CharArray) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                withContext(Dispatchers.Default) {
                    val key = repository.importBackup(bytes, password, pin)
                    key to repository.load(key)
                }
            }.onSuccess { (key, payload) ->
                dek = key
                _state.update {
                    it.copy(
                        busy = false,
                        initialized = true,
                        unlocked = true,
                        cards = payload.cards.sortedBy { card -> card.nickname.lowercase() },
                        message = "Vault restored",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = error.message ?: "Restore failed") }
            }
            wipeChars(password)
            wipeChars(pin)
        }
    }

    fun copyThenClear(context: Context, label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        _state.update { it.copy(message = "$label copied. Clipboard will clear in 30s.") }
        Handler(Looper.getMainLooper()).postDelayed({
            clipboard.clearPrimaryClip()
        }, 30_000)
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun wipeVault() {
        repository.wipe()
        dek?.fill(0)
        dek = null
        _state.update { VaultUiState(ready = true, initialized = false) }
    }

    private fun persist(key: ByteArray, cards: List<CardRecord>) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    repository.save(key, VaultPayload(cards = cards))
                }
            }.onSuccess {
                _state.update {
                    it.copy(cards = cards.sortedBy { card -> card.nickname.lowercase() }, message = null)
                }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "Save failed") }
            }
        }
    }

    companion object {
        const val AUTO_LOCK_MS = 45_000L
    }
}

class VaultViewModelFactory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VaultViewModel(repository) as T
    }
}

fun shareBackup(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Save encrypted backup"))
}
