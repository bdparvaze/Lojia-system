with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('val currency = businessProfile?.currency ?: "SAR"', 'val rawCurrency = businessProfile?.currency ?: "SAR"\n        val currency = if (rawCurrency == "SAR") context.getString(R.string.currency_unit) else rawCurrency')

with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'w', encoding='utf-8') as f:
    f.write(content)
