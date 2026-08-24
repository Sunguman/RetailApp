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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.salesautomation.data.model.Customer
import com.example.salesautomation.util.Graph
import com.example.salesautomation.util.LatLng
import com.example.salesautomation.util.LocationProvider
import kotlinx.coroutines.launch

class CustomerCreationViewModel : ViewModel() {
    private val repo = Graph.customerRepository

    fun load(id: String, onLoaded: (Customer?) -> Unit) {
        viewModelScope.launch { onLoaded(repo.byId(id)) }
    }

    fun save(customer: Customer, photoUri: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            val repId = Graph.authRepository.currentUser()?.uid ?: ""
            // Preserve createdBy on edit; stamp it on create.
            val stamped = customer.copy(
                createdBy = customer.createdBy.ifBlank { repId },
                updatedAt = System.currentTimeMillis()
            )
            repo.save(stamped, photoUri)
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
    var original by remember { mutableStateOf<Customer?>(null) }

    androidx.compose.runtime.LaunchedEffect(customerId) {
        if (customerId != null && original == null) {
            vm.load(customerId) { c ->
                if (c != null) {
                    original = c
                    name = c.name; type = c.type; phone = c.phone; contact = c.contactPerson
                    if (c.latitude != 0.0 || c.longitude != 0.0) {
                        location = LatLng(c.latitude, c.longitude)
                    }
                }
            }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customerId == null) "New customer" else "Edit customer") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                    val base = original ?: Customer()
                    val customer = base.copy(
                        name = name.trim(), type = type.trim(), phone = phone.trim(),
                        contactPerson = contact.trim(),
                        latitude = location?.lat ?: base.latitude,
                        longitude = location?.lng ?: base.longitude
                    )
                    vm.save(customer, photoUri, onDone)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (customerId == null) "Save customer" else "Update customer") }
        }
    }
}

// Local helper so we don't repeat the scope import boilerplate in the composable.
@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
