package com.airemore.fieldapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.airemore.fieldapp.ui.nav.AppNavHost
import com.airemore.fieldapp.ui.theme.AiremoreFieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AiremoreApp
        setContent {
            AiremoreFieldTheme {
                AppNavHost(app = app)
            }
        }
    }
}
