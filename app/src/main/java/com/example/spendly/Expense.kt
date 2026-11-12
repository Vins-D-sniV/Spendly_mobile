package com.example.spendly

import java.time.LocalDate

data class Expense(
    val id : String,
    val description: String,
    val amountCents : Long,
    val category: String,
    val date: LocalDate
)