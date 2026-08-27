package com.example.retail360.ui.theme.screens


import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.example.retail360.ui.components.brandedTopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.Customer
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.Graph
import com.example.retail360.util.LatLng
import com.example.retail360.util.LocationProvider
import com.example.retail360.util.collectAsStateSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect

class CustomerCreationViewModel : ViewModel() {
    private val repo = Graph.customerRepository

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer = _customer.asStateFlow()

    var saving by mutableStateOf(false)
        private set

    fun load(id: String) {
        viewModelScope.launch {
            _customer.value = repo.byId(id)
        }
    }

    fun save(customer: Customer, photoUri: Uri?, onDone: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            val repId = Graph.authRepository.currentUser()?.uid ?: ""
            repo.save(customer.copy(createdBy = repId), photoUri)
            saving = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCreationScreen(
    onDone: () -> Unit,
    customerId: String? = null,
    vm: CustomerCreationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScopeSafe()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Retailer") }
    var phone by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<LatLng?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val loadedCustomer by vm.customer.collectAsStateSafe()

    LaunchedEffect(customerId) {
        customerId?.let { vm.load(it) }
    }

    LaunchedEffect(loadedCustomer) {
        loadedCustomer?.let { c ->
            name = c.name
            type = c.type
            phone = c.phone
            contact = c.contactPerson
            location = LatLng(c.latitude, c.longitude)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch { location = LocationProvider(context).current() }
    }

    Retail360Scaffold(
        title = "New customer",
        onBack = onDone
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("Business name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(type, { type = it }, label = { Text("Type (Retailer/Wholesaler/Kiosk)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(contact, { contact = it }, label = { Text("Contact person") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            OutlinedButton(
                onClick = { locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(location?.let { "Location set (%.5f, %.5f)".format(it.lat, it.lng) }
                    ?: "Capture GPS location")
            }

            OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (photoUri != null) "Photo selected" else "Add storefront photo")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val customer = loadedCustomer?.copy(
                        name = name.trim(), type = type.trim(), phone = phone.trim(),
                        contactPerson = contact.trim(),
                        latitude = location?.lat ?: 0.0, longitude = location?.lng ?: 0.0
                    ) ?: Customer(
                        name = name.trim(), type = type.trim(), phone = phone.trim(),
                        contactPerson = contact.trim(),
                        latitude = location?.lat ?: 0.0, longitude = location?.lng ?: 0.0
                    )
                    vm.save(customer, photoUri, onDone)
                },
                enabled = name.isNotBlank() && !vm.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save customer")
                }
            }
        }
    }
}

// Local helper so we don't repeat the scope import boilerplate in the composable.
@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()