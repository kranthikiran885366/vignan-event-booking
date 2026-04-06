package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.fragment.BookingsFragment
import com.example.myapplication.ui.fragment.HomeFragment
import com.example.myapplication.ui.fragment.ProfileFragment
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    val userName get() = intent.getStringExtra(EXTRA_USER_NAME)
        ?: auth.currentUser?.displayName
        ?: if (auth.currentUser?.isAnonymous == true) "Guest" else "User"

    val userEmail get() = intent.getStringExtra(EXTRA_USER_EMAIL)
        ?: auth.currentUser?.email
        ?: if (auth.currentUser?.isAnonymous == true) "anonymous@firebase.local" else "unknown@email"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = getString(R.string.title_equipment_booking)

        // Load home fragment on first launch
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), R.id.nav_home)
        }

        handleInitialNavigation(intent)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { loadFragment(HomeFragment(), item.itemId); true }
                R.id.nav_bookings -> { loadFragment(BookingsFragment(), item.itemId); true }
                R.id.nav_profile  -> { loadFragment(ProfileFragment(), item.itemId); true }
                else -> false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInitialNavigation(intent)
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment, itemId: Int) {
        val title = when (itemId) {
            R.id.nav_home     -> getString(R.string.title_equipment_booking)
            R.id.nav_bookings -> getString(R.string.title_my_bookings)
            R.id.nav_profile  -> getString(R.string.title_profile)
            else              -> getString(R.string.app_name)
        }
        binding.toolbar.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun showSnackbar(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        val sb = com.google.android.material.snackbar.Snackbar
            .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAnchorView(binding.btnAnchor)
        if (actionLabel != null && action != null) sb.setAction(actionLabel) { action() }
        sb.show()
    }

    fun navigateToBookings() {
        binding.bottomNav.selectedItemId = R.id.nav_bookings
    }

    private fun handleInitialNavigation(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_NAVIGATE_TO) == NAV_BOOKINGS) {
            binding.bottomNav.selectedItemId = R.id.nav_bookings
        }
    }

    companion object {
        const val EXTRA_USER_NAME  = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
        const val NAV_BOOKINGS = "nav_bookings"
    }
}
