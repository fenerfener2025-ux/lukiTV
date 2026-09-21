package com.example.ui.tv

import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import com.example.domain.model.IPTVChannel
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraPurple
import com.example.ui.theme.DeepSpaceBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.LiveRed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

/**
 * Custom D-Pad focus modifier that handles Android TV directional navigation,
 * center selection button events, and explicit visual highlight states.
 */
fun Modifier.dpadFocusable(
    onSelect: () -> Unit,
    onLongSelect: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    focusRequester: FocusRequester? = null
): Modifier = this
    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    .onFocusChanged { state ->
        onFocusChanged?.invoke(state.isFocused)
    }
    .focusable()
    .onKeyEvent { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyUp) {
            when (keyEvent.key) {
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                    onSelect()
                    true
                }
                Key.Menu, Key.Spacebar -> {
                    onLongSelect?.invoke()
                    onLongSelect != null
                }
                else -> false
            }
        } else false
    }
    .clickable(onClick = onSelect)

/**
 * TV D-Pad Focused Item Container with high-contrast glowing border,
 * scale bounce animation, and background highlight for Android TV channel lists.
 */
@Composable
fun TvDpadFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    focusRequester: FocusRequester? = null,
    onFocusedChange: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "tv_card_scale"
    )

    val borderStroke = if (isFocused) {
        BorderStroke(2.5.dp, Brush.horizontalGradient(listOf(AuroraCyan, AuroraPurple)))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    }

    val cardBg = if (isFocused) {
        Color(0xFF142048)
    } else {
        Color(0xFF0D1635).copy(alpha = 0.85f)
    }

    Card(
        modifier = modifier
            .scale(scale)
            .dpadFocusable(
                onSelect = onClick,
                onLongSelect = onLongClick,
                onFocusChanged = { focused ->
                    isFocused = focused
                    onFocusedChange?.invoke(focused)
                },
                focusRequester = focusRequester
            ),
        shape = shape,
        border = borderStroke,
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 1.dp,
                            color = AuroraCyan.copy(alpha = 0.4f),
                            shape = shape
                        )
                    } else Modifier
                )
        ) {
            content(isFocused)
        }
    }
}

/**
 * High-performance TvLazyRow using TvFoundation with PivotOffsets for smooth D-Pad navigation.
 */
@Composable
fun TvDpadChannelRow(
    channels: List<IPTVChannel>,
    onChannelClick: (IPTVChannel) -> Unit,
    onChannelLongClick: (IPTVChannel) -> Unit,
    onChannelDetailClick: (IPTVChannel) -> Unit,
    viewModel: MainViewModel? = null,
    modifier: Modifier = Modifier,
    itemContent: @Composable (IPTVChannel) -> Unit
) {
    val state = rememberTvLazyListState()

    TvLazyRow(
        state = state,
        pivotOffsets = PivotOffsets(parentFraction = 0.15f),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 64.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            itemContent(channel)
        }
    }
}

/**
 * TvLazyVerticalGrid using TvFoundation with PivotOffsets for multi-column channel grid navigation.
 */
@Composable
fun TvDpadChannelGrid(
    channels: List<IPTVChannel>,
    columnsCount: Int = 4,
    modifier: Modifier = Modifier,
    itemContent: @Composable (IPTVChannel) -> Unit
) {
    val state = rememberTvLazyGridState()

    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(columnsCount),
        state = state,
        pivotOffsets = PivotOffsets(parentFraction = 0.15f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(channels, key = { it.id }) { channel ->
            itemContent(channel)
        }
    }
}

