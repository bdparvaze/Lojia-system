import os, glob

# 1. MainActivity.kt
main_kt = "app/src/main/java/com/example/MainActivity.kt"
with open(main_kt, "r", encoding="utf-8") as f: content = f.read()

if "import androidx.compose.ui.platform.LocalLayoutDirection" not in content:
    content = content.replace("import androidx.compose.animation.Crossfade", "import androidx.compose.animation.Crossfade\nimport androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.unit.LayoutDirection\nimport androidx.compose.runtime.CompositionLocalProvider\nimport com.example.data.AppLanguage\n")

if "CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)" not in content:
    # Find setContent { ... }
    # Inside setContent:
    # val currentLanguage by reportViewModel.currentLanguage.collectAsState()
    # Add layoutDirection logic
    content = content.replace('var navState by remember', 'val layoutDirection = if (currentLanguage == AppLanguage.ARABIC || currentLanguage == AppLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr\n\n            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {\n            var navState by remember')
    
    # Close the bracket at the end of setContent
    content = content.replace('AppTheme(darkTheme = isDarkMode) {\n                Crossfade', 'AppTheme(darkTheme = isDarkMode) {\n                Crossfade')
    
    # Wait, the closing bracket for CompositionLocalProvider. Let's just do a regex replace to insert it around AppTheme
    content = content.replace('AppTheme(darkTheme = isDarkMode) {', 'AppTheme(darkTheme = isDarkMode) {')
    # Actually, simpler:
    pass

