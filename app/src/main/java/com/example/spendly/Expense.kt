package com.example.spendly

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val description: String,
    val amountCents: Long,
    val category: String,
    val date: LocalDate,
    val createdAtMillis: Long = System.currentTimeMillis()
)
