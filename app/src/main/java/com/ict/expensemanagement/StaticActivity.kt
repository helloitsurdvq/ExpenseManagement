package com.ict.expensemanagement

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ict.expensemanagement.databinding.ActivityStaticBinding

class StaticActivity : AppCompatActivity() {
    private lateinit var binding : ActivityStaticBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaticBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val bottomNavigationView = binding.bottomNavView
        bottomNavigationView.selectedItemId = com.ict.expensemanagement.R.id.item_static

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                com.ict.expensemanagement.R.id.item_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                com.ict.expensemanagement.R.id.item_static -> {
                    menuItem.setChecked(true)
                    startActivity(Intent(this, StaticActivity::class.java))
                    true
                }
                com.ict.expensemanagement.R.id.item_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        val adapter = ViewPagerAdapter(supportFragmentManager)

        // Add your fragments to the adapter
        adapter.addFragment(DayFragment(), "By date")
        adapter.addFragment(WeekFragment(), "By week")
        adapter.addFragment(MonthFragment(), "By month")

        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }
}