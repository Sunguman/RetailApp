package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.Product
import com.example.retail360.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.retail360.ui.theme.Retail360Theme

class ProductScanViewModel : ViewModel() {
    private val repo = Graph.productRepository; private val _result = MutableStateFlow<Product?>(null); val result = _result.asStateFlow(); private val _notFound = MutableStateFlow(false); val notFound = _notFound.asStateFlow()
    fun onBarcode(barcode: String) { viewModelScope.launch { val product = repo.getByBarcode(barcode.trim()); _result.value = product; _notFound.value = product == null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScanScreen(visitId: String, onBack: () -> Unit, vm: ProductScanViewModel = viewModel()) {
    val product by vm.result.collectAsStateSafe(); val notFound by vm.notFound.collectAsStateSafe(); var code by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Scan product") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        ProductScanContent(code, { code = it }, { vm.onBarcode(code) }, product, notFound, Modifier.padding(padding))
    }
}

@Composable
fun ProductScanContent(code: String, onCodeChange: (String) -> Unit, onLookup: () -> Unit, product: Product?, notFound: Boolean, modifier: Modifier = Modifier) {
    Column(Modifier.fillMaxSize().then(modifier).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = code, onValueChange = onCodeChange, label = { Text("Barcode") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = onLookup, modifier = Modifier.fillMaxWidth()) { Text("Look up") }
        product?.let { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(p.name, style = MaterialTheme.typography.titleMedium); Text("${p.category} . SKU ${p.sku}"); Spacer(Modifier.height(4.dp)); Text(p.price.ksh(), style = MaterialTheme.typography.titleLarge) } } }
        if (notFound) Text("Not found.", color = MaterialTheme.colorScheme.error)
    }
}

@Preview(showBackground = true)
@Composable
fun ProductScanPreview() { Retail360Theme { ProductScanContent("123", {}, {}, Product(name = "Soda", price = 100.0), false) } }
