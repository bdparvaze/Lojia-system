import glob

paths = ["app/src/main/res/values/strings.xml", "app/src/main/res/values-bn/strings.xml", "app/src/main/res/values-ar/strings.xml"]

for path in paths:
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Replace & with &amp; but not if already &amp;
    content = content.replace('SHIFT CLOSING & REVENUE CERTIFICATE', 'SHIFT CLOSING &amp; REVENUE CERTIFICATE')
    content = content.replace('Mada & Bank Card Payments', 'Mada &amp; Bank Card Payments')
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

