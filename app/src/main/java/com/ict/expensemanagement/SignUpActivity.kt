package com.ict.expensemanagement

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ict.expensemanagement.databinding.ActivitySignUpBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private  lateinit var db: AppDatabase
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    val usersRef = firebaseDatabase.getReference("users")
    private lateinit var activity: Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        db = Room.databaseBuilder(this,
            AppDatabase::class.java,
            "transactions")
            .addMigrations(migration_1_2)
            .addMigrations(migration_2_3)
            .addMigrations(migration_3_4)
            .addMigrations(migration_4_5)
            .build()
        activity = this

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.button.setOnClickListener {
            val username = binding.userNameEt.text.toString()
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass.equals(confirmPass)) {
                    GlobalScope.launch {
                        if (db.userDao().checkUsernameExists(username) > 0) {
                            Toast.makeText(activity, "Username is already exist", Toast.LENGTH_SHORT).show()
                        } else {
                            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener{
                                if (it.isSuccessful) {
                                    val authUser = auth.currentUser
                                    val uid = authUser!!.uid
                                    val user = User(uid, username, pass, email, "")
                                    user.setCode()
                                    insert(user)
                                    val userMap = hashMapOf(
                                        "username" to username,
                                        "email" to email,
                                        "code" to user.code,
                                        "passwordHash" to pass,
                                        "id" to uid
                                    )
                                    usersRef.child(uid).setValue(userMap).addOnCompleteListener { task ->
                                        if (!task.isSuccessful)  {
                                            Toast.makeText(activity, "Error when save user in server, but saved in local", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    val intent = Intent(activity, SignInActivity::class.java)
                                    startActivity(intent)

                                } else {
                                    Toast.makeText(activity, it.exception.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                } else {
                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Empty Fields Are Not Allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun insert(user: User) {
        GlobalScope.launch {
            db.userDao().insertAll(user)
            finish()
        }
    }
}