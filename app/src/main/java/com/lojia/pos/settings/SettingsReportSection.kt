package com.lojia.pos.settings

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.lojia.pos.ui.common.LojiaTextField
import com.lojia.pos.R
import com.lojia.pos.data.*
import com.lojia.pos.util.*
import com.lojia.pos.ui.common.*
import com.lojia.pos.ui.theme.*
import com.lojia.pos.auth.*
import com.lojia.pos.pos.*
import com.lojia.pos.report.*
import com.lojia.pos.settings.*

import com.lojia.pos.util.AppLanguageManager
import com.lojia.pos.util.SecurityUtils


import android.content.Intent

import android.net.Uri

import android.widget.Toast


import androidx.compose.animation.*


import androidx.compose.animation.core.*


import androidx.compose.foundation.BorderStroke


import androidx.compose.foundation.background


import androidx.compose.foundation.clickable


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn


import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.shape.CircleShape


import androidx.compose.material.icons.Icons
import androidx.fragment.app.FragmentActivity


import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight


import androidx.compose.material.icons.outlined.*


import androidx.compose.material3.*


import androidx.compose.runtime.*


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.platform.LocalContext


import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.text.input.PasswordVisualTransformation


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp


import java.text.SimpleDateFormat

import java.util.*


import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsReportSection(
    reportViewModel: ReportViewModel,
    language: AppLanguage,
    isAdmin: Boolean,
    onRestrictedClick: (action: () -> Unit) -> Unit,
    onSwitchModule: (AppModule) -> Unit
) {
    val context = LocalContext.current
    val userProfile by reportViewModel.userProfile.collectAsState()
    val businessProfile by reportViewModel.businessProfile.collectAsState()
    val isBackingUp by reportViewModel.isBackingUp.collectAsState()
    val backupProgress by reportViewModel.backupProgress.collectAsState()
    val lastBackupTime: Long by reportViewModel.lastBackupTime.collectAsState()
    val googleAccount: String by reportViewModel.googleAccount.collectAsState()
    val autoBackupFreq: String by reportViewModel.autoBackupFrequency.collectAsState()
    val backupCellular: Boolean by reportViewModel.backupUsingCellular.collectAsState()

    val selectedReportMenu by reportViewModel.selectedReportSettingsMenu.collectAsState()
    val currentMenu = selectedReportMenu ?: "backup"
    val cashiers by reportViewModel.cashiers.collectAsState()
    val currentCountry by reportViewModel.currentCountry.collectAsState()
    var showAddCashierDialog by remember { mutableStateOf(false) }
    var cashierToDelete by remember { mutableStateOf<Cashier?>(null) }
    var langCountryTab by remember(currentMenu) { mutableIntStateOf(if (currentMenu == "country") 1 else 0) }
    var countrySearch by remember { mutableStateOf("") }

    // 1. Profile State
    var fullName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "Demo Owner") }
    var username by remember(userProfile) { mutableStateOf(userProfile?.username ?: "demo") }
    var email by remember(userProfile) { mutableStateOf(userProfile?.email ?: "demo@lojia.local") }
    var password by remember(userProfile) { mutableStateOf(DevCredentials.DEFAULT_PASSWORD) }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "Demo City") }

    // 2. Security State
    var biometricEnabled by remember(userProfile) { mutableStateOf(userProfile?.isBiometricEnabled ?: false) }
    var pinValue by remember(userProfile) { mutableStateOf(userProfile?.pin.orEmpty()) }
    var showChangePinModal by remember { mutableStateOf(false) }

    // 3. Backup State
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val lastBackupFormatted = remember(lastBackupTime) {
        if (lastBackupTime > 0L) dateFormatter.format(Date(lastBackupTime)) else "Today at 02:45 PM"
    }

    // Active edit dialog
    var editFieldDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showTermsModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showSupportTicketDialog by remember { mutableStateOf(false) }
    var langSearch by remember { mutableStateOf("") }

    fun persistProfile(
        fName: String = fullName,
        uName: String = username,
        mail: String = email,
        pass: String = password,
        ph: String = phone,
        addr: String = address,
        bio: Boolean = biometricEnabled,
        p: String = pinValue
    ) {
        val current = userProfile ?: UserProfile()
        reportViewModel.saveUserProfile(
            current.copy(
                fullName = fName,
                username = uName,
                email = mail,
                passwordHash = SecurityUtils.hashSecret(pass),
                phone = ph,
                address = addr,
                isBiometricEnabled = bio,
                pin = SecurityUtils.hashSecret(p)
            )
        )
    }

    val isRootMenu = currentMenu == "root" || currentMenu == "all" || currentMenu == "overview"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        // Quick Action Banner: Daily Shift Report Generator (Shown only in root settings overview or shift sub-menu)
        if (isRootMenu || currentMenu == "daily_shift_report") {
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00796B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            AutoText(id = R.string.daily_shift_report, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                            AutoText(id = R.string.audit_drawer_cash_create, fontSize = 11.5.sp, color = Color(0xFF475569))
                        }
                    }
                    Button(
                        onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        AutoText(id = R.string.open_2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))
        }

        when (currentMenu) {
            // =================================================================
            // 0. ROOT / ALL SETTINGS HUB (Overview of all Shift Report Settings)
            // =================================================================
            "root", "all", "overview" -> {
                AutoText(
                    id = R.string.staff_admin_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Person,
                    title = "Profile",
                    subtitle = stringResource(R.string.profile_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("profile") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.People,
                    title = "Cashier",
                    subtitle = stringResource(R.string.cashiers_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("cashiers") }
                )

                AutoText(
                    id = R.string.security_data_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Shield,
                    title = "Security",
                    subtitle = stringResource(R.string.security_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("security") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Backup",
                    subtitle = stringResource(R.string.backup_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("backup") }
                )

                AutoText(
                    id = R.string.regional_interface_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = "${stringResource(R.string.language_subtitle)}: ${language.displayName}",
                    onClick = { reportViewModel.selectReportSettingsMenu("language") }
                )

                AutoText(
                    id = R.string.system_support_section,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = stringResource(R.string.about_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("about") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = "Support",
                    subtitle = stringResource(R.string.support_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("support") }
                )

                LoyverseMenuItemRow(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Switch Module",
                    subtitle = stringResource(R.string.switch_module_subtitle),
                    onClick = { reportViewModel.selectReportSettingsMenu("switch_module") }
                )
            }

            // =================================================================
            // 0. DAILY SHIFT REPORT GENERATOR
            // =================================================================
            "daily_shift_report", "shift_report", "shift" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Assessment,
                    title = stringResource(R.string.daily_shift_report),
                    subtitle = stringResource(R.string.desc_create_shift_handover),
                    trailing = {
                        Button(
                            onClick = { onSwitchModule(AppModule.SHIFT_REPORT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.launch_1), fontSize = 12.sp)
                        }
                    },
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.PictureAsPdf,
                    title = stringResource(R.string.title_shift_reports_pdf_archives),
                    subtitle = stringResource(R.string.desc_search_shift_reports),
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
            }
            // =================================================================
            // EMPLOYEE / CASHIER MANAGEMENT
            // =================================================================
            "cashiers", "cashier", "staff", "employees", "employee" -> {
                CashierManagementSection(
                    cashiers = cashiers,
                    onAddCashierClick = { showAddCashierDialog = true },
                    onDeleteCashierClick = { cashierToDelete = it },
                    onToggleStatusClick = { reportViewModel.updateCashier(it) }
                )
            }

            // =================================================================
            // 1. PROFILE
            // =================================================================
            "profile" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    val bProfile = businessProfile ?: com.lojia.pos.data.BusinessProfile()
                    BusinessLogoPickerCard(
                        businessProfile = bProfile,
                        onSaveProfile = { updated ->
                            reportViewModel.saveBusinessProfile(updated)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clean List Card for Profile Detail Items
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Badge,
                                label = stringResource(R.string.full_name),
                                value = fullName,
                                onClick = { editFieldDialog = "Full name" to fullName }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.AccountCircle,
                                label = stringResource(R.string.username),
                                value = username,
                                onClick = { editFieldDialog = "Username" to username }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Email,
                                label = stringResource(R.string.email),
                                value = email,
                                onClick = { editFieldDialog = "Email" to email }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Lock,
                                label = stringResource(R.string.password),
                                value = "••••••••",
                                onClick = { editFieldDialog = "Password" to password }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.Phone,
                                label = stringResource(R.string.phone_number),
                                value = phone,
                                onClick = { editFieldDialog = "Number" to phone }
                            )
                            ProfileDetailItemRow(
                                icon = Icons.Outlined.LocationOn,
                                label = stringResource(R.string.address),
                                value = address,
                                isLastItem = true,
                                onClick = { editFieldDialog = "Address" to address }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // =================================================================
            // 2. SECURITY (Biometric & MPIN)
            // =================================================================
            "security", "mpin", "pin" -> {
                var isMpinExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Biometric Login
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFF0FDF4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = stringResource(R.string.cd_biometric_icon),
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.biometric_login),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (biometricEnabled) stringResource(R.string.biometric_enabled_desc) else stringResource(R.string.biometric_disabled_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (biometricEnabled) Color(0xFF16A34A) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        val fragActivity = context as? FragmentActivity
                                        val bioStatus = BiometricAuthManager.checkBiometricAvailability(context)
                                        if (fragActivity != null && bioStatus == BiometricStatus.AVAILABLE) {
                                            BiometricAuthManager.showBiometricPrompt(
                                                activity = fragActivity,
                                                title = "Biometric Verification",
                                                subtitle = "Touch sensor to activate Biometric Login",
                                                description = "Verifying biometric security for your account.",
                                                onResult = { result ->
                                                    when (result) {
                                                        is BiometricAuthResult.Success -> {
                                                            biometricEnabled = true
                                                            persistProfile(bio = true)
                                                            Toast.makeText(context, context.getString(R.string.biometric_login_enabled), Toast.LENGTH_SHORT).show()
                                                        }
                                                        is BiometricAuthResult.Failed -> {
                                                            Toast.makeText(context, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show()
                                                        }
                                                        is BiometricAuthResult.Error -> {
                                                            Toast.makeText(context, result.errString.toString(), Toast.LENGTH_SHORT).show()
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            )
                                        } else {
                                            biometricEnabled = true
                                            persistProfile(bio = true)
                                            Toast.makeText(context, context.getString(R.string.biometric_login_enabled), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        biometricEnabled = false
                                        persistProfile(bio = false)
                                        Toast.makeText(context, context.getString(R.string.biometric_login_disabled), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF16A34A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                }
            }

            // =================================================================
            // 3. BACKUP
            // =================================================================
            "backup" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.backup_settings),
                    subtitle = stringResource(R.string.backup_settings_desc),
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.AccessTime,
                    title = stringResource(R.string.last_backup),
                    subtitle = lastBackupFormatted,
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = if (isBackingUp) "${stringResource(R.string.backing_up)}... (${(backupProgress * 100).toInt()}%)" else stringResource(R.string.backup_now_title),
                    subtitle = if (isBackingUp) stringResource(R.string.please_wait_cloud_sync) else stringResource(R.string.tap_to_start_backup),
                    trailing = {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                progress = { backupProgress },
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = Color(0xFF4F46E5)
                            )
                        } else {
                            Button(
                                onClick = { reportViewModel.triggerCloudBackup() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(stringResource(R.string.backup_now_title), fontSize = 12.sp)
                            }
                        }
                    },
                    onClick = {
                        if (!isBackingUp) {
                            reportViewModel.triggerCloudBackup()
                        }
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = stringResource(R.string.google_account),
                    subtitle = googleAccount ?: "",
                    onClick = {
                        editFieldDialog = "Google Account" to (googleAccount ?: "")
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Autorenew,
                    title = stringResource(R.string.auto_backups),
                    subtitle = when (autoBackupFreq) {
                        "Daily" -> stringResource(R.string.daily)
                        "Weekly" -> stringResource(R.string.weekly)
                        "Monthly" -> stringResource(R.string.monthly)
                        else -> autoBackupFreq ?: stringResource(R.string.daily)
                    },
                    onClick = {
                        val nextFreq = when (autoBackupFreq) {
                            "Daily" -> "Weekly"
                            "Weekly" -> "Monthly"
                            else -> "Daily"
                        }
                        reportViewModel.autoBackupFrequency.value = nextFreq
                        Toast.makeText(context, context.getString(R.string.auto_backup_set_to, nextFreq), Toast.LENGTH_SHORT).show()
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.SignalCellularAlt,
                    title = stringResource(R.string.backup_cellular),
                    subtitle = if (backupCellular) stringResource(R.string.allowed_cellular) else stringResource(R.string.wifi_only),
                    trailing = {
                        Switch(
                            checked = backupCellular,
                            onCheckedChange = { enabled ->
                                reportViewModel.backupUsingCellular.value = enabled
                                Toast.makeText(context, context.getString(if (enabled) R.string.cellular_backup_enabled else R.string.wifi_only_backup), Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onClick = {
                        reportViewModel.backupUsingCellular.value = !backupCellular
                    }
                )
            }

            // =================================================================
            // 4. LANGUAGE / COUNTRY SELECTION
            // =================================================================
            "country", "countries", "currency", "language" -> {
                // Top Segmented Tab Row
                TabRow(
                    selectedTabIndex = langCountryTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Color(0xFF4F46E5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = langCountryTab == 0,
                        onClick = { langCountryTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌐 ", fontSize = 14.sp)
                                Text(stringResource(R.string.language), fontWeight = if (langCountryTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = langCountryTab == 1,
                        onClick = { langCountryTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌍 ", fontSize = 14.sp)
                                Text(stringResource(R.string.country), fontWeight = if (langCountryTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                if (langCountryTab == 0) {
                    // ==========================================
                    // 🌐 APP LANGUAGE TAB (POWERED BY DATASTORE)
                    // ==========================================
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        LanguageSettingsComponent(
                            currentLanguage = language,
                            accentColor = PrimaryIndigo,
                            onLanguageChanged = { newLang ->
                                reportViewModel.setLanguage(newLang)
                            }
                        )
                    }
                } else {
                    // ==========================================
                    // 🌍 COUNTRY TAB (RIGHT TAB - NO TOP CARD)
                    // ==========================================
                    LojiaTextField(
                        value = countrySearch,
                        onValueChange = { countrySearch = it },
                        placeholder = { Text(stringResource(R.string.search_country), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val allCountries = AppCountry.values().filter {
                        it.displayNameEn.contains(countrySearch, ignoreCase = true) ||
                        it.displayNameBn.contains(countrySearch, ignoreCase = true) ||
                        it.displayNameAr.contains(countrySearch, ignoreCase = true) ||
                        it.currencyCode.contains(countrySearch, ignoreCase = true) ||
                        it.currencySymbol.contains(countrySearch, ignoreCase = true)
                    }

                    allCountries.forEach { c ->
                        val isSelected = c == currentCountry
                        val displayName = c.getLocalizedName(language)

                        Surface(
                            color = if (isSelected) Color(0xFFF5F3FF) else Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reportViewModel.setCountry(c)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.country_set_toast_fmt, displayName, c.currencyCode, c.currencySymbol),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = c.flag, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "${c.currencyCode} (${c.currencySymbol}) • VAT: ${c.defaultVatRate.toInt()}%",
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color(0xFF6366F1) else Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = stringResource(R.string.cd_select),
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
                    }
                }
            }

            // =================================================================
            // 5. ABOUT
            // =================================================================
            "about" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.title_lojia_pos_shift_system),
                    subtitle = stringResource(R.string.desc_app_version_production),
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Verified,
                    title = stringResource(R.string.title_system_architecture),
                    subtitle = stringResource(R.string.desc_system_architecture),
                    onClick = {}
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.terms_conditions),
                    subtitle = stringResource(R.string.desc_review_eula_terms),
                    onClick = { showTermsModal = true }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.privacy_policy),
                    subtitle = stringResource(R.string.desc_gdpr_cloud_security),
                    onClick = { showPrivacyModal = true }
                )
            }

            // =================================================================
            // 6. SUPPORT
            // =================================================================
            "support" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.HeadsetMic,
                    title = stringResource(R.string.title_24_7_helpline),
                    subtitle = "+880 1700-000000",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Chat,
                    title = stringResource(R.string.title_whatsapp_support),
                    subtitle = stringResource(R.string.desc_whatsapp_support),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // =================================================================
            // 7. SWITCH MODULE
            // =================================================================
            "switch_module" -> {
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Assessment,
                    title = stringResource(R.string.title_shift_report_module),
                    subtitle = stringResource(R.string.desc_currently_active_shift_report),
                    trailing = {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.active), tint = Color(0xFF4F46E5))
                    },
                    onClick = { onSwitchModule(AppModule.SHIFT_REPORT) }
                )
                LoyverseMenuItemRow(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(R.string.title_shop_module),
                    subtitle = stringResource(R.string.desc_switch_to_shop),
                    onClick = { onSwitchModule(AppModule.SHOPPING) }
                )
            }
        }
    }

    // =========================================================================
    // MODALS
    // =========================================================================

    // Edit field modal
    if (editFieldDialog != null) {
        val (fieldName, fieldVal) = editFieldDialog!!
        var tempVal by remember(fieldName) { mutableStateOf(fieldVal) }
        AlertDialog(
            onDismissRequest = { editFieldDialog = null },
            title = { Text(stringResource(R.string.edit_field_title, fieldName), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.layout.Box(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    LojiaTextField(
                        value = tempVal,
                        onValueChange = { tempVal = it },
                        label = { Text(fieldName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (fieldName) {
                            "Full name" -> { fullName = tempVal; persistProfile(fName = tempVal) }
                            "Username" -> { username = tempVal; persistProfile(uName = tempVal) }
                            "Email" -> { email = tempVal; persistProfile(mail = tempVal) }
                            "Password" -> { password = tempVal; persistProfile(pass = tempVal) }
                            "Number" -> { phone = tempVal; persistProfile(ph = tempVal) }
                            "Address" -> { address = tempVal; persistProfile(addr = tempVal) }
                            "Google Account" -> reportViewModel.googleAccount.value = tempVal
                        }
                        Toast.makeText(context, context.getString(R.string.field_updated_msg, fieldName), Toast.LENGTH_SHORT).show()
                        editFieldDialog = null
                    }
                ) {
                    Text(stringResource(R.string.save_5))
                }
            },
            dismissButton = { TextButton(onClick = { editFieldDialog = null }) { Text(stringResource(R.string.cancel_18)) } }
        )
    }

    // Terms Modal
    if (showTermsModal) {
        AlertDialog(
            onDismissRequest = { showTermsModal = false },
            title = { Text(stringResource(R.string.terms_of_service), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.by_using_lojia_shift),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = { TextButton(onClick = { showTermsModal = false }) { Text(stringResource(R.string.close_7)) } }
        )
    }

    // Privacy Modal
    if (showPrivacyModal) {
        AlertDialog(
            onDismissRequest = { showPrivacyModal = false },
            title = { Text(stringResource(R.string.privacy_policy_2), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.your_financial_data_shift),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacyModal = false }) { Text(stringResource(R.string.close_7)) } }
        )
    }

    // Add Cashier Modal
    if (showAddCashierDialog) {
        AddCashierDialog(
            onDismissRequest = { showAddCashierDialog = false },
            onConfirmAdd = { name, pin ->
                reportViewModel.addCashier(name, pin, "CASHIER")
                Toast.makeText(context, context.getString(R.string.user_added_success_msg, "Cashier", name), Toast.LENGTH_SHORT).show()
                showAddCashierDialog = false
            }
        )
    }

    // Delete Cashier Confirmation Modal
    if (cashierToDelete != null) {
        val target = cashierToDelete!!
        AlertDialog(
            onDismissRequest = { cashierToDelete = null },
            title = { Text(stringResource(R.string.remove_member), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.confirm_delete_cashier_prompt, target.name, target.role))
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportViewModel.deleteCashier(target)
                        Toast.makeText(context, context.getString(R.string.user_deleted_msg, target.name), Toast.LENGTH_SHORT).show()
                        cashierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(stringResource(R.string.delete_1))
                }
            },
            dismissButton = {
                TextButton(onClick = { cashierToDelete = null }) { Text(stringResource(R.string.cancel_18)) }
            }
        )
    }
}
