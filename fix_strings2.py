import re
import os
from xml.etree import ElementTree as ET

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
keys = set()
for child in root:
    if child.tag == 'string':
        keys.add(child.attrib['name'])

missing_keys = [
    'monthly_sales_summary', 'current_month', 'daily_average', 'peak_sales_day', 'projected_monthly',
    'monthly_revenue_trend', 'pos_direct_sales', 'shift_cashier_sales', 'weekly_breakdown', 'top_revenue_days',
    'ticket'
]

changed = False
for mk in missing_keys:
    if mk not in keys:
        ET.SubElement(root, 'string', {'name': mk}).text = mk.replace('_', ' ').title()
        changed = True

if changed:
    tree.write('app/src/main/res/values/strings.xml', encoding='utf-8')
    print("Added missing keys to strings.xml")

