package com.example.mob_dev_portfolio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.LocalDateTime


enum class GameState {
    IN_GAME,
    EXITING,
    SAVING
}

class PlayLocally : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)


        setContent {
            val highlightedSquares = remember { mutableStateOf<Set<Square>>(emptySet()) }
            val selectedSquare = remember { mutableStateOf<Square?>(null) }
            val gameOverData = remember { mutableStateOf(gameOverData()) }
            val promotionData = remember { mutableStateOf(PromotionData()) }

            val mainColor = intent.getStringExtra("color")
            val oppositionColor =
                when (intent.getStringExtra("opponentColor")) {
                    "WHITE" -> Side.WHITE
                    "BLACK" -> Side.BLACK
                    null -> when (mainColor) {
                        "WHITE" -> Side.BLACK
                        "BLACK" -> Side.WHITE
                        "RANDOM" -> listOf(Side.BLACK, Side.WHITE).random()
                        else -> throw IllegalArgumentException("Invalid color (mainColor): ${mainColor}")
                    }

                    else -> throw IllegalArgumentException(
                        "Invalid color (opponentColor): ${
                            intent.getStringExtra(
                                "opponentColor"
                            )
                        }"
                    )
                }


            val timeControlMain = intent.getIntExtra("timeControlMain", 300)
            val increment = intent.getIntExtra("increment", 0)

            val mainPlayerTime = remember {
                mutableIntStateOf(
                    intent.getIntExtra("mainPlayerTime", timeControlMain)
                )
            }
            val opponentPlayerTime = remember {
                mutableIntStateOf(
                    intent.getIntExtra("opponentPlayerTime", timeControlMain)
                )
            }

            val gameState = remember { mutableStateOf(GameState.IN_GAME) }
            val whiteResignData = remember { mutableStateOf(ResignData(Side.WHITE)) }
            val blackResignData = remember { mutableStateOf(ResignData(Side.BLACK)) }
            val offeredDrawData = remember { mutableStateOf(OfferedDrawData()) }


//            TODO: gpt ass code, please verify
//            I mean it works? but still check if its good
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    gameState.value = GameState.EXITING
                }
            }
            )

            MobdevportfolioTheme {
                Log.d("PlayLocally", "PlayLocally")
                val board = remember {
                    mutableStateOf(Board().apply {
                        intent.getStringExtra("boardFEN")?.let { loadFromFen(it) }
                    })
                }



                Scaffold(
                    topBar = {
                        TopBar(
                            soundManager,
                            gameState
                        )
                    },
                    bottomBar = { BottomBar(gameState) },
                ) { contentPadding ->
                    ChessScreen(
                        LocalContext.current,
                        timeControlMain,
                        increment,
                        contentPadding,
                        soundManager,
                        board,
                        highlightedSquares.value,
                        oppositionColor,
                        mainPlayerTime,
                        opponentPlayerTime,
                        promotionData,
                        gameOverData,
                        gameState,
                        whiteResignData,
                        blackResignData,
                        offeredDrawData,
                    ) { square ->
                        handleBoardClick(
                            selectedSquare,
                            highlightedSquares,
                            mainPlayerTime,
                            opponentPlayerTime,
                            oppositionColor,
                            increment,
                            promotionData,
                            gameOverData,
                            board,
                            square,
                            soundManager
                        )
                    }
                }

            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        soundManager.releaseAll()
    }
}



