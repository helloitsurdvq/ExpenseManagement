package com.ict.expensemanagement

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.ict.expensemanagement.databinding.ActivityProfileBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var db: AppDatabase
    private lateinit var user: User
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val usernameLayout = binding.profileUsername
        val emailLayout = binding.profileMail
        val moneyLayout = binding.profileMoney
        val logoutBtn = binding.logoutBtn
        val activity = this
        val bottomNavigationView = binding.bottomNavView

        bottomNavigationView.selectedItemId = R.id.item_profile

        GlobalScope.launch {
            db = Room.databaseBuilder(activity,
                AppDatabase::class.java,
                "transactions")
                .addMigrations(migration_1_2)
                .addMigrations(migration_2_3)
                .addMigrations(migration_3_4)
                .addMigrations(migration_4_5)
                .build()

            user = db.userDao().getUserById(userId!!)
            val money = db.userDao().getMoneyByUserId(userId!!)

            runOnUiThread {
                usernameLayout.text = user.username
                emailLayout.text = user.email
                moneyLayout.text = "${"%, .0f".format(Locale.US, money)} VND"
            }
        }

        logoutBtn.setOnClickListener { onLogoutButtonClick(it) }

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
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    fun onLogoutButtonClick(view: View) {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}