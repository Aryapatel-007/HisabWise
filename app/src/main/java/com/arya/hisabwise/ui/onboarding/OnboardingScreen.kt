package com.arya.hisabwise.ui.onboarding

import android.app.Activity
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.arya.hisabwise.ui.theme.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.arya.hisabwise.ui.auth.AuthChoiceViewModel
import com.arya.hisabwise.ui.auth.AuthState

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    authViewModel: AuthChoiceViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var nameTouched by remember { mutableStateOf(false) }
    var phoneTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }

    val nameValid = name.trim().isNotEmpty()
    val phoneValid = phone.length == 10 && phone.all { it.isDigit() }
    val passwordValid = password.length >= 6
    val isButtonEnabled = nameValid && phoneValid && passwordValid

    val focusManager = LocalFocusManager.current
    
    val onboardingState by viewModel.onboardingState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val authState by authViewModel.authState.collectAsState()
    val authErrorMessage by authViewModel.errorMessage.collectAsState()
    
    var showLoginPopup by remember { mutableStateOf(false) }

    LaunchedEffect(onboardingState) {
        if (onboardingState == OnboardingState.SUCCESS) {
            onGetStarted()
        }
    }

    LaunchedEffect(authState) {
        if (authState == AuthState.SUCCESS) {
            showLoginPopup = false
            authViewModel.resetState()
            onGetStarted()
        }
    }

    GlowingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ZONE 1 - Top 1/4
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 40.dp),
                contentAlignment = Alignment.BottomStart
            ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    text = "HisabWise",
                    color = PrimaryText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Split expenses, stay fair.",
                    color = MutedText,
                    fontSize = 14.sp
                )
            }
        }

        // ZONE 2 - Middle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ReqBox(
                label = "Name",
                value = name,
                placeholder = "e.g. Arya",
                onValueChange = { name = it },
                onFocusChange = { hasFocus ->
                    if (!hasFocus && name.isNotEmpty()) nameTouched = true
                    if (hasFocus) nameTouched = true
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )
            if (nameTouched && !nameValid) {
                Text(
                    text = "Name cannot be empty",
                    color = NegativeColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ReqBox(
                label = "Number",
                value = phone,
                placeholder = "10-digit mobile",
                onValueChange = { if (it.length <= 10) phone = it },
                onFocusChange = { hasFocus ->
                    if (!hasFocus && phone.isNotEmpty()) phoneTouched = true
                    if (hasFocus) phoneTouched = true
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            if (phoneTouched && !phoneValid) {
                Text(
                    text = "Enter a valid 10-digit number",
                    color = NegativeColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            ReqBox(
                label = "Password",
                value = password,
                placeholder = "Min 6 chars",
                onValueChange = { password = it },
                onFocusChange = { hasFocus ->
                    if (!hasFocus && password.isNotEmpty()) passwordTouched = true
                    if (hasFocus) passwordTouched = true
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                isPassword = true
            )
            if (passwordTouched && !passwordValid) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = NegativeColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = NegativeColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }

        // ZONE 3 - Bottom 1/4
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isButtonEnabled && onboardingState != OnboardingState.LOADING) PrimaryAccent else PrimaryAccent.copy(alpha = 0.38f))
                        .bounceClick(enabled = isButtonEnabled && onboardingState != OnboardingState.LOADING) {
                            viewModel.registerUser(name.trim(), phone, password)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Get Started",
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Get Started",
                            tint = PrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Already have an account? Login",
                    color = PrimaryAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .bounceClick { showLoginPopup = true }
                        .padding(8.dp)
                )
            }
        }
    }

    if (showLoginPopup) {
        LoginPopupDialog(
            viewModel = authViewModel,
            authState = authState,
            errorMessage = authErrorMessage,
            onDismiss = {
                showLoginPopup = false
                authViewModel.resetState()
            }
        )
    }
}
}

@Composable
fun ReqBox(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SurfaceColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SurfaceVariantColor),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                textStyle = TextStyle(
                    color = PrimaryText,
                    fontSize = 14.sp
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(PrimaryAccent),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MutedText,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun LoginPopupDialog(
    viewModel: AuthChoiceViewModel,
    authState: AuthState,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    var isOtpState by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariantColor)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isOtpState) "Verify OTP" else "Login",
                    color = PrimaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = NegativeColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (!isOtpState) {
                    AuthInputBox(
                        value = number,
                        placeholder = "Number",
                        onValueChange = { number = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthInputBox(
                        value = password,
                        placeholder = "Password",
                        onValueChange = { password = it },
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "If you forget password contact Arya",
                        color = MutedText,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                    )
                    
                    if (authState == AuthState.LOADING) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    } else {
                        DialogButton("Login with Password") {
                            viewModel.loginWithPassword(number, password)
                        }
                    }
                } else {
                    Text(
                        text = "OTP sent to $number",
                        color = MutedText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    AuthInputBox(
                        value = otpCode,
                        placeholder = "6-digit OTP",
                        onValueChange = { otpCode = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (authState == AuthState.LOADING) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    } else {
                        DialogButton("Verify OTP") {
                            viewModel.verifyOtp(otpCode, number)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PrimaryAccent)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuthInputBox(
    value: String, 
    placeholder: String, 
    onValueChange: (String) -> Unit, 
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceColor)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(PrimaryAccent),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = placeholder, color = MutedText, fontSize = 14.sp)
                }
                innerTextField()
            }
        )
}}
