package com.kinozal.app.ui.free

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinozal.app.data.ArchiveRepository
import com.kinozal.app.data.FreeMovieDetails
import com.kinozal.app.ui.common.UiState
import com.kinozal.app.ui.common.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FreeDetailsViewModel(private val repo: ArchiveRepository, private val id: String) : ViewModel() {
    private val _state = MutableStateFlow<UiState<FreeMovieDetails>>(UiState.Loading)
    val state: StateFlow<UiState<FreeMovieDetails>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repo.details(id) }
                .fold({ UiState.Ready(it) }, { UiState.Error(it.toUserMessage()) })
        }
    }
}
