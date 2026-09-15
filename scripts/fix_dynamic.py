import os

# fix LocaleManager.kt
path = 'app/src/main/java/com/example/util/LocaleManager.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace('import com.example.ui.LocalAppLanguage\n', '')
content = content.replace('object LocaleManager {', 'val LocalAppLanguage = androidx.compose.runtime.compositionLocalOf { AppLanguage.ENGLISH }\n\nobject LocaleManager {')

with open(path, 'w') as f:
    f.write(content)

# fix DynamicText.kt
path = 'app/src/main/java/com/example/ui/DynamicText.kt'
with open(path, 'r') as f:
    content = f.read()

content = content.replace('import com.example.util.TranslationEngine', 'import com.example.util.TranslationEngine\nimport com.example.util.LocalAppLanguage')

# DynamicText has a bug on line 29: actual type is 'kotlin.Int', but 'kotlin.String' was expected.
# Let's see... 'text' might be passed as an Int if we refactored it to use stringResource(R.string...). Wait.
# Oh, we changed DynamicText(text = stringResource("key")) to DynamicText(text = stringResource(R.string.key))
# But stringResource(R.string.key) returns a String!
# Wait, maybe someone changed DynamicText("something") to DynamicText(R.string.something) ?
# Let's search DynamicText usages.

with open(path, 'w') as f:
    f.write(content)
