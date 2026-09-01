package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CompetitorListViewModel : ViewModel() {
    private val vid = MutableStateFlow("")
    var outletName by mutableStateOf(""); private set

    val items = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList()) else Graph.merchandisingRepository.observeCompetitor(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch {
            val v = Graph.visitRepository.byId(visitId)
            outletName = v?.customerId?.let { Graph.customerRepository.byId(it)?.name } ?: ""
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitorActivityScreen(
    visitId: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    vm: CompetitorListViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()
    var tab by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()

    val shown = if (tab == 0)
        items.filter { statusOf(it.startDate, it.endDate, it.ongoing, now) != "Completed" }
    else items

    Retail360Scaffold(
        title = "Competitor Activities",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add competitor activity")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (vm.outletName.isNotBlank())
                Text(vm.outletName, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Active") })
                Tab(tab == 1, { tab = 1 }, text = { Text("All") })
            }
            if (shown.isEmpty()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("No competitor activities yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { a -> CompetitorCard(a, now) }
                }
            }
        }
    }
}

@Composable
private fun CompetitorCard(a: CompetitorActivity, now: Long) {
    val status = statusOf(a.startDate, a.endDate, a.ongoing, now)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(a.competitor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (a.brand.isNotBlank())
                Text(a.brand, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (a.productSku.isNotBlank())
                Text(a.productSku, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(if (a.activityType == "Other") a.otherActivity else a.activityType,
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            if (a.beforePrice > 0) {
                Text("Before: KES %,.0f".format(a.beforePrice), style = MaterialTheme.typography.bodySmall)
                Text("After: KES %,.0f".format(a.afterPrice), style = MaterialTheme.typography.bodySmall)
                Text("Depth: %.0f%%".format(a.discountDepth), style = MaterialTheme.typography.bodySmall)
            }
            if (a.stockStatus.isNotBlank())
                Text("Stock: ${stockLabel(a.stockStatus)}", style = MaterialTheme.typography.bodySmall)
            Text("Duration: ${durationDays(a.startDate, a.endDate)} days",
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = statusColor(status), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    Text(status, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
                Spacer(Modifier.weight(1f))
                if (a.photoUrl.isNotBlank()) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.height(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(" Photo Evidence", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun stockLabel(v: String) = when (v) {
    "IN_STOCK" -> "In Stock"; "OUT_OF_STOCK" -> "Out of Stock"; else -> "Unknown"
}

@Composable
private fun statusColor(status: String) = when (status) {
    "Ongoing" -> MaterialTheme.colorScheme.secondary
    "Upcoming" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.outline
}
