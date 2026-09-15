import re
import os
import xml.etree.ElementTree as ET

xml_path = 'app/src/main/res/values/strings.xml'
tree = ET.parse(xml_path)
root = tree.getroot()

changed_keys = {}

for child in root:
    if child.tag == 'string':
        name = child.attrib['name']
        if re.match(r'^[0-9]', name):
            new_name = 'msg_' + name
            changed_keys[name] = new_name
            child.attrib['name'] = new_name

if changed_keys:
    tree.write(xml_path, encoding='utf-8')
    
    kt_files = []
    for r, d, f in os.walk('app/src/main/java'):
        for file in f:
            if file.endswith('.kt'):
                kt_files.append(os.path.join(r, file))

    for path in kt_files:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()

        orig = content
        for old, new in changed_keys.items():
            content = re.sub(r'R\.string\.' + old + r'\b', f'R.string.{new}', content)

        if orig != content:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)

    print(f"Fixed {len(changed_keys)} bad keys.")
else:
    print("No bad keys found.")
