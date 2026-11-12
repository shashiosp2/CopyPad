package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.CopyPadScreen
import com.example.ui.theme.CopyPadTheme
import com.example.viewmodel.CopyPadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CopyPadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CopyPadTheme {
                CopyPadScreen(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Ensure clipboard copy triggers when user leaves via Home, App Switcher, or Back
        viewModel.handleAppExit(this)
    }

    override fun onStop() {
        super.onStop()
        // Ensure clipboard copy triggers when activity is stopped/backgrounded
        viewModel.handleAppExit(this)
    }
}
