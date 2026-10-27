package com.kinozal.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kinozal.app.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settings: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedKey by settings.tmdbKeyFlow.collectAsState(initial = "")
    val language by settings.languageFlow.collectAsState(initial = SettingsRepository.DEFAULT_LANGUAGE)
    var key by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(savedKey) { key = savedKey }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        Text("Ключ TMDB", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Каталог, постеры и описания берутся из The Movie Database. Подойдёт ключ API (v3) или токен доступа для чтения (v4). Получить бесплатно: зарегистрируйтесь на themoviedb.org, затем Настройки → API.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Ключ или токен") },
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { settings.setTmdbKey(key); saved = true } }) {
                Text(if (saved) "Сохранено" else "Сохранить")
            }
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.themoviedb.org/settings/api")))
            }) { Text("Получить ключ") }
        }

        Spacer(Modifier.height(32.dp))
        Text("Язык описаний", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ru-RU" to "Русский", "uk-UA" to "Українська", "en-US" to "English").forEach { (code, name) ->
                FilterChip(
                    selected = language == code,
                    onClick = { scope.launch { settings.setLanguage(code) } },
                    label = { Text(name) },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("О приложении", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Раздел «Бесплатно» воспроизводит фильмы из Internet Archive, которые находятся в общественном достоянии или выложены под свободной лицензией. Каталог TMDB показывает информацию и трейлеры. Данные TMDB используются через их API, но приложение не одобрено и не сертифицировано TMDB.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
