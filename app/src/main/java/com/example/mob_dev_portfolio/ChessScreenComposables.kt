package com.example.mob_dev_portfolio

import android.content.Context
import android.content.Intent
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.time.LocalDateTime


//NOTE: unbelievable hacky and slow solution, but it works...
fun getCapturedPieces(board: Board): MutableMap<Piece, Int> {
    val startingCounts = mapOf(
        Piece.WHITE_PAWN to 8,
        Piece.WHITE_KNIGHT to 2,
        Piece.WHITE_BISHOP to 2,
        Piece.WHITE_ROOK to 2,
        Piece.WHITE_QUEEN to 1,
        Piece.WHITE_KING to 1,
        Piece.BLACK_PAWN to 8,
        Piece.BLACK_KNIGHT to 2,
        Piece.BLACK_BISHOP to 2,
        Piece.BLACK_ROOK to 2,
        Piece.BLACK_QUEEN to 1,
        Piece.BLACK_KING to 1,
    )

    val pieceCount = mutableMapOf<Piece, Int>()

    for (a in 0..63) {
        val square = Square.entries[a]
        val piece = board.getPiece(square)
        pieceCount[piece] = (pieceCount[piece] ?: 0) + 1
    }

    val capturedPieces = mutableMapOf<Piece, Int>()

    for (piece in startingCounts.keys) {
        val captured = (startingCounts[piece] ?: 0) - (pieceCount[piece] ?: 0)
        if (captured > 0) {
            capturedPieces[piece] = captured
        }
    }

    return capturedPieces
}

@Composable
fun ChessScreen(
    context: Context,
    timeControlMain: Int,
    increment: Int,
    contentPadding: PaddingValues,
    soundManager: SoundManager,
    board: MutableState<Board>,
    highlightedSquares: Set<Square>,
    opponentColor: Side,
    mainPlayerTime: MutableState<Int>,
    opponentPlayerTime: MutableState<Int>,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    gameState: MutableState<GameState>,
    whiteResignData: MutableState<ResignData>,
    blackResignData: MutableState<ResignData>,
    offeredDrawData: MutableState<OfferedDrawData>,
    onSquareClick: (Square) -> Unit
) {
    Log.d("PlayLocally", "ChessScreen")

    LaunchedEffects(
        mainPlayerTime,
        opponentPlayerTime,
        gameOverData,
        board,
        opponentColor,
        soundManager
    )

    if (mainPlayerTime.value <= 0) {
        gameOverData.value.gameOver = true

        when (opponentColor) {
            Side.WHITE -> gameOverData.value.gameOverMessageId = R.string.White_Wins
            Side.BLACK -> gameOverData.value.gameOverMessageId = R.string.Black_Wins
        }
    } else if (opponentPlayerTime.value <= 0) {
        gameOverData.value.gameOver = true

        when (opponentColor) {
            Side.WHITE -> gameOverData.value.gameOverMessageId = R.string.Black_Wins
            Side.BLACK -> gameOverData.value.gameOverMessageId = R.string.White_Wins
        }
    }

    Box() {

        val blurBoard by animateDpAsState(
            targetValue = if (gameState.value in listOf(
                    GameState.SAVING,
                    GameState.EXITING
                ) || gameOverData.value.gameOver
            ) 5.dp else 0.dp,
            animationSpec = tween(durationMillis = 1000)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .blur(
                    blurBoard
                )
                .clickable { gameState.value = GameState.IN_GAME },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayerRow(
                opponentPlayerTime.value, opponentColor, opponentColor, board.value, if (
                    opponentColor == Side.WHITE
                ) {
                    whiteResignData
                } else {
                    blackResignData
                },
                offeredDrawData
            )

            PlaySection(
                board,
                highlightedSquares,
                opponentColor,
                promotionData,
                gameOverData,
                onSquareClick
            )

            PlayerRow(
                mainPlayerTime.value, when (opponentColor) {
                    Side.WHITE -> Side.BLACK
                    Side.BLACK -> Side.WHITE
                }, opponentColor, board.value,
                if (
                    opponentColor == Side.WHITE
                ) {
                    blackResignData
                } else {
                    whiteResignData
                },
                offeredDrawData
            )
        }

        if (whiteResignData.value.confirmed) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.Black_Wins
        } else if (blackResignData.value.confirmed) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.White_Wins
        }

        if (offeredDrawData.value.blackOffered && offeredDrawData.value.whiteOffered) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.draw_by_agreement
        }

        if (gameOverData.value.gameOver) {
            EndGameDialog(gameOverData.value)
        }
        if (gameState.value == GameState.EXITING) {
            ExitDialog(
                gameState,
                context,
                board.value,
                mainPlayerTime.value,
                opponentPlayerTime.value,
                opponentColor,
                timeControlMain,
                increment
            )
        } else if (gameState.value == GameState.SAVING) {
            SaveDialog(
                gameState,
                context,
                board.value,
                mainPlayerTime.value,
                opponentPlayerTime.value,
                opponentColor,
                timeControlMain,
                increment
            )
        }

    }
}

@Composable
fun TopBar(soundManager: SoundManager, gameState: MutableState<GameState>) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
//        Left side
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            BackButton(gameState)

        }

//        Right side
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            VolumeButton(soundManager)
        }

    }
}

@Composable
private fun BackButton(gameState: MutableState<GameState>) {
//    TODO: its white and not gray like the images
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        modifier = Modifier.clickable { gameState.value = GameState.EXITING }
    )
}

