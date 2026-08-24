package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.AvailabilityRecord
import com.example.retail360.model.Product
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.retail360.ui.theme.Retail360Theme

class AvailabilityViewModel : ViewModel() {
    val products: StateFlow<List<Product>> = Graph.productRepository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun save(visitId: String, product: Product, inStock: Boolean, facings: Int, photo: Uri?) { viewModelScope.launch { val record = AvailabilityRecord(id = "$visitId-${product.id}", visitId = visitId, productId = product.id, productName = product.name, inStock = inStock, facings = facings); Graph.visitRepository.saveAvailability(record, photo) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(visitId: String, onBack: () -> Unit, vm: AvailabilityViewModel = viewModel()) {
    val products by vm.products.collectAsStateSafe()
    Scaffold(topBar = { TopAppBar(title = { Text("Availability") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        AvailabilityContent(products = products, onSave = { product, inStock, facings, photo -> vm.save(visitId, product, inStock, facings, photo) }, modifier = Modifier.padding(padding))
    }
}

@Composable
fun AvailabilityContent(products: List<Product>, onSave: (Product, Boolean, Int, Uri?) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(Modifier.fillMaxSize().then(modifier).padding(12.dp)) { items(products, key = { it.id }) { product -> AvailabilityRow(product) { inStock, facings, photo -> onSave(product, inStock, facings, photo) } } }
}

@Composable
private fun AvailabilityRow(product: Product, onSave: (Boolean, Int, Uri?) -> Unit) {
    var inStock by remember { mutableStateOf(false) }; var facings by remember { mutableStateOf("0") }; var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("In stock"); Switch(checked = inStock, onCheckedChange = { inStock = it; onSave(it, facings.toIntOrNull() ?: 0, photo) })
                OutlinedTextField(value = facings, onValueChange = { facings = it.filter(Char::isDigit); onSave(inStock, facings.toIntOrNull() ?: 0, photo) }, label = { Text("Facings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.padding(start = 12.dp).weight(1f))
                IconButton(onClick = { picker.launch("image/*") }) { Icon(Icons.Default.CameraAlt, contentDescription = "Shelf photo") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AvailabilityPreview() { Retail360Theme { AvailabilityContent(listOf(Product(name = "Test")), { _, _, _, _ -> }) } }
