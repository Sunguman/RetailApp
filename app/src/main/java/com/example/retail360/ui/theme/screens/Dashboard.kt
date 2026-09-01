package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.retail360.data.sync.SyncWorker
import com.example.retail360.navigation.LocalDrawerOpener
import com.example.retail360.ui.components.AppFooter
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.collectAsStateSafe
import com.example.retail360.util.Graph
import com.example.retail360.util.ksh
import com.example.retail360.util.LocationProvider
import com.example.retail360.util.NotificationCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class DateFilter { TODAY, WEEK, MONTH, YEAR }

class DashboardViewModel : ViewModel() {
    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange = _dateRange

    private fun getStartTimestamp(filter: DateFilter): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return when (filter) {
            DateFilter.TODAY -> cal.timeInMillis
            DateFilter.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            DateFilter.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            DateFilter.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
        }
    }

    private val _currentFilter = MutableStateFlow(DateFilter.WEEK)
    val currentFilter = _currentFilter

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val visited: StateFlow<Int> = combine(_currentFilter, _dateRange) { filter, range ->
        if (range != null) range.first else getStartTimestamp(filter)
    }.flatMapLatest { start ->
        Graph.visitRepository.countCompletedToday(start)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCustomers: StateFlow<Int> = Graph.customerRepository.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val repName: String = (Graph.authRepository.currentUser()?.email ?: "there")
        .substringBefore("@")
        .replaceFirstChar { it.uppercase() }

    fun setFilter(filter: DateFilter) {
        _currentFilter.value = filter
        _dateRange.value = null
    }

    fun setCustomRange(start: Long, end: Long) {
        _dateRange.value = start to end
    }

    fun refresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { Graph.customerRepository.refreshFromServer() }
            runCatching { Graph.productRepository.refreshCatalog() }
            onDone()
        }
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCustomers: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenRoutePlan: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenSales: () -> Unit,
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
    val filter by vm.currentFilter.collectAsStateSafe()
    val customRange by vm.dateRange.collectAsStateSafe()

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    var accuracyM by remember { mutableStateOf<Float?>(null) }
    
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (hasLocationPermission(context)) {
            accuracyM = LocationProvider(context).current()?.accuracyM
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var notifOpen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val notifications by NotificationCenter.items.collectAsStateSafe()
    val unread = notifications.count { !it.read }

    LaunchedEffect(Unit) { vm.checkPendingSync() }

    Retail360Scaffold(
        title = "Retail360",
        onMenu = LocalDrawerOpener.current,
        actions = {
            IconButton(onClick = {
                if (!refreshing) {
                    refreshing = true
                    WorkManager.getInstance(context)
                        .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
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

            Box {
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
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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

            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> DashboardTab(
                        filter = filter,
                        customRange = customRange,
                        onFilter = { vm.setFilter(it) },
                        onPickDate = { showDatePicker = true },
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
                        onOpenProducts = onOpenProducts,
                        onOpenCheckIn = onOpenCheckIn,
                        onOpenSales = onOpenSales,
                        onActionRequiringVisit = { scope.launch { snackbar.showSnackbar("$it: Select an outlet to start a visit first") } }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerModal(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                if (start != null && end != null) {
                    vm.setCustomRange(start, end)
                }
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTab(
    filter: DateFilter,
    customRange: Pair<Long, Long>?,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterPill("Today", filter == DateFilter.TODAY && customRange == null) { onFilter(DateFilter.TODAY) }
            Spacer(Modifier.width(8.dp))
            FilterPill("Week", filter == DateFilter.WEEK && customRange == null) { onFilter(DateFilter.WEEK) }
            Spacer(Modifier.width(8.dp))
            FilterPill("Month", filter == DateFilter.MONTH && customRange == null) { onFilter(DateFilter.MONTH) }
            Spacer(Modifier.width(8.dp))
            FilterPill("Year", filter == DateFilter.YEAR && customRange == null) { onFilter(DateFilter.YEAR) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPickDate) {
                Icon(
                    Icons.Filled.CalendarMonth, 
                    contentDescription = "Select date range",
                    tint = if (customRange != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (customRange != null) {
            val df = SimpleDateFormat("MMM d", Locale.getDefault())
            Text(
                "Range: ${df.format(Date(customRange.first))} - ${df.format(Date(customRange.second))}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                MiniStat("To visit", planned.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MiniStat("Visited", visited.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                MiniStat("Pending", (planned - visited).coerceAtLeast(0).toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Sales", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                MetricBar("Amount / target", 0, 0)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Customer performance", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                MetricBar("Interaction", visited, totalCustomers)
                Spacer(Modifier.height(12.dp))
                MetricBar("Productivity", 0, totalCustomers)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Current stock", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(stockValue.ksh(), style = MaterialTheme.typography.headlineMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select date range"
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(16.dp)
        )
    }
}
