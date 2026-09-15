import re
with open('app/src/main/res/values-bn/strings.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'\(Night\)', '', content)
content = re.sub(r'\(Morning\)', '', content)
content = re.sub(r'\(Evening\)', '', content)
content = re.sub(r'\(Day\)', '', content)
content = re.sub(r'\(Full Day\)', '', content)
# Trim spaces
content = content.replace('রাত ', 'রাত').replace('সকাল ', 'সকাল').replace('সন্ধ্যা ', 'সন্ধ্যা')

with open('app/src/main/res/values-bn/strings.xml', 'w', encoding='utf-8') as f:
    f.write(content)
