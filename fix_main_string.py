import re
import os

path = 'app/src/main/java/com/example/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

orig = content

def replacer(match):
    key = match.group(1)
    if re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', key):
        return f"stringResource(R.string.{key}"
    return match.group(0)

# Replaces stringResource("something" with stringResource(R.string.something
content = re.sub(r'stringResource\(\s*"([^"]+)"', replacer, content)

if orig != content:
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed MainActivity stringResource")
else:
    print("No change in MainActivity")
