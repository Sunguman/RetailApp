package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.salesautomation.data.local.PlannedStop
import com.example.salesautomation.data.model.Customer
import com.example.salesautomation.ui.collectAsStateSafe
import com.example.salesautomation.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private val DAYS = listOf(
    "Mon" to Calendar.MONDAY,
    "Tue" to Calendar.TUESDAY,
    "Wed" to Calendar.WEDNESDAY,
    "Thu" to Calendar.THURSDAY,
    "Fri" to Calendar.FRIDAY,
    "Sat" to Calendar.SATURDAY,
    "Sun" to Calendar.SUNDAY
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoutePlanViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val startOfDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis

    private val _day = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    val day: StateFlow<Int> = _day

    val stops: StateFlow<List<PlannedStop>> = _day
        .flatMapLatest { Graph.routePlanRepository.observeStops(repId, it, startOfDay) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = Graph.customerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignedIds: StateFlow<Set<String>> = _day
        .flatMapLatest { Graph.routePlanRepository.observeAssignedIds(repId, it) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectDay(d: Int) { _day.value = d }

    fun toggle(customerId: String, assign: Boolean) {
        viewModelScope.launch {
            val d = _day.value
            if (assign) Graph.routePlanRepository.assign(repId, customerId, d)
            else Graph.routePlanRepository.unassign("$repId-$customerId-$d")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanScreen(
    onBack: () -> Unit,
    onCheckIn: (String) -> Unit,
    vm: RoutePlanViewModel = viewModel()
) {
    val day by vm.day.collectAsStateSafe()
    val stops by vm.stops.collectAsStateSafe()
    val customers by vm.allCustomers.collectAsStateSafe()
    val assigned by vm.assignedIds.collectAsStateSafe()
    var assigning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { assigning = true }) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Assign customers")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Day selector
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAYS.forEach { (label, cal) ->
                    DayChip(label, selected = day == cal, modifier = Modifier.weight(1f)) {
                        vm.selectDay(cal)
                    }
                }
            }

            if (stops.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No customers assigned for this day.",
                        style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to build the route.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stops, key = { it.entryId }) { stop ->
                        StopRow(stop, onClick = { if (!stop.visited) onCheckIn(stop.customerId) })
                    }
                }
            }
        }
    }

    if (assigning) {
        AssignDialog(
            customers = customers,
            assigned = assigned,
            onToggle = { id, on -> vm.toggle(id, on) },
            onDismiss = { assigning = false }
        )
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun StopRow(stop: PlannedStop, onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (stop.visited) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (stop.visited) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline
            )
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(stop.customerName, style = MaterialTheme.typography.titleMedium)
                Text(stop.customerType, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (stop.visited) "Visited" else "Pending",
                style = MaterialTheme.typography.labelMedium,
                color = if (stop.visited) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AssignDialog(
    customers: List<Customer>,
    assigned: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Assign to this day") },
        text = {
            if (customers.isEmpty()) {
                Text("No customers yet. Add customers first.")
            } else {
                LazyColumn {
                    items(customers, key = { it.id }) { c ->
                        val isOn = c.id in assigned
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggle(c.id, !isOn) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isOn, onCheckedChange = { onToggle(c.id, it) })
                            Spacer(Modifier.height(0.dp))
                            Text(c.name, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    )
}
