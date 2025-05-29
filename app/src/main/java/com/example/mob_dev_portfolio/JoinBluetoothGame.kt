package com.example.mob_dev_portfolio

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.UUID


class JoinBluetoothGame : ComponentActivity() {
    private lateinit var soundManager: SoundManager
    private val socket = mutableStateOf<BluetoothSocket?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val APP_UUID = UUID.fromString("b3dabd2a-3706-4118-b6b4-3202d0491e2")
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

//        TODO: maybe worth refactoring into own file, dont like this mixing
        requestBluetoothPermissions(this)

        val discoveredDevices = mutableStateListOf<BluetoothDevice>()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device =
//                        im the king for writing code this good
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let {
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                        }
                    }
                }
            }
        }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this.startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )
            return
        }
        bluetoothAdapter.startDiscovery()

        val startGame = mutableStateOf(false)

        val jsonData = mutableStateOf<JSONObject?>(null);


        val buffersDefined = mutableStateOf(false)
        val bufferedReader = mutableStateOf<BufferedReader?>(null)
        val bufferedWriter = mutableStateOf<BufferedWriter?>(null)

        setContent {
            val selectedDevice = remember { mutableStateOf<BluetoothDevice?>(null) }

            MobdevportfolioTheme {
                if (socket.value != null && !buffersDefined.value) {
                    bufferedWriter.value = socket.value!!.outputStream.bufferedWriter()
                    bufferedReader.value = socket.value!!.inputStream.bufferedReader()
                    buffersDefined.value = true
                }

                if (startGame.value && socket.value != null && jsonData.value != null) {


                    val highlightedSquares = remember { mutableStateOf<Set<Square>>(emptySet()) }
                    val selectedSquare = remember { mutableStateOf<Square?>(null) }
                    val gameOverData = remember { mutableStateOf(gameOverData()) }
                    val promotionData = remember { mutableStateOf(PromotionData()) }

                    soundManager = SoundManager(this)

                    val hostColor = when (jsonData.value?.getString("hostColor")) {
                        "WHITE" -> Side.WHITE
                        "BLACK" -> Side.BLACK
                        else -> {
                            throw IllegalArgumentException(
                                "Invalid color: ${
                                    jsonData.value?.getString(
                                        "hostColor"
                                    )
                                }"
                            )
                        }
                    }
                    val timeControlMain = jsonData.value?.getInt("timeControlMain")
                    val increment = jsonData.value?.getInt("increment")

                    val hostTime = remember {
                        mutableIntStateOf(
                            intent.getIntExtra("hostTime", timeControlMain ?: 300)
                        )
                    }
                    val joinerTime = remember {
                        mutableIntStateOf(
                            intent.getIntExtra("joinerTime", timeControlMain ?: 300)
                        )
                    }

                    val gameState = remember { mutableStateOf(GameState.IN_GAME) }
                    val whiteResignData = remember { mutableStateOf(ResignData(Side.WHITE)) }
                    val blackResignData = remember { mutableStateOf(ResignData(Side.BLACK)) }
                    val offeredDrawData = remember { mutableStateOf(OfferedDrawData()) }

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
                        bottomBar = { },
                    ) { contentPadding ->
//                    TODO: a little bit janky but were gonna do host/joiner swap
                        BluetoothChessScreen(
                            increment = increment!!,
                            contentPadding = contentPadding,
                            soundManager = soundManager,
                            gameState = gameState,
                            hostTime = joinerTime,
                            joinerTime = hostTime,
                            board = board,
                            highlightedSquares = highlightedSquares,
                            hostColor = if (hostColor == Side.WHITE) {
                                Side.BLACK
                            } else {
                                Side.WHITE
                            },
                            selectedSquare = selectedSquare,
                            gameOverData = gameOverData,
                            promotionData = promotionData,
                            whiteResignData = whiteResignData,
                            blackResignData = blackResignData,
                            offeredDrawData = offeredDrawData,
                            writer = bufferedWriter.value!!,
                            reader = bufferedReader.value!!,
                        ) { square ->
                            bluetoothHandleBoardClick(
                                square,
                                selectedSquare,
                                highlightedSquares,
                                promotionData,
                                board,
                                gameOverData,
                                joinerTime,
                                hostTime,
                                if (hostColor == Side.WHITE) {
                                    Side.BLACK
                                } else {
                                    Side.WHITE
                                },
                                increment,
                                soundManager,
                                writer = bufferedWriter.value!!,
                            )
                        }
                    }
                } else {

                    if (socket.value == null) {
                        ListDevices(discoveredDevices) { device ->
                            selectedDevice.value = device
                            bluetoothAdapter.cancelDiscovery()

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val newSocket =
                                        device.createRfcommSocketToServiceRecord(APP_UUID)
                                    newSocket.connect()
                                    socket.value = newSocket!!

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } else {
                        WaitingForHostDialog()

                        LaunchedEffect(startGame.value, socket.value) {
                            withContext(Dispatchers.IO) {
                                if (!startGame.value && socket.value != null) {
                                    Log.d("JoinBluetoothGame", "Waiting for jsonData")
                                    var line = bufferedReader.value!!.readLine() ?: null
                                    Log.d("JoinBluetoothGame", "Line: $line")
                                    while (line != null) {
                                        if (isValidJson(line)) {
                                            Log.d("JoinBluetoothGame", "Valid JSON: $line")
                                            jsonData.value = JSONObject(line)
                                            startGame.value = true
                                            writeData(bufferedWriter.value!!, "got initial JSON")
                                            break
                                        } else {
                                            Log.d("JoinBluetoothGame", "Line: $line")
                                            line = bufferedReader.value!!.readLine()
                                            writeData(bufferedWriter.value!!, "resend initial JSON")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.value?.close()
        socket.value = null
    }
}
//TODO: after pressing phone, immditely go to waiting...

fun isValidJson(jsonString: String): Boolean {
    return try {
        JSONObject(jsonString)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun WaitingForHostDialog() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.waiting_for_host_to_start),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}


@Composable
fun ListDevices(devices: List<BluetoothDevice>, onClick: (BluetoothDevice) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.join_or_host),
            textAlign = TextAlign.Center,
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
            Modifier
                .padding(top = 16.dp, bottom = 16.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            items(devices) { device ->
                DeviceButton(device) {
                    onClick(device)
                }
            }
        }
    }

}

@Composable
fun DeviceButton(
    device: BluetoothDevice,
    onClick: (BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    OutlinedButton(
        onClick = { onClick(device) },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(width = screenWidth * 0.7f, height = 100.dp)
            .padding(
                top = 10.dp,
                bottom = 10.dp,
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary),
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            context.startActivity(
                Intent(
                    context,
                    MainActivity::class.java
                )
            )

            return@OutlinedButton
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED

        ) {
            context.startActivity(
                Intent(
                    context,
                    MainActivity::class.java
                )
            )

            return@OutlinedButton
        }

        Text(
//            TODO: string resources
            text = device.alias ?: device.name ?: stringResource(R.string.unknown_device),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}


