package com.pyllar.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pyllar.consumer.platform.AndroidPermissionManager
import org.koin.android.ext.android.get
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {

    // Launchers must be registered before onCreate completes
    private val notifPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        get<AndroidPermissionManager>().onNotificationResult(granted)
    }
    private val locationPermissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
        get<AndroidPermissionManager>().onLocationResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Pass launchers to the permission manager so it can request permissions from the ViewModel
        get<AndroidPermissionManager>().setLaunchers(notifPermissionLauncher, locationPermissionLauncher)

        setContent {
            KoinContext {
                App()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}