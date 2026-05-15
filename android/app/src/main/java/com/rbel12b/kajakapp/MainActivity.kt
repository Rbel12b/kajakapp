package com.rbel12b.kajakapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rbel12b.kajakapp.ui.nav.AppNavigation
import com.rbel12b.kajakapp.ui.theme.Kajakapp2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KajakApplication
        setContent {
            Kajakapp2Theme(darkTheme = true) {
                AppNavigation(app)
            }
        }
    }
}
