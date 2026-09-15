import re

path = 'app/src/main/java/com/example/ui/ReportViewModel.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('_uiMessage.emit(msg)', '_uiMessage.emit(UiText.DynamicString(msg))')
content = content.replace('_uiMessage.emit("Auto-filled', '_uiMessage.emit(UiText.DynamicString("Auto-filled')
content = content.replace('cardTotal))', 'cardTotal)))')
content = content.replace('_uiMessage.emit("Shift closed. Variance:', '_uiMessage.emit(UiText.DynamicString("Shift closed. Variance:')
content = content.replace('curr))', 'curr)))')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
