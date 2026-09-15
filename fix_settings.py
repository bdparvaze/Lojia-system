import re

path = 'app/src/main/java/com/example/ui/SettingsScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure AppLanguageManager is imported
if 'import com.example.util.AppLanguageManager' not in content:
    content = content.replace('import com.example.util.ExportHelper', 'import com.example.util.ExportHelper\nimport com.example.util.AppLanguageManager')

# Find where language is changed
# reportViewModel.setLanguage(AppLanguage.BENGALI) -> AppLanguageManager.changeLanguage("bn")
# Let's replace the usages.
def repl(m):
    lang = m.group(1)
    if lang == 'ENGLISH': code = 'en'
    elif lang == 'BENGALI': code = 'bn'
    elif lang == 'ARABIC': code = 'ar'
    elif lang == 'SPANISH': code = 'es'
    elif lang == 'FRENCH': code = 'fr'
    elif lang == 'GERMAN': code = 'de'
    elif lang == 'HINDI': code = 'hi'
    elif lang == 'URDU': code = 'ur'
    elif lang == 'TURKISH': code = 'tr'
    elif lang == 'CHINESE': code = 'zh'
    else: code = 'en'
    return f'AppLanguageManager.changeLanguage("{code}")\nreportViewModel.setLanguage(AppLanguage.{lang})'

content = re.sub(r'reportViewModel\.setLanguage\(AppLanguage\.([A-Z_]+)\)', repl, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("SettingsScreen updated.")
