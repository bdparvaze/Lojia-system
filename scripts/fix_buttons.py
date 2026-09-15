import os, glob

files = ["app/src/main/java/com/example/ui/SettingsShopSection.kt", "app/src/main/java/com/example/ui/ShiftReportScreen.kt", "app/src/main/java/com/example/ui/PosScreen.kt"]

for f in files:
    with open(f, "r", encoding="utf-8") as fp: content = fp.read()
    orig = content
    # Find Text(stringResource(...)) inside Button { ... } and add maxLines = 1
    # Actually just a simple string replace
    content = content.replace('Text(stringResource(R.string.menu_close_shift))', 'Text(stringResource(R.string.menu_close_shift), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.btn_clear))', 'Text(stringResource(R.string.btn_clear), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.confirm))', 'Text(stringResource(R.string.confirm), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.cancel))', 'Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.save))', 'Text(stringResource(R.string.save), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.add))', 'Text(stringResource(R.string.add), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    content = content.replace('Text(stringResource(R.string.checkout))', 'Text(stringResource(R.string.checkout), maxLines = 1, overflow = TextOverflow.Ellipsis)')
    
    if orig != content:
        if "TextOverflow" not in content:
            content = "import androidx.compose.ui.text.style.TextOverflow\n" + content
        with open(f, "w", encoding="utf-8") as fp: fp.write(content)
        print(f"Updated buttons in {f}")
