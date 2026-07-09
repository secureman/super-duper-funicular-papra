package com.papra.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.papra.mobile.data.remote.dto.OrganizationDto

@Composable
fun OrgSwitcher(
    organizations: List<OrganizationDto>,
    active: OrganizationDto?,
    onSelect: (OrganizationDto) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = active?.name ?: "Select organization",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch organization")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            organizations.forEach { org ->
                DropdownMenuItem(
                    text = { Text(org.name) },
                    onClick = {
                        expanded = false
                        onSelect(org)
                    },
                )
            }
        }
    }
}
