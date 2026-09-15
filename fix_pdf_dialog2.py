with open('app/src/main/java/com/example/ui/PdfPreviewDialog.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('''                    Text(
                        if (exportedUri != null) stringResource(R.string.open_pdf)
                        else stringResource(R.string.export_pdf)
                    )''', '''                    Text(
                        text = if (exportedUri != null) stringResource(R.string.open_pdf)
                        else stringResource(R.string.export_pdf),
                        maxLines = 1,
                        softWrap = false
                    )''')

with open('app/src/main/java/com/example/ui/PdfPreviewDialog.kt', 'w', encoding='utf-8') as f:
    f.write(content)
