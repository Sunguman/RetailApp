package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.retail360.data.model.Customer
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CheckInCheckoutViewModel : ViewModel() {
    private val repo = Graph.customerRepository

    val customers: StateFlow<List<Customer>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.refreshFromServer() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInCheckoutScreen(
    onBack: () -> Unit,
    onCheckIn: (String) -> Unit,
    vm: CheckInCheckoutViewModel = viewModel()
) {
    val all by vm.customers.collectAsStateSafe()
    var query by remember { mutableStateOf("") }
    val filtered = remember(all, query) {
        if (query.isBlank()) all
        else all.filter { it.name.contains(query, ignoreCase = true) }
    }

    Retail360Scaffold(
        title = "Check-in / Checkout",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Search outlets") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(c.type) },
                        modifier = Modifier.clickable { onCheckIn(c.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
