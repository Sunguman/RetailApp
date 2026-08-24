package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.retail360.data.SyncWorker
import com.example.retail360.navigation.LocalDrawerOpener
import com.example.retail360.ui.theme.screens.OperationsHub
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import com.example.retail360.util.LocationProvider
import com.example.retail360.util.NotificationCenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DashboardViewModel : ViewModel() {
    private val startOfDay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis

    val visited: StateFlow<Int> = Graph.visitRepository
        .countCompletedToday(startOfDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCustomers: StateFlow<Int> = Graph.customerRepository.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // In memory for now — persist (DataStore/Room) so a started day survives an app restart.
    private val _dayStartedAt = MutableStateFlow<Long?>(null)
    val dayStartedAt: StateFlow<Long?> = _dayStartedAt.asStateFlow()

    val repName: String = (Graph.authRepository.currentUser()?.email ?: "there")
        .substringBefore("@")
        .replaceFirstChar { it.uppercase() }

    fun startDay() { _dayStartedAt.value = System.currentTimeMillis() }
    fun closeDay() { _dayStartedAt.value = null }

    fun refresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { Graph.customerRepository.refreshFromServer() }
            runCatching { Graph.productRepository.refreshCatalog() }
            onDone()
        }
    }

    /** Surfaces a notification if any local records are still waiting to upload. */
    fun checkPendingSync() {
        viewModelScope.launch {
            val db = Graph.db
            val pending = db.customerDao().unsynced().size +
                    db.visitDao().unsynced().size +
                    db.availabilityDao().unsynced().size +
                    db.saleDao().unsynced().size
            if (pending > 0) {
                NotificationCenter.postUnique(
                    key = "pending-sync",
                    title = "Pending sync",
                    body = "$pending record${if (pending == 1) "" else "s"} waiting to upload"
                )
            }
        }
    }

    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    val plannedToday: StateFlow<Int> = Graph.routePlanRepository.countForDay(repId, todayDow)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val stockValue: StateFlow<Double> = Graph.inventoryRepository.totalValue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun logout() = Graph.authRepository.signOut()
}

