package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.local.ApiSource
import com.example.data.local.FavoriteVod
import com.example.data.model.CategoryItem
import com.example.data.model.VodItem
import com.example.ui.MovieUiState
import com.example.ui.MovieViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MovieMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MovieMainScreen() {
    val viewModel: MovieViewModel = viewModel()
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val apiSources by viewModel.apiSources.collectAsStateWithLifecycle()
    val activeSource by viewModel.activeSource.collectAsStateWithLifecycle()
    
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsStateWithLifecycle()
    val showSourceDialog by viewModel.showSourceDialog.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    val selectedVod by viewModel.selectedVod.collectAsStateWithLifecycle()
    val activePlayingUrl by viewModel.activePlayingUrl.collectAsStateWithLifecycle()
    val activePlayingTitle by viewModel.activePlayingTitle.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()

    val parallelSearchEnabled by viewModel.parallelSearchEnabled.collectAsStateWithLifecycle()

    // Back button handling
    BackHandler(enabled = activePlayingUrl != null || selectedVod != null) {
        if (activePlayingUrl != null) {
            viewModel.activePlayingUrl.value = null
        } else {
            viewModel.selectedVod.value = null
        }
    }

    val gradientBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFAF7F2), // Sophisticated oat/cream
            Color(0xFFF3EDE3), // Silky linen sand
            Color(0xFFECEBF5), // Delicate Lavender tint
            Color(0xFFE5F0EC)  // Gentle soft mint dew
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        val topPadding = if (innerPadding.calculateTopPadding() > 14.dp) innerPadding.calculateTopPadding() - 14.dp else 0.dp
        val bottomPadding = if (innerPadding.calculateBottomPadding() > 14.dp) innerPadding.calculateBottomPadding() - 14.dp else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(top = topPadding, bottom = bottomPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activePlayingUrl != null) {
                    EmbeddedVideoSection(
                        title = activePlayingTitle ?: "视频播放",
                        url = activePlayingUrl!!,
                        onClose = { viewModel.activePlayingUrl.value = null }
                    )
                }

                AnimatedContent(
                    targetState = selectedVod,
                    transitionSpec = {
                        if (targetState != null) {
                            ContentTransform(
                                targetContentEnter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(300, easing = LinearEasing)),
                                initialContentExit = fadeOut(animationSpec = tween(200, easing = LinearEasing))
                            )
                        } else {
                            ContentTransform(
                                targetContentEnter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(animationSpec = tween(300, easing = LinearEasing)),
                                initialContentExit = fadeOut(animationSpec = tween(200, easing = LinearEasing))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "DetailsTransition"
                ) { targetVod ->
                    if (targetVod != null) {
                        VodDetailsSection(
                            vod = targetVod,
                            isFav = favoriteIds.contains(targetVod.vodId),
                            onToggleFav = { viewModel.toggleFavorite(targetVod) },
                            onBack = { viewModel.selectedVod.value = null },
                            onPlayEpisode = { ep ->
                                viewModel.activePlayingUrl.value = ep.url
                                viewModel.activePlayingTitle.value = "${targetVod.vodName} - ${ep.name}"
                            }
                        )
                    } else {
                        HomeBrowsingDashboard(
                            activeSourceName = activeSource?.name ?: "1号 极速秒播专线",
                            showFavoritesOnly = showFavoritesOnly,
                            onToggleFavorites = { viewModel.showFavoritesOnly.value = !showFavoritesOnly },
                            searchQuery = searchQuery,
                            onSearch = { viewModel.search(it) },
                            onClearSearch = { viewModel.clearSearch() },
                            onTriggerCategory = { viewModel.showCategoryDialog.value = true },
                            onTriggerSource = { viewModel.showSourceDialog.value = true },
                            selectedCategoryName = selectedCategory?.typeName ?: "全部分类",
                            uiState = uiState,
                            favorites = favorites,
                            favoriteIds = favoriteIds,
                            onToggleFav = { viewModel.toggleFavorite(it) },
                            onSelectVod = { item ->
                                viewModel.selectAndFetchDetails(item.vodId, item.vodName, item.apiSourceUrl)
                            },
                            currentPage = currentPage,
                            onPageChange = { viewModel.changePage(it) },
                            onSelectFavoriteVod = { fav ->
                                viewModel.selectAndFetchDetails(fav.vodId, fav.vodName, fav.apiSourceUrl)
                            }
                        )
                    }
                }
            }

            if (showCategoryDialog && selectedVod == null) {
                CategorySelectionDialog(
                    currentSelected = selectedCategory,
                    categories = categories,
                    onSelect = { viewModel.selectCategory(it) },
                    onDismiss = { viewModel.showCategoryDialog.value = false }
                )
            }

            if (showSourceDialog && selectedVod == null) {
                SourceManagementDialog(
                    activeSource = activeSource,
                    sources = apiSources,
                    parallelSearchEnabled = parallelSearchEnabled,
                    onToggleParallelSearch = { viewModel.parallelSearchEnabled.value = it },
                    onSelectSource = { 
                        viewModel.selectApiSource(it)
                        viewModel.showSourceDialog.value = false
                    },
                    onAddSource = { name, url -> viewModel.addCustomSource(name, url) },
                    onDeleteSource = { viewModel.deleteApiSource(it) },
                    onDismiss = { viewModel.showSourceDialog.value = false }
                )
            }
        }
    }
}

