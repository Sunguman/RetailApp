package com.example.retail360.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.retail360.util.Graph
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import com.example.retail360.ui.theme.Retail360Theme
import com.example.retail360.ui.theme.White
import com.example.retail360.ui.theme.Black
import com.example.retail360.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the splash decides to send the rep once startup work finishes. */
enum class StartDestination { PENDING, AUTH }

class SplashViewModel : ViewModel() {
    private val _target = MutableStateFlow(StartDestination.PENDING)
    val target = _target.asStateFlow()

    fun decide() {
        // Guard against re-running if the ViewModel is retained across config changes.
        if (_target.value != StartDestination.PENDING) return
        viewModelScope.launch {
            // Warm the local catalog while the splash is shown.
            runCatching { Graph.productRepository.refreshCatalog() }
            
            // Keep the brand frame on screen long enough to be seen.
            delay(7000)
            _target.value = StartDestination.AUTH
        }
    }
}

@Composable
fun SplashScreen(
    onNext: () -> Unit,
    vm: SplashViewModel = viewModel()
) {
    val target by vm.target.collectAsState()

    LaunchedEffect(Unit) { vm.decide() }
    LaunchedEffect(target) {
        if (target == StartDestination.AUTH) {
            onNext()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Retail360",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Black
                )
                Text(
                    "Field rep companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Black
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7)
@Composable
fun SplashPreview() {
    Retail360Theme {
        SplashScreen(onNext = {})
    }
}
