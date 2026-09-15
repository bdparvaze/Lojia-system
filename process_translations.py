#!/usr/bin/env python3
import os
import re
import json
import time
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_DIR = "app/src/main/res"
ENGLISH_XML = os.path.join(BASE_DIR, "values", "strings.xml")

TARGETS = [
    ("hi", "values-hi", "hi"),
    ("id", "values-id", "id"),
    ("ja", "values-ja", "ja"),
    ("ru", "values-ru", "ru"),
    ("tr", "values-tr", "tr"),
    ("ur", "values-ur", "ur"),
    ("zh-rCN", "values-zh-rCN", "zh-CN")
]

SPEC_PATTERN = re.compile(r"%(?:\d+\$)?[.\d]*[a-zA-Z]")

def is_pure_format_or_symbol(s):
    if not s:
        return True
    stripped = re.sub(r"%\d+\$[a-zA-Z]|%\.[0-9]+f|%[a-zA-Z]|\d+|\+|\-|\.|\:|\/|\*|\%|\s+|\(|\)|•|\$|€|£|SAR|AED|USD|EUR|GBP", "", s)
    return len(stripped) == 0

def clean_text_for_trans(raw_text):
    text = raw_text.replace(r"\'", "'")
    specifiers = []
    def repl(m):
        specifiers.append(m.group(0))
        return f" XSPEC{len(specifiers)-1}X "
    protected = SPEC_PATTERN.sub(repl, text)
    return protected, specifiers

def restore_specifiers(translated, specifiers):
    res = translated
    for idx, spec in enumerate(specifiers):
        pattern = re.compile(rf"(?:X\s*SPEC\s*{idx}\s*X|x\s*spec\s*{idx}\s*x|XSPEC{idx}X)", re.IGNORECASE)
        res = pattern.sub(spec, res)
    return res

def escape_for_android_xml(text):
    s = text.replace(r"\'", "'")
    s = s.replace("'", r"\'")
    s = re.sub(r"&(?!amp;|lt;|gt;|quot;|apos;)", "&amp;", s)
    s = s.replace("<", "&lt;").replace(">", "&gt;")
    return s

def fetch_single(text, target_lang):
    if not text.strip() or is_pure_format_or_symbol(text):
        return text
    url = f"https://translate.google.com/translate_a/single?client=at&sl=en&tl={target_lang}&dt=t&q={urllib.parse.quote(text)}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=8) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return "".join(seg[0] for seg in data[0] if seg and seg[0]).strip()
        except Exception:
            time.sleep(0.3 * (attempt + 1))
    return text

def parse_xml_file(filepath):
    tree = ET.parse(filepath)
    root = tree.getroot()
    mapping = {}
    for elem in root.findall("string"):
        name = elem.get("name")
        text = elem.text or ""
        mapping[name] = text
    return mapping

def translate_locale(lang_tag, dir_name, g_lang, en_map, unique_texts):
    target_path = os.path.join(BASE_DIR, dir_name, "strings.xml")
    target_map = {}
    if os.path.exists(target_path):
        target_map = parse_xml_file(target_path)

    before_identical = sum(1 for k, v in target_map.items() if v == en_map.get(k, ""))
    print(f"[{dir_name}] Before: {before_identical}/{len(en_map)} ({before_identical/len(en_map)*100:.2f}%)")

    # Determine unique texts needing translation for this language
    needed_texts = {}
    for k, en_val in en_map.items():
        cur_val = target_map.get(k, "")
        needs_tr = False
        if not cur_val:
            needs_tr = True
        elif cur_val == en_val and not is_pure_format_or_symbol(en_val):
            needs_tr = True
        else:
            en_specs = sorted(SPEC_PATTERN.findall(en_val))
            cur_specs = sorted(SPEC_PATTERN.findall(cur_val))
            if en_specs != cur_specs:
                needs_tr = True
        
        if needs_tr and not is_pure_format_or_symbol(en_val):
            if en_val not in needed_texts:
                needed_texts[en_val] = unique_texts[en_val]

    print(f"[{dir_name}] Needs translation for {len(needed_texts)} unique strings.")

    trans_cache = {}
    # Translate concurrently with ThreadPoolExecutor
    def worker(en_text):
        prot_text, specs = needed_texts[en_text]
        raw_tr = fetch_single(prot_text, g_lang)
        restored = restore_specifiers(raw_tr, specs)
        # Check specifiers
        tr_specs = sorted(SPEC_PATTERN.findall(restored))
        en_specs = sorted(SPEC_PATTERN.findall(en_text))
        if tr_specs != en_specs:
            # If missing a specifier, fallback to clean replacement
            restored = en_text
        escaped = escape_for_android_xml(restored)
        return en_text, escaped

    with ThreadPoolExecutor(max_workers=12) as executor:
        futures = [executor.submit(worker, t) for t in needed_texts]
        for f in as_completed(futures):
            try:
                en_t, tr_t = f.result()
                trans_cache[en_t] = tr_t
            except Exception as e:
                pass

    final_map = {}
    for k, en_val in en_map.items():
        if is_pure_format_or_symbol(en_val):
            final_map[k] = en_val
            continue
        cur_val = target_map.get(k, "")
        if cur_val and cur_val != en_val:
            en_specs = sorted(SPEC_PATTERN.findall(en_val))
            cur_specs = sorted(SPEC_PATTERN.findall(cur_val))
            if en_specs == cur_specs:
                final_map[k] = cur_val
                continue
        
        if en_val in trans_cache:
            final_map[k] = trans_cache[en_val]
        else:
            final_map[k] = cur_val or en_val

    # Write output
    lines = ['<resources>']
    for k, en_val in en_map.items():
        val = final_map.get(k, en_val)
        lines.append(f'    <string name="{k}">{val}</string>')
    lines.append('</resources>\n')

    with open(target_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    after_identical = sum(1 for k, v in final_map.items() if v == en_map.get(k, ""))
    pct = (after_identical / len(en_map)) * 100
    print(f"[{dir_name}] After: {after_identical}/{len(en_map)} ({pct:.2f}%)")
    return dir_name, before_identical, after_identical, pct

def main():
    en_map = parse_xml_file(ENGLISH_XML)
    unique_texts = {}
    for k, v in en_map.items():
        if not is_pure_format_or_symbol(v) and v not in unique_texts:
            unique_texts[v] = clean_text_for_trans(v)

    results = []
    for lang_tag, dir_name, g_lang in TARGETS:
        res = translate_locale(lang_tag, dir_name, g_lang, en_map, unique_texts)
        results.append(res)

    print("\n================ FINAL TRANSLATION SUMMARY ================")
    for dir_name, b_cnt, a_cnt, pct in results:
        print(f"{dir_name}: before={b_cnt} ({b_cnt/len(en_map)*100:.2f}%) -> after={a_cnt} ({pct:.2f}%)")

if __name__ == "__main__":
    main()
