path = 'app/src/main/java/com/example/ui/PosViewModel.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'Stock adjusted for' in line:
        lines[i] = '            _uiToast.emit(UiText.DynamicString("Stock adjusted for $productName (${if (quantityChange >= 0) "+$quantityChange" else "$quantityChange"})"))\n'
        break

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
