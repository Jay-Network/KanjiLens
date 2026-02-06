package com.jworks.kanjilens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jworks.kanjilens.ui.camera.CameraScreen
import com.jworks.kanjilens.ui.settings.SettingsScreen
import com.jworks.kanjilens.ui.theme.KanjiLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanjiLensTheme {
                var showSettings by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    if (showSettings) {
                        SettingsScreen(onBackClick = { showSettings = false })
                    } else {
                        CameraScreen(onSettingsClick = { showSettings = true })
                    }
                }
            }
        }
    }
}
