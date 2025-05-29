package com.example.mob_dev_portfolio

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SoundManager(context: Context) {
    val muted = mutableStateOf(false)

    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
    private val moveSoundId = soundPool.load(context, R.raw.move_self, 1)
    private val checkWinSoundId = soundPool.load(context, R.raw.check_win, 1)

    fun playMoveSound() {
        if (!muted.value) {
            soundPool.play(moveSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playCheckWinSound() {
        if (!muted.value) {
            soundPool.play(checkWinSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun releaseAll() {
        soundPool.release()
    }

    fun toggleMute() {
        muted.value = !muted.value
    }

}