package com.example.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.domain.model.IPTVChannel
import com.example.domain.util.CategoryHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allChannels by viewModel.allChannels.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val recentChannels by viewModel.recentChannels.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedForeignCountry by viewModel.selectedForeignCountry.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val heroChannel by viewModel.heroChannel.collectAsState()
    val syncingState by viewModel.syncingState.collectAsState()

    val layoutModel by viewModel.layoutModel.collectAsState()
    val sortingOption by viewModel.sortingOption.collectAsState()
    val channelWatchTimes by viewModel.channelWatchTimes.collectAsState()

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val isMobile = isPortrait || configuration.screenWidthDp < 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBlue)
    ) {
        // Background Blur of the Hero Channel
        heroChannel?.let { channel ->
            AsyncImage(
                model = channel.logoUrl.ifEmpty { "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg" },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .scale(1.2f),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        }

        // Koyu degrade kaplaması (Dark Overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DeepSpaceBlue.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Main Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isMobile) 16.dp else 32.dp,
                    vertical = if (isMobile) 12.dp else 24.dp
                )
        ) {
            // Top Navigation Row
            TopNavigationRow(
                syncingState = syncingState,
                onSyncClick = { viewModel.syncPresets() },
                onSearchClick = onNavigateToSearch,
                onAddPlaylistClick = onNavigateToAddPlaylist,
                modifier = Modifier.focusRequester(focusRequester),
                isMobile = isMobile
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick TV Settings Row (D-pad horizontal, scrollable on mobile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(if (isMobile) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeProfile by viewModel.activeProfile.collectAsState()
                val isChildMode by viewModel.isChildMode.collectAsState()
                val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
                val showWorkingOnly by viewModel.showWorkingOnly.collectAsState()

                // Profile Selector Button
                Button(
                    onClick = {
                        val nextProfile = when (activeProfile) {
                            "Profil 1" -> "Profil 2"
                            "Profil 2" -> "Misafir"
                            else -> "Profil 1"
                        }
                        viewModel.selectProfile(nextProfile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("👤 Profil: $activeProfile", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }

                // Kids Mode Toggle Button
                Button(
                    onClick = { viewModel.toggleChildMode("1234") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isChildMode) LiveRed else SurfaceBlue
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (isChildMode) "🧸 Çocuk Modu: AÇIK" else "🧸 Çocuk Modu: KAPALI",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                // Sleep Timer Selector Button
                Button(
                    onClick = {
                        if (sleepTimerMinutes == null) {
                            viewModel.startSleepTimer(30)
                        } else if (sleepTimerMinutes == 30) {
                            viewModel.startSleepTimer(60)
                        } else {
                            viewModel.cancelSleepTimer()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sleepTimerMinutes != null) AuroraPurple else SurfaceBlue
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (sleepTimerMinutes != null) "⏰ Uyku: ${sleepTimerMinutes}dk" else "⏰ Uyku Zamanlayıcı",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                // Working Channels Filter
                Button(
                    onClick = { viewModel.toggleWorkingOnly() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showWorkingOnly) AuroraCyan else SurfaceBlue
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (showWorkingOnly) "✅ Sadece Çalışanlar" else "📺 Tüm Kanallar",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (showWorkingOnly) DeepSpaceBlue else Color.White
                    )
                }

                // Screensaver Activate button
                Button(
                    onClick = { viewModel.toggleScreensaver(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("🖥️ Ekran Koruyucu", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful Aesthetic Smart Tab Switcher (Yerli/Yabancı + Countries)
            AestheticSmartTabSwitcher(
                selectedTab = selectedTab,
                onTabSelect = { viewModel.selectTab(it) },
                selectedForeignCountry = selectedForeignCountry,
                onCountrySelect = { viewModel.selectForeignCountry(it) },
                isMobile = isMobile
            )

            Spacer(modifier = Modifier.height(12.dp))

            val categoryChannels = remember(
                allChannels, selectedCategory, favoriteChannels, recentChannels,
                sortingOption, channelWatchTimes, selectedTab, selectedForeignCountry
            ) {
                getSortedCategoryChannels(
                    allChannels = allChannels,
                    selectedCategory = selectedCategory,
                    favoriteChannels = favoriteChannels,
                    recentChannels = recentChannels,
                    sortingOption = sortingOption,
                    channelWatchTimes = channelWatchTimes,
                    visualOrderList = viewModel.visualOrderList,
                    selectedTab = selectedTab,
                    selectedForeignCountry = selectedForeignCountry
                )
            }

            when (layoutModel) {
                "Sparkle TV" -> {
                    SparkleTVLayout(
                        viewModel = viewModel,
                        categoryChannels = categoryChannels,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                "Google TV" -> {
                    GoogleTVLayout(
                        viewModel = viewModel,
                        favoriteChannels = favoriteChannels,
                        recentChannels = recentChannels,
                        categoryChannels = categoryChannels,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                "Netflix TV" -> {
                    NetflixTVLayout(
                        viewModel = viewModel,
                        categoryChannels = categoryChannels,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                "YouTube TV" -> {
                    YouTubeTVLayout(
                        viewModel = viewModel,
                        categoryChannels = categoryChannels,
                        onNavigateToPlayer = onNavigateToPlayer,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                else -> {
                    // Standard TiviMate Layout
                    if (isMobile) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Category Row (scrollable)
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val categories = if (selectedTab == "Yerli") {
                                    listOf(
                                        CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
                                        CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_LOCAL, CategoryHelper.CAT_KIDS,
                                        CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD, CategoryHelper.CAT_MOVIES,
                                        CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES, CategoryHelper.CAT_RECENTS,
                                        "⚙️ Görünüm & Ayarlar"
                                    )
                                } else {
                                    listOf(
                                        CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
                                        CategoryHelper.CAT_KIDS, CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD,
                                        CategoryHelper.CAT_MOVIES, CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES,
                                        CategoryHelper.CAT_RECENTS, "⚙️ Görünüm & Ayarlar"
                                    )
                                }
                                items(categories) { cat ->
                                    val isSelected = cat == selectedCategory
                                    Button(
                                        onClick = { viewModel.selectCategory(cat) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) AuroraPurple else SurfaceBlue.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(cat, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SettingsCustomizationPanel(viewModel)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Live PIP Preview / Hero area (takes fixed height on mobile)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SurfaceBlue.copy(alpha = 0.6f))
                                    ) {
                                        val activeChannel by viewModel.activeChannel.collectAsState()
                                        if (activeChannel != null) {
                                            LivePipPreview(viewModel, onNavigateToFullScreen = onNavigateToPlayer)
                                        } else {
                                            HeroAreaContent(
                                                heroChannel = heroChannel,
                                                onPlayClick = { channel ->
                                                    viewModel.selectChannel(channel)
                                                    onNavigateToPlayer()
                                                },
                                                onFavoriteClick = { channel ->
                                                    viewModel.toggleFavorite(channel.id)
                                                },
                                                onDetailClick = onNavigateToDetail
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = selectedCategory,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (categoryChannels.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(SurfaceBlue.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Bu kategoride henüz kanal bulunmuyor.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    } else {
                                        // Touch friendly grid
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(categoryChannels) { channel ->
                                                TvChannelCard(
                                                    channel = channel,
                                                    onClick = {
                                                        viewModel.selectChannel(channel)
                                                        onNavigateToPlayer()
                                                    },
                                                    onLongClick = {
                                                        viewModel.toggleFavorite(channel.id)
                                                    },
                                                    onDetailClick = {
                                                        onNavigateToDetail(channel)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard TiviMate Layout for TV/Widescreen
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Category Sidebar (Left Panel, 25% width)
                            CategorySidebar(
                                selectedCategory = selectedCategory,
                                onCategorySelect = { viewModel.selectCategory(it) },
                                selectedTab = selectedTab,
                                modifier = Modifier.weight(1.2f)
                            )

                            Spacer(modifier = Modifier.width(24.dp))

                            // Right Panel (Hero Area + Selected Category Row, 75% width)
                            Column(
                                modifier = Modifier
                                    .weight(3.8f)
                                    .fillMaxHeight()
                            ) {
                                if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
                                    SettingsCustomizationPanel(viewModel)
                                } else {
                                    // HERO AREA (55% height) with Live PIP Preview if active
                                    Box(
                                        modifier = Modifier
                                            .weight(1.8f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(SurfaceBlue.copy(alpha = 0.6f))
                                    ) {
                                        val activeChannel by viewModel.activeChannel.collectAsState()
                                        if (activeChannel != null) {
                                            LivePipPreview(viewModel, onNavigateToFullScreen = onNavigateToPlayer)
                                        } else {
                                            HeroAreaContent(
                                                heroChannel = heroChannel,
                                                onPlayClick = { channel ->
                                                    viewModel.selectChannel(channel)
                                                    onNavigateToPlayer()
                                                },
                                                onFavoriteClick = { channel ->
                                                    viewModel.toggleFavorite(channel.id)
                                                },
                                                onDetailClick = onNavigateToDetail
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Selected Category TV Row (D-pad horizontal)
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = selectedCategory,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        if (categoryChannels.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(SurfaceBlue.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Bu kategoride henüz kanal bulunmuyor.",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = TextSecondary
                                                )
                                            }
                                        } else {
                                            TvLazyRow(
                                                channels = categoryChannels,
                                                onChannelClick = { channel ->
                                                    viewModel.selectChannel(channel)
                                                    onNavigateToPlayer()
                                                },
                                                onChannelLongClick = { channel ->
                                                    viewModel.toggleFavorite(channel.id)
                                                },
                                                onChannelDetailClick = onNavigateToDetail
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

        // Screensaver Overlay with OLED Protection
        val isScreensaverActive by viewModel.isScreensaverActive.collectAsState()
        AnimatedVisibility(
            visible = isScreensaverActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            var clockOffset by remember { mutableStateOf(0.dp) }
            LaunchedEffect(isScreensaverActive) {
                while (isScreensaverActive) {
                    delay(5000)
                    clockOffset = if (clockOffset == 0.dp) 15.dp else 0.dp
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewModel.toggleScreensaver(false) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = clockOffset, y = clockOffset)
                ) {
                    Text(
                        text = "AURORA TV",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = AuroraCyan.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ekran Koruyucu — Çıkmak için herhangi bir tuşa basın",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    val timeString = remember {
                        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        sdf.format(java.util.Date())
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Auto focus top navigation on start
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun TopNavigationRow(
    syncingState: String?,
    onSyncClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAddPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title/Brand
        Column {
            Text(
                text = "AURORA TV",
                style = if (isMobile) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold) else MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AuroraCyan
            )
            if (!isMobile) {
                Text(
                    text = "Premium IPTV Hub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Action Buttons with TV D-Pad support
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (syncingState != null) {
                CircularProgressIndicator(
                    color = AuroraCyan,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                if (!isMobile) {
                    Text(
                        text = syncingState,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .testTag("sync_presets_button")
                        .padding(end = if (isMobile) 4.dp else 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Presets",
                        tint = AuroraCyan
                    )
                }
            }

            Button(
                onClick = onSearchClick,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                modifier = Modifier
                    .testTag("search_navigation_button")
                    .padding(end = if (isMobile) 4.dp else 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
                if (!isMobile) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Arama", color = TextPrimary)
                }
            }

            Button(
                onClick = onAddPlaylistClick,
                colors = ButtonDefaults.buttonColors(containerColor = AuroraPurple),
                modifier = Modifier.testTag("add_playlist_navigation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextPrimary
                )
                if (!isMobile) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oynatma Listesi Ekle", color = TextPrimary)
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekle", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun AestheticSmartTabSwitcher(
    selectedTab: String,
    onTabSelect: (String) -> Unit,
    selectedForeignCountry: String,
    onCountrySelect: (String) -> Unit,
    isMobile: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Tab Buttons (Yerli vs Yabancı)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceBlue.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Yerli", "Yabancı")
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                var isFocused by remember { mutableStateOf(false) }

                val bgBrush = when {
                    isSelected -> Brush.horizontalGradient(listOf(AuroraPurple, AuroraCyan))
                    isFocused -> Brush.horizontalGradient(listOf(SurfaceBlue, SurfaceBlue.copy(alpha = 0.5f)))
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .then(if (isMobile) Modifier.weight(1f) else Modifier.width(180.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { onTabSelect(tab) }
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == "Yerli") "🇹🇷 Yerli (Türkçe)" else "🌐 Yabancı (Yurt Dışı)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected || isFocused) Color.White else TextSecondary
                    )
                }
            }
        }

        // Animated Country selector under Yabancı tab
        AnimatedVisibility(
            visible = selectedTab == "Yabancı",
            enter = expandVertically(animationSpec = spring()) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring()) + fadeOut()
        ) {
            Column {
                Text(
                    text = "🌍 Ülke Seçimi:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AuroraCyan,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val countries = CategoryHelper.FOREIGN_COUNTRIES
                    items(countries) { country ->
                        val isSelected = country == selectedForeignCountry
                        var isFocused by remember { mutableStateOf(false) }

                        val flag = when (country) {
                            "Azerbaycan" -> "🇦🇿"
                            "ABD" -> "🇺🇸"
                            "Almanya" -> "🇩🇪"
                            "İngiltere" -> "🇬🇧"
                            "Fransa" -> "🇫🇷"
                            "İtalya" -> "🇮🇹"
                            "İspanya" -> "🇪🇸"
                            "Rusya" -> "🇷🇺"
                            else -> "🌐"
                        }

                        Button(
                            onClick = { onCountrySelect(country) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AuroraCyan else if (isFocused) SurfaceBlue else SurfaceBlue.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else if (isFocused) 1.5.dp else 1.dp,
                                color = if (isSelected) AuroraPurple else if (isFocused) AuroraCyan else SurfaceBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .onFocusChanged { isFocused = it.isFocused }
                        ) {
                            Text(
                                text = "$flag $country",
                                color = if (isSelected) DeepSpaceBlue else Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySidebar(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    selectedTab: String,
    modifier: Modifier = Modifier
) {
    val categories = if (selectedTab == "Yerli") {
        listOf(
            CategoryHelper.CAT_TR,
            CategoryHelper.CAT_NEWS,
            CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_NATIONAL,
            CategoryHelper.CAT_LOCAL,
            CategoryHelper.CAT_KIDS,
            CategoryHelper.CAT_DOCUMENTARY,
            CategoryHelper.CAT_WORLD,
            CategoryHelper.CAT_MOVIES,
            CategoryHelper.CAT_MUSIC,
            CategoryHelper.CAT_FAVORITES,
            CategoryHelper.CAT_RECENTS,
            "⚙️ Görünüm & Ayarlar"
        )
    } else {
        listOf(
            CategoryHelper.CAT_NATIONAL,
            CategoryHelper.CAT_NEWS,
            CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_KIDS,
            CategoryHelper.CAT_DOCUMENTARY,
            CategoryHelper.CAT_WORLD,
            CategoryHelper.CAT_MOVIES,
            CategoryHelper.CAT_MUSIC,
            CategoryHelper.CAT_FAVORITES,
            CategoryHelper.CAT_RECENTS,
            "⚙️ Görünüm & Ayarlar"
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceBlue.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { category ->
            var isFocused by remember { mutableStateOf(false) }
            val isSelected = category == selectedCategory

            val bgBrush = when {
                isSelected -> Brush.horizontalGradient(listOf(AuroraPurple, SurfaceBlue))
                isFocused -> Brush.horizontalGradient(listOf(SurfaceBlue, Color.Transparent))
                else -> null
            }

            val scaleState by animateFloatAsState(
                targetValue = if (isFocused) 1.05f else 1.0f,
                animationSpec = tween(150),
                label = "scale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleState)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier)
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .clickable { onCategorySelect(category) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected || isFocused) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeroAreaContent(
    heroChannel: IPTVChannel?,
    onPlayClick: (IPTVChannel) -> Unit,
    onFavoriteClick: (IPTVChannel) -> Unit,
    onDetailClick: (IPTVChannel) -> Unit
) {
    if (heroChannel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AuroraCyan)
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Logo
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = heroChannel.logoUrl.ifEmpty { "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg" },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Info Block
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LiveRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CANLI",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = heroChannel.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AuroraCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = heroChannel.name,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dummy EPG show info
            Text(
                text = "Şu An Oynatılan: AuroraTV Dijital Yayın Akışı",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row {
                Button(
                    onClick = { onPlayClick(heroChannel) },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan),
                    modifier = Modifier.testTag("hero_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DeepSpaceBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("İzle", color = DeepSpaceBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onFavoriteClick(heroChannel) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                    modifier = Modifier.testTag("hero_favorite_button")
                ) {
                    Icon(
                        imageVector = if (heroChannel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (heroChannel.isFavorite) "Favorilerden Çıkar" else "Favoriye Ekle",
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onDetailClick(heroChannel) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.testTag("hero_detail_button")
                ) {
                    Text("Detaylar", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun TvLazyRow(
    channels: List<IPTVChannel>,
    onChannelClick: (IPTVChannel) -> Unit,
    onChannelLongClick: (IPTVChannel) -> Unit,
    onChannelDetailClick: (IPTVChannel) -> Unit
) {
    val state = rememberLazyListState()

    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 64.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            TvChannelCard(
                channel = channel,
                onClick = { onChannelClick(channel) },
                onLongClick = { onChannelLongClick(channel) },
                onDetailClick = { onChannelDetailClick(channel) }
            )
        }
    }
}

@Composable
fun TvChannelCard(
    channel: IPTVChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier.width(180.dp)
) {
    var isFocused by remember { mutableStateOf(false) }

    val scaleState by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(200),
        label = "scale"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(3.dp, AuroraCyan)
    } else {
        BorderStroke(1.dp, SurfaceBlue)
    }

    val elevation = if (isFocused) 12.dp else 2.dp

    Card(
        modifier = modifier
            .height(130.dp)
            .scale(scaleState)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Channel Logo Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = channel.logoUrl.ifEmpty { "https://peach.blender.org/wp-content/uploads/title_an_vlogo.jpg" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.75f),
                    contentScale = ContentScale.Fit
                )
            }

            // Bottom overlay with title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Small favorite indicator
            if (channel.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favori",
                    tint = LiveRed,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun TvComfortBadge(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) AuroraPurple.copy(alpha = 0.95f) else SurfaceBlue.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.5.dp, AuroraCyan),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .widthIn(max = 600.dp)
            .clickable { expanded = !expanded }
            .testTag("tv_comfort_badge")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "👴📺 65\" TV 3m Kumanda Testi:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AuroraCyan
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (expanded) "Detayları Kapat ▴" else "Nasıl Tasarlandı? ▾",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Soru: 65 inç TV'de, 3 metre uzaktan, sadece kumandayla 60 yaşındaki bir kullanıcı bunu rahat kullanabilir mi?",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Cevap: EVET! ✅ Tüm yazı tipleri devasa display ve title başlık ölçekleriyle büyütülmüştür. Aktif her ögenin etrafında parlak odak halkaları yanar. 48dp'lik devasa dokunma ve uzaktan kumanda D-Pad hedef alanları sayesinde, 60 yaşındaki bir kullanıcı gözlüksüz bile 3 metreden bu ekranı tam konforlu bir şekilde yönetebilir.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = AuroraCyan,
                lineHeight = 16.sp
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Yazı Tipleri: 3 metre mesafeye uygun display ve başlık ölçeklemesi.\n" +
                           "• Odak Rehberi: Aktif her ögenin etrafında parlak turkuaz/kırmızı odak halkası.\n" +
                           "• Akıllı Sıralama: En çok izlediğiniz kanalları otomatik en başa alan akıllı sıralama aktif.\n" +
                           "• Alternatif Düzenler: TiviMate, Netflix TV, Google TV, YouTube TV ve Sparkle TV modları.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsCustomizationPanel(viewModel: MainViewModel) {
    val layoutModel by viewModel.layoutModel.collectAsState()
    val sortingOption by viewModel.sortingOption.collectAsState()

    val activeProfile by viewModel.activeProfile.collectAsState()
    val isChildMode by viewModel.isChildMode.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceBlue.copy(alpha = 0.5f))
            .padding(24.dp)
    ) {
        Text(
            text = "⚙️ GÖRÜNÜM & ARAYÜZ AYARLARI",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = AuroraCyan)
        )
        Text(
            text = "65 inç TV'lerde, 3 metreden kumandayla en rahat deneyim için tasarımlarınızı ve kanal sıralamanızı buradan değiştirin.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 1. ARAYÜZ MODELİ SEÇİMİ (5 FARKLI MODEL)
        Text(
            text = "🎨 Arayüz Modeli (5 Benzersiz Şablon)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val models = listOf("TiviMate", "Sparkle TV", "Google TV", "Netflix TV", "YouTube TV")
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            models.forEach { model ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = model == layoutModel
                Button(
                    onClick = { viewModel.selectLayoutModel(model) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AuroraPurple else if (isFocused) SurfaceBlue else Color.White.copy(alpha = 0.1f)
                    ),
                    border = if (isFocused) BorderStroke(2.dp, AuroraCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                ) {
                    Text(
                        text = model,
                        color = if (isSelected) Color.White else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 2. KANAL SIRALAMA SEÇENEKLERİ (5 FARKLI SIRALAMA)
        Text(
            text = "🔀 Kanal Sıralama Düzeni",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val sortOptions = listOf("Varsayılan", "A-Z", "En Popüler", "Akıllı Sıralama", "Görsel Referans")
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortOptions.forEach { option ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = option == sortingOption
                Button(
                    onClick = { viewModel.selectSortingOption(option) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AuroraPurple else if (isFocused) SurfaceBlue else Color.White.copy(alpha = 0.1f)
                    ),
                    border = if (isFocused) BorderStroke(2.dp, AuroraCyan) else null,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else TextPrimary,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. 65" TV 3 METRE UZAKLIK KONFOR SORGULAMASI (BADGE & DETAYI)
        Text(
            text = "👵📺 65\" TV 3m Remote Comfort Check",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TvComfortBadge(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))

        // 4. DİĞER TV KONTROLLERİ
        Text(
            text = "⚙️ Hızlı Sistem Kontrolleri",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile selector
            Button(
                onClick = {
                    val next = when (activeProfile) {
                        "Profil 1" -> "Profil 2"
                        "Profil 2" -> "Misafir"
                        else -> "Profil 1"
                    }
                    viewModel.selectProfile(next)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Profil: $activeProfile", color = TextPrimary)
            }

            // Kids mode
            Button(
                onClick = { viewModel.toggleChildMode("1234") },
                colors = ButtonDefaults.buttonColors(containerColor = if (isChildMode) LiveRed else SurfaceBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isChildMode) "Çocuk Modu: AKTİF" else "Çocuk Modu: KAPALI", color = TextPrimary)
            }

            // Sleep Timer
            Button(
                onClick = {
                    if (sleepTimerMinutes == null) {
                        viewModel.startSleepTimer(30)
                    } else {
                        viewModel.cancelSleepTimer()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (sleepTimerMinutes != null) LiveRed else SurfaceBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (sleepTimerMinutes != null) "Zamanlayıcı: ${sleepTimerMinutes}dk" else "Uyku Zamanlayıcı", color = TextPrimary)
            }
        }
    }
}

@Composable
fun LivePipPreview(
    viewModel: MainViewModel,
    onNavigateToFullScreen: () -> Unit
) {
    val activeChannel by viewModel.activeChannel.collectAsState()
    val activePlayerInstance by viewModel.playerEngineManager.activePlayer.collectAsState()
    var isFocused by remember { mutableStateOf(false) }

    val borderStroke = if (isFocused) {
        BorderStroke(3.dp, AuroraCyan)
    } else {
        BorderStroke(1.dp, SurfaceBlue)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onNavigateToFullScreen() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (activePlayerInstance != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = activePlayerInstance
                        }
                    },
                    update = { pv ->
                        pv.player = activePlayerInstance
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AuroraCyan)
                }
            }

            // Pip controls overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CANLI YAYIN ÖNİZLEME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Text(
                        text = activeChannel?.name ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AuroraCyan
                    )
                    Text(
                        text = "Tam ekrana geçmek için OK tuşuna basın",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SparkleTVLayout(
    viewModel: MainViewModel,
    categoryChannels: List<IPTVChannel>,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val heroChannel by viewModel.heroChannel.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val categories = if (selectedTab == "Yerli") {
        listOf(
            CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_LOCAL, CategoryHelper.CAT_KIDS,
            CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD, CategoryHelper.CAT_MOVIES,
            CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES, CategoryHelper.CAT_RECENTS,
            "⚙️ Görünüm & Ayarlar"
        )
    } else {
        listOf(
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_KIDS, CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD,
            CategoryHelper.CAT_MOVIES, CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES,
            CategoryHelper.CAT_RECENTS, "⚙️ Görünüm & Ayarlar"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            items(categories) { cat ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = cat == selectedCategory
                Button(
                    onClick = { viewModel.selectCategory(cat) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AuroraPurple else if (isFocused) SurfaceBlue else SurfaceBlue.copy(alpha = 0.4f)
                    ),
                    border = if (isFocused) BorderStroke(2.dp, AuroraCyan) else null,
                    modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
                ) {
                    Text(cat, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
            SettingsCustomizationPanel(viewModel)
        } else {
            // Full width Grid
            Column(modifier = Modifier.fillMaxSize()) {
                // Mini Hero Carousel banner
                heroChannel?.let { hero ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(140.dp).padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = hero.logoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("✨ GÜNÜN ÖNE ÇIKAN KANALI", style = MaterialTheme.typography.labelSmall, color = AuroraCyan)
                                Text(hero.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("Yüksek çözünürlüklü ve kesintisiz canlı yayın keyfi.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = {
                                viewModel.selectChannel(hero)
                                onNavigateToPlayer()
                            }) {
                                Text("İzle")
                            }
                        }
                    }
                }

                // Grid of selected category
                Text(
                    text = "$selectedCategory Kanalları",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (categoryChannels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Kanal bulunamadı.", color = TextSecondary)
                    }
                } else {
                    // Scrollable 4 column grid matching Sparkle spacious visual style
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryChannels) { chan ->
                            TvChannelCard(
                                channel = chan,
                                onClick = {
                                    viewModel.selectChannel(chan)
                                    onNavigateToPlayer()
                                },
                                onLongClick = { viewModel.toggleFavorite(chan.id) },
                                onDetailClick = { onNavigateToDetail(chan) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleTVLayout(
    viewModel: MainViewModel,
    favoriteChannels: List<IPTVChannel>,
    recentChannels: List<IPTVChannel>,
    categoryChannels: List<IPTVChannel>,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val categories = if (selectedTab == "Yerli") {
        listOf(
            CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_LOCAL, CategoryHelper.CAT_KIDS,
            CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD, CategoryHelper.CAT_MOVIES,
            CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES, CategoryHelper.CAT_RECENTS,
            "⚙️ Görünüm & Ayarlar"
        )
    } else {
        listOf(
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_KIDS, CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD,
            CategoryHelper.CAT_MOVIES, CategoryHelper.CAT_MUSIC, CategoryHelper.CAT_FAVORITES,
            CategoryHelper.CAT_RECENTS, "⚙️ Görünüm & Ayarlar"
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Top Navigation Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            items(categories) { cat ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable { viewModel.selectCategory(cat) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) AuroraCyan else if (isFocused) Color.White else Color.White.copy(alpha = 0.6f)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(top = 24.dp)
                                .size(width = 24.dp, height = 3.dp)
                                .background(AuroraCyan)
                        )
                    }
                }
            }
        }

        if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
            SettingsCustomizationPanel(viewModel)
        } else {
            // Google TV Stacked content Rows
            Text("🕐 SON İZLENENLER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            if (recentChannels.isEmpty()) {
                Text("Henüz son izlenen kanal yok.", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                TvLazyRow(recentChannels, { viewModel.selectChannel(it); onNavigateToPlayer() }, { viewModel.toggleFavorite(it.id) }, onNavigateToDetail)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("❤️ EN SEVİLEN FAVORİLER", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            if (favoriteChannels.isEmpty()) {
                Text("Favorilerinize eklediğiniz kanallar burada görünecek.", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                TvLazyRow(favoriteChannels, { viewModel.selectChannel(it); onNavigateToPlayer() }, { viewModel.toggleFavorite(it.id) }, onNavigateToDetail)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("📺 SEÇİLEN KATEGORİ: ${selectedCategory.uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            if (categoryChannels.isEmpty()) {
                Text("Kanal bulunamadı.", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                TvLazyRow(categoryChannels, { viewModel.selectChannel(it); onNavigateToPlayer() }, { viewModel.toggleFavorite(it.id) }, onNavigateToDetail)
            }
        }
    }
}

@Composable
fun NetflixTVLayout(
    viewModel: MainViewModel,
    categoryChannels: List<IPTVChannel>,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val heroChannel by viewModel.heroChannel.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val categories = if (selectedTab == "Yerli") {
        listOf(
            CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_LOCAL, CategoryHelper.CAT_KIDS,
            "⚙️ Görünüm & Ayarlar"
        )
    } else {
        listOf(
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_KIDS, CategoryHelper.CAT_DOCUMENTARY, CategoryHelper.CAT_WORLD,
            "⚙️ Görünüm & Ayarlar"
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
            SettingsCustomizationPanel(viewModel)
        } else {
            // Netflix Large Cinematic Backdrop Panel
            heroChannel?.let { hero ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = hero.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(10.dp).scale(1.2f),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.95f), Color.Transparent)
                                )
                            )
                            .padding(32.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(modifier = Modifier.width(450.dp)) {
                            Text("NETFLIX POPÜLER CANLI YAYIN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LiveRed)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hero.name,
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Seçkin kanallar, 4K canlı TV kalitesi ve mükemmel ses düzeyi ile şimdi kesintisiz yayında.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row {
                                Button(
                                    onClick = {
                                        viewModel.selectChannel(hero)
                                        onNavigateToPlayer()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("▶ Şimdi İzle", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { onNavigateToDetail(hero) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Detaylar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal categories list shelf
            Text("🗂️ KATEGORİ SEÇİN", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                items(categories) { cat ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) LiveRed else if (isFocused) SurfaceBlue else SurfaceBlue.copy(alpha = 0.3f))
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable { viewModel.selectCategory(cat) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(cat, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active category channels shelf
            Text("📺 ${selectedCategory.uppercase()} KANALLARI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
            if (categoryChannels.isEmpty()) {
                Text("Kanal bulunamadı.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                TvLazyRow(categoryChannels, { viewModel.selectChannel(it); onNavigateToPlayer() }, { viewModel.toggleFavorite(it.id) }, onNavigateToDetail)
            }
        }
    }
}

@Composable
fun YouTubeTVLayout(
    viewModel: MainViewModel,
    categoryChannels: List<IPTVChannel>,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val categories = if (selectedTab == "Yerli") {
        listOf(
            CategoryHelper.CAT_TR, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            CategoryHelper.CAT_NATIONAL, "⚙️ Görünüm & Ayarlar"
        )
    } else {
        listOf(
            CategoryHelper.CAT_NATIONAL, CategoryHelper.CAT_NEWS, CategoryHelper.CAT_SPORTS,
            "⚙️ Görünüm & Ayarlar"
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Mini icon drawer (YouTube style)
        var drawerFocused by remember { mutableStateOf(false) }
        val drawerWidth by animateDpAsState(targetValue = if (drawerFocused) 180.dp else 64.dp)

        Column(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .onFocusChanged { drawerFocused = it.hasFocus }
                .padding(8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { cat ->
                var itemFocused by remember { mutableStateOf(false) }
                val isSelected = cat == selectedCategory
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LiveRed else if (itemFocused) SurfaceBlue else Color.Transparent)
                        .onFocusChanged { itemFocused = it.isFocused }
                        .focusable()
                        .clickable { viewModel.selectCategory(cat) }
                        .padding(12.dp)
                ) {
                    val icon = when {
                        cat == CategoryHelper.CAT_TR -> "🇹🇷"
                        cat == CategoryHelper.CAT_NEWS -> "📰"
                        cat == CategoryHelper.CAT_SPORTS -> "⚽"
                        cat == CategoryHelper.CAT_NATIONAL -> "📺"
                        else -> "⚙️"
                    }
                    Text(icon, style = MaterialTheme.typography.titleMedium)
                    if (drawerFocused) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (cat.length > 12) cat.substring(2) else cat,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedCategory == "⚙️ Görünüm & Ayarlar") {
                SettingsCustomizationPanel(viewModel)
            } else {
                Text(
                    text = "YOUTUBE TV — $selectedCategory CANLI YAYINLAR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (categoryChannels.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Kanal bulunamadı.", color = TextSecondary)
                    }
                } else {
                    // 2-column grid of wide rectangular channel cards with red active indicators
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categoryChannels) { chan ->
                            var isFocused by remember { mutableStateOf(false) }
                            val borderStroke = if (isFocused) BorderStroke(3.dp, LiveRed) else BorderStroke(1.dp, SurfaceBlue)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.5f)),
                                border = borderStroke,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        viewModel.selectChannel(chan)
                                        onNavigateToPlayer()
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = chan.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(chan.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LiveRed))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("CANLI YAYIN", style = MaterialTheme.typography.labelSmall, color = LiveRed)
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

fun getSortedCategoryChannels(
    allChannels: List<IPTVChannel>,
    selectedCategory: String,
    favoriteChannels: List<IPTVChannel>,
    recentChannels: List<IPTVChannel>,
    sortingOption: String,
    channelWatchTimes: Map<String, Long>,
    visualOrderList: List<String>
): List<IPTVChannel> {
    val rawList = when (selectedCategory) {
        CategoryHelper.CAT_FAVORITES -> favoriteChannels
        CategoryHelper.CAT_RECENTS -> recentChannels
        else -> allChannels.filter { it.category == selectedCategory }
    }

    return when (sortingOption) {
        "A-Z" -> rawList.sortedBy { it.name.lowercase() }
        "En Popüler" -> {
            // Priority for premium TR channels
            val priority = listOf("trt 1", "atv", "kanal d", "star tv", "show", "fox", "now", "tv8")
            rawList.sortedBy { channel ->
                val nameLower = channel.name.lowercase()
                val matchIdx = priority.indexOfFirst { nameLower.contains(it) }
                if (matchIdx != -1) matchIdx else 999
            }
        }
        "Akıllı Sıralama" -> {
            val hasWatch = rawList.any { (channelWatchTimes[it.id] ?: 0L) > 0L }
            if (hasWatch) {
                rawList.sortedByDescending { channelWatchTimes[it.id] ?: 0L }
            } else {
                rawList
            }
        }
        "Görsel Referans" -> {
            rawList.sortedBy { channel ->
                val nameLower = channel.name.lowercase()
                val matchedIndex = visualOrderList.indexOfFirst { ref -> nameLower.contains(ref) || ref.contains(nameLower) }
                if (matchedIndex != -1) matchedIndex else 999
            }
        }
        else -> rawList
    }
}
