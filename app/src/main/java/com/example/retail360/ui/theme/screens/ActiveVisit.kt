package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.Customer
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.Visit
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import com.example.retail360.util.ksh
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActiveVisitViewModel : ViewModel() {
    private val _visit = MutableStateFlow<Visit?>(null)
    val visit = _visit.asStateFlow()
    private val _customer = MutableStateFlow<Customer?>(null)
    val customer = _customer.asStateFlow()
    private val _sales = MutableStateFlow<List<SaleItem>>(emptyList())
    val sales = _sales.asStateFlow()
    private val _availability = MutableStateFlow<List<AvailabilityRecord>>(emptyList())
    val availability = _availability.asStateFlow()

    fun load(visitId: String) {
        viewModelScope.launch {
            val v = Graph.visitRepository.byId(visitId)
            _visit.value = v
            v?.let { _customer.value = Graph.customerRepository.byId(it.customerId) }
        }
        viewModelScope.launch {
            Graph.visitRepository.observeSales(visitId).collect { _sales.value = it }
        }
        viewModelScope.launch {
            Graph.visitRepository.observeAvailability(visitId).collect { _availability.value = it }
        }
    }
}

private data class Activity(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveVisitScreen(
    visitId: String,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCompetitor: () -> Unit,
    onPayments: () -> Unit,
    onProductUpdates: () -> Unit,
    onShareOfShelf: () -> Unit,
    onPhotos: () -> Unit,
    onAvailability: () -> Unit,
    onSales: () -> Unit,
    onCheckOut: () -> Unit,
    vm: ActiveVisitViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.load(visitId) }
    val visit by vm.visit.collectAsStateSafe()
    val customer by vm.customer.collectAsStateSafe()
    val sales by vm.sales.collectAsStateSafe()
    val availability by vm.availability.collectAsStateSafe()

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }

    var elapsed by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(visit) {
        val start = visit?.checkInTime ?: return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            elapsed = now - start
            delay(1000)
        }
    }

    val activities = listOf(
        Activity("Sell", Icons.Filled.ShoppingCart, true, onSales),
        Activity("Payments", Icons.Filled.Payments, true, onPayments),
        Activity("On-shelf availability", Icons.Filled.Checklist, true, onAvailability),
        Activity("Stock taking / scan", Icons.Filled.QrCodeScanner, true, onScan),
        Activity("Competitor activity", Icons.Filled.CompareArrows, true, onCompetitor),
        Activity("Share of shelf", Icons.Filled.ViewWeek, true, onShareOfShelf),
        Activity("Product updates", Icons.Filled.Refresh, true, onProductUpdates),
        Activity("Photos", Icons.Filled.PhotoCamera, true, onPhotos)
    )

    Retail360Scaffold(
        title = customer?.name ?: "Active visit",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ---- Blue timer header ----
            Surface(color = MaterialTheme.colorScheme.primary) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    visit?.let {
                        Text("Check-in: ${clockFormat.format(Date(it.checkInTime))}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(formatElapsed(elapsed), style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stampFormat.format(Date(now)), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onCheckOut,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Check out") }
                }
            }

            // ---- Tabs ----
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Activities") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Info") })
                Tab(tab == 2, { tab = 2 }, text = { Text("Insights") })
            }

            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> ActivitiesGrid(activities)
                    1 -> InfoTab(customer, visit)
                    else -> InsightsTab(sales, availability)
                }
            }
        }
    }
}

@Composable
private fun ActivitiesGrid(activities: List<Activity>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(activities, key = { it.label }) { a -> ActivityTile(a) }
    }
}

@Composable
private fun ActivityTile(a: Activity) {
    Card(
        onClick = a.onClick,
        enabled = a.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f), // Slightly taller than square
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = if (a.enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        a.icon, 
                        contentDescription = null,
                        tint = if (a.enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = a.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoTab(customer: Customer?, visit: Visit?) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoLine("Customer", customer?.name ?: "—")
        InfoLine("Category", customer?.type ?: "—")
        InfoLine("Phone", customer?.phone?.ifBlank { "—" } ?: "—")
        InfoLine("Check-in", visit?.let { stampFormat.format(Date(it.checkInTime)) } ?: "—")
        InfoLine("Status", visit?.status ?: "—")
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InsightsTab(sales: List<SaleItem>, availability: List<AvailabilityRecord>) {
    val total = sales.sumOf { it.lineTotal }
    val units = sales.sumOf { it.quantity }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StatBig("Sales so far", total.ksh())
        StatBig("Units sold", units.toString())
        StatBig("Lines recorded", sales.size.toString())
        StatBig("Availability checks", availability.size.toString())
    }
}

@Composable
private fun StatBig(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val clockFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
private val stampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02dhrs %02dmin %02dsec".format(h, m, s)
}
