package com.papra.mobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.papra.mobile.data.remote.dto.OrganizationDto

/**
 * Icon-button-anchored dropdown, deliberately NOT placed in a TopAppBar
 * title slot -- Material3's CenterAlignedTopAppBar tightly constrains/
 * centers that slot and can end up clipping or misaligning an interactive
 * child's tap target. An IconButton in the actions row gets a real 48dp
 * touch target and anchors the DropdownMenu reliably. This mirrors how
 * Drive itself places its account switcher as a standalone icon rather
 * than inside the title.
 */
@Composable
fun OrgSwitcher(
    organizations: List<OrganizationDto>,
    active: OrganizationDto?,
    onSelect: (OrganizationDto) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.Apartment, contentDescription = "Switch organization")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (organizations.isEmpty()) {
            DropdownMenuItem(text = { androidx.compose.material3.Text("No organizations found") }, onClick = {})
        }
        organizations.forEach { org ->
            DropdownMenuItem(
                text = { androidx.compose.material3.Text(org.name) },
                onClick = {
                    expanded = false
                    onSelect(org)
                },
            )
        }
    }
}
