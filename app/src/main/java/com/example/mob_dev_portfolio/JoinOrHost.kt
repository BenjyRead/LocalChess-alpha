package com.example.mob_dev_portfolio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme

class JoinOrHost : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermissions(this)
        setContent {
            MobdevportfolioTheme {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Title()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    HostButton()
                    JoinButton()
                }

            }
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResource(id = R.string.join_or_host),
        textAlign = TextAlign.Center,
        fontSize = 40.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier =
        Modifier
            .padding(top = 16.dp, bottom = 16.dp),
    )
}


@Composable
fun JoinButton() {
    val context = LocalContext.current
//    TODO: a little janky, maybe refactor
    MainMenuButton(R.string.join) {
        val intent = Intent(context, JoinBluetoothGame::class.java)
        context.startActivity(intent)
    }
}

@Composable
fun HostButton() {
    val context = LocalContext.current
//    TODO: here too
    MainMenuButton(R.string.host) {
        val intent = Intent(context, HostBluetoothGame::class.java)
        context.startActivity(intent)
    }
}