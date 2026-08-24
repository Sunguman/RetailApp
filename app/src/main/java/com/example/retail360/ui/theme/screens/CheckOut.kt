package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.retail360.ui.theme.Retail360Theme

class CheckOutViewModel : ViewModel() {
    private val _done = MutableStateFlow(false); val done = _done.asStateFlow()
    fun checkOut(context: Context, visitId: String, notes: String) { viewModelScope.launch { val loc = LocationProvider(context).current(); Graph.visitRepository.checkOut(visitId, loc?.lat ?: 0.0, loc?.lng ?: 0.0, notes); _done.value = true } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(visitId: String, onBack: () -> Unit, onCheckedOut: () -> Unit, vm: CheckOutViewModel = viewModel()) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val done by vm.done.collectAsStateSafe(); var notes by remember { mutableStateOf("") }; var location by remember { mutableStateOf<LatLng?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) scope.launch { location = LocationProvider(context).current() } }
    LaunchedEffect(done) { if (done) onCheckedOut() }
    Scaffold(topBar = { TopAppBar(title = { Text("Check out") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        CheckOutContent(notes, { notes = it }, location != null, { permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }, { vm.checkOut(context, visitId, notes) }, Modifier.padding(padding))
    }
}

@Composable
fun CheckOutContent(notes: String, onNotesChange: (String) -> Unit, locationSet: Boolean, onRequestLocation: () -> Unit, onCheckOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.fillMaxSize().then(modifier).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Visit notes") }, modifier = Modifier.fillMaxWidth().height(160.dp))
        OutlinedButton(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth()) { Text(if (locationSet) "Location set" else "Capture location") }
        Spacer(Modifier.height(8.dp)); Button(onClick = onCheckOut, modifier = Modifier.fillMaxWidth()) { Text("Complete visit") }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckOutPreview() { Retail360Theme { CheckOutContent("Test notes", {}, true, {}, {}) } }
