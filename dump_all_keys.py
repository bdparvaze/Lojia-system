import xml.etree.ElementTree as ET
import json

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()

all_items = []
for e in root.findall('string'):
    all_items.append({
        'name': e.get('name'),
        'text': e.text or '',
        'formatted': e.get('formatted')
    })

with open('en_keys.json', 'w', encoding='utf-8') as f:
    json.dump(all_items, f, indent=2, ensure_ascii=False)

print(f'Saved {len(all_items)} keys to en_keys.json')
