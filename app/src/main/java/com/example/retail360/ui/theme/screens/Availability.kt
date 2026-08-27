package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.Product
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AvailabilityViewModel : ViewModel() {
    val products: StateFlow<List<Product>> = Graph.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(visitId: String, product: Product, inStock: Boolean, facings: Int, photo: Uri?) {
        viewModelScope.launch {
            val record = AvailabilityRecord(
                visitId = visitId, productId = product.id, productName = product.name,
                inStock = inStock, facings = facings
            )
            Graph.visitRepository.saveAvailability(record, photo)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: AvailabilityViewModel = viewModel()
) {
    val products by vm.products.collectAsStateSafe()

    Retail360Scaffold(
        title = "Availability",
        onBack = onBack
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(products, key = { it.id }) { product ->
                AvailabilityRow(product) { inStock, facings, photo ->
                    vm.save(visitId, product, inStock, facings, photo)
                }
            }
        }
    }
}

@Composable
private fun AvailabilityRow(
    product: Product,
    onSave: (Boolean, Int, Uri?) -> Unit
) {
    var inStock by remember { mutableStateOf(false) }
    var facings by remember { mutableStateOf("0") }
    var photo by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("In stock")
                Switch(
                    checked = inStock,
                    onCheckedChange = { inStock = it; onSave(it, facings.toIntOrNull() ?: 0, photo) }
                )
                OutlinedTextField(
                    value = facings,
                    onValueChange = {
                        facings = it.filter(Char::isDigit)
                        onSave(inStock, facings.toIntOrNull() ?: 0, photo)
                    },
                    label = { Text("Facings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.padding(start = 12.dp)
                )
                IconButton(onClick = { picker.launch("image/*") }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Shelf photo")
                }
            }
            if (photo != null) Text("Shelf photo attached", style = MaterialTheme.typography.bodySmall)
        }
    }
}