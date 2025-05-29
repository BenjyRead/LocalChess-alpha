package com.example.mob_dev_portfolio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
    Typography(
        bodyLarge =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        labelMedium =
        TextStyle(
            fontSize = 24.sp,
            shadow = Shadow(
                color = Color.White,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
//        NOTE: im just using headline as meaning error message, kinda like how news headlines are red
        headlineMedium =
        TextStyle(
            fontSize = 24.sp,
            shadow = Shadow(
                color = SuperDarkRed,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
        labelSmall = TextStyle(
            fontSize = 18.sp,
            shadow = Shadow(
                color = Color.White,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
        displaySmall = TextStyle(
            fontSize = 16.sp,
            shadow = Shadow(
                color = Color.White,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
        headlineSmall = TextStyle(
            fontSize = 18.sp,
            shadow = Shadow(
                color = SuperDarkRed,
                offset = Offset(2f, 2f),
                blurRadius = 2f
            )
        ),
        /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
     */
    )
