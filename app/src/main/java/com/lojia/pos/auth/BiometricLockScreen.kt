package com.lojia.pos.auth

import android.content.Context
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.lojia.pos.BuildConfig
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

    var isBn by remember { mutableStateOf(language.code == "bn") }
    var showLanguageDialog by remember { mutableStateOf(false) }

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

    val preferencesRepository = remember(context) { PreferencesRepository.getInstance(context) }

    LaunchedEffect(userProfile) {
        userProfile?.let { preferencesRepository.syncWithUserProfile(it) }
    }

    val prefs = remember(context) { context.getSharedPreferences("lojia", Context.MODE_PRIVATE) }
    val savedUser = remember { prefs.getString("lojiaUser", "") ?: "" }
    val sessionUser = remember(preferencesRepository) { preferencesRepository.getSavedUsername() }

    val isCashierRole = remember(userProfile) {
        val role = userProfile?.currentRole?.lowercase().orEmpty()
        val desig = userProfile?.designation?.lowercase().orEmpty()
        role.contains("cashier") || role.contains("staff") || desig.contains("cashier") || desig.contains("staff")
    }

    var isLoadingSkeleton by remember { mutableStateOf(false) }
    var loginUser by remember { mutableStateOf(sessionUser.ifEmpty { savedUser }) }
    var loginPass by remember { mutableStateOf("") }
    var loginPassVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(if (isCashierRole) false else preferencesRepository.isRememberMe()) }
    var isSigningIn by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Quick Login is available only when biometric or quick login is explicitly enabled
    val isQuickLoginConfigured by remember(preferencesRepository, userProfile) {
        derivedStateOf {
            (userProfile?.isBiometricEnabled == true) ||
                preferencesRepository.isBiometricEnabled() ||
                preferencesRepository.isQuickLoginEnabled()
        }
    }
    var loginMethod by remember(isQuickLoginConfigured) {
        mutableStateOf(if (isQuickLoginConfigured) LoginMethod.QUICK else LoginMethod.PASSWORD)
    }

    var quickPin by remember { mutableStateOf("") }
    var quickPinError by remember { mutableStateOf<String?>(null) }
    var isQuickPinSuccess by remember { mutableStateOf(false) }

    var failedPinAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntilMillis by remember { mutableLongStateOf(0L) }
    var remainingLockoutSeconds by remember { mutableIntStateOf(0) }

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
                quickPinError = if (isBn) "ভুল PIN কোড! আপনার সঠিক পিন দিন।" else "Incorrect PIN! Please enter your pin."
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

    fun launchBiometricPrompt() {
        val status = BiometricAuthManager.checkBiometricAvailability(context)
        if (status != BiometricStatus.AVAILABLE) {
            val msg = if (isBn) "বায়োমেট্রিক সেন্সর প্রস্তুত নয়। পিন ব্যবহার করুন।" else "Biometric sensor unavailable. Please use PIN."
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

            if (rememberMe) {
                prefs.edit().putString("lojiaUser", u).apply()
            } else {
                prefs.edit().remove("lojiaUser").apply()
            }

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
                        .background(Color.White)
                        .imePadding()
                ) {
                    LojiaHeader(isBn = isBn)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.White),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLoadingSkeleton) {
                                LojiaSkeletonLoader()
                            } else {
                                // Conditional Quick Login Switcher Tabs
                                if (isQuickLoginConfigured) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp)
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
                                            color = if (isQuick) Color(0xFF0F172A) else Color.Transparent,
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
                                            color = if (isPass) Color(0xFF0F172A) else Color.Transparent,
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
                                }

                                if (isQuickLoginConfigured && loginMethod == LoginMethod.QUICK) {
                                    LojiaQuickLoginContent(
                                        quickPin = quickPin,
                                        quickPinError = quickPinError,
                                        isQuickPinSuccess = isQuickPinSuccess,
                                        isBn = isBn,
                                        onQuickPinDigit = { onQuickPinDigit(it) },
                                        onQuickPinBackspace = { onQuickPinBackspace() },
                                        onFingerprintClick = if (userProfile?.isBiometricEnabled != false) { { launchBiometricPrompt() } } else null
                                    )
                                } else {
                                    LojiaPasswordLoginContent(
                                        loginUser = loginUser,
                                        onLoginUserChange = {
                                            loginUser = it
                                            loginErrorMessage = null
                                        },
                                        loginPass = loginPass,
                                        onLoginPassChange = {
                                            loginPass = it
                                            loginErrorMessage = null
                                        },
                                        loginPassVisible = loginPassVisible,
                                        onTogglePasswordVisible = { loginPassVisible = !loginPassVisible },
                                        rememberMe = rememberMe,
                                        onRememberMeChange = { rememberMe = it },
                                        onForgotPassword = { showForgotDialog = true },
                                        loginErrorMessage = loginErrorMessage,
                                        isSigningIn = isSigningIn,
                                        onSignIn = { handleLogin() },
                                        onRegisterClick = {
                                            loginErrorMessage = null
                                            currentPage = AuthScreenPage.REGISTER
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AuthScreenPage.REGISTER -> {
                LojiaRegisterScreen(
                    userProfile = userProfile,
                    isBn = isBn,
                    onRegisterSuccess = { updatedProfile ->
                        onSaveUserProfile?.invoke(updatedProfile)
                        currentPage = AuthScreenPage.SUCCESS
                    },
                    onGoToLogin = {
                        currentPage = AuthScreenPage.LOGIN
                    }
                )
            }

            AuthScreenPage.SUCCESS -> {
                LojiaRegisterSuccessScreen(
                    isBn = isBn,
                    onGoToLogin = {
                        currentPage = AuthScreenPage.LOGIN
                        if (preferencesRepository.hasValidSecurityState()) {
                            loginMethod = LoginMethod.QUICK
                        }
                    }
                )
            }
        }

        // Recovery Modal Dialog
        LojiaPasswordRecoveryDialog(
            showDialog = showForgotDialog,
            onDismiss = { showForgotDialog = false },
            userProfile = userProfile,
            isBn = isBn,
            onAuthenticated = onAuthenticated
        )

        // Language Selection Dialog
        LojiaLanguageDialog(
            showDialog = showLanguageDialog,
            onDismiss = { showLanguageDialog = false },
            isBn = isBn,
            onSelectLanguage = { isBn = it }
        )
    }
}

