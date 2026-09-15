import xml.etree.ElementTree as ET
import re

# Comprehensive dictionary for Bengali translations
BN_MAP = {
    # Cashier & Staff
    "cashier_management_1": "ক্যাশিয়ার ব্যবস্থাপনা",
    "cashier_management_2": "ক্যাশিয়ার ও ব্যবস্থাপনা",
    "cashier_management_3": "ক্যাশিয়ার ব্যবস্থাপনা",
    "add_edit_or_delete": "শিফট রিপোর্ট ও পিওএস-এর জন্য ক্যাশিয়ার যোগ, পরিবর্তন বা মুছুন",
    "manage_pos_cashiers_and": "পিওএস ক্যাশিয়ার এবং শিফট রিপোর্ট কর্মী পরিচালনা করুন",
    "add_cashier_1": "+ ক্যাশিয়ার যোগ করুন",
    "add_cashier_2": "ক্যাশিয়ার যোগ করুন",
    "add_cashier_3": "ক্যাশিয়ার যোগ করুন",
    "add_cashier_4": "+ ক্যাশিয়ার যোগ করুন",
    "no_cashiers_found_1": "কোনো ক্যাশিয়ার পাওয়া যায়নি",
    "no_cashiers_found_2": "কোনো ক্যাশিয়ার পাওয়া যায়নি",
    "add_cashiers_here_to": "দৈনিক শিফট রিপোর্ট এবং ক্যাশ ট্র্যাকিংয়ের জন্য এখানে ক্যাশিয়ার যোগ করুন।",
    "add_staff_members_here": "এখানে স্টাফ সদস্য যোগ করুন যাতে পিওএস এবং দৈনিক শিফট রিপোর্টে ক্যাশিয়ার নির্বাচন করা যায়।",
    "add_first_cashier": "+ প্রথম ক্যাশিয়ার যোগ করুন",
    "add_first_cashier_1": "+ প্রথম ক্যাশিয়ার যোগ করুন",
    "pin_status_active": "পিন: •••••• • অবস্থা: সক্রিয়",
    "pin_status_active_1": "পিন: •••••• • অবস্থা: সক্রিয়",
    "cashier_1": "ক্যাশিয়ার",
    "cashier_2": "ক্যাশিয়ার",
    "cashier_3": "ক্যাশিয়ার",
    "cashier_4": "ক্যাশিয়ার",
    "cashier_5": "ক্যাশিয়ার",
    "cashier_6": "ক্যাশিয়ার",
    "employee": "কর্মচারী",
    "employee_1": "কর্মচারী",
    "manager": "ম্যানেজার",
    "manager_1": "ম্যানেজার",
    "administrator": "অ্যাডমিনিস্ট্রেটর",
    "add_member": "সদস্য যোগ করুন",
    "add_member_1": "সদস্য যোগ করুন",
    "remove_member": "সদস্য মুছবেন?",
    "full_name_1": "পূর্ণ নাম *",
    "full_name_2": "পূর্ণ নাম *",
    "eg_john_doe_sarah": "যেমন: আহমেদ / সারা",
    "eg_john_doe_sarah_1": "যেমন: আহমেদ / সারা",
    "msg_6digit_security_pin": "৬ সংখ্যার সিকিউরিটি পিন",
    "msg_6digit_security_pin_1": "৬ সংখ্যার সিকিউরিটি পিন",
    "select_role_position": "দায়িত্ব / ভূমিকা নির্বাচন করুন:",
    "select_role_position_1": "দায়িত্ব / ভূমিকা নির্বাচন করুন:",
    "select_target_role_to": "স্টাফ অ্যাক্সেস দেখতে ভূমিকা নির্বাচন করুন:",

    # Cash & Drawer Management
    "cash_management_title": "ক্যাশ ড্রয়ার ব্যবস্থাপনা",
    "cash_management_1": "ক্যাশ ব্যবস্থাপনা",
    "pay_in_desc": "ড্রয়ারে অতিরিক্ত নগদ বা প্রারম্ভিক ক্যাশ যোগ করুন",
    "pay_out_desc": "খরচ বা ভেন্ডর পেমেন্টের জন্য ড্রয়ার থেকে নগদ উত্তোলন",
    "drawer_history": "ক্যাশ ড্রয়ার লেনদেনের ইতিহাস",
    "shift_open_time": "শিফট শুরু হয়েছিল",
    "pay_in_add_cash": "ক্যাশ ইন (টাকা যোগ)",
    "pay_out_remove_cash": "ক্যাশ আউট (টাকা উত্তোলন)",
    "reason_note": "কারণ / নোট",
    "cash_in": "ক্যাশ ইন",
    "cash_out": "ক্যাশ আউট",
    "cash_movement_recorded_successfully": "ক্যাশ লেনদেন সফলভাবে সংরক্ষিত হয়েছে",
    "please_enter_a_valid": "সঠিক পরিমাণ লিখুন এবং ক্যাশিয়ার নির্বাচন করুন",
    "cash_drawer_started_with": "ক্যাশ ড্রয়ার %.2f %s প্রারম্ভিক ব্যালেন্স নিয়ে শুরু হয়েছে।",

    # MPIN Security
    "mpin_security": "এমপিন (MPIN) নিরাপত্তা",
    "mpin_security_1": "এমপিন (MPIN) নিরাপত্তা",
    "quick_login_with_mpin": "এমপিন দিয়ে দ্রুত লগইন",
    "quick_login_with_mpin_1": "এমপিন দিয়ে দ্রুত লগইন",
    "unlock_pos_and_approve": "৬-সংখ্যার এমপিন দিয়ে পিওএস আনলক ও লেনদেন অনুমোদন করুন",
    "unlock_pos_and_approve_1": "৬-সংখ্যার এমপিন দিয়ে পিওএস আনলক ও লেনদেন অনুমোদন করুন",
    "reset_mpin_1": "এমপিন পরিবর্তন",
    "reset_mpin_2": "এমপিন রিসেট করুন",
    "reset_mpin_3": "এমপিন পরিবর্তন",
    "reset_mpin_4": "এমপিন রিসেট করুন",
    "tap_to_setup_or": "আপনার ৬-সংখ্যার সিকিউরিটি এমপিন সেট বা পরিবর্তন করতে ট্যাপ করুন",
    "tap_to_setup_or_1": "আপনার ৬-সংখ্যার সিকিউরিটি এমপিন সেট বা পরিবর্তন করতে ট্যাপ করুন",
    "shouldnt_be_simple_number": "সহজ ক্রম বা একই সংখ্যার পুনরাবৃত্তি হওয়া উচিত নয় (যেমন: ১২৩৪৫৬ বা ১১১১১১)",
    "shouldnt_be_simple_number_1": "সহজ ক্রম বা একই সংখ্যার পুনরাবৃত্তি হওয়া উচিত নয় (যেমন: ১২৩৪৫৬ বা ১১১১১১)",
    "use_security_pin": "সিকিউরিটি পিন ব্যবহার করুন",
    "admin_pin_123456": "অ্যাডমিন পিন (১২৩৪৫৬)",
    "admin_pin_default_123456": "অ্যাডমিন পিন (ডিফল্ট: ১২৩৪৫৬)",
    "admin_authorization_required": "অ্যাডমিন অনুমতি প্রয়োজন",
    "this_setting_is_protected": "এই সেটিংসটি সুরক্ষিত। পরিবর্তন করতে ৬ সংখ্যার অ্যাডমিন পিন দিন।",
    "incorrect_pin_try_again": "ভুল পিন। আবার চেষ্টা করুন।",
    "incorrect_pin": "ভুল পিন",
    "unlock": "আনলক করুন",
    "authenticated_as_administrator": "অ্যাডমিনিস্ট্রেটর হিসেবে যাচাই সম্পন্ন",
    "invalid_admin_pin": "অকার্যকর অ্যাডমিন পিন",

    # Shifts & Reports
    "average_shift_sales": "গড় শিফট বিক্রয়",
    "total_reports_count": "মোট রিপোর্টের সংখ্যা",
    "payment_distribution": "পেমেন্ট পদ্ধতি বণ্টন",
    "shift_comparison": "শিফট তুলনা",
    "detailed_shift_records": "বিস্তারিত শিফট রেকর্ড",
    "close_shift_1": "শিফট সমাপ্ত করুন",
    "close_shift_2": "শিফট সমাপ্ত করুন",
    "shift_status_active": "শিফটের অবস্থা: সক্রিয়",
    "current_shift_cash_2f": "বর্তমান শিফট ক্যাশ: %.2f %s",
    "closing_notes": "ক্লোজিং নোট",
    "audit_drawer_cash_create": "ক্যাশ ড্রয়ার পরীক্ষা ও শিফট পিডিএফ রিপোর্ট তৈরি করুন",
    "shift_closed_successfully": "শিফট সফলভাবে সম্পন্ন হয়েছে",
    "shift_report_submitted_successfully": "শিফট রিপোর্ট সফলভাবে জমা দেওয়া হয়েছে",
    "shift_report_submitted_successfully_1": "শিফট রিপোর্ট সফলভাবে জমা দেওয়া হয়েছে",
    "shift_report_saved_successfully": "শিফট রিপোর্ট সফলভাবে সংরক্ষিত হয়েছে!",
    "draft_saved_successfully": "ড্রাফট রিপোর্ট সংরক্ষিত হয়েছে",
    "please_enter_shift_sales": "জমা দেওয়ার আগে শিফট বিক্রয় বা ক্যাশের পরিমাণ দিন",
    "report_deleted": "রিপোর্ট মুছে ফেলা হয়েছে",
    "lojia_system_shift_reports": "লজিয়া সিস্টেম - শিফট রিপোর্ট সারাংশ",
    "monthly_sales_summary": "মাসিক বিক্রয় সারাংশ",
    "current_month": "চলতি মাস",
    "daily_average": "দৈনিক গড়",
    "peak_sales_day": "সর্বোচ্চ বিক্রির দিন",
    "projected_monthly": "প্রত্যাশিত মাসিক আয়",
    "monthly_revenue_trend": "মাসিক আয়ের ধারা",
    "pos_direct_sales": "পিওএস সরাসরি বিক্রয়",
    "shift_cashier_sales": "শিফট ক্যাশিয়ার বিক্রয়",
    "weekly_breakdown": "সাপ্তাহিক বিশ্লেষণ",
    "top_revenue_days": "সর্বোচ্চ আয়ের দিনসমূহ",
    "msg_5week_distribution": "৫-সপ্তাহের বিতরণ",
    "total_revenue_1": "মোট রাজস্ব",
    "grand_totals": "সর্বমোট:",
    "total_sales": "মোট বিক্রয়",
    "net_cash_1": "নিট ক্যাশ",
    "manager_supervisor_signature": "ম্যানেজার / সুপারভাইজারের স্বাক্ষর",
    "finance_auditor_official_seal": "অডিটর / অফিশিয়াল সিল",
    "ajoya_pos_system_v10": "আজোয়া পিওএস সিস্টেম v১.০ • গোপনীয় আর্থিক প্রতিবেদন",
    "tax_invoice_details": "ট্যাক্স ইনভয়েস বিবরণ",
    "this_is_an_official": "এটি ZATCA বিধিমালার সাথে সামঞ্জস্যপূর্ণ একটি অফিসিয়াল ট্যাক্স ইনভয়েস।",
    "no_pos_sales_available": "স্বয়ংক্রিয়ভাবে পূরণের জন্য কোনো বিক্রয় নেই",
    "please_fix_invalid_numeric": "সংরক্ষণের আগে ভুল সংখ্যা সংশোধন করুন",
    "please_fix_invalid_numeric_1": "সংরক্ষণের আগে ভুল সংখ্যা সংশোধন করুন",

    # Products & Catalog
    "add_product": "পণ্য যোগ করুন",
    "add_new_product": "নতুন পণ্য যোগ করুন",
    "product_name_1": "পণ্যের নাম",
    "stock_quantity": "স্টকের পরিমাণ",
    "adjust_stock": "স্টক সমন্বয়",
    "category": "ক্যাটাগরি",
    "add_new_category": "নতুন ক্যাটাগরি যোগ করুন",
    "category_name": "ক্যাটাগরির নাম",
    "add_modifier_1": "মডিফায়ার যোগ করুন",
    "modifier_name": "মডিফায়ারের নাম",
    "add_discount_1": "ডিসকাউন্ট যোগ করুন",
    "discount_name": "ডিসকাউন্টের নাম",
    "value_eg_10": "মান (যেমন: ১০)",
    "percentage_discount": "শতাংশ (%) ছাড়",
    "restock": "+ স্টক বৃদ্ধি",
    "deduct": "- স্টক হ্রাস",
    "price_2f_s": "মূল্য: %.2f %s",
    "stock_0f_s": "স্টক: %.0f %s",

    # Hardware & Printers
    "printers_configuration": "প্রিন্টার কনফিগারেশন",
    "printer_name": "প্রিন্টারের নাম",
    "autocut_receipts": "অটো-কাট রসিদ",
    "print_test_receipt": "টেস্ট রসিদ প্রিন্ট করুন",
    "printing_test_receipt": "টেস্ট রসিদ প্রিন্ট হচ্ছে...",
    "printer_test_simulated": "প্রিন্টার টেস্ট সিমুলেশন",
    "thermal_print_command_sent": "থার্মাল প্রিন্ট কমান্ড পাঠানো হয়েছে!",
    "paper_width_80mmn_cash": "• কাগজের প্রস্থ: ৮০মিমি\\n• ক্যাশ ড্রয়ার: কিক পালস পাঠানো হয়েছে\\n• অবস্থা: প্রস্তুত (অনলাইন)\\n• টেস্ট রসিদ সফলভাবে প্রিন্ট হয়েছে।",
    "customer_displays_1": "কাস্টমার ডিসপ্লে",
    "enable_customer_display": "কাস্টমার ডিসপ্লে চালু করুন",
    "welcome_greeting_message": "স্বাগতম শুভেচ্ছা বার্তা",
    "loyverse_kds_ready": "কিচেন ডিসপ্লে (KDS) প্রস্তুত",
    "loyverse_cds_connected": "কাস্টমার ডিসপ্লে (CDS) সংযুক্ত",

    # Barcode & Camera
    "barcode_scanner_1": "বারকোড স্ক্যানার",
    "align_barcode_with_camera": "ক্যামেরার সামনে বারকোড রাখুন বা ম্যানুয়ালি কোড লিখুন:",
    "camera_viewfinder_active": "ক্যামেরা ভিউফাইন্ডার সক্রিয়",
    "enter_barcode_eg_1001": "বারকোড লিখুন (যেমন: ১০০১, ১০০২)",
    "lookup_item": "পণ্য খুঁজুন",
    "optical_ai_lens_active": "অপটিক্যাল এআই লেন্স সক্রিয়",
    "camera_permission_required": "ক্যামেরা অনুমতি প্রয়োজন",
    "tap_product_to_test": "⚡ টেস্ট স্ক্যান করতে পণ্যে ট্যাপ করুন:",
    "eg_101_102_628100": "যেমন: ১০১, ১০২, ৬২৮১০০...",
    "scan": "স্ক্যান করুন",

    # Taxes & Store Settings
    "taxes_vat": "ট্যাক্স ও ভ্যাট",
    "vat_rate": "ভ্যাট হার (%)",
    "tax_vat_registration_number": "ট্যাক্স / ভ্যাট রেজিস্ট্রেশন নম্বর",
    "tax_settings_saved": "ট্যাক্স সেটিংস সংরক্ষিত হয়েছে",
    "general_store_settings": "সাধারণ দোকান সেটিংস",
    "store_business_name": "দোকান / ব্যবসার নাম",
    "store_settings_saved": "দোকান সেটিংস সংরক্ষিত হয়েছে",
    "business_settings_updated": "ব্যবসা সেটিংস আপডেট হয়েছে",
    "profile_updated_successfully": "প্রোফাইল সফলভাবে আপডেট হয়েছে",
    "receipt_customization_updated": "রসিদ কাস্টমাইজেশন আপডেট হয়েছে",
    "select_any_settings_category": "পিওএস ফিচার কনফিগার করতে নিচে যেকোনো সেটিংস নির্বাচন করুন",
    "catalog_sales": "ক্যাটালগ ও বিক্রয়",
    "hardware_localization": "হার্ডওয়্যার ও ভাষা",

    # Support & Legal
    "terms_of_service": "ব্যবহারের শর্তাবলী",
    "by_using_lojia_shift": "লজিয়া পিওএস ও শিফট রিপোর্ট ব্যবহারের মাধ্যমে আপনি স্থানীয় ডাটা সংরক্ষণ এবং নিরাপদ ক্লাউড সিঙ্ক করতে সম্মত হচ্ছেন।",
    "terms_conditions_1": "শর্তাবলী ও নিয়মাবলী",
    "msg_1_license_usagenlojia_system": "১. লাইসেন্স ও ব্যবহার\\nএই সফটওয়্যারটি বাণিজ্যিক পিওএস এবং শিফট ব্যবস্থাপনার জন্য তৈরি। সকল ডাটার পূর্ণ নিয়ন্ত্রণ আপনার হাতে।\\n\\n২. সঠিক হিসাব\\nক্যাশিয়ার ও ম্যানেজার সঠিক ক্যাশ হিসাবের জন্য দায়ী।\\n\\n৩. ক্লাউড ব্যাকআপ\\nক্লাউড ব্যাকআপ এনক্রিপ্ট করে সুরক্ষিত রাখা হয়।",
    "i_understand": "আমি বুঝতে পেরেছি",
    "privacy_policy_1": "গোপনীয়তা নীতি",
    "privacy_policy_2": "গোপনীয়তা নীতি",
    "your_financial_data_shift": "আপনার আর্থিক ডাটা এবং শিফট রেকর্ড এই ডিভাইসে এনক্রিপ্ট করা থাকে। কোনো ব্যক্তিগত তথ্য বিক্রি বা হস্তান্তর করা হয় না।",
    "msg_1_data_collectionnlojia_system": "১. তথ্য সংগ্রহ\\nবিক্রয় লেনদেন, ইনভেন্টরি ক্যাটালগ ও শিফট হিসাব লোকাল ডিভাইসে সংরক্ষিত হয়।\\n\\n২. নিরাপত্তা\\nবায়োমেট্রিক ডাটা অ্যান্ড্রয়েড সিকিউর কি-স্টোরে সুরক্ষিত থাকে।\\n\\n৩. তৃতীয় পক্ষ\\nব্যবহারকারীর অনুমতি ছাড়া কোনো তথ্য কারো সাথে শেয়ার করা হয় না।",
    "submit_support_ticket": "সাপোর্ট টিকিট জমা দিন",
    "ticket_tk8892_created": "টিকিট #TK-8892 তৈরি হয়েছে!",
    "our_technical_support_team": "আমাদের টেকনিক্যাল সাপোর্ট টিম দ্রুত আপনার সাথে যোগাযোগ করবে।",
    "need_help_with_pos": "পিওএস, প্রিন্টার বা শিফট রিপোর্ট সংক্রান্ত যেকোনো সহায়তায় আমাদের টিম ২৪/৭ প্রস্তুত:",
    "subject_eg_printer_setup": "বিষয় (যেমন: প্রিন্টার সেটআপ)",
    "describe_the_issue": "সমস্যার বিবরণ লিখুন...",
    "submit_ticket_1": "টিকিট পাঠান",

    # Common Actions & Buttons
    "save_1": "সংরক্ষণ",
    "save_2": "সংরক্ষণ",
    "save_3": "সংরক্ষণ",
    "save_4": "সংরক্ষণ",
    "save_5": "সংরক্ষণ",
    "cancel_1": "বাতিল",
    "cancel_2": "বাতিল",
    "cancel_3": "বাতিল",
    "cancel_4": "বাতিল",
    "cancel_5": "বাতিল",
    "cancel_6": "বাতিল",
    "cancel_7": "বাতিল",
    "cancel_8": "বাতিল",
    "cancel_9": "বাতিল",
    "cancel_10": "বাতিল",
    "cancel_11": "বাতিল",
    "cancel_12": "বাতিল",
    "cancel_13": "বাতিল",
    "cancel_14": "বাতিল",
    "cancel_15": "বাতিল",
    "cancel_16": "বাতিল",
    "cancel_17": "বাতিল",
    "cancel_18": "বাতিল",
    "close_1": "বন্ধ করুন",
    "close_2": "বন্ধ করুন",
    "close_3": "বন্ধ করুন",
    "close_4": "বন্ধ করুন",
    "close_5": "বন্ধ করুন",
    "close_6": "বন্ধ করুন",
    "close_7": "বন্ধ করুন",
    "add_1": "যোগ করুন",
    "add_2": "যোগ করুন",
    "add_3": "যোগ করুন",
    "add_4": "যোগ করুন",
    "delete_1": "মুছুন",
    "open_1": "খুলুন",
    "open_2": "খুলুন",
    "launch_1": "চালু করুন",
    "done": "সম্পন্ন",
    "done_1": "সম্পন্ন",
    "submit": "জমা দিন",
    "apply": "প্রয়োগ করুন",
    "clear": "পরিষ্কার করুন",
    "search_world_languages": "ভাষা খুঁজুন...",
    "search_world_languages_1": "ভাষা খুঁজুন...",
    "pdf_exported_success": "পিডিএফ সফলভাবে এক্সপোর্ট হয়েছে",
    "open_pdf": "পিডিএফ খুলুন",
    "invoice": "ইনভয়েস",
    "invoice_pdf_saved": "ইনভয়েস পিডিএফ সংরক্ষিত হয়েছে!",
    "backup_now_title": "এখনই ব্যাকআপ নিন",
    "select_date": "তারিখ নির্বাচন করুন",
    "opened": "শুরু হয়েছে",
    "ticket": "টিকিট",
    "date_1": "তারিখ",
    "date_2": "তারিখ",
    "shift_1": "শিফট",
    "shift_2": "শিফট",
    "cash_1": "ক্যাশ",
    "madacard": "মাদা / কার্ড",
    "expenses": "খরচ",
    "sync_complete_data_up": "সিঙ্ক সম্পন্ন! সকল তথ্য আপডেট করা হয়েছে।",
    "opening_loyverse_dashboard": "ড্যাশবোর্ড খোলা হচ্ছে...",
    "configuration_successfully_restored_and": "কনফিগারেশন সফলভাবে পুনরুদ্ধার ও সিঙ্ক হয়েছে",
    "failed_to_import_configuration": "কনফিগারেশন ফাইল ইম্পোর্ট ব্যর্থ হয়েছে",
    "background_sync_task_enqueued": "ব্যাকগ্রাউন্ড সিঙ্ক শুরু হয়েছে।",
    "no_pdf_viewer_app": "পিডিএফ দেখার অ্যাপ পাওয়া যায়নি। ফাইল স্টোরেজে সংরক্ষিত।",
    "interactive_curve_with_tooltips": "টুলটিপ এবং দৈনিক রাজস্বের রেখাচিত্র",
    "security_question": "সিকিউরিটি প্রশ্ন (Security Question):",
    "password_1": "রেজিস্ট্রেশন পাসওয়ার্ড (Password)",
    "security_answer": "প্রশ্নের উত্তর (Security Answer)",
    "pin_1": "পিন (PIN)",
}

