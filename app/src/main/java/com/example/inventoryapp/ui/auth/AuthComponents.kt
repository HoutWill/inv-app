package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.theme.NawanoLightOutline
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight

@Composable
fun AuthTopBar(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NawanoTextPrimaryLight
                )
            }
        }
    }
}

@Composable
fun AuthLogoHeader(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(R.drawable.ic_nawano_logo),
        contentDescription = "Nawano Logo",
        modifier = modifier.size(52.dp)
    )
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = label,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 14.sp, color = NawanoTextSecondaryLight.copy(alpha = 0.6f)) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = NawanoTextSecondaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NawanoPrimary200,
            unfocusedBorderColor = NawanoLightOutline,
            focusedLabelColor = NawanoPrimary200,
            unfocusedLabelColor = NawanoTextSecondaryLight,
            focusedTextColor = NawanoTextPrimaryLight,
            unfocusedTextColor = NawanoTextPrimaryLight,
            cursorColor = NawanoPrimary200,
            focusedContainerColor = NawanoLightSurface,
            unfocusedContainerColor = NawanoLightSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NawanoPrimary200,
            contentColor = Color.White,
            disabledContainerColor = NawanoPrimary200.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AuthOrDivider(
    text: String = "Or continue with",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = NawanoLightOutline
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = NawanoTextSecondaryLight,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = NawanoLightOutline
        )
    }
}

@Composable
fun SocialAuthButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    text: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = NawanoLightSurface,
        border = BorderStroke(1.dp, NawanoLightOutline),
        shadowElevation = 0.dp,
        modifier = modifier
            .height(48.dp)
            .width(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            } else if (text != null) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NawanoTextPrimaryLight
                )
            }
        }
    }
}

@Composable
fun AuthBottomLink(
    promptText: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = promptText,
            fontSize = 13.sp,
            color = NawanoTextSecondaryLight
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = actionText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = NawanoPrimary200,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}
