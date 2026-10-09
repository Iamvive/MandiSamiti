package com.appwork.mandisamiti.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appwork.mandisamiti.ui.theme.MandiAmberDark
import com.appwork.mandisamiti.ui.theme.MandiAmberLight
import com.appwork.mandisamiti.ui.theme.MandiAmberPrimary
import com.appwork.mandisamiti.ui.theme.MandiBackground
import com.appwork.mandisamiti.ui.theme.MandiBorder
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessBg
import com.appwork.mandisamiti.ui.theme.MandiBtnSuccessFg
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiGreenText
import com.appwork.mandisamiti.ui.theme.MandiRedBorder
import com.appwork.mandisamiti.ui.theme.MandiRedLight
import com.appwork.mandisamiti.ui.theme.MandiRedText
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistrationSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isRegistrationComplete) {
        if (uiState.isRegistrationComplete) {
            onRegistrationSuccess()
        }
    }

    Scaffold(
        containerColor = MandiBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            // Saffron Top Brand Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(MandiAmberDark, MandiAmberPrimary)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.step != AuthStep.PHONE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.goBackToPhone()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "पीछे जाएं",
                                    tint = MandiPrimaryActionText
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MandiPrimaryActionText.copy(alpha = 0.2f))
                            .border(2.dp, MandiAmberLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MandiPrimaryActionText,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "मंडी समिति",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MandiPrimaryActionText
                    )

                    Text(
                        text = "आढ़त, व्यापार व दैनिक रोकड़ बहीखाता",
                        fontSize = 13.sp,
                        color = MandiAmberLight,
                        fontWeight = FontWeight.Medium
                    )

                    // Step Indicator
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepDot(number = "1", label = "फ़ोन", isActive = uiState.step == AuthStep.PHONE, isDone = uiState.step != AuthStep.PHONE)
                        StepDivider()
                        StepDot(number = "2", label = "OTP", isActive = uiState.step == AuthStep.OTP, isDone = uiState.step == AuthStep.NEW_SHOP || uiState.step == AuthStep.ENTER_MPIN)
                        StepDivider()
                        StepDot(number = "3", label = "MPIN", isActive = uiState.step == AuthStep.NEW_SHOP || uiState.step == AuthStep.ENTER_MPIN, isDone = false)
                    }
                }
            }

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.generalErrorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MandiRedLight),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MandiRedBorder)
                        )
                    ) {
                        Text(
                            text = uiState.generalErrorMessage!!,
                            color = MandiRedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                when (uiState.step) {
                    AuthStep.PHONE -> {
                        PhoneSection(
                            phoneNumber = uiState.phoneNumber,
                            phoneError = uiState.phoneError,
                            isValid = uiState.isStep1Valid,
                            isLoading = uiState.isLoading,
                            onPhoneChange = viewModel::onPhoneNumberChanged,
                            onSubmit = viewModel::submitPhone
                        )
                    }

                    AuthStep.OTP -> {
                        OtpVerificationSection(
                            phoneNumber = uiState.phoneNumber,
                            otp = uiState.otp,
                            otpError = uiState.otpError,
                            cooldownSeconds = uiState.resendCooldownSeconds,
                            isResendEnabled = uiState.isResendEnabled,
                            isLoading = uiState.isLoading,
                            onOtpChange = viewModel::onOtpChanged,
                            onSubmit = viewModel::submitOtp,
                            onResend = viewModel::resendOtp
                        )
                    }

                    AuthStep.NEW_SHOP -> {
                        NewShopSection(
                            uiState = uiState,
                            onShopChange = viewModel::onShopNameChanged,
                            onOwnerChange = viewModel::onOwnerNameChanged,
                            onMandiChange = viewModel::onMandiNameChanged,
                            onMpinChange = viewModel::onMpinChanged,
                            onConfirmMpinChange = viewModel::onConfirmMpinChanged,
                            onSubmit = viewModel::submitNewShop
                        )
                    }

                    AuthStep.ENTER_MPIN -> {
                        EnterMpinSection(
                            mpin = uiState.mpin,
                            isLoading = uiState.isLoading,
                            isValid = uiState.isEnterMpinValid,
                            errorMessage = uiState.generalErrorMessage,
                            onMpinChange = viewModel::onMpinChanged,
                            onSubmit = viewModel::submitMpin
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


@Composable
private fun AuthCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
        )
    ) {
        content()
    }
}

@Composable
private fun AuthActionButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    containerColor: Color = MandiPrimaryAction,
    contentColor: Color = MandiPrimaryActionText
) {
    val focusManager = LocalFocusManager.current
    val active = enabled && !isLoading
    val textColor = if (active) contentColor else MandiTextSecondary
    Button(
        onClick = {
            focusManager.clearFocus()
            onClick()
        },
        enabled = active,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MandiSurfaceElevated,
            disabledContentColor = MandiTextSecondary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MandiTextSecondary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.size(8.dp))
                Icon(trailingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun mpinTextStyle() = androidx.compose.ui.text.TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    letterSpacing = 8.sp
)

@Composable
private fun PhoneSection(
    phoneNumber: String,
    phoneError: String?,
    isValid: Boolean,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "अपना मोबाइल नंबर दर्ज करें",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("मोबाइल नंबर *") },
                placeholder = { Text("उदा. 9837123456", color = MandiTextSecondary) },
                prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = MandiAmberDark) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MandiAmberPrimary) },
                isError = phoneError != null,
                supportingText = {
                    if (phoneError != null) {
                        Text(phoneError, color = MandiRedText, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isValid && !isLoading) onSubmit()
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            AuthActionButton(
                text = "OTP भेजें",
                enabled = isValid,
                isLoading = isLoading,
                onClick = onSubmit,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

@Composable
private fun OtpVerificationSection(
    phoneNumber: String,
    otp: String,
    otpError: String?,
    cooldownSeconds: Int,
    isResendEnabled: Boolean,
    isLoading: Boolean,
    onOtpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MandiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MandiBorder)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MandiGreenLight.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MandiGreenPayable,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "सुरक्षा कोड दर्ज करें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            Text(
                text = "+91 $phoneNumber पर 6-अंकों का OTP भेजा गया है",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MandiTextSecondary
            )

            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) onOtpChange(it) },
                label = { Text("6-अंकीय OTP कोड") },
                placeholder = { Text("1 2 3 4 5 6", color = MandiTextSecondary, textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (otp.length == 6 && !isLoading) onSubmit()
                    }
                ),
                singleLine = true,
                isError = otpError != null,
                supportingText = {
                    if (otpError != null) {
                        Text(otpError, color = MandiRedText, fontSize = 11.sp)
                    }
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 6.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            AuthActionButton(
                text = "OTP सत्यापित करें",
                enabled = otp.length == 6,
                isLoading = isLoading,
                onClick = onSubmit,
                leadingIcon = Icons.Default.Check,
                containerColor = MandiBtnSuccessBg,
                contentColor = MandiBtnSuccessFg
            )

            // Resend Countdown Throttler
            if (isResendEnabled && !isLoading) {
                Text(
                    text = "OTP पुनः भेजें",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiAmberDark,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onResend() }
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            } else {
                Text(
                    text = if (isResendEnabled) "OTP पुनः भेजें" else "OTP पुनः भेजें (${cooldownSeconds}s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextSecondary
                )
            }
        }
    }
}


