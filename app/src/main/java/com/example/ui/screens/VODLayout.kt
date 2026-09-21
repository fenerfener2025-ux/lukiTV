package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.VODItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VODLayout(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    val vodItems by viewModel.vodItems.collectAsState()
    val selectedVODSourceName by viewModel.selectedVODSourceName.collectAsState()
    val isVODLoading by viewModel.isVODLoading.collectAsState()
    val vodLoadingMessage by viewModel.vodLoadingMessage.collectAsState()
    val selectedVODCategory by viewModel.selectedVODCategory.collectAsState()
    val selectedVODGenre by viewModel.selectedVODGenre.collectAsState()
    val vodSearchQuery by viewModel.vodSearchQuery.collectAsState()

    var focusedItem by remember { mutableStateOf<VODItem?>(null) }

    // 1. Dynamic genres list
    val genres = remember(vodItems) {
        listOf("Tümü") + vodItems.map { it.groupTitle }.distinct().sorted()
    }

    // 2. Filter logic
    val filteredItems = remember(vodItems, selectedVODCategory, selectedVODGenre, vodSearchQuery) {
        vodItems.filter { item ->
            val matchesCategory = when (selectedVODCategory) {
                "Filmler" -> item.category == "Movies"
                "Diziler" -> item.category == "Series"
                else -> true
            }
            val matchesGenre = selectedVODGenre == "Tümü" || item.groupTitle == selectedVODGenre
            val matchesSearch = vodSearchQuery.isEmpty() || item.name.contains(vodSearchQuery, ignoreCase = true)
            
            matchesCategory && matchesGenre && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Source Name & Refresh Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎬 Sinema & VOD Arşivi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (selectedVODSourceName.isNotEmpty()) {
                    Text(
                        text = "Kaynak: $selectedVODSourceName (${filteredItems.size} içerik)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuroraCyan
                    )
                }
            }

            // Spin animation for Refresh Button
            val rotationTransition = rememberInfiniteTransition(label = "spin")
            val angle by rotationTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )

            IconButton(
                onClick = { viewModel.fetchVODItems(forceRefresh = true) },
                modifier = Modifier
                    .testTag("vod_refresh_button")
                    .then(if (isVODLoading) Modifier.scale(1.1f) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "VOD Listesini Yenile",
                    tint = AuroraCyan,
                    modifier = if (isVODLoading) Modifier.scale(1.2f).onFocusChanged {  } else Modifier
                )
            }
        }

        // Active filters: Search Input, Category Selectors, Genre Filters
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Search Text Field
            OutlinedTextField(
                value = vodSearchQuery,
                onValueChange = { viewModel.searchVOD(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vod_search_input"),
                placeholder = { Text("Film veya dizi ara...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AuroraCyan) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = TextSecondary,
                    focusedContainerColor = SurfaceBlue.copy(alpha = 0.3f),
                    unfocusedContainerColor = SurfaceBlue.copy(alpha = 0.3f),
                    cursorColor = AuroraCyan,
                    focusedIndicatorColor = AuroraCyan,
                    unfocusedIndicatorColor = SurfaceBlue
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category Tabs: Tümü, Filmler, Diziler
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceBlue.copy(alpha = 0.5f))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val categories = listOf("Tümü", "Filmler", "Diziler")
                categories.forEach { cat ->
                    val isSelected = cat == selectedVODCategory
                    var isFocused by remember { mutableStateOf(false) }

                    val bgBrush = when {
                        isSelected -> Brush.horizontalGradient(listOf(AuroraPurple, AuroraCyan))
                        isFocused -> Brush.horizontalGradient(listOf(SurfaceBlue, SurfaceBlue.copy(alpha = 0.5f)))
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier)
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.selectVODCategory(cat) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected || isFocused) Color.White else TextSecondary
                        )
                    }
                }
            }

            // Genre LazyRow
            if (genres.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(genres) { genre ->
                        val isSelected = genre == selectedVODGenre
                        var isFocused by remember { mutableStateOf(false) }
                        
                        val containerColor = when {
                            isSelected -> AuroraPurple
                            isFocused -> SurfaceBlue
                            else -> SurfaceBlue.copy(alpha = 0.4f)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isFocused) AuroraCyan else Color.Transparent),
                            modifier = Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clickable { viewModel.selectVODGenre(genre) }
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero detail panel for the focused item (Fenerbahçe Theme & TMDB Integration)
        val displayItem = focusedItem ?: filteredItems.firstOrNull()
        AnimatedVisibility(
            visible = displayItem != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (displayItem != null) {
                // Trigger TMDB lazy load when display item changes
                LaunchedEffect(displayItem.id) {
                    viewModel.loadTMDBMetadata(displayItem)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBlue),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AuroraPurple)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Poster image on left
                        Card(
                            modifier = Modifier
                                .width(75.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AuroraCyan.copy(alpha = 0.5f))
                        ) {
                            val posterUrl = displayItem.tmdbPosterUrl ?: displayItem.logoUrl
                            if (posterUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(AuroraPurple, DeepSpaceBlue))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎬", fontSize = 24.sp)
                                }
                            }
                        }

                        // Text details on right
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayItem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Rating Badge if available
                                if (displayItem.tmdbRating != null && displayItem.tmdbRating > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(AuroraCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(1.dp, AuroraCyan, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⭐",
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = String.format("%.1f", displayItem.tmdbRating),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AuroraCyan
                                        )
                                    }
                                }
                            }

                            // Genre & Type Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(AuroraPurple, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (displayItem.category == "Series") "DİZİ" else "FİLM",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                val genreText = displayItem.tmdbGenre ?: displayItem.groupTitle
                                if (genreText.isNotEmpty() && genreText != "Genel") {
                                    Text(
                                        text = genreText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AuroraCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Overview / Description
                            val overviewText = displayItem.overview ?: "Açıklama TMDB üzerinden sorgulanıyor..."
                            Text(
                                text = overviewText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Display results or empty states
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isVODLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = AuroraCyan, strokeWidth = 3.dp)
                    Text(
                        text = vodLoadingMessage ?: "İçerikler yükleniyor, lütfen bekleyin...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (filteredItems.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🔎 Arama Kriterine Uygun Film veya Dizi Bulunamadı",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Farklı bir kelime arayabilir ya da kategori filtrelerini değiştirebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.fetchVODItems(forceRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple)
                    ) {
                        Text("Yeniden Yüklemeyi Dene", color = Color.White)
                    }
                }
            } else {
                // Highly Responsive Poster Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (isMobile) 110.dp else 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        VODItemCard(
                            item = item,
                            onFocused = { focusedItem = item },
                            onLoadMetadata = { viewModel.loadTMDBMetadata(item) },
                            onClick = {
                                viewModel.selectChannel(item.toIPTVChannel())
                                onNavigateToPlayer()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VODItemCard(
    item: VODItem,
    onFocused: () -> Unit,
    onLoadMetadata: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    // Lazy TMDB query on card mount
    LaunchedEffect(item.id) {
        onLoadMetadata()
    }

    // Smooth hover scaling and neon glow border animation
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AuroraCyan else Color.Transparent,
        animationSpec = tween(200),
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.68f) // 2:3 Cinematic movie poster aspect ratio
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) borderColor else SurfaceBlue.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (it.isFocused) {
                    onFocused()
                }
            }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster Image
            val displayPoster = item.tmdbPosterUrl ?: item.logoUrl
            if (displayPoster.isNotEmpty()) {
                AsyncImage(
                    model = displayPoster,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Gradient cinema placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SurfaceBlue, DeepSpaceBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎬",
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dark cinematic overlay and text label at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Content Type Tag (Movies / Series) on top left
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (item.category == "Series") AuroraPurple else AuroraCyan)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = if (item.category == "Series") "DİZİ" else "FİLM",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.category == "Series") Color.White else Color.Black
                )
            }

            // Rating Badge on top right if available
            if (item.tmdbRating != null && item.tmdbRating > 0.0) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("⭐", fontSize = 8.sp)
                        Text(
                            text = String.format("%.1f", item.tmdbRating),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuroraCyan
                        )
                    }
                }
            }

            // Genre tag on bottom left or group title
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val genreText = item.tmdbGenre ?: item.groupTitle
                if (genreText.isNotEmpty() && genreText != "Genel") {
                    Text(
                        text = genreText,
                        fontSize = 8.sp,
                        color = AuroraCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