private enum class DateFilter { TODAY, WEEK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCustomers: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenRoutePlan: () -> Unit,
    onOpenInventory: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val visited by vm.visited.collectAsStateSafe()
    val totalCustomers by vm.totalCustomers.collectAsStateSafe()
    val plannedToday by vm.plannedToday.collectAsStateSafe()
    val stockValue by vm.stockValue.collectAsStateSafe()
    val dayStartedAt by vm.dayStartedAt.collectAsStateSafe()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(dayStartedAt) {
        while (true) { now = System.currentTimeMillis(); delay(30_000) }
    }

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    var accuracyM by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            accuracyM = LocationProvider(context).current()?.accuracyM
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(DateFilter.TODAY) }
    var refreshing by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }
    val notifications by NotificationCenter.items.collectAsStateSafe()
    val unread = notifications.count { !it.read }

    LaunchedEffect(Unit) { vm.checkPendingSync() }

    fun comingSoon(name: String) {
        scope.launch { snackbar.showSnackbar("$name — coming soon") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retail360") },
                navigationIcon = {
                    IconButton(onClick = LocalDrawerOpener.current) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!refreshing) {
                            refreshing = true
                            // Push any unsynced local work up…
                            WorkManager.getInstance(context)
                                .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
                            // …and pull the latest down. Screens observe Room, so the
                            // whole app updates once these land.
                            vm.refresh {
                                refreshing = false
                                vm.checkPendingSync()
                                NotificationCenter.post(
                                    "Data refreshed",
                                    "Latest customers and products loaded"
                                )
                                scope.launch { snackbar.showSnackbar("Refreshed") }
                            }
                        }
                    }) {
                        if (refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }

                    androidx.compose.foundation.layout.Box {
                        BadgedBox(
                            badge = { if (unread > 0) Badge { Text(unread.toString()) } }
                        ) {
                            IconButton(onClick = {
                                notifOpen = true
                                NotificationCenter.markAllRead()
                            }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                            }
                        }
                        DropdownMenu(
                            expanded = notifOpen,
                            onDismissRequest = { notifOpen = false }
                        ) {
                            if (notifications.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No notifications") },
                                    onClick = { notifOpen = false }
                                )
                            } else {
                                notifications.forEach { n ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(n.title, style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    n.body,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { notifOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { vm.logout(); onLoggedOut() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { DayBar(dayStartedAt, version, accuracyM, vm::startDay, vm::closeDay) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Blue header: greeting + tabs (continuous with the app bar)
            Surface(color = MaterialTheme.colorScheme.primary) {
                Column {
                    Text(
                        "${greeting()}, ${vm.repName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    TabRow(
                        selectedTabIndex = tab,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Tab(selected = tab == 0, onClick = { tab = 0 },
                            text = { Text("Dashboard") })
                        Tab(selected = tab == 1, onClick = { tab = 1 },
                            text = { Text("Operations") })
                    }
                }
            }

            androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> DashboardTab(
                        filter = filter,
                        onFilter = { filter = it },
                        onPickDate = { comingSoon("Date picker") },
                        visited = visited,
                        planned = plannedToday,
                        totalCustomers = totalCustomers,
                        stockValue = stockValue
                    )
                    else -> OperationsHub(
                        onOpenCustomers = onOpenCustomers,
                        onOpenSync = onOpenSync,
                        onOpenRoutePlan = onOpenRoutePlan,
                        onOpenInventory = onOpenInventory,
                        onComingSoon = { comingSoon(it) }
                    )
                }
            }
        }
    }
}

/* ---------------- Dashboard (KPI) tab ---------------- */

@Composable
private fun DashboardTab(
    filter: DateFilter,
    onFilter: (DateFilter) -> Unit,
    onPickDate: () -> Unit,
    visited: Int,
    planned: Int,
    totalCustomers: Int,
    stockValue: Double
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter pills + calendar
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterPill("Today", filter == DateFilter.TODAY) { onFilter(DateFilter.TODAY) }
            Spacer(Modifier.width(12.dp))
            FilterPill("This week", filter == DateFilter.WEEK) { onFilter(DateFilter.WEEK) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPickDate) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
            }
        }

        // Visits + sales summary
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    MiniStat("To visit", planned.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    MiniStat("Visited", visited.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    MiniStat("Pending", (planned - visited).coerceAtLeast(0).toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                MetricBar("Sales amount", 0, 0) // TODO: sum today's SaleItem line totals
            }
        }

        // Customer performance
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Customer performance", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                MetricBar("Interaction", visited, totalCustomers)
                Spacer(Modifier.height(12.dp))
                MetricBar("Productivity", 0, totalCustomers) // TODO: orders placed today
            }
        }

        // Pending trips / deliveries
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountCard("Pending trips", 0, Modifier.weight(1f))     // TODO: trips model
            CountCard("Pending deliveries", 0, Modifier.weight(1f)) // TODO: deliveries model
        }

        // Current stock value
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Current stock", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(ksh(stockValue), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MetricBar(label: String, value: Int, target: Int) {
    val fraction = if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$value / $target", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.weight(1f).height(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier) {
    Card(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Badge { Text(count.toString()) }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

/* ---------------- Activities (launcher) tab ---------------- */

/* ---------------- Shared bottom bar ---------------- */

@Composable
private fun DayBar(
    dayStartedAt: Long?,
    version: String,
    accuracyM: Float?,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    Column {
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dayStartedAt != null) {
                    Text("Started ${timeFormat.format(Date(dayStartedAt))}",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Close day") }
                } else {
                    Text("Day not started",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) { Text("Open day") }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.primary) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("v$version", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.weight(1f))
                Text(accuracyM?.let { "GPS ±${it.roundToInt()} m" } ?: "GPS —",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/* ---------------- helpers ---------------- */

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun ksh(v: Double): String = "KES %,.2f".format(v)

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED


