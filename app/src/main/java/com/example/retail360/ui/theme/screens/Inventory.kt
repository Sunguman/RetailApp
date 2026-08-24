package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.salesautomation.data.model.InventoryItem
import com.example.salesautomation.data.model.Product
import com.example.salesautomation.ui.collectAsStateSafe
import com.example.salesautomation.util.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel : ViewModel() {
    val items: StateFlow<List<InventoryItem>> = Graph.inventoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalValue: StateFlow<Double> = Graph.inventoryRepository.totalValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val catalog: StateFlow<List<Product>> = Graph.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuantity(product: Product, quantity: Int) {
        viewModelScope.launch { Graph.inventoryRepository.setQuantity(product, quantity) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    vm: InventoryViewModel = viewModel()
) {
    val items by vm.items.collectAsStateSafe()
    val total by vm.totalValue.collectAsStateSafe()
    val catalog by vm.catalog.collectAsStateSafe()
    var adding by remember { mutableStateOf(false) }
    var editProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add stock")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Current stock value", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(ksh(total), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (items.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No stock recorded.", style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to load stock from the catalog.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    catalog.find { it.id == item.productId }?.let {
                                        adding = true; editProduct = it
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.bodyLarge)
                                Text("${item.quantity} @ ${ksh(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(ksh(item.value), style = MaterialTheme.typography.titleMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (adding) {
        SetStockDialog(
            catalog = catalog,
            preselected = editProduct,
            onConfirm = { product, qty -> vm.setQuantity(product, qty); adding = false; editProduct = null },
            onDismiss = { adding = false; editProduct = null }
        )
    }
}

@Composable
private fun SetStockDialog(
    catalog: List<Product>,
    preselected: Product?,
    onConfirm: (Product, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(preselected) { mutableStateOf(preselected) }
    var qty by remember(preselected) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = selected != null && qty.toIntOrNull() != null,
                onClick = { selected?.let { onConfirm(it, qty.toInt()) } }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (selected == null) "Select product" else "Set quantity") },
        text = {
            if (selected == null) {
                if (catalog.isEmpty()) {
                    Text("No products in the catalog yet.")
                } else {
                    LazyColumn {
                        items(catalog, key = { it.id }) { p ->
                            Text(
                                p.name,
                                Modifier.fillMaxWidth().clickable { selected = p }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            } else {
                Column {
                    Text(selected!!.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it.filter(Char::isDigit) },
                        label = { Text("Quantity on hand") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    )
}

private fun ksh(v: Double): String = "KES %,.2f".format(v)
