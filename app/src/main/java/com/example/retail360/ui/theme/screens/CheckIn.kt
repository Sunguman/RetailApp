package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.salesautomation.ui.collectAsStateSafe
import com.example.salesautomation.util.Graph
import com.example.salesautomation.util.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CheckInState(
    val loading: Boolean = false,
    val distanceM: Float? = null,
    val allowed: Boolean = false,
    val message: String = "Tap to capture your location",
    val startedVisitId: String? = null
)

class CheckInViewModel : ViewModel() {
    private val customerRepo = Graph.customerRepository
    private val visitRepo = Graph.visitRepository

    private val _state = MutableStateFlow(CheckInState())
    val state = _state.asStateFlow()

    fun locateAndValidate(context: Context, customerId: String) {
        _state.value = CheckInState(loading = true, message = "Getting GPS fix…")
        viewModelScope.launch {
            val customer = customerRepo.byId(customerId)
            val here = LocationProvider(context).current()
            if (customer == null || here == null) {
                _state.value = CheckInState(message = "Couldn't get a location fix. Try again.")
                return@launch
            }
            val distance = LocationProvider.distanceMeters(
                here.lat, here.lng, customer.latitude, customer.longitude
            )
            val within = customer.latitude != 0.0 && distance <= customer.geofenceRadiusM
            _state.value = CheckInState(
                distanceM = distance,
                allowed = within,
                message = if (within) "You're at the store (%.0f m). Ready to check in.".format(distance)
                else "You're %.0f m away — must be within %d m.".format(distance, customer.geofenceRadiusM)
            )
        }
    }

    fun checkIn(context: Context, customerId: String, selfieUri: Uri?) {
        viewModelScope.launch {
            val here = LocationProvider(context).current() ?: return@launch
            val repId = Graph.authRepository.currentUser()?.uid ?: ""
            val visit = visitRepo.startVisit(customerId, repId, here.lat, here.lng, selfieUri)
            _state.value = _state.value.copy(startedVisitId = visit.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    customerId: String,
    onBack: () -> Unit,
    onCheckedIn: (String) -> Unit,
    vm: CheckInViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateSafe()

    var selfieUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.locateAndValidate(context, customerId) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) selfieUri = pendingUri }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createSelfieUri(context)
            pendingUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(state.startedVisitId) {
        state.startedVisitId?.let { onCheckedIn(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Location ----
            if (state.loading) CircularProgressIndicator()
            else Icon(Icons.Default.MyLocation, contentDescription = null,
                modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(12.dp))
            Text(state.message, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Get my location") }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            // ---- Selfie gate ----
            if (selfieUri == null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You need to take a selfie before checking into this customer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text("  Take selfie")
                }
            } else {
                AsyncImage(
                    model = selfieUri,
                    contentDescription = "Check-in selfie",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text("  Selfie captured", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
                    Text("Retake")
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---- Check in (needs both: in range + selfie) ----
            val canCheckIn = state.allowed && selfieUri != null
            Button(
                onClick = { vm.checkIn(context, customerId, selfieUri) },
                enabled = canCheckIn,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Check in here") }

            if (!canCheckIn) {
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        !state.allowed -> "Get within range to enable check-in."
                        selfieUri == null -> "Take a selfie to enable check-in."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Creates a cache-file Uri (via FileProvider) for the camera to write the selfie into. */
private fun createSelfieUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "selfie_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
