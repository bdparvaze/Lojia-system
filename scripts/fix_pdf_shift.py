with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'r', encoding='utf-8') as f:
    content = f.read()

helper = """
    private fun getLocalizedShiftName(context: Context, shift: String): String {
        return when (shift.lowercase()) {
            "morning" -> context.getString(R.string.shift_morning)
            "evening" -> context.getString(R.string.shift_evening)
            "night" -> context.getString(R.string.shift_night)
            "day" -> context.getString(R.string.shift_day)
            else -> shift
        }
    }
"""
# insert before object PdfReportGenerator
content = content.replace('object PdfReportGenerator {', 'object PdfReportGenerator {' + helper)

content = content.replace('appendLine("Shift: ${report.shift} | Cashier: ${report.cashierName}")', 'appendLine("Shift: ${getLocalizedShiftName(context, report.shift)} | Cashier: ${report.cashierName}")')
content = content.replace('canvas.drawText(report.shift, colShift, rowY, textPaint)', 'canvas.drawText(getLocalizedShiftName(context, report.shift), colShift, rowY, textPaint)')
content = content.replace('canvas.drawText("${pStr.shift}: ${report.shift}", margin + 14f, currentY + 66f, textBoldPaint)', 'canvas.drawText("${pStr.shift}: ${getLocalizedShiftName(context, report.shift)}", margin + 14f, currentY + 66f, textBoldPaint)')

with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'w', encoding='utf-8') as f:
    f.write(content)