@Composable
fun HomeBrowsingDashboard(
    activeSourceName: String,
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTriggerCategory: () -> Unit,
    onTriggerSource: () -> Unit,
    selectedCategoryName: String,
    uiState: MovieUiState,
    favorites: List<FavoriteVod>,
    favoriteIds: List<Int>,
    onToggleFav: (VodItem) -> Unit,
    onSelectVod: (VodItem) -> Unit,
    onSelectFavoriteVod: (FavoriteVod) -> Unit,
    currentPage: Int,
    onPageChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp)
    ) {
        TopFunctionsBar(
            searchQuery = searchQuery,
            onSearch = onSearch,
            onClear = onClearSearch,
            onTriggerCategory = onTriggerCategory,
            selectedCategoryName = selectedCategoryName,
            showFavoritesOnly = showFavoritesOnly,
            onToggleFavorites = onToggleFavorites,
            onTriggerSource = onTriggerSource
        )

        AnimatedVisibility(
            visible = searchQuery.isNotBlank(),
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp, bottom = 2.dp)
        ) {
            Surface(
                onClick = onClearSearch,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCCFFEBEE),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "重置检索",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "过滤内容中: \"$searchQuery\" [点击清空]",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            if (showFavoritesOnly) {
                if (favorites.isEmpty()) {
                    EmptyPlaceholder(icon = Icons.Default.FavoriteBorder, text = "收藏夹是空的\n去浏览页面点击海报下方的爱心收藏吧。")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favorites) { favorite ->
                            FavoriteVodGridCard(
                                fav = favorite,
                                onClick = { onSelectFavoriteVod(favorite) }
                            )
                        }
                    }
                }
            } else {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it / 6 } togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    },
                    label = "DashboardState"
                ) { state ->
                    when (state) {
                        is MovieUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF9800),
                                        strokeWidth = 4.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "正在穿越星轨，极速加载中...",
                                        color = Color(0xFFB5AA9A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        is MovieUiState.Error -> {
                            EmptyPlaceholder(
                                icon = Icons.Default.WifiOff,
                                text = "${state.message}\n如长时不加载，请点击“极速换源”按钮更换其他线路API。"
                            )
                        }
                        is MovieUiState.Success -> {
                            val list = state.response.list ?: emptyList()
                            if (list.isEmpty()) {
                                EmptyPlaceholder(icon = Icons.Default.SearchOff, text = "未检索到相关资源")
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. 今日首推精选 (spans both columns)
                                    if (list.isNotEmpty()) {
                                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                            val firstItem = list.first()
                                            HeadlineSpotlightCard(
                                                vod = firstItem,
                                                isFav = favoriteIds.contains(firstItem.vodId),
                                                onToggleFav = { onToggleFav(firstItem) },
                                                onClick = { onSelectVod(firstItem) }
                                            )
                                        }
                                    }
                                    
                                    // 2. 多样式网格列表
                                    if (list.size > 1) {
                                        val remainingList = list.drop(1)
                                        itemsIndexed(remainingList) { index, item ->
                                            VodGridCard(
                                                vod = item,
                                                isFav = favoriteIds.contains(item.vodId),
                                                onToggleFav = { onToggleFav(item) },
                                                onClick = { onSelectVod(item) },
                                                isAlteredStyle = (index % 3 == 0) // Alternating styles
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        MovieUiState.Idle -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("等待激活接口数据...", color = Color(0xFFB5AA9A))
                            }
                        }
                    }
                }
            }
        }

        if (!showFavoritesOnly && uiState is MovieUiState.Success) {
            val totalPages = uiState.response.pagecount?.toString()?.toDoubleOrNull()?.toInt() ?: 1
            PaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = onPageChange
            )
        }

        FooterSection()
    }
}

