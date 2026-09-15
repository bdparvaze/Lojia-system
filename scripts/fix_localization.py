import glob, re, os, xml.etree.ElementTree as ET

def get_keys(path):
    if not os.path.exists(path): return {}
    tree = ET.parse(path)
    root = tree.getroot()
    return {child.attrib["name"]: child.text for child in root if "name" in child.attrib}

def add_keys(path, new_keys):
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f: content = f.read()
    tree = ET.parse(path)
    root = tree.getroot()
    existing = {child.attrib["name"] for child in root if "name" in child.attrib}
    to_add = [(k, v) for k, v in new_keys.items() if k not in existing]
    if to_add:
        lines = []
        for k, v in to_add:
            lines.append(f'    <string name="{k}">{v}</string>')
        replacement = "\n".join(lines) + "\n</resources>"
        content = content.replace("</resources>", replacement)
        with open(path, "w", encoding="utf-8") as f: f.write(content)

en_add = {
    "title_drawer_menu": "Drawer Menu",
    "title_customer": "Customer",
    "title_options": "Options",
    "title_restricted_access": "Restricted Access",
    "msg_admin_pin_required": "Please enter your 6-digit administrator PIN to switch modules",
    "btn_clear": "Clear",
    "menu_items": "Items",
    "menu_categories": "Categories",
    "menu_modifiers": "Modifiers",
    "menu_discounts": "Discounts",
    "menu_printers": "Printers",
    "menu_customer_displays": "Customer displays",
    "menu_close_shift": "CLOSE SHIFT",
    "menu_daily_shift_report": "DAILY SHIFT REPORT GENERATOR",
    "desc_daily_shift_report": "Automated shift handover, cash reconciliation & PDF reports",
    "desc_manage_cashiers": "Manage %1$d cashier accounts, 6-digit PINs, and access privileges",
    "desc_app_language": "App language: %1$s",
    "desc_products_categories": "Products (%1$d), Categories (%2$d), Modifiers & Discounts",
    "desc_sales_history": "Sales history, transactions and receipts log",
    "desc_receipt_settings": "Receipt logo, custom footer, customer memos & live preview",
    "desc_hardware_settings": "Receipt printers, customer displays, VAT & tax configuration",
    "desc_business_profile": "Business & owner profile, address, contact information",
    "desc_security_settings": "6-Digit security PIN, biometrics lock, auto-lock timeout",
    "desc_cloud_back_office": "Cloud back office hub, inventory sync, multi-store management",
    "desc_system_about": "System version, software details, terms & privacy policy",
    "desc_customer_support": "Customer support & WhatsApp live chat",
    "desc_switch_module": "Switch between POS Shop Module and Shift Report Module",
    "shift_closing_and_revenue_report": "SHIFT CLOSING & REVENUE CERTIFICATE",
    "pdf_staff_meals": "Staff Meals",
    "pdf_regular_muassel": "Regular Muassel",
    "pdf_outdoor_muassel": "Outdoor Muassel",
    "pdf_notes": "Notes",
    "pdf_cashier_sign": "Cashier Signature"
}

