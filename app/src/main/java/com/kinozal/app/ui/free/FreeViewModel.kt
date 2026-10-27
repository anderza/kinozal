package com.kinozal.app.ui.free

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinozal.app.data.ArchiveRepository
import com.kinozal.app.data.FreeCollection
import com.kinozal.app.data.FreeMovie
import com.kinozal.app.ui.common.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FreeState(
    val collection: FreeCollection = FreeCollection.Feature,
    val query: String = "",
    val movies: List<FreeMovie> = emptyList(),
    val page: Int = 0,
    val endReached: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore get() = !loading && error == null && !endReached
}

class FreeViewModel(private val repo: ArchiveRepository, initialQuery: String?) : ViewModel() {
    private val _state = MutableStateFlow(FreeState(query = initialQuery.orEmpty()))
    val state: StateFlow<FreeState> = _state
    private var job: Job? = null

    init { reload() }

    fun selectCollection(collection: FreeCollection) {
        _state.update { it.copy(collection = collection) }
        reload()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        reload(debounce = true)
    }

    fun retry() = reload()

    fun loadMore() {
        if (_state.value.canLoadMore && job?.isActive != true) load(_state.value.page + 1, false)
    }

    private fun reload(debounce: Boolean = false) {
        _state.update { it.copy(movies = emptyList(), page = 0, endReached = false, error = null) }
        load(1, debounce)
    }

    private fun load(page: Int, debounce: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(500)
            _state.update { it.copy(loading = true, error = null) }
            val s = _state.value
            try {
                val result = if (s.query.isBlank()) repo.browse(s.collection, page) else repo.search(s.query, page)
                _state.update {
                    it.copy(
                        movies = if (page == 1) result else (it.movies + result).distinctBy { m -> m.id },
                        page = page,
                        endReached = result.size < PAGE_SIZE,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
