with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('staffName = obj.optString("staffName", obj.optString("name", "Staff"))', 'staffName = obj.optString("staffName", obj.optString("name", context.getString(R.string.staff_label)))')

with open('app/src/main/java/com/example/util/PdfReportGenerator.kt', 'w', encoding='utf-8') as f:
    f.write(content)
