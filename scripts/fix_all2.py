import re

def fix_cards(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    out = []
    i = 0
    in_card = False
    card_key = ""
    brace_count = 0

    while i < len(lines):
        line = lines[i]
        
        # Clean up the messed up Profile/Security comments
        if "// =========================================================================" in line and i + 2 < len(lines) and "// 2. SECURITY" in lines[i+1] and "// =========================================================================" in lines[i+2]:
            if i + 4 < len(lines) and "// 1. PROFILE" in lines[i+3]:
                # Skip the duplicate comments
                i += 3
                continue
                
        if not in_card:
            if "ShopMenuCard(" in line and not "if (expandedMenu ==" in line:
                # Find the key
                # Look ahead for isExpanded
                j = i
                key = ""
                while j < len(lines):
                    m = re.search(r'isExpanded\s*=\s*expandedMenu\s*==\s*"([^"]+)"', lines[j])
                    if m:
                        key = m.group(1)
                        break
                    if "{" in lines[j] and j != i: 
                        pass # keep searching a bit if needed
                    j += 1
                    if j - i > 15: break
                
                if key:
                    out.append(f'        if (expandedMenu == "{key}") {{\n')
                    in_card = True
                    card_key = key
                    brace_count = 0
                    
            if not in_card:
                out.append(line)
            else:
                # Process the first line of the card
                # Actually, we should replace isExpanded and onClick while inside
                pass
                
        if in_card:
            # Modify line if needed
            new_line = line
            if f'isExpanded = expandedMenu == "{card_key}"' in line:
                new_line = line.replace(f'isExpanded = expandedMenu == "{card_key}"', 'isExpanded = true')
            
            # Since onClick can span multiple lines, let's just leave it alone! It won't hurt if they are true/false or clickable.
            # But we must format it with indentation.
            out.append("    " + new_line if new_line.strip() else new_line)
            
            # Track braces to find the end of ShopMenuCard { ... }
            brace_count += line.count('{')
            brace_count -= line.count('}')
            
            if brace_count == 0:
                # Assuming the ShopMenuCard ends when brace_count goes back to 0
                # Wait, ShopMenuCard( ... ) { ... }
                # The first `{` might not be on the `ShopMenuCard(` line.
                pass
                
            # Actually, tracking braces from `ShopMenuCard` is robust if we start counting correctly.
            # But `ShopMenuCard` might have `{` in `onClick = { ... }`. 
            # If brace_count hits 0 AND we have seen the main body `{`, it's done.
            # Let's refine brace counting:
            # We only finish when brace_count == 0 AND we've seen at least one '{' for the content block.
            
        i += 1

# Actually, the python script might be too complex for a quick fix. Let's just do it with a simpler state machine.