fun turnOffVolume(soundManager: SoundManager) {
    soundManager.muted.value = true
}

fun turnOnVolume(soundManager: SoundManager) {
    soundManager.muted.value = false
}

fun toggleVolume(soundManager: SoundManager) {
    soundManager.toggleMute()
}

@Composable
fun VolumeButton(soundManager: SoundManager) {
    if (!soundManager.muted.value) {
        Image(
            painter = painterResource(id = R.drawable.volume),
            contentDescription = "Volume",
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    turnOffVolume(soundManager)
                }
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.volume_off),
            contentDescription = "Volume",
            modifier = Modifier
                .size(24.dp)
                .clickable {
                    turnOnVolume(soundManager)
                }
        )
    }

}

@Composable
fun BottomBar(gameState: MutableState<GameState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
//        Left side
        Row(
            modifier = Modifier.padding(16.dp)
        ) {

        }
//        Right side
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            SaveButton(gameState)
        }
    }
}

@Composable
fun SaveButton(gameState: MutableState<GameState>) {
    OutlinedButton(
        onClick = { gameState.value = GameState.SAVING },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.save),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun LaunchedEffects(
    mainPlayerTime: MutableState<Int>,
    opponentPlayerTime: MutableState<Int>,
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    oppositionColor: Side,
    soundManager: SoundManager
) {
    LaunchedEffect(Unit) {
        while (!gameOverData.value.gameOver) {
            delay(1000)
            if (board.value.sideToMove != oppositionColor) {
                mainPlayerTime.value -= 1
            } else {
                opponentPlayerTime.value -= 1
            }
        }

    }

    Log.d("PlayLocally", "recomposition isKingAttacked: ${board.value.isKingAttacked}")
    LaunchedEffect(board.value.isKingAttacked) {
        if (board.value.isKingAttacked) {
            soundManager.playCheckWinSound()
        }
        Log.d("PlayLocally", "isKingAttacked: ${board.value.isKingAttacked}")
    }
}

fun handleBoardClick(
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    mainPlayerTime: MutableState<Int>,
    opponentPlayerTime: MutableState<Int>,
    oppositionColor: Side,
    increment: Int,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    square: Square,
    soundManager: SoundManager
) {

    Log.d("PlayLocally", "handleBoardClick")
    updatePromotionPiece(selectedSquare, promotionData, board, square)

    val move = Move(selectedSquare.value, square)

    val isLegalMove =
        move in board.value.legalMoves()
    if (isLegalMove) {
        executeMove(
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
    } else {
        highlightLegalMoves(selectedSquare, highlightedSquares, board, square)
    }

    updateKingCheckHighlight(board.value, highlightedSquares)
}

fun updateKingCheckHighlight(board: Board, highlightedSquares: MutableState<Set<Square>>) {
    if (board.isKingAttacked) {
        val kingSquare = board.getKingSquare(board.sideToMove)
        val newSet = highlightedSquares.value.toHashSet()
        newSet.add(kingSquare)
        highlightedSquares.value = newSet
    }
}

fun updatePromotionPiece(
    selectedSquare: MutableState<Square?>,
    promotionData: MutableState<PromotionData>,
    board: MutableState<Board>,
    square: Square,
) {
    Log.d("PlayLocally", "updatePromotionPiece")
    if (selectedSquare.value != null) {
        val selectedPiece = board.value.getPiece(selectedSquare.value)

        promotionData.value.midPromotion =
            (selectedPiece == Piece.WHITE_PAWN && square.rank == Rank.RANK_8) ||
                    (selectedPiece == Piece.BLACK_PAWN && square.rank == Rank.RANK_1)

        Log.d("updatePromotionPiece", "promotionData: ${promotionData.value}")

        if (promotionData.value.midPromotion) {
            promotionData.value.pawnSquare = selectedSquare.value
            promotionData.value.promotionSquare = square
            Log.d("updatePromotionPiece", "promotionData updated: ${promotionData.value}")
        }
    }
}

fun executeMove(
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    mainPlayerTime: MutableState<Int>,
    opponentPlayerTime: MutableState<Int>,
    oppositionColor: Side,
    increment: Int,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    square: Square,
    soundManager: SoundManager
) {

    if (selectedSquare.value == null) {
        return
    }


    val move = Move(selectedSquare.value, square)


    if (board.value.sideToMove != oppositionColor) {
        mainPlayerTime.value += increment
    } else if (board.value.sideToMove == oppositionColor) {
        opponentPlayerTime.value += increment
    }

    board.value.doMove(move)
    soundManager.playMoveSound()
    selectedSquare.value = null
    highlightedSquares.value = emptySet()
    promotionData.value = PromotionData()


    checkEndGameConditions(board, gameOverData)

    Log.d("executeMove", "board: ${board.value}")
}

fun executeMove(
    moveString: String,
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    mainPlayerTime: MutableState<Int>,
    opponentPlayerTime: MutableState<Int>,
    oppositionColor: Side,
    increment: Int,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    soundManager: SoundManager
) {
    if (board.value.sideToMove != oppositionColor) {
        mainPlayerTime.value += increment
    } else if (board.value.sideToMove == oppositionColor) {
        opponentPlayerTime.value += increment
    }

    board.value.doMove(moveString)
    soundManager.playMoveSound()
    selectedSquare.value = null
    highlightedSquares.value = emptySet()
    promotionData.value = PromotionData()


    checkEndGameConditions(board, gameOverData)
}

fun checkEndGameConditions(
    board: MutableState<Board>,
    gameOverData: MutableState<gameOverData>
) {

    val isWon = board.value.isMated()
    val isThreefoldRepetition = board.value.isRepetition(3)
    val isDraw = board.value.isDraw() or isThreefoldRepetition
//    Draw by insufficient material isnt automatic

    gameOverData.value.gameOver = isWon or isDraw
    if (gameOverData.value.gameOver) {
        gameOverData.value.gameOverMessageId = getEndGameMessage(board.value)
    }

}

fun highlightLegalMoves(
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    board: MutableState<Board>,
    square: Square
) {
    Log.d("PlayLocally", "highlightLegalMoves")
    if (selectedSquare.value == square) {
        selectedSquare.value = null
        highlightedSquares.value = emptySet()
    } else {
        selectedSquare.value = square
        highlightedSquares.value =
            getPieceLegalMoves(
                board,
                square
            ).map { it.to }
                .toSet()
    }

    Log.d("highlightLegalMoves", "highlightedSquares: ${highlightedSquares}")
}

fun isWhiteSquare(square: Square): Boolean {
    return (square.rank.ordinal + square.file.ordinal) % 2 == 1
}

@Composable
fun PlaySection(
    board: MutableState<Board>,
    highlightedSquares: Set<Square>,
    oppositionColors: Side,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    onSquareClick: (Square) -> Unit
) {
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {

        ChessBoard(
            board = board.value,
            highlightedSquares = highlightedSquares,
            oppositionColors = oppositionColors,
            promotionData = promotionData.value,
            onSquareClick = if (!promotionData.value.midPromotion) onSquareClick else { _ ->
            }
        )

//        if (gameOverData.value.gameOver) {
//            EndGameDialog(gameOverData.value)
//        }

        if (promotionData.value.midPromotion) {
            PromotionChoice(
                gameOverData,
                board,
                oppositionColors,
                promotionData
            )
        }
    }
}

@Composable
fun EndGameDialog(
    gameOverData: gameOverData,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = gameOverData.gameOverMessageId!!),
            modifier = Modifier
                .padding(8.dp)
                .rotate(180f),
            style = MaterialTheme.typography.labelMedium
        )
        ExitButton()
        Text(
            text = stringResource(id = gameOverData.gameOverMessageId!!),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelMedium

        )
    }
}


