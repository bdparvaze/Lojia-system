import os, glob

replacements = {
    '"Drawer Menu"': 'stringResource(R.string.title_drawer_menu)',
    '"Customer"': 'stringResource(R.string.title_customer)',
    '"Options"': 'stringResource(R.string.title_options)',
    '"Restricted Access"': 'stringResource(R.string.title_restricted_access)',
    '"Please enter your 6-digit administrator PIN to switch modules"': 'stringResource(R.string.msg_admin_pin_required)',
    '"Clear"': 'stringResource(R.string.btn_clear)',
    '"Items"': 'stringResource(R.string.menu_items)',
    '"Categories"': 'stringResource(R.string.menu_categories)',
    '"Modifiers"': 'stringResource(R.string.menu_modifiers)',
    '"Discounts"': 'stringResource(R.string.menu_discounts)',
    '"Printers"': 'stringResource(R.string.menu_printers)',
    '"Customer displays"': 'stringResource(R.string.menu_customer_displays)',
    '"CLOSE SHIFT"': 'stringResource(R.string.menu_close_shift)',
    '"DAILY SHIFT REPORT GENERATOR"': 'stringResource(R.string.menu_daily_shift_report)',
    '"Automated shift handover, cash reconciliation & PDF reports"': 'stringResource(R.string.desc_daily_shift_report)',
    '"Manage ${cashiers.size} cashier accounts, 6-digit PINs, and access privileges"': 'stringResource(R.string.desc_manage_cashiers, cashiers.size)',
    '"App language: ${language.displayName}"': 'stringResource(R.string.desc_app_language, language.displayName)',
    '"Products (${products.size}), Categories (${categories.size}), Modifiers & Discounts"': 'stringResource(R.string.desc_products_categories, products.size, categories.size)',
    '"Sales history, transactions and receipts log"': 'stringResource(R.string.desc_sales_history)',
    '"Receipt logo, custom footer, customer memos & live preview"': 'stringResource(R.string.desc_receipt_settings)',
    '"Receipt printers, customer displays, VAT & tax configuration"': 'stringResource(R.string.desc_hardware_settings)',
    '"Business & owner profile, address, contact information"': 'stringResource(R.string.desc_business_profile)',
    '"6-Digit security PIN, biometrics lock, auto-lock timeout"': 'stringResource(R.string.desc_security_settings)',
    '"Cloud back office hub, inventory sync, multi-store management"': 'stringResource(R.string.desc_cloud_back_office)',
    '"System version, software details, terms & privacy policy"': 'stringResource(R.string.desc_system_about)',
    '"Customer support & WhatsApp live chat"': 'stringResource(R.string.desc_customer_support)',
    '"Switch between POS Shop Module and Shift Report Module"': 'stringResource(R.string.desc_switch_module)'
}

for f in glob.glob("app/src/main/java/**/*.kt", recursive=True):
    with open(f, "r", encoding="utf-8") as fp: content = fp.read()
    orig = content
    for k, v in replacements.items():
        if k in content:
            content = content.replace(k, v)
    if orig != content:
        # add import if needed
        if "stringResource" in content and "import androidx.compose.ui.res.stringResource" not in content:
            content = "import androidx.compose.ui.res.stringResource\n" + content
        if "R.string" in content and "import com.example.R" not in content:
            content = "import com.example.R\n" + content
        with open(f, "w", encoding="utf-8") as fp: fp.write(content)
        print(f"Updated strings in {f}")

# Also handle PdfReportGenerator which isn't Compose
pdf_replacements = {
    '"Staff Meals"': 'pStr.staffMeals',
    '"Regular Muassel"': 'pStr.regularMuassel',
    '"Outdoor Muassel"': 'pStr.outdoorMuassel',
    '"Notes"': 'pStr.notes',
    '"Cashier Signature"': 'pStr.cashierSign',
    '"SHIFT CLOSING & REVENUE CERTIFICATE"': 'pStr.shiftClosingAndRevenueReport'
}
for f in glob.glob("app/src/main/java/com/example/util/PdfReportGenerator.kt", recursive=True):
    with open(f, "r", encoding="utf-8") as fp: content = fp.read()
    orig = content
    for k, v in pdf_replacements.items():
        if k in content: content = content.replace(k, v)
    
    # Also we need to add these fields to PdfStrings data class
    if "val staffMeals: String" not in content:
        content = content.replace("val notes: String = \"Notes\"", "val notes: String = \"Notes\",\n    val staffMeals: String = \"Staff Meals\",\n    val regularMuassel: String = \"Regular Muassel\",\n    val outdoorMuassel: String = \"Outdoor Muassel\",\n    val cashierSign: String = \"Cashier Signature\",\n    val shiftClosingAndRevenueReport: String = \"SHIFT CLOSING & REVENUE CERTIFICATE\"")
        
        # update the instances
        content = content.replace("AppLanguage.BENGALI -> PdfStrings(", "AppLanguage.BENGALI -> PdfStrings(\n                staffMeals = \"স্টাফ মিলস\",\n                regularMuassel = \"রেগুলার মুয়াসসেল\",\n                outdoorMuassel = \"আউটডোর মুয়াসসেল\",\n                notes = \"নোট\",\n                cashierSign = \"ক্যাশিয়ারের স্বাক্ষর\",\n                shiftClosingAndRevenueReport = \"শিফট ক্লোজিং এবং রেভিনিউ রিপোর্ট\",\n")
        content = content.replace("AppLanguage.ARABIC -> PdfStrings(", "AppLanguage.ARABIC -> PdfStrings(\n                staffMeals = \"وجبات الموظفين\",\n                regularMuassel = \"معسل عادي\",\n                outdoorMuassel = \"معسل خارجي\",\n                notes = \"ملاحظات\",\n                cashierSign = \"توقيع الكاشير\",\n                shiftClosingAndRevenueReport = \"تقرير إغلاق الوردية والإيرادات\",\n")

    if orig != content:
        with open(f, "w", encoding="utf-8") as fp: fp.write(content)
        print(f"Updated PdfReportGenerator.kt")
