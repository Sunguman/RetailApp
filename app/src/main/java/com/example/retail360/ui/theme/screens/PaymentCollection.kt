package com.example.retail360.ui.theme.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.data.model.PaymentCollection
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.ui.components.SelectChips
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val METHODS = listOf("Cash", "M-Pesa", "Cheque", "Bank")

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""

    val items = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else Graph.merchandisingRepository.observePayments(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun save(amount: Double, method: String, reference: String, note: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            Graph.merchandisingRepository.savePayment(
                PaymentCollection(
                    visitId = vid.value, customerId = customerId, repId = repId,
                    amount = amount, method = method, reference = reference.trim(), note = note.trim()
                )
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCollectionScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: PaymentViewModel = viewModel()
) {
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()
    val total = items.sumOf { it.amount }

    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(METHODS.first()) }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amountValue = amount.toDoubleOrNull()

    Retail360Scaffold(
        title = "Payment collection",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Collected this visit", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("KES %,.2f".format(total), style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (KES)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                SelectChips(METHODS, method) { method = it }
                OutlinedTextField(reference, { reference = it }, singleLine = true,
                    label = { Text("Reference (e.g. M-Pesa code)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        vm.save(amountValue ?: 0.0, method, reference, note) {
                            amount = ""; reference = ""; note = ""
                        }
                    },
                    enabled = amountValue != null && amountValue > 0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Record payment") }
            }
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { p ->
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("KES %,.2f".format(p.amount),
                                style = MaterialTheme.typography.titleMedium)
                            Text("${p.method}${if (p.reference.isNotBlank()) " · ${p.reference}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

