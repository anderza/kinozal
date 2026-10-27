package com.kinozal.app.ui.free

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinozal.app.data.FreeMovieDetails
import com.kinozal.app.player.PlayerActivity
import com.kinozal.app.ui.common.LoadingBox
import com.kinozal.app.ui.common.MessageBox
import com.kinozal.app.ui.common.Poster
import com.kinozal.app.ui.common.UiState
import com.kinozal.app.ui.common.bottomScrim

@Composable
fun FreeDetailsScreen(viewModel: FreeDetailsViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> MessageBox(s.message, actionLabel = "Повторить", onAction = viewModel::load)
            is UiState.Ready -> Details(s.data)
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
private fun Details(details: FreeMovieDetails) {
    val context = LocalContext.current
    val movie = details.movie
    var expanded by remember { mutableStateOf(false) }
    val play = { index: Int ->
        val file = details.videos[index]
        context.startActivity(PlayerActivity.intent(context, file.url, movie.title))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Poster(movie.thumbnail, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(bottomScrim))
            if (details.videos.isNotEmpty()) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { play(0) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, "Смотреть", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text(movie.title, style = MaterialTheme.typography.headlineMedium)
            val meta = listOfNotNull(movie.year, details.runtime).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            if (details.videos.isEmpty()) {
                Text(
                    "У этой записи нет видеофайла, который можно воспроизвести.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(onClick = { play(0) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Смотреть")
                }
                if (details.videos.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    Text("Другие версии", style = MaterialTheme.typography.titleMedium)
                    details.videos.drop(1).take(6).forEachIndexed { i, file ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { play(i + 1) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(file.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (!movie.description.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("Описание", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    movie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = !expanded },
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Источник: archive.org/details/${movie.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
