package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.IPTVChannel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeConfig
import com.example.ui.theme.AppSurfaceTextureBox
import com.example.ui.theme.SurfaceBlue
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraPurple
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    Home,
    Player,
    Search,
    Detail,
    AddPlaylist
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var activeScreen = Screen.Home
    private val isInPiPMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val palette by viewModel.themePalette.collectAsState()
            val texture by viewModel.backgroundTexture.collectAsState()
            val themeConfig = remember(palette, texture) { ThemeConfig(palette, texture) }

            MyApplicationTheme(themeConfig = themeConfig) {
                AppSurfaceTextureBox(palette = palette, texture = texture) {
                    var currentScreen by remember { mutableStateOf(Screen.Home) }
                    var selectedDetailChannel by remember { mutableStateOf<IPTVChannel?>(null) }
                    var showExitDialog by remember { mutableStateOf(false) }
                    val isPipActive by isInPiPMode

                    val targetScreen = if (isPipActive) Screen.Player else currentScreen

                LaunchedEffect(targetScreen) {
                    activeScreen = targetScreen
                }

                val context = LocalContext.current

                // Listen to UI notifications/toasts (Failsafe custom TV notification)
                var activeNotification by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    viewModel.uiNotification.collectLatest { msg ->
                        activeNotification = msg
                        // Automatically clear notification after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        activeNotification = null
                    }
                }

                // Handle global D-pad BACK button logic
                val scope = rememberCoroutineScope()
                var backPressCount by remember { mutableStateOf(0) }
                var backPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                BackHandler {
                    when (currentScreen) {
                        Screen.Player -> {
                            backPressCount++
                            if (backPressCount == 2) {
                                backPressJob?.cancel()
                                backPressCount = 0
                                viewModel.handleDoubleBackRecall()
                            } else {
                                backPressJob = scope.launch {
                                    kotlinx.coroutines.delay(400) // 400ms window
                                    if (backPressCount == 1) {
                                        backPressCount = 0
                                        viewModel.stopPlayback()
                                        currentScreen = Screen.Home
                                    }
                                }
                            }
                        }
                        Screen.Search, Screen.Detail, Screen.AddPlaylist -> {
                            currentScreen = Screen.Home
                        }
                        Screen.Home -> {
                            showExitDialog = true
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Screen state transitions using high performance custom AnimatedContent
                    AnimatedContent(
                        targetState = targetScreen,
                        transitionSpec = {
                            if (targetState == Screen.Player) {
                                // Premium slide up + scale in on player launch
                                (slideInVertically(animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f)) { it } + 
                                 fadeIn(animationSpec = tween(250)) +
                                 scaleIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f), initialScale = 0.95f))
                                    .togetherWith(
                                        scaleOut(animationSpec = tween(200), targetScale = 0.95f) + 
                                        fadeOut(animationSpec = tween(200))
                                    )
                            } else if (initialState == Screen.Player) {
                                // Premium slide down + scale out when exiting player back to home
                                (scaleIn(animationSpec = tween(250), initialScale = 0.95f) + 
                                 fadeIn(animationSpec = tween(250)))
                                    .togetherWith(
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.88f, stiffness = 220f)) { it } + 
                                        fadeOut(animationSpec = tween(200))
                                    )
                            } else {
                                // Soft, elegant dynamic fade & slight scale for other standard screen shifts
                                (fadeIn(animationSpec = tween(250)) + 
                                 scaleIn(animationSpec = tween(250), initialScale = 0.98f))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(200))
                                    )
                            }
                        },
                        label = "screen_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            Screen.Home -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    isActive = currentScreen == Screen.Home,
                                    onNavigateToPlayer = { currentScreen = Screen.Player },
                                    onNavigateToSearch = { currentScreen = Screen.Search },
                                    onNavigateToDetail = { channel ->
                                        selectedDetailChannel = channel
                                        currentScreen = Screen.Detail
                                    },
                                    onNavigateToAddPlaylist = { currentScreen = Screen.AddPlaylist }
                                )
                            }
                            Screen.Player -> {
                                PlayerScreen(
                                    viewModel = viewModel,
                                    isInPictureInPicture = isPipActive,
                                    onNavigateBack = {
                                        viewModel.stopPlayback()
                                        currentScreen = Screen.Home
                                    }
                                )
                            }
                            Screen.Search -> {
                                SearchScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.Home },
                                    onChannelSelect = { channel ->
                                        viewModel.selectChannel(channel)
                                        currentScreen = Screen.Player
                                    }
                                )
                            }
                            Screen.Detail -> {
                                selectedDetailChannel?.let { channel ->
                                    DetailScreen(
                                        channel = channel,
                                        viewModel = viewModel,
                                        onPlayClick = { chan ->
                                            viewModel.selectChannel(chan)
                                            currentScreen = Screen.Player
                                        },
                                        onNavigateBack = { currentScreen = Screen.Home }
                                    )
                                }
                            }
                            Screen.AddPlaylist -> {
                                AddPlaylistScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.Home }
                                )
                            }
                        }
                    }

                    // Failsafe Custom TV-Snackbar Notification Overlay
                    AnimatedVisibility(
                        visible = activeNotification != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp)
                    ) {
                        activeNotification?.let { msg ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AuroraPurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    // Numeric Input Overlay
                    val numericInput by viewModel.numericInput.collectAsState()
                    AnimatedVisibility(
                        visible = numericInput.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(32.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AuroraPurple),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = "Kanal Git: ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                                Text(
                                    text = numericInput,
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                    color = AuroraCyan
                                )
                            }
                        }
                    }

                    // Exit Application Dialog (D-pad Focusable)
                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("Çıkış", color = AuroraCyan) },
                            text = { Text("AuroraTV uygulamasından çıkmak istiyor musunuz?", color = Color.White) },
                            confirmButton = {
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple),
                                    modifier = Modifier.testTag("exit_confirm_button")
                                ) {
                                    Text("Evet")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showExitDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                                    modifier = Modifier.testTag("exit_dismiss_button")
                                ) {
                                    Text("Hayır")
                                }
                            },
                            containerColor = SurfaceBlue,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (activeScreen == Screen.Player && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val params = android.app.PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPiPMode.value = isInPictureInPictureMode
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Red (183), Green (184), Yellow (185), Blue (186)
        // Also support F1-F4 keys as fallbacks for development/emulators: F1 (131), F2 (132), F3 (133), F4 (134)
        when (keyCode) {
            183, 131 -> {
                viewModel.handleColorKey("RED")
                return true
            }
            184, 132 -> {
                viewModel.handleColorKey("GREEN")
                return true
            }
            185, 133 -> {
                viewModel.handleColorKey("YELLOW")
                return true
            }
            186, 134 -> {
                viewModel.handleColorKey("BLUE")
                return true
            }
            // Up Arrow / DPAD UP: Switch channel/content in player
            19 -> {
                if (activeScreen == Screen.Player) {
                    viewModel.playPreviousChannel()
                    return true
                }
            }
            // Down Arrow / DPAD DOWN: Switch channel/content in player
            20 -> {
                if (activeScreen == Screen.Player) {
                    viewModel.playNextChannel()
                    return true
                }
            }
            // F Key (Favorite / Favori)
            34 -> {
                viewModel.toggleCurrentChannelFavorite()
                return true
            }
            // Number keys: KEYCODE_0 (7) to KEYCODE_9 (16)
            in 7..16 -> {
                val digit = (keyCode - 7).toString()
                viewModel.handleNumericKeyPress(digit)
                return true
            }
            // Number pad keys: KEYCODE_NUMPAD_0 (144) to KEYCODE_NUMPAD_9 (153)
            in 144..153 -> {
                val digit = (keyCode - 144).toString()
                viewModel.handleNumericKeyPress(digit)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
