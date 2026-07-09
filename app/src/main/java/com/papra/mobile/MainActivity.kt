package com.papra.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.papra.mobile.ui.navigation.PapraNavGraph
import com.papra.mobile.ui.theme.PapraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PapraApp

        setContent {
            PapraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PapraNavGraph(app = app)
                }
            }
        }
    }
}
