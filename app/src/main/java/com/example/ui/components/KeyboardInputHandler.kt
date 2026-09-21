package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalThemeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * D-Pad yukarı/aşağı navigasyonu ve sayısal tuş takımı (0-9) ile doğrudan kanal numarasına
 * odaklanıp geçiş yapmayı sağlayan klavye / kumanda girdi yöneticisi.
 */
class KeyboardInputHandlerState(
    private val scope: CoroutineScope,
    private val onNavigateUp: () -> Unit,
    private val onNavigateDown: () -> Unit,
    private val onNumberCommitted: (Int) -> Unit
) {
    var numericInput by mutableStateOf("")
        private set

    var isHUDVisible by mutableStateOf(false)
        private set

    private var commitJob: Job? = null

    fun handleKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        val nativeEvent = keyEvent.nativeKeyEvent
        val keyCode = nativeEvent.keyCode

        // D-Pad Yukarı / Channel UP
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
            if (numericInput.isNotEmpty()) {
                commitCurrentNumber()
            } else {
                onNavigateUp()
            }
            return true
        }

        // D-Pad Aşağı / Channel DOWN
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            if (numericInput.isNotEmpty()) {
                commitCurrentNumber()
            } else {
                onNavigateDown()
            }
            return true
        }

        // Sayı Tuşları: 0..9 (KeyEvent.KEYCODE_0=7 .. KEYCODE_9=16)
        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            val digit = (keyCode - KeyEvent.KEYCODE_0).toString()
            appendDigit(digit)
            return true
        }

        // Numpad Sayı Tuşları: NUMPAD_0=144 .. NUMPAD_9=153
        if (keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9) {
            val digit = (keyCode - KeyEvent.KEYCODE_NUMPAD_0).toString()
            appendDigit(digit)
            return true
        }

        // Enter / DPAD Center: Eğer sayı giriliyorsa doğrudan o kanala git
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (numericInput.isNotEmpty()) {
                commitCurrentNumber()
                return true
            }
        }

        // Back: Sayı giriliyorsa iptal et
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (numericInput.isNotEmpty()) {
                clear()
                return true
            }
        }

        return false
    }

    private fun appendDigit(digit: String) {
        if (numericInput.length >= 4) return // En fazla 4 basamaklı kanal
        numericInput += digit
        isHUDVisible = true

        commitJob?.cancel()
        commitJob = scope.launch {
            // 1.5 saniye bekle, yeni sayı girilmezse o kanala geçiş yap
            delay(1500)
            commitCurrentNumber()
        }
    }

    private fun commitCurrentNumber() {
        commitJob?.cancel()
        val num = numericInput.toIntOrNull()
        clear()
        if (num != null && num > 0) {
            onNumberCommitted(num)
        }
    }

    fun clear() {
        commitJob?.cancel()
        numericInput = ""
        isHUDVisible = false
    }
}

@Composable
fun rememberKeyboardInputHandler(
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onNumberCommitted: (Int) -> Unit
): KeyboardInputHandlerState {
    val scope = rememberCoroutineScope()
    return remember(onNavigateUp, onNavigateDown, onNumberCommitted) {
        KeyboardInputHandlerState(
            scope = scope,
            onNavigateUp = onNavigateUp,
            onNavigateDown = onNavigateDown,
            onNumberCommitted = onNumberCommitted
        )
    }
}

/**
 * Sayı girildiğinde ekranda beliren minimalist ve şık Kanal Numarası HUD göstergesi
 */
@Composable
fun ChannelNumberHUD(
    state: KeyboardInputHandlerState,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemeConfig.current.palette

    AnimatedVisibility(
        visible = state.isHUDVisible && state.numericInput.isNotEmpty(),
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.95f)),
            border = BorderStroke(1.5.dp, palette.secondary),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kanal Git:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = state.numericInput,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = palette.secondary
                )
            }
        }
    }
}
