package com.dueday.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dueday.app.data.Bill
import com.dueday.app.data.BillRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillUiState(
    val bills: List<Bill> = emptyList(),
    val message: String? = null,
)

class BillViewModel(private val repository: BillRepository) : ViewModel() {
    private val _state = MutableStateFlow(BillUiState())
    val state: StateFlow<BillUiState> = _state

    init {
        _state.update {
            it.copy(bills = repository.load().sortedBy { bill -> bill.generationDay().takeIf { day -> day > 0 } ?: bill.paymentDueDay() })
        }
    }

    fun upsert(
        id: String?,
        title: String,
        amount: String,
        billGenerationDay: Int,
        dueDay: Int,
        notes: String,
        notify: Boolean,
    ): Boolean {
        if (title.isBlank()) {
            _state.update { it.copy(message = "Give this bill a name") }
            return false
        }
        if (billGenerationDay !in 1..31 && dueDay !in 1..31) {
            _state.update { it.copy(message = "Pick a bill generation day or a due day") }
            return false
        }
        if (billGenerationDay != 0 && billGenerationDay !in 1..31) {
            _state.update { it.copy(message = "Bill generation day must be 1–31") }
            return false
        }
        if (dueDay != 0 && dueDay !in 1..31) {
            _state.update { it.copy(message = "Due day must be 1–31") }
            return false
        }
        val record = Bill(
            id = id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            amount = amount.trim(),
            dayOfMonth = dueDay,
            billGenerationDay = billGenerationDay,
            dueDay = dueDay,
            notes = notes.trim(),
            notify = notify,
        )
        val next = _state.value.bills.filterNot { it.id == record.id } + record
        persist(next)
        return true
    }

    fun delete(id: String) {
        persist(_state.value.bills.filterNot { it.id == id })
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun persist(bills: List<Bill>) {
        viewModelScope.launch {
            repository.save(bills)
            _state.update {
                it.copy(
                    bills = bills.sortedBy { bill ->
                        bill.generationDay().takeIf { day -> day > 0 } ?: bill.paymentDueDay()
                    },
                    message = null,
                )
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val repo = BillRepository(context.applicationContext)
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BillViewModel(repo) as T
                }
            }
        }
    }
}
