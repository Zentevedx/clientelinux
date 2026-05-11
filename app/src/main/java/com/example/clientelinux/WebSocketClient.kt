package com.example.clientelinux

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONObject

/**
 * Estados posibles de la conexión WebSocket.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Cliente WebSocket que se comunica con el servidor Python.
 * Usa OkHttp internamente y expone StateFlows para observar desde Compose.
 */
class RemoteWebSocketClient {

    private val TAG = "WSClient"

    private var socket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // ─── Flows observables ───────────────────────────────────────────────────
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state

    private val _frame = MutableStateFlow<ByteArray?>(null)
    val frame: StateFlow<ByteArray?> = _frame

    // ─── Conexión ────────────────────────────────────────────────────────────

    fun connect(ip: String, port: Int = 8765, token: String, resolution: String = "1080p") {
        if (_state.value is ConnectionState.Connected || _state.value is ConnectionState.Connecting) return
        _state.value = ConnectionState.Connecting

        val resVal = if (resolution == "720p") "720" else "1080"
        val url = "ws://$ip:$port/ws?token=$token&resolution=$resVal"
        Log.i(TAG, "Conectando a $url")

        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "✅ Conectado")
                _state.value = ConnectionState.Connected
                // Iniciar stream de frames inmediatamente
                send("""{"type":"start_stream"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "frame" -> {
                            val b64 = json.getString("data")
                            _frame.value = Base64.decode(b64, Base64.DEFAULT)
                        }
                        "error" -> Log.e(TAG, "Error del servidor: ${json.getString("message")}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando mensaje: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ Fallo: ${t.message}")
                _state.value = ConnectionState.Error(t.message ?: "Error desconocido")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Desconectado ($code): $reason")
                _state.value = ConnectionState.Disconnected
            }
        })
    }

    fun disconnect() {
        send("""{"type":"stop_stream"}""")
        socket?.close(1000, "Usuario desconectó")
        socket = null
        _state.value = ConnectionState.Disconnected
    }

    // ─── Envío de input ──────────────────────────────────────────────────────

    fun sendMouseMove(x: Int, y: Int) =
        send("""{"type":"mouse_move","x":$x,"y":$y}""")

    fun sendMouseRelative(dx: Int, dy: Int) =
        send("""{"type":"mouse_move_rel","dx":$dx,"dy":$dy}""")

    fun sendMouseClick(button: Int = 1) =
        send("""{"type":"mouse_click","button":$button}""")

    fun sendScroll(direction: String, amount: Int = 3) =
        send("""{"type":"mouse_scroll","direction":"$direction","amount":$amount}""")

    fun sendKey(combo: String) =
        send("""{"type":"key","combo":"$combo"}""")

    fun sendRawKey(key: String, action: String) =
        send("""{"type":"raw_key","key":"$key","action":"$action"}""")

    fun sendText(text: String) {
        val escaped = text.replace("\"", "\\\"")
        send("""{"type":"type","text":"$escaped"}""")
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private fun send(json: String) {
        val ok = socket?.send(json) ?: false
        if (!ok) Log.w(TAG, "No se pudo enviar: $json")
    }
}