@Composable
fun TopFunctionsBar(
    searchQuery: String,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onTriggerCategory: () -> Unit,
    selectedCategoryName: String,
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    onTriggerSource: () -> Unit
) {
    var isSearchingOpen by remember { mutableStateOf(false) }
    var localSearchText by remember { mutableStateOf(searchQuery) }
    
    LaunchedEffect(searchQuery) {
        localSearchText = searchQuery
        if (searchQuery.isNotBlank()) {
            isSearchingOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-end minimalist human-centric custom control panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0x99FFFFFF), // Elegant satin white frosted glass
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Search Console
                TactileFuncButton(
                    icon = Icons.Default.Search,
                    label = if (searchQuery.isNotBlank()) "检索中" else "搜影片",
                    selected = isSearchingOpen || searchQuery.isNotBlank(),
                    onClick = { isSearchingOpen = !isSearchingOpen },
                    activeColor = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )

                // Button 2: Category Picker
                TactileFuncButton(
                    icon = Icons.Default.FilterList,
                    label = if (selectedCategoryName != "全部分类") selectedCategoryName else "选分类",
                    selected = selectedCategoryName != "全部分类",
                    onClick = onTriggerCategory,
                    activeColor = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )

                // Button 3: Favorites Toggle
                TactileFuncButton(
                    icon = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = "看收藏",
                    selected = showFavoritesOnly,
                    onClick = onToggleFavorites,
                    activeColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )

                // Button 4: Change Source Line
                TactileFuncButton(
                    icon = Icons.Default.Dns,
                    label = "换专线",
                    selected = false,
                    onClick = onTriggerSource,
                    activeColor = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Animated drop-down exquisite mini search field
        AnimatedVisibility(
            visible = isSearchingOpen,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(0.85f)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF7F7F7))
                    .border(0.5.dp, Color(0xFFE5E5E5), CircleShape)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = localSearchText,
                    onValueChange = { localSearchText = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF424242)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(localSearchText.trim()) }),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (localSearchText.isEmpty()) {
                                    Text("寻找精彩...", fontSize = 12.sp, color = Color(0xFFBDBDBD))
                                }
                                innerTextField()
                            }
                            if (localSearchText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0))
                                        .clickable { 
                                            localSearchText = ""
                                            onClear()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF757575), modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TactileFuncButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) activeColor.copy(alpha = 0.4f) else Color.Transparent
        ),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) activeColor else Color(0xFF4A3E31),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else Color(0xFF4A3E31),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassDockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit,
    colorTint: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "DockBtnScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (selected) colorTint.copy(alpha = 0.18f) else Color(0x0A000000)
                )
                .border(
                    width = if (selected) 1.5.dp else 0.8.dp,
                    brush = if (selected) {
                        Brush.horizontalGradient(listOf(colorTint, Color.White, colorTint))
                    } else {
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent))
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colorTint else Color(0xFF5C5246),
                modifier = Modifier.size(16.dp)
            )
        }
        
        if (badgeText != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                color = colorTint.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF24201A),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HeadlineSpotlightCard(
    vod: VodItem,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onClick: () -> Unit
) {
    var visibleState by remember(vod.vodId) { mutableStateOf(false) }
    LaunchedEffect(vod.vodId) {
        visibleState = true
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visibleState) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "HeadlineAlpha"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visibleState) 0f else 30f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "HeadlineOffsetY"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
            .clickable(onClick = onClick)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.8f), Color(0xFFFCA524).copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("headline_card_${vod.vodId}"),
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(vod.vodPic)
                    .crossfade(true)
                    .build(),
                contentDescription = vod.vodName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Luminous warm translucent ambient mask to replace cold black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x331F1B16),
                                Color(0xCC1F1B16) // Elegant warm chocolate ground shadow for rich legibility
                            )
                        )
                    )
            )

            // Dynamic quality status indicator
            if (!vod.vodRemarks.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = vod.vodRemarks,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Classification badge
            if (!vod.typeName.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = vod.typeName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Info and Spotlight labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE91E63), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "FEATURED", 
                                color = Color.White, 
                                fontSize = 8.sp, 
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "精选影片推荐",
                            color = Color(0xFFFCA524),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = vod.vodName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFav,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏本片",
                            tint = if (isFav) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .background(Color(0xFFFCA524), RoundedCornerShape(18.dp))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "立即点播",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VodGridCard(
    vod: VodItem,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onClick: () -> Unit,
    isAlteredStyle: Boolean = false
) {
    // Elegant entrance rise animation
    var visibleState by remember(vod.vodId) { mutableStateOf(false) }
    LaunchedEffect(vod.vodId) {
        visibleState = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visibleState) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "CardAlpha"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visibleState) 0f else 30f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "CardOffsetY"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isAlteredStyle) 235.dp else 220.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            }
            .clickable(onClick = onClick)
            .border(
                width = if (isAlteredStyle) 1.2.dp else 0.8.dp,
                brush = if (isAlteredStyle) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFCA524), Color(0xFF00E5FF))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.15f))
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("vod_card_${vod.vodId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(vod.vodPic)
                    .crossfade(true)
                    .build(),
                contentDescription = vod.vodName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dynamic quality status indicator with glass sticker style
            if (!vod.vodRemarks.isNullOrBlank()) {
                Surface(
                    color = Color(0xEBFFFAF0),
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFFCA524).copy(alpha = 0.4f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = vod.vodRemarks,
                        color = Color(0xFFE65100),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (!vod.typeName.isNullOrBlank()) {
                Surface(
                    color = Color(0xEBF0FAF2),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF4CAF50).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = vod.typeName,
                        color = Color(0xFF2E7D32),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Compact floating heart toggle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp, bottom = 26.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xD9FFFFFF))
                    .border(0.5.dp, Color(0xFFFFCDD2), CircleShape)
                    .clickable { onToggleFav() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏本剧",
                    tint = if (isFav) Color(0xFFFF5252) else Color(0xFF5C5246),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Glassmorphic floating caption capsule
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF5FAF7F2)) // Sophisticated micro glass content overlay
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.3f))),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = vod.vodName,
                    color = Color(0xFF24201A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!vod.vodActor.isNullOrBlank()) {
                    Text(
                        text = "主演: ${vod.vodActor}",
                        color = Color(0xFF6E6356),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteVodGridCard(
    fav: FavoriteVod,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick)
            .border(
                0.8.dp,
                Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.2f))),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(fav.vodPic)
                    .crossfade(true)
                    .build(),
                contentDescription = fav.vodName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (!fav.vodRemarks.isNullOrBlank()) {
                Surface(
                    color = Color(0xEBFFFAF0),
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFFCA524).copy(alpha = 0.4f)),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = fav.vodRemarks,
                        color = Color(0xFFE65100),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Glassmorphic floating caption capsule
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF5FAF7F2))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.3f))),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = fav.vodName,
                    color = Color(0xFF24201A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fav.typeName ?: "未知类型",
                    color = Color(0xFF4CAF50),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategorySelectionDialog(
    currentSelected: CategoryItem?,
    categories: List<CategoryItem>,
    onSelect: (CategoryItem?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .wrapContentHeight()
                .testTag("category_dialog_card"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF2FAF7F2)), // Sophisticated cream milk-glass
            border = BorderStroke(
                1.2.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.2f)))
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "系统分类筛选",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF24201A),
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    item {
                        CategoryGridButton(
                            name = "全部影视",
                            selected = currentSelected == null,
                            onClick = { onSelect(null) }
                        )
                    }

                    items(categories) { category ->
                        CategoryGridButton(
                            name = category.typeName,
                            selected = currentSelected?.typeId == category.typeId,
                            onClick = { onSelect(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("完成返回", color = Color(0xFFFCA524), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryGridButton(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFCA524).copy(alpha = 0.15f) else Color(0x0A000000),
        contentColor = if (selected) Color(0xFFFCA524) else Color(0xFF5C5246),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .border(
                1.5.dp,
                if (selected) {
                    Brush.horizontalGradient(listOf(Color(0xFFFCA524), Color(0xFFFF9800)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), Color.Transparent))
                },
                RoundedCornerShape(14.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SourceManagementDialog(
    activeSource: ApiSource?,
    sources: List<ApiSource>,
    parallelSearchEnabled: Boolean,
    onToggleParallelSearch: (Boolean) -> Unit,
    onSelectSource: (ApiSource) -> Unit,
    onAddSource: (String, String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var isAddingMode by remember { mutableStateOf(false) }

    // Breathing effect for the active indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "BreatheDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("source_dialog_card"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF2FAF7F2)), // Elegant light pearl frosted glass plate
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color(0xFFFCA524).copy(alpha = 0.3f)))
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "极速源专线舱",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF24201A)
                        )
                        Text(
                            text = "多线路智能解码，即刻热切换",
                            fontSize = 10.sp,
                            color = Color(0xFF6E6356)
                        )
                    }

                    IconButton(
                        onClick = { isAddingMode = !isAddingMode },
                        modifier = Modifier
                            .background(Color(0x0A000000), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isAddingMode) Icons.Default.Cancel else Icons.Default.AddCircle,
                            contentDescription = "添加",
                            tint = Color(0xFFFCA524),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0A000000))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("多源并行搜索", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF24201A))
                        Text("开启全站自动热搜跨域检测匹配引擎", fontSize = 10.sp, color = Color(0xFF6E6356))
                    }
                    Switch(
                        checked = parallelSearchEnabled,
                        onCheckedChange = { onToggleParallelSearch(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFCA524),
                            uncheckedThumbColor = Color(0xFFB5AA9A),
                            uncheckedTrackColor = Color(0xFFE2D6C5)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = isAddingMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0x0A000000),
                                RoundedCornerShape(18.dp)
                            )
                            .border(1.dp, Color(0xFFFCA524).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text("新增极速线路", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF24201A))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newSourceName,
                            onValueChange = { newSourceName = it },
                            placeholder = { Text("例如: 私家秒播4K极速源", fontSize = 11.sp, color = Color(0xFF8C7F6E)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF24201A)),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFCA524),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0x33FFFFFF),
                                unfocusedContainerColor = Color(0x33FFFFFF)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newSourceUrl,
                            onValueChange = { newSourceUrl = it },
                            placeholder = { Text("数据源API地址(json格式/ac格式)", fontSize = 11.sp, color = Color(0xFF8C7F6E)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color(0xFF24201A)),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFCA524),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0x33FFFFFF),
                                unfocusedContainerColor = Color(0x33FFFFFF)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newSourceName.isNotBlank() && newSourceUrl.isNotBlank()) {
                                    onAddSource(newSourceName.trim(), newSourceUrl.trim())
                                    newSourceName = ""
                                    newSourceUrl = ""
                                    isAddingMode = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCA524)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("保存并激活此解码源", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sources) { source ->
                        val isSelected = activeSource?.url == source.url
                        
                        // Glassy responsive borders
                        val itemBorderBrush = if (isSelected) {
                            Brush.horizontalGradient(listOf(Color(0xFFFCA524), Color(0xFFFF9800)))
                        } else {
                            Brush.linearGradient(listOf(Color.White, Color.Transparent))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFFFCA524).copy(alpha = 0.12f) else Color(0x0A000000)
                                )
                                .border(1.dp, itemBorderBrush, RoundedCornerShape(16.dp))
                                .clickable { onSelectSource(source) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active breathing dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .graphicsLayer {
                                        alpha = if (isSelected) dotAlpha else 0.4f
                                    }
                                    .background(
                                        if (isSelected) Color(0xFF4CAF50) else Color(0xFF8C7F6E),
                                        CircleShape
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = source.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) Color(0xFF24201A) else Color(0xFF5C5246)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Glass tags for lines
                                    Surface(
                                        color = if (source.isDefault) Color(0xFFE4703C).copy(alpha = 0.12f) else Color(0xFF00E5FF).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                    ) {
                                        Text(
                                            text = if (source.isDefault) "官方精品" else "专线接入",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (source.isDefault) Color(0xFFE4703C) else Color(0xFF00B8D4),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = source.url,
                                    fontSize = 9.sp,
                                    color = Color(0xFF8C7F6E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!source.isDefault) {
                                IconButton(
                                    onClick = { onDeleteSource(source.url) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除源",
                                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFFFCA524).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Text("保存并应用舱单", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VodDetailsSection(
    vod: VodItem,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onBack: () -> Unit,
    onPlayEpisode: (com.example.data.model.PlayEpisode) -> Unit
) {
    val context = LocalContext.current
    val episodes = remember(vod) { vod.getPlayEpisodes() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 动态模糊顶部背景海报层
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(vod.vodPic)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.25f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x33FAF7F2),
                                Color(0xCCFAF7F2)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "解除详情", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "影片详情",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onToggleFav) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏本剧",
                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(115.dp)
                        .height(165.dp)
                        .border(1.2.dp, Color(0xFFFCA524).copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(vod.vodPic)
                            .crossfade(true)
                            .build(),
                        contentDescription = vod.vodName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vod.vodName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (!vod.apiSourceName.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 2.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "源: ${vod.apiSourceName}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (!vod.vodRemarks.isNullOrBlank()) {
                    Text(
                        text = vod.vodRemarks,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "导演: ${vod.vodDirector ?: "未知"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "主演: ${vod.vodActor ?: "未载入"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "分类: ${vod.typeName ?: "不详"} • ${vod.vodArea ?: "其它"} • ${vod.vodYear ?: "未知"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!vod.vodContent.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("剧情提要:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                val plainDesc = remember(vod.vodContent) {
                    vod.vodContent.replace(Regex("<.*?>"), "").trim()
                }
                Text(
                    text = plainDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("分集播放链接 (内置/外链):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (episodes.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "该影片在当前源未抓取到直接播放线路，系统已经为您自动在主页触发了全局检索，请返回主页选择对应实时播放源。",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val grouped = remember(episodes) { episodes.groupBy { it.sourceName } }
                
                grouped.forEach { (sourceName, list) ->
                    Text(
                        text = "播放源路线: $sourceName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Displaying list as standard flexible grid chunks for optimal scrolling compatibility
                        val chunked = list.chunked(3)
                        chunked.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { ep ->
                                    Surface(
                                        onClick = { onPlayEpisode(ep) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("episode_card_${ep.name}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        border = BorderStroke(1.dp, Color(0xFFFCA524).copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = ep.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Launch,
                                                contentDescription = "跳转外部打开",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clickable {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ep.url))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "跳转系统失败", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                                // Placeholders to keep columns neat on final lines
                                repeat(3 - chunk.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun EmbeddedVideoSection(
    title: String,
    url: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    val exoPlayer = remember {
        // Bypass SSL certificate errors globally for DefaultHttpDataSource (often needed for custom APIs)
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            })
            val sc = javax.net.ssl.SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("ExoPlayer", "Playback error: ${error.message}", error)
                        android.widget.Toast.makeText(context, "播放失败: 无法解析播放地址或网络异常", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
            }
    }

    val activity = context as? android.app.Activity
    DisposableEffect(isFullScreen) {
        if (isFullScreen) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            if (isFullScreen) {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    DisposableEffect(url) {
        val uri = Uri.parse(url)
        val mediaItemBuilder = androidx.media3.common.MediaItem.Builder().setUri(uri)
        if (url.contains(".m3u8") || url.contains("m3u8")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        } else if (url.contains(".mp4")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MP4)
        }
        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val content = @Composable { modifier: Modifier ->
        Column(modifier = modifier.background(Color.Black)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Playing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed Control
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable {
                            playbackSpeed = when (playbackSpeed) {
                                1f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2f
                                2f -> 0.5f
                                0.5f -> 1f
                                else -> 1f
                            }
                            exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed)
                        }
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = Color(0xFF4DB6AC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { isFullScreen = !isFullScreen },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setShowFastForwardButton(true)
                            setShowRewindButton(true)
                            controllerShowTimeoutMs = 3000
                            setKeepScreenOn(true)
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    }
                )
            }
        }
    }

    if (isFullScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                content(Modifier.fillMaxSize())
            }
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(260.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}

fun getHlsHtmlTemplate(m3u8Url: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body, html { margin:0; padding:0; width:100%; height:100%; background-color:#000; overflow:hidden; display:flex; justify-content:center; align-items:center; }
                video { width:100%; height:100%; object-fit:contain; outline:none; cursor: pointer; }
            </style>
            <!-- High-speed Multi-CDN failover loading chain -->
            <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.12/dist/hls.min.js"></script>
            <script>
                if (typeof Hls === 'undefined') {
                    document.write('<script src="https://unpkg.com/hls.js@1.4.12/dist/hls.min.js"><\/script>');
                }
            </script>
            <script>
                if (typeof Hls === 'undefined') {
                    document.write('<script src="https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.4.12/hls.min.js"><\/script>');
                }
            </script>
        </head>
        <body>
            <video id="video" window-playsinline playsinline autoplay controls style="width:100%; height:100%;"></video>
            <script>
                var video = document.getElementById('video');
                var videoSrc = '$m3u8Url';
                
                function playOrFallback() {
                    video.src = videoSrc;
                    video.play().catch(function(e) {
                        console.log("Autoplay bound, waiting user trigger: ", e);
                    });
                }
                
                // Allow touch-interaction play-start if autoplay fails
                window.addEventListener('click', function() {
                    if (video.paused) {
                        video.play().catch(function(err){ console.log(err); });
                    }
                }, { once: true });

                function initPlayer() {
                    if (typeof Hls !== 'undefined' && Hls.isSupported()) {
                        var hls = new Hls({
                            enableWorker: true,
                            lowLatencyMode: true,
                            maxMaxBufferLength: 30,
                            backBufferLength: 10
                        });
                        
                        hls.loadSource(videoSrc);
                        hls.attachMedia(video);
                        
                        hls.on(Hls.Events.MANIFEST_PARSED, function() {
                            video.play().catch(function(err) {
                                console.log("Autoplay blocked, waiting interaction: ", err);
                            });
                        });
                        
                        hls.on(Hls.Events.ERROR, function(event, data) {
                            if (data.fatal) {
                                switch (data.type) {
                                    case Hls.ErrorTypes.NETWORK_ERROR:
                                        console.log("Network error fatal, retrying...");
                                        hls.startLoad();
                                        break;
                                    case Hls.ErrorTypes.MEDIA_ERROR:
                                        console.log("Media error fatal, recovering...");
                                        hls.recoverMediaError();
                                        break;
                                    default:
                                        console.log("Fatal crash, switching engine play");
                                        playOrFallback();
                                        break;
                                }
                            }
                        });
                    } else {
                        playOrFallback();
                    }
                }

                // Small delay to ensure any doc.write async loads execute
                setTimeout(initPlayer, 100);
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    if (totalPages <= 1) return

    var jumpPageInput by remember { mutableStateOf("") }
    var isInputExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage) {
        jumpPageInput = ""
        isInputExpanded = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic glassmorphic micro-floating capsule
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xE6FAF7F2)) // Silky high-opacity frosted white warm glass
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.9f), Color(0xFFFCA524).copy(alpha = 0.2f))
                    ),
                    shape = CircleShape
                )
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Previous button, circular & subtle
            IconButton(
                onClick = { onPageChange(currentPage - 1) },
                enabled = currentPage > 1,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (currentPage > 1) Color(0x0F000000) else Color.Transparent)
                    .testTag("prev_page_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "上一页",
                    tint = if (currentPage > 1) Color(0xFF24201A) else Color(0x3324201A),
                    modifier = Modifier.size(12.dp)
                )
            }

            // Compact Elegant indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { isInputExpanded = !isInputExpanded }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$currentPage",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFCA524)
                )
                Text(
                    text = " / $totalPages",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6E6356)
                )
            }

            // Quick tiny slide input for high-end feel
            AnimatedVisibility(
                visible = isInputExpanded,
                enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = jumpPageInput,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                jumpPageInput = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val parse = jumpPageInput.toIntOrNull()
                                if (parse != null && parse in 1..totalPages) {
                                    onPageChange(parse)
                                    isInputExpanded = false
                                }
                            }
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF24201A),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .width(32.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x0F000000))
                            .border(0.5.dp, Color(0xFFFCA524).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(top = 1.dp)
                            .testTag("jump_page_input")
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCA524))
                            .clickable {
                                val parse = jumpPageInput.toIntOrNull()
                                if (parse != null && parse in 1..totalPages) {
                                    onPageChange(parse)
                                    isInputExpanded = false
                                }
                            }
                            .testTag("jump_go_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "验证",
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            // Next button, circular & subtle
            IconButton(
                onClick = { onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (currentPage < totalPages) Color(0x0F000000) else Color.Transparent)
                    .testTag("next_page_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "下一页",
                    tint = if (currentPage < totalPages) Color(0xFF24201A) else Color(0x3324201A),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "状态占位",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun FooterSection() {
    val context = LocalContext.current
    var showDisclaimer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x0A000000))
                .clickable { showDisclaimer = true }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "查看免责及源码地址",
                tint = Color(0x6624201A),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "vx: gjxly0304",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0x6624201A)
            )
        }
    }

    if (showDisclaimer) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDisclaimer = false }
        ) {
            var isDialogVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                isDialogVisible = true
            }

            AnimatedVisibility(
                visible = isDialogVisible,
                enter = fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(390.dp)
                        .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF0FAF7F2),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE4703C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "关于本项目及源码",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF24201A)
                                )
                            }

                            IconButton(
                                onClick = { showDisclaimer = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = Color(0x9924201A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scrollable content pane for the repo address & disclaimers
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .border(0.5.dp, Color(0xFFE2D6C5).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "开源仓库地址 (点击自动复制)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD84315)
                                )
                                
                                Surface(
                                    color = Color(0x0D000000),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("repo_url", "https://github.com/gjxly0304/VodPlayer")
                                                clipboard.setPrimaryClip(clip)
                                                android.widget.Toast.makeText(context, "仓库地址已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                // Clipboard fallback
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "https://github.com/gjxly0304/VodPlayer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }

                                Text(
                                    text = "免责声明及项目说明",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF24201A)
                                )

                                Text(
                                    text = "1. 本项目属于完全开源的 Android 高性能影视检索与播放实验工具，初衷是进行 Jetpack Compose 技术路线深海化测试和 HLS 离线码流探查。\n\n" +
                                           "2. 本项目不提供、不存储任何音视频影视载体，亦无任何人工录制或服务内容转传中介行为。\n\n" +
                                           "3. 所有呈现在界面上的封面、详情及在线流。均来自公开的第三方 MacCMS 平台开放源数据接口准实时读取及网页注入，属于常规的工具形态多功能浏览器导航。\n\n" +
                                           "4. 用户需遵守所在地的版权与法规规范合理行使检索。如有任何侵权或涉嫌违法信息，请径直联系原视频存储分发云商，本软件仅配合主流规范阻隔底层解析接口。\n\n" +
                                           "5. 本系统不收取任何注册费用或包含隐性收费，任何因为二次销售或违规运营造成的纠纷，本项目均不承担任何连带担保或法律责任。",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4A3E31),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer Action button
                        Button(
                            onClick = { showDisclaimer = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4703C)
                            )
                        ) {
                            Text("已知悉并同意", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
