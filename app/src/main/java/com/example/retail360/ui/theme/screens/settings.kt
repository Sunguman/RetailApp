package com.example.retail360.ui.theme.screens


import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.navigation.LocalDrawerOpener
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.SectionHeader
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {
    /** Wipes every locally cached table. Runs off the main thread (Room requires it). */
    fun clearLocalData(onCleared: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { Graph.db.clearAllTables() }
            onCleared()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }

    Retail360Scaffold(
        title = "Settings",
        onMenu = LocalDrawerOpener.current,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Device")

            SettingRow(
                icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                title = "App permissions",
                subtitle = "Manage location and camera access in system settings"
            ) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }

            SettingRow(
                icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                title = "Clear local data",
                subtitle = "Remove customers, visits and sales stored on this device",
                destructive = true
            ) { confirmClear = true }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear local data?") },
            text = {
                Text(
                    "This removes all customers, visits, availability and sales cached on this " +
                            "device. Anything not yet synced to the server will be lost."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearLocalData {
                        scope.launch { snackbar.showSnackbar("Local data cleared") }
                    }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint =
            if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint
        ) {
            Column(Modifier.size(24.dp)) { icon() }
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}
