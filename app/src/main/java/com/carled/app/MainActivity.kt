package com.carled.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import com.carled.app.presentation.navigation.AppNavigation
import com.carled.app.presentation.theme.CarLedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(253, 253, 253)
        window.navigationBarColor = if (android.os.Build.VERSION.SDK_INT >= 27) android.graphics.Color.rgb(253, 253, 253) else android.graphics.Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        // Bluetooth permissions are requested after the scan screen explains them.
        setContent { CarLedTheme { AppNavigation() } }
    }
}
