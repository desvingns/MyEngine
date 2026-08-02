package dev.myengine.android

import android.content.res.AssetManager
import android.content.res.AssetFileDescriptor
import android.media.SoundPool
import dev.myengine.content.SoundRef
import dev.myengine.core.GameplayEvent
import dev.myengine.core.GameplayEventType
import dev.myengine.render.EngineSnapshot

/**
 * Android presentation adapter for transient gameplay sounds. Pack refs are relative to the
 * packaged asset root; no simulation or render code opens or owns audio resources.
 */
class SoundPoolPresentationConsumer(
    assets: AssetManager,
    soundRefs: Map<GameplayEventType, SoundRef>,
    private val assetRoot: String = "",
) {
    private val soundPool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).build()
    private val soundIds = mutableMapOf<GameplayEventType, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val openDescriptors = mutableMapOf<Int, AssetFileDescriptor>()
    private val pendingEvents = mutableMapOf<Int, MutableList<GameplayEvent>>()
    private var cursor = SoundEventCursor()
    private var released = false
    private var presentationState = SoundPresentationState()

    var volume: Float
        get() = presentationState.volume
        set(value) {
            presentationState = presentationState.withVolume(value)
        }

    var muted: Boolean
        get() = presentationState.muted
        set(value) {
            presentationState = presentationState.withMuted(value)
        }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released) return@setOnLoadCompleteListener
            openDescriptors.remove(sampleId)?.close()
            if (status != 0) {
                pendingEvents.remove(sampleId)
                return@setOnLoadCompleteListener
            }
            loadedSoundIds += sampleId
            pendingEvents.remove(sampleId).orEmpty().forEach(::playLoaded)
        }
        soundRefs.toSortedMap(compareBy { it.id }).forEach { (type, ref) ->
            val path = packagedPath(ref.path) ?: return@forEach
            val descriptor = runCatching { assets.openFd(path) }.getOrNull() ?: return@forEach
            val sampleId = runCatching { soundPool.load(descriptor, PRIORITY) }.getOrDefault(0)
            if (sampleId > 0) {
                soundIds[type] = sampleId
                openDescriptors[sampleId] = descriptor
            } else {
                descriptor.close()
            }
        }
    }

    /** Consumes each newly observed sound event at most once. */
    fun consume(snapshot: EngineSnapshot) {
        if (released) return
        val observation = cursor.observe(snapshot.combatEvents.gameplayEvents)
        cursor = observation.cursor
        observation.events.forEach { event ->
            val sampleId = soundIds[event.type] ?: return@forEach
            if (sampleId in loadedSoundIds) {
                playLoaded(event, sampleId)
            } else {
                pendingEvents.getOrPut(sampleId, ::mutableListOf) += event
            }
        }
    }

    /** Releases the platform audio resource; safe to call more than once from lifecycle code. */
    fun release() {
        if (released) return
        released = true
        pendingEvents.clear()
        loadedSoundIds.clear()
        soundIds.clear()
        openDescriptors.values.forEach(AssetFileDescriptor::close)
        openDescriptors.clear()
        soundPool.release()
    }

    private fun playLoaded(event: GameplayEvent) {
        val sampleId = soundIds[event.type] ?: return
        playLoaded(event, sampleId)
    }

    private fun playLoaded(@Suppress("UNUSED_PARAMETER") event: GameplayEvent, sampleId: Int) {
        if (!released && !muted && volume > 0f) {
            soundPool.play(sampleId, volume, volume, PRIORITY, NO_LOOP, PLAYBACK_RATE)
        }
    }

    private fun packagedPath(path: String): String? {
        val normalized = path.trim().trim('/')
        if (normalized.isBlank()) return null
        val segments = normalized.split('/')
        if (segments.any { it == ".." || it.isBlank() }) return null
        return listOf(assetRoot.trim('/'), normalized)
            .filter { it.isNotBlank() }
            .joinToString("/")
    }

    private companion object {
        private const val MAX_STREAMS = 8
        private const val PRIORITY = 1
        private const val NO_LOOP = 0
        private const val PLAYBACK_RATE = 1f
    }
}
