package com.kinozal.app.ui.catalog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinozal.app.data.TmdbImages
import com.kinozal.app.data.TmdbMovieDetails
import com.kinozal.app.data.bestTrailer
import com.kinozal.app.ui.common.LoadingBox
import com.kinozal.app.ui.common.MessageBox
import com.kinozal.app.ui.common.Poster
import com.kinozal.app.ui.common.RatingBadge
import com.kinozal.app.ui.common.UiState
import com.kinozal.app.ui.common.bottomScrim

@Composable
fun MovieDetailsScreen(
    viewModel: MovieDetailsViewModel,
    onBack: () -> Unit,
    onFindFree: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> MessageBox(s.message, actionLabel = "Повторить", onAction = viewModel::load)
            is UiState.Ready -> Details(s.data, onFindFree)
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
        }
    }
}

@Composable
private fun Details(movie: TmdbMovieDetails, onFindFree: (String) -> Unit) {
    val context = LocalContext.current
    val trailer = movie.bestTrailer()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            Poster(TmdbImages.backdrop(movie.backdropPath), Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Box(Modifier.matchParentSize().background(bottomScrim))
        }
        Row(
            Modifier.padding(horizontal = 16.dp).offset(y = (-48).dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Poster(
                TmdbImages.poster(movie.posterPath),
                Modifier.width(110.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(movie.title, style = MaterialTheme.typography.titleLarge)
                if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) {
                    Text(movie.originalTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                val meta = listOfNotNull(
                    movie.releaseDate?.take(4)?.takeIf { it.isNotBlank() },
                    movie.runtime?.takeIf { it > 0 }?.let { "${it / 60} ч ${it % 60} мин" },
                ).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (movie.voteAverage > 0) {
                    Spacer(Modifier.height(6.dp))
                    RatingBadge(movie.voteAverage)
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offset(y = (-32).dp)) {
            if (movie.genres.isNotEmpty()) {
                Text(
                    movie.genres.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (trailer != null) {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${trailer.key}"))
                        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { }
                    }) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Трейлер")
                    }
                }
                OutlinedButton(onClick = { onFindFree(movie.originalTitle.ifBlank { movie.title }) }) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("В открытом доступе")
                }
            }
            if (!movie.tagline.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("«${movie.tagline}»", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (movie.overview.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(movie.overview, style = MaterialTheme.typography.bodyMedium)
            }
        }

        val cast = movie.credits?.cast.orEmpty().take(20)
        if (cast.isNotEmpty()) {
            Text(
                "В ролях",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cast, key = { it.id }) { person ->
                    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Poster(TmdbImages.profile(person.profilePath), Modifier.size(72.dp).clip(CircleShape))
                        Spacer(Modifier.height(6.dp))
                        Text(person.name, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        person.character?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
