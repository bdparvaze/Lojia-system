import re

with open("app/src/main/java/com/example/ui/SettingsShopSection.kt", "r") as f:
    content = f.read()

# Remove the whole Quick Jump section
content = re.sub(r'// Quick Jump.*?}\s*}\s*}', '', content, flags=re.DOTALL)

# Wrap each ShopMenuCard with an if block
def replacer(match):
    key = match.group(1)
    card_content = match.group(0)
    # The card is already checking isExpanded = expandedMenu == "key".
    # We will just wrap the whole thing in:
    # if (expandedMenu == "key") {
    #     ShopMenuCard(...)
    # }
    return f'if (expandedMenu == "{key}") {{\n        {card_content}\n    }}'

# We need to find ShopMenuCard... isExpanded = expandedMenu == "(.*?)"
content = re.sub(r'ShopMenuCard\([^)]*?expandedMenu == "([^"]+)"[^)]*?\)\s*\{.*?\n\s{8}\}', replacer, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/SettingsShopSection.kt", "w") as f:
    f.write(content)
