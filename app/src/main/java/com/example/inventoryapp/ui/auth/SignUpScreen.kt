package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
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
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(com.example.inventoryapp.data.local.entity.UserRole.OWNER) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }

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

            // Role selection: Middle Man vs Supplier
            Text(
                text = "Account Type",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NawanoTextPrimaryLight
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isMiddleman = selectedRole == com.example.inventoryapp.data.local.entity.UserRole.OWNER
                androidx.compose.material3.Surface(
                    onClick = { selectedRole = com.example.inventoryapp.data.local.entity.UserRole.OWNER },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = if (isMiddleman) NawanoPrimary200 else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isMiddleman) NawanoPrimary200 else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Middle Man",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isMiddleman) Color.White else NawanoTextPrimaryLight
                        )
                        Text(
                            text = "Buyer / Retailer",
                            fontSize = 11.sp,
                            color = if (isMiddleman) Color.White.copy(alpha = 0.85f) else NawanoTextSecondaryLight
                        )
                    }
                }

                val isSupplier = selectedRole == com.example.inventoryapp.data.local.entity.UserRole.SUPPLIER
                androidx.compose.material3.Surface(
                    onClick = { selectedRole = com.example.inventoryapp.data.local.entity.UserRole.SUPPLIER },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = if (isSupplier) NawanoPrimary200 else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSupplier) NawanoPrimary200 else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Supplier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSupplier) Color.White else NawanoTextPrimaryLight
                        )
                        Text(
                            text = "Vendor / Distributor",
                            fontSize = 11.sp,
                            color = if (isSupplier) Color.White.copy(alpha = 0.85f) else NawanoTextSecondaryLight
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Phone Number",
                placeholder = "+1 (555) 019-2831",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
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
                            viewModel.signUp(
                                fullName = fullName,
                                email = email,
                                password = password,
                                role = selectedRole,
                                phoneNumber = phoneNumber,
                                onSuccess = onSignUpSuccess
                            )
                        }
                    }
                )
            )

            Spacer(Modifier.height(14.dp))

            // Terms and Conditions agreement checkbox & clickable sheet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Checkbox(
                    checked = isTermsAccepted,
                    onCheckedChange = { isTermsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NawanoPrimary200,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "I have read & agree to the ",
                        fontSize = 12.sp,
                        color = NawanoTextSecondaryLight
                    )
                    Text(
                        text = "Terms & Rules",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        modifier = Modifier.clickable { showTermsSheet = true }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            val isFormValid = fullName.isNotBlank() && email.isNotBlank() && password.length >= 6 && isTermsAccepted

            AuthPrimaryButton(
                text = "Create Account",
                enabled = isFormValid,
                isLoading = uiState.isLoading,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signUp(
                        fullName = fullName,
                        email = email,
                        password = password,
                        role = selectedRole,
                        phoneNumber = phoneNumber,
                        onSuccess = onSignUpSuccess
                    )
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

        if (showTermsSheet) {
            TermsAndConditionsSheet(
                onDismiss = { showTermsSheet = false },
                onAccept = {
                    isTermsAccepted = true
                    showTermsSheet = false
                }
            )
        }
    }
}
