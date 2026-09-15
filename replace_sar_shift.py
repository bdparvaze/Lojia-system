import re
import os

def replace_sar_in_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Composable files
    if 'import androidx.compose.' in content:
        # replace "SAR" -> stringResource(R.string.currency_unit)
        content = content.replace('"SAR"', 'stringResource(R.string.currency_unit)')
        content = content.replace(' suffix = " SAR"', ' suffix = " ${stringResource(R.string.currency_unit)}"')
        content = content.replace(' "%.2f SAR"', ' "%.2f ${stringResource(R.string.currency_unit)}"')
        content = content.replace(' "0.00 SAR"', ' "0.00 ${stringResource(R.string.currency_unit)}"')
        content = content.replace(' "%.2f SAR (${e.type.name.lowercase()})"', ' "%.2f ${stringResource(R.string.currency_unit)} (${e.type.name.lowercase()})"')
    else:
        # ViewModel / non-composable
        # wait, ViewModels might need context. We have a context in some cases, or we can just pass Context or leave it for now.
        pass

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# First, find Morning, Evening, Night
kt_files = []
for r, d, f in os.walk('app/src/main/java'):
    for file in f:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(r, file))

for kt_file in kt_files:
    replace_sar_in_file(kt_file)
    
