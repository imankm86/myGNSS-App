package com.example.gnssviewer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gnssviewer.data.GnssSignal
import com.example.gnssviewer.data.GnssUtils
import com.example.gnssviewer.service.GnssRepository
import com.example.gnssviewer.service.MockGnssProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    CN0_DESC("Highest C/N0"),
    CN0_ASC("Lowest C/N0"),
    CONSTELLATION("Constellation"),
    FREQUENCY("Frequency"),
    SVID("Satellite PRN")
}

data class FilterState(
    val selectedConstellation: String = "All",
    val selectedFrequencyCategory: String = "All",
    val sortOption: SortOption = SortOption.CN0_DESC,
    val isDemoMode: Boolean = false,
    val hasPermission: Boolean = false
)

class GnssViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GnssRepository(application.applicationContext)
    private val mockProvider = MockGnssProvider()

    private val _rawSignals = MutableStateFlow<List<GnssSignal>>(emptyList())
    val rawSignals: StateFlow<List<GnssSignal>> = _rawSignals.asStateFlow()

    private val _filterState = MutableStateFlow(
        FilterState(hasPermission = repository.hasLocationPermission())
    )
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private var signalCollectorJob: Job? = null

    init {
        refreshPermission()
        startSignalCollection()
    }

    fun refreshPermission() {
        val granted = repository.hasLocationPermission()
        _filterState.value = _filterState.value.copy(hasPermission = granted)
        if (granted && !_filterState.value.isDemoMode) {
            startSignalCollection()
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        _filterState.value = _filterState.value.copy(isDemoMode = enabled)
        startSignalCollection()
    }

    fun selectConstellation(constellation: String) {
        _filterState.value = _filterState.value.copy(selectedConstellation = constellation)
    }

    fun selectFrequencyCategory(freqCategory: String) {
        _filterState.value = _filterState.value.copy(selectedFrequencyCategory = freqCategory)
    }

    fun setSortOption(option: SortOption) {
        _filterState.value = _filterState.value.copy(sortOption = option)
    }

    private fun startSignalCollection() {
        signalCollectorJob?.cancel()
        signalCollectorJob = viewModelScope.launch {
            if (_filterState.value.isDemoMode) {
                mockProvider.getSignalsFlow().collect { signals ->
                    _rawSignals.value = signals
                }
            } else {
                if (repository.hasLocationPermission()) {
                    try {
                        repository.getRealGnssSignalsFlow().collect { signals ->
                            _rawSignals.value = signals
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _rawSignals.value = emptyList()
                    }
                } else {
                    _rawSignals.value = emptyList()
                }
            }
        }
    }

    // Filtered and sorted signals
    val filteredSignals: StateFlow<List<GnssSignal>> = combine(_rawSignals, _filterState) { signals, filter ->
        var list = signals

        // Filter by Constellation
        if (filter.selectedConstellation != "All") {
            list = list.filter { it.constellationName.equals(filter.selectedConstellation, ignoreCase = true) }
        }

        // Filter by Frequency category
        if (filter.selectedFrequencyCategory != "All") {
            list = list.filter {
                GnssUtils.getFrequencyCategory(it.carrierFrequencyHz) == filter.selectedFrequencyCategory
            }
        }

        // Sort
        when (filter.sortOption) {
            SortOption.CN0_DESC -> list.sortedByDescending { it.cn0DbHz }
            SortOption.CN0_ASC -> list.sortedBy { it.cn0DbHz }
            SortOption.CONSTELLATION -> list.sortedWith(compareBy({ it.constellationName }, { it.svid }))
            SortOption.FREQUENCY -> list.sortedByDescending { it.carrierFrequencyHz ?: 0f }
            SortOption.SVID -> list.sortedBy { it.svid }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic list of available constellations from current signals
    val availableConstellations: StateFlow<List<Pair<String, Int>>> = _rawSignals.combine(_filterState) { signals, filter ->
        val counts = mutableMapOf<String, Int>()
        // Filter by current frequency selection so counts reflect applicable signals
        val relevantSignals = if (filter.selectedFrequencyCategory != "All") {
            signals.filter { GnssUtils.getFrequencyCategory(it.carrierFrequencyHz) == filter.selectedFrequencyCategory }
        } else {
            signals
        }

        counts["All"] = relevantSignals.size
        for (sig in relevantSignals) {
            counts[sig.constellationName] = (counts[sig.constellationName] ?: 0) + 1
        }

        val standardOrder = listOf("All", "GPS", "Galileo", "GLONASS", "BeiDou", "QZSS", "NavIC", "SBAS")
        val result = mutableListOf<Pair<String, Int>>()
        for (name in standardOrder) {
            if (counts.containsKey(name) || name == "All") {
                result.add(name to (counts[name] ?: 0))
            }
        }
        // Add any remaining
        for ((name, count) in counts) {
            if (name !in standardOrder) {
                result.add(name to count)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All" to 0))

    // Dynamic list of available frequency bands/categories from current signals
    val availableFrequencies: StateFlow<List<Pair<String, Int>>> = _rawSignals.combine(_filterState) { signals, filter ->
        val counts = mutableMapOf<String, Int>()
        // Filter by current constellation selection
        val relevantSignals = if (filter.selectedConstellation != "All") {
            signals.filter { it.constellationName.equals(filter.selectedConstellation, ignoreCase = true) }
        } else {
            signals
        }

        counts["All"] = relevantSignals.size
        for (sig in relevantSignals) {
            val cat = GnssUtils.getFrequencyCategory(sig.carrierFrequencyHz)
            counts[cat] = (counts[cat] ?: 0) + 1
        }

        val result = mutableListOf<Pair<String, Int>>()
        result.add("All" to (counts["All"] ?: 0))
        for ((cat, count) in counts) {
            if (cat != "All") {
                result.add(cat to count)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All" to 0))
}

