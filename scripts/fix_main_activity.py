import re

path = 'app/src/main/java/com/example/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# insert language initialization
init_code = """        super.onCreate(savedInstanceState)
        
        // Initialize saved language
        val savedLang = com.example.util.LanguagePreferences.getLanguage(this)
        val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(savedLang)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)"""

content = content.replace('        super.onCreate(savedInstanceState)', init_code)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
