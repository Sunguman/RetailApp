package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.Visit
import com.example.retail360.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.retail360.ui.theme.Retail360Theme

data class VisitSummaryData(val visit: Visit? = null, val durationMin: Long = 0, val salesTotal: Double = 0.0, val synced: Boolean = false)
class VisitSummaryViewModel : ViewModel() {
    private val _summary = MutableStateFlow(VisitSummaryData()); val summary = _summary.asStateFlow()
    fun load(visitId: String) { viewModelScope.launch { val visit = Graph.visitRepository.byId(visitId) ?: return@launch; val total = Graph.visitRepository.salesTotal(visitId); val duration = ((visit.checkOutTime ?: visit.checkInTime) - visit.checkInTime) / 60000; _summary.value = VisitSummaryData(visit, duration, total, visit.synced) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitSummaryScreen(visitId: String, onDone: () -> Unit, vm: VisitSummaryViewModel = viewModel()) {
    LaunchedEffect(visitId) { vm.load(visitId) }; val s by vm.summary.collectAsStateSafe()
    Scaffold(topBar = { TopAppBar(title = { Text("Visit summary") }) }) { padding -> VisitSummaryContent(s, onDone, Modifier.padding(padding)) }
}

@Composable
fun VisitSummaryContent(summary: VisitSummaryData, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(Modifier.fillMaxSize().then(modifier).padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { SummaryRow("Duration", "${summary.durationMin} min"); SummaryRow("Sales total", summary.salesTotal.ksh()); SummaryRow("Status", summary.visit?.status ?: "-"); SummaryRow("Sync", if (summary.synced) "Synced" else "Pending") } }
        Spacer(Modifier.height(24.dp)); Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) { Column(Modifier.padding(vertical = 6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleMedium) } }

@Preview(showBackground = true)
@Composable
fun VisitSummaryPreview() { Retail360Theme { VisitSummaryContent(VisitSummaryData(durationMin = 30, salesTotal = 500.0), {}) } }
