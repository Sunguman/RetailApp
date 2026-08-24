package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.Product
import com.example.retail360.model.SaleItem
import com.example.retail360.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.retail360.ui.theme.Retail360Theme

class SalesViewModel : ViewModel() {
    val products: StateFlow<List<Product>> = Graph.productRepository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _cart = MutableStateFlow<List<SaleItem>>(emptyList()); val cart = _cart.asStateFlow()
    fun bind(visitId: String) { viewModelScope.launch { Graph.visitRepository.observeSales(visitId).collect { _cart.value = it } } }
    fun add(visitId: String, product: Product) { viewModelScope.launch { val existing = _cart.value.firstOrNull { it.productId == product.id }; val item = existing?.copy(quantity = existing.quantity + 1) ?: SaleItem(id = "$visitId-${product.id}", visitId = visitId, productId = product.id, productName = product.name, quantity = 1, unitPrice = product.price); Graph.visitRepository.addSale(item) } }
    fun remove(id: String) = viewModelScope.launch { Graph.visitRepository.removeSale(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(visitId: String, onBack: () -> Unit, vm: SalesViewModel = viewModel()) {
    LaunchedEffect(visitId) { vm.bind(visitId) }; val products by vm.products.collectAsStateSafe(); val cart by vm.cart.collectAsStateSafe(); val total = cart.sumOf { it.lineTotal }
    Scaffold(topBar = { TopAppBar(title = { Text("Sales") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        SalesContent(products, cart, total, { vm.add(visitId, it) }, { vm.remove(it) }, Modifier.padding(padding))
    }
}

@Composable
fun SalesContent(products: List<Product>, cart: List<SaleItem>, total: Double, onAdd: (Product) -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.fillMaxSize().then(modifier)) {
        Text("Catalog", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) { items(products, key = { it.id }) { p -> ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text(p.price.ksh()) }, trailingContent = { IconButton(onClick = { onAdd(p) }) { Icon(Icons.Default.Add, null) } }) } }
        HorizontalDivider(); Text("Cart", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) { items(cart, key = { it.id }) { item -> ListItem(headlineContent = { Text("${item.productName} x${item.quantity}") }, supportingContent = { Text(item.lineTotal.ksh()) }, trailingContent = { IconButton(onClick = { onRemove(item.id) }) { Icon(Icons.Default.Delete, null) } }) } }
        Card(Modifier.fillMaxWidth().padding(12.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("Total", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.weight(1f)); Text(total.ksh(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
    }
}

@Preview(showBackground = true)
@Composable
fun SalesPreview() { 
    Retail360Theme { 
        SalesContent(
            products = listOf(Product(id = "1", name = "Soda", price = 100.0), Product(id = "2", name = "Water", price = 50.0)), 
            cart = listOf(SaleItem(id = "1", productName = "Soda", quantity = 2, unitPrice = 100.0)), 
            total = 200.0, 
            onAdd = {}, 
            onRemove = {}
        ) 
    } 
}
