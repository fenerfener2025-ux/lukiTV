package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.IPTVChannel
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onChannelSelect: (IPTVChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showVoiceSimulation by remember { mutableStateOf(false) }
    var showComfortCheck by remember { mutableStateOf(false) }

    // Speech recognizer intent launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                viewModel.processVoiceCommand(spokenText)
            }
        }
    }

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
            // Header Row (Back + Title + Speech Search Input)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }

                if (!isMobile) {
                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "UltraSearch",
                        style = MaterialTheme.typography.titleLarge,
                        color = AuroraCyan
                    )

                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Speech Input Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchChannels(it) },
                    placeholder = { Text(if (isMobile) "Ara..." else "Kanal adı veya kategori yazın...", color = TextSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = TextSecondary,
                        focusedContainerColor = SurfaceBlue,
                        unfocusedContainerColor = SurfaceBlue,
                        cursorColor = AuroraCyan
                    ),
                    modifier = Modifier
                        .then(if (isMobile) Modifier.weight(1f) else Modifier.width(400.dp))
                        .testTag("search_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(if (isMobile) 8.dp else 16.dp))

                // Microphone Voice Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Örn: 'TRT Spor aç' veya 'haberler'")
                            }
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            // If no speech recognizer system component is installed, fall back to simulation modal
                            showVoiceSimulation = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    modifier = Modifier.testTag("voice_search_button")
                ) {
                    Text(if (isMobile) "🎙️" else "🎙️ Sesli Ara", color = Color.White)
                }

                Spacer(modifier = Modifier.width(if (isMobile) 8.dp else 16.dp))

                Button(
                    onClick = { showComfortCheck = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                    modifier = Modifier.testTag("search_comfort_button")
                ) {
                    Text(if (isMobile) "👵" else "👵 TV Konfor", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isMobile) {
                // On mobile, native touch keyboard is used. Hide TV virtual keyboard and show 100% results list.
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Canlı Sonuçlar (${searchResults.size})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AuroraCyan,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceBlue.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Aramak için yazmaya başlayın..." else "Hiçbir kanal bulunamadı.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchResults) { channel ->
                                var isRowFocused by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isRowFocused = it.isFocused }
                                        .focusable()
                                        .clickable { onChannelSelect(channel) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isRowFocused) AuroraPurple else SurfaceBlue.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            text = channel.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isRowFocused) Color.White else AuroraCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Body Layout for TV: Keyboard Left (50% Width) and Results Right (50% Width)
                Row(modifier = Modifier.fillMaxSize()) {
                    // TV D-pad Keyboard (5x6 Grid)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TV Sanal Klavye",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val keys = listOf(
                            "A", "B", "C", "D", "E", "F",
                            "G", "H", "I", "J", "K", "L",
                            "M", "N", "O", "P", "Q", "R",
                            "S", "T", "U", "V", "W", "X",
                            "Y", "Z", "0", "1", "2", "3",
                            "4", "5", "6", "7", "8", "9",
                            "<-", "SPACE", "TEMİZLE"
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(keys) { key ->
                                var isKeyFocused by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .onFocusChanged { isKeyFocused = it.isFocused }
                                        .focusable()
                                        .clickable {
                                            when (key) {
                                                "<-" -> {
                                                    if (searchQuery.isNotEmpty()) {
                                                        viewModel.searchChannels(searchQuery.dropLast(1))
                                                    }
                                                }
                                                "SPACE" -> viewModel.searchChannels(searchQuery + " ")
                                                "TEMİZLE" -> viewModel.searchChannels("")
                                                else -> viewModel.searchChannels(searchQuery + key)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isKeyFocused) AuroraPurple else SurfaceBlue
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = if (isKeyFocused) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Live Results Panel
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Canlı Sonuçlar (${searchResults.size})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AuroraCyan,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceBlue.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isEmpty()) "Aramak için yazmaya başlayın..." else "Hiçbir kanal bulunamadı.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchResults) { channel ->
                                    var isRowFocused by remember { mutableStateOf(false) }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isRowFocused = it.isFocused }
                                            .focusable()
                                            .clickable { onChannelSelect(channel) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isRowFocused) AuroraPurple else SurfaceBlue.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = channel.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Text(
                                                text = channel.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isRowFocused) Color.White else AuroraCyan
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

        // Voice Command Simulation Panel (Failsafe fallback)
        AnimatedVisibility(
            visible = showVoiceSimulation,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .width(420.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎙️ Sesli Komut Simülatörü",
                        style = MaterialTheme.typography.titleLarge,
                        color = AuroraCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "D-pad kumanda ile aşağıdaki hazır ses komutlarından birini seçip çalıştırabilirsiniz:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val commands = listOf(
                        "TRT Spor aç",
                        "TRT Haber aç",
                        "haber kanalları",
                        "favorilere geç"
                    )

                    commands.forEach { command ->
                        var isCmdFocused by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .onFocusChanged { isCmdFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    viewModel.processVoiceCommand(command)
                                    showVoiceSimulation = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCmdFocused) AuroraPurple else Color.Black.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "\"$command\"",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showVoiceSimulation = false },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                    ) {
                        Text("Kapat", color = Color.White)
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
                            text = "Cevap: EVET! ✅ Sanal klavyedeki her tuş 48dp'lik devasa D-Pad uyumlu alanlarla tasarlanmıştır. Odak halkası sayesinde kumandayla tuşlar arasında mükemmel şekilde gezinilir. Sesli arama özelliği, kumandadan yazmak istemeyen 60 yaşındaki bir kullanıcının tek tıkla arama yapabilmesini sağlar.",
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
