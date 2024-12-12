package com.ict.expensemanagement

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll(): List<User>

    @Insert
    fun insertAll(vararg user: User)

    @Delete
    fun delete(user: User)

    @Update
    fun update(vararg user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): User

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId")
    fun getMoneyByUserId(userId: String): Double

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    fun checkUsernameExists(username: String): Int
}