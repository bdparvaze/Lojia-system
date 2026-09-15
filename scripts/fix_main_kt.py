import re

path = "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

if "import androidx.compose.ui.platform.LocalLayoutDirection" not in content:
    content = content.replace("import androidx.compose.animation.Crossfade", "import androidx.compose.animation.Crossfade\nimport androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.unit.LayoutDirection\nimport androidx.compose.runtime.CompositionLocalProvider\nimport com.example.data.AppLanguage")

# We want to wrap AppTheme(...) in CompositionLocalProvider
# Search for:
# AppTheme(darkTheme = isDarkMode) {
#     Crossfade...
replacement = """
            val layoutDirection = if (currentLanguage == AppLanguage.ARABIC || currentLanguage == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AppTheme(darkTheme = isDarkMode) {"""

if "CompositionLocalProvider(LocalLayoutDirection" not in content:
    content = content.replace("AppTheme(darkTheme = isDarkMode) {", replacement)
    # add closing brace before the end of setContent
    content = re.sub(r'(}\n\s*)(}\n\s*)$', r'\1    }\n\2', content)

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
