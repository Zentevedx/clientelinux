package com.example.clientelinux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Paleta de colores ───────────────────────────────────────────────────────
private val BgDark     = Color(0xFF0D1117)
private val BgCard     = Color(0xFF161B22)
private val Accent     = Color(0xFF58A6FF)
private val AccentGlow = Color(0xFF1F6FEB)
private val TextPrim   = Color(0xFFE6EDF3)
private val TextSecond = Color(0xFF8B949E)
private val Success    = Color(0xFF3FB950)
private val Danger     = Color(0xFFF85149)

/**
 * Pantalla de conexión — permite ingresar IP Tailscale y token, luego conectar.
 */
@Composable
fun ConnectScreen(
    connectionState: ConnectionState,
    onConnect: (ip: String, token: String) -> Unit,
    onDisconnect: () -> Unit,
) {
    // Valores por defecto precargados para el usuario
    var ip by remember { mutableStateOf("100.107.167.88") }
    var token by remember { mutableStateOf("cambia-este-token-secreto") }
    var showToken by remember { mutableStateOf(false) }

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting
    val errorMsg = (connectionState as? ConnectionState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.Center,
    ) {
        // Fondo con gradiente sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D2137), BgDark),
                        radius = 900f,
                    )
                )
        )

        Card(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ─── Header ──────────────────────────────────────────────────
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🖥️",
                        fontSize = 48.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cliente Linux",
                        color = TextPrim,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Control remoto vía Tailscale",
                        color = TextSecond,
                        fontSize = 13.sp,
                    )
                }

                HorizontalDivider(color = Color(0xFF30363D))

                // ─── Estado de conexión ───────────────────────────────────────
                StatusChip(connectionState)

                // ─── Campos de entrada ────────────────────────────────────────
                AnimatedVisibility(!isConnected) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // IP / Host
                        RemoteTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = "IP Tailscale",
                            placeholder = "100.x.x.x",
                            keyboardType = KeyboardType.Uri,
                            enabled = !isConnecting,
                        )

                        // Token secreto
                        RemoteTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = "Token de acceso",
                            placeholder = "tu-token-secreto",
                            keyboardType = KeyboardType.Password,
                            visualTransformation = if (showToken)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showToken = !showToken }) {
                                    Text(
                                        if (showToken) "Ocultar" else "Ver",
                                        color = Accent,
                                        fontSize = 12.sp,
                                    )
                                }
                            },
                            enabled = !isConnecting,
                        )
                    }
                }

                // ─── Error ────────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = errorMsg != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    errorMsg?.let {
                        Surface(
                            color = Danger.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "⚠️ $it",
                                color = Danger,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                // ─── Botón principal ──────────────────────────────────────────
                Button(
                    onClick = {
                        if (isConnected) onDisconnect()
                        else onConnect(ip.trim(), token.trim())
                    },
                    enabled = !isConnecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Danger else AccentGlow,
                    ),
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Conectando…", color = Color.White, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(
                            if (isConnected) "Desconectar" else "Conectar",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                // ─── Info de ayuda ────────────────────────────────────────────
                if (!isConnected && !isConnecting) {
                    Text(
                        text = "Puerto: 8765 · Protocolo: WebSocket",
                        color = TextSecond,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

// ─── Chip de estado ──────────────────────────────────────────────────────────

@Composable
private fun StatusChip(state: ConnectionState) {
    val (color, label) = when (state) {
        is ConnectionState.Connected    -> Pair(Success, "● Conectado")
        is ConnectionState.Connecting   -> Pair(Color(0xFFE3B341), "◌ Conectando…")
        is ConnectionState.Error        -> Pair(Danger, "✕ Error")
        is ConnectionState.Disconnected -> Pair(TextSecond, "○ Desconectado")
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

// ─── TextField personalizado ─────────────────────────────────────────────────

@Composable
private fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecond) },
        placeholder = { Text(placeholder, color = TextSecond.copy(alpha = 0.5f)) },
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Color(0xFF30363D),
            focusedTextColor = TextPrim,
            unfocusedTextColor = TextPrim,
            cursorColor = Accent,
            disabledTextColor = TextSecond,
            disabledBorderColor = Color(0xFF30363D),
        ),
        shape = RoundedCornerShape(12.dp),
    )
}
