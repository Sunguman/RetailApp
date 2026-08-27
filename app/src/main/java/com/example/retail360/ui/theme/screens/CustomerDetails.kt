package com.example.retail360.ui.theme.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.example.retail360.ui.components.brandedTopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.Customer
import com.example.retail360.data.model.Visit
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomerDetailsViewModel : ViewModel() {
    private val customerRepo = Graph.customerRepository
    private val visitRepo = Graph.visitRepository

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer = _customer.asStateFlow()

    private val _visits = MutableStateFlow<List<Visit>>(emptyList())
    val visits = _visits.asStateFlow()

    fun load(customerId: String) {
        viewModelScope.launch { _customer.value = customerRepo.byId(customerId) }
        viewModelScope.launch {
            visitRepo.observeForCustomer(customerId).collect { _visits.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onBack: () -> Unit,
    onCheckIn: () -> Unit,
    onEdit: () -> Unit,
    vm: CustomerDetailsViewModel = viewModel()
) {
    LaunchedEffect(customerId) { vm.load(customerId) }
    val customer by vm.customer.collectAsStateSafe()
    val visits by vm.visits.collectAsStateSafe()

    var tab by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    Retail360Scaffold(
        title = customer?.name ?: "Outlet",
        onBack = onBack,
        actions = {
            IconButton(onClick = { customer?.id?.let(vm::load) }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; onEdit() }
                    )
                }
            }
        },
        floatingActionButton = {
            if (customer != null) {
                ExtendedFloatingActionButton(
                    onClick = onCheckIn,
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    text = { Text("Check in") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color = MaterialTheme.colorScheme.primary) {
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("Info") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("Insights") })
                    Tab(tab == 2, { tab = 2 }, text = { Text("Products") })
                }
            }

            val c = customer
            if (c == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
            } else {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> InfoTab(c, visits)
                        1 -> InsightsTab(visits)
                        else -> ProductsTab()
                    }
                }
            }
        }
    }
}

/* ---------------- Info tab ---------------- */

@Composable
private fun InfoTab(c: Customer, visits: List<Visit>) {
    val lastVisit = visits.filter { it.status == "COMPLETED" }.maxByOrNull { it.checkInTime }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DetailRow(Icons.Filled.Store, "Category", c.type)
        if (c.phone.isNotBlank()) DetailRow(Icons.Filled.Phone, "Phone", c.phone)
        if (c.contactPerson.isNotBlank()) DetailRow(Icons.Filled.Person, "Contact person", c.contactPerson)
        DetailRow(
            Icons.Filled.LocationOn, "Location",
            if (c.latitude != 0.0 || c.longitude != 0.0)
                "%.5f, %.5f · geofence ${c.geofenceRadiusM} m".format(c.latitude, c.longitude)
            else "Not captured"
        )
        DetailRow(Icons.Filled.Payments, "Balance", "KES 0.00") // TODO: wire finance balance
        DetailRow(
            if (c.synced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
            "Sync status", if (c.synced) "Synced" else "Pending upload"
        )
        DetailRow(Icons.Filled.Event, "Last updated", dateFormat.format(Date(c.updatedAt)))
        DetailRow(
            Icons.Filled.History, "Last visit",
            lastVisit?.let { daysAgo(it.checkInTime) } ?: "No visits yet"
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

/* ---------------- Insights tab ---------------- */

@Composable
private fun InsightsTab(visits: List<Visit>) {
    val completed = visits.filter { it.status == "COMPLETED" }
    val startOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis
    val thisMonth = completed.count { it.checkInTime >= startOfMonth }
    val last = completed.maxByOrNull { it.checkInTime }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatLine("Total visits", visits.size.toString())
        StatLine("Completed", completed.size.toString())
        StatLine("This month", thisMonth.toString())
        StatLine("Last visit", last?.let { daysAgo(it.checkInTime) } ?: "—")
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider()
}

/* ---------------- Products tab ---------------- */

@Composable
private fun ProductsTab() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Product intelligence for this outlet — availability and sales trends — coming soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ---------------- helpers ---------------- */

private val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

private fun daysAgo(ts: Long): String {
    val days = ((System.currentTimeMillis() - ts) / (1000L * 60 * 60 * 24)).toInt()
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Yesterday"
        else -> "$days days ago"
    }
}
