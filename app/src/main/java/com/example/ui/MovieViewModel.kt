package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.MaccmsService
import com.example.data.local.ApiSource
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteVod
import com.example.data.model.CategoryItem
import com.example.data.model.MaccmsResponse
import com.example.data.model.VodItem
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MovieUiState {
    object Idle : MovieUiState
    object Loading : MovieUiState
    data class Success(val response: MaccmsResponse) : MovieUiState
    data class Error(val message: String) : MovieUiState
}

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovieRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MovieRepository(
            favoriteDao = database.favoriteDao(),
            apiSourceDao = database.apiSourceDao(),
            maccmsService = MaccmsService.create()
        )
        
        // Ensure default api source is available, then load data
        viewModelScope.launch {
            repository.ensureDefaultSource()
            observeActiveSourceAndLoad()
        }
    }

    // Dynamic Lists & Streams from Room
    val favorites: StateFlow<List<FavoriteVod>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<Int>> = repository.allFavoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiSources: StateFlow<List<ApiSource>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSource: StateFlow<ApiSource?> = repository.activeSourceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Configuration States
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val currentPage = MutableStateFlow(1)
    
    // UI Layout controllers
    val showFavoritesOnly = MutableStateFlow(false)
    val showCategoryDialog = MutableStateFlow(false)
    val showSourceDialog = MutableStateFlow(false)
    
    // Details & Playing state
    val selectedVod = MutableStateFlow<VodItem?>(null)
    val activePlayingUrl = MutableStateFlow<String?>(null)
    val activePlayingTitle = MutableStateFlow<String?>(null)

    // Api Categories compiled from remote response on load
    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    // Response State for Main View
    private val _uiState = MutableStateFlow<MovieUiState>(MovieUiState.Idle)
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private var activeLoadJob: kotlinx.coroutines.Job? = null

    private fun observeActiveSourceAndLoad() {
        viewModelScope.launch {
            activeSource.collect { source ->
                if (source != null) {
                    // Reset page and categories when API key/endpoint changes
                    currentPage.value = 1
                    selectedCategory.value = null
                    _categories.value = emptyList()
                    loadMovies()
                }
            }
        }
    }

    fun loadMovies() {
        activeLoadJob?.cancel()
        activeLoadJob = viewModelScope.launch {
            val source = repository.getActiveSource() ?: return@launch
            _uiState.value = MovieUiState.Loading
            try {
                val response = repository.fetchVodList(
                    baseUrl = source.url,
                    pg = currentPage.value,
                    categoryId = selectedCategory.value?.typeId,
                    keyword = searchQuery.value.ifBlank { null }
                )
                
                // Keep loaded categories in VM state so category picker has them
                response.classList?.let { list ->
                    if (list.isNotEmpty()) {
                        _categories.value = list
                    }
                }
                
                _uiState.value = MovieUiState.Success(response)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = MovieUiState.Error(e.localizedMessage ?: "数据加载失败，请检查网络或更换源")
            }
        }
    }

    // Interactions
    fun search(query: String) {
        searchQuery.value = query
        currentPage.value = 1
        loadMovies()
    }

    fun clearSearch() {
        searchQuery.value = ""
        currentPage.value = 1
        loadMovies()
    }

    fun selectCategory(category: CategoryItem?) {
        selectedCategory.value = category
        currentPage.value = 1
        showCategoryDialog.value = false
        loadMovies()
    }

    fun changePage(page: Int) {
        val maxPage = when (val state = _uiState.value) {
            is MovieUiState.Success -> {
                val pc = state.response.pagecount?.toString()?.toDoubleOrNull()?.toInt() ?: 1
                pc
            }
            else -> 1
        }
        val targetPage = page.coerceIn(1, maxPage)
        if (targetPage != currentPage.value) {
            currentPage.value = targetPage
            loadMovies()
        }
    }

    // Favorites
    fun toggleFavorite(vod: VodItem) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(vod.vodId)
            val activeSrc = repository.getActiveSource()
            if (isFav) {
                repository.removeFavorite(vod.vodId)
            } else {
                repository.addFavorite(vod, activeSrc?.url ?: "")
            }
        }
    }
    
    fun removeFavoriteById(vodId: Int) {
        viewModelScope.launch {
            repository.removeFavorite(vodId)
        }
    }

    // API Source Configurations
    fun selectApiSource(source: ApiSource) {
        viewModelScope.launch {
            repository.selectSource(source.url)
        }
    }

    fun addCustomSource(name: String, url: String) {
        viewModelScope.launch {
            repository.addCustomSource(name, url)
        }
    }

    fun deleteApiSource(url: String) {
        viewModelScope.launch {
            repository.deleteSource(url)
        }
    }
}
