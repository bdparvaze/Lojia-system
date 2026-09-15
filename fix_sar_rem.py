with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('text = "${if (move.type == "PAY_IN") "+" else "-"}%.2f SAR".format(move.amount),',
                          'text = "${if (move.type == "PAY_IN") "+" else "-"}%.2f ${stringResource(R.string.currency_unit)}".format(move.amount),')

with open('app/src/main/java/com/example/ui/ShiftReportScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
