package com.example.mob_dev_portfolio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.mob_dev_portfolio.ui.theme.MobdevportfolioTheme
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

//object BluetoothHostData {
//    var socket: BluetoothSocket? = null
//    var outputStream: OutputStream? = null
//    var inputStream: InputStream? = null
//}

//TODO: limit connect to only 2 devices (maybe implement spectating in future)
class HostBluetoothGame : ComponentActivity() {
    private lateinit var soundManager: SoundManager
    private val socket = mutableStateOf<BluetoothSocket?>(null)
    private val serverSocket = mutableStateOf<BluetoothServerSocket?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions(this)

        val APP_UUID = UUID.fromString("b3dabd2a-3706-4118-b6b4-3202d0491e2")
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        serverSocket.value =
            createServerSocket(this, this, bluetoothAdapter, APP_UUID) ?: run {
                this.startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )
                return
            }
        Log.d("BluetoothHostGame", "Server socket created: $serverSocket")

        makeDeviceDiscoverable(this)


//        Launch - start a coroutine (multi-threaded async type deal)
//        Dispatchers.IO - run on IO thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                socket.value = serverSocket.value?.accept()

                Log.d("BluetoothHostGame", "Socket accepted: $socket")

            } catch (e: Exception) {
                Log.d("BluetoothHostGame", "Error accepting socket: ${e.message}")
            }
        }

        val initialJSONreceived = mutableStateOf(false)
        setContent {

//           HostBluetoothGame
            val chosenColor = remember { mutableStateOf<ColorChoice?>(null) }
            val timeControlSelected = remember { mutableStateOf<TimeControl?>(null) }
            val timeControlMainInSeconds = remember { mutableIntStateOf(300) }
            val incrementInSeconds = remember { mutableIntStateOf(0) }
            val error = remember { mutableStateOf<Error?>(null) }

            val startGamePressed = remember { mutableStateOf(false) }

            MobdevportfolioTheme {

                if (socket.value != null && startGamePressed.value) {

                    val writer = socket.value!!.outputStream.bufferedWriter()
                    val reader = socket.value!!.inputStream.bufferedReader()

                    soundManager = SoundManager(this)

                    val highlightedSquares = remember { mutableStateOf<Set<Square>>(emptySet()) }
                    val selectedSquare = remember { mutableStateOf<Square?>(null) }
                    val gameOverData = remember { mutableStateOf(gameOverData()) }
                    val promotionData = remember { mutableStateOf(PromotionData()) }

                    val gameState = remember { mutableStateOf(GameState.IN_GAME) }
                    val whiteResignData = remember { mutableStateOf(ResignData(Side.WHITE)) }
                    val blackResignData = remember { mutableStateOf(ResignData(Side.BLACK)) }
                    val offeredDrawData = remember { mutableStateOf(OfferedDrawData()) }

                    val hostTime = remember {
                        mutableIntStateOf(
                            intent.getIntExtra("hostTime", timeControlMainInSeconds.value!!)
                        )
                    }
                    val joinerTime = remember {
                        mutableIntStateOf(
                            intent.getIntExtra("joinerTime", timeControlMainInSeconds.value!!)
                        )
                    }


                    val hostColor = when (chosenColor.value?.name) {
                        "WHITE" -> Side.WHITE
                        "BLACK" -> Side.BLACK
                        "RANDOM" -> listOf(Side.BLACK, Side.WHITE).random()
                        else -> throw IllegalArgumentException(
                            "Invalid color (hostColor): ${
                                chosenColor.value?.name
                            }"
                        )
                    }

                    val initialJsonData = JSONObject()
                        .put("hostColor", hostColor.name)
                        .put("timeControlMain", timeControlMainInSeconds.value!!)
                        .put("increment", incrementInSeconds.value!!)

                    LaunchedEffect(initialJSONreceived.value) {
                        if (!initialJSONreceived.value) {
                            withContext(Dispatchers.IO) {
                                writeData(
                                    writer,
                                    initialJsonData
                                )
                                var line = reader.readLine()
                                while (true) {
                                    if (line == "resend initial JSON") {
                                        writeData(
                                            writer,
                                            initialJsonData
                                        )
                                        line = reader.readLine()
                                    }
                                    if (line == "got initial JSON") {
                                        initialJSONreceived.value = true
                                        break
                                    }
                                }
                            }
                        }
                    }

                    if (initialJSONreceived.value) {


                        Log.d("BluetoothHostGame", "jsonData Sent")

//            TODO: gpt ass code, please verify
//            I mean it works? but still check if its good
                        onBackPressedDispatcher.addCallback(
                            this,
                            object : OnBackPressedCallback(true) {
                                override fun handleOnBackPressed() {
                                    gameState.value = GameState.EXITING
                                }
                            }
                        )

                        val board = remember {
                            mutableStateOf(Board().apply {
                                intent.getStringExtra("boardFEN")?.let { loadFromFen(it) }
                            })
                        }

                        Scaffold(
                            topBar = {
//                                TODO: bluetooth topbar
                                TopBar(
                                    soundManager,
                                    gameState
                                )
                            },
                            bottomBar = { },
                        ) { contentPadding ->
                            BluetoothChessScreen(
                                increment = incrementInSeconds.value!!,
                                contentPadding = contentPadding,
                                soundManager = soundManager,
                                gameState = gameState,
                                hostTime = hostTime,
                                joinerTime = joinerTime,
                                board = board,
                                highlightedSquares = highlightedSquares,
                                hostColor = hostColor,
                                selectedSquare = selectedSquare,
                                gameOverData = gameOverData,
                                promotionData = promotionData,
                                whiteResignData = whiteResignData,
                                blackResignData = blackResignData,
                                offeredDrawData = offeredDrawData,
                                writer = writer,
                                reader = reader,
                            ) { square ->
                                bluetoothHandleBoardClick(
                                    square,
                                    selectedSquare,
                                    highlightedSquares,
                                    promotionData,
                                    board,
                                    gameOverData,
                                    hostTime,
                                    joinerTime,
                                    hostColor,
                                    incrementInSeconds.value!!,
                                    soundManager,
                                    writer,
                                )
                            }
                        }
                    }
                } else {
                    if (startGamePressed.value) {
                        ConnectingDialog()
                    } else {
                        BluetoothInitialChoices(
                            chosenColor,
                            timeControlSelected,
                            timeControlMainInSeconds,
                            incrementInSeconds,
                            error,
                            startGamePressed
                        )
                    }
                }

            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.value?.close()
        serverSocket.value?.close()
        socket.value = null
        serverSocket.value = null
    }
}

