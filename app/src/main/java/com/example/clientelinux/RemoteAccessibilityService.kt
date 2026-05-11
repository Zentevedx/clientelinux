package com.example.clientelinux

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteAccessibilityService : AccessibilityService() {

    companion object {
        private val _keyEvents = MutableSharedFlow<KeyEvent>(
            extraBufferCapacity = 64,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
        )
        val keyEvents = _keyEvents.asSharedFlow()

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        var isIntercepting = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        _isServiceActive.value = false
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No necesitamos procesar eventos de accesibilidad UI, solo teclado
    }

    override fun onInterrupt() {
        // Requerido por la interfaz
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (isIntercepting) {
            // Evitar spam de auto-repeat, dejamos que Linux haga el auto-repeat natural
            if (event.repeatCount == 0 || event.action == KeyEvent.ACTION_UP) {
                _keyEvents.tryEmit(event)
            }
            return true // Consume el evento
        }
        return super.onKeyEvent(event)
    }
}