def update_strings_file(filepath, new_entries):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Strip closing </resources>
    closing_tag = '</resources>'
    idx = content.rfind(closing_tag)
    if idx == -1:
        print("Closing tag not found in", filepath)
        return

    prefix = content[:idx]
    
    # Generate new XML tags
    lines = []
    for name, val in new_entries.items():
        # Escape XML entities if not already escaped
        val_clean = val.replace('&', '&amp;') if '&amp;' not in val else val
        val_clean = val_clean.replace('<', '&lt;').replace('>', '&gt;')
        lines.append(f'    <string name="{name}">{val_clean}</string>')

    added_content = '\n'.join(lines) + '\n' + closing_tag + '\n'
    new_full_content = prefix + added_content

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_full_content)
    print(f"Updated {filepath} with {len(new_entries)} strings.")

if __name__ == '__main__':
    en_tree = ET.parse('app/src/main/res/values/strings.xml')
    bn_tree = ET.parse('app/src/main/res/values-bn/strings.xml')

    en_dict = {elem.attrib['name']: (elem.text or '') for elem in en_tree.getroot().findall('string')}
    bn_keys = {elem.attrib['name'] for elem in bn_tree.getroot().findall('string')}

    missing = {k: v for k, v in en_dict.items() if k not in bn_keys}
    print(f"Total missing in BN: {len(missing)}")

    bn_to_add = {}
    for k, en_text in missing.items():
        if k in BN_MAP:
            bn_to_add[k] = BN_MAP[k]
        elif '%s' in en_text or '%.2f' in en_text or '%d' in en_text:
            # Preserves format specifiers
            val = en_text
            val = val.replace('SAR', 'SAR').replace('POS', 'পিওএস').replace('Shift', 'শিফট')
            val = val.replace('Cash', 'ক্যাশ').replace('Mada', 'মাদা').replace('Price', 'মূল্য').replace('Stock', 'স্টক')
            bn_to_add[k] = val
        else:
            # Fallback
            bn_to_add[k] = en_text

    update_strings_file('app/src/main/res/values-bn/strings.xml', bn_to_add)
