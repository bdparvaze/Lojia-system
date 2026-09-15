def add_string_if_missing(file_path, key, value):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        if f'name="{key}"' not in content:
            content = content.replace('</resources>', f'    <string name="{key}">{value}</string>\n</resources>')
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
    except Exception as e:
        pass

add_string_if_missing('app/src/main/res/values/strings.xml', 'shifts_count', '%1$d shifts')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'shifts_count', '%1$d শিফট')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'shifts_count', '%1$d نوبات')
