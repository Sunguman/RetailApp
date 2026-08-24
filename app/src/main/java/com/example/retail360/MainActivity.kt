package com.example.retail360

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.retail360.navigation.NavGraph
import com.example.retail360.navigation.Screen
import com.example.retail360.ui.theme.Retail360Theme
import com.example.retail360.ui.theme.White
import com.example.retail360.util.Graph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Graph.provide(this)
        setContent {
            Retail360Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = White
                ) {
                    val navController = rememberNavController()
                    // Splash decides where to go (Dashboard if a Firebase session
                    // exists, else Auth) and warms the catalog while it's shown.
                    NavGraph(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    )
                }
            }
        }
    }
}
