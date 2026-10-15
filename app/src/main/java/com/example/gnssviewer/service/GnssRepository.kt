package com.example.gnssviewer.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.gnssviewer.data.GnssSignal
import com.example.gnssviewer.data.GnssUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

enum class ReceiverState {
    IDLE,
    SEARCHING,
    RECEIVING,
    GPS_DISABLED,
    NO_PERMISSION
}

class GnssRepository(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    val isGpsEnabled: Boolean
        get() = try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (_: Exception) {
            false
        }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Emits real-time GNSS satellite signals received from Android LocationManager.
     * Crucially requests GPS location updates to power on the GNSS hardware receiver.
     */
    @SuppressLint("MissingPermission")
    fun getRealGnssSignalsFlow(onStateChanged: ((ReceiverState) -> Unit)? = null): Flow<List<GnssSignal>> = callbackFlow {
        if (!hasLocationPermission()) {
            onStateChanged?.invoke(ReceiverState.NO_PERMISSION)
            trySend(emptyList())
            close(SecurityException("ACCESS_FINE_LOCATION permission is required."))
            return@callbackFlow
        }

        if (locationManager == null) {
            trySend(emptyList())
            close(IllegalStateException("LocationManager is not available on this device."))
            return@callbackFlow
        }

        if (!isGpsEnabled) {
            onStateChanged?.invoke(ReceiverState.GPS_DISABLED)
        } else {
            onStateChanged?.invoke(ReceiverState.SEARCHING)
        }

        val handler = Handler(Looper.getMainLooper())

        // 1. LocationListener: CRITICAL to force Android to power on the GNSS hardware chip.
        // In Android, GnssStatus callbacks alone do NOT power on the satellite receiver.
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // GPS receiver is actively acquiring/providing fixes
            }

            override fun onProviderEnabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    onStateChanged?.invoke(ReceiverState.SEARCHING)
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    onStateChanged?.invoke(ReceiverState.GPS_DISABLED)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        // Request GPS provider location updates to keep GNSS receiver awake
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also request PASSIVE_PROVIDER if available
        try {
            if (locationManager.allProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    1000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {}

        // 2. GnssStatus.Callback: Primary source for satellite status, C/N0, and carrier frequencies
        val statusCallback = object : GnssStatus.Callback() {
            override fun onStarted() {
                onStateChanged?.invoke(ReceiverState.SEARCHING)
            }

            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val count = status.satelliteCount
                val currentBatch = ArrayList<GnssSignal>(count)

                for (i in 0 until count) {
                    val svid = status.getSvid(i)
                    val constellationType = status.getConstellationType(i)
                    val cn0 = status.getCn0DbHz(i)

                    val carrierFreqHz: Float? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        status.hasCarrierFrequencyHz(i)
                    ) {
                        status.getCarrierFrequencyHz(i)
                    } else {
                        null
                    }

                    val usedInFix = status.usedInFix(i)

                    val signal = GnssSignal(
                        svid = svid,
                        constellationType = constellationType,
                        constellationName = GnssUtils.getConstellationName(constellationType),
                        carrierFrequencyHz = carrierFreqHz,
                        frequencyString = GnssUtils.formatFrequency(carrierFreqHz),
                        bandLabel = GnssUtils.getBandLabel(constellationType, carrierFreqHz),
                        cn0DbHz = String.format(Locale.US, "%.1f", cn0).toFloat(),
                        usedInFix = usedInFix
                    )
                    currentBatch.add(signal)
                }

                if (currentBatch.isNotEmpty()) {
                    onStateChanged?.invoke(ReceiverState.RECEIVING)
                    trySend(currentBatch)
                } else {
                    onStateChanged?.invoke(ReceiverState.SEARCHING)
                    trySend(emptyList())
                }
            }

            override fun onStopped() {
                onStateChanged?.invoke(ReceiverState.IDLE)
            }
        }

        // 3. GnssMeasurementsEvent.Callback: Also captures raw measurements if supported by chipset
        val measurementsCallback = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                val measurements = event.measurements
                if (measurements.isEmpty()) return

                val batch = ArrayList<GnssSignal>(measurements.size)
                for (m in measurements) {
                    val svid = m.svid
                    val constellation = m.constellationType
                    val cn0 = m.cn0DbHz.toFloat()
                    val freqHz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && m.hasCarrierFrequencyHz()) {
                        m.carrierFrequencyHz
                    } else null

                    val usedInFix = (m.state and GnssMeasurement.STATE_CODE_LOCK) != 0

                    val signal = GnssSignal(
                        svid = svid,
                        constellationType = constellation,
                        constellationName = GnssUtils.getConstellationName(constellation),
                        carrierFrequencyHz = freqHz,
                        frequencyString = GnssUtils.formatFrequency(freqHz),
                        bandLabel = GnssUtils.getBandLabel(constellation, freqHz),
                        cn0DbHz = String.format(Locale.US, "%.1f", cn0).toFloat(),
                        usedInFix = usedInFix
                    )
                    batch.add(signal)
                }

                if (batch.isNotEmpty()) {
                    onStateChanged?.invoke(ReceiverState.RECEIVING)
                    trySend(batch)
                }
            }
        }

        try {
            locationManager.registerGnssStatusCallback(statusCallback, handler)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            locationManager.registerGnssMeasurementsCallback(measurementsCallback, handler)
        } catch (_: Exception) {}

        awaitClose {
            try {
                locationManager.removeUpdates(locationListener)
            } catch (_: Exception) {}
            try {
                locationManager.unregisterGnssStatusCallback(statusCallback)
            } catch (_: Exception) {}
            try {
                locationManager.unregisterGnssMeasurementsCallback(measurementsCallback)
            } catch (_: Exception) {}
        }
    }
}
