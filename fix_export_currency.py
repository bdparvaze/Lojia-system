with open('app/src/main/java/com/example/util/ExportHelper.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('fun exportReportsAsCsv(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {',
'''fun exportReportsAsCsv(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {
        val actualCurrency = if (currency == "SAR") context.getString(R.string.currency_unit) else currency''')

content = content.replace('fun exportReportsAsPdf(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {',
'''fun exportReportsAsPdf(context: Context, reports: List<ShiftReport>, currency: String = "SAR") {
        val actualCurrency = if (currency == "SAR") context.getString(R.string.currency_unit) else currency''')

content = content.replace('fun exportSalesAsCsv(context: Context, sales: List<Sale>, currency: String = "SAR") {',
'''fun exportSalesAsCsv(context: Context, sales: List<Sale>, currency: String = "SAR") {
        val actualCurrency = if (currency == "SAR") context.getString(R.string.currency_unit) else currency''')

content = content.replace('fun exportSalesAsPdf(context: Context, sales: List<Sale>, currency: String = "SAR") {',
'''fun exportSalesAsPdf(context: Context, sales: List<Sale>, currency: String = "SAR") {
        val actualCurrency = if (currency == "SAR") context.getString(R.string.currency_unit) else currency''')

# In ExportHelper, replace `currency` with `actualCurrency` for the formatted strings
content = content.replace('${report.totalSales} $currency', '${report.totalSales} $actualCurrency')
content = content.replace('Total ($currency)', 'Total ($actualCurrency)')
content = content.replace('${sale.total} $currency', '${sale.total} $actualCurrency')

with open('app/src/main/java/com/example/util/ExportHelper.kt', 'w', encoding='utf-8') as f:
    f.write(content)
