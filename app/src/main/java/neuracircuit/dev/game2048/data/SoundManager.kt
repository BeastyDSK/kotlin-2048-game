package neuracircuit.dev.game2048.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import neuracircuit.dev.game2048.R

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val moveSoundId: Int
    private val mergeSoundId: Int
    private var isLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Allow multiple sounds (e.g., rapid moves)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds from res/raw
        moveSoundId = soundPool.load(context, R.raw.sound_default_move, 1)
        mergeSoundId = soundPool.load(context, R.raw.sound_default_merge, 1)

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isLoaded = true
        }
    }

    fun playMove(volume: Float) {
        if (isLoaded && volume > 0) {
            soundPool.play(moveSoundId, volume, volume, 0, 0, 1f)
        }
    }

    fun playMerge(volume: Float) {
        if (isLoaded && volume > 0) {
            soundPool.play(mergeSoundId, volume, volume, 1, 0, 1f)
        }
    }
    
    // Optional: A distinct sound for testing settings (reusing move for now)
    fun playTest(volume: Float) {
        if (isLoaded && volume > 0) {
            soundPool.play(moveSoundId, volume, volume, 0, 0, 1.5f) // Higher pitch for test
        }
    }

    fun release() {
        soundPool.release()
    }
}
