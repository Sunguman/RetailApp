package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.repository.ProductStock
import com.example.retail360.data.repository.StockControlRepository
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.ui.components.SearchableDropdown
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SellViewModel : ViewModel() {
    private val vid = MutableStateFlow("")

    /** 
     * Reps sell from their global Van Stock (all approved movements minus all sales).
     * This ensures stock loaded in a previous visit or via global requisition is available.
     */
    val sellable = Graph.stockControlRepository.observeVanStock()
        .map { list -> list.filter { it.available > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ProductStock>())

    fun bind(visitId: String) { vid.value = visitId }

    fun record(stock: ProductStock, quantity: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            Graph.visitRepository.addSale(
                SaleItem(
                    visitId = vid.value, productId = stock.productId,
                    productName = stock.productName, quantity = quantity, unitPrice = stock.unitPrice
                )
            )
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: SellViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val sellable by vm.sellable.collectAsStateSafe()

    var productName by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    val stock: ProductStock? = sellable.firstOrNull { it.productName == productName }
    val qtyN = qty.toIntOrNull() ?: 0
    val available = stock?.available ?: 0
    val insufficient = stock != null && qtyN > available
    val canSell = stock != null && qtyN > 0 && !insufficient

    Retail360Scaffold(
        title = "Sell",
        onBack = onBack
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchableDropdown("Product", sellable.map { it.productName }, productName,
                onSelect = { productName = it; qty = "" })

            if (stock != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        InfoLine("Available Stock", "${stock.available} ${stock.unit}")
                        InfoLine("Unit", stock.unit)
                        InfoLine("Selling Price", "KES %,.2f".format(stock.unitPrice))
                    }
                }
            }

            OutlinedTextField(
                qty, { qty = it.filter(Char::isDigit) }, singleLine = true,
                label = { Text("Sale Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (insufficient) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Insufficient Stock", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Only $available ${stock?.unit ?: "PCS"} are available for this product.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Text("Total: KES %,.2f".format(qtyN * (stock?.unitPrice ?: 0.0)),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { stock?.let { vm.record(it, qtyN) { productName = ""; qty = "" } } },
                enabled = canSell, modifier = Modifier.fillMaxWidth()
            ) { Text("Record Sale") }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { Text("Confirm & Finish Visit Task") }

            if (sellable.isEmpty()) {
                HorizontalDivider()
                Text("No stock available in van. Load stock (Uplift) or get Requisition approval first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
