package com.example.gnssviewer.data

import android.location.GnssStatus
import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.abs

object GnssUtils {

    fun getConstellationName(constellationType: Int): String {
        return when (constellationType) {
            GnssStatus.CONSTELLATION_GPS -> "GPS"
            GnssStatus.CONSTELLATION_SBAS -> "SBAS"
            GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
            GnssStatus.CONSTELLATION_QZSS -> "QZSS"
            GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
            GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
            GnssStatus.CONSTELLATION_IRNSS -> "NavIC"
            else -> "Unknown"
        }
    }

    fun getConstellationColor(constellationType: Int): Color {
        return when (constellationType) {
            GnssStatus.CONSTELLATION_GPS -> Color(0xFF1E88E5)       // Material Blue
            GnssStatus.CONSTELLATION_GALILEO -> Color(0xFFFB8C00)   // Material Orange
            GnssStatus.CONSTELLATION_GLONASS -> Color(0xFFE53935)   // Material Red
            GnssStatus.CONSTELLATION_BEIDOU -> Color(0xFF8E24AA)    // Material Purple
            GnssStatus.CONSTELLATION_QZSS -> Color(0xFF00ACC1)      // Material Cyan
            GnssStatus.CONSTELLATION_IRNSS -> Color(0xFF43A047)     // Material Green
            GnssStatus.CONSTELLATION_SBAS -> Color(0xFF6D4C41)      // Material Brown
            else -> Color(0xFF757575)                              // Material Grey
        }
    }

    fun formatFrequency(carrierFrequencyHz: Float?): String {
        if (carrierFrequencyHz == null || carrierFrequencyHz <= 0f) {
            return "Legacy / L1"
        }
        val freqMhz = carrierFrequencyHz / 1_000_000.0
        return String.format(Locale.US, "%.2f MHz", freqMhz)
    }

    /**
     * Resolves the GNSS signal frequency band based on constellation type and carrier frequency in Hz.
     */
    fun getBandLabel(constellationType: Int, carrierFrequencyHz: Float?): String {
        if (carrierFrequencyHz == null || carrierFrequencyHz <= 0f) {
            return "L1 / Standard"
        }

        val freqMhz = (carrierFrequencyHz / 1_000_000.0).toFloat()

        return when (constellationType) {
            GnssStatus.CONSTELLATION_GPS -> when {
                isCloseTo(freqMhz, 1575.42f) -> "L1"
                isCloseTo(freqMhz, 1227.60f) -> "L2"
                isCloseTo(freqMhz, 1176.45f) -> "L5"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_GALILEO -> when {
                isCloseTo(freqMhz, 1575.42f) -> "E1"
                isCloseTo(freqMhz, 1176.45f) -> "E5a"
                isCloseTo(freqMhz, 1207.14f) -> "E5b"
                isCloseTo(freqMhz, 1278.75f) -> "E6"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_GLONASS -> when {
                freqMhz in 1598.0f..1606.0f -> "G1"
                freqMhz in 1242.0f..1249.0f -> "G2"
                isCloseTo(freqMhz, 1202.025f) -> "G3"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_BEIDOU -> when {
                isCloseTo(freqMhz, 1561.098f) -> "B1I"
                isCloseTo(freqMhz, 1575.42f) -> "B1C"
                isCloseTo(freqMhz, 1176.45f) -> "B2a"
                isCloseTo(freqMhz, 1207.14f) -> "B2b"
                isCloseTo(freqMhz, 1268.52f) -> "B3I"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_QZSS -> when {
                isCloseTo(freqMhz, 1575.42f) -> "L1"
                isCloseTo(freqMhz, 1227.60f) -> "L2"
                isCloseTo(freqMhz, 1176.45f) -> "L5"
                isCloseTo(freqMhz, 1278.75f) -> "L6"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_IRNSS -> when {
                isCloseTo(freqMhz, 1176.45f) -> "L5"
                isCloseTo(freqMhz, 2492.028f) -> "S-Band"
                else -> "${freqMhz.toInt()} MHz"
            }
            GnssStatus.CONSTELLATION_SBAS -> when {
                isCloseTo(freqMhz, 1575.42f) -> "L1"
                isCloseTo(freqMhz, 1176.45f) -> "L5"
                else -> "${freqMhz.toInt()} MHz"
            }
            else -> "${freqMhz.toInt()} MHz"
        }
    }

    private fun isCloseTo(value: Float, target: Float, tolerance: Float = 2.0f): Boolean {
        return abs(value - target) <= tolerance
    }

    /**
     * Groups raw frequencies into broad frequency categories (e.g. L1/E1, L5/E5a, L2, G1, etc.)
     * for easy chip filtering.
     */
    fun getFrequencyCategory(freqHz: Float?): String {
        if (freqHz == null || freqHz <= 0f) return "L1 / Standard (~1575 MHz)"
        val mhz = freqHz / 1_000_000.0f
        return when {
            isCloseTo(mhz, 1575.42f, 5.0f) -> "L1 / E1 / B1C (~1575 MHz)"
            isCloseTo(mhz, 1561.098f, 5.0f) -> "B1I (~1561 MHz)"
            isCloseTo(mhz, 1602.0f, 6.0f) -> "G1 (~1602 MHz)"
            isCloseTo(mhz, 1176.45f, 5.0f) -> "L5 / E5a / B2a (~1176 MHz)"
            isCloseTo(mhz, 1207.14f, 5.0f) -> "E5b / B2b (~1207 MHz)"
            isCloseTo(mhz, 1227.60f, 5.0f) -> "L2 (~1227 MHz)"
            isCloseTo(mhz, 1246.0f, 5.0f) -> "G2 (~1246 MHz)"
            isCloseTo(mhz, 1278.75f, 15.0f) -> "E6 / B3I / L6 (~1278 MHz)"
            isCloseTo(mhz, 2492.028f, 10.0f) -> "S-Band (~2492 MHz)"
            else -> String.format(Locale.US, "%.1f MHz", mhz)
        }
    }
}

