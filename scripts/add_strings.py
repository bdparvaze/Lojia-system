import xml.etree.ElementTree as ET
import glob
import re

pdf_strings = {
    "pdf_official_report": ("OFFICIAL REPORT", "অফিসিয়াল রিপোর্ট", "رپورٹ"),
    "pdf_report_id": ("Report ID", "রিপোর্ট আইডি", "رپورٹ آئی ڈی"),
    "pdf_cashier": ("Cashier", "ক্যাশিয়ার", "کیشیئر"),
    "pdf_shift": ("Shift", "শিফট", "شفٹ"),
    "pdf_date": ("Date", "তারিখ", "تاریخ"),
    "pdf_financial_breakdown": ("Financial Revenue Breakdown", "আর্থিক রাজস্ব বিবরণী", "مالیاتی آمدنی تفصیلات"),
    "pdf_gross_cash": ("Gross Cash Received", "ক্যাশ বিক্রয় (নগদ)", "کل نقد موصول"),
    "pdf_mada_bank": ("Mada & Bank Card Payments", "মাদা এবং ব্যাংক কার্ড", "مدى اور بینک کارڈز"),
    "pdf_digital_wallet": ("Digital Wallet / Apple Pay", "ডিজিٹل والٹ", "ڈیجیٹل والٹ"),
    "pdf_gross_total_sales": ("GROSS TOTAL SALES", "মোট ক্যাশ বিক্রয়", "کل سیلز"),
    "pdf_expenses": ("Shift Operational Expenses (-)", "শিফট খরচ (-)", "شفٹ اخراجات (-)"),
    "pdf_net_cash_in_drawer": ("NET CASH IN DRAWER", "ক্যাশ ড্রয়ারে নিট নগদ", "ڈراؤر میں خالص نقد"),
    "pdf_net_mada_bank": ("NET MADA / BANK", "নিট মাদা / ব্যাংক", "خالص مدى اور بینک"),
    "pdf_due_sales": ("Due Sales (Credit Entries)", "বকেয়া বিক্রি", "ادھار فروخت (Due Sales)"),
    "pdf_due_collection": ("Due Collection (Previous Dues)", "বকেয়া আদায়", "سابقہ بقایا جات وصولی"),
    "pdf_employer_advances": ("Employer Advances (Staff Advances)", "কর্মচারী অগ্রিম", "ملازمین کے ایڈوانس"),
    "pdf_walkout_bills": ("Walk-out Bills (Unpaid)", "ওয়াক-আউট বিল (অপরিশোধিত)", "غیر ادا شدہ بلز"),
    "pdf_paid_out_items": ("Paid Out Items / Purchases", "পরিশোধিত আইটেম / ক্রয়", "خریداری اور اخراجات"),
    "pdf_operational_metrics": ("Operational Metrics", "অপারেশনাল মেট্রিক্স", "آپریشنل اعداد و شمار"),
    "pdf_staff_meals": ("Staff Meals Count", "স্টাফ খাবার", "ملازمین کے کھانے"),
    "pdf_regular_muassel": ("Muassel / Shisha Served", "মুয়াস্সেল / শিশা", "معسل"),
    "pdf_outdoor_muassel": ("Outdoor Muassel / Shisha", "আউটডোর মুয়াস্সেল / শিশা", "آؤٹ ڈور معسل"),
    "pdf_receipt_no": ("Receipt #", "রসিদ নং", "رسید نمبر"),
    "pdf_customer": ("Customer / Reference", "গ্রাহক / রেফারেন্স", "گاہک / تفصیل"),
    "pdf_amount": ("Amount", "পরিমাণ", "رقم"),
    "pdf_mode": ("Mode", "মোড", "طریقہ کار"),
    "pdf_staff_name": ("Staff Name", "স্টাফ নাম", "ملازم کا نام"),
    "pdf_item": ("Item / Description", "আইটেম / বিবরণ", "آئٹم / تفصیل"),
    "pdf_qty": ("Qty", "পরিমাণ", "تعداد"),
    "pdf_total": ("Total", "মোট", "کل"),
    "pdf_notes": ("Notes", "নোট", "نوٹس"),
    "pdf_cashier_sign": ("Cashier Signature", "ক্যাশিয়ারের স্বাক্ষর", "دستخط کیشیئر"),
    "pdf_supervisor_sign": ("Supervisor / Shift Auditor", "সুপারভাইজার / শিফট অডিটর", "سپروائزر / آڈیٹر"),
    "pdf_page": ("Page", "পৃষ্ঠা", "صفحہ")
}

paths = {
    "en": "app/src/main/res/values/strings.xml",
    "bn": "app/src/main/res/values-bn/strings.xml",
    "ar": "app/src/main/res/values-ar/strings.xml"
}

for lang, path in paths.items():
    if not os.path.exists(path):
        continue
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # insert before </resources>
    insert_str = ""
    for key, vals in pdf_strings.items():
        if f'name="{key}"' not in content:
            val = vals[0] if lang == "en" else vals[1] if lang == "bn" else vals[2]
            insert_str += f'    <string name="{key}">{val}</string>\n'
    
    if insert_str:
        content = content.replace("</resources>", f"{insert_str}</resources>")
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)

