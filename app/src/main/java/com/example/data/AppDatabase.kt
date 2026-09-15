package com.example.data


import android.content.Context


import androidx.room.Database


import androidx.room.Room


import androidx.room.RoomDatabase


import androidx.sqlite.db.SupportSQLiteDatabase


import kotlinx.coroutines.CoroutineScope


import kotlinx.coroutines.Dispatchers


import kotlinx.coroutines.launch
import com.example.util.SecurityUtils

@Database(
    entities = [
        User::class,
        UserProfile::class,
        Cashier::class,
        ShiftReport::class,
        ShiftSession::class,
        CashMovement::class,
        DraftReport::class,
        AuditLog::class,
        BusinessProfile::class,
        ShopReceiptConfig::class,
        AppSetting::class,
        POSCategory::class,
        POSProduct::class,
        POSModifier::class,
        POSDiscount::class,
        POSSale::class,
        POSSaleItem::class,
        POSCustomer::class,
        POSEmployee::class,
        POSSupplier::class,
        POSStockAdjustment::class,
        TranslationCacheEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun posDao(): POSDao
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lojia_main.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getInstance(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val reportDao = db.reportDao()
            val posDao = db.posDao()

            // Default Business Profile
            reportDao.saveBusinessProfile(
                BusinessProfile(
                    id = 1,
                    businessName = "Lojia POS & Cafe",
                    vatNumber = "310123456700003",
                    phone = "+966 50 123 4567",
                    email = "contact@lojiasystem.com",
                    address = "Riyadh, Saudi Arabia",
                    workingHours = "08:00 AM - 02:00 AM",
                    currency = "SAR",
                    country = "Saudi Arabia",
                    vatRate = 15.0,
                    isTaxEnabled = true,
                    isTaxIncluded = true
                )
            )

            // Default Registered User Profile
            reportDao.saveUserProfile(
                UserProfile(
                    id = 1,
                    fullName = "Lojia Manager",
                    username = "admin",
                    email = "bdparvaze.backup@gmail.com",
                    password = SecurityUtils.hashSecret("admin123"),
                    phone = "+966 50 123 4567",
                    designation = "Store Owner & Manager",
                    nationalIdOrPassport = "NID-8827391823",
                    address = "Riyadh, Saudi Arabia",
                    avatarIndex = 0,
                    dateOfBirthOrJoin = "15 Jan 2024",
                    emergencyContact = "+966 55 987 6543",
                    pin = SecurityUtils.hashSecret("123456"),
                    isBiometricEnabled = true,
                    autoLockMinutes = 5,
                    currentRole = "ADMIN",
                    isRegistered = true
                )
            )

            // Default Receipt Config
            reportDao.saveReceiptConfig(
                ShopReceiptConfig(
                    id = 1,
                    shopLogo = "logo_lojia",
                    customHeader = "Lojia POS & Cafe",
                    customFooterText = "Thank you for visiting us! Visit again.",
                    showTaxNumber = true,
                    showCashierName = true,
                    showBarcode = true,
                    showCustomerMemo = true
                )
            )

            // Default Users & Cashiers
            reportDao.insertUser(User(username = "admin", passwordHash = SecurityUtils.hashSecret("admin123"), role = "ADMIN", pin = SecurityUtils.hashSecret("123456")))
            reportDao.insertUser(User(username = "cashier1", passwordHash = SecurityUtils.hashSecret("cashier123"), role = "CASHIER", pin = SecurityUtils.hashSecret("1111")))
            
            reportDao.insertCashier(Cashier(name = "Lojia Manager", pin = SecurityUtils.hashSecret("123456"), role = "ADMIN"))
            reportDao.insertCashier(Cashier(name = "Ahmed Al-Harbi", pin = SecurityUtils.hashSecret("1111"), role = "CASHIER"))
            reportDao.insertCashier(Cashier(name = "Fahad Al-Otaibi", pin = SecurityUtils.hashSecret("2222"), role = "CASHIER"))
            reportDao.insertCashier(Cashier(name = "Sultan Al-Ghamdi", pin = SecurityUtils.hashSecret("3333"), role = "CASHIER"))

            // Sample Categories
            val catHookah = posDao.insertCategory(POSCategory(name = "شيشة و رؤوس", iconName = "SmokeFree", colorHex = "#4CAF50"))
            val catDrinks = posDao.insertCategory(POSCategory(name = "مشروبات وقهوة", iconName = "Coffee", colorHex = "#6366F1"))
            val catFood = posDao.insertCategory(POSCategory(name = "مأكولات وخفايف", iconName = "Restaurant", colorHex = "#F59E0B"))

            // Products matching the exact Loyverse POS screenshot
            posDao.insertProduct(POSProduct(name = "تغيير راس بلوبيري", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "101", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس تفاحتين فاخر", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "102", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس علك مستكا", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "103", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس ليمون نعناع", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "104", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس مكس", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "105", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس عنب ساده", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "106", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس عنب توت", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "107", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس عنب نعناع", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "108", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس بطيخ نعناع", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "109", unit = "head"))
            posDao.insertProduct(POSProduct(name = "تغيير راس تفاحتين نخلة", categoryId = catHookah.toInt(), price = 25.0, costPrice = 5.0, stockQuantity = 500.0, minStockAlert = 20.0, barcode = "110", unit = "head"))

            posDao.insertProduct(POSProduct(name = "شاي تركي", categoryId = catDrinks.toInt(), price = 5.0, costPrice = 1.0, stockQuantity = 300.0, minStockAlert = 15.0, barcode = "201", unit = "cup"))
            posDao.insertProduct(POSProduct(name = "قهوة تركي", categoryId = catDrinks.toInt(), price = 10.0, costPrice = 2.0, stockQuantity = 200.0, minStockAlert = 15.0, barcode = "202", unit = "cup"))
            posDao.insertProduct(POSProduct(name = "اسبريسو دبل", categoryId = catDrinks.toInt(), price = 12.0, costPrice = 3.0, stockQuantity = 150.0, minStockAlert = 20.0, barcode = "203", unit = "cup"))
            posDao.insertProduct(POSProduct(name = "سبانش لاتيه", categoryId = catDrinks.toInt(), price = 18.0, costPrice = 5.0, stockQuantity = 120.0, minStockAlert = 15.0, barcode = "204", unit = "cup"))
            posDao.insertProduct(POSProduct(name = "ماء نقي بارد", categoryId = catDrinks.toInt(), price = 2.0, costPrice = 0.5, stockQuantity = 400.0, minStockAlert = 30.0, barcode = "301", unit = "bottle"))

            // Sample Modifiers
            posDao.insertModifier(POSModifier(name = "راس إضافي", optionGroup = "خيارات الشيشة", extraPrice = 10.0))
            posDao.insertModifier(POSModifier(name = "سيروب نكهة إضافية", optionGroup = "إضافات", extraPrice = 5.0))
            posDao.insertModifier(POSModifier(name = "حليب مضاعف", optionGroup = "إضافات قهوة", extraPrice = 4.0))

            // Sample Discounts
            posDao.insertDiscount(POSDiscount(name = "خصم موظفين 20%", percentage = 20.0, isPercentage = true, code = "STAFF20"))
            posDao.insertDiscount(POSDiscount(name = "عرض خاص 10%", percentage = 10.0, isPercentage = true, code = "PROMO10"))
            posDao.insertDiscount(POSDiscount(name = "خصم مباشر 5 ريال", fixedAmount = 5.0, isPercentage = false, code = "FLAT5"))

            // Sample Shift Sessions
            val activeSessionId = reportDao.insertShiftSession(
                ShiftSession(
                    cashierName = "Md. Parvaze",
                    shiftName = "Morning",
                    status = "OPEN",
                    openedAt = System.currentTimeMillis() - 14400000L,
                    startingCash = 1000.0,
                    cashSales = 4500.0,
                    cardSales = 6800.0,
                    digitalSales = 2100.0,
                    totalPayIn = 200.0,
                    totalPayOut = 150.0,
                    expectedCash = 5550.0,
                    actualCashCount = 0.0,
                    variance = 0.0,
                    notes = "Morning peak shift running smoothly"
                )
            )

            reportDao.insertCashMovement(
                CashMovement(
                    shiftSessionId = activeSessionId.toInt(),
                    type = "PAY_IN",
                    amount = 200.0,
                    reason = "Added small change coins to drawer",
                    cashierName = "Md. Parvaze"
                )
            )

            reportDao.insertCashMovement(
                CashMovement(
                    shiftSessionId = activeSessionId.toInt(),
                    type = "PAY_OUT",
                    amount = 150.0,
                    reason = "Cleaning supplies cash purchase",
                    cashierName = "Md. Parvaze"
                )
            )

            // Sample Shift Reports
            val now = System.currentTimeMillis()
            val oneDay = 86400000L
            reportDao.insertShiftReport(ShiftReport(cashierName = "Ahmed Al-Harbi", shift = "Morning", dateInMillis = now - (3 * oneDay), grossCash = 14500.0, madaPayments = 32000.0, digitalWallet = 8500.0, staffMealsCount = 4, totalExpenses = 1800.0, muasselQty = 0.0, notes = "Smooth morning shift"))
            reportDao.insertShiftReport(ShiftReport(cashierName = "Fahad Al-Otaibi", shift = "Evening", dateInMillis = now - (3 * oneDay), grossCash = 21000.0, madaPayments = 51000.0, digitalWallet = 13500.0, staffMealsCount = 6, totalExpenses = 3200.0, muasselQty = 0.0, notes = "High footfall evening"))
            reportDao.insertShiftReport(ShiftReport(cashierName = "Sultan Al-Ghamdi", shift = "Morning", dateInMillis = now - (2 * oneDay), grossCash = 16800.0, madaPayments = 34500.0, digitalWallet = 9200.0, staffMealsCount = 3, totalExpenses = 1500.0, muasselQty = 0.0, notes = "Morning rush handled"))
            reportDao.insertShiftReport(ShiftReport(cashierName = "Md. Parvaze", shift = "Evening", dateInMillis = now - (1 * oneDay), grossCash = 27500.0, madaPayments = 64000.0, digitalWallet = 18500.0, staffMealsCount = 8, totalExpenses = 4500.0, muasselQty = 0.0, notes = "Superb sales volume"))

            // Sample Audit Log
            reportDao.insertAuditLog(AuditLog(username = "System", action = "INITIALIZATION", details = "Lojia System database initialized with dual Shop & Shift Report modules"))
        }
    }
}