@Composable
fun ConnectingDialog() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.connecting),

            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}


@Composable
fun BluetoothInitialChoices(
    pieceColor: MutableState<ColorChoice?>,
    timeControlSelected: MutableState<TimeControl?>,
    timeControlMainInSeconds: MutableState<Int>,
    incrementInSeconds: MutableState<Int>,
    error: MutableState<Error?>,
    startGamePressed: MutableState<Boolean>,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        ChooseColorSegment(pieceColor, error.value)
        ChooseTimeControl(
            timeControlSelected,
            timeControlMainInSeconds,
            incrementInSeconds,
            error.value
        )
        BluetoothStartGameButton(
            pieceColor.value,
            timeControlSelected.value,
            error,
            startGamePressed
        )
    }
}

@Composable
fun BluetoothStartGameButton(
    pieceColor: ColorChoice?,
    timeControl: TimeControl?,
    error: MutableState<Error?>,
    startGamePressed: MutableState<Boolean>,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var borderColor = MaterialTheme.colorScheme.onPrimary
        if (error.value != null) {
            borderColor = MaterialTheme.colorScheme.error
        }
        OutlinedButton(
            onClick = {
                if (pieceColor == null && timeControl == null) {
                    error.value = Error.BOTH
                } else if (pieceColor == null) {
                    error.value = Error.NO_COLOR
                } else if (timeControl == null) {
                    error.value = Error.NO_TIME_CONTROL
                } else {
                    startGamePressed.value = true


//                    val intent = Intent(context, nextActivity).apply {
//                        putExtra("color", pieceColor.name)
//                        putExtra("timeControlMain", timeControlMain)
//                        putExtra("increment", increment)
//                    }
//                    intentToSend.value = intent
//
//                    Log.d("BluetoothHostGame", "Intent created: $intent")


                }


            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(2.dp, borderColor),
        ) {
            Text(
                text = stringResource(id = R.string.start_game),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelMedium,
            )

        }
        if (error.value == Error.BOTH) {
            Text(
                text = stringResource(id = R.string.choose_color_time_control),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineSmall,
            )
        } else if (error.value == Error.NO_COLOR) {
            Text(
                text = stringResource(id = R.string.please_choose_color),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineSmall,
            )
        } else if (error.value == Error.NO_TIME_CONTROL) {
            Text(
                text = stringResource(id = R.string.please_choose_time_control),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

fun requestBluetoothPermissions(activity: ComponentActivity) {
    val permissions = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    )

//    TODO: what are request codes
    ActivityCompat.requestPermissions(activity, permissions, 0)
}

fun makeDeviceDiscoverable(context: Context) {

    if (ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        context.startActivity(discoverableIntent)
    }
}

fun createServerSocket(
    context: Context,
    activity: ComponentActivity,
    bluetoothAdapter: BluetoothAdapter,
    appUUID: UUID
): BluetoothServerSocket? {
    val serverSocket: BluetoothServerSocket;
    if (ActivityCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    ) {

        serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
            "ChessServer",
            appUUID
        )
    } else {
        context.startActivity(Intent(context, MainActivity::class.java))
        return null
    }

    return serverSocket

}

