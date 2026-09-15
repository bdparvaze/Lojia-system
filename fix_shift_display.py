import re

with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

replacement = """                                        val shiftDisplayName = when (report.shift.lowercase()) {
                                            "morning" -> stringResource(R.string.shift_morning)
                                            "evening" -> stringResource(R.string.shift_evening)
                                            "night" -> stringResource(R.string.shift_night)
                                            "day" -> stringResource(R.string.shift_day)
                                            else -> report.shift
                                        }
                                        Text(
                                            text = shiftDisplayName.uppercase(),"""

content = content.replace('''                                        Text(
                                            text = report.shift.uppercase(),''', replacement)

with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