@Composable
private fun LojiaQuickLoginContent(
    quickPin: String,
    quickPinError: String?,
    isQuickPinSuccess: Boolean,
    isBn: Boolean,
    onQuickPinDigit: (String) -> Unit,
    onQuickPinBackspace: () -> Unit,
    onFingerprintClick: (() -> Unit)?
) {
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = if (isBn) "পিন দিন" else "Enter PIN",
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = if (isBn) "আপনার পিন দিন" else "Enter your pin",
        fontSize = 13.sp,
        color = Color.DarkGray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 20.dp)
    )

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

    Spacer(modifier = Modifier.height(16.dp))

    AlRajhiKeypad(
        onDigitClick = onQuickPinDigit,
        onBackspaceClick = onQuickPinBackspace,
        onFingerprintClick = onFingerprintClick,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun LojiaPasswordLoginContent(
    loginUser: String,
    onLoginUserChange: (String) -> Unit,
    loginPass: String,
    onLoginPassChange: (String) -> Unit,
    loginPassVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onForgotPassword: () -> Unit,
    loginErrorMessage: String?,
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isUserFocused by remember { mutableStateOf(false) }
    var isPassFocused by remember { mutableStateOf(false) }

    Text(
        text = "Username or Email",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF334155),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isUserFocused) Color(0xFF4338CA) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = loginUser,
                onValueChange = onLoginUserChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isUserFocused = it.isFocused }
                    .testTag("etLoginUser")
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Password",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF334155),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isPassFocused) Color(0xFF4338CA) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = loginPass,
                onValueChange = onLoginPassChange,
                singleLine = true,
                visualTransformation = if (loginPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSignIn()
                    }
                ),
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isPassFocused = it.isFocused }
                    .testTag("etLoginPass")
            )
            IconButton(
                onClick = onTogglePasswordVisible,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (loginPassVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = "Toggle password",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onRememberMeChange(!rememberMe) }
                .testTag("cbRemember")
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (rememberMe) Color(0xFF4338CA) else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (rememberMe) Color(0xFF4338CA) else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (rememberMe) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Remember me",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155)
            )
        }

        Text(
            text = "Forgot password?",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4338CA),
            modifier = Modifier
                .clickable { onForgotPassword() }
                .testTag("tvForgot")
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (!loginErrorMessage.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFECACA)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Text(
                text = loginErrorMessage,
                color = Color(0xFFDC2626),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }

    Button(
        onClick = onSignIn,
        enabled = !isSigningIn,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4338CA),
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btnLogin")
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Local secure storage · On-device database",
            fontSize = 13.sp,
            color = Color(0xFF64748B)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Don't have an account? ",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = "Register here",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4338CA),
            modifier = Modifier
                .clickable { onRegisterClick() }
                .testTag("goRegister")
        )
    }
}
