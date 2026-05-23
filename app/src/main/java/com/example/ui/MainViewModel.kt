package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val repository: TransactionRepository) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentMonthTransactions = repository.allTransactions.map { transactions ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        transactions.filter {
            calendar.timeInMillis = it.timestamp
            calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val monthlyGroupedTransactions = repository.allTransactions.map { transactions ->
        transactions.groupBy {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("bn", "BD"))
            sdf.format(Date(it.timestamp))
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap())

    fun addTransaction(amount: Double, type: TransactionType, description: String) {
        viewModelScope.launch {
            repository.insert(Transaction(amount = amount, type = type, description = description))
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}

class MainViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
