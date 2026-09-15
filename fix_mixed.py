with open('app/src/main/res/values-bn/strings.xml', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('দিন (Day)', 'দিন')
content = content.replace('পুরো দিন (Full Day)', 'পুরো দিন')

with open('app/src/main/res/values-bn/strings.xml', 'w', encoding='utf-8') as f:
    f.write(content)
