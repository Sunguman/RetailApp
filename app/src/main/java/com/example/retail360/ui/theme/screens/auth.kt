package com.example.retail360.ui.theme.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retail360.util.Graph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import com.example.retail360.ui.theme.Retail360Theme
import com.example.retail360.ui.theme.White
import com.example.retail360.ui.theme.Black
import com.example.retail360.R

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val success: Boolean = false,
    val isSignUp: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val repo = Graph.authRepository
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun toggleMode() {
        _state.value = _state.value.copy(
            isSignUp = !_state.value.isSignUp, 
            error = null,
            infoMessage = null
        )
    }

    fun authenticate(
        email: String,
        password: String,
        firstName: String = "",
        lastName: String = "",
        phone: String = "",
        confirmPassword: String = ""
    ) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Enter email and password", infoMessage = null)
            return
        }
        if (_state.value.isSignUp) {
            if (firstName.isBlank() || lastName.isBlank()) {
                _state.value = _state.value.copy(error = "Enter first and second names", infoMessage = null)
                return
            }
            if (password != confirmPassword) {
                _state.value = _state.value.copy(error = "Passwords do not match", infoMessage = null)
                return
            }
        }
        _state.value = _state.value.copy(loading = true, error = null, infoMessage = null)
        viewModelScope.launch {
            val isSigningUp = _state.value.isSignUp
            val result = if (isSigningUp) {
                runCatching { repo.signUp(email, password, "$firstName $lastName", phone) }
            } else {
                runCatching { repo.signIn(email, password) }
            }

            result.onSuccess {
                if (isSigningUp) {
                    // Registration successful. Don't go to dashboard yet.
                    // Switch to login mode and show success message.
                    repo.signOut() // Firebase automatically logs in on signup, sign out to force manual login.
                    _state.value = AuthUiState(
                        isSignUp = false,
                        infoMessage = "Account created! Please sign in with your credentials."
                    )
                } else {
                    // Warm the local catalog right after successful login.
                    runCatching { Graph.productRepository.refreshCatalog() }
                    _state.value = _state.value.copy(loading = false, success = true)
                }
            }
            .onFailure { 
                _state.value = _state.value.copy(loading = false, error = it.message ?: "Authentication failed") 
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthed: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onAuthed()
    }

    AuthContent(
        state = state,
        onAuthenticate = { email, password, fName, lName, phone, cPassword -> 
            vm.authenticate(email, password, fName, lName, phone, cPassword) 
        },
        onToggleMode = { vm.toggleMode() }
    )
}

@Composable
fun AuthContent(
    state: AuthUiState,
    onAuthenticate: (String, String, String, String, String, String) -> Unit,
    onToggleMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Retail360", style = MaterialTheme.typography.headlineMedium, color = Black)
            Text(
                if (state.isSignUp) "Create new account" else "Field rep sign-in",
                style = MaterialTheme.typography.bodyMedium,
                color = Black.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(24.dp))

            if (state.isSignUp) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = firstName, onValueChange = { firstName = it },
                        label = { Text("First Name") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = lastName, onValueChange = { lastName = it },
                        label = { Text("Second Name") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone Number") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email Address") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isSignUp) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") }, singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (confirmPasswordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            state.infoMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onAuthenticate(email, password, firstName, lastName, phone, confirmPassword) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.height(20.dp), color = White)
                else Text(if (state.isSignUp) "Sign up" else "Sign in")
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onToggleMode) {
                Text(
                    if (state.isSignUp) "Already have an account? Sign in" 
                    else "Don't have an account? Sign up"
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7)
@Composable
fun AuthPreview() {
    Retail360Theme {
        AuthContent(
            state = AuthUiState(),
            onAuthenticate = { _, _, _, _, _, _ -> },
            onToggleMode = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7)
@Composable
fun AuthSignUpPreview() {
    Retail360Theme {
        AuthContent(
            state = AuthUiState(isSignUp = true),
            onAuthenticate = { _, _, _, _, _, _ -> },
            onToggleMode = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7)
@Composable
fun AuthLoadingPreview() {
    Retail360Theme {
        AuthContent(
            state = AuthUiState(loading = true),
            onAuthenticate = { _, _, _, _, _, _ -> },
            onToggleMode = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_7)
@Composable
fun AuthErrorPreview() {
    Retail360Theme {
        AuthContent(
            state = AuthUiState(error = "Invalid credentials"),
            onAuthenticate = { _, _, _, _, _, _ -> },
            onToggleMode = {}
        )
    }
}


