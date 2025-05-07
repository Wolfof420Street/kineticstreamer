package com.kevmo314.kineticstreamer

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.ln
import kotlin.math.pow

enum class SupportedVideoCodec(val mimeType: String) {
    H264("video/avc"),
    H265("video/hevc"),
    VP8("video/x-vnd.on2.vp8"),
    VP9("video/x-vnd.on2.vp9"),
    AV1("video/av01");
}

enum class SupportedAudioCodec(val mimeType: String) {
    AAC(MediaFormat.MIMETYPE_AUDIO_AAC),
    OPUS(MediaFormat.MIMETYPE_AUDIO_OPUS);
}


fun bytesToString(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
}

data class Resolution(val width: Int, val height: Int) {
    override fun toString(): String = "${width}x${height}"

    companion object {
        fun fromString(string: String): Resolution {
            val (width, height) = string.split("x")
            return Resolution(width.toInt(), height.toInt())
        }
    }
}

data class OutputConfiguration(val url: String, val enabled: Boolean) {
    override fun toString(): String = "$enabled:$url"

    val protocol: String
        get() = url.split(":", limit=2).first().uppercase()

    companion object {
        fun fromString(string: String): OutputConfiguration {
            val (enabled, url) = string.split(":", limit=2)
            return OutputConfiguration(url, enabled.toBoolean())
        }
    }
}

class Settings(private val dataStore: DataStore<Preferences>) {
    private val _codec = stringPreferencesKey("codec")
    private val _bitrate = intPreferencesKey("bitrate")
    private val _resolution = stringPreferencesKey("resolution")
    private val _outputConfigurations = stringPreferencesKey("output_configurations")
    private val _recordingMaxCapacityBytes = longPreferencesKey("recording_max_capacity_bytes")

    val codec = dataStore.data
         .map { SupportedVideoCodec.valueOf(it[_codec] ?: SupportedVideoCodec.H264.name) }

    suspend fun setCodec(codec: SupportedVideoCodec) {
        dataStore.edit { it[_codec] = codec.name }
    }

    val bitrate = dataStore.data.map { it[_bitrate] ?: 2_000_000 }

    suspend fun setBitrate(bitrate: Int) {
        dataStore.edit { it[_bitrate] = bitrate }
    }

    val resolution = dataStore.data.map {
        Resolution.fromString(it[_resolution] ?: "1920x1080")
    }

    suspend fun setResolution(resolution: Resolution) {
        dataStore.edit { it[_resolution] = resolution.toString() }
    }

    val outputConfigurations = dataStore.data
        .map { it[_outputConfigurations] }
        .map {
            it?.split(";")?.map { s -> OutputConfiguration.fromString(s) } ?: listOf()
        }

    suspend fun setOutputConfigurations(outputConfigurations: List<OutputConfiguration>) {
        dataStore.edit {
            it[_outputConfigurations] = outputConfigurations.joinToString(";") { c -> c.toString() }
        }
    }

    val recordingMaxCapacityBytes = dataStore.data.map {
        it[_recordingMaxCapacityBytes] ?: (1024L * 1024 * 1024)
    }

    suspend fun setRecordingMaxCapacityBytes(maxCapacityBytes: Long) {
        dataStore.edit { it[_recordingMaxCapacityBytes] = maxCapacityBytes }
    }

    suspend fun getStreamingConfiguration(): StreamingConfiguration {
        val currentResolution = resolution.first()
        val currentBitrate = bitrate.first()
        return StreamingConfiguration(
            url = "rtsp://localhost:8554/stream",
            width = currentResolution.width,
            height = currentResolution.height,
            frameRate = 30,
            bitrate = currentBitrate
        )
    }

    fun createStreamingConfiguration(url: String): StreamingConfiguration {
        return StreamingConfiguration(
            url = url,
            width = 1280,
            height = 720,
            frameRate = 30,
            bitrate = 2_000_000 // 2 Mbps
        )
    }
}
