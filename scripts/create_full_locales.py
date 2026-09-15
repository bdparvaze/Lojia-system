import xml.etree.ElementTree as ET
import json
import os
import re

# Load base English XML
tree_en = ET.parse('app/src/main/res/values/strings.xml')
root_en = tree_en.getroot()
en_items = root_en.findall('string')

# Helper to escape XML special characters if needed, although ElementTree handles text encoding
# Languages to generate:
# 'es': Spanish
# 'fr': French
# 'de': German
# 'hi': Hindi (Devanagari)
# 'ur': Urdu (Nastaliq / Arabic script)

