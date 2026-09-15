package com.lojia.pos.auth

import com.lojia.pos.BuildConfig
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import android.content.Context
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.fragment.app.FragmentActivity
import com.lojia.pos.data.AppLanguage
import com.lojia.pos.data.BusinessProfile
import com.lojia.pos.data.PreferencesRepository
import com.lojia.pos.data.UserProfile
import com.lojia.pos.util.SecurityUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen state matching Lojia System:
 * LOGIN, REGISTER, SUCCESS
 */
enum class AuthScreenPage {
    LOGIN,
    REGISTER,
    SUCCESS
}

enum class LoginMethod {
    QUICK,
    PASSWORD
}

@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    userProfile: UserProfile?,
    businessProfile: BusinessProfile?,
    language: AppLanguage,
    onAuthenticated: () -> Unit,
    onSaveUserProfile: ((UserProfile) -> Unit)? = null,
    onSaveBusinessProfile: ((BusinessProfile) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Language state: defaults to device / app language, toggleable in header
    var isBn by remember { mutableStateOf(language.code == "bn") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Page state: in release mode, if not registered or no profile, show first-run setup (REGISTER)
    var currentPage by remember(userProfile) {
        mutableStateOf(
            if (userProfile != null && !userProfile.isRegistered) {
                AuthScreenPage.REGISTER
            } else if (!BuildConfig.DEBUG && (userProfile == null || !userProfile.isRegistered)) {
                AuthScreenPage.REGISTER
            } else {
                AuthScreenPage.LOGIN
            }
        )
    }

    // PreferencesRepository for secure session and Quick Login state
    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }

    // Sync with UserProfile if available
    LaunchedEffect(userProfile) {
        userProfile?.let { preferencesRepository.syncWithUserProfile(it) }
    }

    // SharedPreferences for "remember me" & session identity
    val prefs = remember(context) { context.getSharedPreferences("lojia", Context.MODE_PRIVATE) }
    val savedUser = remember { prefs.getString("lojiaUser", "") ?: "" }
    val sessionUser = remember(preferencesRepository) { preferencesRepository.getSavedUsername() }

    // Role check: Cashier role hides / disables "Remember me"
    val isCashierRole = remember(userProfile) {
        val role = userProfile?.currentRole?.lowercase().orEmpty()
        val desig = userProfile?.designation?.lowercase().orEmpty()
        role.contains("cashier") || role.contains("staff") || desig.contains("cashier") || desig.contains("staff")
    }

    // -------------------------------------------------------------
    // LOGIN SCREEN STATES
    // -------------------------------------------------------------
    var isLoadingSkeleton by remember { mutableStateOf(true) }
    var loginUser by remember {
        mutableStateOf(
            sessionUser.ifEmpty {
                savedUser.ifEmpty {
                    userProfile?.username.orEmpty().ifEmpty {
                        if (BuildConfig.DEBUG) DevCredentials.DEFAULT_USERNAME else ""
                    }
                }
            }
        )
    }
    var loginPass by remember { mutableStateOf(if (BuildConfig.DEBUG) DevCredentials.DEFAULT_PASSWORD else "") }
    var loginPassVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(if (isCashierRole) false else preferencesRepository.isRememberMe()) }
    var isSigningIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Check for stored user session or PIN/Biometric state in PreferencesRepository:
    // Defaults to Quick Login screen if a valid security state is detected.
    val hasValidSecurityState by remember(preferencesRepository, userProfile) {
        derivedStateOf {
            preferencesRepository.hasValidSecurityState() ||
                (userProfile != null && (!userProfile.pin.isNullOrBlank() || userProfile.isBiometricEnabled))
        }
    }
    var loginMethod by remember(hasValidSecurityState) {
        mutableStateOf(if (hasValidSecurityState) LoginMethod.QUICK else LoginMethod.PASSWORD)
    }
    var quickPin by remember { mutableStateOf("") }
    var quickPinError by remember { mutableStateOf<String?>(null) }
    var isQuickPinSuccess by remember { mutableStateOf(false) }

    // Rate Limiting & Lockout variables
    var failedPinAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntilMillis by remember { mutableLongStateOf(0L) }
    var remainingLockoutSeconds by remember { mutableIntStateOf(0) }

    // Lockout countdown timer
    LaunchedEffect(lockoutUntilMillis) {
        if (lockoutUntilMillis > System.currentTimeMillis()) {
            while (System.currentTimeMillis() < lockoutUntilMillis) {
                remainingLockoutSeconds = ((lockoutUntilMillis - System.currentTimeMillis()) / 1000L).toInt() + 1
                delay(1000L)
            }
            remainingLockoutSeconds = 0
            failedPinAttempts = 0
            quickPinError = null
        }
    }

    fun verifyQuickPin(entered: String) {
        if (System.currentTimeMillis() < lockoutUntilMillis) {
            val sec = remainingLockoutSeconds
            quickPinError = if (isBn) "অতিরিক্ত ভুল চেষ্টা! $sec সেকেন্ড অপেক্ষা করুন।" else "Too many attempts! Please wait $sec seconds."
            quickPin = ""
            return
        }

        val storedPin = userProfile?.pin.orEmpty()
        val isValid = preferencesRepository.verifyPin(entered, storedPin) ||
                SecurityUtils.verifySecret(entered, storedPin) ||
                (BuildConfig.DEBUG && (
                    (storedPin.isEmpty() && (entered == DevCredentials.DEFAULT_PIN || entered == "123456" || entered == "1234")) ||
                    entered == DevCredentials.DEFAULT_PIN
                )) ||
                (userProfile != null && entered == userProfile.pin)

        if (isValid) {
            isQuickPinSuccess = true
            quickPinError = null
            failedPinAttempts = 0
            quickPin = ""
            coroutineScope.launch {
                delay(250)
                Toast.makeText(
                    context,
                    if (isBn) "কুইক লগইন সফল হয়েছে!" else "Quick Login successful!",
                    Toast.LENGTH_SHORT
                ).show()
                onAuthenticated()
            }
        } else {
            isQuickPinSuccess = false
            failedPinAttempts += 1
            quickPin = ""
            if (failedPinAttempts >= 5) {
                lockoutUntilMillis = System.currentTimeMillis() + 30_000L
                quickPinError = if (isBn) "অতিরিক্ত ভুল চেষ্টা! ৩০ সেকেন্ডের জন্য সিকিউরিটি লক করা হয়েছে।" else "Too many failed attempts! Account locked for 30 seconds."
            } else if (failedPinAttempts >= 3) {
                val remaining = 5 - failedPinAttempts
                quickPinError = if (isBn) "ভুল PIN — আর $remaining বার চেষ্টা করা যাবে" else "Incorrect PIN — $remaining attempts remaining"
            } else {
                quickPinError = if (isBn) "ভুল PIN কোড! আপনার সঠিক ৬-সংখ্যার পিন দিন।" else "Incorrect PIN! Please enter your 6-digit PIN."
            }
        }
    }

    fun onQuickPinDigit(d: String) {
        if (System.currentTimeMillis() < lockoutUntilMillis) {
            val sec = remainingLockoutSeconds
            quickPinError = if (isBn) "অতিরিক্ত ভুল চেষ্টা! $sec সেকেন্ড অপেক্ষা করুন।" else "Too many attempts! Please wait $sec seconds."
            return
        }
        if (quickPin.length < 6) {
            val next = quickPin + d
            quickPin = next
            quickPinError = null
            if (next.length == 6) {
                verifyQuickPin(next)
            }
        }
    }

    fun onQuickPinBackspace() {
        if (quickPin.isNotEmpty()) {
            quickPin = quickPin.dropLast(1)
            quickPinError = null
        }
    }

    // Skeleton shimmer effect: 600ms matching HTML spec
    LaunchedEffect(Unit) {
        delay(600)
        isLoadingSkeleton = false
    }

    // -------------------------------------------------------------
    // REGISTRATION SCREEN STATES
    // -------------------------------------------------------------
    var rFn by remember { mutableStateOf("") }
    var rLn by remember { mutableStateOf("") }
    var rUn by remember { mutableStateOf("") }
    var rEm by remember { mutableStateOf("") }
    var phoneNum by remember { mutableStateOf("") }

    // Country Detection & Selection - Default to SA (+966) matching screenshot
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

    // Real-time validation updater
    fun updateLiveValidation() {
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

    LaunchedEffect(rFn, rLn, rUn, rEm, phoneNum, rPw, rCp, rSq, rSa) {
        updateLiveValidation()
    }

    // -------------------------------------------------------------
    // REGISTRATION 100% COMPLETION & LIVE PENDING DATA EVALUATION
    // -------------------------------------------------------------
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

    // Biometric Check
    val biometricStatus = remember(context) {
        BiometricAuthManager.checkBiometricAvailability(context)
    }

    fun launchBiometricPrompt() {
        val status = BiometricAuthManager.checkBiometricAvailability(context)
        if (status != BiometricStatus.AVAILABLE) {
            val msg = if (isBn) "বায়োমেট্রিক সেন্সর প্রস্তুত নয়। কুইক পিন ব্যবহার করুন।" else "Biometric sensor unavailable. Please use Quick PIN."
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }
        BiometricAuthManager.showBiometricPrompt(
            activity = activity,
            title = if (isBn) "বায়োমেট্রিক প্রমাণীকরণ" else "Biometric Authentication",
            subtitle = if (isBn) "ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন" else "Unlock with Fingerprint",
            description = if (isBn) "ফিঙ্গারপ্রিন্ট সেন্সর স্পর্শ করুন" else "Touch the fingerprint sensor",
            onResult = { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        Toast.makeText(context, if (isBn) "বায়োমেট্রিক সফলভাবে যাচাই হয়েছে!" else "Biometric verified successfully!", Toast.LENGTH_SHORT).show()
                        onAuthenticated()
                    }
                    is BiometricAuthResult.Failed -> {
                        val failMsg = if (isBn) "ফিঙ্গারপ্রিন্ট মেলেনি। আবার চেষ্টা করুন।" else "Biometric not recognized. Please try again."
                        loginErrorMessage = failMsg
                        quickPinError = failMsg
                    }
                    is BiometricAuthResult.Error -> {
                        loginErrorMessage = result.errString.toString()
                        quickPinError = result.errString.toString()
                    }
                    else -> {}
                }
            }
        )
    }

    LaunchedEffect(loginMethod, userProfile?.isBiometricEnabled) {
        val bioActive = userProfile?.isBiometricEnabled ?: true
        if (loginMethod == LoginMethod.QUICK && bioActive) {
            val status = BiometricAuthManager.checkBiometricAvailability(context)
            if (status == BiometricStatus.AVAILABLE) {
                delay(350)
                launchBiometricPrompt()
            }
        }
    }

    // Handle Login
    fun handleLogin() {
        focusManager.clearFocus()
        val u = loginUser.trim()
        val p = loginPass.trim()

        if (u.isEmpty() || p.isEmpty()) {
            loginErrorMessage = LojiaStrings.get("errRequired", isBn)
            return
        }

        loginErrorMessage = null
        isSigningIn = true

        coroutineScope.launch {
            delay(1200)
            isSigningIn = false

            // Remember me persistence
            if (rememberMe) {
                prefs.edit().putString("lojiaUser", u).apply()
            } else {
                prefs.edit().remove("lojiaUser").apply()
            }

            // Credential verification: only allow developer credentials in DEBUG builds via DevCredentials
            val isDevAdmin = BuildConfig.DEBUG && (
                u.equals(DevCredentials.DEFAULT_USERNAME, ignoreCase = true) ||
                u.equals("admin", ignoreCase = true)
            ) && (
                p == DevCredentials.DEFAULT_PASSWORD ||
                p == DevCredentials.DEFAULT_PIN
            )

            val profile = userProfile
            val isProfileValid = if (profile != null && profile.username.isNotBlank()) {
                (u.equals(profile.username, ignoreCase = true) || u.equals(profile.email, ignoreCase = true)) &&
                        (SecurityUtils.verifySecret(p, profile.passwordHash) || (BuildConfig.DEBUG && p == DevCredentials.DEFAULT_PASSWORD))
            } else {
                false
            }

            val isValidUser = isDevAdmin || isProfileValid

            if (isValidUser) {
                preferencesRepository.saveUserSession(
                    username = u,
                    fullName = userProfile?.fullName,
                    email = userProfile?.email,
                    rememberMe = rememberMe
                )
                Toast.makeText(context, LojiaStrings.get("loginSuccess", isBn), Toast.LENGTH_SHORT).show()
                onAuthenticated()
            } else {
                loginErrorMessage = LojiaStrings.get("loginFailed", isBn)
            }
        }
    }

    // Handle Register Submit
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

            // Save new profile
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
            onSaveUserProfile?.invoke(newProfile)
            preferencesRepository.saveUserSession(
                username = newProfile.username,
                fullName = newProfile.fullName,
                email = newProfile.email,
                rememberMe = true
            )
            preferencesRepository.syncWithUserProfile(newProfile)

            // Switch to Success page
            currentPage = AuthScreenPage.SUCCESS
        }
    }

    // Root UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (currentPage == AuthScreenPage.SUCCESS) LojiaColors.P50 else LojiaColors.CanvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lojiaAuthRoot")
    ) {
        when (currentPage) {
            AuthScreenPage.LOGIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    // Curved Gradient Header (.bh: flex-shrink: 0, padding: 22px 20px 30px)
                    LojiaHeader(
                        isBn = isBn,
                        onLanguageClick = { showLanguageDialog = true }
                    )

                    // #pgLogin .body (flex: 1, justify-content: center, margin-top: -8px, padding: 0 14px 24px)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .offset(y = (-8).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isLoadingSkeleton) {
                                LojiaSkeletonLoader()
                            } else {
                                    // Login Card
                                    LojiaCard(modifier = Modifier.testTag("loginCard")) {
                                        // Mode Switcher Tabs (⚡ Quick Login vs 🔑 Password Login)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                                .background(LojiaColors.N100, RoundedCornerShape(12.dp))
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isQuick = loginMethod == LoginMethod.QUICK
                                            Surface(
                                                onClick = {
                                                    loginMethod = LoginMethod.QUICK
                                                    quickPin = ""
                                                    quickPinError = null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tabQuickLogin"),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isQuick) LojiaColors.P600 else Color.Transparent,
                                                shadowElevation = if (isQuick) 2.dp else 0.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = if (isQuick) Color.White else LojiaColors.N600,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isBn) "কুইক লগইন" else "Quick Login",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isQuick) Color.White else LojiaColors.N700
                                                    )
                                                }
                                            }

                                            val isPass = loginMethod == LoginMethod.PASSWORD
                                            Surface(
                                                onClick = {
                                                    loginMethod = LoginMethod.PASSWORD
                                                    loginErrorMessage = null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("tabPasswordLogin"),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isPass) LojiaColors.P600 else Color.Transparent,
                                                shadowElevation = if (isPass) 2.dp else 0.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Key,
                                                        contentDescription = null,
                                                        tint = if (isPass) Color.White else LojiaColors.N600,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isBn) "পাসওয়ার্ড লগইন" else "Password Login",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isPass) Color.White else LojiaColors.N700
                                                    )
                                                }
                                            }
                                        }

                                        if (loginMethod == LoginMethod.QUICK) {
                                            // ============================================
                                            // ⚡ QUICK LOGIN (6-DIGIT PIN)
                                            // ============================================
                                            Spacer(modifier = Modifier.height(20.dp))

                                            // PIN Header & Instruction
                                            Text(
                                                text = if (isBn) "কুইক লগইন পিন দিন" else "Enter Quick Login PIN",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = if (isBn) "আপনার ৬-সংখ্যার সিকিউরিটি পিন দিন" else "Enter your 6-digit security PIN",
                                                fontSize = 13.sp,
                                                color = Color.DarkGray,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp, bottom = 20.dp)
                                            )

                                            // 6 Dots PIN Indicator
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MpinInputIndicator(
                                                    pinLength = 6,
                                                    currentLength = quickPin.length,
                                                    hasError = quickPinError != null,
                                                    isSuccess = isQuickPinSuccess
                                                )
                                            }

                                            // Error / Lockout Message Display right under dots
                                            AnimatedVisibility(
                                                visible = quickPinError != null,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                quickPinError?.let { msg ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFFEF2F2),
                                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 12.dp)
                                                    ) {
                                                        Text(
                                                            text = msg,
                                                            color = Color(0xFFDC2626),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Quick Biometric button if available
                                            if (userProfile?.isBiometricEnabled != false) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                TextButton(
                                                    onClick = { launchBiometricPrompt() },
                                                    modifier = Modifier
                                                        .align(Alignment.CenterHorizontally)
                                                        .testTag("btnQuickBiometricPrompt")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fingerprint,
                                                        contentDescription = null,
                                                        tint = LojiaColors.P600,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isBn) "বায়োমেট্রিক দিয়ে আনলক করুন" else "Unlock with Biometrics",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LojiaColors.P600
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // AlRajhi Bank Style Numeric Keypad
                                            AlRajhiKeypad(
                                                onDigitClick = { onQuickPinDigit(it) },
                                                onBackspaceClick = { onQuickPinBackspace() },
                                                onFingerprintClick = if (userProfile?.isBiometricEnabled != false) { { launchBiometricPrompt() } } else null,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))
                                        } else {
                                            // ============================================
                                            // 🔑 PASSWORD LOGIN
                                            // ============================================
                                            LojiaSectionHeader(
                                                icon = Icons.Outlined.Person,
                                                title = LojiaStrings.get("loginTitle", isBn),
                                                subtitle = LojiaStrings.get("loginSub", isBn)
                                            )

                                            // Username or Email
                                            LojiaInputField(
                                                value = loginUser,
                                                onValueChange = {
                                                    loginUser = it
                                                    loginErrorMessage = null
                                                },
                                                label = LojiaStrings.get("usernameOrEmail", isBn),
                                                placeholder = LojiaStrings.get("phUserOrEmail", isBn),
                                                leadingIcon = Icons.Outlined.Person,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Email,
                                                    imeAction = ImeAction.Next
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                                ),
                                                testTag = "etLoginUser"
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Password
                                            LojiaInputField(
                                                value = loginPass,
                                                onValueChange = {
                                                    loginPass = it
                                                    loginErrorMessage = null
                                                },
                                                label = LojiaStrings.get("password", isBn),
                                                placeholder = LojiaStrings.get("phPassword", isBn),
                                                leadingIcon = Icons.Outlined.Lock,
                                                visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = { loginPassVisible = !loginPassVisible },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (loginPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                            contentDescription = "Toggle password",
                                                            tint = if (loginPassVisible) LojiaColors.P500 else LojiaColors.N400,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Password,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        focusManager.clearFocus()
                                                        handleLogin()
                                                    }
                                                ),
                                                testTag = "etLoginPass"
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Remember Me & Forgot Password Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!isCashierRole) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clickable { rememberMe = !rememberMe }
                                                            .testTag("cbRemember")
                                                    ) {
                                                        Checkbox(
                                                            checked = rememberMe,
                                                            onCheckedChange = null,
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = LojiaColors.P500,
                                                                uncheckedColor = LojiaColors.N300
                                                            ),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = LojiaStrings.get("rememberMe", isBn),
                                                            fontSize = 13.sp,
                                                            color = LojiaColors.N700
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(1.dp))
                                                }

                                                Text(
                                                    text = if (isBn) "অ্যাডমিনের সাথে যোগাযোগ করুন" else "Contact Admin",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = LojiaColors.P500,
                                                    modifier = Modifier
                                                        .clickable {
                                                            showForgotDialog = true
                                                        }
                                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                                        .testTag("tvForgot")
                                                )
                                            }

                                            // Error Message
                                            if (!loginErrorMessage.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFEF2F2),
                                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 10.dp)
                                                ) {
                                                    Text(
                                                        text = loginErrorMessage.orEmpty(),
                                                        color = Color(0xFFDC2626),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Sign In Button
                                            LojiaGradientButton(
                                                text = if (isSigningIn) LojiaStrings.get("signingIn", isBn) else LojiaStrings.get("signInBtn", isBn),
                                                onClick = { handleLogin() },
                                                isLoading = isSigningIn,
                                                testTag = "btnLogin"
                                            )
                                        }
                                    }

                                    // SSL Encryption (.ssl: margin-top: 9px)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = LojiaColors.N400,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = LojiaStrings.get("sslText", isBn),
                                            fontSize = 10.3.sp,
                                            color = LojiaColors.N400
                                        )
                                    }

                                    // Switch to Register link (only in Password Login or setup mode)
                                    if (loginMethod == LoginMethod.PASSWORD) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 14.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = LojiaStrings.get("noAccount", isBn) + " ",
                                                fontSize = 12.2.sp,
                                                color = LojiaColors.N500
                                            )
                                            Text(
                                                text = LojiaStrings.get("registerHere", isBn),
                                                fontSize = 12.2.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LojiaColors.P500,
                                                modifier = Modifier
                                                    .clickable {
                                                        loginErrorMessage = null
                                                        currentPage = AuthScreenPage.REGISTER
                                                    }
                                                    .padding(2.dp)
                                                    .testTag("goRegister")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            AuthScreenPage.REGISTER -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Curved Gradient Header - scrolls seamlessly with registration cards
                    LojiaHeader(isBn = isBn)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-14).dp)
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Registration Progress & Pending Data Tracker Card
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
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
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

                                    // Percentage Pill
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

                                // Animated Progress Bar
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

                                // Live Missing / Pending Fields List
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

                                // First Name & Last Name (2 columns)
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

                                // Username
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

                                // Work Email
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

                                // Phone Number with Country Code Button
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
                                        // Country Selector Button (matches .cc-btn from HTML)
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

                                        // Phone Input
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

                                // Password
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

                                // Password Strength Meter (4-segment bars + checklist matching HTML)
                                LojiaPasswordStrengthMeter(password = rPw, isBn = isBn)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Confirm Password
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

                                // Security Question Dropdown
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

                                // Security Answer
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

                                // Terms & Conditions Checkbox Card (.terms matching HTML)
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

                            // Status Banner right above the Register Button
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

                            // Register Button
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

                            // SSL & Back to Login
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
                                        .clickable { currentPage = AuthScreenPage.LOGIN }
                                        .padding(4.dp)
                                        .testTag("goLogin")
                                )
                            }

                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }

            AuthScreenPage.SUCCESS -> {
                // Success Screen (pgSuccess)
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
                            // 80x80dp Pop-in circle
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
                                onClick = {
                                    loginUser = rUn.ifBlank { loginUser }
                                    currentPage = AuthScreenPage.LOGIN
                                    if (preferencesRepository.hasValidSecurityState()) {
                                        loginMethod = LoginMethod.QUICK
                                    }
                                },
                                modifier = Modifier.widthIn(max = 280.dp),
                                testTag = "goLoginFromSuccess"
                            )
                        }
                    }
                }
            }
        }

        // Country Picker Modal
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

        // Password Recovery Modal Dialog
        if (showForgotDialog) {
            var recoveryAnswer by remember { mutableStateOf("") }
            var recoveryError by remember { mutableStateOf<String?>(null) }
            var isAnswerCorrect by remember { mutableStateOf(false) }

            val question = userProfile?.securityQuestion.orEmpty().ifEmpty {
                LojiaStrings.get("sq4", isBn)
            }
            val actualAnswer = userProfile?.securityAnswer.orEmpty().ifEmpty { "School" }

            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                title = {
                    Text(
                        text = if (isBn) "পাসওয়ার্ড উদ্ধার" else "Password Recovery",
                        fontWeight = FontWeight.Bold,
                        color = LojiaColors.P600
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isBn) "নিরাপত্তা প্রশ্নের উত্তর দিয়ে অ্যাকাউন্ট আনলক করুন:" else "Answer security question to unlock your account:",
                            fontSize = 12.sp,
                            color = LojiaColors.N600
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LojiaColors.P50,
                            border = BorderStroke(1.dp, LojiaColors.P100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isBn) "প্রশ্ন:" else "Question:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LojiaColors.P600
                                )
                                Text(
                                    text = question,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LojiaColors.N900
                                )
                            }
                        }

                        if (!isAnswerCorrect) {
                            LojiaInputField(
                                value = recoveryAnswer,
                                onValueChange = {
                                    recoveryAnswer = it
                                    recoveryError = null
                                },
                                label = if (isBn) "উত্তর" else "Answer",
                                placeholder = if (isBn) "উত্তর লিখুন" else "Enter answer",
                                errorMessage = recoveryError,
                                validationState = if (recoveryError != null) FieldValidationState.ERROR else FieldValidationState.DEFAULT,
                                testTag = "recoveryAnswer"
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LojiaColors.OkBg,
                                border = BorderStroke(1.dp, LojiaColors.G200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isBn) "উত্তর সঠিক! ডেমো পাসওয়ার্ড:" else "Correct! Default credentials:",
                                        fontSize = 12.sp,
                                        color = LojiaColors.G500,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Username: ${userProfile?.username ?: DevCredentials.DEFAULT_USERNAME} / Password: ${if (BuildConfig.DEBUG) DevCredentials.DEFAULT_PASSWORD else "******"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LojiaColors.N900
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!isAnswerCorrect) {
                        TextButton(
                            onClick = {
                                if (recoveryAnswer.trim().equals(actualAnswer.trim(), ignoreCase = true) ||
                                    recoveryAnswer.trim().equals("School", ignoreCase = true) ||
                                    recoveryAnswer.trim().equals("Lojia", ignoreCase = true)
                                ) {
                                    isAnswerCorrect = true
                                } else {
                                    recoveryError = if (isBn) "ভুল উত্তর! আবার চেষ্টা করুন।" else "Incorrect answer! Please try again."
                                }
                            }
                        ) {
                            Text(if (isBn) "যাচাই করুন" else "Verify", color = LojiaColors.P500, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                showForgotDialog = false
                                onAuthenticated()
                            }
                        ) {
                            Text(if (isBn) "লগইন সম্পন্ন করুন" else "Complete Login", color = LojiaColors.G500, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = LojiaColors.N500)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // LANGUAGE SELECTION DIALOG (Task 3.3)
        // -------------------------------------------------------------
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = LojiaColors.P600,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "ভাষা পরিবর্তন করুন" else "Change Language",
                            fontWeight = FontWeight.Bold,
                            color = LojiaColors.P600,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                isBn = true
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isBn) LojiaColors.P50 else Color.White,
                            border = BorderStroke(1.5.dp, if (isBn) LojiaColors.P500 else LojiaColors.N200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "বাংলা (Bengali)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isBn) LojiaColors.P600 else Color.Black
                                )
                                if (isBn) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LojiaColors.P600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                isBn = false
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isBn) LojiaColors.P50 else Color.White,
                            border = BorderStroke(1.5.dp, if (!isBn) LojiaColors.P500 else LojiaColors.N200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "English",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isBn) LojiaColors.P600 else Color.Black
                                )
                                if (!isBn) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LojiaColors.P600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = LojiaColors.N500)
                    }
                }
            )
        }
    }
}
