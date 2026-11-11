package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignUpSuccess: (UserEntity) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
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
                text = "Create an account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NawanoTextPrimaryLight
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign up now and start exploring.",
                fontSize = 14.sp,
                color = NawanoTextSecondaryLight
            )

            Spacer(Modifier.height(28.dp))

            AuthTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                placeholder = "Esther Howard",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(Modifier.height(16.dp))

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "estherhoward@gmail.com",
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
                        if (fullName.isNotBlank() && email.isNotBlank() && password.length >= 6) {
                            viewModel.signUp(fullName, email, password, onSignUpSuccess)
                        }
                    }
                )
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "By clicking on \"Create Account\" you agree to Nawano's Terms & Conditions and Privacy Policy.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = NawanoTextSecondaryLight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

            val isFormValid = fullName.isNotBlank() && email.isNotBlank() && password.length >= 6

            AuthPrimaryButton(
                text = "Create Account",
                enabled = isFormValid,
                isLoading = uiState.isLoading,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signUp(fullName, email, password, onSignUpSuccess)
                }
            )

            Spacer(Modifier.height(28.dp))

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
            }

            Spacer(Modifier.height(36.dp))

            AuthBottomLink(
                promptText = "Already have an account?",
                actionText = "Login.",
                onActionClick = onNavigateToLogin
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
