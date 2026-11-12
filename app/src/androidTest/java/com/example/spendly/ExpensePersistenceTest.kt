package com.example.spendly

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpensePersistenceTest {
    @Test
    fun additionsAndDeletionsSurviveReopeningTheDatabase() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "spendly-persistence-${UUID.randomUUID()}.db"
        val groceries = Expense(
            id = "groceries",
            description = "Courses",
            amountCents = 4_250L,
            category = "Alimentation",
            date = LocalDate.of(2026, 11, 11),
            createdAtMillis = 1L
        )
        val transport = Expense(
            id = "transport",
            description = "Métro",
            amountCents = 230L,
            category = "Transport",
            date = LocalDate.of(2026, 11, 12),
            createdAtMillis = 2L
        )

        try {
            withDao(context, name) { dao ->
                dao.insert(groceries)
                dao.insert(transport)
            }
            withDao(context, name) { dao ->
                assertEquals(listOf(transport, groceries), dao.observeExpenses().first())
                dao.deleteById(transport.id)
            }
            withDao(context, name) { dao ->
                assertEquals(listOf(groceries), dao.observeExpenses().first())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private suspend fun withDao(
        context: Context,
        name: String,
        block: suspend (ExpenseDao) -> Unit
    ) {
        val database = Room.databaseBuilder(context, SpendlyDatabase::class.java, name).build()
        try {
            block(database.expenseDao())
        } finally {
            database.close()
        }
    }
}
