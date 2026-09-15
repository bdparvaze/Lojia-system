import re

for vm in ['app/src/main/java/com/example/ui/ReportViewModel.kt', 'app/src/main/java/com/example/ui/PosViewModel.kt']:
    with open(vm, 'r', encoding='utf-8') as f:
        content = f.read()

    # Add imports
    if 'import com.example.util.UiText' not in content:
        content = content.replace('import androidx.lifecycle.ViewModel', 'import androidx.lifecycle.ViewModel\nimport com.example.util.UiText\nimport com.example.R')
        content = content.replace('import androidx.lifecycle.AndroidViewModel', 'import androidx.lifecycle.AndroidViewModel\nimport com.example.util.UiText\nimport com.example.R')

    # Fix PosViewModel String -> UiText issues
    content = content.replace('_uiMessage.emit("', '_uiMessage.emit(UiText.DynamicString("')
    content = content.replace('"))\n', '")))\n')
    # some might be double-closed, let's fix carefully
    def repl(m):
        return f'_uiMessage.emit(UiText.DynamicString("{m.group(1)}"))'
    content = re.sub(r'_uiMessage\.emit\(\s*"([^"]+)"\s*\)', repl, content)

    with open(vm, 'w', encoding='utf-8') as f:
        f.write(content)

print("Imports and PosViewModel fixed.")
