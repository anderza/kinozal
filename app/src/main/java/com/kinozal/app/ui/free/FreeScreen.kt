package com.kinozal.app.ui.free

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.kinozal.app.data.FreeCollection
import com.kinozal.app.ui.common.LoadingBox
import com.kinozal.app.ui.common.MessageBox
import com.kinozal.app.ui.common.MovieCard
import com.kinozal.app.ui.common.SearchField
import com.kinozal.app.ui.common.gridPadding

@Composable
fun FreeScreen(viewModel: FreeViewModel, onMovieClick: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            "Смотреть бесплатно",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        Text(
            "Фильмы в общественном достоянии из Internet Archive",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
        )
        SearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Название (лучше на английском)",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (state.query.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(FreeCollection.entries) { collection ->
                    FilterChip(
                        selected = state.collection == collection,
                        onClick = { viewModel.selectCollection(collection) },
                        label = { Text(collection.title) },
                    )
                }
            }
        }

        when {
            state.movies.isEmpty() && state.loading -> LoadingBox()
            state.movies.isEmpty() && state.error != null ->
                MessageBox(state.error!!, actionLabel = "Повторить", onAction = viewModel::retry)
            state.movies.isEmpty() -> MessageBox(
                if (state.query.isBlank()) "Пусто" else "В открытом доступе такого фильма нет. Обычно там старые фильмы, примерно до 1970-х.",
            )
            else -> FreeGrid(state, onMovieClick, viewModel::loadMore)
        }
    }
}

@Composable
private fun FreeGrid(state: FreeState, onMovieClick: (String) -> Unit, onLoadMore: () -> Unit) {
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.movies.size) { if (nearEnd) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = gridState,
        contentPadding = gridPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.movies, key = { it.id }) { movie ->
            MovieCard(
                title = movie.title,
                subtitle = movie.year,
                imageUrl = movie.thumbnail,
                rating = null,
                onClick = { onMovieClick(movie.id) },
                imageAspect = 4f / 3f,
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
