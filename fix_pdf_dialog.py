with open('app/src/main/java/com/example/ui/PdfPreviewDialog.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Text(stringResource(R.string.share))', 'Text(text = stringResource(R.string.share), maxLines = 1, softWrap = false)')
content = content.replace('Text(stringResource(R.string.close))', 'Text(text = stringResource(R.string.close), maxLines = 1, softWrap = false)')

with open('app/src/main/java/com/example/ui/PdfPreviewDialog.kt', 'w', encoding='utf-8') as f:
    f.write(content)
