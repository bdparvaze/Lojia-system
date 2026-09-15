import os
import re
import xml.etree.ElementTree as ET
from xml.dom import minidom

# 1. Parse AppStrings.kt to extract keys and translations
appstrings_path = 'app/src/main/java/com/example/ui/AppStrings.kt'
with open(appstrings_path, 'r', encoding='utf-8') as f:
    appstrings_content = f.read()

def extract_language_map(content, func_name):
    # Matches: private fun getEnglish(key: String): String = when (key) { ... }
    # Using a simple parser since regex on nested blocks is hard
    start_idx = content.find(f'fun {func_name}')
    if start_idx == -1: return {}
    when_idx = content.find('when', start_idx)
    open_brace_idx = content.find('{', when_idx)
    
    # find closing brace of when block
    brace_count = 1
    end_idx = open_brace_idx + 1
    while brace_count > 0 and end_idx < len(content):
        if content[end_idx] == '{': brace_count += 1
        elif content[end_idx] == '}': brace_count -= 1
        end_idx += 1
        
    block = content[open_brace_idx+1 : end_idx-1]
    
    res = {}
    # extract "key" -> "value"
    # value can be multi-line or contain escaped chars
    pattern = re.compile(r'"([^"]+)"\s*->\s*"((?:\\.|[^"\\])*)"')
    for match in pattern.finditer(block):
        res[match.group(1)] = match.group(2)
    return res

en_dict = extract_language_map(appstrings_content, 'getEnglish')
bn_dict = extract_language_map(appstrings_content, 'getBengali')
ar_dict = extract_language_map(appstrings_content, 'getArabic')

print(f"Extracted {len(en_dict)} keys from English")

def sanitize_xml_string(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace("'", "\\'") # Android requires escaping single quotes
    # replace %s with %s or %1$s etc if needed, but simple stringResource format works well
    # also convert any actual newlines if any
    return s

def write_strings_xml(lang_dict, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    root = ET.Element("resources")
    for k, v in lang_dict.items():
        # Only write if k is a valid xml name (no spaces)
        if re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', k):
            string_elem = ET.SubElement(root, "string", name=k)
            # handle format args %d, %s etc.
            if '%' in v:
                # Android lint might complain if %s isn't positional like %1$s, but let's keep it simple first
                pass
            string_elem.text = v.replace("'", r"\'").replace('"', r'\"').replace('\n', r'\n').replace('&', '&amp;')

    xmlstr = minidom.parseString(ET.tostring(root, encoding='utf-8')).toprettyxml(indent="    ")
    
    # Fix the escaping that minidom might mess up
    xmlstr = xmlstr.replace('&amp;amp;', '&amp;')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(xmlstr)

write_strings_xml(en_dict, 'app/src/main/res/values/strings.xml')
write_strings_xml(bn_dict, 'app/src/main/res/values-bn/strings.xml')
write_strings_xml(ar_dict, 'app/src/main/res/values-ar/strings.xml')

# 2. Refactor all Kotlin files
kt_files = []
for root, dirs, files in os.walk('app/src/main/java'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root, file))

for kt_file in kt_files:
    with open(kt_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    orig_content = content

    # Replace stringResource("key") -> stringResource(R.string.key)
    # Be careful with stringResource("some string") vs keys. The keys in AppStrings don't have spaces.
    def replacer(match):
        key = match.group(1)
        if re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', key):
            # check if it exists in en_dict to be sure
            if key in en_dict:
                return f"stringResource(R.string.{key}"
        # unchanged if not a valid key
        return match.group(0)

    content = re.sub(r'stringResource\(\s*"([^"]+)"', replacer, content)

    if orig_content != content:
        # Add imports if changed
        if 'import androidx.compose.ui.res.stringResource' not in content:
            # find last import
            last_import_idx = content.rfind('import ')
            if last_import_idx != -1:
                end_of_line = content.find('\n', last_import_idx)
                content = content[:end_of_line] + "\nimport androidx.compose.ui.res.stringResource\nimport com.example.R" + content[end_of_line:]
        
        # Remove custom import
        content = re.sub(r'import com\.example\.ui\.stringResource\n?', '', content)

        with open(kt_file, 'w', encoding='utf-8') as f:
            f.write(content)

print("Refactoring complete.")
