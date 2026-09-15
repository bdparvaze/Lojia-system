with open('app/src/main/java/com/example/ui/DashboardScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Text("${morningShifts.size} shifts", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)',
                          'Text(stringResource(R.string.shifts_count, morningShifts.size), style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)')
content = content.replace('Text("${eveningShifts.size} shifts", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)',
                          'Text(stringResource(R.string.shifts_count, eveningShifts.size), style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)')

with open('app/src/main/java/com/example/ui/DashboardScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
