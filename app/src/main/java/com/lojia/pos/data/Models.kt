package com.lojia.pos.data

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.lojia.pos.R
import com.lojia.pos.auth.DevCredentials
import com.lojia.pos.util.SecurityUtils

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PASSWORD),
    val role: String = "ADMIN", // ADMIN, CASHIER
    val pin: String = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Demo Owner",
    val username: String = "demo",
    val email: String = "demo@lojia.local",
    @ColumnInfo(name = "passwordHash")
    val passwordHash: String = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PASSWORD),
    val securityQuestion: String = "What is your primary store location?",
    val securityAnswer: String = "demo",
    val phone: String = "",
    val designation: String = "Store Owner & Manager",
    val nationalIdOrPassport: String = "",
    val address: String = "Demo City",
    val profilePictureUri: String = "",
    val avatarIndex: Int = 0,
    val dateOfBirthOrJoin: String = "01 Jan 2024",
    val emergencyContact: String = "",
    val pin: String = SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN),
    val isBiometricEnabled: Boolean = true,
    val autoLockMinutes: Int = 5, // 0 = Never, 1, 5, 15, 30
    val currentRole: String = "ADMIN", // ADMIN, CASHIER
    val registeredAt: Long = System.currentTimeMillis(),
    val isRegistered: Boolean = false
)

@Entity(tableName = "shop_receipt_config")
data class ShopReceiptConfig(
    @PrimaryKey val id: Int = 1,
    val shopLogo: String = "store_logo_default",
    val customHeader: String = "Welcome to Lojia",
    val customFooterText: String = "Thank you, visit again!",
    val showTaxNumber: Boolean = true,
    val showCashierName: Boolean = true,
    val showBarcode: Boolean = true,
    val showCustomerMemo: Boolean = true
)

@Entity(tableName = "shift_sessions")
data class ShiftSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String = "Staff",
    val shiftName: String = "Morning", // Morning, Evening, Night
    val status: String = "OPEN", // OPEN, CLOSED
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val startingCash: Double = 500.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val digitalSales: Double = 0.0,
    val totalPayIn: Double = 0.0,
    val totalPayOut: Double = 0.0,
    val expectedCash: Double = 500.0,
    val actualCashCount: Double = 0.0,
    val variance: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "cash_movements")
data class CashMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shiftSessionId: Int = 0,
    val type: String, // PAY_IN, PAY_OUT
    val amount: Double,
    val reason: String,
    val cashierName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashiers")
data class Cashier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String = SecurityUtils.hashSecret("1111"),
    val role: String = "CASHIER", // ADMIN, CASHIER
    val active: Boolean = true
)

@Entity(tableName = "shift_reports")
data class ShiftReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cashierName: String,
    val shift: String = "Day", // "Day", "Night", "Morning", "Evening"
    val dateInMillis: Long = System.currentTimeMillis(),
    val grossCash: Double = 0.0,
    val madaPayments: Double = 0.0,
    val digitalWallet: Double = 0.0,
    val staffMealsCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = ""
) {
    val totalSales: Double
        get() = grossCash + madaPayments + digitalWallet

    val netCash: Double
        get() = grossCash - totalExpenses
}

@Entity(tableName = "draft_reports")
data class DraftReport(
    @PrimaryKey val id: Int = 1,
    val cashierName: String = "",
    val shift: String = "Day",
    val dateInMillis: Long = System.currentTimeMillis(),
    val grossCash: Double = 0.0,
    val madaPayments: Double = 0.0,
    val digitalWallet: Double = 0.0,
    val staffMealsCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val muasselQty: Double = 0.0,
    val outdoorShishaQty: Double = 0.0,
    val dueCreditEntriesJson: String = "[]",
    val previousDueCollectionsJson: String = "[]",
    val staffAdvancesJson: String = "[]",
    val unpaidBillsJson: String = "[]",
    val purchasedItemsJson: String = "[]",
    val notes: String = ""
)

