def add_string_if_missing(file_path, key, value):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    if f'name="{key}"' not in content:
        content = content.replace('</resources>', f'    <string name="{key}">{value}</string>\n</resources>')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

add_string_if_missing('app/src/main/res/values/strings.xml', 'currency_unit', 'SAR')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'currency_unit', 'রিয়াল')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'currency_unit', 'ر.س')
