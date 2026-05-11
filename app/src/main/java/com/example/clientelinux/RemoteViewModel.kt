package com.example.clientelinux

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel que mantiene el cliente WebSocket vivo a través de rotaciones y recomposiciones.
 * Expone los flows del cliente directamente a la UI.
 */
class RemoteViewModel : ViewModel() {

    val wsClient = RemoteWebSocketClient()

    val connectionState: StateFlow<ConnectionState> = wsClient.state
    val frame: StateFlow<ByteArray?> = wsClient.frame

    private val _lastKeyStr = kotlinx.coroutines.flow.MutableStateFlow("Ninguna")
    val lastKeyStr: StateFlow<String> = _lastKeyStr

    // Resolución del escritorio remoto (se actualiza al recibir el primer frame)
    var remoteWidth: Int = 1920
    var remoteHeight: Int = 1080

    fun connect(ip: String, token: String) {
        wsClient.connect(ip = ip, token = token)
    }

    fun disconnect() {
        wsClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }

    fun handleKeyEvent(event: android.view.KeyEvent): Boolean {
        if (connectionState.value !is ConnectionState.Connected) return false
        
        val keyName = android.view.KeyEvent.keyCodeToString(event.keyCode).replace("KEYCODE_", "")
        val action = if (event.action == android.view.KeyEvent.ACTION_DOWN) "down" else "up"
        
        val meta = "Ctrl:${event.isCtrlPressed} Alt:${event.isAltPressed} Shift:${event.isShiftPressed} Meta:${event.isMetaPressed}"
        _lastKeyStr.value = "[$keyName] $action $meta"
        
        wsClient.sendRawKey(keyName, action)
        return true
    }

    fun handleMouseDelta(dx: Float, dy: Float) {
        if (connectionState.value is ConnectionState.Connected) {
            wsClient.sendMouseRelative(dx.toInt(), dy.toInt())
        }
    }

    fun handleMouseClick(button: Int) {
        if (connectionState.value is ConnectionState.Connected) {
            wsClient.sendMouseClick(button)
        }
    }

    fun handleScroll(scrollDeltaY: Float) {
        if (connectionState.value is ConnectionState.Connected) {
            if (scrollDeltaY < 0) wsClient.sendScroll("up", 1)
            else if (scrollDeltaY > 0) wsClient.sendScroll("down", 1)
        }
    }
}
