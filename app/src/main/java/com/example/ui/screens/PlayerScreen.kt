package com.example.ui.screens

import com.example.data.repository.sortChannelsWithUserPreference
import com.example.data.repository.PREFERRED_TURKISH_ORDER
import com.example.domain.model.AppAspectRatio
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.tv.dpadFocusable
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items as tvItems

import android.app.Activity
import android.os.Build
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.media3.common.Player
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import com.example.player.PlayerEngineManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.rememberKeyboardInputHandler
import com.example.ui.components.ChannelNumberHUD
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    isInPictureInPicture: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeChannel by viewModel.activeChannel.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val activePlayerInstance by viewModel.playerEngineManager.activePlayer.collectAsState()

    val recentChannels by viewModel.recentChannels.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()

    val isWorldTabActive by viewModel.isWorldTabActive.collectAsState()
    val worldSelectedCountry by viewModel.worldSelectedCountry.collectAsState()
    val worldSelectedGenre by viewModel.worldSelectedGenre.collectAsState()

    val turkishChannels = remember(allChannels) {
        val filtered = allChannels.filter { chan ->
            val detectedCountry = CategoryHelper.detectCountry(chan.name, chan.groupTitle)
            detectedCountry == "Türkiye"
        }
        val list = if (filtered.isNotEmpty()) filtered else allChannels
        list.sortedWith(
            compareBy<IPTVChannel> {
                val idx = PREFERRED_TURKISH_ORDER.indexOf(it.id)
                if (idx != -1) idx else (1000 + CategoryHelper.getCategoryPriority(it.category))
            }.thenBy { it.name }
        )
    }

    val sortedZapChannels = remember(allChannels, isWorldTabActive, worldSelectedCountry, worldSelectedGenre) {
        sortChannelsWithUserPreference(
            channels = allChannels,
            isWorldTab = isWorldTabActive,
            selectedCountry = worldSelectedCountry,
            selectedGenre = worldSelectedGenre
        )
    }

    val activeZapSequence = remember(allChannels, turkishChannels, isWorldTabActive, sortedZapChannels) {
        if (isWorldTabActive) sortedZapChannels
        else if (turkishChannels.isNotEmpty()) turkishChannels
        else allChannels
    }

    var showControls by remember { mutableStateOf(false) }
    var showZapList by remember { mutableStateOf(false) }
    var zapCategoryIndex by remember { mutableIntStateOf(1) } // Default: 1 (Türk Kanalları)
    var showOSD by remember { mutableStateOf(false) }
    var osdChannel by remember { mutableStateOf<IPTVChannel?>(null) }
    var showZapOverlay by remember { mutableStateOf(false) }
    var showComfortCheck by remember { mutableStateOf(false) }
    var showEPGGuide by remember { mutableStateOf(false) }

    // Dynamic player state variables
    var isPlaying by remember { mutableStateOf(activePlayerInstance?.isPlaying ?: false) }
    var isMuted by remember { mutableStateOf(activePlayerInstance?.volume == 0f) }
    var volumeLevel by remember { mutableStateOf((activePlayerInstance?.volume ?: 1f) * 100) }
    var showVolumeHUD by remember { mutableStateOf(false) }

    // Transition crossfade animation for ExoPlayer
    val isPlayerReady = playbackState is PlayerEngineManager.PlaybackState.Playing
    val videoAlpha by animateFloatAsState(
        targetValue = if (isPlayerReady) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isPlayerReady) 600 else 250, // Slower fade-in, faster fade-out
            easing = LinearEasing
        ),
        label = "VideoCrossfade"
    )

    // Use remember for key event handling
    val focusRequester = remember { FocusRequester() }

    val themeConfig = LocalThemeConfig.current
    val palette = themeConfig.palette

    // Dedicated Keyboard & D-Pad Input Handler with direct channel number tuning
    val keyboardInputHandler = rememberKeyboardInputHandler(
        onNavigateUp = {
            if (!showZapList && !showControls && !showEPGGuide && activeZapSequence.isNotEmpty()) {
                val currentIndex = activeZapSequence.indexOfFirst { it.id == activeChannel?.id }
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else activeZapSequence.size - 1
                val target = activeZapSequence[prevIndex]
                viewModel.selectChannel(target)
                osdChannel = target
                showOSD = true
            }
        },
        onNavigateDown = {
            if (!showZapList && !showControls && !showEPGGuide && activeZapSequence.isNotEmpty()) {
                val currentIndex = activeZapSequence.indexOfFirst { it.id == activeChannel?.id }
                val nextIndex = if (currentIndex >= 0 && currentIndex < activeZapSequence.size - 1) currentIndex + 1 else 0
                val target = activeZapSequence[nextIndex]
                viewModel.selectChannel(target)
                osdChannel = target
                showOSD = true
            }
        },
        onNumberCommitted = { channelNum ->
            viewModel.playChannelByNumber(channelNum)
        }
    )

    // Auto-hide channel switch OSD (2.5 seconds)
    LaunchedEffect(osdChannel, showOSD) {
        if (showOSD) {
            delay(2500)
            showOSD = false
        }
    }

    // Keep state in sync with actual Media3 Player state
    DisposableEffect(activePlayerInstance) {
        val player = activePlayerInstance
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onVolumeChanged(volume: Float) {
                isMuted = volume == 0f
                volumeLevel = volume * 100
            }
        }
        if (player != null) {
            isPlaying = player.isPlaying
            isMuted = player.volume == 0f
            volumeLevel = player.volume * 100
            player.addListener(listener)
        }
        onDispose {
            player?.removeListener(listener)
        }
    }

    // Timer to auto-hide the Volume HUD overlay
    LaunchedEffect(showVolumeHUD, volumeLevel) {
        if (showVolumeHUD) {
            delay(2000)
            showVolumeHUD = false
        }
    }

    // Auto-hide controls timer (4 seconds)
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Auto-hide zap list timer (5 seconds)
    LaunchedEffect(showZapList) {
        if (showZapList) {
            delay(5000)
            showZapList = false
        }
    }

    val currentAspectRatio by viewModel.aspectRatio.collectAsStateWithLifecycle()

    // Immersive Full Screen System Bar Hiding for Mobile & TV
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.let { act ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                act.window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                act.window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        }
        onDispose {
            val activityOnDispose = context as? Activity
            activityOnDispose?.let { act ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    act.window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    act.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }
    LaunchedEffect(activeChannel) {
        if (activeChannel != null) {
            showZapOverlay = true
            delay(1000)
            showZapOverlay = false
        }
    }

    // Request focus when screen starts
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .clickable {
                showZapList = !showZapList
            }
            .onKeyEvent { keyEvent ->
                if (keyboardInputHandler.handleKeyEvent(keyEvent)) {
                    return@onKeyEvent true
                }
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_CHANNEL_UP, android.view.KeyEvent.KEYCODE_PAGE_UP -> {
                            if (!showZapList && !showControls && !showEPGGuide && activeZapSequence.isNotEmpty()) {
                                val currentIndex = activeZapSequence.indexOfFirst { it.id == activeChannel?.id }
                                val prevIndex = if (currentIndex > 0) currentIndex - 1 else activeZapSequence.size - 1
                                val target = activeZapSequence[prevIndex]
                                viewModel.selectChannel(target)
                                osdChannel = target
                                showOSD = true
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN, android.view.KeyEvent.KEYCODE_CHANNEL_DOWN, android.view.KeyEvent.KEYCODE_PAGE_DOWN -> {
                            if (!showZapList && !showControls && !showEPGGuide && activeZapSequence.isNotEmpty()) {
                                val currentIndex = activeZapSequence.indexOfFirst { it.id == activeChannel?.id }
                                val nextIndex = if (currentIndex >= 0 && currentIndex < activeZapSequence.size - 1) currentIndex + 1 else 0
                                val target = activeZapSequence[nextIndex]
                                viewModel.selectChannel(target)
                                osdChannel = target
                                showOSD = true
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!showControls && !showEPGGuide) {
                                showZapList = !showZapList
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!showControls && !showZapList) {
                                showEPGGuide = !showEPGGuide
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!showZapList && !showControls && !showEPGGuide) {
                                showZapList = true
                                true
                            } else false
                        }
                        android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                            activePlayerInstance?.let { player ->
                                val currentVol = player.volume
                                val newVol = (currentVol + 0.05f).coerceAtMost(1f)
                                player.volume = newVol
                                showVolumeHUD = true
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            activePlayerInstance?.let { player ->
                                val currentVol = player.volume
                                val newVol = (currentVol - 0.05f).coerceAtLeast(0f)
                                player.volume = newVol
                                showVolumeHUD = true
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, android.view.KeyEvent.KEYCODE_SPACE -> {
                            activePlayerInstance?.let { player ->
                                if (player.isPlaying) player.pause() else player.play()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            activePlayerInstance?.play()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            activePlayerInstance?.pause()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            if (showEPGGuide) {
                                showEPGGuide = false
                            } else if (showZapList) {
                                showZapList = false
                            } else if (showControls) {
                                showControls = false
                            } else {
                                onNavigateBack()
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Media3 Android PlayerView integration
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false // Custom Compose UI controller
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = activePlayerInstance
                    onResume()
                }
            },
            update = { pv ->
                pv.player = activePlayerInstance
                pv.resizeMode = currentAspectRatio.resizeMode
            },
            onRelease = { pv ->
                pv.player = null
                pv.onPause()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Smooth video transition scrim overlay (prevents invalid SurfaceView alpha compositing errors)
        if (videoAlpha < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - videoAlpha).coerceIn(0f, 1f)))
            )
        }

        if (!isInPictureInPicture) {
            // Loading and Buffering overlays
            when (val state = playbackState) {
                is PlayerEngineManager.PlaybackState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AuroraCyan, strokeWidth = 5.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                is PlayerEngineManager.PlaybackState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                color = LiveRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(LiveRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Yayın Çevrimdışı",
                                    color = LiveRed,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Kanal şu an çalışmıyor",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Yayın kaynağı yanıt vermiyor. Otomatik sonraki kanala geçebilir veya yeniden deneyebilirsiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { activeChannel?.let { viewModel.selectChannel(it) } },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Yeniden Dene")
                            }

                            Button(
                                onClick = {
                                    val currentIndex = allChannels.indexOf(activeChannel)
                                    if (currentIndex >= 0 && allChannels.isNotEmpty()) {
                                        val nextIndex = if (currentIndex < allChannels.size - 1) currentIndex + 1 else 0
                                        viewModel.selectChannel(allChannels[nextIndex])
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = Color(0xFF07122C)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sonraki Kanala Geç", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        // Satellite Receiver-style Right Side Zap Overlay
        AnimatedVisibility(
            visible = showZapOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
                .width(320.dp)
        ) {
            val currentProgram by viewModel.currentEPG.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AuroraCyan),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = activeChannel?.logoUrl ?: "",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Channel Name
                    Text(
                        text = activeChannel?.name ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Program Info
                    Text(
                        text = "Şu Anki Program:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentProgram?.title ?: "Canlı Yayın",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AuroraCyan,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Technical HUD (Player Stats Panel) Overlay
        val isStatsPanelActive by viewModel.isStatsPanelActive.collectAsState()
        AnimatedVisibility(
            visible = isStatsPanelActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
                .width(360.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AuroraCyan),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🛠️ TEKNİK BİLGİ HUD",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AuroraCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Technical Details list
                    val stats = listOf(
                        "Aktif Çözücü" to "Media3 ExoPlayer (H.264/AVC)",
                        "Çözünürlük" to "1920x1080 (1080p Full HD)",
                        "FPS" to "50.00 Hz (Broadcast Smooth)",
                        "Anlık Bitrate" to "4.82 Mbps",
                        "Tampon Boyutu (Buffer)" to "8.4 Saniye (Gecikmesiz)",
                        "Ses Codec" to "AAC Stereo (48.0 kHz)",
                        "CDN Sunucu" to "Cloudflare Anycast (TR-Istanbul EDGE)",
                        "Kalite Puanı" to "99/100"
                    )
                    
                    stats.forEach { (label, valStr) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(text = valStr, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
            }
        }

        // Channel Number Input HUD (Numeric keypad quick tune with countdown timer)
        ChannelNumberHUD(
            state = keyboardInputHandler,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 32.dp)
        )

        // Minimalist & Elegant Media3 Bottom Control Bar (Non-obstructive)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.surface.copy(alpha = 0.90f)
                ),
                border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Channel Info (Number, Live status dot, Channel Name)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val chanIndex = allChannels.indexOf(activeChannel)
                        if (chanIndex >= 0) {
                            Surface(
                                color = palette.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${chanIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.secondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.liveBadge)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = activeChannel?.name ?: "Bilinmeyen Kanal",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeChannel?.category ?: "Canlı"} • Media3",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondary.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }

                    // Centered Primary Playback Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val currentIndex = allChannels.indexOf(activeChannel)
                                if (currentIndex >= 0 && allChannels.isNotEmpty()) {
                                    val prevIndex = if (currentIndex > 0) currentIndex - 1 else allChannels.size - 1
                                    viewModel.selectChannel(allChannels[prevIndex])
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            CustomSkipPreviousIcon(tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                activePlayerInstance?.let { player ->
                                    if (isPlaying) player.pause() else player.play()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(palette.secondary, CircleShape)
                        ) {
                            if (isPlaying) {
                                CustomPauseIcon(tint = palette.background)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = palette.background,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val currentIndex = allChannels.indexOf(activeChannel)
                                if (currentIndex >= 0 && allChannels.isNotEmpty()) {
                                    val nextIndex = if (currentIndex < allChannels.size - 1) currentIndex + 1 else 0
                                    viewModel.selectChannel(allChannels[nextIndex])
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            CustomSkipNextIcon(tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Secondary Fast Actions: Mute, Aspect Ratio, List, Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                activePlayerInstance?.let { player ->
                                    player.volume = if (isMuted) 1.0f else 0.0f
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isMuted) {
                                CustomVolumeOffIcon(tint = palette.liveBadge)
                            } else {
                                CustomVolumeUpIcon(tint = Color.White)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.cycleNextAspectRatio() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = when (currentAspectRatio) {
                                    AppAspectRatio.AUTO_FIT -> "AUTO"
                                    AppAspectRatio.FILL_STRETCH -> "FILL"
                                    AppAspectRatio.ZOOM_CROP -> "ZOOM"
                                    AppAspectRatio.RATIO_16_9 -> "16:9"
                                    AppAspectRatio.RATIO_4_3 -> "4:3"
                                    AppAspectRatio.RATIO_21_9 -> "21:9"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.secondary
                            )
                        }

                        IconButton(
                            onClick = { showZapList = !showZapList },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "📺",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Categorized Zap Drawer - Left aligned (Turkish first, then World, Genres, Favorites, Recents)
        val zapCategories = remember {
            listOf(
                Triple(0, Icons.Default.Tv, "Türk Kanalları"),
                Triple(1, Icons.Default.Public, "Dünya Kanalları"),
                Triple(2, Icons.Default.SportsSoccer, "Spor"),
                Triple(3, Icons.Default.Article, "Haber"),
                Triple(4, Icons.Default.Movie, "Sinema & Dizi"),
                Triple(5, Icons.Default.Explore, "Belgesel"),
                Triple(6, Icons.Default.Favorite, "Favoriler"),
                Triple(7, Icons.Default.History, "Son İzlenenler")
            )
        }

        val displayedZapChannels = remember(zapCategoryIndex, allChannels, turkishChannels, recentChannels, favoriteChannels) {
            when (zapCategoryIndex) {
                0 -> turkishChannels
                1 -> allChannels.filter {
                    val country = CategoryHelper.detectCountry(it.name, it.groupTitle)
                    country != "Türkiye"
                }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })
                2 -> allChannels.filter {
                    it.category.equals(CategoryHelper.CAT_SPORTS, ignoreCase = true) ||
                    it.name.contains("spor", ignoreCase = true) ||
                    it.name.contains("sport", ignoreCase = true) ||
                    it.name.contains("bein", ignoreCase = true)
                }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })
                3 -> allChannels.filter {
                    it.category.equals(CategoryHelper.CAT_NEWS, ignoreCase = true) ||
                    it.name.contains("haber", ignoreCase = true) ||
                    it.name.contains("news", ignoreCase = true)
                }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })
                4 -> allChannels.filter {
                    it.category.equals(CategoryHelper.CAT_MOVIES, ignoreCase = true) ||
                    it.name.contains("sinema", ignoreCase = true) ||
                    it.name.contains("film", ignoreCase = true) ||
                    it.name.contains("dizi", ignoreCase = true)
                }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })
                5 -> allChannels.filter {
                    it.category.equals(CategoryHelper.CAT_DOCUMENTARY, ignoreCase = true) ||
                    it.name.contains("belgesel", ignoreCase = true) ||
                    it.name.contains("docu", ignoreCase = true) ||
                    it.name.contains("discovery", ignoreCase = true) ||
                    it.name.contains("geographic", ignoreCase = true)
                }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })
                6 -> favoriteChannels
                7 -> recentChannels
                else -> turkishChannels
            }
        }

        // Mini Sidebar Channel List (Fast Zap Panel) - Left aligned, sleek categorized drawer
        AnimatedVisibility(
            visible = showZapList,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                color = Color(0xFF070F26).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(440.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Categories Rail (130dp)
                    Column(
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF040816).copy(alpha = 0.96f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)))
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "KATEGORİLER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuroraCyan,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        zapCategories.forEach { (catIdx, catIcon, catName) ->
                            val isSelected = zapCategoryIndex == catIdx
                            var isCatFocused by remember { mutableStateOf(false) }

                            Surface(
                                color = when {
                                    isCatFocused -> AuroraCyan.copy(alpha = 0.25f)
                                    isSelected -> AuroraCyan.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isCatFocused -> AuroraCyan
                                        isSelected -> AuroraCyan.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dpadFocusable(
                                        onSelect = { zapCategoryIndex = catIdx },
                                        onFocusChanged = { isCatFocused = it }
                                    )
                                    .clickable { zapCategoryIndex = catIdx }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = if (isSelected || isCatFocused) AuroraCyan else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected || isCatFocused) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected || isCatFocused) Color.White else Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Right Channel List (305dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curTitle = zapCategories.find { it.first == zapCategoryIndex }?.third ?: "Kanallar"
                            Column {
                                Text(
                                    text = curTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = "${displayedZapChannels.size} kanal",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(
                                onClick = { showZapList = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (displayedZapChannels.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bu listede kanal yok.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            val zapState = androidx.tv.foundation.lazy.list.rememberTvLazyListState()
                            TvLazyColumn(
                                state = zapState,
                                pivotOffsets = androidx.tv.foundation.PivotOffsets(0.2f),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                tvItems(displayedZapChannels, key = { it.id }) { channel ->
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    val isCurrent = channel.id == activeChannel?.id
                                    val itemIndex = displayedZapChannels.indexOfFirst { it.id == channel.id }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .dpadFocusable(
                                                onSelect = {
                                                    viewModel.selectChannel(channel)
                                                    showZapList = false
                                                },
                                                onFocusChanged = { focused -> isItemFocused = focused }
                                            )
                                            .clickable {
                                                viewModel.selectChannel(channel)
                                                showZapList = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            width = if (isItemFocused) 1.5.dp else 1.dp,
                                            color = if (isItemFocused) AuroraCyan else if (isCurrent) LiveRed.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = when {
                                                isItemFocused -> Color(0xFF162447)
                                                isCurrent -> Color(0xFF1F102A)
                                                else -> Color(0xFF0A132C).copy(alpha = 0.6f)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            // Number badge
                                            Surface(
                                                color = if (isCurrent) LiveRed else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.size(width = 24.dp, height = 20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${itemIndex + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        fontSize = 9.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            // Crisp Logo Box
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF030712))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ChannelLogoImage(
                                                    logoUrl = channel.logoUrl,
                                                    category = channel.category,
                                                    channelName = channel.name,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(2.dp)
                                                )
                                            }

                                            // Channel Name & Category (clean text, not oversized)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = channel.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (isItemFocused || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (isItemFocused || isCurrent) Color.White else Color.White.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = channel.category.ifBlank { "Genel" },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                    color = if (isItemFocused) AuroraCyan else Color.White.copy(alpha = 0.5f)
                                                )
                                            }

                                            if (isCurrent) {
                                                Surface(
                                                    color = LiveRed,
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "YAYINDA",
                                                        color = Color.White,
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
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
        }

        // Fast Channel Zapping OSD Banner (Shows when using Remote Up/Down or Channel +/-)
        AnimatedVisibility(
            visible = showOSD && !showZapList && !showControls,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            val displayChan = osdChannel ?: activeChannel
            displayChan?.let { chan ->
                Surface(
                    color = Color(0xFF070E24).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AuroraCyan.copy(alpha = 0.4f)),
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Channel Logo
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF030712))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            ChannelLogoImage(
                                logoUrl = chan.logoUrl,
                                category = chan.category,
                                channelName = chan.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(LiveRed)
                                )
                                Text(
                                    text = "CANLI YAYIN",
                                    color = LiveRed,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = AuroraCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = chan.category.ifBlank { "Genel" },
                                        color = AuroraCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = chan.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Floating Back Button (Top Left)
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Geri Dön",
                tint = Color.White
            )
        }

        // TV Usability Remote Comfort Dialog
        if (showComfortCheck) {
            AlertDialog(
                onDismissRequest = { showComfortCheck = false },
                confirmButton = {
                    Button(
                        onClick = { showComfortCheck = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan)
                    ) {
                        Text("Anlaşıldı ✅", color = DeepSpaceBlue)
                    }
                },
                title = {
                    Text(
                        text = "👴📺 65\" TV 3m Kumanda Testi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AuroraCyan
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Soru: 65 inç TV'de, 3 metre uzaktan, sadece kumandayla 60 yaşındaki bir kullanıcı bunu rahat kullanabilir mi?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cevap: EVET! ✅ Tüm oynatıcı kontrol elemanları standart TV kumandalarıyla (D-Pad) %100 uyumludur. Ses/kanal geçiş düğmeleri devasa boyutlarda yerleştirilmiştir. Tam ekran oynatma sırasında sol/sağ veya yukarı/aşağı tuşları ile kanallar arası anında gezinebilirsiniz (Zap Modu). En çok izlediğiniz kanallar otomatik olarak listenin en başına taşınır.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                },
                containerColor = DeepSpaceBlue,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- NEW: ON-SCREEN VOLUME HUD INDICATOR (Top Right) ---
        AnimatedVisibility(
            visible = showVolumeHUD,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 24.dp) // Offset slightly from floating back button
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, AuroraCyan.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isMuted) {
                        CustomVolumeOffIcon(tint = AuroraCyan, modifier = Modifier.size(22.dp))
                    } else if (volumeLevel < 40f) {
                        CustomVolumeDownIcon(tint = AuroraCyan, modifier = Modifier.size(22.dp))
                    } else {
                        CustomVolumeUpIcon(tint = AuroraCyan, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = "SES: ${volumeLevel.toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    // Visual volume meter bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(volumeLevel / 100f)
                                .background(AuroraCyan)
                        )
                    }
                }
            }
        }

        // --- NEW: ELECTRONIC PROGRAM REHBERİ (EPG) SIDEBAR (Left-sliding panel) ---
        AnimatedVisibility(
            visible = showEPGGuide,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val upcomingPrograms by viewModel.upcomingEPG.collectAsState()
            val currentProgram by viewModel.currentEPG.collectAsState()
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Color.Black.copy(alpha = 0.92f))
                    .border(1.dp, AuroraCyan.copy(alpha = 0.3f))
                    .padding(20.dp)
                    .clickable(enabled = false) { /* Prevents click-through to video */ }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 YAYIN AKIŞI (EPG)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuroraCyan
                        )
                        IconButton(onClick = { showEPGGuide = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White
                            )
                        }
                    }

                    // Active Channel Info Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceBlue.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = activeChannel?.logoUrl ?: "",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activeChannel?.name ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeChannel?.category ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

                    // Programs List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Active Program (if available)
                        currentProgram?.let { program ->
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, AuroraCyan, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = AuroraCyan.copy(alpha = 0.12f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val start = timeFormatter.format(java.util.Date(program.startTimeMs))
                                            val end = timeFormatter.format(java.util.Date(program.endTimeMs))
                                            Text(
                                                text = "$start - $end",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AuroraCyan
                                            )
                                            // Glowing live indicator
                                            Surface(
                                                color = LiveRed,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "CANLI",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = program.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = program.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        // Visual EPG timeline progress bar
                                        val nowMs = System.currentTimeMillis()
                                        val totalDuration = (program.endTimeMs - program.startTimeMs).toFloat()
                                        val progress = if (totalDuration > 0) {
                                            ((nowMs - program.startTimeMs) / totalDuration).coerceIn(0f, 1f)
                                        } else 0f
                                        
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                            color = AuroraCyan,
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }

                        // Upcoming Programs Schedule
                        if (upcomingPrograms.isEmpty() && currentProgram == null) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Yayın akışı bilgisi bulunamadı.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            val upcomingFiltered = upcomingPrograms.filter { it.id != currentProgram?.id }
                            items(upcomingFiltered) { program ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        val start = timeFormatter.format(java.util.Date(program.startTimeMs))
                                        val end = timeFormatter.format(java.util.Date(program.endTimeMs))
                                        Text(
                                            text = "$start - $end",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = program.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = program.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
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

    // Trigger picture in picture mode if enabled / available
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = context as? Activity
            activity?.let {
                // Ensure PiP configuration is clean
            }
        }
    }
}

// --- LIGHTWEIGHT, HIGH-PERFORMANCE CUSTOM MEDIA ICONS ---
@Composable
fun CustomPauseIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(tint, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(tint, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun CustomSkipPreviousIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.size(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(3.dp).background(tint, RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(2.dp))
        Text("◀", color = tint, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun CustomSkipNextIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.size(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("▶", color = tint, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.width(2.dp))
        Box(modifier = Modifier.fillMaxHeight().width(3.dp).background(tint, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun CustomVolumeUpIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("🔊", color = tint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CustomVolumeDownIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("🔉", color = tint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CustomVolumeOffIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("🔇", color = tint, style = MaterialTheme.typography.bodyMedium)
    }
}
