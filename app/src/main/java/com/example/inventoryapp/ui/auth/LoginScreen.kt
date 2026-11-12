package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoPrimary100
import com.example.inventoryapp.ui.theme.NawanoPrimary50
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoRose
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (UserEntity) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToResetPassword: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NawanoLightSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            AuthTopBar(onBack = onBack)

            Spacer(Modifier.height(8.dp))
            AuthLogoHeader()

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NawanoTextPrimaryLight
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "to Nawano Inventory",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NawanoPrimary200
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Login to your account.",
                fontSize = 14.sp,
                color = NawanoTextSecondaryLight
            )

            Spacer(Modifier.height(28.dp))

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "yourname@domain.com",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email, password, onLoginSuccess)
                        }
                    }
                )
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NawanoPrimary200,
                    modifier = Modifier.clickable { onNavigateToResetPassword() }
                )
            }

            Spacer(Modifier.height(28.dp))

            val isFormValid = email.isNotBlank() && password.isNotBlank()

            AuthPrimaryButton(
                text = "Login",
                enabled = isFormValid,
                isLoading = uiState.isLoading,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login(email, password, onLoginSuccess)
                }
            )

            Spacer(Modifier.height(20.dp))

            // Quick Demo Login (One-Tap & Test Creds)
            Surface(
                color = NawanoPrimary50,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NawanoPrimary100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quick Demo Login",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.quickDemoLogin(UserRole.OWNER, onLoginSuccess)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NawanoPrimary200),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = NawanoLightSurface,
                                contentColor = NawanoPrimary200
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Middle Man", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.quickDemoLogin(UserRole.SUPPLIER, onLoginSuccess)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NawanoPrimary200),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = NawanoLightSurface,
                                contentColor = NawanoPrimary200
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Supplier", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Quick Demo Auto-Fill (Tap to Test):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NawanoTextSecondaryLight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                email = "middleman@nawano.com"
                                password = "password123"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = NawanoPrimary200.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🏢 Middleman HQ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                email = "supplier@nawano.com"
                                password = "password123"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🚚 Supplier Portal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AuthOrDivider()

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialAuthButton(
                    painter = painterResource(R.drawable.ic_google_logo),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Google sign-in coming soon in cloud sync update")
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                SocialAuthButton(
                    painter = painterResource(R.drawable.ic_apple_logo),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Apple sign-in coming soon")
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                SocialAuthButton(
                    text = "SSO",
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Enterprise SSO coming soon")
                        }
                    }
                )
            }

            Spacer(Modifier.height(40.dp))

            AuthBottomLink(
                promptText = "New here?",
                actionText = "Create an account.",
                onActionClick = onNavigateToSignUp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
