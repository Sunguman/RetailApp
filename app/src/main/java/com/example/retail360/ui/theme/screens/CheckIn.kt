package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckInState(
    val loading: Boolean = false,
    val distanceM: Float? = null,
    val allowed: Boolean = false,
    val message: String = "Locating store…",
    val startedVisitId: String? = null
)

class CheckInViewModel : ViewModel() {
    private val customerRepo = Graph.customerRepository
    private val visitRepo = Graph.visitRepository

    private val _state = MutableStateFlow(CheckInState())
    val state = _state.asStateFlow()

    /** Fetch GPS, compare against the customer geofence, and check in automatically if within. */
    fun locateAndCheckIn(context: Context, customerId: String) {
        _state.value = CheckInState(loading = true, message = "Getting GPS fix…")
        viewModelScope.launch {
            val customer = customerRepo.byId(customerId)
            val here = LocationProvider(context).current()
            if (customer == null || here == null) {
                _state.value = CheckInState(message = "Couldn't get a location fix. Ensure GPS is on and try again.")
                return@launch
            }
            val distance = LocationProvider.distanceMeters(
                here.lat, here.lng, customer.latitude, customer.longitude
            )
            val noStoreLoc = customer.latitude == 0.0 && customer.longitude == 0.0
            val within = noStoreLoc || distance <= customer.geofenceRadiusM
            
            if (within) {
                _state.value = _state.value.copy(
                    distanceM = if (noStoreLoc) 0f else distance,
                    allowed = true,
                    message = if (noStoreLoc) "First visit to this store. Capturing location…"
                             else "Store found (%.0f m). Starting visit…".format(distance)
                )
                
                // If the store had no location, update it now so geofencing works next time.
                if (noStoreLoc) {
                    runCatching { 
                        Graph.customerRepository.save(customer.copy(latitude = here.lat, longitude = here.lng))
                    }
                }

                val repId = Graph.authRepository.currentUser()?.uid ?: ""
                val visit = visitRepo.startVisit(customerId, repId, here.lat, here.lng)
                _state.value = _state.value.copy(loading = false, startedVisitId = visit.id)
            } else {
                _state.value = CheckInState(
                    distanceM = distance,
                    allowed = false,
                    message = "You're %.0f m away — must be within %d m to check in.".format(distance, customer.geofenceRadiusM)
                )
            }
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.locateAndCheckIn(context, customerId)
        else onBack() // Cannot proceed without location
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            vm.locateAndCheckIn(context, customerId)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(state.startedVisitId) {
        state.startedVisitId?.let { onCheckedIn(it) }
    }

    Retail360Scaffold(
        title = "Check in",
        onBack = onBack
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(24.dp))
            } else if (!state.allowed && state.distanceM != null) {
                Icon(
                    Icons.Default.MyLocation, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Icon(
                    Icons.Default.MyLocation, 
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(24.dp))
            }

            Text(
                state.message, 
                textAlign = TextAlign.Center, 
                style = MaterialTheme.typography.titleMedium
            )

            if (!state.loading && !state.allowed) {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { vm.locateAndCheckIn(context, customerId) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Retry location check") }
            }
        }
    }
}
