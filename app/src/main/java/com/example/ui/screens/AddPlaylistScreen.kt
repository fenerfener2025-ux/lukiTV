package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AddPlaylistScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: M3U URL, 1: Xtream Codes

    // M3U inputs
    var m3uUrl by remember { mutableStateOf("") }
    var m3uName by remember { mutableStateOf("") }

    // Xtream inputs
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBlue)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Oynatma Listesi Kaynağı Ekle",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuroraCyan
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Buttons (M3U vs Xtream)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("M3U Oynatma Listesi URL", "Xtream Codes API").forEachIndexed { index, title ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = activeTab == index

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable { activeTab = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AuroraPurple else if (isFocused) SurfaceBlue else SurfaceBlue.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceBlue.copy(alpha = 0.5f))
                    .padding(24.dp)
            ) {
                if (activeTab == 0) {
                    // M3U URL Form
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "M3U Link Girişi",
                            style = MaterialTheme.typography.titleLarge,
                            color = AuroraCyan
                        )

                        OutlinedTextField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = { Text("Liste İsmi (Örn: Özel Kanal Listesi)", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TextSecondary,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_name_input")
                        )

                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U URL Bağlantısı (https://...)", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TextSecondary,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_url_input")
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (m3uUrl.isNotEmpty() && m3uName.isNotEmpty()) {
                                    viewModel.addPlaylistUrl(m3uUrl, m3uName)
                                    onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple),
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(50.dp)
                                .testTag("submit_m3u_button")
                        ) {
                            Text("Kaydet ve İndir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Xtream Codes Form
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Xtream API Girişi",
                            style = MaterialTheme.typography.titleLarge,
                            color = AuroraCyan
                        )

                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Sunucu Adresi (http://sunucu.com:port)", color = TextSecondary) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TextSecondary,
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xtream_server_input")
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = xtreamUser,
                                onValueChange = { xtreamUser = it },
                                label = { Text("Kullanıcı Adı", color = TextSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TextSecondary,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("xtream_user_input")
                            )

                            OutlinedTextField(
                                value = xtreamPass,
                                onValueChange = { xtreamPass = it },
                                label = { Text("Şifre", color = TextSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TextSecondary,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("xtream_pass_input")
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty()) {
                                    viewModel.addXtream(xtreamServer, xtreamUser, xtreamPass)
                                    onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple),
                            modifier = Modifier
                                .align(Alignment.End)
                                .height(50.dp)
                                .testTag("submit_xtream_button")
                        ) {
                            Text("Giriş Yap ve Bağlan", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
