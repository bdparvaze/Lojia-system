import xml.etree.ElementTree as ET
import os
import json
import re

# Load base English file
tree_en = ET.parse('app/src/main/res/values/strings.xml')
root_en = tree_en.getroot()
en_elements = root_en.findall('string')
key_names = [e.get('name') for e in en_elements]
key_formatted = {e.get('name'): e.get('formatted') for e in en_elements}
key_en_text = {e.get('name'): e.text or '' for e in en_elements}

