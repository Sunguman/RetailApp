package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.Product
import com.example.retail360.ui.components.AppFooter
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import com.example.retail360.util.LatLng
import com.example.retail360.util.LocationProvider
import com.example.retail360.util.collectAsStateSafe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Business statuses. Value is stored; label is shown.
enum class OsaStatus(val label: String) {
    AVAILABLE("Available"),
    INSUFFICIENT("Insufficient"),
    NOT_AVAILABLE("Not available")
}

data class CategoryGroup(val name: String, val products: List<Product>)

@OptIn(ExperimentalCoroutinesApi::class)
class AvailabilityViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    var outletName by mutableStateOf("")
        private set
    private var customerId = ""
    private var loc: LatLng? = null

    /** Catalog grouped into categories (SKUs come from the backend catalog — no code change to add more). */
    val categories = Graph.productRepository.observeAll()
        .map { list ->
            list.groupBy { it.category.ifBlank { "Uncategorised" } }
                .toSortedMap()
                .map { (name, products) -> CategoryGroup(name, products.sortedBy { it.name }) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** productId -> stored status, so selections survive paging and app restarts. */
    val responses = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else Graph.visitRepository.observeAvailability(it)
    }.map { recs -> recs.filter { it.status.isNotBlank() }.associate { it.productId to it.status } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun bind(visitId: String, context: Context) {
        vid.value = visitId
        viewModelScope.launch {
            val v = Graph.visitRepository.byId(visitId)
            customerId = v?.customerId ?: ""
            outletName = customerId.takeIf { it.isNotBlank() }
                ?.let { Graph.customerRepository.byId(it)?.name } ?: ""
            if (hasLoc(context)) loc = LocationProvider(context).current()
        }
    }

    fun setStatus(product: Product, status: OsaStatus) {
        viewModelScope.launch {
            Graph.visitRepository.saveAvailability(
                AvailabilityRecord(
                    id = "${vid.value}-${product.id}",
                    visitId = vid.value, customerId = customerId, outletName = outletName,
                    repId = repId, category = product.category, productId = product.id,
                    productName = product.name, status = status.name,
                    lat = loc?.lat ?: 0.0, lng = loc?.lng ?: 0.0
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: AvailabilityViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(visitId) { vm.bind(visitId, context) }
    val categories by vm.categories.collectAsStateSafe()
    val responses by vm.responses.collectAsStateSafe()

    var page by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }

    val totalSkus = categories.sumOf { it.products.size }
    val overallDone = responses.size

    Retail360Scaffold(
        title = "Availability",
        onBack = onBack
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                categories.isEmpty() ->
                    EmptyOsa()
                completed ->
                    CompletedOsa(overallDone, totalSkus, onBack)
                else -> {
                    if (categories.isNotEmpty()) {
                        val group = categories[page.coerceIn(0, categories.lastIndex)]
                        val catDone = group.products.count { responses.containsKey(it.id) }
                        val allAnswered = catDone == group.products.size

                        Column(Modifier.fillMaxSize()) {
                            // Category header + counters
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${group.name.uppercase()}  ($catDone / ${group.products.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                                    )
                                    Text("$overallDone / $totalSkus",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalSkus == 0) 0f else overallDone.toFloat() / totalSkus },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // SKU list
                            LazyColumn(Modifier.weight(1f)) {
                                items(group.products, key = { it.id }) { product ->
                                    val selected = responses[product.id]
                                    val missing = showErrors && selected == null
                                    SkuRow(product, selected, missing) { vm.setStatus(product, it) }
                                }
                            }

                            // Pager + Next
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${page + 1} / ${categories.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f))
                                Button(onClick = {
                                    if (!allAnswered) {
                                        showErrors = true
                                    } else {
                                        showErrors = false
                                        if (page < categories.lastIndex) page++ else completed = true
                                    }
                                }) { Text(if (page < categories.lastIndex) "Next" else "Complete OSA") }
                            }
                            if (showErrors && !allAnswered) {
                                Text(
                                    "Please assess every product on this page before continuing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkuRow(
    product: Product,
    selected: String?,
    missing: Boolean,
    onSelect: (OsaStatus) -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            product.name,
            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
            color = if (missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Row {
            OsaStatus.entries.forEach { status ->
                Row(
                    Modifier.weight(1f).clickable { onSelect(status) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == status.name,
                        onClick = { onSelect(status) }
                    )
                    Text(status.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth().height(0.5.dp)) {}
    }
}

@Composable
private fun EmptyOsa() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No products configured for this outlet.",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("Add products to the catalog to run an OSA audit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CompletedOsa(done: Int, total: Int, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        Text("OSA Completed", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("$done of $total products assessed.", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

private fun hasLoc(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
