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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Bolt
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
import com.appwork.mandisamiti.ui.theme.MandiPrimaryAction
import com.appwork.mandisamiti.ui.theme.MandiPrimaryActionText
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiSurfaceElevated
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
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
                    if (uiState.step != AuthStep.PHONE_AND_SHOP) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (uiState.step == AuthStep.MPIN_SETUP) viewModel.goBackToOtp()
                                    else viewModel.goBackToDetails()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "पीछे जाएं",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, MandiAmberLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "मंडी समिति",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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
                        StepDot(number = "1", label = "विवरण", isActive = uiState.step == AuthStep.PHONE_AND_SHOP, isDone = uiState.step != AuthStep.PHONE_AND_SHOP)
                        StepDivider()
                        StepDot(number = "2", label = "OTP", isActive = uiState.step == AuthStep.OTP_VERIFICATION, isDone = uiState.step == AuthStep.MPIN_SETUP)
                        StepDivider()
                        StepDot(number = "3", label = "MPIN", isActive = uiState.step == AuthStep.MPIN_SETUP, isDone = false)
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF87171))
                        )
                    ) {
                        Text(
                            text = uiState.generalErrorMessage!!,
                            color = MandiRedReceivable,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                when (uiState.step) {
                    AuthStep.PHONE_AND_SHOP -> {
                        ShopDetailsSection(
                            uiState = uiState,
                            onPhoneChange = viewModel::onPhoneNumberChanged,
                            onShopChange = viewModel::onShopNameChanged,
                            onOwnerChange = viewModel::onOwnerNameChanged,
                            onMandiChange = viewModel::onMandiNameChanged,
                            onSubmit = viewModel::proceedToOtp
                        )
                    }

                    AuthStep.OTP_VERIFICATION -> {
                        OtpVerificationSection(
                            phoneNumber = uiState.phoneNumber,
                            otp = uiState.otp,
                            otpError = uiState.otpError,
                            cooldownSeconds = uiState.resendCooldownSeconds,
                            isResendEnabled = uiState.isResendEnabled,
                            onOtpChange = viewModel::onOtpChanged,
                            onSubmit = viewModel::verifyOtp,
                            onResend = viewModel::resendOtp
                        )
                    }

                    AuthStep.MPIN_SETUP -> {
                        MpinSetupSection(
                            mpin = uiState.mpin,
                            confirmMpin = uiState.confirmMpin,
                            confirmError = uiState.confirmMpinError,
                            isLoading = uiState.isLoading,
                            isMpinValid = uiState.isMpinValid,
                            onMpinChange = viewModel::onMpinChanged,
                            onConfirmMpinChange = viewModel::onConfirmMpinChanged,
                            onSubmit = viewModel::completeRegistration
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ShopDetailsSection(
    uiState: RegisterUiState,
    onPhoneChange: (String) -> Unit,
    onShopChange: (String) -> Unit,
    onOwnerChange: (String) -> Unit,
    onMandiChange: (String) -> Unit,
    onSubmit: () -> Unit
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "दुकान व फर्म का विवरण",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            // Mobile Number
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = onPhoneChange,
                label = { Text("मोबाइल नंबर *") },
                placeholder = { Text("उदा. 9837123456", color = MandiTextMuted) },
                prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = MandiAmberDark) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MandiAmberPrimary) },
                isError = uiState.phoneError != null,
                supportingText = {
                    if (uiState.phoneError != null) {
                        Text(uiState.phoneError!!, color = MandiRedReceivable, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Shop Name
            OutlinedTextField(
                value = uiState.shopName,
                onValueChange = onShopChange,
                label = { Text("दुकान / फर्म का नाम *") },
                placeholder = { Text("उदा. श्री गणेश ट्रेडिंग", color = MandiTextMuted) },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MandiAmberPrimary) },
                isError = uiState.shopNameError != null,
                supportingText = {
                    if (uiState.shopNameError != null) {
                        Text(uiState.shopNameError!!, color = MandiRedReceivable, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Owner / Trader Name
            OutlinedTextField(
                value = uiState.ownerName,
                onValueChange = onOwnerChange,
                label = { Text("व्यापारी / मुनीम का नाम *") },
                placeholder = { Text("उदा. लाला मदन लाल जी", color = MandiTextMuted) },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MandiAmberPrimary) },
                isError = uiState.ownerNameError != null,
                supportingText = {
                    if (uiState.ownerNameError != null) {
                        Text(uiState.ownerNameError!!, color = MandiRedReceivable, fontSize = 11.sp)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Mandi Name
            OutlinedTextField(
                value = uiState.mandiName,
                onValueChange = onMandiChange,
                label = { Text("मंडी प्रांगण का नाम") },
                placeholder = { Text("उदा. मथुरा कृषि उपज मंडी", color = MandiTextMuted) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isStep1Valid) onSubmit()
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(4.dp))

            val buttonTextColor = if (uiState.isStep1Valid) MandiPrimaryActionText else MandiTextMuted
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                enabled = uiState.isStep1Valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText,
                    disabledContainerColor = MandiSurfaceElevated,
                    disabledContentColor = MandiTextMuted
                )
            ) {
                Text(
                    text = "आगे बढ़ें (OTP प्राप्त करें)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonTextColor
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = buttonTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
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
                placeholder = { Text("1 2 3 4 5 6", color = MandiTextMuted, textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (otp.length == 6) onSubmit()
                    }
                ),
                singleLine = true,
                isError = otpError != null,
                supportingText = {
                    if (otpError != null) {
                        Text(otpError, color = MandiRedReceivable, fontSize = 11.sp)
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

            val otpButtonTextColor = if (otp.length == 6) MandiBtnSuccessFg else MandiTextMuted
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                enabled = otp.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiBtnSuccessBg,
                    contentColor = MandiBtnSuccessFg,
                    disabledContainerColor = MandiSurfaceElevated,
                    disabledContentColor = MandiTextMuted
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = otpButtonTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "OTP सत्यापित करें",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = otpButtonTextColor
                )
            }

            // Resend Countdown Throttler
            if (isResendEnabled) {
                Text(
                    text = "OTP पुनः भेजें",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MandiAmberDark,
                    modifier = Modifier.clickable { onResend() }
                )
            } else {
                Text(
                    text = "OTP पुनः भेजें (${cooldownSeconds}s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MandiTextMuted
                )
            }
        }
    }
}

@Composable
private fun MpinSetupSection(
    mpin: String,
    confirmMpin: String,
    confirmError: String?,
    isLoading: Boolean,
    isMpinValid: Boolean,
    onMpinChange: (String) -> Unit,
    onConfirmMpinChange: (String) -> Unit,
    onSubmit: () -> Unit
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
                    .background(MandiAmberLight.copy(alpha = 0.5f)),
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
                text = "4-अंकों का सुरक्षा MPIN बनाएं",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            Text(
                text = "दैनिक मंडी खाता खोलने के लिए 4 अंकों का गुप्त पिन चुनें",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MandiTextSecondary
            )

            OutlinedTextField(
                value = mpin,
                onValueChange = { if (it.length <= 4) onMpinChange(it) },
                label = { Text("नया 4-अंकीय MPIN *") },
                placeholder = { Text("• • • •", color = MandiTextMuted, textAlign = TextAlign.Center) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = confirmMpin,
                onValueChange = { if (it.length <= 4) onConfirmMpinChange(it) },
                label = { Text("MPIN पुनः दर्ज करें *") },
                placeholder = { Text("• • • •", color = MandiTextMuted, textAlign = TextAlign.Center) },
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmError != null,
                supportingText = {
                    if (confirmError != null) {
                        Text(confirmError, color = MandiRedReceivable, fontSize = 11.sp)
                    } else if (confirmMpin.length == 4 && confirmMpin == mpin) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MandiGreenPayable,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("MPIN मेल खा गया है", color = MandiGreenPayable, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isMpinValid && !isLoading) onSubmit()
                    }
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            val mpinButtonTextColor = if (isMpinValid && !isLoading) MandiPrimaryActionText else MandiTextMuted
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                },
                enabled = isMpinValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MandiPrimaryAction,
                    contentColor = MandiPrimaryActionText,
                    disabledContainerColor = MandiSurfaceElevated,
                    disabledContentColor = MandiTextMuted
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MandiPrimaryActionText, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = mpinButtonTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "पंजीयन पूर्ण करें व खाता खोलें",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = mpinButtonTextColor
                    )
                }
            }
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
                .background(if (isActive || isDone) Color.White else Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive || isDone) MandiAmberDark else Color.White
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StepDivider() {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.4f))
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MandiAmberPrimary,
    unfocusedBorderColor = MandiBorder,
    focusedLabelColor = MandiAmberDark,
    unfocusedLabelColor = MandiTextSecondary,
    cursorColor = MandiAmberPrimary
)
