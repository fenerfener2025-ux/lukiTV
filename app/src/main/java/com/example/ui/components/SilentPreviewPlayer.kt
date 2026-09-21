package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.domain.model.IPTVChannel
import com.example.ui.theme.LocalThemeConfig
import kotlinx.coroutines.delay

/**
 * Kanal listesinde gezinirken seçili olan kanalın sağ tarafta
 * küçük bir pencerede sessiz (muted) ve fütüristik bir tasarımla oynatılmasını sağlayan önizleme bileşeni.
 */
@OptIn(UnstableApi::class)
@Composable
fun SilentPreviewPlayer(
    channel: IPTVChannel?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalThemeConfig.current.palette

    var playerState by remember { mutableStateOf<PreviewPlayerState>(PreviewPlayerState.Idle) }
    val streamUrl = channel?.streamUrl

    // Lightweight ExoPlayer for preview
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(5000)
            .setReadTimeoutMs(5000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                8000,  // minBufferMs
                15000, // maxBufferMs
                500,   // bufferForPlaybackMs
                1000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f // STRICTLY MUTED
                playWhenReady = true
            }
    }

    // Handle play state and error listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playerState = when (state) {
                    Player.STATE_BUFFERING -> PreviewPlayerState.Buffering
                    Player.STATE_READY -> PreviewPlayerState.Playing
                    Player.STATE_ENDED -> PreviewPlayerState.Ended
                    else -> PreviewPlayerState.Idle
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playerState = PreviewPlayerState.Error("Önizleme yüklenemedi")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Watch channel stream source updates
    LaunchedEffect(streamUrl) {
        if (streamUrl != null && streamUrl.isNotBlank()) {
            playerState = PreviewPlayerState.Buffering
            // Brief debounced delay to prevent aggressive loading during fast dpad scrolling
            delay(400)
            try {
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                playerState = PreviewPlayerState.Error("Format Hatası")
            }
        } else {
            exoPlayer.stop()
            playerState = PreviewPlayerState.Idle
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.9f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.secondary.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (streamUrl != null) {
                // ExoPlayer AndroidView
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Silent mode indicator badge overlay (Mute)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = "Sessiz Önizleme",
                        tint = palette.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SESSİZ ÖNİZLEME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }

                // Channel title bottom translucent overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${channel.category} • Canlı Akış",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                // Placeholder when no channel is focused / loaded
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = palette.secondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Önizleme için kanallarda gezinin",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Loading / Buffering HUD state overlay
            if (playerState == PreviewPlayerState.Buffering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = palette.secondary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Error Overlay HUD
            if (playerState is PreviewPlayerState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (playerState as PreviewPlayerState.Error).message,
                        color = palette.liveBadge,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

sealed interface PreviewPlayerState {
    object Idle : PreviewPlayerState
    object Buffering : PreviewPlayerState
    object Playing : PreviewPlayerState
    object Ended : PreviewPlayerState
    data class Error(val message: String) : PreviewPlayerState
}
