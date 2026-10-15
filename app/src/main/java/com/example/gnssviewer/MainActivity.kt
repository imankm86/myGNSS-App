package com.example.gnssviewer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.gnssviewer.ui.GnssScreen
import com.example.gnssviewer.ui.GnssViewModel
import com.example.gnssviewer.ui.theme.GNSSViewerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GnssViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GNSSViewerTheme {
                GnssScreen(
                    viewModel = viewModel,
                    onRequestPermission = {
                        requestGnssPermissions()
                    }
                )
            }
        }

        // Request permissions on startup if not already granted
        requestGnssPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermission()
    }

    private fun requestGnssPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
