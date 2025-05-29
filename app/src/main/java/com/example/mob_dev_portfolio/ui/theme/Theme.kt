package com.example.mob_dev_portfolio.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
//        primary = Color.White,
//        secondary = Color.White,
//        tertiary = Color.White,
//        background = Color.White,

        primary = Color.Black,
        onPrimary = Color.LightGray,
        secondary = Color.DarkGray,
        tertiary = Color.Gray,
        background = SuperDarkGrey,
        onBackground = Color.White,
        error = DarkRed,

        )

private val LightColorScheme =
    lightColorScheme(
//        primary = Color.White,
//        secondary = Color.LightGray,
//        tertiary = Color.Gray,
//        background = Color.White,

        primary = Color.White,
        onPrimary = Color.Black,
        secondary = Color.Gray,
        tertiary = Color.Gray,
        background = Color.LightGray,
        onBackground = Color.Black,
        error = Color.Red,
        /* Other default colors to override
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
         */
    )

@Composable
fun MobdevportfolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
//    val colorScheme =
//        when {
////        Build.VERSION_CODES.S is the version code for Android 12 (Snow Cone)
////        Build.VERSION.SDK_INT is the version code for the current device
//            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//                val context = LocalContext.current
//                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//            }
//
//            darkTheme -> DarkColorScheme
//            else -> LightColorScheme
//        }


    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme;


//    Sets the status bar/navigation bar color to the background color of the app
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
    }

//    if (darkTheme) {
//        colorScheme = DarkColorScheme
//    } else {
//        colorScheme = LightColorScheme
//    }

    Log.d("ThemeDebug", "colorScheme: $colorScheme")
    Log.d("ThemeDebug", "Typography: $Typography")
    Log.d("ThemeDebug", "content: $content")

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
