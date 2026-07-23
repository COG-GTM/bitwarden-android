package com.bitwarden.demoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bitwarden.demoapp.ui.generator.GeneratorScreen
import com.bitwarden.ui.platform.theme.BitwardenTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Primary entry point for the Password Playground demo app.
 *
 * Hosts the single [GeneratorScreen] inside the shared [BitwardenTheme].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BitwardenTheme {
                GeneratorScreen()
            }
        }
    }
}