@Composable
private fun NewShopSection(
    uiState: RegisterUiState,
    onShopChange: (String) -> Unit,
    onOwnerChange: (String) -> Unit,
    onMandiChange: (String) -> Unit,
    onMpinChange: (String) -> Unit,
    onConfirmMpinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val mpin = uiState.mpin
    val confirmMpin = uiState.confirmMpin
    val confirmError = uiState.confirmMpinError

    AuthCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "दुकान व फर्म का विवरण",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            OutlinedTextField(
                value = uiState.shopName,
                onValueChange = onShopChange,
                label = { Text("दुकान / फर्म का नाम *") },
                placeholder = { Text("उदा. श्री गणेश ट्रेडिंग", color = MandiTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MandiAmberPrimary) },
                isError = uiState.shopNameError != null,
                supportingText = {
                    if (uiState.shopNameError != null) {
                        Text(uiState.shopNameError!!, color = MandiRedText, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = uiState.ownerName,
                onValueChange = onOwnerChange,
                label = { Text("व्यापारी / मुनीम का नाम *") },
                placeholder = { Text("उदा. लाला मदन लाल जी", color = MandiTextSecondary) },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MandiAmberPrimary) },
                isError = uiState.ownerNameError != null,
                supportingText = {
                    if (uiState.ownerNameError != null) {
                        Text(uiState.ownerNameError!!, color = MandiRedText, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = uiState.mandiName,
                onValueChange = onMandiChange,
                label = { Text("मंडी प्रांगण का नाम") },
                placeholder = { Text("उदा. मथुरा कृषि उपज मंडी", color = MandiTextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Text(
                text = "4-अंकों का सुरक्षा MPIN बनाएं",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            OutlinedTextField(
                value = mpin,
                onValueChange = { if (it.length <= 4) onMpinChange(it) },
                label = { Text("नया 4-अंकीय MPIN *") },
                placeholder = { Text("• • • •", color = MandiTextSecondary, textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MandiAmberPrimary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                textStyle = mpinTextStyle(),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = confirmMpin,
                onValueChange = { if (it.length <= 4) onConfirmMpinChange(it) },
                label = { Text("MPIN पुनः दर्ज करें *") },
                placeholder = { Text("• • • •", color = MandiTextSecondary, textAlign = TextAlign.Center) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MandiAmberPrimary) },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmError != null,
                supportingText = {
                    if (confirmError != null) {
                        Text(confirmError, color = MandiRedText, fontSize = 11.sp)
                    } else if (confirmMpin.length == 4 && confirmMpin == mpin) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MandiGreenText,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("MPIN मेल खा गया है", color = MandiGreenText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isNewShopValid && !uiState.isLoading) onSubmit()
                    }
                ),
                singleLine = true,
                textStyle = mpinTextStyle(),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            AuthActionButton(
                text = "दुकान बनाएँ",
                enabled = uiState.isNewShopValid,
                isLoading = uiState.isLoading,
                onClick = onSubmit,
                leadingIcon = Icons.Default.Check
            )
        }
    }
}

@Composable
private fun EnterMpinSection(
    mpin: String,
    isLoading: Boolean,
    isValid: Boolean,
    errorMessage: String?,
    onMpinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthCard {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MandiAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MandiAmberDark,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "अपना MPIN दर्ज करें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            Text(
                text = "खाते में लॉगिन करने के लिए अपना 4 अंकों का गुप्त पिन डालें",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MandiTextSecondary
            )

            OutlinedTextField(
                value = mpin,
                onValueChange = { if (it.length <= 4) onMpinChange(it) },
                label = { Text("4-अंकीय MPIN *") },
                placeholder = { Text("• • • •", color = MandiTextSecondary, textAlign = TextAlign.Center) },
                visualTransformation = PasswordVisualTransformation(),
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Text(errorMessage, color = MandiRedText, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isValid && !isLoading) onSubmit()
                    }
                ),
                singleLine = true,
                textStyle = mpinTextStyle(),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            AuthActionButton(
                text = "लॉगिन करें",
                enabled = isValid,
                isLoading = isLoading,
                onClick = onSubmit,
                leadingIcon = Icons.Default.Check
            )
        }
    }
}

@Composable
private fun StepDot(number: String, label: String, isActive: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isActive || isDone) MandiPrimaryActionText else MandiPrimaryActionText.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive || isDone) MandiAmberDark else MandiPrimaryActionText
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MandiPrimaryActionText else MandiPrimaryActionText.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StepDivider() {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(1.dp)
            .background(MandiPrimaryActionText.copy(alpha = 0.4f))
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MandiAmberPrimary,
    unfocusedBorderColor = MandiBorder,
    focusedLabelColor = MandiAmberDark,
    unfocusedLabelColor = MandiTextSecondary,
    cursorColor = MandiAmberPrimary,
    focusedTextColor = MandiTextPrimary,
    unfocusedTextColor = MandiTextPrimary,
    errorTextColor = MandiTextPrimary,
    disabledTextColor = MandiTextSecondary,
    focusedPlaceholderColor = MandiTextSecondary,
    unfocusedPlaceholderColor = MandiTextSecondary,
    errorPlaceholderColor = MandiTextSecondary,
    errorLabelColor = MandiRedText,
    errorBorderColor = MandiRedText,
    errorCursorColor = MandiRedText,
    focusedSupportingTextColor = MandiTextSecondary,
    unfocusedSupportingTextColor = MandiTextSecondary,
    errorSupportingTextColor = MandiRedText,
    focusedLeadingIconColor = MandiTextSecondary,
    unfocusedLeadingIconColor = MandiTextSecondary,
    errorLeadingIconColor = MandiRedText,
    focusedPrefixColor = MandiTextPrimary,
    unfocusedPrefixColor = MandiTextPrimary,
    errorPrefixColor = MandiTextPrimary
)
