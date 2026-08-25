package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/* ---------------- data ---------------- */

private enum class Accent { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

private data class OpModule(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val badge: String? = null,
    val onClick: () -> Unit
)

private data class OpSection(
    val title: String,
    val accent: Accent,
    val modules: List<OpModule>
)

/* ---------------- view model (live counts) ---------------- */

class OperationsViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    val planned: StateFlow<Int> = Graph.routePlanRepository.countForDay(repId, todayDow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customers: StateFlow<Int> = Graph.customerRepository.observeAll()
        .map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val inventoryItems: StateFlow<Int> = Graph.inventoryRepository.observeAll()
        .map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val productsCount: StateFlow<Int> = Graph.productRepository.observeAll()
        .map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _pending = MutableStateFlow(0)
    val pending: StateFlow<Int> = _pending.asStateFlow()

    fun refreshPending() {
        viewModelScope.launch {
            val db = Graph.db
            _pending.value = db.customerDao().unsynced().size +
                    db.visitDao().unsynced().size +
                    db.availabilityDao().unsynced().size +
                    db.saleDao().unsynced().size +
                    db.routePlanDao().unsynced().size +
                    db.inventoryDao().unsynced().size
        }
    }
}

/* ---------------- UI ---------------- */

@Composable
fun OperationsHub(
    onOpenCustomers: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenRoutePlan: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onComingSoon: (String) -> Unit,
    vm: OperationsViewModel = viewModel()
) {
    val planned by vm.planned.collectAsStateSafe()
    val customers by vm.customers.collectAsStateSafe()
    val inventoryItems by vm.inventoryItems.collectAsStateSafe()
    val productsCount by vm.productsCount.collectAsStateSafe()
    val pending by vm.pending.collectAsStateSafe()

    LaunchedEffect(Unit) { vm.refreshPending() }

    val sections = listOf(
        OpSection(
            "Field work", Accent.PRIMARY, listOf(
                OpModule("Check-in / Checkout", "Start and close customer visits",
                    Icons.Filled.PinDrop, true, onClick = onOpenCheckIn),
                OpModule("Route plan", "Customers assigned for today",
                    Icons.Filled.Route, true,
                    badge = planned.takeIf { it > 0 }?.toString(), onClick = onOpenRoutePlan),
                OpModule("Customers", "Browse and add outlets",
                    Icons.Filled.Groups, true,
                    badge = customers.takeIf { it > 0 }?.toString(), onClick = onOpenCustomers)
            )
        ),
        OpSection(
            "Sales & stock", Accent.SECONDARY, listOf(
                OpModule("Products", "View and manage catalog",
                    Icons.Filled.Inventory, true,
                    badge = productsCount.takeIf { it > 0 }?.toString(), onClick = onOpenProducts),
                OpModule("Inventory", "Van stock on hand",
                    Icons.Filled.Inventory2, true,
                    badge = inventoryItems.takeIf { it > 0 }?.toString(), onClick = onOpenInventory),
                OpModule("Sales / Orders", "Record sales during a visit",
                    Icons.Filled.ShoppingCart, false) { onComingSoon("Sales") },
                OpModule("Payment collection", "Collect and record payments",
                    Icons.Filled.Payments, false) { onComingSoon("Payments") }
            )
        ),
        OpSection(
            "Insights", Accent.TERTIARY, listOf(
                OpModule("On-shelf availability", "Audit whether products are stocked",
                    Icons.Filled.Checklist, false) { onComingSoon("OSA") },
                OpModule("Share of shelf", "Measure your shelf presence",
                    Icons.Filled.ViewWeek, false) { onComingSoon("Share of shelf") },
                OpModule("Product updates", "Log new or removed SKUs",
                    Icons.Filled.Update, false) { onComingSoon("Product updates") },
                OpModule("Competitor activity", "Track competitor moves",
                    Icons.AutoMirrored.Filled.CompareArrows, false) { onComingSoon("Competitor activity") }
            )
        ),
        OpSection(
            "System", Accent.NEUTRAL, listOf(
                OpModule("Sync", "Upload offline work to the server",
                    Icons.Filled.CloudSync, true,
                    badge = pending.takeIf { it > 0 }?.toString(), onClick = onOpenSync),
                OpModule("Pending trips", "Planned deliveries",
                    Icons.Filled.LocalShipping, false) { onComingSoon("Pending trips") }
            )
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sections.forEach { section ->
            item(span = { GridItemSpan(2) }) {
                SectionHeader(section.title)
            }
            items(section.modules, key = { it.label }) { module ->
                ModuleCard(section.accent, module)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ModuleCard(accent: Accent, module: OpModule) {
    val effective = if (module.enabled) accent else Accent.NEUTRAL
    val container = accentContainer(effective)
    val onContainer = accentOnContainer(effective)

    Card(
        onClick = module.onClick,
        enabled = module.enabled,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = container,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            module.icon, 
                            contentDescription = null, 
                            tint = onContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    module.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
                Text(
                    module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            if (module.badge != null || !module.enabled) {
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    TrailingPill(module, accent)
                }
            }
        }
    }
}

@Composable
private fun TrailingPill(module: OpModule, accent: Accent) {
    when {
        !module.enabled -> Pill(
            "Soon",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        module.badge != null -> Pill(
            module.badge,
            accentContainer(accent),
            accentOnContainer(accent)
        )
    }
}

@Composable
private fun Pill(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun accentContainer(a: Accent): Color = when (a) {
    Accent.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
    Accent.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
    Accent.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
    Accent.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun accentOnContainer(a: Accent): Color = when (a) {
    Accent.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
    Accent.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
    Accent.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
    Accent.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}
