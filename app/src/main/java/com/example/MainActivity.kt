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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0B09), // 深黑色顶
            Color(0xFF1E1815)  // 暖炭灰色底
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
                            val tempVod = VodItem(
                                vodId = fav.vodId,
                                vodName = fav.vodName,
                                vodPic = fav.vodPic,
                                vodRemarks = fav.vodRemarks,
                                typeName = fav.typeName,
                                vodPlayUrl = "", 
                                vodPlayFrom = "",
                                vodContent = "本地已收藏。集数将为您在当前激活的数据API源中自动在线拉取匹配。"
                            )
                            viewModel.selectedVod.value = tempVod
                            viewModel.searchQuery.value = fav.vodName
                            viewModel.loadMovies()
                            Toast.makeText(context, "已在当前API源加载 \"${fav.vodName}\" 实时集数", Toast.LENGTH_SHORT).show()
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
                    onSelectSource = { viewModel.selectApiSource(it) },
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF4CAF50), CircleShape) // 绿色状态点
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "星辰影视 PRO",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "当前专线: $activeSourceName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (searchQuery.isNotBlank()) {
                FilledTonalButton(
                    onClick = onClearSearch,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重置过滤", fontSize = 11.sp)
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
                when (uiState) {
                    is MovieUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is MovieUiState.Error -> {
                        EmptyPlaceholder(
                            icon = Icons.Default.WifiOff,
                            text = "${uiState.message}\n如长时不加载，请点击“极速换源”按钮更换其他线路API。"
                        )
                    }
                    is MovieUiState.Success -> {
                        val list = uiState.response.list ?: emptyList()
                        if (list.isEmpty()) {
                            EmptyPlaceholder(icon = Icons.Default.SearchOff, text = "未检索到相关资源")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(list) { item ->
                                    VodGridCard(
                                        vod = item,
                                        isFav = favoriteIds.contains(item.vodId),
                                        onToggleFav = { onToggleFav(item) },
                                        onClick = { onSelectVod(item) }
                                    )
                                }
                            }
                        }
                    }
                    MovieUiState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("等待激活接口数据...")
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
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterBadgeChip(
                selected = isSearchingOpen || searchQuery.isNotBlank(),
                icon = Icons.Default.Search,
                label = if (searchQuery.isNotBlank()) "检索: $searchQuery" else "关键词/主演搜索",
                onClick = { isSearchingOpen = !isSearchingOpen },
                modifier = Modifier.weight(1f)
            )

            FilterBadgeChip(
                selected = selectedCategoryName != "全部分类",
                icon = Icons.Default.Category,
                label = selectedCategoryName,
                onClick = onTriggerCategory,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterBadgeChip(
                selected = showFavoritesOnly,
                icon = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "本地收藏夹",
                onClick = onToggleFavorites,
                modifier = Modifier.weight(1f)
            )

            FilterBadgeChip(
                selected = false,
                icon = Icons.Default.SwapHoriz,
                label = "极速换源",
                onClick = onTriggerSource,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = isSearchingOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = localSearchText,
                    onValueChange = { localSearchText = it },
                    placeholder = { Text("搜索影片、导演、演员名称...", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (localSearchText.isNotBlank()) {
                                IconButton(onClick = {
                                    localSearchText = ""
                                    onClear()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                            IconButton(onClick = { onSearch(localSearchText) }) {
                                Icon(Icons.Default.Search, contentDescription = "点击搜索", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(localSearchText) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    }
}

@Composable
fun FilterBadgeChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VodGridCard(
    vod: VodItem,
    isFav: Boolean,
    onToggleFav: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                Color(0xFFFCA524).copy(alpha = 0.08f),
                RoundedCornerShape(18.dp)
            )
            .testTag("vod_card_${vod.vodId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            if (!vod.vodRemarks.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = vod.vodRemarks,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!vod.typeName.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = vod.typeName,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleFav,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏本剧",
                    tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.8f)
                    .padding(12.dp)
            ) {
                Text(
                    text = vod.vodName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!vod.vodActor.isNullOrBlank()) {
                    Text(
                        text = "主演: ${vod.vodActor}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
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
            .height(250.dp)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                Color(0xFFFCA524).copy(alpha = 0.08f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            if (!fav.vodRemarks.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = fav.vodRemarks,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = fav.vodName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fav.typeName ?: "未分类",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "系统分类筛选 (居中卡片)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text("取消", color = MaterialTheme.colorScheme.primary)
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
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("source_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "资源API换源系统",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { isAddingMode = !isAddingMode }) {
                        Icon(
                            imageVector = if (isAddingMode) Icons.Default.Cancel else Icons.Default.AddCircle,
                            contentDescription = "添加",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(visible = isAddingMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text("添加新数据源", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newSourceName,
                            onValueChange = { newSourceName = it },
                            placeholder = { Text("例: 亮子秒播源", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newSourceUrl,
                            onValueChange = { newSourceUrl = it },
                            placeholder = { Text("例: https://cj.lziapi.com/api.php/provide/vod/", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newSourceName.isNotBlank() && newSourceUrl.isNotBlank()) {
                                    onAddSource(newSourceName, newSourceUrl)
                                    newSourceName = ""
                                    newSourceUrl = ""
                                    isAddingMode = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("保存并注入此源", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(sources) { source ->
                        val isSelected = activeSource?.url == source.url
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onSelectSource(source)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSource(source) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = source.url,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!source.isDefault) {
                                IconButton(onClick = { onDeleteSource(source.url) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除源",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("完成", color = MaterialTheme.colorScheme.primary)
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
            .background(MaterialTheme.colorScheme.background)
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
                alpha = 0.2f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background
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
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                        }
                    },
                    update = { view ->
                        if (url.endsWith(".m3u8") || url.contains(".m3u8")) {
                            val template = getHlsHtmlTemplate(url)
                            view.loadDataWithBaseURL("https://cdn.jsdelivr.net", template, "text/html", "utf-8", null)
                        } else {
                            view.loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                video { width:100%; height:100%; object-fit:contain; outline:none; }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
        </head>
        <body>
            <video id="video" controls autoplay playsinline></video>
            <script>
                var video = document.getElementById('video');
                var videoSrc = '$m3u8Url';
                if (Hls.isSupported()) {
                    var hls = new Hls({
                        enableWorker: true,
                        lowLatencyMode: true
                    });
                    hls.loadSource(videoSrc);
                    hls.attachMedia(video);
                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                    video.src = videoSrc;
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
    var jumpPageInput by remember { mutableStateOf("") }
    
    LaunchedEffect(currentPage) {
        jumpPageInput = ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPageChange(currentPage - 1) },
                    enabled = currentPage > 1,
                    modifier = Modifier.testTag("prev_page_button")
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "上一页",
                        tint = if (currentPage > 1) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Text(
                    text = "第 $currentPage / $totalPages 页",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    modifier = Modifier.testTag("next_page_button")
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "下一页",
                        tint = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("快速跳转:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                
                OutlinedTextField(
                    value = jumpPageInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            jumpPageInput = it
                        }
                    },
                    modifier = Modifier
                        .width(65.dp)
                        .height(38.dp)
                        .testTag("jump_page_input"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val parse = jumpPageInput.toIntOrNull()
                            if (parse != null && parse in 1..totalPages) {
                                onPageChange(parse)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = {
                        val parse = jumpPageInput.toIntOrNull()
                        if (parse != null && parse in 1..totalPages) {
                            onPageChange(parse)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("jump_go_button")
                ) {
                    Text("前往", fontSize = 11.sp)
                }
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
        Text(
            text = "转述（vx号gjxly0304）",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5F),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
