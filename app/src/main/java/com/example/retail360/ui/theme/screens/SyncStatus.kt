package com.example.retail360.ui.theme.screens



import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.example.retail360.ui.theme.Components.brandedTopBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.retail360.data.SyncWorker
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PendingCounts(
    val customers: Int = 0,
    val visits: Int = 0,
    val availability: Int = 0,
    val sales: Int = 0
) {
    val total get() = customers + visits + availability + sales
}

class SyncStatusViewModel : ViewModel() {
    private val _counts = MutableStateFlow(PendingCounts())
    val counts = _counts.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val db = Graph.db
            _counts.value = PendingCounts(
                customers = db.customerDao().unsynced().size,
                visits = db.visitDao().unsynced().size,
                availability = db.availabilityDao().unsynced().size,
                sales = db.saleDao().unsynced().size
            )
        }
    }

    fun syncNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    onBack: () -> Unit,
    vm: SyncStatusViewModel = viewModel()
) {
    val context = LocalContext.current
    val counts by vm.counts.collectAsStateSafe()
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = brandedTopBarColors()
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pending upload", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Customers: ${counts.customers}")
                    Text("Visits: ${counts.visits}")
                    Text("Availability: ${counts.availability}")
                    Text("Sales: ${counts.sales}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (counts.total == 0) "Everything is synced." else "${counts.total} records waiting.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.syncNow(context); vm.refresh() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sync now") }
        }
    }
}
