package com.ict.expensemanagement

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE transactionDate = :date AND userId = :userId")
    fun getTransByDate(date: String, userId: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE strftime('%W', transactionDate) = :week AND strftime('%Y', transactionDate) = :year AND userId = :userId")
    fun getTransByWeek(year: String, week: String, userId: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE strftime('%m', transactionDate) = :month AND strftime('%Y', transactionDate) = :year AND userId = :userId" )
    fun getTransByMonth(year: String, month: String, userId: String): List<Transaction>

    @Query("SELECT * FROM transactions")
    fun getAll(): List<Transaction>
    @Insert
    fun insertAll(vararg transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Update
    fun update(vararg transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId")
    fun getTransByUserId(userId: String): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY id DESC LIMIT 1")
    fun getTranWithLargestId(): List<Transaction>
}