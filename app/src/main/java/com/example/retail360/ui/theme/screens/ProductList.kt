package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.Product
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import com.example.retail360.util.ksh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProductListViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val products: StateFlow<List<Product>> = combine(
        Graph.productRepository.observeAll(),
        _query
    ) { all, q -> all to q }
        .flowOn(Dispatchers.Default)
        .map { (all, q) ->
            if (q.isBlank()) all
            else all.filter {
                it.name.contains(q, ignoreCase = true) || 
                it.sku.contains(q, ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    vm: ProductListViewModel = viewModel()
) {
    val items by vm.products.collectAsStateSafe()
    val query by vm.query.collectAsStateSafe()

    Retail360Scaffold(
        title = "Products",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, 
                onValueChange = { vm.setQuery(it) },
                label = { Text("Search catalog") }, 
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("${p.category} · SKU ${p.sku}") },
                        trailingContent = { Text(p.price.ksh(), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        modifier = Modifier.clickable { /* Detail? */ }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