data class DueCreditItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Double,
    val note: String = "",
    val phone: String = ""
)

data class PreviousDueCollectionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val customerName: String,
    val amount: Double,
    val paymentMode: String = "CASH", // CASH, CARD
    val note: String = ""
)

data class StaffAdvanceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val staffName: String,
    val amount: Double,
    val reason: String = ""
)

data class UnpaidBillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tableOrOrderRef: String,
    val amount: Double,
    val reason: String = ""
)

data class PurchasedInventoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val itemName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalAmount: Double = quantity * unitPrice,
    val paidVia: String = "CASH", // CASH, BANK
    val supplier: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val action: String,
    val details: String
)

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "Lojia",
    val vatNumber: String = "",
    val phone: String = "",
    val email: String = "contact@lojia.local",
    val address: String = "Demo City",
    val workingHours: String = "08:00 AM - 10:00 PM",
    val currency: String = "USD",
    val country: String = "United States",
    val vatRate: Double = 0.0,
    val isTaxEnabled: Boolean = false,
    val isTaxIncluded: Boolean = true,
    val logoUri: String = ""
) {
    @get:Ignore
    val taxEnabled: Boolean get() = isTaxEnabled
    @get:Ignore
    val taxRatePercent: Double get() = vatRate
    @get:Ignore
    val taxInclusive: Boolean get() = isTaxIncluded
}

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

// =========================================================================
// SHOP & POS MODULE ENTITIES
// =========================================================================

@Entity(tableName = "pos_categories")
data class POSCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String = "Category",
    val colorHex: String = "#6366F1"
)

@Entity(tableName = "pos_products")
data class POSProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryId: Int,
    val price: Double,
    val costPrice: Double = 0.0,
    val stockQuantity: Double = 100.0,
    val minStockAlert: Double = 10.0,
    val barcode: String = "",
    val sku: String = "",
    val unit: String = "pcs", // pcs, cup, kg, head
    val colorHex: String = "#10B981",
    val active: Boolean = true
)

@Entity(tableName = "pos_modifiers")
data class POSModifier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val optionGroup: String = "General",
    val extraPrice: Double = 0.0,
    val active: Boolean = true
)

@Entity(tableName = "pos_discounts")
data class POSDiscount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val percentage: Double = 0.0,
    val fixedAmount: Double = 0.0,
    val isPercentage: Boolean = true,
    val code: String = "",
    val active: Boolean = true
)

@Entity(tableName = "pos_sales")
data class POSSale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val cashierName: String,
    val customerName: String = "Walk-in Customer",
    val subtotal: Double,
    val vatAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String = "CASH", // CASH, CARD, DIGITAL_WALLET, SPLIT
    val timestamp: Long = System.currentTimeMillis(),
    val isVoided: Boolean = false,
    val voidReason: String = ""
)

@Entity(tableName = "pos_sale_items")
data class POSSaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalPrice: Double,
    val selectedModifiers: String = ""
)

@Entity(tableName = "pos_customers")
data class POSCustomer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val loyaltyPoints: Int = 0,
    val totalSpent: Double = 0.0
)

@Entity(tableName = "pos_employees")
data class POSEmployee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String = "CASHIER", // ADMIN, MANAGER, CASHIER
    val pin: String = SecurityUtils.hashSecret("1234"),
    val active: Boolean = true
)

@Entity(tableName = "pos_suppliers")
data class POSSupplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = ""
)

@Entity(tableName = "pos_stock_adjustments")
data class POSStockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val quantityChange: Double,
    val type: String, // RESTOCK, DAMAGE, AUDIT_CORRECTION, DEDUCTION
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val username: String = "Admin"
)

data class CartItem(
    val product: POSProduct,
    val quantity: Double = 1.0,
    val selectedModifiers: List<POSModifier> = emptyList(),
    val discount: Double = 0.0
) {
    val lineTotal: Double
        get() {
            val modExtra = selectedModifiers.sumOf { it.extraPrice }
            return ((product.price + modExtra) * quantity) - discount
        }
}

