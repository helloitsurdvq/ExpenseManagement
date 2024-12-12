package com.ict.expensemanagement

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ict.expensemanagement.databinding.ActivityHomeBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var deletedTransaction : Transaction
    private lateinit var transactions: List<Transaction>
    private  lateinit var oldTransactions: List<Transaction>
    private lateinit var  transactionAdapter: TransactionAdapter
    private  lateinit var  linearLayoutManager: LinearLayoutManager
    private lateinit var db: AppDatabase
    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid
    private val activity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)
        val bottomNavigationView = binding.bottomNavView
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.item_static -> {
                    startActivity(Intent(this, StaticActivity::class.java))
                    true
                }
                R.id.item_profile -> {
                    GlobalScope.launch {
                        val intent = Intent(activity, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }
        db = Room.databaseBuilder(this,
            AppDatabase::class.java,
            "transactions")
            .addMigrations(migration_1_2)
            .addMigrations(migration_2_3)
            .addMigrations(migration_3_4)
            .addMigrations(migration_4_5)
            .build()

        val recyclerView = binding.recyclerview
        recyclerView.adapter = transactionAdapter
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTransaction(transactions[viewHolder.adapterPosition])
            }
        }
        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recyclerView)
        val addBtn = binding.addBtn
        addBtn.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        GlobalScope.launch {
            syncUsers()
            syncTrans()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchAll() {
        GlobalScope.launch {
            transactions = db.transactionDao().getTransByUserId(userId!!)

            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAll()
    }

    @SuppressLint("SetTextI18n")
    private fun updateDashboard(){
        val totalAmount = transactions.map { it.amount }.sum()
        val budgetAmount = transactions.filter{ it.amount > 0 }.map{ it.amount }.sum()
        val expenseAmount = totalAmount - budgetAmount
        val balance = binding.balance
        val budget = binding.budget
        val expense = binding.expense
        balance.text = "${"%,.0f".format(Locale.US, totalAmount)} VND"
        budget.text = "${"%,.0f".format(Locale.US, budgetAmount)} VND"
        expense.text = "${"%,.0f".format(Locale.US, expenseAmount)} VND"
    }

    private fun undoDelete() {
        GlobalScope.launch {
            db.transactionDao().insertAll(deletedTransaction)
            val databaseReference = FirebaseDatabase.getInstance().getReference("transactions/${deletedTransaction.id}")
            databaseReference.setValue(deletedTransaction).addOnSuccessListener {

            }.addOnFailureListener {

            }

            transactions = oldTransactions

            runOnUiThread {
                transactionAdapter.setData(transactions)
                updateDashboard()
            }
        }
    }

    private fun showSnackbar() {
        val view : View = binding.coordinator
        val snackbar : Snackbar = Snackbar.make(view, "Transaction deleted!", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            undoDelete()
        }
            .setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        deletedTransaction = transaction
        oldTransactions = transactions

        GlobalScope.launch {
            db.transactionDao().delete(transaction)

            val databaseReference = FirebaseDatabase.getInstance().getReference("transactions/${transaction.id}")
            databaseReference.removeValue().addOnSuccessListener {

            }.addOnFailureListener {

            }
            transactions = transactions.filter {it.id != transaction.id}
            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
                showSnackbar()
            }
        }
    }

    fun syncTrans() {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val transRef = firebaseDatabase.getReference("transactions")
        val transFromServer: MutableList<Transaction> = mutableListOf()
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    val data = child.getValue(Transaction::class.java)
                    if (data != null) {
                        transFromServer.add(data)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        transRef.addValueEventListener(valueEventListener)
        val transFromLocal = db.transactionDao().getAll()
        val iterator = transFromServer.iterator()

        while (iterator.hasNext()) {
            val tranFromServer = iterator.next()
            val codeServer = tranFromServer.code
            val tranFromLocal = transFromLocal.find { it.id == tranFromServer.id }
            val serverAttr = codeServer.split(",")
            val serverLabel = serverAttr[0]
            val serverAmount = serverAttr[1]
            val serverDescription = serverAttr[2]
            val serverTransactionDate = serverAttr[3]

            if (tranFromLocal != null ) {
                if (codeServer != tranFromLocal.code) {
                    val localAttr = tranFromLocal.code.split(",")
                    var localLabel = localAttr[0]
                    var localAmount = localAttr[1]
                    var localDescription = localAttr[2]
                    var localTransactionDate = localAttr[3]

                    if (localLabel != serverLabel) {
                        localLabel = serverLabel
                    } else if (localAmount != serverAmount) {
                        localAmount = serverAmount
                    } else if (localDescription != serverDescription) {
                        localDescription = serverDescription
                    } else if (localTransactionDate != serverTransactionDate){
                        localTransactionDate = serverTransactionDate
                    }
                    var updateTran = Transaction(tranFromLocal.id, localLabel, localAmount.toDouble(), localDescription, localTransactionDate, userId!!, tranFromLocal.code)
                    updateTran.setCode()
                    db.transactionDao().update(updateTran)
                }

            } else {
                val transaction = Transaction(tranFromServer.id, tranFromServer.label, tranFromServer.amount, tranFromServer.description, tranFromServer
                    .transactionDate, tranFromServer.userId, tranFromServer.code)
                db.transactionDao().insertAll(transaction)
            }
        }
    }

    fun syncUsers() {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val usersRef = firebaseDatabase.getReference("users")
        val usersFromServer: MutableList<User> = mutableListOf()
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    val data = child.getValue(User::class.java)
                    if (data != null) {
                        usersFromServer.add(data)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        usersRef.addValueEventListener(valueEventListener)
        val usersFromLocal = db.userDao().getAll()

        usersFromServer.forEach { userFromServer ->
            val codeServer = userFromServer.code
            val userFromLocal = usersFromLocal.find { it.id == userFromServer.id }
            val serverAttr = codeServer.split(",")
            val serverUserName = serverAttr[0]
            val serverPassword = serverAttr[1]
            val serverEmail = serverAttr[2]

            if (userFromLocal != null) {
                if (codeServer != userFromLocal.code) {
                    val localAttr = userFromLocal.code.split(",")
                    var localUserName = localAttr[0]
                    var localPassword = localAttr[1]
                    var localEmail = localAttr[2]

                    if (localUserName != serverUserName) {
                        localUserName = serverUserName
                    } else if (localPassword != serverPassword) {
                        localPassword= serverPassword
                    } else if (localEmail != serverEmail) {
                        localEmail = serverEmail
                    }
                    var updateUser = User(userFromLocal.id, localUserName, localPassword, localEmail, userFromLocal.code)
                    updateUser.setCode()
                    db.userDao().update(updateUser)
                }
            } else {
                val user = User(userFromServer.id, userFromServer.username, userFromServer.passwordHash, userFromServer.email, userFromServer.code)
                db.userDao().insertAll(user)
            }
        }
    }
}