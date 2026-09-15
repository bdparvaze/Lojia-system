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

# English
add_string_if_missing('app/src/main/res/values/strings.xml', 'shift_morning', 'Morning')
add_string_if_missing('app/src/main/res/values/strings.xml', 'shift_evening', 'Evening')
add_string_if_missing('app/src/main/res/values/strings.xml', 'shift_night', 'Night')

# Bengali
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'shift_morning', 'সকাল')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'shift_evening', 'সন্ধ্যা')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'shift_night', 'রাত')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'day', 'দিন')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'staff', 'স্টাফ')
add_string_if_missing('app/src/main/res/values-bn/strings.xml', 'staff_label', 'স্টাফ')

# Arabic
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'shift_morning', 'صباح')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'shift_evening', 'مساء')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'shift_night', 'ليل')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'day', 'يوم')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'staff', 'طاقم')
add_string_if_missing('app/src/main/res/values-ar/strings.xml', 'staff_label', 'طاقم')

