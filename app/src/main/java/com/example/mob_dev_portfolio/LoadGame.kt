package com.example.mob_dev_portfolio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

class LoadGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this

        setContent {
//            val fileList = remember { fileList() }
            val fileList = remember { mutableStateListOf(*context.fileList()) }
            MobdevportfolioTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopBar()
                    },
                    bottomBar = {
                        BottomBar()
                    }
                ) { paddingValues ->
                    LoadGameScreen(paddingValues, fileList)
                }
            }
        }
    }
}

@Composable
fun DeleteButton(fileName: String, fileList: SnapshotStateList<String>) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            context.deleteFile(fileName)
            fileList.remove(fileName)
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.delete),
            modifier = Modifier
                .padding(4.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
        )
    }

}

@Composable
fun OverTheBoardFile(context: Context, fileName: String, fileList: SnapshotStateList<String>) {

    val dateTimeString = fileName.substringBeforeLast("_")
    val truncated = dateTimeString.substringBefore(".")

    val date: LocalDateTime;
    try {
        date = LocalDateTime.parse(truncated)
    } catch (e: Exception) {
        Log.d("OverTheBoardFile", "Error parsing date: $truncated")
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(
            onClick = {
                val fileInput = context.openFileInput(fileName)
                val json = fileInput.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(json)
//                val gameData = mapOf(
//                    "board" to board,
//                    "mainPlayerTime" to mainPlayerTime,
//                    "opponentPlayerTime" to opponentPlayerTime,
//                    "opponentColor" to opponentColor,
//                    "timeControlMain" to timeControlMain,
//                    "increment" to increment
//                )

                val board = jsonObject.getString("boardFEN")
                val mainPlayerTime = jsonObject.getInt("mainPlayerTime")
                val opponentPlayerTime = jsonObject.getInt("opponentPlayerTime")
                val opponentColor = jsonObject.getString("opponentColor")
                val timeControlMain = jsonObject.getInt("timeControlMain")
                val increment = jsonObject.getInt("increment")

                val intent = Intent(context, PlayLocally::class.java)
                intent.putExtra("boardFEN", board)
                intent.putExtra("mainPlayerTime", mainPlayerTime)
                intent.putExtra("opponentPlayerTime", opponentPlayerTime)
                intent.putExtra("opponentColor", opponentColor)
                intent.putExtra("timeControlMain", timeControlMain)
                intent.putExtra("increment", increment)
                context.startActivity(intent)

            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
        ) {
            Text(
//            TODO: Not using strings.xml might be bad practice
                text = "${date.year}-${date.monthValue}-${date.dayOfMonth} ${date.hour}:${date.minute}",
                modifier = Modifier
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        DeleteButton(fileName, fileList)
    }
}

@Composable
fun LoadOverTheBoardGames(fileList: SnapshotStateList<String>) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.over_the_board_games),
            modifier = Modifier
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
        )

        val overTheBoardFiles = fileList.filter { it.endsWith("_OverTheBoard.json") }
        Log.d("LoadOverTheBoardGames", "overTheBoardFiles: $overTheBoardFiles")

        for (file in overTheBoardFiles) {

            OverTheBoardFile(context, file, fileList);
        }
    }

}

@Composable
fun LoadGameScreen(paddingValues: PaddingValues, fileList: SnapshotStateList<String>) {

    Column(
        modifier = Modifier
            .padding(paddingValues)
    ) {
        LoadOverTheBoardGames(fileList)
    }

}


@Composable
private fun BackButton() {
//    TODO: its white and not gray like the images
    val context = LocalContext.current
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    )
}

@Composable
fun TopBar() {
    Row(

        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(16.dp),
    ) {
        BackButton()

    }
}


@Composable
private fun BottomBar() {
}