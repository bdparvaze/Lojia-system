import re
import os
from xml.etree import ElementTree as ET

# 1. Parse strings.xml to get all keys
tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
keys = set()
for child in root:
    if child.tag == 'string':
        keys.add(child.attrib['name'])

# Add missing keys to strings.xml if needed
missing_keys = [
    'average_shift_sales', 'total_reports_count', 'payment_distribution', 'shift_comparison',
    'add_product', 'stock_quantity', 'adjust_stock', 'pdf_exported_success', 'open_pdf', 'invoice',
    'clear', 'backup_now_title', 'select_date', 'staff', 'opened', 'backing_up'
]

changed_xml = False
for mk in missing_keys:
    if mk not in keys:
        ET.SubElement(root, 'string', {'name': mk}).text = mk.replace('_', ' ').title()
        changed_xml = True

if changed_xml:
    tree.write('app/src/main/res/values/strings.xml', encoding='utf-8')

# 2. Add imports
kt_files = []
for root_dir, dirs, files in os.walk('app/src/main/java/com/example/ui'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root_dir, file))

for kt_file in kt_files:
    with open(kt_file, 'r', encoding='utf-8') as f:
        content = f.read()

    orig = content
    if 'stringResource' in content and 'import androidx.compose.ui.res.stringResource' not in content:
        # find package or first import
        content = content.replace('import androidx.compose.runtime.', 'import androidx.compose.ui.res.stringResource\nimport com.example.R\nimport androidx.compose.runtime.', 1)
    
    if 'import com.example.R' not in content and 'R.string.' in content:
        content = content.replace('import androidx.compose.runtime.', 'import com.example.R\nimport androidx.compose.runtime.', 1)

    if orig != content:
        with open(kt_file, 'w', encoding='utf-8') as f:
            f.write(content)

print("Imports added")
