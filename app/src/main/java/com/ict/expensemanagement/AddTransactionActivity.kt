package com.ict.expensemanagement

import android.app.DatePickerDialog
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.widget.addTextChangedListener
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ict.expensemanagement.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddTransactionActivity : AppCompatActivity() {
    private  lateinit var binding: ActivityAddTransactionBinding
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)

        val labelInput = binding.labelInput
        val amountInput = binding.amountInput
        val labelLayout = binding.labelLayout
        val amountLayout = binding.amountLayout
        val descriptionInput = binding.descriptionInput
        val dateInput = binding.dateInput

        dateInput.setOnClickListener {
            showDatePickerDialog()
        }
        Log.d("app", userId!!)

        labelInput.addTextChangedListener {
            if (it!!.isNotEmpty())
                labelLayout.error = null
        }

        amountInput.addTextChangedListener {
            if (it!!.isNotEmpty())
                amountLayout.error = null
        }

        val addTransactionBtn = binding.addTransactionBtn
        addTransactionBtn.setOnClickListener {
            val label = labelInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val description = descriptionInput.text.toString()
            val date = dateInput.text.toString()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = LocalDate.parse(date, formatter).toString()

            if (label.isEmpty())
                labelLayout.error = "Please enter a valid label"

            else if (amount == null)
                amountLayout.error = "Please enter a valid amount"

            else {
                val transaction = Transaction(0, label, amount, description, localDate, userId!!, "")
                transaction.setCode()
                insert(transaction)
            }
        }
        val closeBtn = binding.closeBtn
        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dateInput = binding.dateInput

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                var selectedDate : String

                if (month < 10 && dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-0$dayOfMonth"
                else if (month < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else if (dayOfMonth < 10)
                    selectedDate = "$year-0${month + 1}-$dayOfMonth"
                else
                    selectedDate = "$year-${month + 1}-$dayOfMonth"

                dateInput.setText(selectedDate)
            },
            year,
            month,
            dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun insert(transaction: Transaction) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val transRef = firebaseDatabase.getReference("transactions")
        val db = Room.databaseBuilder(this,
            AppDatabase::class.java,
            "transactions")
            .addMigrations(migration_1_2)
            .addMigrations(migration_2_3)
            .addMigrations(migration_3_4)
            .addMigrations(migration_4_5)
            .build()

        GlobalScope.launch {
            db.transactionDao().insertAll(transaction)
            val newTran = db.transactionDao().getTranWithLargestId()
            val tranMap = hashMapOf(
                "amount" to transaction.amount,
                "description" to transaction.description,
                "code" to transaction.code,
                "label" to transaction.label,
                "transactionDate" to transaction.transactionDate,
                "userId" to transaction.userId
            )
            transRef.child(newTran[0].id.toString()).setValue(tranMap)
            finish()
        }
    }
}