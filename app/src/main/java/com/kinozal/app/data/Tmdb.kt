package com.kinozal.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun trending(@Query("language") language: String, @Query("page") page: Int = 1): TmdbPage

    @GET("movie/{list}")
    suspend fun list(
        @Path("list") list: String,
        @Query("language") language: String,
        @Query("page") page: Int = 1,
    ): TmdbPage

    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int = 1,
    ): TmdbPage

    @GET("movie/{id}")
    suspend fun details(
        @Path("id") id: Int,
        @Query("language") language: String,
        @Query("append_to_response") append: String = "videos,credits",
        @Query("include_video_language") videoLanguages: String,
    ): TmdbMovieDetails
}

@Serializable
data class TmdbPage(
    val page: Int = 1,
    val results: List<TmdbMovie> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val overview: String = "",
)

@Serializable
data class TmdbMovieDetails(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val tagline: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<TmdbGenre> = emptyList(),
    val videos: TmdbVideos? = null,
    val credits: TmdbCredits? = null,
)

@Serializable
data class TmdbGenre(val id: Int, val name: String)

@Serializable
data class TmdbVideos(val results: List<TmdbVideo> = emptyList())

@Serializable
data class TmdbVideo(
    val key: String,
    val site: String = "",
    val type: String = "",
    val name: String = "",
    val official: Boolean = false,
)

@Serializable
data class TmdbCredits(val cast: List<TmdbCast> = emptyList())

@Serializable
data class TmdbCast(
    val id: Int,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

object TmdbImages {
    fun poster(path: String?) = path?.let { "https://image.tmdb.org/t/p/w342$it" }
    fun backdrop(path: String?) = path?.let { "https://image.tmdb.org/t/p/w1280$it" }
    fun profile(path: String?) = path?.let { "https://image.tmdb.org/t/p/w185$it" }
}

enum class TmdbList(val path: String?, val title: String) {
    Trending(null, "В тренде"),
    Popular("popular", "Популярные"),
    NowPlaying("now_playing", "Сейчас в кино"),
    TopRated("top_rated", "Лучшие"),
    Upcoming("upcoming", "Скоро"),
}

class TmdbRepository(private val api: TmdbApi, private val settings: SettingsRepository) {
    suspend fun hasKey() = settings.currentTmdbKey().isNotEmpty()

    suspend fun list(list: TmdbList, page: Int = 1): TmdbPage {
        val lang = settings.currentLanguage()
        return if (list.path == null) api.trending(lang, page) else api.list(list.path, lang, page)
    }

    suspend fun search(query: String, page: Int = 1) = api.search(query, settings.currentLanguage(), page)

    suspend fun details(id: Int): TmdbMovieDetails {
        val lang = settings.currentLanguage()
        return api.details(id, lang, videoLanguages = "${lang.substringBefore('-')},en,null")
    }
}

/** Лучший YouTube-трейлер: официальный трейлер, затем любой трейлер, затем тизер. */
fun TmdbMovieDetails.bestTrailer(): TmdbVideo? {
    val yt = videos?.results.orEmpty().filter { it.site == "YouTube" }
    return yt.firstOrNull { it.type == "Trailer" && it.official }
        ?: yt.firstOrNull { it.type == "Trailer" }
        ?: yt.firstOrNull { it.type == "Teaser" }
}
