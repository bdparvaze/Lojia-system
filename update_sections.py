import re

for path in ['app/src/main/java/com/example/ui/SettingsReportSection.kt', 'app/src/main/java/com/example/ui/SettingsShopSection.kt']:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Make sure AppLanguageManager is imported
    if 'import com.example.util.AppLanguageManager' not in content:
        content = 'import com.example.util.AppLanguageManager\n' + content

    # In those files, the logic is: reportViewModel.setLanguage(appLang)
    # We should add AppLanguageManager.changeLanguage(appLang.code) before it.
    
    content = content.replace(
        'reportViewModel.setLanguage(appLang)',
        'AppLanguageManager.changeLanguage(appLang.code)\n                                reportViewModel.setLanguage(appLang)'
    )
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Sections updated.")
