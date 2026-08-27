package com.example.retail360.ui.theme.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.retail360.data.model.VisitPhoto
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
import java.io.File

private val CATEGORIES = listOf("Storefront", "Shelf", "Display", "POSM", "Other")

@OptIn(ExperimentalCoroutinesApi::class)
class VisitPhotosViewModel : ViewModel() {
    private val repId = Graph.authRepository.currentUser()?.uid ?: ""
    private val vid = MutableStateFlow("")
    private var customerId = ""

    val items = vid.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else Graph.merchandisingRepository.observePhotos(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bind(visitId: String) {
        vid.value = visitId
        viewModelScope.launch { customerId = Graph.visitRepository.byId(visitId)?.customerId ?: "" }
    }

    fun save(category: String, caption: String, photo: Uri, onSaved: () -> Unit) {
        viewModelScope.launch {
            Graph.merchandisingRepository.savePhoto(
                VisitPhoto(
                    visitId = vid.value, customerId = customerId, repId = repId,
                    category = category, caption = caption.trim()
                ),
                photo
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitPhotosScreen(
    visitId: String,
    onBack: () -> Unit,
    vm: VisitPhotosViewModel = viewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(visitId) { vm.bind(visitId) }
    val items by vm.items.collectAsStateSafe()

    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var caption by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingUri
        if (success && uri != null) {
            vm.save(category, caption, uri) { caption = "" }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Retail360Scaffold(
        title = "Photos",
        onBack = onBack
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectChips(CATEGORIES, category) { category = it }
                OutlinedTextField(caption, { caption = it }, singleLine = true,
                    label = { Text("Caption (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text("  Take photo")
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos yet for this visit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { photo ->
                        Column {
                            AsyncImage(
                                model = photo.photoUrl.ifBlank { photo.id },
                                contentDescription = photo.caption,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    photo.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

