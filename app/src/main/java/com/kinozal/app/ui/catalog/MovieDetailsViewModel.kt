package com.kinozal.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinozal.app.data.TmdbMovieDetails
import com.kinozal.app.data.TmdbRepository
import com.kinozal.app.ui.common.UiState
import com.kinozal.app.ui.common.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MovieDetailsViewModel(private val repo: TmdbRepository, private val movieId: Int) : ViewModel() {
    private val _state = MutableStateFlow<UiState<TmdbMovieDetails>>(UiState.Loading)
    val state: StateFlow<UiState<TmdbMovieDetails>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repo.details(movieId) }
                .fold({ UiState.Ready(it) }, { UiState.Error(it.toUserMessage()) })
        }
    }
}
