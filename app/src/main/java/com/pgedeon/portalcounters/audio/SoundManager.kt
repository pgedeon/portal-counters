package com.pgedeon.portalcounters.audio

import android.content.Context
import android.media.SoundPool
import com.pgedeon.portalcounters.data.GameStorage

class SoundManager(context: Context) {

    private val storage = GameStorage(context)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .build()

    private val damageSounds: List<Int>
    private val healSounds: List<Int>

    var isMuted: Boolean
        get() = storage.soundMuted
        set(value) { storage.soundMuted = value }

    init {
        val res = context.resources
        fun load(name: String): Int {
            val id = res.getIdentifier(name, "raw", context.packageName)
            return if (id != 0) soundPool.load(context, id, 1) else -1
        }

        damageSounds = listOf(
            load("damage_hit"),
            load("damage_dark"),
            load("damage_sting"),
            load("damage_doom"),
            load("damage_crunch"),
        ).filter { it > 0 }

        healSounds = listOf(
            load("heal_sparkle"),
            load("heal_shimmer"),
            load("heal_chime"),
            load("heal_ascending"),
            load("heal_glow"),
        ).filter { it > 0 }
    }

    fun playDamage() {
        if (isMuted || damageSounds.isEmpty()) return
        val id = damageSounds.random()
        soundPool.play(id, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    fun playHeal() {
        if (isMuted || healSounds.isEmpty()) return
        val id = healSounds.random()
        soundPool.play(id, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
