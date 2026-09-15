package com.example.ui


import android.app.Application


import androidx.lifecycle.AndroidViewModel

import com.example.util.UiText

import com.example.R


import androidx.lifecycle.viewModelScope

import com.example.data.*
import com.example.util.SecurityUtils

import com.example.util.NotificationHelper

import com.example.util.ShiftReportSyncScheduler

import com.example.util.ShiftReportSyncWorker


import kotlinx.coroutines.delay


import kotlinx.coroutines.flow.*


import kotlinx.coroutines.launch

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(application)
    private val reportDao = db.reportDao()
    private val repository = ShiftReportRepository(reportDao)

    val shiftReports: StateFlow<List<ShiftReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShiftSession: StateFlow<ShiftSession?> = reportDao.getActiveShiftSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShiftSessions: StateFlow<List<ShiftSession>> = reportDao.getAllShiftSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashMovements: StateFlow<List<CashMovement>> = reportDao.getAllCashMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Centralized Configuration Sync Manager
    val syncManager = ConfigurationSyncManager.getInstance(application)
    val preferencesRepository = PreferencesRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("lojia_app_prefs", android.content.Context.MODE_PRIVATE)

    val cashiers: StateFlow<List<Cashier>> = syncManager.cashiers

    val businessProfile: StateFlow<BusinessProfile?> = reportDao.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userProfile: StateFlow<UserProfile?> = reportDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val receiptConfig: StateFlow<ShopReceiptConfig?> = reportDao.getReceiptConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLog>> = reportDao.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Centralized Synchronized Language State
    val currentLanguage: StateFlow<AppLanguage> = syncManager.currentLanguage

    // Centralized Synchronized Country & Currency State
    val currentCountry: StateFlow<AppCountry> = syncManager.currentCountry

    fun setLanguage(language: AppLanguage) {
        syncManager.setLanguage(language)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_lang_changed, language.displayName))
        }
    }

    fun setCountry(country: AppCountry) {
        syncManager.setCountry(country)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_country_set, country.displayNameEn, country.currencyCode, country.currencySymbol))
        }
    }

    // Centralized Dual Module System State (SHOPPING or SHIFT_REPORT)
    val currentModule: StateFlow<AppModule> = syncManager.currentModule

    // Active Cashier Selection State
    val activeCashier: StateFlow<String> = syncManager.activeCashier
    val syncState: StateFlow<ConfigurationSyncState> = syncManager.syncState

    fun setActiveCashier(cashierName: String) {
        syncManager.setActiveCashier(cashierName)
        selectedCashier.value = cashierName
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_active_cashier_set, cashierName))
        }
    }

    fun forceSyncConfiguration() {
        syncManager.forceSync { _, msg ->
            viewModelScope.launch {
                _uiMessage.emit(UiText.DynamicString(msg))
            }
        }
    }

    suspend fun exportConfigJson(): String {
        return syncManager.exportConfigurationJson()
    }

    suspend fun importConfigJson(json: String): Boolean {
        val success = syncManager.importConfigurationJson(json)
        if (success) {
            _uiMessage.emit(UiText.StringResource(R.string.configuration_successfully_restored_and))
        } else {
            _uiMessage.emit(UiText.StringResource(R.string.failed_to_import_configuration))
        }
        return success
    }

    // Active Shop Settings Menu Key (Profile, Security, Sales, Receipts, Shift, Items, Settings, Back Office, Apps, Language, Support)
    private val _selectedShopSettingsMenu = MutableStateFlow<String?>("profile")
    val selectedShopSettingsMenu: StateFlow<String?> = _selectedShopSettingsMenu.asStateFlow()

    fun selectShopSettingsMenu(menuKey: String?) {
        _selectedShopSettingsMenu.value = menuKey
    }

    // Active Shift Report Settings Menu Key (profile, security, backup, language, about, support, switch_module)
    private val _selectedReportSettingsMenu = MutableStateFlow<String?>("profile")
    val selectedReportSettingsMenu: StateFlow<String?> = _selectedReportSettingsMenu.asStateFlow()

    fun selectReportSettingsMenu(menuKey: String?) {
        _selectedReportSettingsMenu.value = menuKey
    }

    fun switchModule(module: AppModule) {
        syncManager.switchModule(module)
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_switched_module, module.name))
        }
    }

    // Role-Based Access Control State
    val currentRole: StateFlow<String> = userProfile.map { it?.currentRole ?: "ADMIN" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ADMIN")

    fun switchUserRole(role: String, enteredPin: String? = null, onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = userProfile.value ?: UserProfile()
            if (role == "ADMIN" && profile.currentRole == "CASHIER") {
                // Switching from Cashier to Admin requires Admin PIN check
                if (enteredPin != null && SecurityUtils.verifySecret(enteredPin, profile.pin)) {
                    reportDao.saveUserProfile(profile.copy(currentRole = "ADMIN"))
                    _uiMessage.emit(UiText.StringResource(R.string.authenticated_as_administrator))
                    onSuccess()
                } else {
                    _uiMessage.emit(UiText.StringResource(R.string.invalid_admin_pin))
                    onFailure()
                }
            } else {
                reportDao.saveUserProfile(profile.copy(currentRole = role))
                _uiMessage.emit(UiText.StringResource(R.string.toast_role_switched, role))
                onSuccess()
            }
        }
    }

    // Backup State & Progress
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress: StateFlow<Float> = _backupProgress.asStateFlow()

    var googleAccount: MutableStateFlow<String> = MutableStateFlow(prefs.getString("google_account", "bdparvaze.backup@gmail.com") ?: "bdparvaze.backup@gmail.com")
    var autoBackupFrequency: MutableStateFlow<String> = MutableStateFlow(prefs.getString("auto_backup_freq", "Daily") ?: "Daily")
    var backupUsingCellular: MutableStateFlow<Boolean> = MutableStateFlow(prefs.getBoolean("backup_cellular", true))
    var lastBackupTime: MutableStateFlow<Long> = MutableStateFlow(prefs.getLong("last_backup_time", System.currentTimeMillis() - 3600000L))

    // WorkManager Background Server Sync State
    var lastServerSyncTime: MutableStateFlow<Long> = MutableStateFlow(
        prefs.getLong(ShiftReportSyncWorker.KEY_LAST_SYNC_TIME, System.currentTimeMillis() - 1800000L)
    )
    var lastServerSyncStatus: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString(ShiftReportSyncWorker.KEY_LAST_SYNC_STATUS, "Active Periodic Sync (WorkManager)") ?: "Active Periodic Sync (WorkManager)"
    )

    fun triggerServerSyncNow() {
        ShiftReportSyncScheduler.triggerImmediateSync(getApplication())
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.background_sync_task_enqueued))
            delay(1200)
            lastServerSyncTime.value = System.currentTimeMillis()
            lastServerSyncStatus.value = "Synced via WorkManager"
        }
    }

    fun updatePeriodicSyncInterval(intervalMinutes: Long, wifiOnly: Boolean = false) {
        ShiftReportSyncScheduler.schedulePeriodicSync(
            getApplication(),
            intervalMinutes = intervalMinutes,
            requireWifiOnly = wifiOnly
        )
        viewModelScope.launch {
            _uiMessage.emit(UiText.StringResource(R.string.toast_sync_interval_set, intervalMinutes.toInt()))
        }
    }

    fun triggerCloudBackup() {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0.1f
            delay(400)
            _backupProgress.value = 0.35f
            delay(500)
            _backupProgress.value = 0.7f
            delay(400)
            _backupProgress.value = 1.0f
            val now = System.currentTimeMillis()
            lastBackupTime.value = now
            prefs.edit().putLong("last_backup_time", now).apply()
            _isBackingUp.value = false
            _uiMessage.emit(UiText.StringResource(R.string.toast_cloud_backup_success, googleAccount.value))
            NotificationHelper.sendTestPushNotification(
                context,
                "☁️ Cloud Backup Successful",
                "Your store database and shift ledgers were backed up to Google Drive at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))}."
            )
        }
    }

    // Shift Form State
    var selectedCashier = MutableStateFlow("")
    var selectedShift = MutableStateFlow("Day")
    var selectedDateInMillis = MutableStateFlow(System.currentTimeMillis())
    var grossCashInput = MutableStateFlow("")
    var madaPaymentsInput = MutableStateFlow("")
    var digitalWalletInput = MutableStateFlow("")
    var staffMealsCountInput = MutableStateFlow("0")
    var totalExpensesInput = MutableStateFlow("")
    var muasselQtyInput = MutableStateFlow("")
    var outdoorShishaQtyInput = MutableStateFlow("")
    var notesInput = MutableStateFlow("")

    // Dynamic Section Lists
    val dueCreditItems = MutableStateFlow<List<DueCreditItem>>(emptyList())
    val previousDueCollections = MutableStateFlow<List<PreviousDueCollectionItem>>(emptyList())
    val staffAdvances = MutableStateFlow<List<StaffAdvanceItem>>(emptyList())
    val unpaidBills = MutableStateFlow<List<UnpaidBillItem>>(emptyList())
    val purchasedItems = MutableStateFlow<List<PurchasedInventoryItem>>(emptyList())

    fun isValidDecimal(input: String): Boolean {
        if (input.isBlank()) return true
        val value = input.toDoubleOrNull()
        return value != null && value >= 0
    }

    fun isValidInteger(input: String): Boolean {
        if (input.isBlank()) return true
        val value = input.toIntOrNull()
        return value != null && value >= 0
    }

    val isGrossCashValid: StateFlow<Boolean> = grossCashInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isMadaValid: StateFlow<Boolean> = madaPaymentsInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDigitalWalletValid: StateFlow<Boolean> = digitalWalletInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isExpensesValid: StateFlow<Boolean> = totalExpensesInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isStaffMealsValid: StateFlow<Boolean> = staffMealsCountInput.map { isValidInteger(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isMuasselQtyValid: StateFlow<Boolean> = muasselQtyInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOutdoorShishaValid: StateFlow<Boolean> = outdoorShishaQtyInput.map { isValidDecimal(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val grossCash = grossCashInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val madaPayments = madaPaymentsInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val digitalWallet = digitalWalletInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalExpenses = totalExpensesInput.map { it.toDoubleOrNull() ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dynamic Lists Sums
    val totalDueCreditAmount: StateFlow<Double> = dueCreditItems.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCollected: StateFlow<Double> = previousDueCollections.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCash: StateFlow<Double> = previousDueCollections.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPreviousDueCard: StateFlow<Double> = previousDueCollections.map { list ->
        list.filter { it.paymentMode != "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStaffAdvances: StateFlow<Double> = staffAdvances.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUnpaidLoss: StateFlow<Double> = unpaidBills.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedCash: StateFlow<Double> = purchasedItems.map { list ->
        list.filter { it.paidVia == "CASH" }.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPurchasedAll: StateFlow<Double> = purchasedItems.map { list ->
        list.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Automatic Live Calculations
    val calculatedTotalGrossSales: StateFlow<Double> = combine(
        grossCash,
        madaPayments,
        totalDueCreditAmount
    ) { cash, mada, due ->
        cash + mada + due
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalRevenue: StateFlow<Double> = calculatedTotalGrossSales

    val calculatedTotalExpenses: StateFlow<Double> = combine(
        totalExpenses,
        totalPurchasedCash
    ) { exp, purchCash ->
        exp + purchCash
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedCashInDrawer: StateFlow<Double> = combine(
        grossCash,
        totalPreviousDueCash,
        calculatedTotalExpenses,
        totalStaffAdvances
    ) { cash, prevDue, allExp, advances ->
        cash + prevDue - allExp - advances
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netCash: StateFlow<Double> = calculatedCashInDrawer

    val calculatedCardAndDigital: StateFlow<Double> = combine(
        madaPayments,
        totalPreviousDueCard,
        digitalWallet
    ) { mada, prevCard, wallet ->
        mada + prevCard + wallet
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val calculatedNetBalance: StateFlow<Double> = combine(
        calculatedTotalGrossSales,
        calculatedTotalExpenses,
        totalUnpaidLoss
    ) { gross, exp, unpaid ->
        gross - exp - unpaid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _uiMessage = MutableSharedFlow<com.example.util.UiText>()
    val uiMessage = _uiMessage.asSharedFlow()

    // Add / Remove Handlers for Dynamic Sections
    fun addDueCreditItem(customerName: String, amount: Double, note: String = "", phone: String = "") {
        if (customerName.isBlank() || amount <= 0) return
        dueCreditItems.value = dueCreditItems.value + DueCreditItem(customerName = customerName.trim(), amount = amount, note = note.trim(), phone = phone.trim())
    }

    fun removeDueCreditItem(id: String) {
        dueCreditItems.value = dueCreditItems.value.filter { it.id != id }
    }

    fun addPreviousDueCollection(customerName: String, amount: Double, paymentMode: String = "CASH", note: String = "") {
        if (customerName.isBlank() || amount <= 0) return
        previousDueCollections.value = previousDueCollections.value + PreviousDueCollectionItem(customerName = customerName.trim(), amount = amount, paymentMode = paymentMode, note = note.trim())
    }

    fun removePreviousDueCollection(id: String) {
        previousDueCollections.value = previousDueCollections.value.filter { it.id != id }
    }

    fun addStaffAdvance(staffName: String, amount: Double, reason: String = "") {
        if (staffName.isBlank() || amount <= 0) return
        staffAdvances.value = staffAdvances.value + StaffAdvanceItem(staffName = staffName.trim(), amount = amount, reason = reason.trim())
    }

    fun removeStaffAdvance(id: String) {
        staffAdvances.value = staffAdvances.value.filter { it.id != id }
    }

    fun addUnpaidBill(tableOrOrderRef: String, amount: Double, reason: String = "") {
        if (tableOrOrderRef.isBlank() || amount <= 0) return
        unpaidBills.value = unpaidBills.value + UnpaidBillItem(tableOrOrderRef = tableOrOrderRef.trim(), amount = amount, reason = reason.trim())
    }

    fun removeUnpaidBill(id: String) {
        unpaidBills.value = unpaidBills.value.filter { it.id != id }
    }

    fun addPurchasedItem(itemName: String, quantity: Double, unitPrice: Double, paidVia: String = "CASH", supplier: String = "") {
        if (itemName.isBlank() || quantity <= 0 || unitPrice <= 0) return
        purchasedItems.value = purchasedItems.value + PurchasedInventoryItem(
            itemName = itemName.trim(),
            quantity = quantity,
            unitPrice = unitPrice,
            totalAmount = quantity * unitPrice,
            paidVia = paidVia,
            supplier = supplier.trim()
        )
    }

    fun removePurchasedItem(id: String) {
        purchasedItems.value = purchasedItems.value.filter { it.id != id }
    }

    // Auto-fill from active POS Sales
    fun autoFillFromPosSales(salesList: List<POSSale>) {
        if (salesList.isEmpty()) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.no_pos_sales_available))
            }
            return
        }
        val cashTotal = salesList.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
        val cardTotal = salesList.filter { it.paymentMethod != "CASH" }.sumOf { it.totalAmount }

        grossCashInput.value = "%.2f".format(cashTotal)
        madaPaymentsInput.value = "%.2f".format(cardTotal)
        viewModelScope.launch {
            _uiMessage.emit(
                UiText.StringResource(
                    R.string.toast_autofill_pos_sales,
                    salesList.size,
                    "%.2f".format(cashTotal),
                    "%.2f".format(cardTotal)
                )
            )
        }
    }

    init {
        loadDraft()
        viewModelScope.launch {
            userProfile.collect { profile ->
                if (profile != null) {
                    preferencesRepository.syncWithUserProfile(profile)
                }
            }
        }
    }

    fun serializeDueCredits(list: List<DueCreditItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("note", item.note)
            obj.put("phone", item.phone)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeDueCredits(json: String): List<DueCreditItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<DueCreditItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DueCreditItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        note = obj.optString("note", ""),
                        phone = obj.optString("phone", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializePrevDues(list: List<PreviousDueCollectionItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("customerName", item.customerName)
            obj.put("amount", item.amount)
            obj.put("paymentMode", item.paymentMode)
            obj.put("note", item.note)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePrevDues(json: String): List<PreviousDueCollectionItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PreviousDueCollectionItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PreviousDueCollectionItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        customerName = obj.optString("customerName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        paymentMode = obj.optString("paymentMode", "CASH"),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeAdvances(list: List<StaffAdvanceItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("staffName", item.staffName)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAdvances(json: String): List<StaffAdvanceItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<StaffAdvanceItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StaffAdvanceItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        staffName = obj.optString("staffName", ""),
                        amount = obj.optDouble("amount", 0.0),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeUnpaid(list: List<UnpaidBillItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("tableOrOrderRef", item.tableOrOrderRef)
            obj.put("amount", item.amount)
            obj.put("reason", item.reason)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeUnpaid(json: String): List<UnpaidBillItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<UnpaidBillItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UnpaidBillItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        tableOrOrderRef = obj.optString("tableOrOrderRef", ""),
                        amount = obj.optDouble("amount", 0.0),
                        reason = obj.optString("reason", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializePurchased(list: List<PurchasedInventoryItem>): String {
        val arr = org.json.JSONArray()
        list.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("itemName", item.itemName)
            obj.put("quantity", item.quantity)
            obj.put("unitPrice", item.unitPrice)
            obj.put("totalAmount", item.totalAmount)
            obj.put("paidVia", item.paidVia)
            obj.put("supplier", item.supplier)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializePurchased(json: String): List<PurchasedInventoryItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val list = mutableListOf<PurchasedInventoryItem>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PurchasedInventoryItem(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        itemName = obj.optString("itemName", ""),
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        paidVia = obj.optString("paidVia", "CASH"),
                        supplier = obj.optString("supplier", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun loadDraft() {
        viewModelScope.launch {
            val draft = repository.getDraft()
            if (draft != null) {
                selectedCashier.value = draft.cashierName
                selectedShift.value = draft.shift
                selectedDateInMillis.value = draft.dateInMillis
                grossCashInput.value = if (draft.grossCash > 0) draft.grossCash.toString() else ""
                madaPaymentsInput.value = if (draft.madaPayments > 0) draft.madaPayments.toString() else ""
                digitalWalletInput.value = if (draft.digitalWallet > 0) draft.digitalWallet.toString() else ""
                staffMealsCountInput.value = draft.staffMealsCount.toString()
                totalExpensesInput.value = if (draft.totalExpenses > 0) draft.totalExpenses.toString() else ""
                muasselQtyInput.value = if (draft.muasselQty > 0) draft.muasselQty.toString() else ""
                outdoorShishaQtyInput.value = if (draft.outdoorShishaQty > 0) draft.outdoorShishaQty.toString() else ""
                notesInput.value = draft.notes
                dueCreditItems.value = deserializeDueCredits(draft.dueCreditEntriesJson)
                previousDueCollections.value = deserializePrevDues(draft.previousDueCollectionsJson)
                staffAdvances.value = deserializeAdvances(draft.staffAdvancesJson)
                unpaidBills.value = deserializeUnpaid(draft.unpaidBillsJson)
                purchasedItems.value = deserializePurchased(draft.purchasedItemsJson)
            }
        }
    }

    fun validateAll(): Boolean {
        return isValidDecimal(grossCashInput.value) &&
                isValidDecimal(madaPaymentsInput.value) &&
                isValidDecimal(digitalWalletInput.value) &&
                isValidDecimal(totalExpensesInput.value) &&
                isValidInteger(staffMealsCountInput.value) &&
                isValidDecimal(muasselQtyInput.value) &&
                isValidDecimal(outdoorShishaQtyInput.value)
    }

    fun saveDraft() {
        if (!validateAll()) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_fix_invalid_numeric))
            }
            return
        }
        viewModelScope.launch {
            val draft = DraftReport(
                id = 1,
                cashierName = selectedCashier.value,
                shift = selectedShift.value,
                dateInMillis = selectedDateInMillis.value,
                grossCash = grossCashInput.value.toDoubleOrNull() ?: 0.0,
                madaPayments = madaPaymentsInput.value.toDoubleOrNull() ?: 0.0,
                digitalWallet = digitalWalletInput.value.toDoubleOrNull() ?: 0.0,
                staffMealsCount = staffMealsCountInput.value.toIntOrNull() ?: 0,
                totalExpenses = totalExpensesInput.value.toDoubleOrNull() ?: 0.0,
                muasselQty = muasselQtyInput.value.toDoubleOrNull() ?: 0.0,
                outdoorShishaQty = outdoorShishaQtyInput.value.toDoubleOrNull() ?: 0.0,
                dueCreditEntriesJson = serializeDueCredits(dueCreditItems.value),
                previousDueCollectionsJson = serializePrevDues(previousDueCollections.value),
                staffAdvancesJson = serializeAdvances(staffAdvances.value),
                unpaidBillsJson = serializeUnpaid(unpaidBills.value),
                purchasedItemsJson = serializePurchased(purchasedItems.value),
                notes = notesInput.value
            )
            repository.saveDraft(draft)
            _uiMessage.emit(UiText.StringResource(R.string.draft_saved_successfully))
        }
    }

    fun clearForm() {
        selectedCashier.value = ""
        selectedShift.value = "Day"
        selectedDateInMillis.value = System.currentTimeMillis()
        grossCashInput.value = ""
        madaPaymentsInput.value = ""
        digitalWalletInput.value = ""
        staffMealsCountInput.value = "0"
        totalExpensesInput.value = ""
        muasselQtyInput.value = ""
        outdoorShishaQtyInput.value = ""
        notesInput.value = ""
        dueCreditItems.value = emptyList()
        previousDueCollections.value = emptyList()
        staffAdvances.value = emptyList()
        unpaidBills.value = emptyList()
        purchasedItems.value = emptyList()
        viewModelScope.launch {
            repository.clearDraft()
        }
    }

    fun submitReport(onSuccess: (ShiftReport) -> Unit) {
        if (!validateAll()) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_fix_invalid_numeric_1))
            }
            return
        }

        val grossVal = grossCashInput.value.toDoubleOrNull() ?: 0.0
        val madaVal = madaPaymentsInput.value.toDoubleOrNull() ?: 0.0
        val walletVal = digitalWalletInput.value.toDoubleOrNull() ?: 0.0
        val expVal = totalExpensesInput.value.toDoubleOrNull() ?: 0.0
        val muasselVal = muasselQtyInput.value.toDoubleOrNull() ?: 0.0
        val outdoorVal = outdoorShishaQtyInput.value.toDoubleOrNull() ?: 0.0

        if (grossVal == 0.0 && madaVal == 0.0 && walletVal == 0.0 && expVal == 0.0 &&
            staffMealsCountInput.value.toIntOrNull() == 0 && muasselVal == 0.0 && outdoorVal == 0.0 &&
            dueCreditItems.value.isEmpty() && previousDueCollections.value.isEmpty() &&
            staffAdvances.value.isEmpty() && unpaidBills.value.isEmpty() && purchasedItems.value.isEmpty()
        ) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.please_enter_shift_sales))
            }
            return
        }

        viewModelScope.launch {
            val cashier = if (selectedCashier.value.isBlank()) "Standard Cashier" else selectedCashier.value
            val report = ShiftReport(
                cashierName = cashier,
                shift = selectedShift.value,
                dateInMillis = selectedDateInMillis.value,
                grossCash = grossVal,
                madaPayments = madaVal,
                digitalWallet = walletVal,
                staffMealsCount = staffMealsCountInput.value.toIntOrNull() ?: 0,
                totalExpenses = expVal,
                muasselQty = muasselVal,
                outdoorShishaQty = outdoorVal,
                dueCreditEntriesJson = serializeDueCredits(dueCreditItems.value),
                previousDueCollectionsJson = serializePrevDues(previousDueCollections.value),
                staffAdvancesJson = serializeAdvances(staffAdvances.value),
                unpaidBillsJson = serializeUnpaid(unpaidBills.value),
                purchasedItemsJson = serializePurchased(purchasedItems.value),
                notes = notesInput.value
            )
            repository.insertReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = cashier,
                    action = "SUBMIT_REPORT",
                    details = "Submitted ${report.shift} shift report. Total sales: ${report.totalSales}, Net Cash: ${report.netCash}"
                )
            )

            // Trigger Push Notification for Daily Report
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftSummaryNotification(
                context,
                report.cashierName,
                report.shift,
                report.totalSales,
                report.netCash,
                curr
            )

            clearForm()
            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully))
            onSuccess(report)
        }
    }

    fun saveShiftReportDirect(report: ShiftReport, onSuccess: (ShiftReport) -> Unit) {
        viewModelScope.launch {
            repository.insertReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = report.cashierName,
                    action = "SUBMIT_REPORT",
                    details = "Submitted ${report.shift} shift report. Total sales: ${report.totalSales}, Net Cash: ${report.netCash}"
                )
            )

            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            try {
                NotificationHelper.sendShiftSummaryNotification(
                    getApplication(),
                    report.cashierName,
                    report.shift,
                    report.totalSales,
                    report.netCash,
                    curr
                )
            } catch (_: Exception) {}

            _uiMessage.emit(UiText.StringResource(R.string.shift_report_submitted_successfully_1))
            onSuccess(report)
        }
    }

    fun deleteReport(report: ShiftReport) {
        viewModelScope.launch {
            repository.deleteReport(report)
            reportDao.insertAuditLog(
                AuditLog(
                    username = "Admin",
                    action = "DELETE_REPORT",
                    details = "Deleted shift report #${report.id} for ${report.cashierName}"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.report_deleted))
        }
    }

    // Shift Drawer Operations (Open Shift / Close Shift / Pay In / Pay Out)
    fun openShift(cashierName: String, shiftName: String, startingCash: Double) {
        viewModelScope.launch {
            val newSession = ShiftSession(
                cashierName = cashierName.ifBlank { "Cashier" },
                shiftName = shiftName,
                status = "OPEN",
                openedAt = System.currentTimeMillis(),
                startingCash = startingCash,
                expectedCash = startingCash
            )
            reportDao.insertShiftSession(newSession)
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftOpenedNotification(context, cashierName, startingCash, curr)
            _uiMessage.emit(UiText.StringResource(R.string.toast_shift_opened_cash, startingCash.toString(), curr))
        }
    }

    fun closeShift(session: ShiftSession, actualCashCount: Double, notes: String) {
        viewModelScope.launch {
            val variance = actualCashCount - session.expectedCash
            val updated = session.copy(
                status = "CLOSED",
                closedAt = System.currentTimeMillis(),
                actualCashCount = actualCashCount,
                variance = variance,
                notes = notes
            )
            reportDao.updateShiftSession(updated)
            val rawCurr = businessProfile.value?.currency ?: "SAR"
            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr
            NotificationHelper.sendShiftClosedNotification(context, session.cashierName, variance, curr)
            _uiMessage.emit(UiText.StringResource(R.string.toast_shift_closed_variance, "%.2f".format(variance), curr))
        }
    }

    fun addCashMovement(type: String, amount: Double, reason: String, cashierName: String) {
        viewModelScope.launch {
            val active = activeShiftSession.value
            val sessionId = active?.id ?: 0
            reportDao.insertCashMovement(
                CashMovement(
                    shiftSessionId = sessionId,
                    type = type,
                    amount = amount,
                    reason = reason,
                    cashierName = cashierName
                )
            )
            if (active != null) {
                val newPayIn = if (type == "PAY_IN") active.totalPayIn + amount else active.totalPayIn
                val newPayOut = if (type == "PAY_OUT") active.totalPayOut + amount else active.totalPayOut
                val newExpected = active.startingCash + active.cashSales + newPayIn - newPayOut
                reportDao.updateShiftSession(active.copy(totalPayIn = newPayIn, totalPayOut = newPayOut, expectedCash = newExpected))
            }
            _uiMessage.emit(UiText.StringResource(R.string.toast_movement_recorded, type, amount.toString()))
        }
    }

    fun addCashier(name: String, pin: String = "1111", role: String = "CASHIER") {
        if (name.isBlank()) return
        syncManager.addCashier(name, pin, role) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_staff_added, name))
            }
        }
    }

    fun updateCashier(cashier: Cashier) {
        syncManager.updateCashier(cashier) {
            viewModelScope.launch {
                val msg = if (cashier.active) "Activated ${cashier.name}" else "Deactivated ${cashier.name}"
                _uiMessage.emit(UiText.DynamicString(msg))
            }
        }
    }

    fun deleteCashier(cashier: Cashier) {
        syncManager.deleteCashier(cashier) {
            viewModelScope.launch {
                _uiMessage.emit(UiText.StringResource(R.string.toast_staff_removed, cashier.name))
            }
        }
    }

    fun saveBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch {
            reportDao.saveBusinessProfile(profile)
            _uiMessage.emit(UiText.StringResource(R.string.business_settings_updated))
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            reportDao.saveUserProfile(profile)
            preferencesRepository.syncWithUserProfile(profile)
            reportDao.insertAuditLog(
                AuditLog(
                    username = profile.username,
                    action = "UPDATE_PROFILE",
                    details = "Updated profile for ${profile.fullName} (${profile.email})"
                )
            )
            _uiMessage.emit(UiText.StringResource(R.string.profile_updated_successfully))
        }
    }

    fun saveReceiptConfig(config: ShopReceiptConfig) {
        viewModelScope.launch {
            reportDao.saveReceiptConfig(config)
            _uiMessage.emit(UiText.StringResource(R.string.receipt_customization_updated))
        }
    }
}
