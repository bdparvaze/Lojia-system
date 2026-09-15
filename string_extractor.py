import re
import os
import xml.etree.ElementTree as ET
from xml.dom import minidom

# First, read existing keys from strings.xml
xml_path = 'app/src/main/res/values/strings.xml'
tree = ET.parse(xml_path)
root = tree.getroot()

existing_keys = set()
for child in root:
    if child.tag == 'string':
        existing_keys.add(child.attrib['name'])

def generate_key(text):
    clean = re.sub(r'[^a-zA-Z0-9\s_]', '', text)
    key = '_'.join(clean.lower().split()[:4])
    if not key:
        return None
    # make unique
    if key not in existing_keys:
        return key
    i = 1
    while f"{key}_{i}" in existing_keys:
        i += 1
    return f"{key}_{i}"

def is_human_readable(s):
    # skip single words that are all lowercase
    if re.match(r'^[a-z_0-9]+$', s): return False
    # skip empty or purely numeric or symbols
    if not re.search(r'[a-zA-Z]', s): return False
    # skip obvious sql or internal stuff
    if "SELECT" in s or "pos_" in s or "room" in s: return False
    if len(s) <= 2: return False
    return True

kt_files = []
for r, d, f in os.walk('app/src/main/java'):
    for file in f:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(r, file))

extracted = {}

for kt_file in kt_files:
    with open(kt_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find Toast.makeText(..., "...", ...)
    toasts = re.findall(r'Toast\.makeText\([^,]+,\s*"([^"]+)"', content)
    for t in toasts:
        if is_human_readable(t) and '{' not in t and '$' not in t:
            k = generate_key(t)
            extracted[k] = t
            existing_keys.add(k)
            
    # Find Text("...")
    texts = re.findall(r'Text\(\s*(?:text\s*=\s*)?"([^"]+)"', content)
    for t in texts:
        if is_human_readable(t) and '{' not in t and '$' not in t:
            k = generate_key(t)
            extracted[k] = t
            existing_keys.add(k)
            
    # Find DynamicText("...")
    dtexts = re.findall(r'DynamicText\(\s*(?:text\s*=\s*)?"([^"]+)"', content)
    for t in dtexts:
        if is_human_readable(t) and '{' not in t and '$' not in t:
            k = generate_key(t)
            extracted[k] = t
            existing_keys.add(k)

# Write to XML
for k, v in extracted.items():
    if k:
        ET.SubElement(root, 'string', {'name': k}).text = v

tree.write(xml_path, encoding='utf-8')
print(f"Extracted {len(extracted)} new strings.")
