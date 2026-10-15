package com.example.gnssviewer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gnssviewer.ui.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    availableConstellations: List<Pair<String, Int>>,
    selectedConstellation: String,
    onConstellationSelected: (String) -> Unit,
    availableFrequencies: List<Pair<String, Int>>,
    selectedFrequency: String,
    onFrequencySelected: (String) -> Unit,
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Constellations Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CONSTELLATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sort button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                InputChip(
                    selected = true,
                    onClick = { sortMenuExpanded = true },
                    label = {
                        Text(
                            text = "Sort: ${currentSort.displayName}",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Sort",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Sort",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                onSortSelected(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Horizontal chips for Constellations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableConstellations.forEach { (name, count) ->
                val selected = selectedConstellation.equals(name, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onConstellationSelected(name) },
                    label = {
                        Text(
                            text = if (name == "All") "All ($count)" else "$name ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Frequencies Row
        Text(
            text = "FREQUENCY / BAND",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        // Horizontal chips for Frequencies
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableFrequencies.forEach { (name, count) ->
                val selected = selectedFrequency.equals(name, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onFrequencySelected(name) },
                    label = {
                        Text(
                            text = if (name == "All") "All Frequencies ($count)" else "$name ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
