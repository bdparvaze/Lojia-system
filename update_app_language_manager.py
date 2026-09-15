import re

path = 'app/src/main/java/com/example/util/AppLanguageManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('fun changeLanguage(languageCode: String)', 'fun changeLanguage(context: android.content.Context, languageCode: String)')
content = content.replace('AppCompatDelegate.setApplicationLocales(appLocale)', 'AppCompatDelegate.setApplicationLocales(appLocale)\n        LanguagePreferences.saveLanguage(context, languageCode)')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
