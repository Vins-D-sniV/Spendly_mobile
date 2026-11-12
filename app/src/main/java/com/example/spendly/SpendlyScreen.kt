package com.example.spendly

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun SpendlySreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val dao = remember(context) { SpendlyDatabase.getInstance(context).expenseDao() }
    var loadedExpenses by remember(dao) { mutableStateOf<List<Expense>?>(null) }
    var readError by remember(dao) { mutableStateOf(false) }

    LaunchedEffect(dao) {
        try {
            dao.observeExpenses().collect { expenses -> loadedExpenses = expenses }
        } catch (_: SQLiteException) {
            readError = true
        }
    }

    val expenses = loadedExpenses
    when {
        readError -> Text("Impossible de lire les dépenses. Relance l’application.", modifier)
        expenses == null -> Text("Chargement des dépenses…", modifier)
        else -> SpendlyContent(
            expenses = expenses,
            onAdd = dao::insert,
            onDelete = dao::deleteById,
            modifier = modifier
        )
    }
}

@Composable
internal fun SpendlyContent(
    expenses: List<Expense>,
    onAdd: suspend (Expense) -> Unit,
    onDelete: suspend (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var deleteError by remember { mutableStateOf(false) }

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
            ExpenseForm(scope = scope, onAdd = onAdd)
        }
        if (deleteError) {
            item { Text("Impossible de supprimer la dépense. Réessaie.") }
        }
        items(expenses, key = { it.id }) { expense ->
            Column {
                Text(expense.description)
                Text("${expense.category} : ${formatEuros(expense.amountCents)}")
                Button(onClick = {
                    scope.launch {
                        try {
                            onDelete(expense.id)
                            deleteError = false
                        } catch (_: SQLiteException) {
                            deleteError = true
                        }
                    }
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
private fun ExpenseForm(scope: CoroutineScope, onAdd: suspend (Expense) -> Unit) {
    var saving by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentation") }
    var error by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            enabled = !saving,
            value = description,
            onValueChange = { newText -> description = newText },
            label = { Text("Description") },
            singleLine = true
        )

        OutlinedTextField(
            enabled = !saving,
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
                    enabled = !saving,
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option) }
                )
            }
        }

        Button(
            enabled = !saving,
            onClick = {
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
                    saving = true
                    error = ""
                    scope.launch {
                        try {
                            onAdd(expense)
                            description = ""
                            amount = ""
                        } catch (_: SQLiteException) {
                            error = "Impossible d’enregistrer la dépense. Réessaie."
                        } finally {
                            saving = false
                        }
                    }
                }
            }
        ) {
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
