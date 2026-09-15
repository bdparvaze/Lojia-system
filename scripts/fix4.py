import re

def process(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    # Clean up the weird comments from previous manual fix
    content = content.replace('''        // =========================================================================
        // 2. SECURITY
        // =========================================================================
        // 1. PROFILE
        // =========================================================================''', '''        // =========================================================================
        // 1. PROFILE
        // =========================================================================''')
        
    content = content.replace('''        // =========================================================================
        ShopMenuCard(
            title = "Security"''', '''        // =========================================================================
        // 2. SECURITY
        // =========================================================================
        ShopMenuCard(
            title = "Security"''')

    # Remove the manual `if (expandedMenu == "profile") {` block wrapping.
    # It starts with `if (expandedMenu == "profile") {` and we can just remove that line and the corresponding closing brace.
    # Actually, it's easier to just match `if (expandedMenu == "profile") {` and remove it, then find the `}` and remove it.
    
    # Or I can just write a script that looks for `ShopMenuCard(` and if it is NOT preceded by `if (expandedMenu ==`, wrap it.
    
    # Let's just output the fixed content directly or use a state machine.
    lines = content.split('\n')
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Remove the `if (expandedMenu == "profile") {` line if present
        if line.strip() == 'if (expandedMenu == "profile") {':
            i += 1
            continue
            
        # We need to find `ShopMenuCard(` and wrap it.
        # But wait, there's a `}` we need to remove for the profile one.
        # Let's just restore the file from git... oh wait, no git.
        
        out.append(line)
        i += 1

    with open(filepath, 'w') as f:
        f.write("\n".join(out))

process("app/src/main/java/com/example/ui/SettingsShopSection.kt")
