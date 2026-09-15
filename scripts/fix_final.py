import re

def process(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Find the start of the Column block
    # It has `verticalArrangement = Arrangement.spacedBy(14.dp)`
    # followed by `    ) {`
    
    parts = content.split('verticalArrangement = Arrangement.spacedBy(14.dp)\n    ) {\n')
    if len(parts) < 2:
        return
        
    top_part = parts[0]
    rest = parts[1]
    
    # We will wrap the rest inside `        when (expandedMenu) {\n`
    
    # Split rest by `// =========================================================================`
    sections = rest.split('// =========================================================================')
    
    out_sections = []
    
    for section in sections:
        if "ShopMenuCard" in section:
            # find the key
            m = re.search(r'isExpanded\s*=\s*(?:expandedMenu\s*==\s*"([^"]+)"|true)', section)
            key = m.group(1) if m and m.group(1) else None
            if not key:
                if 'title = "Profile"' in section:
                    key = "profile"
                elif 'title = "Security"' in section:
                    key = "security"
                # ...
                
            if key:
                # Replace isExpanded and onClick
                section = re.sub(r'isExpanded\s*=\s*expandedMenu\s*==\s*"[^"]+"', 'isExpanded = true', section)
                
                # We need to wrap it in `            "key" -> {\n`
                # and end it with `            }\n`
                lines = section.strip('\n').split('\n')
                indented = ['    ' + line if line.strip() else line for line in lines]
                
                new_section = f'\n            "{key}" -> {{\n' + '\n'.join(indented) + '\n            }\n'
                out_sections.append(new_section)
            else:
                out_sections.append(section)
        else:
            # Maybe the end brace or something else
            out_sections.append(section)
            
    # Combine back
    new_rest = '        when (expandedMenu) {\n' + ''.join(out_sections) + '        }\n'
    
    with open(filepath, 'w') as f:
        f.write(top_part + 'verticalArrangement = Arrangement.spacedBy(14.dp)\n    ) {\n' + new_rest)

process("app/src/main/java/com/example/ui/SettingsShopSection.kt")
process("app/src/main/java/com/example/ui/SettingsReportSection.kt")
