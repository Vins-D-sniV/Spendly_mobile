package com.example.spendly

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAtMillis DESC, id DESC")
    fun observeExpenses(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: String)
}

class ExpenseDateConverters {
    @TypeConverter
    fun dateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
@TypeConverters(ExpenseDateConverters::class)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        private var instance: SpendlyDatabase? = null

        @Synchronized
        fun getInstance(context: Context): SpendlyDatabase {
            return instance ?: Room.databaseBuilder(
                context.applicationContext,
                SpendlyDatabase::class.java,
                "spendly.db"
            ).build().also { instance = it }
        }
    }
}
