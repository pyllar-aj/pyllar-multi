package com.pyllar.consumer.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ContainedButtonBox(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    showError: Boolean = false
) {
    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(options) { option ->
                Button(
                    onClick = { onOptionSelected(option) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedOption == option)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedOption == option)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    border = if (selectedOption != option) {
                        BorderStroke(
                            1.dp,
                            if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    } else null,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = option.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownFieldWithDisplay(
    label: String,
    selected: String,
    options: List<String>,
    displayMap: Map<String, String>,
    onSelect: (String) -> Unit,
    showError: Boolean = false,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selected.isNotBlank()) displayMap[selected] ?: selected.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } else "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clickable(enabled = enabled) { if (enabled) expanded = true },
            isError = showError,
            supportingText = if (showError) {
                { Text("Field required", color = MaterialTheme.colorScheme.error) }
            } else null
        )
        if (enabled) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayMap[option] ?: option.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ToggleButton(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    showError: Boolean = false
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp)
        ) {
            Switch(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 12.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    checkedTrackColor = if (showError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        if (showError) {
            Text(
                text = "Field required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
