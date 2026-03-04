package com.example.attensync

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.attensync.ui.calender.Assignment
import com.example.attensync.ui.calender.CalendarScreen
import com.example.attensync.ui.calender.ClassroomService
import com.example.attensync.ui.theme.AttenSyncTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var googleSignInClient: GoogleSignInClient
    private var assignments: List<Assignment> = emptyList()

    private val signInLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("SIGN_IN", "Signed in: ${account.email}")
                updateNavHeader(account.displayName, account.email)
                fetchAssignmentsAndNavigate()
            } catch (e: ApiException) {
                Log.e("SIGN_IN", "Sign-in failed", e)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/classroom.courses.readonly"),
                Scope("https://www.googleapis.com/auth/classroom.coursework.me.readonly")
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            updateNavHeader(account.displayName, account.email)
            fetchAssignmentsAndNavigate()
        } else {
            showLoginScreen()
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_sign_out -> {
                    signOut()
                }
                else -> {
                    val screenName = when (menuItem.itemId) {
                        R.id.nav_calendar -> "Calendar"
                        R.id.nav_dashboard -> "Dashboard"
                        R.id.nav_discussion -> "Discussion"
                        R.id.nav_notifications -> "Notifications"
                        R.id.nav_report -> "Report"
                        R.id.nav_leaderboard -> "Leaderboard"
                        R.id.nav_store -> "Redeem Store"
                        else -> "AttenSync"
                    }
                    showComposeScreen(screenName)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showLoginScreen() {
        val loginLayout = findViewById<View>(R.id.loginLayout)
        val mainContent = findViewById<View>(R.id.mainContent)
        loginLayout.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        val signInBtn = findViewById<Button>(R.id.googleSignInBtn)
        signInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            googleSignInClient.revokeAccess().addOnCompleteListener(this) {
                // Return to login screen
                assignments = emptyList()
                showLoginScreen()
                updateNavHeader("User Name", "user@example.com")
            }
        }
    }

    private fun updateNavHeader(name: String?, email: String?) {
        val headerView = navView.getHeaderView(0)
        val nameTextView = headerView.findViewById<TextView>(R.id.user_name)
        val emailTextView = headerView.findViewById<TextView>(R.id.user_email)
        val avatarTextView = headerView.findViewById<TextView>(R.id.user_avatar)

        nameTextView.text = name ?: "User Name"
        emailTextView.text = email ?: "user@example.com"
        avatarTextView.text = name?.firstOrNull()?.toString()?.uppercase() ?: "V"
    }

    private fun fetchAssignmentsAndNavigate() {
        lifecycleScope.launch {
            val service = ClassroomService(this@MainActivity)
            assignments = service.fetchAssignments()
            navigateToMainContent()
        }
    }

    private fun navigateToMainContent() {
        val loginLayout = findViewById<View>(R.id.loginLayout)
        val mainContent = findViewById<View>(R.id.mainContent)
        
        loginLayout.visibility = View.GONE
        mainContent.visibility = View.VISIBLE

        showComposeScreen("Calendar")
    }

    private fun showComposeScreen(screenName: String) {
        title = screenName
        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setContent {
            AttenSyncTheme {
                if (screenName == "Calendar") {
                    CalendarScreen(assignments = assignments)
                } else {
                    ScreenContent(screenName)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
fun ScreenContent(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name Screen",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
