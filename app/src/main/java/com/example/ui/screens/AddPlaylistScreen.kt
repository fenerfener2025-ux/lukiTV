package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.OPEN_SOURCE_PRESETS
import com.example.data.repository.PresetSource
import com.example.ui.theme.*
import com.example.ui.tv.dpadFocusable
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AddPlaylistScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: M3U URL, 1: Yerel Dosya, 2: Xtream Codes, 3: Açık Kaynak Şablonlar
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // M3U URL state
    var m3uUrl by remember { mutableStateOf("") }
    var m3uName by remember { mutableStateOf("") }

    // Xtream state
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    // Local file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val displayName = uri.lastPathSegment?.substringAfterLast('/')?.ifEmpty { "Yerel Oynatma Listesi" } ?: "Yerel Liste"
                    viewModel.addPlaylistFromFile(inputStream, displayName)
                    onNavigateBack()
                }
            } catch (e: Exception) {
                viewModel.showRemoteToast("Dosya açılamadı: ${e.message}")
            }
        }
    }

    // Confirmation dialog to clear all channels
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = SurfaceBlue,
            title = {
                Text(
                    text = "Oynatma Listesini Temizle",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Mevcut tüm kanallar ve program rehberi (EPG) silinecektir. Devam etmek istiyor musunuz?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllChannels()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("Evet, Temizle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("İptal", color = Color.White)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .dpadFocusable(onSelect = onNavigateBack)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kaynak Ekle & Yönet",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Açık kaynak TV oynatıcısı • Kendi listenizi ekleyin",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Clear List Button
                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.dpadFocusable(onSelect = { showClearConfirmDialog = true })
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Listeyi Temizle", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "M3U / M3U8 URL", Icons.Default.Link),
                    Triple(1, "Yerel M3U Dosyası", Icons.Default.FolderOpen),
                    Triple(2, "Xtream API", Icons.Default.Dns),
                    Triple(3, "Açık Kaynak Şablonlar", Icons.Default.Public)
                )

                tabs.forEach { (index, title, icon) ->
                    val isSelected = activeTab == index
                    var isFocused by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeTab = index }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .dpadFocusable(onSelect = { activeTab = index }),
                        color = when {
                            isSelected -> AuroraCyan
                            isFocused -> SurfaceBlueLight
                            else -> SurfaceBlue
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isFocused && !isSelected) BorderStroke(1.5.dp, AuroraCyan) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF061026) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                color = if (isSelected) Color(0xFF061026) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> M3uUrlTab(
                        url = m3uUrl,
                        onUrlChange = { m3uUrl = it },
                        name = m3uName,
                        onNameChange = { m3uName = it },
                        onSave = { url, name ->
                            viewModel.addPlaylistUrl(url, name.ifEmpty { "Özel M3U" })
                            onNavigateBack()
                        }
                    )
                    1 -> LocalFileTab(
                        onPickFile = {
                            filePickerLauncher.launch("*/*")
                        }
                    )
                    2 -> XtreamTab(
                        server = xtreamServer,
                        onServerChange = { xtreamServer = it },
                        user = xtreamUser,
                        onUserChange = { xtreamUser = it },
                        pass = xtreamPass,
                        onPassChange = { xtreamPass = it },
                        onSave = { s, u, p ->
                            viewModel.addXtream(s, u, p)
                            onNavigateBack()
                        }
                    )
                    3 -> OpenSourcePresetsTab(
                        presets = OPEN_SOURCE_PRESETS,
                        onSelectPreset = { preset ->
                            viewModel.loadOpenSourcePreset(preset)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

/* ==========================================================================
   TAB 0: M3U / M3U8 URL
   ========================================================================== */
@Composable
private fun M3uUrlTab(
    url: String,
    onUrlChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onSave: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "M3U / M3U8 Bağlantısı Ekle",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "İnternet üzerindeki herhangi bir doğrudan .m3u veya .m3u8 linkini yapıştırın.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Oynatma Listesi Adı (İsteğe bağlı)") },
                placeholder = { Text("Örn: Türk Kanalları, Spor Paketi") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("M3U / M3U8 URL *") },
                placeholder = { Text("https://example.com/playlist.m3u8") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_url_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onSave(url.trim(), name.trim())
                    }
                },
                enabled = url.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_m3u_button")
                    .dpadFocusable(
                        onSelect = {
                            if (url.isNotBlank()) {
                                onSave(url.trim(), name.trim())
                            }
                        }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraCyan,
                    contentColor = Color(0xFF061026)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Oynatma Listesini İndir ve Ekle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ==========================================================================
   TAB 1: YEREL M3U DOSYASI
   ========================================================================== */
@Composable
private fun LocalFileTab(
    onPickFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AuroraCyan.copy(alpha = 0.15f))
                    .border(2.dp, AuroraCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Cihazınızdaki M3U Dosyasını Seçin",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Telefonunuzun veya TV kutunuzun dahili hafızasındaki veya USB bellekteki .m3u, .m3u8 veya .txt dosyasını içe aktarın.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onPickFile,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(52.dp)
                    .dpadFocusable(onSelect = onPickFile),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraCyan,
                    contentColor = Color(0xFF061026)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dosya Gezginini Aç",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ==========================================================================
   TAB 2: XTREAM CODES API
   ========================================================================== */
@Composable
private fun XtreamTab(
    server: String,
    onServerChange: (String) -> Unit,
    user: String,
    onUserChange: (String) -> Unit,
    pass: String,
    onPassChange: (String) -> Unit,
    onSave: (String, String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Xtream Codes API Girişi",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Sağlayıcınızın sunduğu sunucu URL'si, kullanıcı adı ve şifresini girin.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = server,
                onValueChange = onServerChange,
                label = { Text("Sunucu Adresi (Örn: http://domain.com:8080)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = user,
                    onValueChange = onUserChange,
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = onPassChange,
                    label = { Text("Şifre") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                        onSave(server.trim(), user.trim(), pass.trim())
                    }
                },
                enabled = server.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .dpadFocusable(
                        onSelect = {
                            if (server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                                onSave(server.trim(), user.trim(), pass.trim())
                            }
                        }
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraCyan,
                    contentColor = Color(0xFF061026)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Giriş Yap ve Kanalları Yükle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* ==========================================================================
   TAB 3: AÇIK KAYNAK ŞABLONLAR
   ========================================================================== */
@Composable
private fun OpenSourcePresetsTab(
    presets: List<PresetSource>,
    onSelectPreset: (PresetSource) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = SurfaceBlueLight.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AuroraCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Aşağıdaki listeler yalnızca kamuya açık ve telifsiz serbest yayınları (iptv-org projesi) içerir. Tek tıkla hızlıca yükleyebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(presets, key = { it.url }) { preset ->
            var isFocused by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .clickable { onSelectPreset(preset) }
                    .dpadFocusable(onSelect = { onSelectPreset(preset) }),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFocused) SurfaceBlueLight else SurfaceBlue
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isFocused) AuroraCyan else Color.White.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuroraCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (preset.category) {
                                    "TR" -> Icons.Default.Tv
                                    "Spor" -> Icons.Default.SportsSoccer
                                    "Haber" -> Icons.Default.Newspaper
                                    else -> Icons.Default.Public
                                },
                                contentDescription = null,
                                tint = AuroraCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { onSelectPreset(preset) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraCyan,
                            contentColor = Color(0xFF061026)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yükle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
