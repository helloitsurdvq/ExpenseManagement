package com.ict.expensemanagement

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.io.Serializable

@Entity(tableName = "transactions", foreignKeys = [ForeignKey(
    entity = User::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("userId"),
    onDelete = ForeignKey.CASCADE
)])
data class Transaction(
    @PrimaryKey (autoGenerate = true) val id: Int,
    val label: String,
    val amount: Double,
    val description: String?=null,
    val transactionDate: String,
    val userId: String,
    var code: String
) : Serializable {
    constructor() : this(-1, "", 0.0, "", "", "", "") {

    }

    fun setCode(){
        this.code =  "${this.label},${this.amount},${this.description},${this.transactionDate}"
    }
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String,
    val email: String,
    var code: String
) : Serializable {
    constructor() : this("", "", "", "", "") {

    }
    fun setCode(){
        this.code =  "${this.username},${this.passwordHash},${this.email}"
    }
}

data class UserAndTransactions(
    @Embedded
    val user: User,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val transactions: List<Transaction>
) : Serializable {

}

