package com.example.retail360.ui.theme.Components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Shared UI components for Retail360.
 */

/** Slim primary-blue footer with app version and (optional) GPS accuracy. */
@Composable
fun AppFooter(version: String, accuracyM: Float? = null) {
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
