package com.example.mob_dev_portfolio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MobdevportfolioTheme {
                LandingPage()
            }
        }
    }
}

@Composable
private fun Title() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.homepageTitle),
            textAlign = TextAlign.Center,
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
            Modifier
                .padding(top = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
fun LandingPage() {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Title()
        },
        bottomBar = {
            BottomBar()
        },
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues), // ✅ Ensures no overlap with top bar
//                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            ButtonSection() // ✅ Calling the extracted button section
        }
    }
}

@Composable
fun LoadGameButton() {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { context.startActivity(Intent(context, LoadGame::class.java)) },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
        modifier = Modifier.padding(16.dp),

        ) {
        Text(text = stringResource(id = R.string.load_game))
    }
}

@Composable
private fun BottomBar() {
    Row(
        horizontalArrangement = Arrangement.Start,
    ) {
        LoadGameButton()
    }
}

@Composable
fun MainMenuButton(stringId: Int, onClick: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.size(width = screenWidth * 0.7f, height = 100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = stringId),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun ButtonSection() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly, // ✅ Consistent spacing
    ) {
//        MainMenuButton(R.string.playOnline) { }

        MainMenuButton(R.string.play_over_bluetooth) {
            context.startActivity(Intent(context, JoinOrHost::class.java))
        }

        MainMenuButton(R.string.playLocally) {
            context.startActivity(Intent(context, PlayLocallyInitialChoices::class.java))
        }
    }

}
