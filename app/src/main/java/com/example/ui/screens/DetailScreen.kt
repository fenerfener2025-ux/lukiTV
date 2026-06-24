package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.EPGProgram
import com.example.domain.model.IPTVChannel
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    channel: IPTVChannel,
    viewModel: MainViewModel,
    onPlayClick: (IPTVChannel) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val currentEPG by viewModel.currentEPG.collectAsState()
    val upcomingEPG by viewModel.upcomingEPG.collectAsState()
    var showComfortCheck by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val isMobile = isPortrait || configuration.screenWidthDp < 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBlue)
            .padding(if (isMobile) 16.dp else 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Kanal Detayları",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuroraCyan
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isMobile) {
                // Scrollable Portrait Column for Mobile
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Logo + Name Panel
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceBlue.copy(alpha = 0.5f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = channel.logoUrl.ifEmpty { "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg" },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Grup: ${channel.groupTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badges row (horizontal scrollable on mobile)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(containerColor = AuroraPurple) { Text("TR", color = Color.White) }
                        Badge(containerColor = SurfaceBlue) { Text("Türkçe", color = Color.White) }
                        Badge(containerColor = SurfaceBlue) { Text(channel.category, color = Color.White) }
                        Badge(containerColor = LiveRed) { Text("4K UHD", color = Color.White) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current Program Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ŞU AN OYNATILIYOR",
                                style = MaterialTheme.typography.labelMedium,
                                color = AuroraCyan,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentEPG?.title ?: "Aurora Özel Yayın Kuşağı",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val timeSpan = if (currentEPG != null) {
                                "${timeFormatter.format(Date(currentEPG!!.startTimeMs))} - ${timeFormatter.format(Date(currentEPG!!.endTimeMs))}"
                            } else {
                                "24 Saat Kesintisiz"
                            }
                            Text(
                                text = timeSpan,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentEPG?.description ?: "Bu kanal için EPG ve yayın akışı bilgileri otomatik senkronize edilmiştir. 4K akıcı oynatım seçeneği aktiftir.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sonraki 6 Saat Yayın Akışı",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (upcomingEPG.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceBlue.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Gelecek program bilgisi bulunmuyor.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(upcomingEPG) { program ->
                                Card(
                                    modifier = Modifier.width(180.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = timeFormatter.format(Date(program.startTimeMs)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AuroraCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = program.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons Layout (Stacked/Flowing for Mobile)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayClick(channel) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("detail_play_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DeepSpaceBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Oynat", color = DeepSpaceBlue, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.toggleFavorite(channel.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = LiveRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (channel.isFavorite) "Favoriden Çıkar" else "Favoriye Ekle", color = TextPrimary)
                        }

                        Button(
                            onClick = { showComfortCheck = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("detail_comfort_button")
                        ) {
                            Text("👵 TV Konfor", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // Existing Split Layout for TV Box/Widescreen
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Panel (30% Width)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(SurfaceBlue.copy(alpha = 0.5f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = channel.logoUrl.ifEmpty { "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg" },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Grup: ${channel.groupTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Right Panel (70% Width)
                    Column(
                        modifier = Modifier
                            .weight(2.8f)
                            .fillMaxHeight()
                    ) {
                        // Top: Badges & Tags Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Badge(containerColor = AuroraPurple) { Text("TR", color = Color.White) }
                            Badge(containerColor = SurfaceBlue) { Text("Türkçe", color = Color.White) }
                            Badge(containerColor = SurfaceBlue) { Text(channel.category, color = Color.White) }
                            Badge(containerColor = LiveRed) { Text("4K UHD", color = Color.White) }
                            Badge(containerColor = Color.White.copy(alpha = 0.12f)) { Text("H.264 / AAC", color = Color.White) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Middle: Current Program Info Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "ŞU AN OYNATILIYOR",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AuroraCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = currentEPG?.title ?: "Aurora Özel Yayın Kuşağı",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val timeSpan = if (currentEPG != null) {
                                    "${timeFormatter.format(Date(currentEPG!!.startTimeMs))} - ${timeFormatter.format(Date(currentEPG!!.endTimeMs))}"
                                } else {
                                    "24 Saat Kesintisiz"
                                }
                                Text(
                                    text = timeSpan,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = currentEPG?.description ?: "Bu kanal için EPG ve yayın akışı bilgileri otomatik senkronize edilmiştir. 4K akıcı oynatım seçeneği aktiftir.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bottom: "Sonraki 6 Saat" Mini EPG timeline
                        Text(
                            text = "Sonraki 6 Saat Yayın Akışı",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (upcomingEPG.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceBlue.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Gelecek program bilgisi bulunmuyor.", color = TextSecondary)
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(upcomingEPG) { program ->
                                    Card(
                                        modifier = Modifier.width(220.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = timeFormatter.format(Date(program.startTimeMs)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AuroraCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = program.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bottom Action Row
                        Row {
                            Button(
                                onClick = { onPlayClick(channel) },
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                                modifier = Modifier.testTag("detail_play_button")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DeepSpaceBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Oynat", color = DeepSpaceBlue, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { viewModel.toggleFavorite(channel.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                                modifier = Modifier.testTag("detail_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = LiveRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (channel.isFavorite) "Favoriden Çıkar" else "Favoriye Ekle", color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { showComfortCheck = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                                modifier = Modifier.testTag("detail_comfort_button")
                            ) {
                                Text("👵 TV Konfor", color = Color.White)
                            }
                        }
                    }
                }
            }
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
                            text = "Cevap: EVET! ✅ Tüm kanal detayları, logoları, EPG başlıkları ve aksiyon düğmeleri devasa 48dp boyutlarında D-Pad uyumlu hedef alanları ile tasarlanmıştır. Uzaktan kumandanızdaki yön tuşlarıyla 'Oynat' ve 'Favori' düğmeleri arasında zahmetsizce geçiş yapabilirsiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                },
                containerColor = DeepSpaceBlue,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun Badge(
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        content()
    }
}
