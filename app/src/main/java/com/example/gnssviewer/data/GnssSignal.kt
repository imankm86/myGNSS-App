package com.example.gnssviewer.data

import java.util.Locale

/**
 * Represents a single GNSS satellite signal received by the device.
 */
data class GnssSignal(
    val svid: Int,
    val constellationType: Int,
    val constellationName: String,
    val carrierFrequencyHz: Float?,
    val frequencyString: String,
    val bandLabel: String,
    val cn0DbHz: Float,
    val usedInFix: Boolean
) {
    val id: String
        get() = "${constellationType}_${svid}_${carrierFrequencyHz?.toInt() ?: 0}"
}
