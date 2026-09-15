import os
import re

kt_files = []
for root, dirs, files in os.walk('app/src/main/java/com/example/ui'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root, file))

for kt_file in kt_files:
    with open(kt_file, 'r', encoding='utf-8') as f:
        content = f.read()

    orig_content = content

    def replacer(match):
        key = match.group(1)
        if re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', key):
            return f"stringResource(R.string.{key}"
        return match.group(0)

    # Replaces stringResource("something" with stringResource(R.string.something
    content = re.sub(r'stringResource\(\s*"([^"]+)"', replacer, content)

    if orig_content != content:
        with open(kt_file, 'w', encoding='utf-8') as f:
            f.write(content)

print("Fixed stringResource")
