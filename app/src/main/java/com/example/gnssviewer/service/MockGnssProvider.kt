package com.example.gnssviewer.service

import android.location.GnssStatus
import com.example.gnssviewer.data.GnssSignal
import com.example.gnssviewer.data.GnssUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Generates realistic simulated GNSS signals across multiple constellations and frequencies.
 * Useful when running indoors, without satellite lock, or on an Android emulator.
 */
class MockGnssProvider {

    private val baseSignals = listOf(
        // GPS satellites (L1 and dual-frequency L5)
        MockSat(1, GnssStatus.CONSTELLATION_GPS, 1575420000f, 41.2f, true),
        MockSat(1, GnssStatus.CONSTELLATION_GPS, 1176450000f, 38.5f, true),
        MockSat(3, GnssStatus.CONSTELLATION_GPS, 1575420000f, 36.8f, true),
        MockSat(8, GnssStatus.CONSTELLATION_GPS, 1575420000f, 29.4f, false),
        MockSat(14, GnssStatus.CONSTELLATION_GPS, 1575420000f, 39.1f, true),
        MockSat(14, GnssStatus.CONSTELLATION_GPS, 1176450000f, 35.7f, true),
        MockSat(22, GnssStatus.CONSTELLATION_GPS, 1575420000f, 42.0f, true),
        MockSat(31, GnssStatus.CONSTELLATION_GPS, 1575420000f, 24.3f, false),

        // Galileo satellites (E1 and E5a)
        MockSat(2, GnssStatus.CONSTELLATION_GALILEO, 1575420000f, 43.1f, true),
        MockSat(2, GnssStatus.CONSTELLATION_GALILEO, 1176450000f, 39.8f, true),
        MockSat(7, GnssStatus.CONSTELLATION_GALILEO, 1575420000f, 37.6f, true),
        MockSat(11, GnssStatus.CONSTELLATION_GALILEO, 1575420000f, 40.5f, true),
        MockSat(11, GnssStatus.CONSTELLATION_GALILEO, 1176450000f, 36.2f, true),
        MockSat(19, GnssStatus.CONSTELLATION_GALILEO, 1575420000f, 28.1f, false),

        // GLONASS satellites (G1 and G2 FDMA)
        MockSat(4, GnssStatus.CONSTELLATION_GLONASS, 1602000000f, 38.0f, true),
        MockSat(4, GnssStatus.CONSTELLATION_GLONASS, 1246000000f, 32.5f, true),
        MockSat(9, GnssStatus.CONSTELLATION_GLONASS, 1603125000f, 35.2f, true),
        MockSat(15, GnssStatus.CONSTELLATION_GLONASS, 1601437500f, 30.1f, false),
        MockSat(21, GnssStatus.CONSTELLATION_GLONASS, 1604250000f, 37.9f, true),

        // BeiDou satellites (B1I, B1C, B2a)
        MockSat(6, GnssStatus.CONSTELLATION_BEIDOU, 1561098000f, 39.5f, true),
        MockSat(6, GnssStatus.CONSTELLATION_BEIDOU, 1176450000f, 36.0f, true),
        MockSat(12, GnssStatus.CONSTELLATION_BEIDOU, 1575420000f, 41.8f, true),
        MockSat(16, GnssStatus.CONSTELLATION_BEIDOU, 1561098000f, 33.4f, true),
        MockSat(24, GnssStatus.CONSTELLATION_BEIDOU, 1176450000f, 34.8f, true),

        // QZSS satellite (L1 and L5)
        MockSat(193, GnssStatus.CONSTELLATION_QZSS, 1575420000f, 40.0f, true),
        MockSat(193, GnssStatus.CONSTELLATION_QZSS, 1176450000f, 37.2f, true)
    )

    fun getSignalsFlow(): Flow<List<GnssSignal>> = flow {
        val random = Random(System.currentTimeMillis())
        while (true) {
            val signals = baseSignals.map { mock ->
                // Apply slight fluctuation to C/N0 (±0.8 dB-Hz)
                val delta = (random.nextFloat() - 0.5f) * 1.6f
                val currentCn0 = (mock.baseCn0 + delta).coerceIn(15.0f, 48.0f)

                GnssSignal(
                    svid = mock.svid,
                    constellationType = mock.constellation,
                    constellationName = GnssUtils.getConstellationName(mock.constellation),
                    carrierFrequencyHz = mock.freqHz,
                    frequencyString = GnssUtils.formatFrequency(mock.freqHz),
                    bandLabel = GnssUtils.getBandLabel(mock.constellation, mock.freqHz),
                    cn0DbHz = String.format(java.util.Locale.US, "%.1f", currentCn0).toFloat(),
                    usedInFix = mock.usedInFix
                )
            }
            emit(signals)
            delay(1000L) // Update once per second, typical for GNSS NMEA/status updates
        }
    }

    private data class MockSat(
        val svid: Int,
        val constellation: Int,
        val freqHz: Float,
        val baseCn0: Float,
        val usedInFix: Boolean
    )
}

