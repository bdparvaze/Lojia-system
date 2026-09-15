import re

def process(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    # We will split by "// ========================================================================="
    parts = content.split("// =========================================================================")
    
    for i in range(len(parts)):
        part = parts[i]
        if "ShopMenuCard" in part and "isExpanded = expandedMenu ==" in part:
            # check if it's already wrapped in `if (expandedMenu == `
            if "if (expandedMenu ==" not in part:
                # Find the key
                m = re.search(r'isExpanded\s*=\s*expandedMenu\s*==\s*"([^"]+)"', part)
                if m:
                    key = m.group(1)
                    
                    # Indent the part
                    lines = part.strip('\n').split('\n')
                    indented_lines = ['        ' + line if line.strip() else line for line in lines]
                    
                    # Reconstruct
                    new_part = f'\n        if (expandedMenu == "{key}") {{\n' + '\n'.join(indented_lines) + '\n        }\n'
                    parts[i] = new_part

    new_content = "// =========================================================================".join(parts)
    
    with open(filepath, 'w') as f:
        f.write(new_content)

process("app/src/main/java/com/example/ui/SettingsShopSection.kt")
process("app/src/main/java/com/example/ui/SettingsReportSection.kt")
