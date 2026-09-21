package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.repository.OPEN_SOURCE_PRESETS
import com.example.data.repository.PresetSource
import com.example.domain.model.IPTVChannel
import com.example.ui.components.SilentPreviewPlayer
import com.example.domain.util.CategoryHelper
import com.example.ui.theme.*
import com.example.ui.tv.dpadFocusable
import com.example.ui.viewmodel.MainViewModel

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
    val heroChannel by viewModel.heroChannel.collectAsState()
    val syncingState by viewModel.syncingState.collectAsState()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isTvMode = configuration.screenWidthDp >= 650 && !isPortrait

    // Active Bottom Navigation Tab for Mobile
    var activeMobileTab by remember { mutableIntStateOf(0) } // 0: Live, 1: Favorites, 2: Settings
    var selectedCategoryFilter by remember { mutableStateOf(CategoryHelper.CAT_ALL) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1F45), // Deep luxury midnight blue
                        DeepSpaceBlue       // Dark navy edge
                    ),
                    radius = 2000f
                )
            )
    ) {
        // Atmospheric Ambient Glow from Hero Channel
        heroChannel?.let { hero ->
            if (hero.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = hero.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                        .scale(1.3f),
                    contentScale = ContentScale.Crop,
                    alpha = 0.20f
                )
            }
        }

        // Dark Vignette Overlay
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

        if (isTvMode) {
            // ANDROID TV LEANBACK MODE
            TvHomeScreen(
                viewModel = viewModel,
                allChannels = allChannels,
                favoriteChannels = favoriteChannels,
                heroChannel = heroChannel,
                syncingState = syncingState,
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToAddPlaylist = onNavigateToAddPlaylist
            )
        } else {
            // SMARTPHONE & COMPACT TABLET MODE
            MobileHomeScreen(
                viewModel = viewModel,
                allChannels = allChannels,
                favoriteChannels = favoriteChannels,
                recentChannels = recentChannels,
                heroChannel = heroChannel,
                syncingState = syncingState,
                activeTab = activeMobileTab,
                onTabSelect = { activeMobileTab = it },
                selectedCategory = selectedCategoryFilter,
                onCategorySelect = { selectedCategoryFilter = it },
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToAddPlaylist = onNavigateToAddPlaylist
            )
        }
    }
}

/* ==========================================================================
   MOBILE SMARTPHONE LAYOUT
   ========================================================================== */

