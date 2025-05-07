package com.kevmo314.kineticstreamer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StreamingConfiguration(
    val url: String,
    val width: Int,
    val height: Int,
    val frameRate: Int,
    val bitrate: Int
) : Parcelable
