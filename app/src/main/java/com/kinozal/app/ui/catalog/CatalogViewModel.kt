package com.kinozal.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinozal.app.data.SettingsRepository
import com.kinozal.app.data.TmdbList
import com.kinozal.app.data.TmdbMovie
import com.kinozal.app.data.TmdbRepository
import com.kinozal.app.ui.common.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogState(
    val hasKey: Boolean = true,
    val list: TmdbList = TmdbList.Trending,
    val query: String = "",
    val movies: List<TmdbMovie> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore get() = !loading && error == null && page < totalPages
}

class CatalogViewModel(
    private val repo: TmdbRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state
    private var job: Job? = null

    init {
        // Перезагружаем каталог при смене ключа или языка в настройках.
        viewModelScope.launch {
            settings.tmdbKeyFlow.distinctUntilChanged().collect { key ->
                _state.update { it.copy(hasKey = key.isNotEmpty()) }
                if (key.isNotEmpty()) reload()
            }
        }
        viewModelScope.launch {
            var first = true
            settings.languageFlow.distinctUntilChanged().collect {
                if (!first && _state.value.hasKey) reload()
                first = false
            }
        }
    }

    fun selectList(list: TmdbList) {
        _state.update { it.copy(list = list) }
        reload()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        reload(debounce = true)
    }

    fun retry() = reload()

    fun loadMore() {
        if (_state.value.canLoadMore && job?.isActive != true) load(_state.value.page + 1, debounce = false)
    }

    private fun reload(debounce: Boolean = false) {
        _state.update { it.copy(movies = emptyList(), page = 0, totalPages = 1, error = null) }
        load(1, debounce)
    }

    private fun load(page: Int, debounce: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(400)
            _state.update { it.copy(loading = true, error = null) }
            val s = _state.value
            runCatching {
                if (s.query.isBlank()) repo.list(s.list, page) else repo.search(s.query.trim(), page)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        movies = if (page == 1) result.results else (it.movies + result.results).distinctBy { m -> m.id },
                        page = page,
                        totalPages = result.totalPages,
                        loading = false,
                    )
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }
}
