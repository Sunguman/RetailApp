package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.ProductUpdate
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.ui.components.SelectChips
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val UPDATE_TYPES = listOf("New listing", "Delisted", "Price change", "Repackaged")

@OptIn(ExperimentalCoroutinesApi::class)
class ProductUpdateViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""

    val items = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else Graph.merchandisingRepository.observeProductUpdates(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun save(product: String, type: String, detail: String, photo: Uri?, onSaved: () -> Unit) {
        viewModelScope.launch {
            Graph.merchandisingRepository.saveProductUpdate(
                ProductUpdate(
                    visitId = vid.value, customerId = customerId, repId = repId,
                    productName = product.trim(), updateType = type, detail = detail.trim()
                ),
                photo
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductUpdateScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: ProductUpdateViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()

    var product by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(UPDATE_TYPES.first()) }
    var detail by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }

    Retail360Scaffold(
        title = "Product updates",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(product, { product = it }, singleLine = true,
                    label = { Text("Product name") }, modifier = Modifier.fillMaxWidth())
                SelectChips(UPDATE_TYPES, type) { type = it }
                OutlinedTextField(detail, { detail = it }, label = { Text("Details (optional)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picker.launch("image/*") }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text(if (photo == null) "  Add photo" else "  Photo added")
                }
                Button(
                    onClick = {
                        vm.save(product, type, detail, photo) {
                            product = ""; detail = ""; photo = null; type = UPDATE_TYPES.first()
                        }
                    },
                    enabled = product.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Record") }
                
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) { Text("Finish & Go Back") }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { u ->
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${u.productName} · ${u.updateType}",
                            style = MaterialTheme.typography.titleMedium)
                        if (u.detail.isNotBlank())
                            Text(u.detail, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

