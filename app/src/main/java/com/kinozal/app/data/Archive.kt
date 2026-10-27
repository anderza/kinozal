package com.kinozal.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internet Archive: фильмы в общественном достоянии и под свободными лицензиями.
 * Поиск — advancedsearch.php, файлы фильма — /metadata/{id}.
 */
interface ArchiveApi {
    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "year", "description", "downloads"),
        @Query("sort[]") sort: String = "downloads desc",
        @Query("rows") rows: Int = 40,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json",
    ): ArchiveSearchResponse

    @GET("metadata/{id}")
    suspend fun metadata(@Path("id") id: String): ArchiveMetadata
}

@Serializable
data class ArchiveSearchResponse(val response: ArchiveSearchBody = ArchiveSearchBody())

@Serializable
data class ArchiveSearchBody(val numFound: Int = 0, val docs: List<ArchiveDoc> = emptyList())

// Поля в ответах архива бывают то строкой, то массивом, поэтому храним их как JsonElement.
@Serializable
data class ArchiveDoc(
    val identifier: String,
    val title: JsonElement? = null,
    val year: JsonElement? = null,
    val description: JsonElement? = null,
)

@Serializable
data class ArchiveMetadata(
    val metadata: Map<String, JsonElement> = emptyMap(),
    val files: List<ArchiveFile> = emptyList(),
)

@Serializable
data class ArchiveFile(
    val name: String,
    val format: String? = null,
    val size: String? = null,
    val length: String? = null,
)

fun JsonElement?.text(): String? = when (this) {
    null -> null
    is JsonPrimitive -> contentOrNull
    is JsonArray -> mapNotNull { it.text() }.joinToString("\n").ifBlank { null }
    else -> null
}

/** Описания в архиве часто в HTML — убираем теги и сущности. */
fun String.stripHtml(): String = this
    .replace(Regex("(?i)<br\\s*/?>|</p>"), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

data class FreeMovie(
    val id: String,
    val title: String,
    val year: String?,
    val description: String?,
) {
    val thumbnail get() = ArchiveRepository.thumbnail(id)
}

data class FreeMovieDetails(
    val movie: FreeMovie,
    val runtime: String?,
    val videos: List<FreeVideoFile>,
)

data class FreeVideoFile(val name: String, val url: String, val label: String, val sizeBytes: Long?)

enum class FreeCollection(val id: String, val title: String) {
    Feature("feature_films", "Художественные"),
    Noir("Film_Noir", "Нуар"),
    SciFiHorror("SciFi_Horror", "Фантастика и ужасы"),
    Silent("silent_films", "Немое кино"),
    Animation("animationandcartoons", "Мультфильмы"),
}

class ArchiveRepository(private val api: ArchiveApi) {

    suspend fun browse(collection: FreeCollection, page: Int = 1): List<FreeMovie> =
        api.search("collection:(${collection.id}) AND mediatype:(movies)", page = page).toMovies()

    suspend fun search(query: String, page: Int = 1): List<FreeMovie> {
        val cleaned = query.replace(Regex("[\\\\\"():\\[\\]{}^~*?!+-]"), " ").trim()
        if (cleaned.isEmpty()) return emptyList()
        val collections = FreeCollection.entries.joinToString(" OR ") { it.id }
        val q = "title:($cleaned) AND mediatype:(movies) AND collection:($collections)"
        return api.search(q, page = page).toMovies()
    }

    suspend fun details(id: String): FreeMovieDetails {
        val meta = api.metadata(id)
        val movie = FreeMovie(
            id = id,
            title = meta.metadata["title"].text() ?: id,
            year = (meta.metadata["year"] ?: meta.metadata["date"]).text()?.take(4),
            description = meta.metadata["description"].text()?.stripHtml(),
        )
        val videos = meta.files
            .filter { file -> VIDEO_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) } }
            .map { file ->
                FreeVideoFile(
                    name = file.name,
                    url = downloadUrl(id, file.name),
                    label = buildString {
                        append(file.format ?: file.name.substringAfterLast('.').uppercase())
                        file.size?.toLongOrNull()?.let { append(" · ").append(formatSize(it)) }
                    },
                    sizeBytes = file.size?.toLongOrNull(),
                )
            }
            // Сначала оригинальное качество (h.264/MPEG4), «512Kb» — запасной вариант.
            .sortedWith(compareBy<FreeVideoFile> { it.label.contains("512Kb", true) }.thenByDescending { it.sizeBytes ?: 0 })
        val runtime = meta.files.firstNotNullOfOrNull { it.length }?.let(::formatLength)
        return FreeMovieDetails(movie, runtime, videos)
    }

    private fun ArchiveSearchResponse.toMovies() = response.docs.map {
        FreeMovie(
            id = it.identifier,
            title = it.title.text() ?: it.identifier,
            year = it.year.text()?.take(4),
            description = it.description.text()?.stripHtml(),
        )
    }

    companion object {
        private val VIDEO_EXTENSIONS = listOf(".mp4", ".m4v", ".mkv", ".webm")

        fun thumbnail(id: String) = "https://archive.org/services/img/$id"

        fun downloadUrl(id: String, file: String): String =
            "https://archive.org/download/$id/" + file.split('/').joinToString("/") { android.net.Uri.encode(it) }

        private fun formatSize(bytes: Long): String {
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024) "%.1f ГБ".format(mb / 1024) else "%.0f МБ".format(mb)
        }

        /** Длительность приходит как секунды ("5423.12") или как "01:30:23". */
        private fun formatLength(raw: String): String? {
            val seconds = raw.toDoubleOrNull()?.toInt()
                ?: raw.split(':').mapNotNull { it.toIntOrNull() }.takeIf { it.isNotEmpty() }
                    ?.fold(0) { acc, part -> acc * 60 + part }
                ?: return null
            if (seconds < 60) return null
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            return if (h > 0) "$h ч $m мин" else "$m мин"
        }
    }
}
