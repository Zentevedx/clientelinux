package com.example.clientelinux

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RemoteScreen(
    frameFlow: StateFlow<ByteArray?>,
    lastKeyFlow: StateFlow<String>,
    remoteWidth: Int,
    remoteHeight: Int,
    wsClient: RemoteWebSocketClient,
    onDisconnect: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePointerCapture: (Boolean) -> Unit,
) {
    val frameBytes by frameFlow.collectAsState()
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val scaleX = remoteWidth.toFloat() / screenWidthPx
    val scaleY = remoteHeight.toFloat() / screenHeightPx

    var showToolbar by remember { mutableStateOf(true) }
    var showKeyboard by remember { mutableStateOf(false) }

    var isPinned by remember { mutableStateOf(false) }
    var isCaptured by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        frameBytes?.let { bytes ->
            val bitmap = remember(bytes) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }

            bitmap?.let { bmp ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    wsClient.sendMouseMove(
                                        (offset.x * scaleX).toInt(),
                                        (offset.y * scaleY).toInt(),
                                    )
                                    wsClient.sendMouseClick(1)
                                },
                                onLongPress = { offset ->
                                    wsClient.sendMouseMove(
                                        (offset.x * scaleX).toInt(),
                                        (offset.y * scaleY).toInt(),
                                    )
                                    wsClient.sendMouseClick(3)
                                },
                                onDoubleTap = { offset ->
                                    wsClient.sendMouseMove(
                                        (offset.x * scaleX).toInt(),
                                        (offset.y * scaleY).toInt(),
                                    )
                                    wsClient.sendMouseClick(1)
                                    wsClient.sendMouseClick(1)
                                },
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                wsClient.sendMouseMove(
                                    (change.position.x * scaleX).toInt(),
                                    (change.position.y * scaleY).toInt(),
                                )
                            }
                        },
                ) {
                    drawImage(
                        image = bmp,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF58A6FF))
                    Spacer(Modifier.height(16.dp))
                    Text("Recibiendo escritorio…", color = Color(0xFF8B949E), fontSize = 14.sp)
                }
            }
        }

        var showDebug by remember { mutableStateOf(false) }

        if (showToolbar) {
            ControlToolbar(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                wsClient = wsClient,
                isPinned = isPinned,
                isCaptured = isCaptured,
                onToggleKeyboard = { showKeyboard = !showKeyboard },
                onTogglePin = {
                    isPinned = !isPinned
                    onTogglePin()
                },
                onToggleCapture = {
                    isCaptured = !isCaptured
                    onTogglePointerCapture(isCaptured)
                },
                onToggleDebug = { showDebug = !showDebug },
                onDisconnect = onDisconnect,
                onHide = { showToolbar = false },
            )
        } else {
            FloatingActionButton(
                onClick = { showToolbar = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp),
                containerColor = Color(0xFF161B22).copy(alpha = 0.85f),
            ) {
                Text("⚙️", fontSize = 18.sp)
            }
        }

        if (showDebug) {
            val isAccActive by RemoteAccessibilityService.isServiceActive.collectAsState()
            val lastKey by lastKeyFlow.collectAsState()
            
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .width(300.dp),
                color = Color(0xFF161B22).copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🐞 Depuración de Teclado", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Servicio Accesibilidad: ${if (isAccActive) "✅ ACTIVO" else "❌ INACTIVO"}", color = if(isAccActive) Color.Green else Color.Red, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Última tecla: $lastKey", color = Color(0xFF58A6FF), fontSize = 12.sp)
                }
            }
        }

        if (showKeyboard) {
            KeyboardPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                wsClient = wsClient,
                onClose = { showKeyboard = false },
            )
        }
    }
}

@Composable
private fun ControlToolbar(
    modifier: Modifier = Modifier,
    wsClient: RemoteWebSocketClient,
    isPinned: Boolean,
    isCaptured: Boolean,
    onToggleKeyboard: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleCapture: () -> Unit,
    onToggleDebug: () -> Unit,
    onDisconnect: () -> Unit,
    onHide: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF161B22).copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarBtn("🐞", onClick = onToggleDebug)
            ToolbarBtn("⌨️", onClick = onToggleKeyboard)
            ToolbarBtn(if (isPinned) "🔓" else "🔒", onClick = onTogglePin) // Screen Pinning
            ToolbarBtn(if (isCaptured) "🖱️✔" else "🖱️", onClick = onToggleCapture) // Mouse Capture
            ToolbarBtn("📋", onClick = { wsClient.sendKey("ctrl+v") })
            ToolbarBtn("❌", onClick = onHide)
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF85149)),
            ) {
                Text("Desconectar", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ToolbarBtn(icon: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Text(icon, fontSize = 18.sp)
    }
}

@Composable
private fun KeyboardPanel(
    modifier: Modifier = Modifier,
    wsClient: RemoteWebSocketClient,
    onClose: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF161B22).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            KeyRow(
                keys = listOf("Esc", "Tab", "↑", "↓", "←", "→", "Home", "End", "Del"),
                wsClient = wsClient,
                toCombo = { key ->
                    when (key) {
                        "Esc"  -> "Escape"
                        "Tab"  -> "Tab"
                        "↑"    -> "Up"
                        "↓"    -> "Down"
                        "←"    -> "Left"
                        "→"    -> "Right"
                        "Home" -> "Home"
                        "End"  -> "End"
                        "Del"  -> "Delete"
                        else   -> key
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            KeyRow(
                keys = listOf("Ctrl+C", "Ctrl+V", "Ctrl+X", "Ctrl+Z", "Ctrl+A", "Ctrl+S", "Ctrl+F", "Super"),
                wsClient = wsClient,
                toCombo = { key ->
                    when (key) {
                        "Ctrl+C" -> "ctrl+c"
                        "Ctrl+V" -> "ctrl+v"
                        "Ctrl+X" -> "ctrl+x"
                        "Ctrl+Z" -> "ctrl+z"
                        "Ctrl+A" -> "ctrl+a"
                        "Ctrl+S" -> "ctrl+s"
                        "Ctrl+F" -> "ctrl+f"
                        "Super"  -> "super"
                        else     -> key
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) {
                    Text("Cerrar", color = Color(0xFF8B949E), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    wsClient: RemoteWebSocketClient,
    toCombo: (String) -> String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        keys.forEach { key ->
            OutlinedButton(
                onClick = { wsClient.sendKey(toCombo(key)) },
                modifier = Modifier.weight(1f).height(40.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE6EDF3)),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF30363D))
                ),
            ) {
                Text(key, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
