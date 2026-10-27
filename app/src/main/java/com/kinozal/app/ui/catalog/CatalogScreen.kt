package com.kinozal.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinozal.app.data.TmdbImages
import com.kinozal.app.data.TmdbList
import com.kinozal.app.ui.common.LoadingBox
import com.kinozal.app.ui.common.MessageBox
import com.kinozal.app.ui.common.MovieCard
import com.kinozal.app.ui.common.SearchField
import com.kinozal.app.ui.common.gridPadding

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onMovieClick: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Кинозал",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        )
        if (!state.hasKey) {
            MessageBox(
                "Для каталога нужен бесплатный ключ TMDB. Его можно получить на themoviedb.org в настройках аккаунта (раздел API).",
                actionLabel = "Открыть настройки",
                onAction = onOpenSettings,
            )
            return@Column
        }
        SearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Поиск фильмов",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (state.query.isBlank()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TmdbList.entries) { list ->
                    FilterChip(
                        selected = state.list == list,
                        onClick = { viewModel.selectList(list) },
                        label = { Text(list.title) },
                    )
                }
            }
        }

        when {
            state.movies.isEmpty() && state.loading -> LoadingBox()
            state.movies.isEmpty() && state.error != null ->
                MessageBox(state.error!!, actionLabel = "Повторить", onAction = viewModel::retry)
            state.movies.isEmpty() && state.query.isNotBlank() -> MessageBox("Ничего не найдено")
            else -> MovieGrid(state, onMovieClick, viewModel::loadMore)
        }
    }
}

@Composable
private fun MovieGrid(state: CatalogState, onMovieClick: (Int) -> Unit, onLoadMore: () -> Unit) {
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.movies.size) { if (nearEnd) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        state = gridState,
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.movies, key = { it.id }) { movie ->
            MovieCard(
                title = movie.title,
                subtitle = movie.releaseDate?.take(4),
                imageUrl = TmdbImages.poster(movie.posterPath),
                rating = movie.voteAverage,
                onClick = { onMovieClick(movie.id) },
            )
        }
        if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
