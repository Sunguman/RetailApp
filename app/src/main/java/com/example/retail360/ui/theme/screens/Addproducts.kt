package com.example.retail360.ui.theme.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.retail360.data.model.Product
import com.example.retail360.ui.components.Retail360Scaffold
import com.example.retail360.ui.components.brandedTopBarColors
import com.example.retail360.util.Graph
import kotlinx.coroutines.launch

class AddProductViewModel : ViewModel() {
    var saving by mutableStateOf(false)
        private set

    fun save(product: Product, photoUri: Uri?, onDone: () -> Unit) {
        saving = true
        viewModelScope.launch {
            val imageUrl = photoUri?.let {
                runCatching { Graph.cloudinary.upload(it, "products") }.getOrNull()
            }.orEmpty()
            Graph.productRepository.save(product.copy(imageUrl = imageUrl))
            saving = false
            onDone()
        }
    }
}

@Composable
fun AddProductScreen(
    onDone: () -> Unit,
    vm: AddProductViewModel = viewModel()
) {
    AddProductContent(
        saving = vm.saving,
        onSave = { product, uri -> vm.save(product, uri, onDone) },
        onDone = onDone
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductContent(
    saving: Boolean,
    onSave: (Product, Uri?) -> Unit,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) photoUri = uri }

    val priceValue = price.toDoubleOrNull()
    val canSave = name.isNotBlank() && priceValue != null && !saving

    Retail360Scaffold(
        title = "Add product",
        onBack = onDone
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image picker
            Box(
                Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoUri == null) {
                    OutlinedButton(onClick = { picker.launch("image/*") }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Text("  Add photo")
                    }
                } else {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Product photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
            if (photoUri != null) {
                Text("Change photo", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)))
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Product name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sku, onValueChange = { sku = it },
                    label = { Text("SKU") }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Price (KES)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text("Barcode") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(
                        Product(
                            name = name.trim(), sku = sku.trim(), barcode = barcode.trim(),
                            category = category.trim(), price = priceValue ?: 0.0
                        ),
                        photoUri
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp
                    )
                } else Text("Save product")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddProductPreview() {
    MaterialTheme {
        AddProductContent(
            saving = false,
            onSave = { _, _ -> },
            onDone = {}
        )
    }
}

