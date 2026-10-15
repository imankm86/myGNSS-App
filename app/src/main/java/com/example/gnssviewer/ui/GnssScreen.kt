package com.example.gnssviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gnssviewer.ui.components.FilterBar
import com.example.gnssviewer.ui.components.SignalCard
import com.example.gnssviewer.ui.theme.Amber500
import com.example.gnssviewer.ui.theme.Emerald500
import com.example.gnssviewer.ui.theme.Sky500
import com.example.gnssviewer.ui.theme.Slate400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GnssScreen(
    viewModel: GnssViewModel,
    onRequestPermission: () -> Unit
) {
    val filterState by viewModel.filterState.collectAsState()
    val filteredSignals by viewModel.filteredSignals.collectAsState()
    val rawSignals by viewModel.rawSignals.collectAsState()
    val availableConstellations by viewModel.availableConstellations.collectAsState()
    val availableFrequencies by viewModel.availableFrequencies.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (filterState.isDemoMode) Icons.Default.Sensors else Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = if (filterState.isDemoMode) Amber500 else Sky500,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GNSS C/N₀ Viewer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Demo mode toggle switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (filterState.isDemoMode) "Demo" else "Live",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterState.isDemoMode) Amber500 else Sky500
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = filterState.isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Amber500,
                                checkedTrackColor = Amber500.copy(alpha = 0.3f)
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission Banner (if real mode and permission missing)
            if (!filterState.hasPermission && !filterState.isDemoMode) {
                PermissionBanner(onRequestPermission = onRequestPermission)
            }

            // Summary Metrics Card
            SummaryHeader(
                totalSignals = rawSignals.size,
                filteredCount = filteredSignals.size,
                usedInFixCount = filteredSignals.count { it.usedInFix },
                averageCn0 = if (filteredSignals.isNotEmpty()) {
                    filteredSignals.map { it.cn0DbHz }.average().toFloat()
                } else null
            )

            // Filtering Bar (Constellations and Frequencies)
            FilterBar(
                availableConstellations = availableConstellations,
                selectedConstellation = filterState.selectedConstellation,
                onConstellationSelected = { viewModel.selectConstellation(it) },
                availableFrequencies = availableFrequencies,
                selectedFrequency = filterState.selectedFrequencyCategory,
                onFrequencySelected = { viewModel.selectFrequencyCategory(it) },
                currentSort = filterState.sortOption,
                onSortSelected = { viewModel.setSortOption(it) }
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Signals List
            if (filteredSignals.isEmpty()) {
                EmptySignalsView(
                    isDemoMode = filterState.isDemoMode,
                    hasPermission = filterState.hasPermission,
                    onEnableDemo = { viewModel.toggleDemoMode(true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredSignals,
                        key = { it.id }
                    ) { signal ->
                        SignalCard(signal = signal)
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    totalSignals: Int,
    filteredCount: Int,
    usedInFixCount: Int,
    averageCn0: Float?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visible Signals
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$filteredCount / $totalSignals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Signals (Shown/Total)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // In Fix
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$usedInFixCount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Emerald500
                )
                Text(
                    text = "Used in Fix",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avg C/N0
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (averageCn0 != null) String.format(java.util.Locale.US, "%.1f", averageCn0) else "--",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Sky500
                )
                Text(
                    text = "Avg C/N₀ (dB-Hz)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Amber500.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Amber500,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Grant precise location permission to access GNSS satellite telemetry.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Amber500)
            ) {
                Text("Grant", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun EmptySignalsView(
    isDemoMode: Boolean,
    hasPermission: Boolean,
    onEnableDemo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.GpsNotFixed,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Slate400
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No GNSS Signals Detected",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (!hasPermission && !isDemoMode) {
                    "Location permission is needed to receive satellite signals from the GNSS hardware."
                } else if (!isDemoMode) {
                    "Waiting for GNSS satellite lock. Make sure GPS/Location is enabled and you have clear view of the open sky, or turn on Demo Mode."
                } else {
                    "No signals match the selected constellation or frequency filters."
                },
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (!isDemoMode) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onEnableDemo,
                    colors = ButtonDefaults.buttonColors(containerColor = Sky500)
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn On Demo Mode")
                }
            }
        }
    }
}
