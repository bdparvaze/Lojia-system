import glob

replacement = """    val rawCurrency = businessProfile?.currency ?: "SAR"
    val currency = if (rawCurrency == "SAR") stringResource(R.string.currency_unit) else rawCurrency"""

for file in glob.glob('app/src/main/java/com/example/ui/*.kt'):
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'val currency = businessProfile?.currency ?: stringResource(R.string.currency_unit)' in content:
        content = content.replace('val currency = businessProfile?.currency ?: stringResource(R.string.currency_unit)', replacement)
        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
