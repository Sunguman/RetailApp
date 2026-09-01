package com.example.retail360.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Searchable exposed dropdown for master data. The user can TYPE to filter, but a value is
 * only committed by picking an option from the list — free-typed text is never accepted.
 * Disabled until its parent selection is made (cascading dropdowns).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Show the committed selection when closed; show the live filter text when open.
    val fieldText = if (expanded) query else selected
    val filtered = if (query.isBlank()) options
    else options.filter { it.contains(query, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) { expanded = it; if (it) query = "" } },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = fieldText,
            onValueChange = { query = it; expanded = true },
            readOnly = !enabled,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text("Select $label") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled)
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matches") }, onClick = { }, enabled = false)
            } else {
                filtered.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option); query = ""; expanded = false }
                    )
                }
            }
        }
    }
}
