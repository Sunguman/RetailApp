package com.example.retail360.ui.theme.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.model.Visit
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.ksh
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VisitSummary(
    val visit: Visit? = null,
    val durationMin: Long = 0,
    val salesTotal: Double = 0.0,
    val synced: Boolean = false
)

class VisitSummaryViewModel : ViewModel() {
    private val _summary = MutableStateFlow(VisitSummary())
    val summary = _summary.asStateFlow()

    fun load(visitId: String) {
        viewModelScope.launch {
            val visit = Graph.visitRepository.byId(visitId) ?: return@launch
            val total = Graph.visitRepository.salesTotal(visitId)
            val duration = ((visit.checkOutTime ?: visit.checkInTime) - visit.checkInTime) / 60000
            _summary.value = VisitSummary(visit, duration, total, visit.synced)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitSummaryScreen(
    visitId: String,
    onDone: () -> Unit,
    vm: VisitSummaryViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.load(visitId) }
    val s by vm.summary.collectAsStateSafe()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Visit summary") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SummaryRow("Duration", "${s.durationMin} min")
                    SummaryRow("Sales total", s.salesTotal.ksh())
                    SummaryRow("Status", s.visit?.status ?: "-")
                    SummaryRow("Sync", if (s.synced) "Synced" else "Pending (offline queue)")
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}