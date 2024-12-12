package com.ict.expensemanagement

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@TypeConverters(DateConverter::class)
@Database(entities = [Transaction::class, User::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao() : TransactionDao
    abstract fun userDao() : UserDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Perform migration steps here (e.g., CREATE TABLE, ALTER TABLE)
        // Rename the new table to the original table name
        database.execSQL("ALTER TABLE transactions ADD COLUMN transactionDate TEXT NOT NULL");
    }
}

val migration_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Perform migration steps here (e.g., CREATE TABLE, ALTER TABLE)
        // Rename the new table to the original table name
        database.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY NOT NULL, " +
                "username TEXT NOT NULL, passwordHash TEXT NOT NULL, email TEXT NOT NULL)");
    }
}

val migration_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Perform migration steps here (e.g., CREATE TABLE, ALTER TABLE)
        database.execSQL("CREATE TABLE IF NOT EXISTS new_transactions (id INTEGER PRIMARY KEY NOT NULL," +
                "label TEXT NOT NULL," +
                "amount REAL NOT NULL, " +
                "description TEXT, " +
                "transactionDate TEXT NOT NULL)");

        // Migrate data from old_transactions to new_transactions
        database.execSQL("INSERT INTO new_transactions (label, amount, description, transactionDate) SELECT label, amount, description, transactionDate FROM transactions");
        // Drop the old table
        database.execSQL("DROP TABLE IF EXISTS transactions");
        // Rename the new table to the original table name
        database.execSQL("ALTER TABLE new_transactions RENAME TO transactions");
    }
}

val migration_4_5 = object  : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Perform migration steps here (e.g., CREATE TABLE, ALTER TABLE)
        database.execSQL("CREATE TABLE new_users (id TEXT PRIMARY KEY NOT NULL, " +
                "username TEXT NOT NULL, passwordHash TEXT NOT NULL, email TEXT NOT NULL, code TEXT NOT NULL)");
        // Drop the old table
        database.execSQL("DROP TABLE IF EXISTS users");
        // Rename the new table to the original table name
        database.execSQL("ALTER TABLE new_users RENAME TO users");
        database.execSQL("CREATE TABLE IF NOT EXISTS new_transactions (id INTEGER PRIMARY KEY NOT NULL," +
                "label TEXT NOT NULL," +
                "amount REAL NOT NULL, " +
                "description TEXT, " +
                "transactionDate TEXT NOT NULL," +
                "userId TEXT NOT NULL," +
                "code TEXT NOT NULL," +
                "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        // Drop the old table
        database.execSQL("DROP TABLE IF EXISTS transactions");
        // Rename the new table to the original table name
        database.execSQL("ALTER TABLE new_transactions RENAME TO transactions");
    }
}

class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }
    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}