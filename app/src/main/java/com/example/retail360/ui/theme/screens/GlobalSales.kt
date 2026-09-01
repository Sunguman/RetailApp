package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.example.retail360.data.model.SaleItem
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.ksh
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GlobalSalesViewModel : ViewModel() {
    val sales: StateFlow<List<SaleItem>> = Graph.visitRepository.observeAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun GlobalSalesScreen(
    onBack: () -> Unit,
    vm: GlobalSalesViewModel = viewModel()
) {
    val items by vm.sales.collectAsStateSafe()
    val total = items.sumOf { it.lineTotal }

    Retail360Scaffold(
        title = "Global Sales Records",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lifetime Total", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(total.ksh(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            
            if (items.isEmpty()) {
                Text("No sales records found.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.productName) },
                            supportingContent = { Text("Qty: ${item.quantity} · Price: ${item.unitPrice.ksh()}") },
                            trailingContent = { Text(item.lineTotal.ksh(), fontWeight = FontWeight.Bold) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
