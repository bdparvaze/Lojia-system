import re
import os
import xml.etree.ElementTree as ET

xml_path = 'app/src/main/res/values/strings.xml'
tree = ET.parse(xml_path)
root = tree.getroot()
existing_keys = set([c.attrib['name'] for c in root if c.tag == 'string'])

def generate_key(text):
    clean = re.sub(r'[^a-zA-Z0-9\s_]', '', text)
    key = '_'.join(clean.lower().split()[:4])
    if not key: return "msg_key"
    if key not in existing_keys: return key
    i = 1
    while f"{key}_{i}" in existing_keys: i += 1
    return f"{key}_{i}"

new_strings = {}

# Process ViewModels
for vm_path in ['app/src/main/java/com/example/ui/ReportViewModel.kt', 'app/src/main/java/com/example/ui/PosViewModel.kt']:
    with open(vm_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Update Flow type
    content = content.replace('MutableSharedFlow<String>', 'MutableSharedFlow<com.example.util.UiText>')
    content = content.replace('asSharedFlow()', 'asSharedFlow()') # no change here just making sure type inference works
    if 'import com.example.util.UiText' not in content:
        content = content.replace('import kotlinx.coroutines.flow.MutableSharedFlow', 'import kotlinx.coroutines.flow.MutableSharedFlow\nimport com.example.util.UiText\nimport com.example.R')

    # 2. Extract and replace emit("...") calls
    def emit_replacer(match):
        text = match.group(1)
        # handle interpolation
        if '$' in text or '%' in text:
            # Let's keep it simple: DynamicString
            return f'_uiMessage.emit(UiText.DynamicString("{text}"))'
        
        # generate key
        key = generate_key(text)
        new_strings[key] = text
        existing_keys.add(key)
        return f'_uiMessage.emit(UiText.StringResource(R.string.{key}))'

    content = re.sub(r'_uiMessage\.emit\(\s*"([^"]+)"\s*\)', emit_replacer, content)

    with open(vm_path, 'w', encoding='utf-8') as f:
        f.write(content)

# Process UI files
kt_files = []
for r, d, f in os.walk('app/src/main/java/com/example/ui'):
    for file in f:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(r, file))

for path in kt_files:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    orig = content
    # Look for Toast.makeText(context, msg, ...)
    content = re.sub(r'Toast\.makeText\(context,\s*msg,\s*Toast\.LENGTH_SHORT\)', 'Toast.makeText(context, msg.asString(context), Toast.LENGTH_SHORT)', content)
    content = re.sub(r'Toast\.makeText\(context,\s*msg,\s*Toast\.LENGTH_LONG\)', 'Toast.makeText(context, msg.asString(context), Toast.LENGTH_LONG)', content)

    if orig != content:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)

# Update XML
for k, v in new_strings.items():
    ET.SubElement(root, 'string', {'name': k}).text = v

tree.write(xml_path, encoding='utf-8')
print("ViewModels and UIMessage updated")
