with open('app/src/main/java/com/example/ui/ReportViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('val curr = businessProfile.value?.currency ?: "SAR"', 'val rawCurr = businessProfile.value?.currency ?: "SAR"\n            val curr = if (rawCurr == "SAR") context.getString(R.string.currency_unit) else rawCurr')

with open('app/src/main/java/com/example/ui/ReportViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)
