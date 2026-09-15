import os
import re

for file in ['app/src/main/res/values/strings.xml', 'app/src/main/res/values-bn/strings.xml', 'app/src/main/res/values-ar/strings.xml']:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # add app_name if not exists
    if 'name="app_name"' not in content:
        content = content.replace('</resources>', '    <string name="app_name">Smart POS</string>\n</resources>')

    # add formatted="false" to strings that have multiple % symbols
    def repl(m):
        tag = m.group(1)
        val = m.group(2)
        if val.count('%') > 1:
            return f'<string{tag} formatted="false">{val}</string>'
        return m.group(0)

    content = re.sub(r'<string([^>]*)>(.*?)</string>', repl, content)

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print("XMLs fixed.")
