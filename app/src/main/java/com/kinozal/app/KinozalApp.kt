package com.kinozal.app

import android.app.Application
import com.kinozal.app.data.ArchiveRepository
import com.kinozal.app.data.Network
import com.kinozal.app.data.SettingsRepository
import com.kinozal.app.data.TmdbRepository

/** Простой контейнер зависимостей на всё приложение. */
class KinozalApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var tmdb: TmdbRepository
        private set
    lateinit var archive: ArchiveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        tmdb = TmdbRepository(Network.tmdbApi(settings), settings)
        archive = ArchiveRepository(Network.archiveApi())
    }
}