@Composable
fun ExitButton() {

    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            context.startActivity(Intent(context, MainActivity::class.java))
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.exit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun saveGame(
    context: Context,
    board: Board,
    mainPlayerTime: Int,
    opponentPlayerTime: Int,
    opponentColor: Side,
    timeControlMain: Int,
    increment: Int
) {
    val gameData = mapOf(
        "boardFEN" to board.fen,
        "mainPlayerTime" to mainPlayerTime,
        "opponentPlayerTime" to opponentPlayerTime,
        "opponentColor" to opponentColor.name,
        "timeControlMain" to timeControlMain,
        "increment" to increment
    )

    val json = JSONObject(gameData)
    val fileName = LocalDateTime.now().toString() + "_OverTheBoard.json"


    context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
        it.write(json.toString().toByteArray())
        Log.d("saveGame", "Game saved to $fileName")
    }

    context.startActivity(Intent(context, MainActivity::class.java))

}

@Composable
fun SaveButton(
    board: Board,
    mainPlayerTime: Int,
    opponentPlayerTime: Int,
    opponentColor: Side,
    timeControlMain: Int,
    increment: Int
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            saveGame(
                context,
                board,
                mainPlayerTime,
                opponentPlayerTime,
                opponentColor,
                timeControlMain,
                increment
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.save_game),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

}

@Composable
fun CancelButton(gameState: MutableState<GameState>) {
    OutlinedButton(
        onClick = { gameState.value = GameState.IN_GAME },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.cancel),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

}

@Composable
fun ExitDialog(
    gameState: MutableState<GameState>,
    context: Context,
    board: Board,
    mainPlayerTime: Int,
    opponentPlayerTime: Int,
    opponentColor: Side,
    timeControlMain: Int,
    increment: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.exit_save_dialogue),
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            ExitButton()
            SaveButton(
                board,
                mainPlayerTime,
                opponentPlayerTime,
                opponentColor,
                timeControlMain,
                increment
            )
            CancelButton(gameState)
        }
    }

}


@Composable
fun SaveDialog(
    gameState: MutableState<GameState>,
    context: Context,
    board: Board,
    mainPlayerTime: Int,
    opponentPlayerTime: Int,
    opponentColor: Side,
    timeControlMain: Int,
    increment: Int
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.exit_save_dialogue),
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            SaveButton(
                board,
                mainPlayerTime,
                opponentPlayerTime,
                opponentColor,
                timeControlMain,
                increment
            )
            CancelButton(gameState)
        }
    }

}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplayCapturedPieces(
    side: Side,
    board: Board,
    flipped: Boolean = false
) {
    val capturedPieces = getCapturedPieces(board).filter { it.key.pieceSide != side }

    FlowRow(
        maxItemsInEachRow = 4
    ) {
        for ((piece, count) in capturedPieces.entries) {
            for (i in 0 until count) {
                Image(
                    painter = painterResource(id = getPieceImageFromFen(piece.fenSymbol)!!),
                    contentDescription = piece.fenSymbol,
                    modifier = when (flipped) {
                        false -> Modifier.size(40.dp)
                        true -> Modifier
                            .size(30.dp)
                            .rotate(180f)
                    }
                )
            }
        }
    }
}

