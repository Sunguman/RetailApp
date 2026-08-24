package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.Product
import com.example.retail360.model.SaleItem
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.ksh
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel : ViewModel() {
    val products: StateFlow<List<Product>> = Graph.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<SaleItem>>(emptyList())
    val cart = _cart.asStateFlow()

    fun bind(visitId: String) {
        viewModelScope.launch {
            Graph.visitRepository.observeSales(visitId).collect { _cart.value = it }
        }
    }

    fun add(visitId: String, product: Product) {
        viewModelScope.launch {
            // One line per product: if present, bump qty; else insert.
            val existing = _cart.value.firstOrNull { it.productId == product.id }
            val item = existing?.copy(quantity = existing.quantity + 1)
                ?: SaleItem(
                    id = "$visitId-${product.id}",
                    visitId = visitId, productId = product.id, productName = product.name,
                    quantity = 1, unitPrice = product.price
                )
            Graph.visitRepository.addSale(item)
        }
    }

    fun remove(id: String) = viewModelScope.launch { Graph.visitRepository.removeSale(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: SalesViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val products by vm.products.collectAsStateSafe()
    val cart by vm.cart.collectAsStateSafe()
    val total = cart.sumOf { it.lineTotal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Catalog", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(products, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text(p.price.ksh()) },
                        trailingContent = {
                            IconButton(onClick = { vm.add(visitId, p) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    )
                }
            }

            Divider()
            Text("Cart", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(cart, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text("${item.productName} ×${item.quantity}") },
                        supportingContent = { Text(item.lineTotal.ksh()) },
                        trailingContent = {
                            IconButton(onClick = { vm.remove(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text(total.ksh(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
