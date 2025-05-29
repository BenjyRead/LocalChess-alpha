package com.example.mob_dev_portfolio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme


//TODO: refactor initial choices into own file

class PlayLocallyInitialChoices : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobdevportfolioTheme {
                val pieceColor = remember { mutableStateOf<ColorChoice?>(null) }
                val timeControlSelected = remember { mutableStateOf<TimeControl?>(null) }
                val timeControlMainInSeconds = remember { mutableStateOf(300) }
                val incrementInSeconds = remember { mutableStateOf(0) }
                val error = remember { mutableStateOf<Error?>(null) }
                InitialChoices(
                    pieceColor,
                    timeControlSelected,
                    timeControlMainInSeconds,
                    incrementInSeconds,
                    error,
                    PlayLocally::class.java
                )
            }
        }
    }
}


