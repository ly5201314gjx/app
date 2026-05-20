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

@OptIn(ExperimentalMaterial3Api::class)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activePlayingUrl != null) {
                    EmbeddedVideoSection(
                        title = activePlayingTitle ?: "视频播放",
                        url = activePlayingUrl!!,
                        onClose = { viewModel.activePlayingUrl.value = null }
                    )
                }

                if (selectedVod != null) {
                    VodDetailsSection(
                        vod = selectedVod!!,
                        isFav = favoriteIds.contains(selectedVod!!.vodId),
                        onToggleFav = { viewModel.toggleFavorite(selectedVod!!) },
                        onBack = { viewModel.selectedVod.value = null },
                        onPlayEpisode = { ep ->
                            viewModel.activePlayingUrl.value = ep.url
                            viewModel.activePlayingTitle.value = "${selectedVod!!.vodName} - ${ep.name}"
                        }
                    )
                } else {
                    HomeBrowsingDashboard(
                        activeSourceName = activeSource?.name ?: "默认极速源",
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
                        onSelectVod = { viewModel.selectedVod.value = it },
                        currentPage = currentPage,
                        onPageChange = { viewModel.changePage(it) },
                        onSelectFavoriteVod = { fav ->
                            viewModel.selectAndFetchDetails(fav.vodId, fav.vodName)
                        }
                    )
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
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glass concentric glowing emblem
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color.White.copy(0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCA524).copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Core Logo",
                            tint = Color(0xFFFCA524),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Embedded breathing green live status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Surface(
                        color = Color(0x66FFFFFF),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFFE4703C),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeSourceName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF24201A)
                            )
                        }
                    }
                }
            }
            if (searchQuery.isNotBlank()) {
                Surface(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCCFFEBEE),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(11.dp), tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重置检索", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

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
        // High-end minimalist micro glass capsule dock, centered and compact
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9FAF7F2)) // Rich light-creamy frosted glass dock
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.2f))),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Search Glass Icon
                GlassDockIconButton(
                    icon = Icons.Default.Search,
                    selected = isSearchingOpen || searchQuery.isNotBlank(),
                    onClick = { isSearchingOpen = !isSearchingOpen },
                    colorTint = Color(0xFF00E5FF)
                )

                // Button 2: Category Widgets Custom Dial
                GlassDockIconButton(
                    icon = Icons.Default.Widgets,
                    selected = selectedCategoryName != "全部分类",
                    badgeText = if (selectedCategoryName != "全部分类") selectedCategoryName else null,
                    onClick = onTriggerCategory,
                    colorTint = Color(0xFFFCA524)
                )

                // Button 3: Heartbeat Favs Console
                GlassDockIconButton(
                    icon = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    selected = showFavoritesOnly,
                    onClick = onToggleFavorites,
                    colorTint = Color(0xFFFF5252)
                )

                // Button 4: Central Connection Router (Change Source)
                GlassDockIconButton(
                    icon = Icons.Default.Dns,
                    selected = false,
                    onClick = onTriggerSource,
                    colorTint = Color(0xFFE4703C)
                )
            }
        }

        // Animated drop-down glass-frosted input field
        AnimatedVisibility(
            visible = isSearchingOpen,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x99FFFFFF)) // Mist panel
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = localSearchText,
                    onValueChange = { localSearchText = it },
                    placeholder = { Text("搜索影片、导演、主演...", fontSize = 12.sp, color = Color(0xFF8C7F6E)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF24201A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                            if (localSearchText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        localSearchText = ""
                                        onClear()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除", tint = Color(0xFF8C7F6E), modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = { onSearch(localSearchText.trim()) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFCA524), CircleShape)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "搜索", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(localSearchText.trim()) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFCA524),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0x33FFFFFF),
                        unfocusedContainerColor = Color(0x33FFFFFF)
                    )
                )
            }
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
    var visibleState by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
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
    var visibleState by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
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

@Composable
fun EmbeddedVideoSection(
    title: String,
    url: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "内置中",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法唤醒外部播放器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "系统跳转打开",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f) 
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                        }
                    },
                    update = { view ->
                        if (url.endsWith(".m3u8") || url.contains(".m3u8")) {
                            val template = getHlsHtmlTemplate(url)
                            // Use stream base host domain instead of file .m3u8 URL to preserve relative segment resolutions and bypass CORS blockages
                            val hostBaseUrl = try {
                                val uri = Uri.parse(url)
                                val scheme = uri.scheme ?: "https"
                                val host = uri.host ?: ""
                                val port = if (uri.port != -1) ":${uri.port}" else ""
                                "$scheme://$host$port/"
                            } catch (e: Exception) {
                                "https://cdnjs.cloudflare.com"
                            }
                            view.loadDataWithBaseURL(hostBaseUrl, template, "text/html", "utf-8", null)
                        } else {
                            view.loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Quick helper buttons for copy direct link and external play
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(url), "video/*")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "未找到外部视频播放器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("调用第三方播放", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Direct Stream URL", url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "直链地址已复制剪切板 📌", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("直接复制直链", fontSize = 11.sp, color = Color(0xFF00E5FF))
                }
            }
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
            <script src="https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.4.12/hls.min.js"></script>
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
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
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
}
