import re

for vm in ['app/src/main/java/com/example/ui/ReportViewModel.kt', 'app/src/main/java/com/example/ui/PosViewModel.kt']:
    with open(vm, 'r', encoding='utf-8') as f:
        content = f.read()

    # fix double wrapping
    content = content.replace('UiText.DynamicString(UiText.DynamicString(', 'UiText.DynamicString(')
    content = content.replace('UiText.DynamicString(UiText.StringResource(', 'UiText.StringResource(')
    
    # fix trailing parentheses
    content = re.sub(r'UiText\.DynamicString\("([^"]+)"\)\)\)', r'UiText.DynamicString("\1"))', content)
    content = re.sub(r'UiText\.DynamicString\("([^"]+)"\)\)', r'UiText.DynamicString("\1"))', content)
    # wait, the issue is: _uiMessage.emit(UiText.DynamicString("...")))
    # should be: _uiMessage.emit(UiText.DynamicString("..."))
    # Let's just fix `)))`
    content = content.replace(')))', '))')

    with open(vm, 'w', encoding='utf-8') as f:
        f.write(content)

