package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.data.repository.BrandInfo
import com.example.retail360.data.repository.CompetitorInfo
import com.example.retail360.data.repository.DEFAULT_COMPETITORS
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.SearchableDropdown
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ACTIVITY_TYPES = listOf(
    "Price Reduction", "Discount", "Buy One Get One", "Bundle Offer", "Gift With Purchase",
    "Multi-Buy", "Cashback", "Free Installation", "Free Delivery", "Display Promotion",
    "New Product Launch", "In-Store Activation", "Other"
)
val DISPLAY_TYPES = listOf(
    "No Display", "Shelf Display", "End Cap", "Gondola", "Branded Stand",
    "Window Display", "Floor Display", "Hanging Display", "Other"
)
private val PRICE_TYPES = setOf("Price Reduction", "Discount")
private val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

class AddCompetitorViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private var visitId = ""
    private var customerId = ""
    var outletName by mutableStateOf(""); private set
    private var lat = 0.0
    private var lng = 0.0
    var saving by mutableStateOf(false); private set

    fun bind(vid: String, context: Context) {
        visitId = vid
        viewModelScope.launch {
            val v = Graph.visitRepository.byId(vid)
            customerId = v?.customerId ?: ""
            outletName = customerId.takeIf { it.isNotBlank() }
                ?.let { Graph.customerRepository.byId(it)?.name } ?: ""
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                com.example.retail360.util.LocationProvider(context).current()?.let {
                    lat = it.lat; lng = it.lng
                }
            }
        }
    }

    fun save(build: (String, String, String, Double, Double) -> CompetitorActivity, photo: Uri, onDone: () -> Unit) {
        saving = true
        viewModelScope.launch {
            Graph.merchandisingRepository.saveCompetitor(
                build(visitId, customerId, outletName, lat, lng).copy(repId = repId),
                photo
            )
            saving = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompetitorActivityScreen(
    visitId: String,
    onDone: () -> Unit,
    vm: AddCompetitorViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(visitId) { vm.bind(visitId, context) }

    var competitor by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var activityType by remember { mutableStateOf("") }
    var otherActivity by remember { mutableStateOf("") }
    var beforePrice by remember { mutableStateOf("") }
    var afterPrice by remember { mutableStateOf("") }
    var stockStatus by remember { mutableStateOf("") }
    var estQty by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(0L) }
    var ongoing by remember { mutableStateOf(false) }
    var displayType by remember { mutableStateOf("") }
    var otherDisplay by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var pending by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf("") }

    val competitors: List<CompetitorInfo> = DEFAULT_COMPETITORS
    val brands: List<BrandInfo> = competitors.firstOrNull { it.name == competitor }?.brands ?: emptyList()
    val products: List<String> = brands.firstOrNull { it.name == brand }?.products ?: emptyList()

    val isPrice = activityType in PRICE_TYPES
    val before = beforePrice.toDoubleOrNull() ?: 0.0
    val after = afterPrice.toDoubleOrNull() ?: 0.0
    val discountAmt = (before - after).coerceAtLeast(0.0)
    val depth = if (before > 0) (before - after) / before * 100 else 0.0

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photo = pending
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) { val u = createImageUri(context); pending = u; cameraLauncher.launch(u) }
    }

    fun validate(): String? {
        if (competitor.isBlank()) return "Competitor is required."
        if (brands.isNotEmpty() && brand.isBlank()) return "Competitor Brand is required."
        if (products.isNotEmpty() && sku.isBlank()) return "Competitor Product / SKU is required."
        if (activityType.isBlank()) return "Activity Type is required."
        if (activityType == "Other" && otherActivity.isBlank()) return "Describe the activity."
        if (isPrice) {
            if (before <= 0.0 || after <= 0.0) return "Enter valid Before and After prices."
            if (activityType == "Price Reduction" && after > before) return "After Price cannot exceed Before Price."
        }
        if (stockStatus.isBlank()) return "Stock Availability is required."
        if (endDate != 0L && endDate < startDate) return "End Date cannot be earlier than Start Date."
        if (photo == null) return "Photo Evidence is required."
        return null
    }

    Retail360Scaffold(
        title = "Add Competitor Activity",
        onBack = onDone
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchableDropdown("Competitor", competitors.map { it.name }, competitor,
                onSelect = { competitor = it; brand = ""; sku = "" })
            SearchableDropdown("Competitor Brand", brands.map { it.name }, brand,
                onSelect = { brand = it; sku = "" }, enabled = competitor.isNotBlank())
            SearchableDropdown("Competitor Product / SKU", products, sku,
                onSelect = { sku = it }, enabled = brand.isNotBlank())
            SearchableDropdown("Activity Type", ACTIVITY_TYPES, activityType,
                onSelect = { activityType = it })

            if (activityType == "Other")
                OutlinedTextField(otherActivity, { otherActivity = it }, label = { Text("Describe activity") },
                    modifier = Modifier.fillMaxWidth())

            // Dynamic pricing
            if (isPrice) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(beforePrice, { beforePrice = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Before Price") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(afterPrice, { afterPrice = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("After Price") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.weight(1f))
                }
                Text("Discount Amount: KES %,.2f   ·   Depth: %.1f%%".format(discountAmt, depth),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary)
            }

            // Stock availability
            Text("Competitor Stock Availability", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("IN_STOCK" to "In Stock", "OUT_OF_STOCK" to "Out of Stock", "UNKNOWN" to "Unknown")
                .forEach { (v, l) ->
                    Row(Modifier.fillMaxWidth().selectable(stockStatus == v) { stockStatus = v },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = stockStatus == v, onClick = { stockStatus = v })
                        Text(l, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            if (stockStatus == "IN_STOCK")
                OutlinedTextField(estQty, { estQty = it.filter(Char::isDigit) },
                    label = { Text("Estimated Stock Quantity (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())

            // Dates
            DateField("Start Date", startDate, enabled = true) { startDate = it }
            Row(Modifier.fillMaxWidth().selectable(ongoing) { ongoing = !ongoing },
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ongoing, onCheckedChange = { ongoing = it; if (it) endDate = 0L })
                Text("Ongoing (no end date)", style = MaterialTheme.typography.bodyMedium)
            }
            if (!ongoing) DateField("End Date (optional)", endDate, enabled = true) { endDate = it }

            val status = statusOf(startDate, endDate, ongoing, System.currentTimeMillis())
            Text("Status: $status   ·   Duration: ${durationDays(startDate, endDate)} days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Display type
            SearchableDropdown("Display Type", DISPLAY_TYPES, displayType, onSelect = { displayType = it })
            if (displayType == "Other")
                OutlinedTextField(otherDisplay, { otherDisplay = it }, label = { Text("Describe display") },
                    modifier = Modifier.fillMaxWidth())

            // Photo evidence (mandatory)
            Text("Photo Evidence *", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (photo == null) {
                OutlinedButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null); Text("  Capture Photo")
                }
            } else {
                AsyncImage(model = photo, contentDescription = "Evidence", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) { Text("Retake") }
                    TextButton(onClick = { photo = null }) { Text("Remove") }
                }
            }

            OutlinedTextField(notes, { notes = it }, label = { Text("Observations / Notes") },
                modifier = Modifier.fillMaxWidth())

            if (error.isNotBlank())
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val v = validate()
                    if (v != null) { error = v; return@Button }
                    error = ""
                    vm.save({ vid, cid, outlet, la, ln ->
                        CompetitorActivity(
                            visitId = vid, customerId = cid, outletName = outlet,
                            competitor = competitor, brand = brand, productSku = sku,
                            activityType = activityType, otherActivity = otherActivity,
                            beforePrice = before, afterPrice = after,
                            stockStatus = stockStatus, estimatedQty = estQty.toIntOrNull() ?: 0,
                            startDate = startDate, endDate = if (ongoing) 0L else endDate, ongoing = ongoing,
                            displayType = displayType, otherDisplay = otherDisplay, notes = notes,
                            lat = la, lng = ln
                        )
                    }, photo!!, onDone)
                },
                enabled = !vm.saving, modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, millis: Long, enabled: Boolean, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = if (millis == 0L) "—" else dateFmt.format(Date(millis)),
        onValueChange = {}, readOnly = true, enabled = false,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .let { it }
    )
    TextButton(onClick = { if (enabled) open = true }) { Text("Pick $label") }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = if (millis == 0L) System.currentTimeMillis() else millis
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let(onPick); open = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

fun statusOf(start: Long, end: Long, ongoing: Boolean, now: Long): String = when {
    start > now -> "Upcoming"
    ongoing || end == 0L || end >= now -> "Ongoing"
    else -> "Completed"
}

fun durationDays(start: Long, end: Long): Long {
    val to = if (end == 0L) System.currentTimeMillis() else end
    return ((to - start) / 86_400_000L).coerceAtLeast(0)
}

private fun createImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "comp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

