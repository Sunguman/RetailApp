package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.SelectChips
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ACTIVITY_TYPES = listOf("Promotion", "New product", "Price change", "POSM", "Other")

@OptIn(ExperimentalCoroutinesApi::class)
class CompetitorViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""

    val items: StateFlow<List<CompetitorActivity>> = vid.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList())
        else Graph.merchandisingRepository.observeCompetitor(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun save(competitor: String, type: String, desc: String, photo: Uri?, onSaved: () -> Unit) {
        viewModelScope.launch {
            Graph.merchandisingRepository.saveCompetitor(
                CompetitorActivity(
                    visitId = vid.value, customerId = customerId, repId = repId,
                    competitor = competitor.trim(), activityType = type, description = desc.trim()
                ),
                photo
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitorActivityScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: CompetitorViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()

    var competitor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ACTIVITY_TYPES.first()) }
    var desc by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }

    Retail360Scaffold(
        title = "Competitor activities",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(competitor, { competitor = it }, singleLine = true,
                    label = { Text("Competitor name") }, modifier = Modifier.fillMaxWidth())
                SelectChips(ACTIVITY_TYPES, type) { type = it }
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picker.launch("image/*") }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text(if (photo == null) "  Add photo" else "  Photo added")
                }
                Button(
                    onClick = {
                        vm.save(competitor, type, desc, photo) {
                            competitor = ""; desc = ""; photo = null; type = ACTIVITY_TYPES.first()
                        }
                    },
                    enabled = competitor.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Record activity") }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { c ->
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${c.competitor} · ${c.activityType}", style = MaterialTheme.typography.titleMedium)
                        if (c.description.isNotBlank())
                            Text(c.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