data class OpenTicket(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val items: List<CartItem>,
    val customerName: String = "Walk-in Customer",
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() = items.sumOf { it.lineTotal } * 1.15
}

enum class AppModule(val key: String, @StringRes val titleRes: Int) {
    SHOPPING("shopping", R.string.shopping_pos_module),
    SHIFT_REPORT("shift_report", R.string.shift_report_module)
}

enum class AppCountry(
    val code: String,
    val displayNameEn: String,
    val displayNameBn: String,
    val displayNameAr: String,
    val flag: String,
    val currencyCode: String,
    val currencySymbol: String,
    val defaultVatRate: Double = 15.0
) {
    BANGLADESH("BD", "Bangladesh", "বাংলাদেশ", "بنغلاديش", "🇧🇩", "BDT", "৳", 15.0),
    SAUDI_ARABIA("SA", "Saudi Arabia", "সৌদি আরব", "المملكة العربية السعودية", "🇸🇦", "SAR", "﷼", 15.0),
    UNITED_ARAB_EMIRATES("AE", "United Arab Emirates", "সংযুক্ত আরব আমিরাত", "الإمارات العربية المتحدة", "🇦🇪", "AED", "د.إ", 5.0),
    QATAR("QA", "Qatar", "কাতার", "قطر", "🇶🇦", "QAR", "ر.ق", 0.0),
    KUWAIT("KW", "Kuwait", "কুয়েত", "الكويت", "🇰🇼", "KWD", "د.ك", 0.0),
    OMAN("OM", "Oman", "ওমান", "سلطنة عمان", "🇴🇲", "OMR", "ر.ع", 5.0),
    BAHRAIN("BH", "Bahrain", "বাহরাইন", "مملكة البحرين", "🇧🇭", "BHD", "د.ب", 10.0),
    UNITED_STATES("US", "United States", "যুক্তরাষ্ট্র", "الولايات المتحدة", "🇺🇸", "USD", "$", 8.25),
    UNITED_KINGDOM("GB", "United Kingdom", "যুক্তরাজ্য", "المملكة المتحدة", "🇬🇧", "GBP", "£", 20.0),
    EUROPEAN_UNION("EU", "European Union", "ইউরোপীয় ইউনিয়ন", "الاتحاد الأوروبي", "🇪🇺", "EUR", "€", 19.0),
    INDIA("IN", "India", "ভারত", "الهند", "🇮🇳", "INR", "₹", 18.0),
    PAKISTAN("PK", "Pakistan", "পাকিস্তান", "باكستان", "🇵🇰", "PKR", "₨", 17.0),
    MALAYSIA("MY", "Malaysia", "মালয়েশিয়া", "ماليزيا", "🇲🇾", "MYR", "RM", 6.0),
    SINGAPORE("SG", "Singapore", "সিঙ্গাপুর", "سنغافورة", "🇸🇬", "SGD", "S$", 9.0),
    CANADA("CA", "Canada", "কানাডা", "كندا", "🇨🇦", "CAD", "C$", 13.0),
    AUSTRALIA("AU", "Australia", "অস্ট্রেলিয়া", "أستراليا", "🇦🇺", "AUD", "A$", 10.0),
    TURKEY("TR", "Turkey", "তুরস্ক", "تركيا", "🇹🇷", "TRY", "₺", 20.0),
    EGYPT("EG", "Egypt", "মিশর", "مصر", "🇪🇬", "EGP", "E£", 14.0),
    JAPAN("JP", "Japan", "জাপান", "اليابان", "🇯🇵", "JPY", "¥", 10.0),
    CHINA("CN", "China", "চীন", "الصين", "🇨🇳", "CNY", "¥", 13.0),
    INDONESIA("ID", "Indonesia", "ইন্দোনেশিয়া", "إندونيسيا", "🇮🇩", "IDR", "Rp", 11.0);

    fun getLocalizedName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.BENGALI -> displayNameBn
            AppLanguage.ARABIC -> displayNameAr
            else -> displayNameEn
        }
    }

    companion object {
        fun fromCode(code: String?): AppCountry {
            if (code.isNullOrBlank()) return BANGLADESH
            return entries.find { it.code.equals(code, ignoreCase = true) || it.currencyCode.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: BANGLADESH
        }
    }
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "English", "🇺🇸", isRtl = false),
    ARABIC("ar", "Arabic", "العربية", "🇸🇦", isRtl = true),
    BENGALI("bn", "Bengali", "বাংলা", "🇧🇩", isRtl = false),
    SPANISH("es", "Spanish", "Español", "🇪🇸", isRtl = false),
    FRENCH("fr", "French", "Français", "🇫🇷", isRtl = false),
    GERMAN("de", "German", "Deutsch", "🇩🇪", isRtl = false),
    HINDI("hi", "Hindi", "हिन्दी", "🇮🇳", isRtl = false),
    URDU("ur", "Urdu", "اردو", "🇵🇰", isRtl = true),
    CHINESE("zh", "Chinese", "简体中文", "🇨🇳", isRtl = false),
    TURKISH("tr", "Turkish", "Türkçe", "🇹🇷", isRtl = false),
    JAPANESE("ja", "Japanese", "日本語", "🇯🇵", isRtl = false),
    PORTUGUESE("pt", "Portuguese", "Português", "🇧🇷", isRtl = false),
    RUSSIAN("ru", "Russian", "Русский", "🇷🇺", isRtl = false),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", isRtl = false),
    ITALIAN("it", "Italian", "Italiano", "🇮🇹", isRtl = false),
    KOREAN("ko", "Korean", "한국어", "🇰🇷", isRtl = false),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", isRtl = false),
    TAGALOG("tl", "Tagalog / Filipino", "Filipino", "🇵🇭", isRtl = false),
    SWAHILI("sw", "Swahili", "Kiswahili", "🇰🇪", isRtl = false),
    PERSIAN("fa", "Persian", "فارسی", "🇮🇷", isRtl = true),
    THAI("th", "Thai", "ไทย", "🇹🇭", isRtl = false),
    DUTCH("nl", "Dutch", "Nederlands", "🇳🇱", isRtl = false),
    POLISH("pl", "Polish", "Polski", "🇵🇱", isRtl = false),
    MALAY("ms", "Malay", "Bahasa Melayu", "🇲🇾", isRtl = false),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳", isRtl = false),
    TAMIL("ta", "Tamil", "தமிழ்", "🇮🇳", isRtl = false),
    TELUGU("te", "Telugu", "తెలుగు", "🇮🇳", isRtl = false),
    GREEK("el", "Greek", "Ελληνικά", "🇬🇷", isRtl = false),
    HEBREW("he", "Hebrew", "עברית", "🇮🇱", isRtl = true),
    SWEDISH("sv", "Swedish", "Svenska", "🇸🇪", isRtl = false),
    NORWEGIAN("no", "Norwegian", "Norsk", "🇳🇴", isRtl = false),
    DANISH("da", "Danish", "Dansk", "🇩🇰", isRtl = false),
    FINNISH("fi", "Finnish", "Suomi", "🇫🇮", isRtl = false),
    ROMANIAN("ro", "Romanian", "Română", "🇷🇴", isRtl = false),
    HUNGARIAN("hu", "Hungarian", "Magyar", "🇭🇺", isRtl = false),
    CZECH("cs", "Czech", "Čeština", "🇨🇿", isRtl = false),
    UKRAINIAN("uk", "Ukrainian", "Українська", "🇺🇦", isRtl = false);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code.isNullOrBlank()) return ENGLISH
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
