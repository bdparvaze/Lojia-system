package com.lojia.pos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lojia.pos.BuildConfig
import com.lojia.pos.auth.DevCredentials
import com.lojia.pos.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShiftReport::class,
        DraftReport::class,
        Cashier::class,
        User::class,
        UserProfile::class,
        AuditLog::class,
        BusinessProfile::class,
        AppSetting::class,
        ShiftSession::class,
        CashMovement::class,
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
        TranslationCacheEntity::class,
        ShopReceiptConfig::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun posDao(): POSDao
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lojia_system_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                builder.addCallback(DatabaseCallback(scope))
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: getDatabase(context, CoroutineScope(Dispatchers.IO + SupervisorJob()))
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.reportDao(), database.posDao())
                }
            }
        }

        suspend fun populateInitialData(reportDao: ReportDao, posDao: POSDao) {
            if (BuildConfig.DEBUG) {
                reportDao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        fullName = "Demo Owner",
                        username = DevCredentials.DEFAULT_USERNAME,
                        email = "demo@lojia.local",
                        passwordHash = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PASSWORD),
                        securityQuestion = "What is your primary store location?",
                        securityAnswer = "demo",
                        phone = "",
                        designation = "Store Owner & Manager",
                        nationalIdOrPassport = "",
                        address = "Demo City",
                        profilePictureUri = "",
                        avatarIndex = 0,
                        dateOfBirthOrJoin = "01 Jan 2024",
                        emergencyContact = "",
                        pin = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN),
                        isBiometricEnabled = true,
                        autoLockMinutes = 5,
                        currentRole = "ADMIN",
                        registeredAt = System.currentTimeMillis(),
                        isRegistered = true
                    )
                )

                reportDao.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = "Lojia",
                        vatNumber = "",
                        phone = "",
                        email = "contact@lojia.local",
                        address = "Demo City",
                        workingHours = "08:00 AM - 10:00 PM",
                        currency = "USD",
                        country = "United States",
                        vatRate = 0.0,
                        isTaxEnabled = false,
                        isTaxIncluded = true,
                        logoUri = ""
                    )
                )

                reportDao.saveReceiptConfig(
                    ShopReceiptConfig(
                        id = 1,
                        shopLogo = "store_logo_default",
                        customHeader = "Welcome to Lojia",
                        customFooterText = "Thank you, visit again!",
                        showTaxNumber = true,
                        showCashierName = true,
                        showBarcode = true,
                        showCustomerMemo = true
                    )
                )

                reportDao.insertUser(
                    User(
                        username = DevCredentials.DEFAULT_USERNAME,
                        passwordHash = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PASSWORD),
                        role = "ADMIN",
                        pin = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN)
                    )
                )

                reportDao.insertCashier(Cashier(name = "Lojia Manager", pin = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN), role = "ADMIN"))
                reportDao.insertCashier(Cashier(name = "Ahmed Al-Harbi", pin = SecurityUtils.hashSecret("1111"), role = "CASHIER"))
                reportDao.insertCashier(Cashier(name = "Fahad Al-Otaibi", pin = SecurityUtils.hashSecret("2222"), role = "CASHIER"))
                reportDao.insertCashier(Cashier(name = "Sultan Al-Ghamdi", pin = SecurityUtils.hashSecret("3333"), role = "CASHIER"))

                val catHookah = posDao.insertCategory(POSCategory(name = "شيشة و رؤوس", iconName = "SmokeFree", colorHex = "#4CAF50"))
                val catDrinks = posDao.insertCategory(POSCategory(name = "مشروبات وقهوة", iconName = "Coffee", colorHex = "#6366F1"))
                val catFood = posDao.insertCategory(POSCategory(name = "مأكولات وخفايف", iconName = "Restaurant", colorHex = "#F59E0B"))

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

                posDao.insertModifier(POSModifier(name = "راس إضافي", optionGroup = "خيارات الشيشة", extraPrice = 10.0))
                posDao.insertModifier(POSModifier(name = "سيروب نكهة إضافية", optionGroup = "إضافات", extraPrice = 5.0))
                posDao.insertModifier(POSModifier(name = "حليب مضاعف", optionGroup = "إضافات قهوة", extraPrice = 4.0))

                posDao.insertDiscount(POSDiscount(name = "خصم موظفين 20%", percentage = 20.0, isPercentage = true, code = "STAFF20"))
                posDao.insertDiscount(POSDiscount(name = "عرض خاص 10%", percentage = 10.0, isPercentage = true, code = "PROMO10"))
                posDao.insertDiscount(POSDiscount(name = "خصم مباشر 5 ريال", fixedAmount = 5.0, isPercentage = false, code = "FLAT5"))

                val activeSessionId = reportDao.insertShiftSession(
                    ShiftSession(
                        cashierName = "Demo Staff",
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
                        notes = "Morning shift running smoothly"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_IN",
                        amount = 200.0,
                        reason = "Added small change coins to drawer",
                        cashierName = "Demo Staff"
                    )
                )

                reportDao.insertCashMovement(
                    CashMovement(
                        shiftSessionId = activeSessionId.toInt(),
                        type = "PAY_OUT",
                        amount = 150.0,
                        reason = "Cleaning supplies cash purchase",
                        cashierName = "Demo Staff"
                    )
                )

                val now = System.currentTimeMillis()
                val oneDay = 86400000L
                reportDao.insertShiftReport(ShiftReport(cashierName = "Ahmed Al-Harbi", shift = "Morning", dateInMillis = now - (3 * oneDay), grossCash = 14500.0, madaPayments = 32000.0, digitalWallet = 8500.0, staffMealsCount = 4, totalExpenses = 1800.0, muasselQty = 0.0, notes = "Smooth morning shift"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Fahad Al-Otaibi", shift = "Evening", dateInMillis = now - (3 * oneDay), grossCash = 21000.0, madaPayments = 51000.0, digitalWallet = 13500.0, staffMealsCount = 6, totalExpenses = 3200.0, muasselQty = 0.0, notes = "High footfall evening"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Sultan Al-Ghamdi", shift = "Morning", dateInMillis = now - (2 * oneDay), grossCash = 16800.0, madaPayments = 34500.0, digitalWallet = 9200.0, staffMealsCount = 3, totalExpenses = 1500.0, muasselQty = 0.0, notes = "Morning rush handled"))
                reportDao.insertShiftReport(ShiftReport(cashierName = "Demo Staff", shift = "Evening", dateInMillis = now - (1 * oneDay), grossCash = 27500.0, madaPayments = 64000.0, digitalWallet = 18500.0, staffMealsCount = 8, totalExpenses = 4500.0, muasselQty = 0.0, notes = "Superb sales volume"))

                reportDao.insertAuditLog(AuditLog(username = "System", action = "INITIALIZATION", details = "Lojia database initialized with dual Shop & Shift Report modules"))
            } else {
                reportDao.saveUserProfile(
                    UserProfile(
                        id = 1,
                        fullName = "",
                        username = "",
                        email = "",
                        passwordHash = "",
                        securityQuestion = "",
                        securityAnswer = "",
                        phone = "",
                        designation = "",
                        nationalIdOrPassport = "",
                        address = "",
                        profilePictureUri = "",
                        avatarIndex = 0,
                        dateOfBirthOrJoin = "",
                        emergencyContact = "",
                        pin = "",
                        isBiometricEnabled = false,
                        autoLockMinutes = 5,
                        currentRole = "ADMIN",
                        registeredAt = System.currentTimeMillis(),
                        isRegistered = false
                    )
                )
                reportDao.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = "",
                        vatNumber = "",
                        phone = "",
                        email = "",
                        address = "",
                        currency = "SAR",
                        country = "Saudi Arabia",
                        vatRate = 15.0,
                        isTaxEnabled = true,
                        isTaxIncluded = true
                    )
                )
                reportDao.saveReceiptConfig(
                    ShopReceiptConfig(
                        id = 1,
                        shopLogo = "store_logo_default",
                        customHeader = "Welcome to Lojia",
                        customFooterText = "Thank you, visit again!",
                        showTaxNumber = true,
                        showCashierName = true,
                        showBarcode = true,
                        showCustomerMemo = true
                    )
                )
            }
        }
    }
}
