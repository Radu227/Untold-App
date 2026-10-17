package com.untold.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untold.android.ui.theme.*
import com.untold.android.viewmodel.AuthState
import com.untold.android.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var isLoginTab by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        // Aesthetic Gradient Spheres in Background
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(300.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonPurple.copy(alpha = 0.15f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(300.dp)
                .offset(x = 50.dp, y = 50.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonCyan.copy(alpha = 0.15f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Title Brand
            Text(
                text = "UNTOLD",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, UntoldNeonCyan)
                    )
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "PORTAL FESTIVALIER",
                style = MaterialTheme.typography.labelLarge,
                color = UntoldNeonCyan,
                modifier = Modifier.padding(top = 4.dp),
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Glassmorphic Card
            Card(
                colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), UntoldNeonPurple.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .shadow(
                        elevation = 20.dp,
                        spotColor = UntoldNeonPurple.copy(alpha = 0.4f),
                        ambientColor = Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(UntoldDarkBackground),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isLoginTab) UntoldNeonPurple else Color.Transparent)
                                .clickable { isLoginTab = true }
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = "LOGIN",
                                color = if (isLoginTab) Color.White else UntoldOnSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (!isLoginTab) UntoldNeonPurple else Color.Transparent)
                                .clickable { isLoginTab = false }
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = "REGISTER",
                                color = if (!isLoginTab) Color.White else UntoldOnSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fields
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; localError = null },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = UntoldNeonCyan) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UntoldNeonCyan,
                                unfocusedBorderColor = UntoldOutlineVariant,
                                focusedLabelColor = UntoldNeonCyan,
                                unfocusedLabelColor = UntoldOnSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = UntoldOnSurface
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = UntoldNeonCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UntoldNeonCyan,
                            unfocusedBorderColor = UntoldOutlineVariant,
                            focusedLabelColor = UntoldNeonCyan,
                            unfocusedLabelColor = UntoldOnSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = UntoldOnSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = UntoldNeonCyan) },
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = null, tint = UntoldOnSurfaceVariant)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UntoldNeonCyan,
                            unfocusedBorderColor = UntoldOutlineVariant,
                            focusedLabelColor = UntoldNeonCyan,
                            unfocusedLabelColor = UntoldOnSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = UntoldOnSurface
                        ),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isLoginTab) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; localError = null },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = UntoldNeonCyan) },
                            trailingIcon = {
                                val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(icon, contentDescription = null, tint = UntoldOnSurfaceVariant)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UntoldNeonCyan,
                                unfocusedBorderColor = UntoldOutlineVariant,
                                focusedLabelColor = UntoldNeonCyan,
                                unfocusedLabelColor = UntoldOnSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = UntoldOnSurface
                            ),
                            singleLine = true,
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Display errors
                    val displayError = localError ?: (authState as? AuthState.Error)?.message
                    if (displayError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = displayError,
                            color = UntoldNeonPink,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            localError = null
                            if (email.isBlank() || password.isBlank()) {
                                localError = "Please fill in all fields"
                                return@Button
                            }
                            if (!isLoginTab) {
                                if (username.isBlank()) {
                                    localError = "Please enter your full name"
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    localError = "Passwords do not match"
                                    return@Button
                                }
                            }

                            if (isLoginTab) {
                                viewModel.login(email, password)
                            } else {
                                viewModel.register(username, email, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(10.dp, spotColor = UntoldNeonPurple, shape = RoundedCornerShape(12.dp)),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isLoginTab) "ENTER PORTAL" else "CREATE ACCOUNT",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}


