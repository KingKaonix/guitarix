package com.kaonixx.guitarix

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaonixx.guitarix.ui.MainScreen

@Composable
fun KrankApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    if (!LocalView.current.isInEditMode) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            window.statusBarColor = Color(0xFF0A0A0E).toArgb()
            window.navigationBarColor = Color(0xFF0A0A0E).toArgb()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0E)) {
        MainScreen(vm = viewModel)
    }
}
