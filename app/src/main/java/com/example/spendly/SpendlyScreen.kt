package com.example.spendly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import java.time.YearMonth
import java.util.UUID
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun SpendlySreen( modifier: Modifier = Modifier){

//    var expenses by remember {
//        mutableStateOf(
//            listOf(
//                Expense("1", "Courses", 4_250L, "Alimentation", LocalDate.now()),
//                Expense("2", "Métro", 230L, "Transport", LocalDate.now())
//            )
//        )
//    }

    var expenses by remember { mutableStateOf(emptyList<Expense>()) }

    val currentMonth = YearMonth.now()
    val totalCents = expenses
        .filter { YearMonth.from(it.date) == currentMonth }
        .sumOf { it.amountCents }
    val remainingCents = 40_000L - totalCents

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Spendly")
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ce mois-ci")
                    Text("${formatEuros(totalCents)} dépensés sur ${formatEuros(40_000L)}")

                    if (remainingCents >= 0L) {
                        Text("Reste ${formatEuros(remainingCents)}")
                    } else {
                        Text("Dépassement de ${formatEuros(-remainingCents)}")
                    }
                }
            }
        }
        item {
            ExpenseForm { newExpense ->
                expenses = listOf(newExpense) + expenses
            }
        }
        items (expenses, key ={ it.id}) {
            expense ->
            Column {
                Text(expense.description)
                Text("${expense.category} : ${formatEuros(expense.amountCents)}")
                Button(onClick = {
                    expenses = expenses.filterNot { it.id == expense.id }
                }) {
                    Text("Supprimer")
                }
            }
        }
    }

}
private fun formatEuros(cents: Long): String {
    val amount = BigDecimal.valueOf(cents, 2)
    return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(amount)
}

@Composable
private fun ExpenseForm(onAdd: (Expense) -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var error by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = description,
            onValueChange = { newText -> description = newText },
            label = { Text("Description") },
            singleLine = true
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { newText -> amount = newText },
            label = { Text("Montant en euros") },
            singleLine = true
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Alimentation", "Transport", "Autre").forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option) }
                )
            }
        }

        Button(onClick = {
            val cents = parseAmountCents(amount)

            if (description.isBlank() || cents == null) {
                error = "Saisis une description et un montant positif avec deux décimales au maximum."
            } else {
                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    description = description.trim(),
                    amountCents = cents,
                    category = category,
                    date = LocalDate.now()
                )
                onAdd(expense)
                description = ""
                amount = ""
                error = ""
            }
        }) {
            Text("Ajouter la dépense")
        }

        if (error.isNotEmpty()) {
            Text(error)
        }
    }
}

private fun parseAmountCents(input: String): Long? {
    val amount = input.trim().replace(',', '.').toBigDecimalOrNull()
        ?: return null

    if (amount.signum() <= 0 || amount.scale() !in 0..2) {
        return null
    }

    return runCatching { amount.movePointRight(2).longValueExact() }.getOrNull()
}
