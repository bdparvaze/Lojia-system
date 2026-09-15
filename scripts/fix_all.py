import re

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    # Remove the LazyRow / FilterChip section completely
    # It starts with "// Quick Jump / Filter Tabs" and ends just before "// 1. PROFILE"
    content = re.sub(r'// Quick Jump / Filter Tabs.*?// =========================================================================\s*// 1. PROFILE', '// =========================================================================\n        // 1. PROFILE', content, flags=re.DOTALL)

    # For each section, wrap the ShopMenuCard in an if block.
    # Pattern: ShopMenuCard( ... isExpanded = expandedMenu == "key" ... ) { ... }
    
    def replacer(match):
        full_match = match.group(0)
        # find the key
        key_match = re.search(r'isExpanded\s*=\s*expandedMenu\s*==\s*"([^"]+)"', full_match)
        if not key_match:
            return full_match
        key = key_match.group(1)
        
        # We will wrap it in: if (expandedMenu == "key") { ... }
        # And we can force isExpanded = true and onClick = {} if we want, but it's fine to leave them. 
        # Actually, let's replace `isExpanded = expandedMenu == "key"` with `isExpanded = true`
        # and `onClick = { ... }` with `onClick = {}`
        
        replaced = re.sub(r'isExpanded\s*=\s*expandedMenu\s*==\s*"[^"]+"', 'isExpanded = true', full_match)
        replaced = re.sub(r'onClick\s*=\s*\{[^}]*\}', 'onClick = {}', replaced)
        
        # Add the wrapper
        lines = replaced.split('\n')
        indented = ['            ' + line for line in lines]
        wrapped = f'        if (expandedMenu == "{key}") {{\n' + '\n'.join(indented) + '\n        }'
        return wrapped

    # Regex to match a whole ShopMenuCard call.
    # It starts with ShopMenuCard( and ends with a } that is indented by 8 spaces.
    content = re.sub(r'ShopMenuCard\([^)]+\)\s*\{.*?\n        \}', replacer, content, flags=re.DOTALL)

    with open(filepath, "w") as f:
        f.write(content)

process_file("app/src/main/java/com/example/ui/SettingsShopSection.kt")
process_file("app/src/main/java/com/example/ui/SettingsReportSection.kt")
