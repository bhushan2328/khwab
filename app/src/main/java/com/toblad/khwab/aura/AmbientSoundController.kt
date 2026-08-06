package com.toblad.khwab.aura

import android.content.Context
import android.media.MediaPlayer
import com.toblad.khwab.aura.model.TimePhase
import com.toblad.khwab.aura.model.WeatherState
import com.toblad.khwab.aura.ui.LightningBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays looping ambient audio that follows Aura's real
 * current weather and time of day, plus thunder hits synced
 * to the exact same LightningBus events that drive the
 * visual lightning flash in WeatherLayer.
 *
 * Audio files are loaded dynamically by name from res/raw/
 * so missing files are skipped silently — no compile errors.
 *
 * Expected files in app/src/main/res/raw/ (add when ready):
 * rain_loop, storm_loop, thunder_hit, wind_loop,
 * snow_ambience, night_crickets, day_birds
 *
 * Started/stopped by AuraBridge — not called directly.
 */
class AmbientSoundController(
    context: Context
) {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(Dispatchers.Main)

    private var snapshotJob: Job? = null
    private var thunderJob: Job? = null

    private var weatherPlayer: MediaPlayer? = null
    private var timePlayer: MediaPlayer? = null

    private var currentWeatherRes: String? = null
    private var currentTimeRes: String? = null

    fun start() {

        if (snapshotJob?.isActive == true) return

        snapshotJob = scope.launch {
            AuraBridge.snapshotFlow.collect { snapshot ->
                applySnapshot(snapshot)
            }
        }
    }

    fun stop() {

        snapshotJob?.cancel()
        snapshotJob = null

        thunderJob?.cancel()
        thunderJob = null

        LightningBus.update(stormActive = false, intensity = 0f)

        fadeOutAndRelease(weatherPlayer)
        fadeOutAndRelease(timePlayer)

        weatherPlayer = null
        timePlayer = null
        currentWeatherRes = null
        currentTimeRes = null
    }

    private fun applySnapshot(snapshot: AuraSnapshot) {

        if (!snapshot.config.ambientSoundEnabled) {
            silenceLoopsIfPlaying()
            return
        }

        val theme = snapshot.theme

        val weatherRes = weatherResFor(theme.weatherState)
        val timeRes = timeResFor(theme.timePhase)

        if (weatherRes != currentWeatherRes) {
            switchLoop(isWeather = true, newRes = weatherRes)
        }

        if (timeRes != currentTimeRes) {
            switchLoop(isWeather = false, newRes = timeRes)
        }

        val isStorm = theme.weatherState == WeatherState.STORM

        LightningBus.update(
            stormActive = isStorm,
            intensity = theme.profile.stormIntensity
        )

        if (isStorm) {
            if (thunderJob?.isActive != true) {
                thunderJob = scope.launch {
                    LightningBus.flashes.collect {
                        playThunderHit()
                    }
                }
            }
        } else {
            thunderJob?.cancel()
            thunderJob = null
        }
    }

    private fun silenceLoopsIfPlaying() {

        if (weatherPlayer != null) {
            fadeOutAndRelease(weatherPlayer)
            weatherPlayer = null
            currentWeatherRes = null
        }

        if (timePlayer != null) {
            fadeOutAndRelease(timePlayer)
            timePlayer = null
            currentTimeRes = null
        }

        thunderJob?.cancel()
        thunderJob = null

        LightningBus.update(stormActive = false, intensity = 0f)
    }

    private fun weatherResFor(weather: WeatherState): String? = when (weather) {
        WeatherState.RAIN -> "rain_loop"
        WeatherState.STORM -> "storm_loop"
        WeatherState.SNOW -> "snow_ambience"
        WeatherState.FOG -> "wind_loop"
        WeatherState.CLOUDY, WeatherState.CLEAR -> null
    }

    private fun timeResFor(phase: TimePhase): String? = when (phase) {
        TimePhase.NIGHT, TimePhase.MIDNIGHT -> "night_crickets"
        TimePhase.MORNING, TimePhase.NOON -> "day_birds"
        else -> null
    }

    private fun switchLoop(isWeather: Boolean, newRes: String?) {

        val oldPlayer = if (isWeather) weatherPlayer else timePlayer

        fadeOutAndRelease(oldPlayer)

        if (isWeather) {
            currentWeatherRes = newRes
            weatherPlayer = newRes?.let { startLoop(it) }
        } else {
            currentTimeRes = newRes
            timePlayer = newRes?.let { startLoop(it) }
        }
    }

    private fun rawResId(name: String): Int {
        return appContext.resources.getIdentifier(name, "raw", appContext.packageName)
    }

    private fun startLoop(resName: String): MediaPlayer? {

        return try {
            val resId = rawResId(resName)
            if (resId == 0) return null  // file not added yet — skip silently
            MediaPlayer.create(appContext, resId)?.apply {
                isLooping = true
                setVolume(0f, 0f)
                start()
                fadeIn(this)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun playThunderHit() {

        try {
            val resId = rawResId("thunder_hit")
            if (resId == 0) return  // file not added yet — skip silently
            MediaPlayer.create(appContext, resId)?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (e: Exception) {
            // Missing or broken asset — skip silently.
        }
    }

    private fun fadeIn(player: MediaPlayer, steps: Int = 15, stepDelayMs: Long = 100L) {

        scope.launch {
            for (i in 1..steps) {
                val volume = i / steps.toFloat()
                try {
                    player.setVolume(volume, volume)
                } catch (e: Exception) {
                    return@launch
                }
                delay(stepDelayMs)
            }
        }
    }

    private fun fadeOutAndRelease(player: MediaPlayer?, steps: Int = 15, stepDelayMs: Long = 80L) {

        if (player == null) return

        scope.launch {
            for (i in steps downTo 0) {
                val volume = i / steps.toFloat()
                try {
                    player.setVolume(volume, volume)
                } catch (e: Exception) {
                    break
                }
                delay(stepDelayMs)
            }
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                // Already released — ignore.
            }
        }
    }
}