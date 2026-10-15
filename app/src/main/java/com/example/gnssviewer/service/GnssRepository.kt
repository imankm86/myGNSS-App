package com.example.gnssviewer.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.gnssviewer.data.GnssSignal
import com.example.gnssviewer.data.GnssUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class GnssRepository(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    val isGpsEnabled: Boolean
        get() = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Emits real-time GNSS satellite signals received from Android LocationManager.
     */
    @SuppressLint("MissingPermission")
    fun getRealGnssSignalsFlow(): Flow<List<GnssSignal>> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(emptyList())
            close(SecurityException("ACCESS_FINE_LOCATION permission is required."))
            return@callbackFlow
        }

        if (locationManager == null) {
            trySend(emptyList())
            close(IllegalStateException("LocationManager is not available on this device."))
            return@callbackFlow
        }

        val handler = Handler(Looper.getMainLooper())

        val callback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val count = status.satelliteCount
                val signals = ArrayList<GnssSignal>(count)

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
                        cn0DbHz = String.format(java.util.Locale.US, "%.1f", cn0).toFloat(),
                        usedInFix = usedInFix
                    )
                    signals.add(signal)
                }

                trySend(signals)
            }

            override fun onStarted() {
                // GNSS status updates started
            }

            override fun onStopped() {
                trySend(emptyList())
            }
        }

        val registered = try {
            locationManager.registerGnssStatusCallback(callback, handler)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        if (!registered) {
            close(IllegalStateException("Failed to register GnssStatusCallback."))
            return@callbackFlow
        }

        awaitClose {
            try {
                locationManager.unregisterGnssStatusCallback(callback)
            } catch (_: Exception) {
            }
        }
    }
}