@Composable
fun MobileHomeScreen(
    viewModel: MainViewModel,
    allChannels: List<IPTVChannel>,
    favoriteChannels: List<IPTVChannel>,
    recentChannels: List<IPTVChannel>,
    heroChannel: IPTVChannel?,
    syncingState: String?,
    activeTab: Int,
    onTabSelect: (Int) -> Unit,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemeConfig.current.palette

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(true) }

    // World Channels Filtering States
    var selectedCountry by remember { mutableStateOf("Azerbaycan") }
    var selectedGenre by remember { mutableStateOf("Tümü") }

    val countries = listOf("Tümü", "Azerbaycan", "Almanya", "ABD", "İngiltere", "Fransa", "İtalya", "İspanya", "Rusya", "Türkiye", "Global / Diğer")
    val genres = listOf("Tümü", "Spor", "Haber", "Ulusal Kanallar", "Belgesel", "Müzik", "Çocuk", "Film/Dizi")

    // Dynamic filtering for all tabs
    val filteredChannels = remember(allChannels, favoriteChannels, activeTab, selectedCategory, searchQuery, selectedCountry, selectedGenre) {
        val baseList = when (activeTab) {
            0 -> { // Live/Local
                if (selectedCategory == CategoryHelper.CAT_ALL) allChannels
                else allChannels.filter { it.category.equals(selectedCategory, ignoreCase = true) || it.groupTitle.contains(selectedCategory, ignoreCase = true) }
            }
            1 -> favoriteChannels // Favorites
            2 -> { // World Channels
                allChannels.filter { chan ->
                    val detectedCountry = CategoryHelper.detectCountry(chan.name, chan.groupTitle)
                    val countryMatch = if (selectedCountry == "Tümü") {
                        detectedCountry != "Türkiye" // World channels means non-Turkish by default
                    } else {
                        detectedCountry.equals(selectedCountry, ignoreCase = true)
                    }

                    val smartGenre = CategoryHelper.getSmartCategory(chan.name, chan.groupTitle, chan.tvgId)
                    val genreMatch = if (selectedGenre == "Tümü") true
                    else {
                        smartGenre.contains(selectedGenre, ignoreCase = true) || chan.category.contains(selectedGenre, ignoreCase = true)
                    }
                    countryMatch && genreMatch
                }
            }
            else -> emptyList()
        }

        // Map database favorite status onto active items
        val mappedList = baseList.map { chan ->
            val isFav = favoriteChannels.any { fav -> fav.id == chan.id }
            chan.copy(isFavorite = isFav)
        }

        val filtered = if (searchQuery.isBlank()) {
            mappedList
        } else {
            mappedList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }

        val categoryPriority = { cat: String ->
            when {
                cat.equals(CategoryHelper.CAT_NATIONAL, ignoreCase = true) -> 1
                cat.equals(CategoryHelper.CAT_SPORTS, ignoreCase = true) -> 2
                cat.equals(CategoryHelper.CAT_NEWS, ignoreCase = true) -> 3
                cat.equals(CategoryHelper.CAT_MUSIC, ignoreCase = true) -> 4
                cat.equals(CategoryHelper.CAT_LOCAL, ignoreCase = true) -> 5
                cat.equals(CategoryHelper.CAT_MOVIES, ignoreCase = true) -> 6
                cat.equals(CategoryHelper.CAT_KIDS, ignoreCase = true) -> 7
                cat.equals(CategoryHelper.CAT_DOCUMENTARY, ignoreCase = true) -> 8
                cat.equals(CategoryHelper.CAT_TR, ignoreCase = true) -> 9
                cat.equals(CategoryHelper.CAT_WORLD, ignoreCase = true) -> 10
                else -> 100
            }
        }

        // Sort so that favorites are grouped at the top of the list, followed by category priority and then name
        if (activeTab == 1) {
            filtered.sortedBy { it.name }
        } else {
            filtered.sortedWith(
                compareByDescending<IPTVChannel> { it.isFavorite }
                    .thenBy { categoryPriority(it.category) }
                    .thenBy { it.name }
            )
        }
    }

    // Keep track of the currently selected/focused channel for the silent preview player
    var focusedChannel by remember { mutableStateOf<IPTVChannel?>(null) }

    // Automatically focus on the first channel when the list changes
    LaunchedEffect(filteredChannels) {
        if (!filteredChannels.contains(focusedChannel)) {
            focusedChannel = filteredChannels.firstOrNull()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                color = palette.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    val tabs = listOf(
                        Triple(0, "Canlı TV", Icons.Default.Tv),
                        Triple(1, "Favoriler", Icons.Default.Favorite),
                        Triple(2, "Dünya Kanalları", Icons.Default.Language),
                        Triple(3, "Ayarlar & Liste", Icons.Default.Settings)
                    )

                    tabs.forEach { (tabIndex, title, icon) ->
                        val selected = activeTab == tabIndex
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelect(tabIndex) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (selected) palette.secondary else Color.White.copy(alpha = 0.5f)
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    color = if (selected) palette.secondary else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = palette.secondary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "PinpirikTV",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Brush.linearGradient(listOf(palette.secondary, palette.primary)), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pinpirik",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "TV",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = palette.secondary
                            )
                        }
                        Text(
                            text = "PREMIUM CANLI YAYIN MOTORU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = palette.secondary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Header Action Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Arama",
                            tint = palette.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.syncPresets() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToAddPlaylist,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "M3U Ekle",
                            tint = palette.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Syncing Status Banner
            syncingState?.let { stateMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.secondary.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = palette.secondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stateMsg,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Expandable Search Bar Input
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Kanal veya kategori ara...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = palette.secondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle", tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = palette.secondary,
                        unfocusedBorderColor = palette.surface,
                        focusedContainerColor = palette.surface.copy(alpha = 0.8f),
                        unfocusedContainerColor = palette.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            if (activeTab == 3) {
                // Settings Tab
                MobileSettingsView(
                    viewModel = viewModel,
                    onNavigateToAddPlaylist = onNavigateToAddPlaylist
                )
            } else {
                // Adaptive Layout Container
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWidescreen = maxWidth >= 720.dp

                    if (isWidescreen) {
                        // Two-Pane side-by-side layout
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Channels list (60% weight)
                            Column(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                            ) {
                                // Filters Row
                                TabFiltersRow(
                                    activeTab = activeTab,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = onCategorySelect,
                                    selectedCountry = selectedCountry,
                                    onCountrySelect = { selectedCountry = it },
                                    selectedGenre = selectedGenre,
                                    onGenreSelect = { selectedGenre = it },
                                    countries = countries,
                                    genres = genres,
                                    palette = palette
                                )

                                ChannelsLazyList(
                                    channels = filteredChannels,
                                    focusedChannel = focusedChannel,
                                    activeTab = activeTab,
                                    allChannelsEmpty = allChannels.isEmpty(),
                                    onChannelClick = { channel ->
                                        if (focusedChannel == channel) {
                                            viewModel.selectChannel(channel)
                                            onNavigateToPlayer()
                                        } else {
                                            focusedChannel = channel
                                        }
                                    },
                                    onChannelDoubleClick = { channel ->
                                        viewModel.selectChannel(channel)
                                        onNavigateToPlayer()
                                    },
                                    onFavoriteClick = { channel ->
                                        viewModel.toggleFavorite(channel.id)
                                    },
                                    onNavigateToAddPlaylist = onNavigateToAddPlaylist,
                                    viewModel = viewModel,
                                    palette = palette
                                )
                            }

                            // Right Pane: Sticky Preview Sidebar (40% weight)
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "CANLI ÖNİZLEME",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )

                                SilentPreviewPlayer(
                                    channel = focusedChannel,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                focusedChannel?.let { chan ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = palette.surface.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = chan.name,
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${chan.category} • ${if (chan.groupTitle.isNotBlank()) chan.groupTitle else "Genel"}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.secondary
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Button(
                                                    onClick = {
                                                        viewModel.selectChannel(chan)
                                                        onNavigateToPlayer()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = palette.secondary, contentColor = palette.background),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Tam Ekran İzle", fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.toggleFavorite(chan.id) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(palette.surface, RoundedCornerShape(12.dp))
                                                ) {
                                                    Icon(
                                                        imageVector = if (chan.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Favori",
                                                        tint = if (chan.isFavorite) palette.liveBadge else Color.White.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait vertically stacked layout
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Sticky Preview Player at the top
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                SilentPreviewPlayer(
                                    channel = focusedChannel,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Fast launch overlay button
                                focusedChannel?.let { chan ->
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.selectChannel(chan)
                                                onNavigateToPlayer()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = palette.secondary, contentColor = palette.background),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tam Ekran", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }

                            // Horizontal Filters Row
                            TabFiltersRow(
                                activeTab = activeTab,
                                selectedCategory = selectedCategory,
                                onCategorySelect = onCategorySelect,
                                selectedCountry = selectedCountry,
                                onCountrySelect = { selectedCountry = it },
                                selectedGenre = selectedGenre,
                                onGenreSelect = { selectedGenre = it },
                                countries = countries,
                                genres = genres,
                                palette = palette
                            )

                            // Rest: Channel Scrollable List
                            Box(modifier = Modifier.weight(1f)) {
                                ChannelsLazyList(
                                    channels = filteredChannels,
                                    focusedChannel = focusedChannel,
                                    activeTab = activeTab,
                                    allChannelsEmpty = allChannels.isEmpty(),
                                    onChannelClick = { channel ->
                                        if (focusedChannel == channel) {
                                            viewModel.selectChannel(channel)
                                            onNavigateToPlayer()
                                        } else {
                                            focusedChannel = channel
                                        }
                                    },
                                    onChannelDoubleClick = { channel ->
                                        viewModel.selectChannel(channel)
                                        onNavigateToPlayer()
                                    },
                                    onFavoriteClick = { channel ->
                                        viewModel.toggleFavorite(channel.id)
                                    },
                                    onNavigateToAddPlaylist = onNavigateToAddPlaylist,
                                    viewModel = viewModel,
                                    palette = palette
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabFiltersRow(
    activeTab: Int,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    selectedCountry: String,
    onCountrySelect: (String) -> Unit,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    countries: List<String>,
    genres: List<String>,
    palette: AppColorPalette,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (activeTab == 0) {
            // Live TV Smart Categories Filter
            val categories = listOf(
                CategoryHelper.CAT_ALL to "Tümü",
                CategoryHelper.CAT_NATIONAL to "Ulusal",
                CategoryHelper.CAT_SPORTS to "Spor",
                CategoryHelper.CAT_NEWS to "Haber",
                CategoryHelper.CAT_MOVIES to "Sinema",
                CategoryHelper.CAT_MUSIC to "Müzik",
                CategoryHelper.CAT_DOCUMENTARY to "Belgesel",
                CategoryHelper.CAT_KIDS to "Çocuk",
                CategoryHelper.CAT_LOCAL to "Yerel",
                CategoryHelper.CAT_WORLD to "Dünya"
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (catId, label) ->
                    val isSelected = selectedCategory == catId
                    Surface(
                        color = if (isSelected) palette.secondary else palette.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isSelected) palette.secondary else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.clickable { onCategorySelect(catId) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) palette.background else Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else if (activeTab == 2) {
            // World Channels filters: Country then Genre
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // 1. Country Filter (Ülke Seçimi)
                Text(
                    text = "Ülke Seçimi",
                    color = palette.secondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(countries) { country ->
                        val isSelected = selectedCountry == country
                        Surface(
                            color = if (isSelected) palette.secondary else palette.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) palette.secondary else Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { onCountrySelect(country) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (country) {
                                        "Azerbaycan" -> "🇦🇿 "
                                        "Almanya" -> "🇩🇪 "
                                        "ABD" -> "🇺🇸 "
                                        "İngiltere" -> "🇬🇧 "
                                        "Fransa" -> "🇫🇷 "
                                        "İtalya" -> "🇮🇹 "
                                        "İspanya" -> "🇪🇸 "
                                        "Rusya" -> "🇷🇺 "
                                        "Türkiye" -> "🇹🇷 "
                                        else -> "🌐 "
                                    } + country,
                                    color = if (isSelected) palette.background else Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Genre Filter (Tür / Kategori Seçimi)
                Text(
                    text = "Kategori / Tür",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres) { genre ->
                        val isSelected = selectedGenre == genre
                        Surface(
                            color = if (isSelected) palette.secondary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) palette.secondary else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onGenreSelect(genre) }
                        ) {
                            Text(
                                text = genre,
                                color = if (isSelected) palette.secondary else Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelsLazyList(
    channels: List<IPTVChannel>,
    focusedChannel: IPTVChannel?,
    activeTab: Int,
    allChannelsEmpty: Boolean,
    onChannelClick: (IPTVChannel) -> Unit,
    onChannelDoubleClick: (IPTVChannel) -> Unit,
    onFavoriteClick: (IPTVChannel) -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    viewModel: MainViewModel,
    palette: AppColorPalette,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (allChannelsEmpty && activeTab == 0) {
                MobileEmptySourceState(
                    onNavigateToAddPlaylist = onNavigateToAddPlaylist,
                    onQuickLoadPreset = { preset -> viewModel.loadOpenSourcePreset(preset) }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TvOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (activeTab == 1) "Henüz favori kanal eklenmedi" else "Seçilen filtrelerde kanal bulunamadı",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                MobileChannelRow(
                    channel = channel,
                    isSelected = focusedChannel == channel,
                    onClick = { onChannelClick(channel) },
                    onDoubleClick = { onChannelDoubleClick(channel) },
                    onFavoriteClick = { onFavoriteClick(channel) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileChannelRow(
    channel: IPTVChannel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeConfig = LocalThemeConfig.current
    val palette = themeConfig.palette

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) palette.secondary.copy(alpha = 0.15f) else palette.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) palette.secondary else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Logo Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF070E24)),
                contentAlignment = Alignment.Center
            ) {
                ChannelLogoImage(
                    logoUrl = channel.logoUrl,
                    category = channel.category,
                    channelName = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette.liveBadge)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Play icon indicator when selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynatılıyor",
                    tint = palette.secondary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp)
                )
            }

            // Right: Favorite toggle button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (channel.isFavorite) palette.liveBadge else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MobileHeroBanner(
    channel: IPTVChannel,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.85f)),
        border = BorderStroke(1.5.dp, AuroraCyan.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AuroraPurple.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo Container
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B142F))
                        .border(1.dp, AuroraCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ChannelLogoImage(
                        logoUrl = channel.logoUrl,
                        category = channel.category,
                        channelName = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info & Action
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing Live Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CANLI SPOTLIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = LiveRed
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${channel.category} • Full HD",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Play Button
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraCyan,
                            contentColor = Color(0xFF07112B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hemen İzle",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Favorite Toggle
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (channel.isFavorite) LiveRed else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MobileChannelCard(
    channel: IPTVChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: HD Badge and Favorite button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0F2352),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "HD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = AuroraCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (channel.isFavorite) LiveRed else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Center Logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0B142F))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                ChannelLogoImage(
                    logoUrl = channel.logoUrl,
                    category = channel.category,
                    channelName = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }

            // Bottom Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = channel.category,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ==========================================================================
   ANDROID TV LEANBACK 10-FOOT LAYOUT
   ========================================================================== */

@Composable
fun TvHomeScreen(
    viewModel: MainViewModel,
    allChannels: List<IPTVChannel>,
    favoriteChannels: List<IPTVChannel>,
    heroChannel: IPTVChannel?,
    syncingState: String?,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDetail: (IPTVChannel) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRailIndex by remember { mutableIntStateOf(0) }
    var tvSearchQuery by remember { mutableStateOf("") }

    // Map database favorite status onto active items
    val processedChannels = remember(allChannels, favoriteChannels) {
        allChannels.map { chan ->
            val isFav = favoriteChannels.any { fav -> fav.id == chan.id }
            chan.copy(isFavorite = isFav)
        }
    }

    var focusedChannel by remember { mutableStateOf<IPTVChannel?>(null) }

    // Auto-focus on the first channel initially
    LaunchedEffect(processedChannels) {
        if (focusedChannel == null || !processedChannels.any { it.id == focusedChannel?.id }) {
            focusedChannel = processedChannels.firstOrNull()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // TV Navigation Rail (Left side)
        TvNavRail(
            selectedIndex = selectedRailIndex,
            onSelectIndex = {
                selectedRailIndex = it
                tvSearchQuery = "" // Clear search when switching tabs
            },
            onSearchClick = onNavigateToSearch,
            onAddPlaylistClick = onNavigateToAddPlaylist,
            onSyncClick = { viewModel.syncPresets() }
        )

        // TV Main Showcase & Content (Right side)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 24.dp, end = 32.dp, top = 20.dp, bottom = 20.dp)
        ) {
            if (processedChannels.isEmpty()) {
                TvEmptySourceState(
                    onNavigateToAddPlaylist = onNavigateToAddPlaylist,
                    onQuickLoadPreset = { preset -> viewModel.loadOpenSourcePreset(preset) }
                )
            } else {
                // TV Search Bar always visible on channel list screen
                OutlinedTextField(
                    value = tvSearchQuery,
                    onValueChange = { tvSearchQuery = it },
                    placeholder = { Text("Kanal listesinde kanal adı veya kategori ara...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AuroraCyan)
                    },
                    trailingIcon = {
                        if (tvSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { tvSearchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle", tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = SurfaceBlue,
                        unfocusedContainerColor = Color(0xFF0C1636).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("tv_home_search_bar")
                )

                // Hero Spotlight Stage
                TvHeroStage(
                    channel = focusedChannel,
                    onPlayClick = {
                        focusedChannel?.let {
                            viewModel.selectChannel(it)
                            onNavigateToPlayer()
                        }
                    },
                    onFavoriteToggle = { ch ->
                        viewModel.toggleFavorite(ch.id)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Categorized Shelves or specific view based on Rail selection / Search Query
                if (tvSearchQuery.isNotEmpty()) {
                    val filteredTv = processedChannels.filter {
                        it.name.contains(tvSearchQuery, ignoreCase = true) ||
                        it.category.contains(tvSearchQuery, ignoreCase = true)
                    }.sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name })

                    TvCategoryGrid(
                        title = "🔍 Arama Sonuçları (${filteredTv.size})",
                        channels = filteredTv,
                        onChannelFocused = { focusedChannel = it },
                        onChannelClick = { ch ->
                            viewModel.selectChannel(ch)
                            onNavigateToPlayer()
                        }
                    )
                } else {
                    when (selectedRailIndex) {
                        0 -> {
                            // "Canlı TV" - All Shelves (Favorites automatically placed at the top shelf!)
                            TvShelvesView(
                                allChannels = processedChannels,
                                favoriteChannels = processedChannels.filter { it.isFavorite },
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                },
                                onChannelLongClick = onNavigateToDetail
                            )
                        }
                        1 -> {
                            // "Favoriler"
                            TvCategoryGrid(
                                title = "★ Favori Kanallarım",
                                channels = processedChannels.filter { it.isFavorite },
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                        2 -> {
                            // "Ulusal"
                            TvCategoryGrid(
                                title = "🇹🇷 Ulusal Kanallar",
                                channels = processedChannels.filter { it.category.equals(CategoryHelper.CAT_NATIONAL, ignoreCase = true) }
                                    .sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name }),
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                        3 -> {
                            // "Spor"
                            TvCategoryGrid(
                                title = "⚽ Spor Kanalları",
                                channels = processedChannels.filter { it.category.equals(CategoryHelper.CAT_SPORTS, ignoreCase = true) }
                                    .sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name }),
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                        4 -> {
                            // "Haber"
                            TvCategoryGrid(
                                title = "📰 Haber & Gündem",
                                channels = processedChannels.filter { it.category.equals(CategoryHelper.CAT_NEWS, ignoreCase = true) }
                                    .sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name }),
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                        5 -> {
                            // "Sinema"
                            TvCategoryGrid(
                                title = "🎬 Film & Sinema",
                                channels = processedChannels.filter { it.category.equals(CategoryHelper.CAT_MOVIES, ignoreCase = true) }
                                    .sortedWith(compareByDescending<IPTVChannel> { it.isFavorite }.thenBy { it.name }),
                                onChannelFocused = { focusedChannel = it },
                                onChannelClick = { ch ->
                                    viewModel.selectChannel(ch)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                        6 -> {
                            // "Ayarlar & Liste"
                            TvSettingsView(
                                viewModel = viewModel,
                                onNavigateToAddPlaylist = onNavigateToAddPlaylist
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvNavRail(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onAddPlaylistClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF070F26).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Icon (Guinea Pig Mascot)
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "PinpirikTV",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(AuroraCyan, AuroraPurple)), CircleShape)
            )

            // Main TV Destinations
            val destinations = listOf(
                Pair(0, Icons.Default.Tv),
                Pair(1, Icons.Default.Favorite),
                Pair(2, Icons.Default.Flag),
                Pair(3, Icons.Default.SportsSoccer),
                Pair(4, Icons.Default.Article),
                Pair(5, Icons.Default.Movie),
                Pair(6, Icons.Default.Settings)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                destinations.forEach { (index, icon) ->
                    val isSelected = selectedIndex == index
                    var isFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isFocused -> AuroraCyan.copy(alpha = 0.3f)
                                    isSelected -> AuroraPurple.copy(alpha = 0.4f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                1.5.dp,
                                when {
                                    isFocused -> AuroraCyan
                                    isSelected -> AuroraPurple
                                    else -> Color.Transparent
                                },
                                RoundedCornerShape(14.dp)
                            )
                            .dpadFocusable(
                                onSelect = { onSelectIndex(index) },
                                onFocusChanged = { isFocused = it }
                            )
                            .clickable { onSelectIndex(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when {
                                isFocused -> AuroraCyan
                                isSelected -> Color.White
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Bottom Actions (Playlist, Search & Sync)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onAddPlaylistClick,
                    modifier = Modifier
                        .size(40.dp)
                        .dpadFocusable(onSelect = onAddPlaylistClick)
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = "Kaynak Ekle", tint = AuroraCyan)
                }
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .dpadFocusable(onSelect = onSearchClick)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Ara", tint = TextSecondary)
                }
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(40.dp)
                        .dpadFocusable(onSelect = onSyncClick)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yenile", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun TvHeroStage(
    channel: IPTVChannel?,
    onPlayClick: () -> Unit,
    onFavoriteToggle: (IPTVChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.75f)),
        border = BorderStroke(1.5.dp, AuroraCyan.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onPlayClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Channel Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A1229))
                    .border(1.dp, AuroraCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                ChannelLogoImage(
                    logoUrl = channel?.logoUrl ?: "",
                    category = channel?.category ?: "",
                    channelName = channel?.name ?: "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Channel Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(LiveRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CANLI YAYIN • 1080P FULL HD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = LiveRed
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        color = AuroraPurple.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = channel?.category ?: "Genel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AuroraCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = channel?.name ?: "PinpirikTV Canlı Yayın",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Kumanda [OK] tuşuna basarak tam ekranda kesintisiz izleyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Big Watch Button and Favorite Heart Toggle Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraCyan,
                        contentColor = Color(0xFF060E24)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "İzle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }

                channel?.let { chan ->
                    val isFav = chan.isFavorite
                    IconButton(
                        onClick = { onFavoriteToggle(chan) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isFav) LiveRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                            .border(1.dp, if (isFav) LiveRed else Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFav) LiveRed else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvShelvesView(
    allChannels: List<IPTVChannel>,
    favoriteChannels: List<IPTVChannel>,
    onChannelFocused: (IPTVChannel) -> Unit,
    onChannelClick: (IPTVChannel) -> Unit,
    onChannelLongClick: (IPTVChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val national = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_NATIONAL, ignoreCase = true) } }
    val sports = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_SPORTS, ignoreCase = true) } }
    val news = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_NEWS, ignoreCase = true) } }
    val music = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_MUSIC, ignoreCase = true) } }
    val local = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_LOCAL, ignoreCase = true) } }
    val movies = remember(allChannels) { allChannels.filter { it.category.equals(CategoryHelper.CAT_MOVIES, ignoreCase = true) } }
    val kidsAndDoc = remember(allChannels) {
        allChannels.filter {
            it.category.equals(CategoryHelper.CAT_DOCUMENTARY, ignoreCase = true) ||
            it.category.equals(CategoryHelper.CAT_KIDS, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (favoriteChannels.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "★ Favorilerim",
                    channels = favoriteChannels,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (national.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "🇹🇷 Ulusal Kanallar",
                    channels = national,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (sports.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "⚽ Canlı Spor",
                    channels = sports,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (news.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "📰 Haber & Gündem",
                    channels = news,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (music.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "🎵 Müzik & Eğlence",
                    channels = music,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (local.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "📍 Yerel Kanallar",
                    channels = local,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (movies.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "🎬 Film & Sinema",
                    channels = movies,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }

        if (kidsAndDoc.isNotEmpty()) {
            item {
                TvShelfRow(
                    title = "🌿 Belgesel & Çocuk",
                    channels = kidsAndDoc,
                    onChannelFocused = onChannelFocused,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick
                )
            }
        }
    }
}

@Composable
fun TvShelfRow(
    title: String,
    channels: List<IPTVChannel>,
    onChannelFocused: (IPTVChannel) -> Unit,
    onChannelClick: (IPTVChannel) -> Unit,
    onChannelLongClick: (IPTVChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                TvShelfCard(
                    channel = channel,
                    onFocused = { onChannelFocused(channel) },
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onChannelLongClick(channel) }
                )
            }
        }
    }
}

@Composable
fun TvShelfCard(
    channel: IPTVChannel,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "tv_card_scale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) SurfaceBlue else Color(0xFF0C1636).copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            if (isFocused) 2.5.dp else 1.dp,
            if (isFocused) AuroraCyan else Color.White.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .width(180.dp)
            .height(115.dp)
            .scale(scaleAnim)
            .dpadFocusable(
                onSelect = onClick,
                onLongSelect = onLongClick,
                onFocusChanged = {
                    isFocused = it
                    if (it) onFocused()
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF07122C),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "1080p",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AuroraCyan,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                if (channel.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Centered Logo
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF080F24)),
                contentAlignment = Alignment.Center
            ) {
                ChannelLogoImage(
                    logoUrl = channel.logoUrl,
                    category = channel.category,
                    channelName = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }

            // Name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TvCategoryGrid(
    title: String,
    channels: List<IPTVChannel>,
    onChannelFocused: (IPTVChannel) -> Unit,
    onChannelClick: (IPTVChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels, key = { it.id }) { channel ->
                TvShelfCard(
                    channel = channel,
                    onFocused = { onChannelFocused(channel) },
                    onClick = { onChannelClick(channel) },
                    onLongClick = {}
                )
            }
        }
    }
}

/* ==========================================================================
   SETTINGS & AUXILIARY VIEWS
   ========================================================================== */

@Composable
fun MobileSettingsView(
    viewModel: MainViewModel,
    onNavigateToAddPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeConfig = LocalThemeConfig.current
    val currentPalette = themeConfig.palette
    val currentTexture = themeConfig.texture

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ayarlar & Görünüm Seçenekleri",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // 1. Color Palette Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = currentPalette.surface.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Arayüz Renk Paleti (3 Seçenek)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = currentPalette.secondary
                )
                Text(
                    text = "Uygulamanın genelinde kullanılacak fütüristik renk temasını seçin. Tercihiniz otomatik olarak kaydedilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppColorPalette.values().forEach { paletteOption ->
                        val isSelected = paletteOption == currentPalette
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) paletteOption.primary.copy(alpha = 0.3f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) paletteOption.secondary else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemePalette(paletteOption) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Primary & Secondary indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(paletteOption.secondary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = paletteOption.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = paletteOption.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seçili",
                                        tint = paletteOption.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Background Texture Selector
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = currentPalette.surface.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Arayüz Zemin Dokusu (3 Seçenek)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = currentPalette.secondary
                )
                Text(
                    text = "Zeminde uygulanacak canlı, akışkan veya siber doku deseni tasarımını seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppBackgroundTexture.values().forEach { textureOption ->
                        val isSelected = textureOption == currentTexture
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) currentPalette.primary.copy(alpha = 0.2f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) currentPalette.secondary else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setBackgroundTexture(textureOption) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (textureOption) {
                                            AppBackgroundTexture.COSMIC_GRADIENT -> "🌌"
                                            AppBackgroundTexture.DOT_MATRIX -> "⁛"
                                            AppBackgroundTexture.CYBER_GRID -> "◫"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = textureOption.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = textureOption.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seçili",
                                        tint = currentPalette.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Playback Engine Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = currentPalette.surface.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Oynatıcı ve Akış Motoru",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = currentPalette.secondary
                )
                Text(
                    text = "PinpirikTV, donma yapmayan ultra hızlı Media3 ExoPlayer motorunu kullanır. Bağlantılar otomatik olarak en hızlı Türk sunucularından beslenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Button(
                    onClick = onNavigateToAddPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = currentPalette.secondary, contentColor = currentPalette.background),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Özel M3U / Xtream Çalma Listesi Ekle", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.syncPresets() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentPalette.secondary),
                    border = BorderStroke(1.dp, currentPalette.secondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kanal Listesini ve Yayınları Güncelle")
                }
            }
        }
    }
}

@Composable
fun TvSettingsView(
    viewModel: MainViewModel,
    onNavigateToAddPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "⚙️ PinpirikTV Ayarlar ve Yayın Yönetimi",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.8f)),
            border = BorderStroke(1.5.dp, AuroraCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Yayın Kalitesi ve Performans",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AuroraCyan
                )

                Text(
                    text = "• Hızlı Başlatma: Akışlar 800ms'de başlar ve düşük gecikmeli canlı protokolle çalışır.\n" +
                           "• Otomatik Çözücü: Donanım hızlandırmalı H.264/H.265 4K UHD video desteği.\n" +
                           "• Yedek Sunucular: Yayın kesilirse otomatik olarak yedek akış devreye girer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 22.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.syncPresets() },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = Color(0xFF07122C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Yayınları Şimdi Güncelle", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToAddPlaylist,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Harici Liste / M3U Ekle")
                    }
                }
            }
        }
    }
}

/* ==========================================================================
   CHANNEL LOGO COMPOSABLE WITH HIGH CONTRAST FALLBACK
   ========================================================================== */

@Composable
fun ChannelLogoImage(
    logoUrl: String,
    category: String,
    channelName: String,
    modifier: Modifier = Modifier
) {
    if (logoUrl.isNotEmpty()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = channelName,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback initials or category icon
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (category) {
                CategoryHelper.CAT_SPORTS -> Icons.Default.SportsSoccer
                CategoryHelper.CAT_NEWS -> Icons.Default.Article
                CategoryHelper.CAT_MUSIC -> Icons.Default.MusicNote
                CategoryHelper.CAT_KIDS -> Icons.Default.ChildCare
                CategoryHelper.CAT_DOCUMENTARY -> Icons.Default.Nature
                CategoryHelper.CAT_MOVIES -> Icons.Default.Movie
                else -> Icons.Default.Tv
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* ==========================================================================
   EMPTY SOURCE STATES (OPEN SOURCE PRINCIPLE)
   ========================================================================== */

@Composable
fun TvEmptySourceState(
    onNavigateToAddPlaylist: () -> Unit,
    onQuickLoadPreset: (PresetSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.85f)),
        border = BorderStroke(1.5.dp, AuroraCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AuroraCyan.copy(alpha = 0.15f))
                    .border(2.dp, AuroraCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PinpirikTV Açık Kaynak TV Oynatıcısı",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Açık kaynak ilkeleri gereğince uygulama içinde hazır kanal veya yayın linki barındırılmaz.\nKendi M3U listenizi ekleyebilir, yerel dosya seçebilir veya ücretsiz kamuya açık referans listelerini yükleyebilirsiniz.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToAddPlaylist,
                    modifier = Modifier
                        .height(48.dp)
                        .dpadFocusable(onSelect = onNavigateToAddPlaylist),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraCyan,
                        contentColor = Color(0xFF061026)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kaynak Ekle (M3U / Dosya / Xtream)", fontWeight = FontWeight.Bold)
                }

                val quickPreset = OPEN_SOURCE_PRESETS.firstOrNull()
                if (quickPreset != null) {
                    OutlinedButton(
                        onClick = { onQuickLoadPreset(quickPreset) },
                        modifier = Modifier
                            .height(48.dp)
                            .dpadFocusable(onSelect = { onQuickLoadPreset(quickPreset) }),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = AuroraCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🇹🇷 iptv-org Türkiye Yükle", fontWeight = FontWeight.SemiBold)
                    }
                }

                val sportsPreset = OPEN_SOURCE_PRESETS.getOrNull(1)
                if (sportsPreset != null) {
                    OutlinedButton(
                        onClick = { onQuickLoadPreset(sportsPreset) },
                        modifier = Modifier
                            .height(48.dp)
                            .dpadFocusable(onSelect = { onQuickLoadPreset(sportsPreset) }),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SportsSoccer, contentDescription = null, tint = AuroraCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚽ iptv-org Spor Yükle", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun MobileEmptySourceState(
    onNavigateToAddPlaylist: () -> Unit,
    onQuickLoadPreset: (PresetSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlue.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, AuroraCyan.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AuroraCyan.copy(alpha = 0.15f))
                    .border(2.dp, AuroraCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Henüz Oynatma Listesi Yok",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PinpirikTV açık kaynak prensiplerine uygundur ve telifli hazır yayın içermez. Kendi listenizi ekleyebilir veya ücretsiz kamuya açık yayınları yükleyebilirsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onNavigateToAddPlaylist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraCyan,
                    contentColor = Color(0xFF061026)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kendi Listeni Ekle (M3U / Dosya)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val quickPreset = OPEN_SOURCE_PRESETS.firstOrNull()
            if (quickPreset != null) {
                OutlinedButton(
                    onClick = { onQuickLoadPreset(quickPreset) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🇹🇷 Ücretsiz iptv-org Türkiye Listesini Yükle", fontSize = 13.sp)
                }
            }
        }
    }
}

