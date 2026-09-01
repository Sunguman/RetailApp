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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.retail360.data.model.Product
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.StockMovement
import com.example.retail360.data.repository.ProductStock
import com.example.retail360.data.repository.Stock
import com.example.retail360.data.repository.StockControlRepository
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.ui.components.SearchableDropdown
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val UNITS = listOf("PIECE", "CARTON", "DOZEN", "PACK")

/** A staged line before submission. */
data class StockLine(
    val stockPoint: String, val category: String,
    val productId: String, val productName: String,
    val unit: String, val quantity: Int, val unitPrice: Double
) { val total: Double get() = quantity * unitPrice }

@OptIn(ExperimentalCoroutinesApi::class)
class AddStockViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""
    var saving by mutableStateOf(false); private set

    val products = Graph.productRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Product>())

    // Returns operate on the rep's current van stock (products with available > 0).
    val available = Graph.stockControlRepository.observeVanStock()
        .map { list -> list.filter { it.available > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<ProductStock>())

    val stockPoints = Graph.stockControlRepository.stockPoints()

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun categories(): List<String> =
        products.value.map { it.category.ifBlank { "Uncategorised" } }.distinct().sorted()

    fun productsIn(category: String): List<Product> =
        products.value.filter { it.category.ifBlank { "Uncategorised" } == category }.sortedBy { it.name }

    fun submit(type: String, lines: List<StockLine>, onDone: () -> Unit) {
        saving = true
        viewModelScope.launch {
            val status = if (type == Stock.REQUISITION) Stock.PENDING else Stock.CONFIRMED
            Graph.stockControlRepository.submit(
                lines.map { l ->
                    StockMovement(
                        visitId = vid.value, customerId = customerId, repId = repId,
                        stockPoint = l.stockPoint, category = l.category,
                        productId = l.productId, productName = l.productName, unit = l.unit,
                        movementType = type, status = status,
                        quantity = l.quantity, unitPrice = l.unitPrice
                    )
                }
            )
            saving = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    visitId: String,
    onDone: () -> Unit,
    vm: AddStockViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    vm.products.collectAsStateSafe()          // keep catalog warm for dropdowns
    val available by vm.available.collectAsStateSafe()

    var type by remember { mutableStateOf(Stock.REQUISITION) }
    val lines = remember { mutableStateListOf<StockLine>() }

    // current selection
    var stockPoint by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    val isReturn = type == Stock.RETURN
    val selectedProduct = vm.productsIn(category).firstOrNull { it.name == productName }
    val returnStock: ProductStock? = available.firstOrNull { it.productName == productName }
    val unitPrice = if (isReturn) returnStock?.unitPrice ?: 0.0 else selectedProduct?.price ?: 0.0
    val qtyN = qty.toIntOrNull() ?: 0
    val maxReturn = returnStock?.available ?: 0

    val canAdd = qtyN > 0 && productName.isNotBlank() &&
            (if (isReturn) qtyN <= maxReturn else stockPoint.isNotBlank() && unit.isNotBlank())

    fun clearSelection() { category = ""; productName = ""; unit = ""; qty = "" }
    fun switchType(t: String) { type = t; clearSelection(); stockPoint = ""; lines.clear() }

    Retail360Scaffold(
        title = "Add Stock",
        onBack = onDone
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Movement type
            Text("Stock movement type", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth()) {
                listOf(Stock.REQUISITION to "Requisition", Stock.UPLIFT to "Uplifts", Stock.RETURN to "Returns")
                    .forEach { (value, label) ->
                        Row(
                            Modifier.weight(1f).selectable(type == value, onClick = { switchType(value) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = type == value, onClick = { switchType(value) })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
            }

            if (isReturn) {
                SearchableDropdown("Product", available.map { it.productName }, productName,
                    onSelect = { productName = it; qty = "" })
                if (returnStock != null)
                    Text("Available Stock: ${returnStock.available} ${returnStock.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
            } else {
                SearchableDropdown("Stock Point", vm.stockPoints, stockPoint,
                    onSelect = { stockPoint = it; category = ""; productName = ""; unit = "" })
                SearchableDropdown("Order Category", vm.categories(), category,
                    onSelect = { category = it; productName = ""; unit = "" },
                    enabled = stockPoint.isNotBlank())
                SearchableDropdown("Product", vm.productsIn(category).map { it.name }, productName,
                    onSelect = { productName = it; unit = "" },
                    enabled = category.isNotBlank())
                SearchableDropdown("Unit", UNITS, unit,
                    onSelect = { unit = it }, enabled = productName.isNotBlank())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    qty, { qty = it.filter(Char::isDigit) }, singleLine = true,
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "KES %,.2f".format(unitPrice), onValueChange = {}, readOnly = true,
                    label = { Text("Unit Price") }, modifier = Modifier.weight(1f)
                )
            }
            if (isReturn && productName.isNotBlank() && qtyN > 0)
                Text("Remaining after return: ${(maxReturn - qtyN).coerceAtLeast(0)} ${returnStock?.unit ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (qtyN > maxReturn) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Total: KES %,.2f".format(qtyN * unitPrice),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedButton(
                onClick = {
                    val p = if (isReturn) returnStock else selectedProduct
                    val pid = (p as? Product)?.id ?: (p as? ProductStock)?.productId ?: ""
                    lines.add(
                        StockLine(
                            stockPoint = stockPoint, category = category,
                            productId = pid, productName = productName,
                            unit = if (isReturn) (returnStock?.unit ?: "PIECE") else unit,
                            quantity = qtyN, unitPrice = unitPrice
                        )
                    )
                    clearSelection()
                },
                enabled = canAdd, modifier = Modifier.fillMaxWidth()
            ) { Text("+ Add to List") }

            if (lines.isNotEmpty()) {
                HorizontalDivider()
                lines.forEachIndexed { i, l ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(l.productName, style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                            Text("${l.quantity} ${l.unit} × KES %,.2f = KES %,.2f".format(l.unitPrice, l.total),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { lines.removeAt(i) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }
                HorizontalDivider()
                Text("Overall total: KES %,.2f".format(lines.sumOf { it.total }),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    vm.submit(type, lines.toList()) { onDone() }
                },
                enabled = lines.isNotEmpty() && !vm.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (type) {
                        Stock.REQUISITION -> "Submit Requisition"
                        Stock.UPLIFT -> "Confirm Uplift"
                        else -> "Confirm Return"
                    }
                )
            }
            if (type == Stock.REQUISITION)
                Text("Requisitions are submitted for approval and become sellable once approved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
