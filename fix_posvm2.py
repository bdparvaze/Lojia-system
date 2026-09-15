import re

path = 'app/src/main/java/com/example/ui/PosViewModel.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# fix _uiToast.emit
def repl(m):
    val = m.group(1)
    if 'UiText' in val:
        return m.group(0)
    return f'_uiToast.emit(UiText.DynamicString({val}))'

content = re.sub(r'_uiToast\.emit\(([^)]+)\)', repl, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

