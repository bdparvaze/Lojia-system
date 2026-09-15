import re
with open('app/src/main/res/values-ar/strings.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'\(Night\)', '', content)
content = re.sub(r'\(Morning\)', '', content)
content = re.sub(r'\(Evening\)', '', content)
content = re.sub(r'\(Day\)', '', content)
content = re.sub(r'\(Full Day\)', '', content)
content = content.replace('مسائي ', 'مسائي')
content = content.replace('ليلي ', 'ليلي')
content = content.replace('صباحي ', 'صباحي')

with open('app/src/main/res/values-ar/strings.xml', 'w', encoding='utf-8') as f:
    f.write(content)
