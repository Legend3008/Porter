package com.porter.shipper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.porter.core.designsystem.theme.PorterTheme
import com.porter.shipper.navigation.PorterNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PorterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
                    ) {
                        PorterNavGraph()
                    }
                }
            }
        }
    }
}
