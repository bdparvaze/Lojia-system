import re

for path in ['app/src/main/java/com/example/ui/SettingsReportSection.kt', 'app/src/main/java/com/example/ui/SettingsShopSection.kt']:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('AppLanguageManager.changeLanguage(appLang.code)', 'AppLanguageManager.changeLanguage(context, appLang.code)')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