bn_add = {
    "title_drawer_menu": "ড্রয়ার মেনু",
    "title_customer": "গ্রাহক",
    "title_options": "বিকল্প",
    "title_restricted_access": "নিয়ন্ত্রিত প্রবেশাধিকার",
    "msg_admin_pin_required": "মডিউল পরিবর্তন করতে আপনার ৬-সংখ্যার অ্যাডমিনিস্ট্রেটর পিন দিন",
    "btn_clear": "মুছে ফেলুন",
    "menu_items": "আইটেম",
    "menu_categories": "বিভাগ",
    "menu_modifiers": "মডিফায়ার",
    "menu_discounts": "ডিসকাউন্ট",
    "menu_printers": "প্রিন্টার",
    "menu_customer_displays": "গ্রাহক প্রদর্শন",
    "menu_close_shift": "শিফট বন্ধ করুন",
    "menu_daily_shift_report": "দৈনিক শিফট রিপোর্ট জেনারেটর",
    "desc_daily_shift_report": "স্বয়ংক্রিয় শিফট হস্তান্তর, নগদ মেলানো এবং পিডিএফ রিপোর্ট",
    "desc_manage_cashiers": "ক্যাশিয়ার অ্যাকাউন্ট, পিন এবং অ্যাক্সেস পরিচালনা করুন",
    "desc_app_language": "অ্যাপের ভাষা: %1$s",
    "desc_products_categories": "পণ্য (%1$d), বিভাগ (%2$d), মডিফায়ার",
    "desc_sales_history": "বিক্রয়ের ইতিহাস, লেনদেন এবং রসিদ লগ",
    "desc_receipt_settings": "রসিদের লোগো, ফুটার, মেমো এবং প্রিভিউ",
    "desc_hardware_settings": "প্রিন্টার, কাস্টমার ডিসপ্লে, ভ্যাট ও ট্যাক্স",
    "desc_business_profile": "ব্যবসার প্রোফাইল, ঠিকানা, যোগাযোগের তথ্য",
    "desc_security_settings": "৬-সংখ্যার পিন, বায়োমেট্রিক লক, টাইমআউট",
    "desc_cloud_back_office": "ক্লাউড ব্যাক অফিস, ইনভেন্টরি সিঙ্ক, মাল্টি-স্টোর",
    "desc_system_about": "সিস্টেম সংস্করণ, সফ্টওয়্যার বিবরণ, নীতি",
    "desc_customer_support": "গ্রাহক সহায়তা ও হোয়াটসঅ্যাপ লাইভ চ্যাট",
    "desc_switch_module": "পিওএস শপ এবং শিফট রিপোর্টের মধ্যে পরিবর্তন করুন",
    "shift_closing_and_revenue_report": "শিফট ক্লোজিং এবং রেভিনিউ রিপোর্ট",
    "pdf_staff_meals": "স্টাফ মিলস",
    "pdf_regular_muassel": "রেগুলার মুয়াসসেল",
    "pdf_outdoor_muassel": "আউটডোর মুয়াসসেল",
    "pdf_notes": "নোট",
    "pdf_cashier_sign": "ক্যাশিয়ারের স্বাক্ষর"
}

ar_add = {
    "title_drawer_menu": "قائمة الدرج",
    "title_customer": "العميل",
    "title_options": "الخيارات",
    "title_restricted_access": "الوصول مقيد",
    "msg_admin_pin_required": "يرجى إدخال رمز PIN للمسؤول المكون من 6 أرقام",
    "btn_clear": "مسح",
    "menu_items": "العناصر",
    "menu_categories": "الفئات",
    "menu_modifiers": "الإضافات",
    "menu_discounts": "الخصومات",
    "menu_printers": "الطابعات",
    "menu_customer_displays": "شاشات العملاء",
    "menu_close_shift": "إغلاق الوردية",
    "menu_daily_shift_report": "منشئ تقارير الورديات اليومية",
    "desc_daily_shift_report": "تسليم الوردية تلقائياً، تسوية النقد، تقارير PDF",
    "desc_manage_cashiers": "إدارة %1$d حساب كاشير، رموز PIN، صلاحيات",
    "desc_app_language": "لغة التطبيق: %1$s",
    "desc_products_categories": "المنتجات (%1$d)، الفئات (%2$d)، الإضافات والخصومات",
    "desc_sales_history": "سجل المبيعات، المعاملات، الإيصالات",
    "desc_receipt_settings": "شعار الإيصال، تذييل مخصص، المعاينة",
    "desc_hardware_settings": "الطابعات، شاشات العملاء، ضريبة القيمة المضافة",
    "desc_business_profile": "ملف العمل، العنوان، معلومات الاتصال",
    "desc_security_settings": "رمز أمان 6 أرقام، قفل بيومتري، مهلة",
    "desc_cloud_back_office": "مكتب خلفي سحابي، مزامنة المخزون، متاجر",
    "desc_system_about": "إصدار النظام، التفاصيل، سياسة الخصوصية",
    "desc_customer_support": "دعم العملاء ودردشة واتساب المباشرة",
    "desc_switch_module": "التبديل بين وحدة المبيعات وتقرير الورديات",
    "shift_closing_and_revenue_report": "تقرير إغلاق الوردية والإيرادات",
    "pdf_staff_meals": "وجبات الموظفين",
    "pdf_regular_muassel": "معسل عادي",
    "pdf_outdoor_muassel": "معسل خارجي",
    "pdf_notes": "ملاحظات",
    "pdf_cashier_sign": "توقيع الكاشير"
}

add_keys("app/src/main/res/values/strings.xml", en_add)
add_keys("app/src/main/res/values-bn/strings.xml", bn_add)
add_keys("app/src/main/res/values-ar/strings.xml", ar_add)
for other in glob.glob("app/src/main/res/values-*/strings.xml"):
    if "-bn" not in other and "-ar" not in other:
        add_keys(other, en_add) # Fallback English for rest

print("Done appending keys")
