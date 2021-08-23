package xyz.fcr.sberrunner.presentation.view.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import xyz.fcr.sberrunner.R
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import xyz.fcr.sberrunner.databinding.ActivityMainBinding
import xyz.fcr.sberrunner.presentation.view.fragments.main_fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import xyz.fcr.sberrunner.utils.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import xyz.fcr.sberrunner.utils.Constants.TAG_HOME
import xyz.fcr.sberrunner.utils.Constants.TAG_MAP
import xyz.fcr.sberrunner.utils.Constants.TAG_RUN
import xyz.fcr.sberrunner.utils.Constants.TAG_SETTINGS
import xyz.fcr.sberrunner.utils.Constants.TAG_YOU

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        PreferenceManager.setDefaultValues(
            this,
            R.xml.settings_preference,
            false
        )

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            openScreen(HomeFragment(), TAG_HOME)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> openScreen(HomeFragment(), TAG_HOME)
                R.id.nav_map -> openScreen(MapFragment(), TAG_MAP)
                R.id.nav_you -> openScreen(YouFragment(), TAG_YOU)
                R.id.nav_settings -> openScreen(SettingsFragment(), TAG_SETTINGS)
            }
            true
        }

        binding.fabAction.setOnClickListener {
            openScreen(RunFragment(), TAG_RUN)
            binding.bottomNavigationView.uncheckAllItems()
        }
    }

    private fun BottomNavigationView.uncheckAllItems() {
        menu.setGroupCheckable(0, true, false)

        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = false
        }

        menu.setGroupCheckable(0, true, true)
    }

    private fun openScreen(fragmentToOpen: Fragment, tag: String) {
        if (currentFragment?.tag == tag) return
        currentFragment = fragmentToOpen

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, fragmentToOpen, tag)
            .commit()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            openScreen(RunFragment(), TAG_RUN)
            binding.bottomNavigationView.uncheckAllItems()
        }
    }
}