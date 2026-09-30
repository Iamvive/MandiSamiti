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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.appwork.mandisamiti.ui.theme.MandiGreenLight
import com.appwork.mandisamiti.ui.theme.MandiGreenPayable
import com.appwork.mandisamiti.ui.theme.MandiRedReceivable
import com.appwork.mandisamiti.ui.theme.MandiSurface
import com.appwork.mandisamiti.ui.theme.MandiTextMuted
import com.appwork.mandisamiti.ui.theme.MandiTextPrimary
import com.appwork.mandisamiti.ui.theme.MandiTextSecondary

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegistrationSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .verticalScroll(rememberScrollState())
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
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, MandiAmberLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
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
                        modifier = Modifier.padding(top = 10.dp),
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

            // Main Card Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF87171))
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
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
                            onOtpChange = viewModel::onOtpChanged,
                            onSubmit = viewModel::verifyOtp,
                            onResend = viewModel::proceedToOtp
                        )
                    }

                    AuthStep.MPIN_SETUP -> {
                        MpinSetupSection(
                            mpin = uiState.mpin,
                            confirmMpin = uiState.confirmMpin,
                            onMpinChange = viewModel::onMpinChanged,
                            onConfirmMpinChange = viewModel::onConfirmMpinChanged,
                            onSubmit = viewModel::completeRegistration
                        )
                    }
                }
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
                label = { Text("मोबाइल नंबर") },
                prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = MandiAmberDark) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MandiAmberPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Shop Name
            OutlinedTextField(
                value = uiState.shopName,
                onValueChange = onShopChange,
                label = { Text("दुकान / फर्म का नाम") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MandiAmberPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Owner / Trader Name
            OutlinedTextField(
                value = uiState.ownerName,
                onValueChange = onOwnerChange,
                label = { Text("व्यापारी / मुनीम का नाम") },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MandiAmberPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // Mandi Name
            OutlinedTextField(
                value = uiState.mandiName,
                onValueChange = onMandiChange,
                label = { Text("मंडी प्रांगण का नाम") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiAmberPrimary)
            ) {
                Text("आगे बढ़ें (OTP प्राप्त करें)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OtpVerificationSection(
    phoneNumber: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit
) {
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
                    .background(MandiGreenLight),
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
                text = "OTP सत्यापन",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MandiTextPrimary
            )

            Text(
                text = "+91 $phoneNumber पर 6-अंकीय सुरक्षा कोड भेजा गया है",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MandiTextSecondary
            )

            OutlinedTextField(
                value = otp,
                onValueChange = onOtpChange,
                label = { Text("6-अंकीय OTP दर्ज करें") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiGreenPayable)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("OTP सत्यापित करें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "OTP पुनः भेजें",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MandiAmberDark,
                modifier = Modifier.clickable { onResend() }
            )
        }
    }
}

@Composable
private fun MpinSetupSection(
    mpin: String,
    confirmMpin: String,
    onMpinChange: (String) -> Unit,
    onConfirmMpinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
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
                onValueChange = onMpinChange,
                label = { Text("नया 4-अंकीय MPIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 6.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = confirmMpin,
                onValueChange = onConfirmMpinChange,
                label = { Text("MPIN पुनः दर्ज करें") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 6.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MandiAmberPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("पंजीयन पूर्ण करें व खाता खोलें", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
