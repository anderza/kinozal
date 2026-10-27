package com.kinozal.app.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Полноэкранный плеер на Media3 ExoPlayer. */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private var position = 0L
    private var playWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        position = savedInstanceState?.getLong(KEY_POSITION) ?: 0L
        playWhenReady = savedInstanceState?.getBoolean(KEY_PLAYING) ?: true

        playerView = PlayerView(this).apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            setShowNextButton(false)
            setShowPreviousButton(false)
        }
        setContentView(playerView)
        hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player?.let {
            position = it.currentPosition
            playWhenReady = it.playWhenReady
        }
        outState.putLong(KEY_POSITION, position)
        outState.putBoolean(KEY_PLAYING, playWhenReady)
    }

    private fun initPlayer() {
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val exo = ExoPlayer.Builder(this).build().also { player = it }
        playerView.player = exo
        exo.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                .build(),
        )
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, "Не удалось воспроизвести: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
            }
        })
        exo.seekTo(position)
        exo.playWhenReady = playWhenReady
        exo.prepare()
    }

    private fun releasePlayer() {
        player?.let {
            position = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
        playerView.player = null
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val KEY_POSITION = "position"
        private const val KEY_PLAYING = "playing"

        fun intent(context: Context, url: String, title: String) =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }
}
