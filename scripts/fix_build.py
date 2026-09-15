import os

f = "app/src/main/java/com/example/ui/SettingsShopSection.kt"
with open(f, "r", encoding="utf-8") as fp: content = fp.read()
content = content.replace('posViewModel.addModifier(newModName, stringResource(R.string.title_options), extra)', 'val optionsStr = context.getString(R.string.title_options)\n                        posViewModel.addModifier(newModName, optionsStr, extra)')
with open(f, "w", encoding="utf-8") as fp: fp.write(content)

f = "app/src/main/java/com/example/util/PdfReportGenerator.kt"
with open(f, "r", encoding="utf-8") as fp: content = fp.read()
content = content.replace('title = pStr.shiftClosingAndRevenueReport,', 'title = "SHIFT CLOSING & REVENUE CERTIFICATE",')
content = content.replace('notes = pStr.notes,', 'notes = "Notes",')
content = content.replace('cashierSign = pStr.cashierSign,', 'cashierSign = "Cashier Signature",')
content = content.replace('title = TranslationEngine.translate(pStr.shiftClosingAndRevenueReport, code),', 'title = TranslationEngine.translate("SHIFT CLOSING & REVENUE CERTIFICATE", code),')
content = content.replace('notes = TranslationEngine.translate(pStr.notes, code),', 'notes = TranslationEngine.translate("Notes", code),')
content = content.replace('cashierSign = TranslationEngine.translate(pStr.cashierSign, code),', 'cashierSign = TranslationEngine.translate("Cashier Signature", code),')
with open(f, "w", encoding="utf-8") as fp: fp.write(content)
