package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

enum class TransactionType {
    INCOME, EXPENSE
}