@Composable
fun DrawButton(
    offeredDrawData: MutableState<OfferedDrawData>,
    side: Side,
    clickable: Boolean = true,
) {
    OutlinedButton(
        onClick = {
            if (clickable) {
                when (side) {
                    Side.WHITE -> offeredDrawData.value = offeredDrawData.value.copy(
                        whiteOffered = !offeredDrawData.value.whiteOffered
                    )

                    Side.BLACK -> offeredDrawData.value = offeredDrawData.value.copy(
                        blackOffered = !offeredDrawData.value.blackOffered
                    )
                }
            }
        },


        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.primary,
            containerColor = when (side) {
                Side.WHITE -> if (offeredDrawData.value.whiteOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Side.BLACK -> if (offeredDrawData.value.blackOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            },
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.offer_draw),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
//            color = when (side) {
//                Side.WHITE -> if (offeredDrawData.value.whiteOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
//                Side.BLACK -> if (offeredDrawData.value.blackOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
//            }
        )
    }
}

@Composable
fun BluetoothDrawButton(
    offeredDrawData: MutableState<OfferedDrawData>,
    side: Side,
    writer: BufferedWriter,
) {
    OutlinedButton(
        onClick = {
            when (side) {
                Side.WHITE -> offeredDrawData.value = offeredDrawData.value.copy(
                    whiteOffered = !offeredDrawData.value.whiteOffered
                )

                Side.BLACK -> offeredDrawData.value = offeredDrawData.value.copy(
                    blackOffered = !offeredDrawData.value.blackOffered
                )
            }

            val offeredDraw = when (side) {
                Side.WHITE -> offeredDrawData.value.whiteOffered
                Side.BLACK -> offeredDrawData.value.blackOffered
            }

            writeData(
                writer,
                JSONObject(
                    mapOf(
                        "draw" to when (offeredDraw) {
                            true -> "activate"
                            false -> "deactivate"
                        },
                    )
                ).toString()
            )
        },


        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.primary,
            containerColor = when (side) {
                Side.WHITE -> if (offeredDrawData.value.whiteOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Side.BLACK -> if (offeredDrawData.value.blackOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            },
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.offer_draw),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
//            color = when (side) {
//                Side.WHITE -> if (offeredDrawData.value.whiteOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
//                Side.BLACK -> if (offeredDrawData.value.blackOffered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
//            }
        )
    }
}

