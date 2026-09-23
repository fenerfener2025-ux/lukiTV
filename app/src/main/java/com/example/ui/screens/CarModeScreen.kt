package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import com.example.player.PlayerEngineManager
import com.example.player.PlayerEngineManager.PlaybackState
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/**
 * CarModeScreen (Araç / Sürüş Modu):
 * Specially designed for in-vehicle use (Android Auto, Android Automotive OS,
 * and car-mounted devices). Adheres to automotive distraction-free safety guidelines:
 * - Huge touch targets (64dp - 88dp)
 * - Safe Audio-Only (Radio) Mode to prevent driver visual distraction
 * - High-contrast daylight/night HUD
 * - Direct steering-wheel friendly channel controls
 * - One-tap voice channel search
 */
@OptIn(UnstableApi::class)
@Composable
fun CarModeScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel by viewModel.activeChannel.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val player by viewModel.playerEngineManager.activePlayer.collectAsState()
    val carAudioOnlyPref by viewModel.carAudioOnlyDefault.collectAsState()

    var isAudioOnlyMode by remember(carAudioOnlyPref) { mutableStateOf(carAudioOnlyPref) } // Default false for video playback on car screens
    var isNightMode by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var isMuted by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Voice recognition launcher for safe hands-free channel selection in car
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val matched = allChannels.find { it.name.contains(spoken, ignoreCase = true) }
                if (matched != null) {
                    viewModel.selectChannel(matched)
                    viewModel.showRemoteToast("🚗 Sesle Bulundu: ${matched.name}")
                } else {
                    viewModel.showRemoteToast("'$spoken' adında kanal bulunamadı")
                }
            }
        }
    }

    // Colors adjusted for automotive HUD
    val hudBackground = if (isNightMode) Color(0xFF070B14) else Color(0xFF141E33)
    val hudCardBg = if (isNightMode) Color(0xFF111A2E) else Color(0xFF1E2B47)
    val hudAccent = if (isNightMode) AuroraCyan else Color(0xFF00E5FF)

    // Animated pulse for audio-only radio mode
    val infiniteTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(hudBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Car Mode Status, Mode Toggles, Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(hudCardBg)
                            .testTag("car_mode_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Araç Modundan Çık",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(hudCardBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = hudAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARAÇ MODU",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Toggles: Voice Search, Audio/Video Mode, Day/Night HUD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Voice Channel Search Button
                    Button(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Kanal Adı Söyleyin (Örn: TRT Spor)")
                            }
                            try {
                                speechLauncher.launch(intent)
                            } catch (e: Exception) {
                                viewModel.showRemoteToast("Sesli arama cihazınızda desteklenmiyor")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("car_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Sesle Ara",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sesle Ara", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    // Audio Only / Video Mode Toggle (Driving Safety)
                    Button(
                        onClick = {
                            isAudioOnlyMode = !isAudioOnlyMode
                            viewModel.showRemoteToast(
                                if (isAudioOnlyMode) "🚗 Sürüş Emniyeti: Sadece Ses (Radyo) Modu Açık"
                                else "📺 Park Modu: Görüntü Açık"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAudioOnlyMode) Color(0xFF00897B) else hudCardBg
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("car_audio_only_toggle")
                    ) {
                        Icon(
                            imageVector = if (isAudioOnlyMode) Icons.Filled.Headphones else Icons.Filled.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAudioOnlyMode) "Sadece Ses" else "Görüntü Açık",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Day / Night Toggle
                    IconButton(
                        onClick = { isNightMode = !isNightMode },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(hudCardBg)
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Filled.NightlightRound else Icons.Filled.WbSunny,
                            contentDescription = "Gece/Gündüz Teması",
                            tint = hudAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // CENTER AREA: Big Channel Card or Embedded Video
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(hudCardBg)
                    .border(2.dp, if (isNightMode) Color(0xFF1E2D4A) else Color(0xFF2A3D63), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!isAudioOnlyMode && player != null) {
                    // Park Mode Video View
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                this.player = player
                            }
                        },
                        update = { view ->
                            view.player = player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Safe Driving Audio HUD Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(130.dp)
                                .scale(if (playbackState is PlaybackState.Playing) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(hudAccent.copy(alpha = 0.35f), Color.Transparent)
                                    )
                                )
                                .border(3.dp, hudAccent, CircleShape)
                        ) {
                            if (!activeChannel?.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = activeChannel?.logoUrl,
                                    contentDescription = activeChannel?.name,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = null,
                                    tint = hudAccent,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = activeChannel?.name ?: "Kanal Seçilmedi",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeChannel?.category ?: "Canlı TV & Radyo",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = hudAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "•",
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (playbackState) {
                                    is PlaybackState.Playing -> "Canlı Çalıyor 🟢"
                                    is PlaybackState.Loading -> "Yükleniyor ⏳"
                                    else -> "Hazır"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "🚗 Sürüş Güvenliği: Video gizlendi, ses akışı kesintisiz açık",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8E9BAE)
                        )
                    }
                }
            }

            // PRIMARY DRIVER CONTROLS: Large 72dp-88dp Buttons (Prev, Play/Pause, Next, Volume)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute Button
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        player?.volume = if (isMuted) 0f else 1f
                        viewModel.showRemoteToast(if (isMuted) "🔇 Ses Kapatıldı" else "🔊 Ses Açıldı")
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(hudCardBg)
                        .testTag("car_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Ses",
                        tint = if (isMuted) Color.Red else Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Previous Channel Button (Massive 76dp)
                Button(
                    onClick = { viewModel.playPreviousChannel() },
                    colors = ButtonDefaults.buttonColors(containerColor = hudCardBg),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(76.dp)
                        .border(2.dp, hudAccent.copy(alpha = 0.5f), CircleShape)
                        .testTag("car_prev_channel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Önceki Kanal",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause Master Button (Huge 90dp)
                val isPlaying = player?.isPlaying == true
                Button(
                    onClick = { viewModel.togglePlayPause() },
                    colors = ButtonDefaults.buttonColors(containerColor = hudAccent),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(90.dp)
                        .testTag("car_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = Color(0xFF070B14),
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Next Channel Button (Massive 76dp)
                Button(
                    onClick = { viewModel.playNextChannel() },
                    colors = ButtonDefaults.buttonColors(containerColor = hudCardBg),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(76.dp)
                        .border(2.dp, hudAccent.copy(alpha = 0.5f), CircleShape)
                        .testTag("car_next_channel_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Sonraki Kanal",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Favorite Toggle Button
                IconButton(
                    onClick = { viewModel.toggleCurrentChannelFavorite() },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(hudCardBg)
                        .testTag("car_fav_button")
                ) {
                    val isFav = activeChannel?.isFavorite == true
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (isFav) Color(0xFFFF3366) else Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // BOTTOM QUICK STATION DOCKS: Categories & Quick Zapping Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Category Pills (Ulusal, Haber, Spor, Müzik, Favoriler)
                val carCategories = listOf("Tümü", CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS, CategoryHelper.CAT_MUSIC, "Favoriler")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(carCategories) { cat ->
                        val isSelected = selectedCategory == cat
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) hudAccent else hudCardBg,
                            label = "pill_bg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(pillBg)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color(0xFF070B14) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Channel List for Selected Category
                val displayedChannels = remember(selectedCategory, allChannels, favoriteChannels) {
                    when (selectedCategory) {
                        "Favoriler" -> favoriteChannels
                        "Tümü" -> allChannels.take(25)
                        else -> allChannels.filter { it.category == selectedCategory }.take(25)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayedChannels) { channel ->
                        val isCurrent = activeChannel?.id == channel.id
                        Card(
                            onClick = { viewModel.selectChannel(channel) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) hudAccent else hudCardBg
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(170.dp)
                                .height(64.dp)
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) Color.White else Color(0xFF223252),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .testTag("car_channel_card_${channel.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (channel.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2D3C5C)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = channel.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isCurrent) Color(0xFF070B14) else Color.White,
                                    maxLines = 1,
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
