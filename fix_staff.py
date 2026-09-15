with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('val name = obj.optString("staffName").ifBlank { obj.optString("name", "Staff") }', 'val name = obj.optString("staffName").ifBlank { obj.optString("name", "") }')
content = content.replace('Text(item.staffName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)', 'Text(item.staffName.ifEmpty { stringResource(R.string.staff_label) }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ShiftColors.Charcoal)')

with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
