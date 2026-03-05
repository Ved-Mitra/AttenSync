package com.example.attensync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.attensync.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // --- 1. THE GOOGLE AUTH VARIABLES ---
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    // --- 2. THE LOGIN LAUNCHER (Must be outside onCreate to prevent crashes) ---
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNavigationHeader() {
        val navigationView: NavigationView = binding.navView

        // Safeguard to prevent crashes if the header isn't ready
        if (navigationView.headerCount > 0) {
            val headerView = navigationView.getHeaderView(0)

            val navName: TextView? = headerView.findViewById(R.id.nav_header_name)
            val navEmail: TextView? = headerView.findViewById(R.id.nav_header_email)

            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                navName?.text = currentUser.displayName ?: "AttenSync User"
                navEmail?.text = currentUser.email ?: "No email provided"

                // Find the image view (your nav_header_main.xml uses the ID 'imageView')
                val navImage: ImageView? = headerView.findViewById(R.id.imageView)

                // Tell Glide to load the Google photo and make it a perfect circle!
                if (navImage != null && currentUser.photoUrl != null) {
                    Glide.with(this)
                        .load(currentUser.photoUrl)
                        .circleCrop()
                        .into(navImage)
                }
            } else {
                navName?.text = getString(R.string.nav_guest_name)
                navEmail?.text = getString(R.string.nav_guest_email)
            }
        }
    }

    // --- 5. THE FIREBASE LINKING LOGIC ---
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Welcome, ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    updateNavigationHeader() // Update the side menu instantly!
                } else {
                    Toast.makeText(this, "Firebase Auth Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications are disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            updateNavigationHeader()
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.google.firebase.FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        // --- 3. INITIALIZE GOOGLE AND FIREBASE ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    as androidx.navigation.fragment.NavHostFragment

        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard,
                R.id.nav_calender,
                R.id.nav_discussion,
                R.id.nav_notification,
                R.id.nav_report,
                R.id.nav_leaderboard,
                R.id.nav_redeem
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_sign_out) {
                handleSignOut()
                drawerLayout.closeDrawers()
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(item, navController)
                if (handled) {
                    drawerLayout.closeDrawers()
                }
                handled
            }
        }

        if (auth.currentUser == null) {
            if (savedInstanceState == null) {
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        } else {
            updateNavigationHeader()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    as androidx.navigation.fragment.NavHostFragment

        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}