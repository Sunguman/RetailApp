package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.repository.ProductStock
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class InventoryViewModel : ViewModel() {
    val stock = Graph.stockControlRepository.observeVanStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ProductStock>())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onAddStock: () -> Unit,
    vm: InventoryViewModel = viewModel()
) {
    val stock by vm.stock.collectAsStateSafe()
    val available = stock.sumOf { it.available.coerceAtLeast(0) }
    val pending = stock.sumOf { it.pendingReq }
    val sold = stock.sumOf { it.sold }
    val returned = stock.sumOf { it.returned }

    Retail360Scaffold(
        title = "Inventory",
        onBack = onBack,
        actions = {
            IconButton(onClick = onAddProduct) {
                Icon(Icons.Filled.LibraryAdd, contentDescription = "New product")
            }
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = onAddStock) {
                Icon(Icons.Filled.Add, contentDescription = "Add stock")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Van stock summary
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("VAN STOCK", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Cell("Available", available.toString(), Modifier.weight(1f))
                        Cell("Pending", pending.toString(), Modifier.weight(1f))
                        Cell("Sold", sold.toString(), Modifier.weight(1f))
                        Cell("Returned", returned.toString(), Modifier.weight(1f))
                    }
                }
            }
            Button(onClick = onAddStock, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Add Stock  (Requisition · Uplift · Return)")
            }
            HorizontalDivider()

            if (stock.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No stock yet. Tap Add Stock to requisition or uplift.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(stock, key = { it.productId }) { p -> StockRow(p) }
                }
            }
        }
    }
}

@Composable
private fun Cell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun StockRow(p: ProductStock) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.productName, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${p.available.coerceAtLeast(0)} ${p.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        val bits = buildList {
            if (p.pendingReq > 0) add("Pending ${p.pendingReq}")
            if (p.sold > 0) add("Sold ${p.sold}")
            if (p.returned > 0) add("Returned ${p.returned}")
        }
        if (bits.isNotEmpty())
            Text(bits.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = 12.dp))
    }
}
