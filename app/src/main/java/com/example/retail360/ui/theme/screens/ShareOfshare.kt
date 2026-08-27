package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.ShareOfShelf
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
class ShareOfShelfViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""

    val items = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else Graph.merchandisingRepository.observeShareOfShelf(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun save(category: String, our: Int, total: Int, photo: Uri?, onSaved: () -> Unit) {
        viewModelScope.launch {
            Graph.merchandisingRepository.saveShareOfShelf(
                ShareOfShelf(
                    visitId = vid.value, customerId = customerId, repId = repId,
                    category = category.trim(), ourFacings = our, totalFacings = total
                ),
                photo
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareOfShelfScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: ShareOfShelfViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()

    var category by remember { mutableStateOf("") }
    var our by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }

    val ourN = our.toIntOrNull() ?: 0
    val totalN = total.toIntOrNull() ?: 0
    val sos = if (totalN <= 0) 0 else (ourN * 100 / totalN)
    val valid = category.isNotBlank() && totalN > 0 && ourN <= totalN

    Retail360Scaffold(
        title = "Share of shelf",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(category, { category = it }, singleLine = true,
                    label = { Text("Category (e.g. Chocolate)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(our, { our = it.filter(Char::isDigit) }, singleLine = true,
                        label = { Text("Our facings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(total, { total = it.filter(Char::isDigit) }, singleLine = true,
                        label = { Text("Total facings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
                Text("Share of shelf: $sos%", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { picker.launch("image/*") }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text(if (photo == null) "  Add shelf photo" else "  Photo added")
                }
                Button(
                    onClick = {
                        vm.save(category, ourN, totalN, photo) {
                            category = ""; our = ""; total = ""; photo = null
                        }
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Record") }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { s ->
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(s.category, style = MaterialTheme.typography.titleMedium)
                            Text("${s.ourFacings} / ${s.totalFacings} facings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${s.sosPercent}%", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

