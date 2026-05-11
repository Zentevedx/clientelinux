package com.example.clientelinux

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import com.example.clientelinux.ui.theme.ClienteLinuxTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RemoteViewModel by viewModels()
    private var isPinned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClienteLinuxTheme {
                ClienteLinuxApp(
                    vm = viewModel,
                    onTogglePin = { togglePin() },
                    onTogglePointerCapture = { capture ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val view = window.decorView
                            if (capture) {
                                view.setOnCapturedPointerListener { _, event ->
                                    if (event.action == MotionEvent.ACTION_MOVE) {
                                        viewModel.handleMouseDelta(event.x, event.y)
                                        true
                                    } else if (event.action == MotionEvent.ACTION_BUTTON_PRESS || event.action == MotionEvent.ACTION_DOWN) {
                                        if (event.buttonState and MotionEvent.BUTTON_PRIMARY != 0) viewModel.handleMouseClick(1)
                                        else if (event.buttonState and MotionEvent.BUTTON_SECONDARY != 0) viewModel.handleMouseClick(3)
                                        else if (event.buttonState and MotionEvent.BUTTON_TERTIARY != 0) viewModel.handleMouseClick(2)
                                        else viewModel.handleMouseClick(1)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                view.requestPointerCapture()
                            } else {
                                view.setOnCapturedPointerListener(null)
                                view.releasePointerCapture()
                            }
                        }
                    }
                )
            }
        }
    }

    private fun togglePin() {
        if (isPinned) {
            stopLockTask()
            isPinned = false
        } else {
            startLockTask()
            isPinned = true
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (vScroll != 0f) {
                viewModel.handleScroll(vScroll)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

@Composable
fun ClienteLinuxApp(
    vm: RemoteViewModel,
    onTogglePin: () -> Unit,
    onTogglePointerCapture: (Boolean) -> Unit
) {
    val state by vm.connectionState.collectAsState()
    val isConnected = state is ConnectionState.Connected

    LaunchedEffect(isConnected) {
        RemoteAccessibilityService.isIntercepting = isConnected
        if (isConnected) {
            RemoteAccessibilityService.keyEvents.collect { event ->
                if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                    vm.handleKeyEvent(event)
                }
            }
        }
    }

    AnimatedContent(
        targetState = isConnected,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition",
    ) { connected ->
        if (connected) {
            RemoteScreen(
                frameFlow = vm.frame,
                lastKeyFlow = vm.lastKeyStr,
                remoteWidth = vm.remoteWidth,
                remoteHeight = vm.remoteHeight,
                wsClient = vm.wsClient,
                onDisconnect = { vm.disconnect() },
                onTogglePin = onTogglePin,
                onTogglePointerCapture = onTogglePointerCapture
            )
        } else {
            ConnectScreen(
                connectionState = state,
                onConnect = { ip, token -> vm.connect(ip, token) },
                onDisconnect = { vm.disconnect() },
            )
        }
    }
}