package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.data.repository.AuthState
import com.example.vietforces.ui.theme.TextSecondary
import com.example.vietforces.ui.theme.VietRed
import com.example.vietforces.ui.viewmodel.AuthUiState
import com.example.vietforces.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Navigate away when user becomes authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "🐓 VietForces",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VietRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Học tiếng Việt cùng chiến binh",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Hiện mật khẩu"
                        )
                    }
                }
            )

            // Error message
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            // Login button
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.signIn(email.trim(), password) },
                enabled = email.isNotBlank() && password.isNotBlank() && uiState !is AuthUiState.Loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VietRed)
            ) {
                if (uiState is AuthUiState.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang đăng nhập...")
                    }
                } else {
                    Text("Đăng nhập")
                }
            }

            // Forgot password
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu?", color = VietRed)
            }

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = " hoặc ",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Google Sign-In button
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.signInWithGoogle() },
                enabled = uiState !is AuthUiState.Loading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "G",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = VietRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tiếp tục với Google")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigate to Register
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chưa có tài khoản? ")
                Text(
                    text = "Đăng ký",
                    color = VietRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Password reset dialog
    if (showResetDialog) {
        PasswordResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = { resetEmail ->
                viewModel.resetPassword(resetEmail)
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun PasswordResetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đặt lại mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nhập email của bạn để nhận liên kết đặt lại mật khẩu.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(resetEmail.trim()) },
                enabled = resetEmail.isNotBlank()
            ) {
                Text("Gửi", color = VietRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
