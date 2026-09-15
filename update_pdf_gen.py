import re

path = "app/src/main/java/com/example/util/PdfReportGenerator.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Update getPdfStrings signature and implementation
new_get_pdf_strings = """
    private fun getPdfStrings(context: Context, language: AppLanguage): PdfStrings {
        val locale = java.util.Locale(language.code)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        return PdfStrings(
            title = localizedContext.getString(com.example.R.string.shift_closing_and_revenue_report),
            officialReport = localizedContext.getString(com.example.R.string.pdf_official_report),
            reportId = localizedContext.getString(com.example.R.string.pdf_report_id),
            cashier = localizedContext.getString(com.example.R.string.pdf_cashier),
            shift = localizedContext.getString(com.example.R.string.pdf_shift),
            date = localizedContext.getString(com.example.R.string.pdf_date),
            financialBreakdown = localizedContext.getString(com.example.R.string.pdf_financial_breakdown),
            grossCash = localizedContext.getString(com.example.R.string.pdf_gross_cash),
            madaBank = localizedContext.getString(com.example.R.string.pdf_mada_bank),
            digitalWallet = localizedContext.getString(com.example.R.string.pdf_digital_wallet),
            grossTotalSales = localizedContext.getString(com.example.R.string.pdf_gross_total_sales),
            expenses = localizedContext.getString(com.example.R.string.pdf_expenses),
            netCashInDrawer = localizedContext.getString(com.example.R.string.pdf_net_cash_in_drawer),
            netMadaBank = localizedContext.getString(com.example.R.string.pdf_net_mada_bank),
            dueSales = localizedContext.getString(com.example.R.string.pdf_due_sales),
            dueCollection = localizedContext.getString(com.example.R.string.pdf_due_collection),
            employerAdvances = localizedContext.getString(com.example.R.string.pdf_employer_advances),
            walkoutBills = localizedContext.getString(com.example.R.string.pdf_walkout_bills),
            paidOutItems = localizedContext.getString(com.example.R.string.pdf_paid_out_items),
            operationalMetrics = localizedContext.getString(com.example.R.string.pdf_operational_metrics),
            staffMeals = localizedContext.getString(com.example.R.string.pdf_staff_meals),
            regularMuassel = localizedContext.getString(com.example.R.string.pdf_regular_muassel),
            outdoorMuassel = localizedContext.getString(com.example.R.string.pdf_outdoor_muassel),
            receiptNo = localizedContext.getString(com.example.R.string.pdf_receipt_no),
            customer = localizedContext.getString(com.example.R.string.pdf_customer),
            amount = localizedContext.getString(com.example.R.string.pdf_amount),
            mode = localizedContext.getString(com.example.R.string.pdf_mode),
            staffName = localizedContext.getString(com.example.R.string.pdf_staff_name),
            item = localizedContext.getString(com.example.R.string.pdf_item),
            qty = localizedContext.getString(com.example.R.string.pdf_qty),
            total = localizedContext.getString(com.example.R.string.pdf_total),
            notes = localizedContext.getString(com.example.R.string.pdf_notes_1),
            cashierSign = localizedContext.getString(com.example.R.string.pdf_cashier_sign_1),
            supervisorSign = localizedContext.getString(com.example.R.string.pdf_supervisor_sign),
            page = localizedContext.getString(com.example.R.string.pdf_page)
        )
    }
"""

# Find the getPdfStrings method and replace it
# It starts with "private fun getPdfStrings(language: AppLanguage): PdfStrings {"
# and ends when "private fun generateStyledQrCodeBitmap" starts.
old_method_pattern = re.compile(r'private fun getPdfStrings\(language: AppLanguage\): PdfStrings \{.*?\}\s*(?=private fun generateStyledQrCodeBitmap)', re.DOTALL)
content = old_method_pattern.sub(new_get_pdf_strings, content)

# Now update the callers of getPdfStrings
# buildShiftReportQrText
content = content.replace("val pStr = getPdfStrings(language)", "val pStr = getPdfStrings(context, language)")
# generateSingleShiftReportPdf
content = content.replace("val pStr = getPdfStrings(language)", "val pStr = getPdfStrings(context, language)")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

