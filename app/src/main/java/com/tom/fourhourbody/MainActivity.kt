package com.tom.fourhourbody

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tom.fourhourbody.ui.AppRoot
import com.tom.fourhourbody.ui.theme.FourHourBodyTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val startRoute = intent?.getStringExtra(EXTRA_ROUTE)

        setContent {
            // A reminder tap carries the route it belongs to; consumed once, then cleared so a
            // configuration change does not bounce the user back to it.
            var pendingRoute by remember { mutableStateOf(startRoute) }

            FourHourBodyTheme {
                AppRoot(
                    pendingRoute = pendingRoute,
                    onRouteConsumed = { pendingRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
