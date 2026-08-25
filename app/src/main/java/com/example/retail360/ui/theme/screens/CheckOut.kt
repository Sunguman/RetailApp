package com.example.retail360.ui.theme.screens


import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.example.retail360.ui.theme.Components.brandedTopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import com.example.retail360.util.LatLng
import com.example.retail360.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckOutViewModel : ViewModel() {
    private val _done = MutableStateFlow(false)
    val done = _done.asStateFlow()

    fun checkOut(context: Context, visitId: String, notes: String) {
        viewModelScope.launch {
            val loc = LocationProvider(context).current()
            Graph.visitRepository.checkOut(
                visitId = visitId,
                lat = loc?.lat ?: 0.0, lng = loc?.lng ?: 0.0,
                notes = notes
            )
            _done.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(
    visitId: String,
    onBack: () -> Unit,
    onCheckedOut: () -> Unit,
    vm: CheckOutViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val done by vm.done.collectAsStateSafe()

    var notes by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<LatLng?>(null) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) scope.launch { location = LocationProvider(context).current() } }

    LaunchedEffect(done) { if (done) onCheckedOut() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check out") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = brandedTopBarColors()
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Visit notes") },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            OutlinedButton(
                onClick = { permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(location?.let { "Check-out location set" } ?: "Capture check-out location")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.checkOut(context, visitId, notes) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Complete visit") }
        }
    }
}