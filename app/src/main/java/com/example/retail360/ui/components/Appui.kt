package com.example.retail360.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Design system for Retail360. Use these across every screen so the app
 * stays visually uniform — one definition here changes the look everywhere.
 */

/** The single source of truth for the branded (primary-blue) top app bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun brandedTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primary,
    titleContentColor = MaterialTheme.colorScheme.onPrimary,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
)

/**
 * Uniform Scaffold for all Retail360 screens.
 * Includes the branded TopAppBar and the footer automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Retail360Scaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    
    var accuracyM by remember { mutableStateOf<Float?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (onMenu != null) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = actions,
                colors = brandedTopBarColors()
            )
        },
        bottomBar = { AppFooter(version, accuracyM) },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = content
    )
}

/** Uppercase, muted section label used above groups of content. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = modifier.padding(bottom = 8.dp, top = 4.dp)
    )
}

/** Centered empty-state used when a list or tab has no content yet. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Slim primary-blue footer with app version and (optional) GPS accuracy. */
@Composable
fun AppFooter(version: String, accuracyM: Float? = null) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Powered By Retail360 · v$version", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.weight(1f))
            Text(accuracyM?.let { "GPS ±${it.roundToInt()} m" } ?: "GPS —",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** Icon + label/value row for detail lists. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Full-width primary action button — consistent height/shape everywhere. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Text(text)
    }
}

/** Standard content card with uniform corner radius and subtle elevation. */
@Composable
fun ScreenCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}

/** Horizontally-scrollable single-choice chip row (e.g. an activity/method type). */
@Composable
fun SelectChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val on = opt == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSelect(opt) }
            ) {
                Text(
                    opt,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
