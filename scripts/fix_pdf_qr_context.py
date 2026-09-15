with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('''    private fun buildShiftReportQrText(
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        currency: String,
        dateFormatter: SimpleDateFormat
    ): String {''', '''    private fun buildShiftReportQrText(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        currency: String,
        dateFormatter: SimpleDateFormat
    ): String {''')

content = content.replace('val qrText = buildShiftReportQrText(report, businessProfile, currency, dateFormatter)', 'val qrText = buildShiftReportQrText(context, report, businessProfile, currency, dateFormatter)')

with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'w', encoding='utf-8') as f:
    f.write(content)
