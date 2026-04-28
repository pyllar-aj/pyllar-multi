package com.pyllar.consumer.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pyllar.consumer.util.getCurrentYear

// ── Date utilities (KMP-safe, no java.util.Calendar) ─────────────────────────

/** Returns whether [year] is a leap year. */
private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

/** Days in [month] (1-based) of [year]. */
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

/** Naïve cut-off year for 18+ (current year - 18). */
private val CUTOFF_YEAR = getCurrentYear() - 18

@Composable
fun HierarchicalDatePicker(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    currentStep: Int,
    selectedYear: Int?,
    selectedMonth: Int?,
    selectedDay: Int?,
    onStepChange: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentStep) { 0 -> "Year"; 1 -> "Month"; 2 -> "Day"; else -> "Date" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (currentStep > 0) {
                        TextButton(onClick = { onStepChange(currentStep - 1) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Text("Back")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (currentStep) {
                    0 -> KmpYearPicker(onYearSelected = { y -> onYearSelected(y); onStepChange(1) })
                    1 -> KmpMonthPicker(selectedYear = selectedYear, onMonthSelected = { m -> onMonthSelected(m); onStepChange(2) })
                    2 -> KmpDayPicker(selectedYear = selectedYear, selectedMonth = selectedMonth,
                        onDaySelected = { d ->
                            onDaySelected(d)
                            if (selectedYear != null && selectedMonth != null) onDateSelected(selectedYear, selectedMonth, d)
                        })
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun KmpYearPicker(onYearSelected: (Int) -> Unit) {
    val years = (1950..CUTOFF_YEAR).toList().reversed()
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(years) { year ->
            Card(modifier = Modifier.aspectRatio(1.5f).clickable { onYearSelected(year) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(year.toString(), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KmpMonthPicker(selectedYear: Int?, onMonthSelected: (Int) -> Unit) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val count = if (selectedYear != null && selectedYear >= CUTOFF_YEAR) {
        minOf(months.size, 12)
    } else months.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null) {
            Text("Year: $selectedYear", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until count).toList()) { idx ->
                Card(modifier = Modifier.aspectRatio(1.5f).clickable { onMonthSelected(idx + 1) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(months[idx], style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun KmpDayPicker(selectedYear: Int?, selectedMonth: Int?, onDaySelected: (Int) -> Unit) {
    val days = if (selectedYear != null && selectedMonth != null)
        daysInMonth(selectedYear, selectedMonth) else 31
    val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null && selectedMonth != null) {
            Text("${monthNames[selectedMonth - 1]} $selectedYear",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items((1..days).toList()) { day ->
                Card(modifier = Modifier.aspectRatio(1f).clickable { onDaySelected(day) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