@Composable
fun ResignButton(resignData: MutableState<ResignData>, side: Side) {

    if (resignData.value.side != side) {
        throw IllegalStateException("Resign button called with wrong side/resign data")
    }
    Column(
        horizontalAlignment = Alignment.End
    ) {
        OutlinedButton(
            onClick = {
                if (resignData.value.resigning) {
                    resignData.value = resignData.value.copy(
                        confirmed = true,
                    )
                } else {
                    resignData.value = resignData.value.copy(
                        resigning = true,
                    )
                }

            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (resignData.value.resigning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(
                text = if (resignData.value.resigning) stringResource(R.string.confirm_q) else stringResource(
                    id = R.string.resign
                ),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (resignData.value.resigning) {
            OutlinedButton(
                onClick = {
                    resignData.value = resignData.value.copy(
                        confirmed = false,
                        resigning = false
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resignData.value.resigning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

}

@Composable
fun PlayerRow(
    time: Int,
    side: Side,
    oppositionColor: Side,
    board: Board,
    resignData: MutableState<ResignData>,
    offeredDrawData: MutableState<OfferedDrawData>,
) {

    Column(
        modifier = Modifier.rotate(if (oppositionColor == side) 180f else 0f),
    ) {

        Row(
//        TODO: maybe bad hard coding a dp value this high
            modifier = Modifier.width(320.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
            ) {

                DisplayTime(time)
                DrawButton(offeredDrawData, side)
            }

            DisplayCapturedPieces(side, board, oppositionColor == side)

            Column(
                horizontalAlignment = Alignment.End
            ) {
                DisplayTime(time, flipped = true)
                ResignButton(resignData, side)
            }
        }
    }
}


@Composable
fun DisplayTime(
    time: Int,
    flipped: Boolean = false
) {
    val minutes = time / 60
    val seconds = time % 60

    Text(
        text = "%02d:%02d".format(minutes, seconds),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.labelMedium,
        modifier = when (flipped) {
            true -> Modifier.rotate(180f)
            false -> Modifier
        }
    )
}

@Composable
fun PromotionChoice(
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    oppositionColors: Side,
    promotionData: MutableState<PromotionData>,
) {
    Log.d("PlayLocally", "PromotionChoice")
    val sideToMove = board.value.sideToMove

    if (promotionData.value.pawnSquare == null || promotionData.value.promotionSquare == null) {
        return
    }

    val promotionOptions = if (sideToMove == Side.WHITE) {

        listOf(
            Piece.WHITE_QUEEN,
            Piece.WHITE_ROOK,
            Piece.WHITE_BISHOP,
            Piece.WHITE_KNIGHT
        )
    } else {
        listOf(
            Piece.BLACK_QUEEN,
            Piece.BLACK_ROOK,
            Piece.BLACK_BISHOP,
            Piece.BLACK_KNIGHT
        )
    }

    Column(
        modifier = Modifier.rotate(if (oppositionColors == sideToMove) 180f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.Promote_To),
            modifier = Modifier,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,

            ) {

            for (piece in promotionOptions) {
                Image(
                    painter = painterResource(id = getPieceImageFromFen(piece.fenSymbol)!!),
                    contentDescription = piece.fenSymbol,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            promotionData.value.promotionPiece = piece
                            executePromotion(
                                board,
                                promotionData,
                                gameOverData
                            )
                        }
                )
            }


        }
    }
}

//TODO: back button doesnt work in bluetooth

fun executePromotion(
    board: MutableState<Board>,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>
) {
    Log.d("PlayLocally", "executePromotion")
    Log.d("executePromotion", "promotionData: ${promotionData.value}")
    val move = Move(
        promotionData.value.pawnSquare!!,
        promotionData.value.promotionSquare,
        promotionData.value.promotionPiece
    )
    board.value.doMove(move)

    promotionData.value = PromotionData()

    checkEndGameConditions(board, gameOverData)
}


fun getEndGameMessage(board: Board): Int? {
    val isWon = board.isMated()
    val isThreefoldRepetitionDraw = board.isRepetition(3)
    val isInsufficientMaterialDraw = board.isInsufficientMaterial()
    val isDraw = board.isDraw() or isThreefoldRepetitionDraw or isInsufficientMaterialDraw

    if (isWon) {
        val losingSide = board.sideToMove
        if (losingSide == Side.WHITE) {
            return R.string.Black_Wins
        } else {
            return R.string.White_Wins
        }

    } else if (isDraw) {
        if (isThreefoldRepetitionDraw) {
            return R.string.draw_by_repetition
        } else if (board.isStaleMate()) {
            return R.string.Draw_by_stalemate
        } else if (isInsufficientMaterialDraw) {
            return R.string.Draw_by_insufficient_material
        } else {
            return R.string.Draw_Generic
        }
    }
    return null
}


@Composable
fun ChessBoard(
    board: Board,
    highlightedSquares: Set<Square>,
    oppositionColors: Side,
    promotionData: PromotionData,
    onSquareClick: (Square) -> Unit
) {
    val blurBoard by animateDpAsState(
        targetValue = if (promotionData.midPromotion) 5.dp else 0.dp,
        animationSpec = tween(durationMillis = if (promotionData.midPromotion) 1000 else 300)
    )

    Column(
        modifier = Modifier
//            .fillMaxSize()
            .blur(blurBoard)
//            .graphicsLayer { scaleY = if (oppositionColors == Side.BLACK) -1f else 0f },
            .graphicsLayer { rotationZ = if (oppositionColors == Side.WHITE) 180f else 0f },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in 7 downTo 0) {
            Row {
                for (col in 0..7) {
                    val square = Square.entries[row * 8 + col]
                    val piece = board.getPiece(square)

//                    val pieceFlipped = flippedSquares.contains(square)
                    val highlighted = highlightedSquares.contains(square)
                    ChessTile(
                        square,
                        piece,
                        pieceFlipped = piece.pieceSide == Side.BLACK,
                        highlighted = highlighted,
                        onClick = {
                            onSquareClick(it)
                        })

                }

            }
        }
    }
}

fun getPieceImageFromFen(fen: String): Int? {
    val pieceImages: HashMap<String, Int> = hashMapOf(
        "P" to R.drawable.w_pawn,
        "N" to R.drawable.w_knight,
        "B" to R.drawable.w_bishop,
        "R" to R.drawable.w_rook,
        "Q" to R.drawable.w_queen,
        "K" to R.drawable.w_king,
        "p" to R.drawable.b_pawn,
        "n" to R.drawable.b_knight,
        "b" to R.drawable.b_bishop,
        "r" to R.drawable.b_rook,
        "q" to R.drawable.b_queen,
        "k" to R.drawable.b_king,
    )

    return pieceImages[fen]
}

@Composable
fun ChessTile(
    square: Square,
    piece: Piece,
    pieceFlipped: Boolean = false,
    highlighted: Boolean = false,
    onClick: (Square) -> Unit
) {

    val drawableId = getPieceImageFromFen(piece.fenSymbol)

    val tileColor = if (isWhiteSquare(square)) {
        Color.LightGray
    } else {
        Color.DarkGray
    }

    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick(square) },
        color = tileColor,
        shape = RectangleShape
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(color = Color.Red.copy(alpha = if (highlighted) 0.30f else 0f))
        ) {
            if (drawableId != null) {
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = piece.fenSymbol,
                    modifier = Modifier
                        .graphicsLayer(rotationZ = if (pieceFlipped) 180f else 0f)
                )
            }
        }
    }
}

fun getPieceLegalMoves(boardState: State<Board>, pieceSquare: Square): List<Move> {
    Log.d("PlayLocally", "getPieceLegalMoves")
    val board = boardState.value
//    Log.d("getPieceLegalMoves", "boardState: ${boardState.value}")
//    val pieceSquare = board.getPieceLocation(piece)
//    Log.d("getPieceLegalMoves", "pieceSquare: $pieceSquare")
    val legalMoves = board.legalMoves().filter { it.from == pieceSquare }
//    Log.d("getPieceLegalMoves", "legalMoves: $legalMoves")
    return legalMoves
}


//TODO: run out of time (since theres latency)
@Composable
fun BluetoothLaunchedEffects(
    hostTime: MutableState<Int>,
    joinerTime: MutableState<Int>,
    increment: Int,
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    hostColor: Side,
    reader: BufferedReader,
    soundManager: SoundManager,
    whiteResignData: MutableState<ResignData>,
    blackResignData: MutableState<ResignData>,
    promotionData: MutableState<PromotionData>,
    offeredDrawData: MutableState<OfferedDrawData>,
) {


    LaunchedEffect(Unit) {
        while (!gameOverData.value.gameOver) {
            delay(1000)
            if (board.value.sideToMove == hostColor) {
                hostTime.value -= 1
            } else {
                joinerTime.value -= 1
            }
        }

    }

    Log.d("PlayLocally", "recomposition isKingAttacked: ${board.value.isKingAttacked}")
    LaunchedEffect(board.value.isKingAttacked) {
        if (board.value.isKingAttacked) {
            soundManager.playCheckWinSound()
        }
        Log.d("PlayLocally", "isKingAttacked: ${board.value.isKingAttacked}")
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (!gameOverData.value.gameOver) {
                var line: String;
                while (reader.readLine().also { line = it } != null) {
                    val json = JSONObject(line)
                    if (json.has("move")) {
                        val moveString = json.getString("move")
                        executeMove(
                            moveString,
                            selectedSquare = selectedSquare,
                            highlightedSquares = highlightedSquares,
                            mainPlayerTime = hostTime,
                            opponentPlayerTime = joinerTime,
                            oppositionColor = hostColor,
                            increment = increment,
                            promotionData = promotionData,
                            gameOverData = gameOverData,
                            board = board,
                            soundManager = soundManager
                        )
//                        board.value.doMove(moveString)
                    }
                    if (json.has("resign")) {
                        val resignData = if (json.getString("resign") == "WHITE") {
                            whiteResignData
                        } else {
                            blackResignData
                        }

                        resignData.value = resignData.value.copy(
                            confirmed = true,
                        )
                    }
                    if (json.has("draw")) {
                        when (json.getString("draw")) {
                            "activate" -> {
                                when (hostColor) {
                                    Side.WHITE -> {
                                        offeredDrawData.value = offeredDrawData.value.copy(
                                            blackOffered = true,
                                        )
                                    }

                                    Side.BLACK -> {
                                        offeredDrawData.value = offeredDrawData.value.copy(
                                            whiteOffered = true,
                                        )
                                    }
                                }
                            }

                            "deactivate" -> {
                                when (hostColor) {
                                    Side.WHITE -> {
                                        offeredDrawData.value = offeredDrawData.value.copy(
                                            blackOffered = false,
                                        )
                                    }

                                    Side.BLACK -> {
                                        offeredDrawData.value = offeredDrawData.value.copy(
                                            whiteOffered = false,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (json.has("timeout")) {
                        gameOverData.value.gameOver = true
                        gameOverData.value.gameOverMessageId = when (json.getString("timeout")) {
                            "WHITE" -> R.string.Black_Wins
                            "BLACK" -> R.string.White_Wins
                            else -> null
                        }

                    }
                }
            }
        }
    }
}


@Composable
fun BluetoothChessScreen(
    increment: Int,
    contentPadding: PaddingValues,
    soundManager: SoundManager,
    gameState: MutableState<GameState>,
    hostTime: MutableIntState,
    joinerTime: MutableIntState,
    board: MutableState<Board>,
    highlightedSquares: MutableState<Set<Square>>,
    hostColor: Side,
    selectedSquare: MutableState<Square?>,
    gameOverData: MutableState<gameOverData>,
    promotionData: MutableState<PromotionData>,
    whiteResignData: MutableState<ResignData>,
    blackResignData: MutableState<ResignData>,
    offeredDrawData: MutableState<OfferedDrawData>,
    writer: BufferedWriter,
    reader: BufferedReader,
    onSquareClick: (Square) -> Unit,
) {
    BluetoothLaunchedEffects(
        hostTime = hostTime,
        joinerTime = joinerTime,
        increment = increment,
        gameOverData = gameOverData,
        board = board,
        selectedSquare = selectedSquare,
        highlightedSquares = highlightedSquares,
        hostColor = hostColor,
        reader = reader,
        soundManager = soundManager,
        whiteResignData = whiteResignData,
        blackResignData = blackResignData,
        promotionData = promotionData,
        offeredDrawData = offeredDrawData,
    )

    if (hostTime.intValue <= 0) {
        gameOverData.value.gameOver = true

        when (hostColor) {
            Side.WHITE -> gameOverData.value.gameOverMessageId = R.string.Black_Wins
            Side.BLACK -> gameOverData.value.gameOverMessageId = R.string.White_Wins
        }

        writeData(
            writer,
            JSONObject().put("timeout", hostColor.name)
        )
    } else if (joinerTime.intValue <= 0) {
        gameOverData.value.gameOver = true

        when (hostColor) {
            Side.WHITE -> gameOverData.value.gameOverMessageId = R.string.White_Wins
            Side.BLACK -> gameOverData.value.gameOverMessageId = R.string.Black_Wins
        }

        writeData(
            writer,
            JSONObject().put(
                "timeout",
                if (hostColor == Side.WHITE) Side.BLACK.name else Side.WHITE.name
            )
        )
    }

    Box() {

        val blurBoard by animateDpAsState(
            targetValue = if (gameState.value in listOf(
                    GameState.SAVING,
                    GameState.EXITING
                ) || gameOverData.value.gameOver
            ) 5.dp else 0.dp,
            animationSpec = tween(durationMillis = 1000)
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .blur(
                    blurBoard
                )
                .clickable { gameState.value = GameState.IN_GAME },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JoinerRow(
                time = joinerTime.intValue,
                side = if (hostColor == Side.WHITE) Side.BLACK else Side.WHITE,
                board = board.value,
                offeredDrawData,
            )
            BluetoothPlaySection(
                board = board,
                highlightedSquares = highlightedSquares.value,
                hostColor = hostColor,
                promotionData = promotionData,
                gameOverData = gameOverData,
                writer,
                onSquareClick
            )
            HostMainRow(
                time = hostTime.intValue,
                side = hostColor,
                board = board.value,
                resignData = if (hostColor == Side.WHITE) whiteResignData else blackResignData,
                offeredDrawData = offeredDrawData,
                writer,
            )
        }

        if (whiteResignData.value.confirmed) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.Black_Wins
        } else if (blackResignData.value.confirmed) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.White_Wins
        }

        if (offeredDrawData.value.blackOffered && offeredDrawData.value.whiteOffered) {
            gameOverData.value.gameOver = true
            gameOverData.value.gameOverMessageId = R.string.draw_by_agreement
        }

        if (gameOverData.value.gameOver) {
            EndGameDialog(
                gameOverData = gameOverData.value,
            )
        }
        if (gameState.value == GameState.EXITING) {
            BluetoothExitDialog(
                gameState = gameState,
                mainResignData = if (hostColor == Side.WHITE) whiteResignData else blackResignData,
                writer,
            )
        }

        if (gameState.value == GameState.SAVING) {
            gameState.value = GameState.IN_GAME
        }


    }

}

//TODO: connecting..., listDevices, offer draw joiner row, taken pieces joiner row

@Composable
fun BluetoothExitDialog(
    gameState: MutableState<GameState>,
    mainResignData: MutableState<ResignData>,
    writer: BufferedWriter,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.are_you_sure_you_want_to_exit),
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            BluetoothExitButton(mainResignData, writer)
            CancelButton(gameState)
        }
    }

}

@Composable
fun BluetoothExitButton(
    mainResignData: MutableState<ResignData>,
    writer: BufferedWriter,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            mainResignData.value = mainResignData.value.copy(
                confirmed = true,
            )
            writeData(
                writer,
                JSONObject().put(
                    "resign",
                    mainResignData.value.side.name
                )
            )
            context.startActivity(Intent(context, MainActivity::class.java))
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        Text(
            text = stringResource(id = R.string.exit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun HostMainRow(
    time: Int,
    side: Side,
    board: Board,
    resignData: MutableState<ResignData>,
    offeredDrawData: MutableState<OfferedDrawData>,
    writer: BufferedWriter,
) {
    Row(
//        TODO: maybe bad hard coding a dp value this high
        modifier = Modifier.width(320.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
        ) {

            DisplayTime(time)
            BluetoothDrawButton(offeredDrawData, side, writer)
        }

        DisplayCapturedPieces(side, board)

        BluetoothResignButton(resignData, side, writer)
    }
}

@Composable
fun BluetoothResignButton(
    resignData: MutableState<ResignData>,
    side: Side,
    writer: BufferedWriter,
) {
    if (resignData.value.side != side) {
        throw IllegalStateException("Resign button called with wrong side/resign data")
    }
    Column(
        horizontalAlignment = Alignment.End
    ) {
        OutlinedButton(
            onClick = {
                if (resignData.value.resigning) {
                    resignData.value = resignData.value.copy(
                        confirmed = true,
                    )

                    val resignData = JSONObject().put("resign", side.name)
                    writeData(writer, resignData)

                } else {
                    resignData.value = resignData.value.copy(
                        resigning = true,
                    )
                }

            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (resignData.value.resigning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.width(90.dp)
        ) {
            Text(
                text = if (resignData.value.resigning) stringResource(R.string.confirm_q) else stringResource(
                    id = R.string.resign
                ),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (resignData.value.resigning) {
            OutlinedButton(
                onClick = {
                    resignData.value = resignData.value.copy(
                        confirmed = false,
                        resigning = false
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resignData.value.resigning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.width(90.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

}

@Composable
fun JoinerRow(
    time: Int,
    side: Side,
    board: Board,
    offeredDrawData: MutableState<OfferedDrawData>
) {
    Row(
        modifier = Modifier.width(320.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
        ) {
            DrawButton(
                offeredDrawData = offeredDrawData,
                side = side,
                clickable = false
            )
            DisplayTime(time)
        }

        DisplayCapturedPieces(
            side,
            board,
        )

        Box(
            modifier = Modifier
                .width(90.dp)
        ) {}
    }
}

@Composable
fun BluetoothChessBoard(
    board: Board,
    highlightedSquares: Set<Square>,
    promotionData: PromotionData,
    hostColor: Side,
    onSquareClick: (Square) -> Unit,
) {
    val blurBoard by animateDpAsState(
        targetValue = if (promotionData.midPromotion) 5.dp else 0.dp,
        animationSpec = tween(durationMillis = if (promotionData.midPromotion) 1000 else 300)
    )

    Column(
        modifier = Modifier
            .blur(blurBoard)
            .graphicsLayer { rotationZ = if (hostColor == Side.BLACK) 180f else 0f },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        for (row in 7 downTo 0) {
            Row {
                for (col in 0..7) {
                    val square = Square.entries[row * 8 + col]
                    val piece = board.getPiece(square)

                    val highlighted = highlightedSquares.contains(square)
                    ChessTile(
                        square,
                        piece,
                        pieceFlipped = hostColor == Side.BLACK,
                        highlighted = highlighted,
                        onClick = {
                            onSquareClick(square)
                        }
                    )

                }

            }
        }
    }
}

fun bluetoothHandleBoardClick(
    square: Square,
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    promotionData: MutableState<PromotionData>,
    board: MutableState<Board>,
    gameOverData: MutableState<gameOverData>,
    hostTime: MutableIntState,
    joinerTime: MutableIntState,
    hostColor: Side,
    increment: Int,
    soundManager: SoundManager,
    writer: BufferedWriter,
) {
    if (board.value.sideToMove != hostColor) {
        return
    }

    updatePromotionPiece(selectedSquare, promotionData, board, square)

    val move = Move(selectedSquare.value, square)

    val isLegalMove =
        move in board.value.legalMoves()
    if (isLegalMove) {
        bluetoothExecuteMove(
            square,
            selectedSquare,
            highlightedSquares,
            promotionData,
            board,
            gameOverData,
            hostTime,
            joinerTime,
            hostColor,
            increment,
            soundManager,
            writer
        )
    } else {
        highlightLegalMoves(selectedSquare, highlightedSquares, board, square)
    }

    updateKingCheckHighlight(board.value, highlightedSquares)
}

fun bluetoothExecuteMove(
    square: Square,
    selectedSquare: MutableState<Square?>,
    highlightedSquares: MutableState<Set<Square>>,
    promotionData: MutableState<PromotionData>,
    board: MutableState<Board>,
    gameOverData: MutableState<gameOverData>,
    hostTime: MutableIntState,
    joinerTime: MutableIntState,
    hostColor: Side,
    increment: Int,
    soundManager: SoundManager,
    writer: BufferedWriter,
) {
    if (selectedSquare.value == null) {
        return
    }


    val move = Move(selectedSquare.value, square)


    if (board.value.sideToMove == hostColor) {
        hostTime.value += increment
    } else {
        joinerTime.value += increment
    }

    board.value.doMove(move)
    writeData(
        writer,
        JSONObject()
            .put("move", move.toString())
    )
    soundManager.playMoveSound()
    selectedSquare.value = null
    highlightedSquares.value = emptySet()
    promotionData.value = PromotionData()


    checkEndGameConditions(board, gameOverData)

    Log.d("executeMove", "board: ${board.value}")
}

@Composable
fun BluetoothPlaySection(
    board: MutableState<Board>,
    highlightedSquares: Set<Square>,
    hostColor: Side,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    writer: BufferedWriter,
    onSquareClick: (Square) -> Unit,
) {

    Box(
        contentAlignment = Alignment.Center,
    ) {
        BluetoothChessBoard(
            board = board.value,
            highlightedSquares = highlightedSquares,
            promotionData = promotionData.value,
            hostColor = hostColor,
            onSquareClick = if (!promotionData.value.midPromotion) onSquareClick else { _ ->
            }
        )

//        if (promotionData.value.midPromotion && promotionData.value.promotionPiece?.pieceSide == hostColor) {
        if (promotionData.value.midPromotion) {
            BluetoothPromotionChoice(
                gameOverData = gameOverData,
                board = board,
                hostColor = hostColor,
                promotionData = promotionData,
                writer
            )
        }

    }

}

@Composable
fun BluetoothPromotionChoice(
    gameOverData: MutableState<gameOverData>,
    board: MutableState<Board>,
    hostColor: Side,
    promotionData: MutableState<PromotionData>,
    writer: BufferedWriter
) {
    val sideToMove = board.value.sideToMove

    if (promotionData.value.pawnSquare == null || promotionData.value.promotionSquare == null || sideToMove != hostColor) {
        return
    }

    val promotionOptions = if (hostColor == Side.WHITE) {

        listOf(
            Piece.WHITE_QUEEN,
            Piece.WHITE_ROOK,
            Piece.WHITE_BISHOP,
            Piece.WHITE_KNIGHT
        )
    } else {
        listOf(
            Piece.BLACK_QUEEN,
            Piece.BLACK_ROOK,
            Piece.BLACK_BISHOP,
            Piece.BLACK_KNIGHT
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = stringResource(id = R.string.Promote_To),
            modifier = Modifier,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,

            ) {

            for (piece in promotionOptions) {
                Image(
                    painter = painterResource(id = getPieceImageFromFen(piece.fenSymbol)!!),
                    contentDescription = piece.fenSymbol,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            promotionData.value.promotionPiece = piece
                            bluetoothExecutePromotion(
                                board,
                                promotionData,
                                gameOverData,
                                writer
                            )
                        }
                )
            }
        }
    }

}

fun bluetoothExecutePromotion(
    board: MutableState<Board>,
    promotionData: MutableState<PromotionData>,
    gameOverData: MutableState<gameOverData>,
    writer: BufferedWriter,
) {
    val move = Move(
        promotionData.value.pawnSquare!!,
        promotionData.value.promotionSquare,
        promotionData.value.promotionPiece
    )

    board.value.doMove(move)
    writeData(
        writer,
        JSONObject()
            .put("move", move.toString())
    )

    promotionData.value = PromotionData()

    checkEndGameConditions(board, gameOverData)
}

fun writeData(
    writer: BufferedWriter,
    data: JSONObject
) {
    writer.write(data.toString())
    writer.newLine()
    writer.flush()
}

fun writeData(
    writer: BufferedWriter,
    data: String
) {
    writer.write(data)
    writer.newLine()
    writer.flush()
}
