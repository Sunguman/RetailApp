package com.example.retail360.ui.theme.screens


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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.Product
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.ksh
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductScanViewModel : ViewModel() {
    private val repo = Graph.productRepository

    private val _result = MutableStateFlow<Product?>(null)
    val result = _result.asStateFlow()
    private val _notFound = MutableStateFlow(false)
    val notFound = _notFound.asStateFlow()

    /** Called by manual entry now, and by the ML Kit analyzer once wired up. */
    fun onBarcode(barcode: String) {
        viewModelScope.launch {
            val product = repo.getByBarcode(barcode.trim())
            _result.value = product
            _notFound.value = product == null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScanScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: ProductScanViewModel = viewModel()
) {
    val product by vm.result.collectAsStateSafe()
    val notFound by vm.notFound.collectAsStateSafe()
    var code by remember { mutableStateOf("") }

    Retail360Scaffold(
        title = "Scan product",
        onBack = onBack
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera scanning: attach a CameraX PreviewView + ML Kit BarcodeScanner
            // analyzer here and call vm.onBarcode(value) on each detection.
            // Manual entry below covers the flow end-to-end without the camera.
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("Enter / scan barcode") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { vm.onBarcode(code) }, modifier = Modifier.fillMaxWidth()) {
                Text("Look up")
            }

            product?.let { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text("${p.category} · SKU ${p.sku}")
                        Spacer(Modifier.height(4.dp))
                        Text(p.price.ksh(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            if (notFound) Text("No product matched that barcode.", color = MaterialTheme.colorScheme.error)
        }
    }
}