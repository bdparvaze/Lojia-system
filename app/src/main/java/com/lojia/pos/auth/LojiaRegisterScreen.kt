package com.lojia.pos.auth

import android.content.Context
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.data.PreferencesRepository
import com.lojia.pos.data.UserProfile
import com.lojia.pos.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LojiaRegisterScreen(
    userProfile: UserProfile?,
    isBn: Boolean,
    onRegisterSuccess: (UserProfile) -> Unit,
    onGoToLogin: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }

    // Registration input states
    var rFn by remember { mutableStateOf("") }
    var rLn by remember { mutableStateOf("") }
    var rUn by remember { mutableStateOf("") }
    var rEm by remember { mutableStateOf("") }
    var phoneNum by remember { mutableStateOf("") }

    // Country Detection & Selection
    val defaultCountry = remember {
        var detectedCountry: LojiaCountry? = null
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso?.lowercase()
            val netCountry = tm?.networkCountryIso?.lowercase()
            val localeCountry = context.resources.configuration.locales[0]?.country?.lowercase()
            val iso = when {
                !simCountry.isNullOrEmpty() -> simCountry
                !netCountry.isNullOrEmpty() -> netCountry
                !localeCountry.isNullOrEmpty() -> localeCountry
                else -> if (isBn) "bd" else "sa"
            }
            detectedCountry = LOJIA_COUNTRIES.find { it.code.lowercase() == iso }
        } catch (_: Exception) {}
        detectedCountry ?: LOJIA_COUNTRIES.find { it.code == (if (isBn) "BD" else "SA") } ?: LOJIA_COUNTRIES[0]
    }
    var selectedCountry by remember { mutableStateOf(defaultCountry) }
    var showCountryPicker by remember { mutableStateOf(false) }

    var rPw by remember { mutableStateOf("") }
    var rPwVisible by remember { mutableStateOf(false) }
    var rCp by remember { mutableStateOf("") }
    var rCpVisible by remember { mutableStateOf(false) }
    var rSq by remember { mutableStateOf("") }
    var rSa by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }
    var isProcessingReg by remember { mutableStateOf(false) }

    // Validation states
    var vFn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vLn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vUn by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vEm by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vPhone by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vPw by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vCp by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vSq by remember { mutableStateOf(FieldValidationState.DEFAULT) }
    var vSa by remember { mutableStateOf(FieldValidationState.DEFAULT) }

    LaunchedEffect(rFn, rLn, rUn, rEm, phoneNum, rPw, rCp, rSq, rSa) {
        vFn = if (rFn.isEmpty()) FieldValidationState.DEFAULT else if (rFn.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        vLn = if (rLn.isEmpty()) FieldValidationState.DEFAULT else if (rLn.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        vUn = if (rUn.isEmpty()) FieldValidationState.DEFAULT else {
            val u = rUn.trim()
            if (u.length in 3..20 && u.all { it.isLetterOrDigit() || it == '_' }) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vEm = if (rEm.isEmpty()) FieldValidationState.DEFAULT else {
            val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
            if (emailRegex.matches(rEm.trim())) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vPhone = if (phoneNum.isEmpty()) FieldValidationState.DEFAULT else {
            val digits = phoneNum.filter { it.isDigit() }
            if (digits.length in 7..15) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vPw = if (rPw.isEmpty()) FieldValidationState.DEFAULT else {
            if (rPw.length >= 8) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vCp = if (rCp.isEmpty()) FieldValidationState.DEFAULT else {
            if (rCp == rPw && rPw.isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
        vSq = if (rSq.isEmpty()) FieldValidationState.DEFAULT else FieldValidationState.SUCCESS
        vSa = if (rSa.isEmpty()) FieldValidationState.DEFAULT else {
            if (rSa.trim().isNotEmpty()) FieldValidationState.SUCCESS else FieldValidationState.ERROR
        }
    }

    val isFnValid = rFn.trim().isNotEmpty()
    val isLnValid = rLn.trim().isNotEmpty()
    val isUnValid = rUn.trim().length in 3..20 && rUn.trim().all { it.isLetterOrDigit() || it == '_' }
    val isEmValid = rEm.trim().isNotEmpty() && "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex().matches(rEm.trim())
    val isPhoneValid = phoneNum.filter { it.isDigit() }.length in 7..15
    val isPwValid = rPw.length >= 8
    val isCpValid = rCp.isNotEmpty() && rCp == rPw
    val isSqValid = rSq.isNotBlank()
    val isSaValid = rSa.trim().isNotEmpty()
    val isTermsValid = agreeTerms

    val regRequirements = remember(isFnValid, isLnValid, isUnValid, isEmValid, isPhoneValid, isPwValid, isCpValid, isSqValid, isSaValid, isTermsValid, isBn) {
        listOf(
            Triple(LojiaStrings.get("fieldFirstName", isBn), isFnValid, if (isBn) "লিখুন" else "Enter"),
            Triple(LojiaStrings.get("fieldLastName", isBn), isLnValid, if (isBn) "লিখুন" else "Enter"),
            Triple(LojiaStrings.get("fieldUsername", isBn), isUnValid, if (isBn) "৩–২০ অক্ষর" else "3–20 chars"),
            Triple(LojiaStrings.get("fieldEmail", isBn), isEmValid, if (isBn) "সঠিক ফরম্যাট" else "Valid format"),
            Triple(LojiaStrings.get("fieldPhone", isBn), isPhoneValid, if (isBn) "৭–১৫ ডিজিট" else "7–15 digits"),
            Triple(LojiaStrings.get("fieldPassword", isBn), isPwValid, if (isBn) "কমপক্ষে ৮ অক্ষর" else "Min. 8 chars"),
            Triple(LojiaStrings.get("fieldConfirmPw", isBn), isCpValid, if (isBn) "উভয় পাসওয়ার্ড একই" else "Must match"),
            Triple(LojiaStrings.get("fieldSecQuestion", isBn), isSqValid, if (isBn) "প্রশ্ন নির্বাচন" else "Select"),
            Triple(LojiaStrings.get("fieldSecAnswer", isBn), isSaValid, if (isBn) "উত্তর লিখুন" else "Enter answer"),
            Triple(LojiaStrings.get("fieldTerms", isBn), isTermsValid, if (isBn) "শর্তে টিক দিন" else "Check box")
        )
    }

    val regCompletedCount = regRequirements.count { it.second }
    val regTotalCount = regRequirements.size
    val regCompletionPercent = (regCompletedCount * 100) / regTotalCount
    val isFormComplete = regCompletedCount == regTotalCount
    val pendingRequirements = regRequirements.filter { !it.second }

    fun handleRegister() {
        focusManager.clearFocus()

        if (!isFormComplete) {
            Toast.makeText(
                context,
                if (isBn) "রেজিস্ট্রেশন করতে সম্পূর্ণ ডাটা ১০০% পূরণ করুন!" else "Please complete 100% of all required fields!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        var hasError = false
        if (rFn.trim().isEmpty()) { vFn = FieldValidationState.ERROR; hasError = true }
        if (rLn.trim().isEmpty()) { vLn = FieldValidationState.ERROR; hasError = true }

        val u = rUn.trim()
        if (u.length !in 3..20 || !u.all { it.isLetterOrDigit() || it == '_' }) {
            vUn = FieldValidationState.ERROR
            hasError = true
        }

        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(rEm.trim())) {
            vEm = FieldValidationState.ERROR
            hasError = true
        }

        val digits = phoneNum.filter { it.isDigit() }
        if (phoneNum.isNotBlank() && digits.length !in 7..15) {
            vPhone = FieldValidationState.ERROR
            hasError = true
        }

        if (rPw.length < 8) {
            vPw = FieldValidationState.ERROR
            hasError = true
        }

        if (rCp != rPw || rCp.isEmpty()) {
            vCp = FieldValidationState.ERROR
            hasError = true
        }

        if (rSq.isBlank()) {
            vSq = FieldValidationState.ERROR
            hasError = true
        }

        if (rSa.trim().isEmpty()) {
            vSa = FieldValidationState.ERROR
            hasError = true
        }

        if (hasError) return

        isProcessingReg = true
        coroutineScope.launch {
            delay(1500)
            isProcessingReg = false

            val fullPhone = "${selectedCountry.dial} $phoneNum".trim()
            val newProfile = (userProfile ?: UserProfile()).copy(
                fullName = "${rFn.trim()} ${rLn.trim()}".trim(),
                username = rUn.trim(),
                email = rEm.trim(),
                passwordHash = SecurityUtils.hashSecret(rPw),
                securityQuestion = rSq,
                securityAnswer = rSa.trim(),
                phone = fullPhone,
                isRegistered = true
            )
            preferencesRepository.saveUserSession(
                username = newProfile.username,
                fullName = newProfile.fullName,
                email = newProfile.email,
                rememberMe = true
            )
            preferencesRepository.syncWithUserProfile(newProfile)
            onRegisterSuccess(newProfile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LojiaHeader(isBn = isBn)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-14).dp)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Registration Progress Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isFormComplete) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                border = BorderStroke(
                    1.2.dp,
                    if (isFormComplete) Color(0xFF86EFAC) else Color(0xFFFCD34D)
                ),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFormComplete) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFormComplete) Icons.Default.Check else Icons.Outlined.Assignment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = LojiaStrings.get("regProgress", isBn),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFormComplete) Color(0xFF065F46) else Color(0xFF92400E)
                                )
                                Text(
                                    text = if (isFormComplete) {
                                        if (isBn) "১০০% সম্পূর্ণ · বাটন সচল" else "100% Completed · Button Active"
                                    } else {
                                        if (isBn) "$regCompletedCount/$regTotalCount পূরণ হয়েছে (বাকি ${pendingRequirements.size}টি)" else "$regCompletedCount/$regTotalCount filled (${pendingRequirements.size} remaining)"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isFormComplete) Color(0xFF047857) else Color(0xFFB45309)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isFormComplete) Color(0xFF10B981) else if (regCompletionPercent >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        ) {
                            Text(
                                text = "$regCompletionPercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = regCompletionPercent / 100f,
                        animationSpec = tween(durationMillis = 350),
                        label = "regProgressAnim"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isFormComplete) Color(0xFF10B981) else if (regCompletionPercent >= 50) Color(0xFFF59E0B) else Color(0xFFEF4444),
                        trackColor = Color.Black.copy(alpha = 0.08f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isFormComplete) {
                        Text(
                            text = if (isBn) "❌ যেসব ডাটা এখনও এন্ট্রি হয়নি (বাকি ${pendingRequirements.size}টি):" else "❌ Missing Data (${pendingRequirements.size} remaining):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            pendingRequirements.forEach { req ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.88f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = req.first,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        text = " — ${req.third}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LojiaStrings.get("allFieldsCompleted", isBn),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }
            }

            // Card 1: Personal Profile
            LojiaCard(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.Person,
                    title = LojiaStrings.get("profileTitle", isBn),
                    subtitle = LojiaStrings.get("profileSub", isBn)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LojiaInputField(
                        value = rFn,
                        onValueChange = { rFn = it },
                        label = LojiaStrings.get("firstName", isBn),
                        placeholder = LojiaStrings.get("phFirstName", isBn),
                        leadingIcon = Icons.Outlined.Person,
                        isRequired = true,
                        isValid = rFn.trim().isNotEmpty(),
                        validationState = vFn,
                        errorMessage = LojiaStrings.get("errRequired", isBn),
                        successMessage = LojiaStrings.get("okGood", isBn),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                        modifier = Modifier.weight(1f),
                        testTag = "rFn"
                    )

                    LojiaInputField(
                        value = rLn,
                        onValueChange = { rLn = it },
                        label = LojiaStrings.get("lastName", isBn),
                        placeholder = LojiaStrings.get("phLastName", isBn),
                        leadingIcon = Icons.Outlined.Person,
                        isRequired = true,
                        isValid = rLn.trim().isNotEmpty(),
                        validationState = vLn,
                        errorMessage = LojiaStrings.get("errRequired", isBn),
                        successMessage = LojiaStrings.get("okGood", isBn),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.weight(1f),
                        testTag = "rLn"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rUn,
                    onValueChange = {
                        rUn = it.filter { ch -> !ch.isWhitespace() }.lowercase()
                    },
                    label = LojiaStrings.get("username", isBn),
                    placeholder = LojiaStrings.get("phUsername", isBn),
                    leadingIcon = Icons.Outlined.Person,
                    isRequired = true,
                    isValid = rUn.trim().length in 3..20,
                    validationState = vUn,
                    errorMessage = LojiaStrings.get("errUserLength", isBn),
                    successMessage = LojiaStrings.get("okUserAvail", isBn),
                    infoTooltip = LojiaStrings.get("userTip", isBn),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rUn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rEm,
                    onValueChange = { rEm = it },
                    label = LojiaStrings.get("workEmail", isBn),
                    placeholder = LojiaStrings.get("phEmail", isBn),
                    leadingIcon = Icons.Outlined.Email,
                    isRequired = true,
                    isValid = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex().matches(rEm.trim()),
                    validationState = vEm,
                    errorMessage = LojiaStrings.get("errValidEmail", isBn),
                    successMessage = LojiaStrings.get("okValidEmail", isBn),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rEm"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = LojiaStrings.get("phoneNumber", isBn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LojiaColors.N600,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LojiaColors.N50,
                            border = BorderStroke(1.5.dp, LojiaColors.N300),
                            modifier = Modifier
                                .height(42.dp)
                                .widthIn(min = 78.dp)
                                .clickable { showCountryPicker = true }
                                .testTag("ccBtn")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCountry.dial,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LojiaColors.N700
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = LojiaColors.N400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            LojiaInputField(
                                value = phoneNum,
                                onValueChange = { phoneNum = it },
                                label = "",
                                placeholder = LojiaStrings.get("phPhone", isBn),
                                leadingIcon = Icons.Outlined.Phone,
                                validationState = vPhone,
                                errorMessage = LojiaStrings.get("errValidPhone", isBn),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                testTag = "phoneNum"
                            )
                        }
                    }
                }
            }

            // Card 2: Security Details
            LojiaCard(modifier = Modifier.padding(top = 12.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.Shield,
                    title = LojiaStrings.get("secTitle", isBn),
                    subtitle = LojiaStrings.get("secSub", isBn)
                )

                LojiaInputField(
                    value = rPw,
                    onValueChange = { rPw = it },
                    label = LojiaStrings.get("password", isBn),
                    placeholder = LojiaStrings.get("phPwMin", isBn),
                    leadingIcon = Icons.Outlined.Lock,
                    isRequired = true,
                    isValid = rPw.length >= 8,
                    validationState = vPw,
                    errorMessage = LojiaStrings.get("errPwMin", isBn),
                    visualTransformation = if (rPwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { rPwVisible = !rPwVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (rPwVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = if (rPwVisible) LojiaColors.P500 else LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rPw"
                )

                LojiaPasswordStrengthMeter(password = rPw, isBn = isBn)

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rCp,
                    onValueChange = { rCp = it },
                    label = LojiaStrings.get("confirmPw", isBn),
                    placeholder = LojiaStrings.get("phReEnterPw", isBn),
                    leadingIcon = Icons.Outlined.Shield,
                    isRequired = true,
                    isValid = rCp == rPw && rPw.isNotEmpty(),
                    validationState = vCp,
                    errorMessage = LojiaStrings.get("errPwMatch", isBn),
                    successMessage = LojiaStrings.get("okPwMatch", isBn),
                    visualTransformation = if (rCpVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { rCpVisible = !rCpVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (rCpVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle password",
                                tint = if (rCpVisible) LojiaColors.P500 else LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    testTag = "rCp"
                )
            }

            // Card 3: Recovery & Compliance
            LojiaCard(modifier = Modifier.padding(top = 12.dp)) {
                LojiaSectionHeader(
                    icon = Icons.Outlined.HelpOutline,
                    title = LojiaStrings.get("recTitle", isBn),
                    subtitle = LojiaStrings.get("recSub", isBn)
                )

                var showQuestionMenu by remember { mutableStateOf(false) }
                val questionKeys = listOf("sq1", "sq2", "sq3", "sq4", "sq5")

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    ) {
                        Text(
                            text = LojiaStrings.get("secQuestion", isBn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LojiaColors.N600
                        )
                        Text(
                            text = " *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rSq.isNotEmpty()) LojiaColors.G500 else LojiaColors.R500
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LojiaColors.N50)
                            .border(
                                1.5.dp,
                                if (vSq == FieldValidationState.ERROR) LojiaColors.R500 else LojiaColors.N300,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { showQuestionMenu = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = LojiaColors.N400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (rSq.isBlank()) LojiaStrings.get("chooseQuestion", isBn) else rSq,
                                fontSize = 13.sp,
                                color = if (rSq.isBlank()) LojiaColors.N400 else LojiaColors.N900,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = LojiaColors.N400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showQuestionMenu,
                            onDismissRequest = { showQuestionMenu = false },
                            modifier = Modifier.background(LojiaColors.White)
                        ) {
                            questionKeys.forEach { key ->
                                val qText = LojiaStrings.get(key, isBn)
                                DropdownMenuItem(
                                    text = { Text(text = qText, fontSize = 13.sp, color = LojiaColors.N900) },
                                    onClick = {
                                        rSq = qText
                                        showQuestionMenu = false
                                        vSq = FieldValidationState.SUCCESS
                                    }
                                )
                            }
                        }
                    }

                    if (vSq == FieldValidationState.ERROR) {
                        Text(
                            text = LojiaStrings.get("errSelQuestion", isBn),
                            color = LojiaColors.R500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 3.dp, start = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LojiaInputField(
                    value = rSa,
                    onValueChange = { rSa = it },
                    label = LojiaStrings.get("secAnswer", isBn),
                    placeholder = LojiaStrings.get("phAnswer", isBn),
                    leadingIcon = Icons.Outlined.CheckCircle,
                    isRequired = true,
                    isValid = rSa.trim().isNotEmpty(),
                    validationState = vSa,
                    hintMessage = if (isBn) "এনক্রিপ্ট করে সংরক্ষিত · কাউকে দেখানো হবে না" else "Stored encrypted · never shown to anyone",
                    errorMessage = LojiaStrings.get("errRequired", isBn),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    testTag = "rSa"
                )

                val termsAnnotated = remember(isBn) {
                    buildAnnotatedString {
                        if (isBn) {
                            append("আমি ")
                            withStyle(SpanStyle(color = LojiaColors.P500, fontWeight = FontWeight.SemiBold)) {
                                append("শর্তাবলী")
                            }
                            append(" এবং ")
                            withStyle(SpanStyle(color = LojiaColors.P500, fontWeight = FontWeight.SemiBold)) {
                                append("গোপনীয়তা নীতি")
                            }
                            append(" মেনে চলছি এবং ব্যবসায়িক নিয়ম মেনে Lojia ব্যবহার করার অঙ্গীকার করছি।")
                        } else {
                            append("I agree to the ")
                            withStyle(SpanStyle(color = LojiaColors.P500, fontWeight = FontWeight.SemiBold)) {
                                append("Terms of Service")
                            }
                            append(" and ")
                            withStyle(SpanStyle(color = LojiaColors.P500, fontWeight = FontWeight.SemiBold)) {
                                append("Privacy Policy")
                            }
                            append(", and certify I will use Lojia in compliance with business policies.")
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 13.dp)
                        .clickable { agreeTerms = !agreeTerms }
                        .testTag("cbTerms"),
                    shape = RoundedCornerShape(10.dp),
                    color = LojiaColors.N50,
                    border = BorderStroke(1.dp, LojiaColors.N200)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Checkbox(
                            checked = agreeTerms,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = LojiaColors.P500,
                                uncheckedColor = LojiaColors.N300
                            ),
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = termsAnnotated,
                            fontSize = 11.5.sp,
                            color = LojiaColors.N500,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Status Banner
            if (!isFormComplete) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isBn) "রেজিস্ট্রেশন বাটন নিষ্ক্রিয় (OFF)" else "Register Button Disabled (OFF)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = if (isBn)
                                    "সম্পূর্ণ ডাটা ১০০% পূরণ করার পর বাটন অন হবে। এখনও ${pendingRequirements.size}টি তথ্য বাকি রয়েছে।"
                                else
                                    "Complete 100% of all required information to turn ON the button. ${pendingRequirements.size} items remaining.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "সব তথ্য ১০০% পূরণ সম্পন্ন হয়েছে! রেজিস্ট্রেশন বাটন এখন সক্রিয় (ON)।" else "100% information completed! Register button is now ACTIVE (ON).",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LojiaGradientButton(
                text = if (isProcessingReg) LojiaStrings.get("processing", isBn)
                else if (isFormComplete) LojiaStrings.get("regBtn", isBn)
                else (if (isBn) "সম্পূর্ণ তথ্য পূরণ করুন ($regCompletionPercent%)" else "Fill All Fields ($regCompletionPercent%)"),
                onClick = {
                    if (isFormComplete) {
                        handleRegister()
                    }
                },
                enabled = isFormComplete && !isProcessingReg,
                isLoading = isProcessingReg,
                testTag = "regBtn"
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = LojiaColors.N400,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = LojiaStrings.get("sslText", isBn),
                    fontSize = 10.sp,
                    color = LojiaColors.N400
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LojiaStrings.get("alreadyAccount", isBn) + " ",
                    fontSize = 12.sp,
                    color = LojiaColors.N500
                )
                Text(
                    text = LojiaStrings.get("signInLink", isBn),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LojiaColors.P500,
                    modifier = Modifier
                        .clickable { onGoToLogin() }
                        .padding(4.dp)
                        .testTag("goLogin")
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (showCountryPicker) {
        LojiaCountryPickerDialog(
            countries = LOJIA_COUNTRIES,
            selectedDial = selectedCountry.dial,
            onSelectCountry = {
                selectedCountry = it
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
            isBn = isBn
        )
    }
}

@Composable
fun LojiaRegisterSuccessScreen(
    isBn: Boolean,
    onGoToLogin: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pgSuccess")
    ) {
        LojiaHeader(isBn = isBn)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale.value)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(LojiaColors.G100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = LojiaColors.G500,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = LojiaStrings.get("successTitle", isBn),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LojiaColors.N900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = LojiaStrings.get("successSub", isBn),
                    fontSize = 13.sp,
                    color = LojiaColors.N500,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 280.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                LojiaGradientButton(
                    text = LojiaStrings.get("goToLogin", isBn),
                    onClick = onGoToLogin,
                    modifier = Modifier.widthIn(max = 280.dp),
                    testTag = "goLoginFromSuccess"
                )
            }
        }
    }
}
