package com.lojia.pos.util


import com.lojia.pos.R


import android.content.ContentValues

import android.content.Context

import android.content.Intent

import android.graphics.*

import android.graphics.pdf.PdfDocument

import android.net.Uri

import android.os.Build

import android.os.Environment

import android.provider.MediaStore

import android.widget.Toast


import androidx.core.content.FileProvider

import com.lojia.pos.data.AppLanguage

import com.lojia.pos.data.BusinessProfile

import com.lojia.pos.data.POSSale

import com.lojia.pos.data.ShiftReport
import org.json.JSONArray

import java.io.File

import java.io.FileOutputStream

import java.io.OutputStream

import java.text.SimpleDateFormat

import java.util.*

object PdfReportGenerator {
    private fun getLocalizedShiftName(language: AppLanguage, shift: String): String {
        val code = language.code
        return when (shift.lowercase()) {
            "morning" -> TranslationEngine.translate("Morning", code)
            "evening" -> TranslationEngine.translate("Evening", code)
            "night" -> TranslationEngine.translate("Night", code)
            "day" -> TranslationEngine.translate("Day", code)
            else -> TranslationEngine.translate(shift, code)
        }
    }


    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)

    private data class DueCreditEntry(val receiptNo: String, val customerName: String, val amount: Double)
    private data class DueCollectionEntry(val receiptNo: String, val customerName: String, val amount: Double, val paymentMode: String)
    private data class StaffAdvanceEntry(val staffName: String, val amount: Double, val paymentMode: String)
    private data class WalkoutEntry(val tableOrOrderRef: String, val amount: Double)
    private data class PurchasedItemEntry(val itemName: String, val quantity: Double, val unitPrice: Double, val totalAmount: Double)

    private fun parseDueCreditEntries(jsonStr: String): List<DueCreditEntry> {
        val list = mutableListOf<DueCreditEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DueCreditEntry(
                        receiptNo = obj.optString("receiptNo", obj.optString("receipt", "-")),
                        customerName = obj.optString("customerName", "Receipt #${obj.optString("receiptNo", "")}"),
                        amount = obj.optDouble("amount", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseDueCollectionEntries(jsonStr: String): List<DueCollectionEntry> {
        val list = mutableListOf<DueCollectionEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DueCollectionEntry(
                        receiptNo = obj.optString("receiptNo", obj.optString("receipt", "-")),
                        customerName = obj.optString("customerName", "Receipt #${obj.optString("receiptNo", "")}"),
                        amount = obj.optDouble("amount", 0.0),
                        paymentMode = obj.optString("paymentMode", obj.optString("type", "CASH"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseStaffAdvanceEntries(jsonStr: String): List<StaffAdvanceEntry> {
        val list = mutableListOf<StaffAdvanceEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StaffAdvanceEntry(
                        staffName = obj.optString("staffName", obj.optString("name", "Staff")),
                        amount = obj.optDouble("amount", 0.0),
                        paymentMode = obj.optString("paymentMode", obj.optString("type", "CASH"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseWalkoutEntries(jsonStr: String): List<WalkoutEntry> {
        val list = mutableListOf<WalkoutEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WalkoutEntry(
                        tableOrOrderRef = obj.optString("tableOrOrderRef", obj.optString("name", obj.optString("description", "Walkout"))),
                        amount = obj.optDouble("amount", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parsePurchasedItems(jsonStr: String): List<PurchasedItemEntry> {
        val list = mutableListOf<PurchasedItemEntry>()
        try {
            if (jsonStr.isBlank() || jsonStr == "[]") return list
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PurchasedItemEntry(
                        itemName = obj.optString("itemName", obj.optString("name", "Item")),
                        quantity = obj.optDouble("quantity", obj.optDouble("qty", 1.0)),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        totalAmount = obj.optDouble("totalAmount", obj.optDouble("amount", 0.0))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private data class PdfStrings(
        val title: String,
        val officialReport: String,
        val reportId: String,
        val cashier: String,
        val shift: String,
        val date: String,
        val financialBreakdown: String,
        val grossCash: String,
        val madaBank: String,
        val digitalWallet: String,
        val grossTotalSales: String,
        val expenses: String,
        val netCashInDrawer: String,
        val netMadaBank: String,
        val dueSales: String,
        val dueCollection: String,
        val employerAdvances: String,
        val walkoutBills: String,
        val paidOutItems: String,
        val operationalMetrics: String,
        val staffMeals: String,
        val regularMuassel: String,
        val outdoorMuassel: String,
        val receiptNo: String,
        val customer: String,
        val amount: String,
        val mode: String,
        val staffName: String,
        val item: String,
        val qty: String,
        val total: String,
        val notes: String,
        val cashierSign: String,
        val supervisorSign: String,
        val page: String
    )

    
    private fun getPdfStrings(context: Context, language: AppLanguage): PdfStrings {
        val locale = try {
            java.util.Locale.forLanguageTag(language.code)
        } catch (_: Exception) {
            java.util.Locale(language.code)
        }
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)

        return PdfStrings(
            title = localizedContext.getString(com.lojia.pos.R.string.shift_closing_and_revenue_report),
            officialReport = localizedContext.getString(com.lojia.pos.R.string.pdf_official_report),
            reportId = localizedContext.getString(com.lojia.pos.R.string.pdf_report_id),
            cashier = localizedContext.getString(com.lojia.pos.R.string.pdf_cashier),
            shift = localizedContext.getString(com.lojia.pos.R.string.pdf_shift),
            date = localizedContext.getString(com.lojia.pos.R.string.pdf_date),
            financialBreakdown = localizedContext.getString(com.lojia.pos.R.string.pdf_financial_breakdown),
            grossCash = localizedContext.getString(com.lojia.pos.R.string.pdf_gross_cash),
            madaBank = localizedContext.getString(com.lojia.pos.R.string.pdf_mada_bank),
            digitalWallet = localizedContext.getString(com.lojia.pos.R.string.pdf_digital_wallet),
            grossTotalSales = localizedContext.getString(com.lojia.pos.R.string.pdf_gross_total_sales),
            expenses = localizedContext.getString(com.lojia.pos.R.string.pdf_expenses),
            netCashInDrawer = localizedContext.getString(com.lojia.pos.R.string.pdf_net_cash_in_drawer),
            netMadaBank = localizedContext.getString(com.lojia.pos.R.string.pdf_net_mada_bank),
            dueSales = localizedContext.getString(com.lojia.pos.R.string.pdf_due_sales),
            dueCollection = localizedContext.getString(com.lojia.pos.R.string.pdf_due_collection),
            employerAdvances = localizedContext.getString(com.lojia.pos.R.string.pdf_employer_advances),
            walkoutBills = localizedContext.getString(com.lojia.pos.R.string.pdf_walkout_bills),
            paidOutItems = localizedContext.getString(com.lojia.pos.R.string.pdf_paid_out_items),
            operationalMetrics = localizedContext.getString(com.lojia.pos.R.string.pdf_operational_metrics),
            staffMeals = localizedContext.getString(com.lojia.pos.R.string.pdf_staff_meals),
            regularMuassel = localizedContext.getString(com.lojia.pos.R.string.pdf_regular_muassel),
            outdoorMuassel = localizedContext.getString(com.lojia.pos.R.string.pdf_outdoor_muassel),
            receiptNo = localizedContext.getString(com.lojia.pos.R.string.pdf_receipt_no),
            customer = localizedContext.getString(com.lojia.pos.R.string.pdf_customer),
            amount = localizedContext.getString(com.lojia.pos.R.string.pdf_amount),
            mode = localizedContext.getString(com.lojia.pos.R.string.pdf_mode),
            staffName = localizedContext.getString(com.lojia.pos.R.string.pdf_staff_name),
            item = localizedContext.getString(com.lojia.pos.R.string.pdf_item),
            qty = localizedContext.getString(com.lojia.pos.R.string.pdf_qty),
            total = localizedContext.getString(com.lojia.pos.R.string.pdf_total),
            notes = localizedContext.getString(com.lojia.pos.R.string.pdf_notes_1),
            cashierSign = localizedContext.getString(com.lojia.pos.R.string.pdf_cashier_sign_1),
            supervisorSign = localizedContext.getString(com.lojia.pos.R.string.pdf_supervisor_sign),
            page = localizedContext.getString(com.lojia.pos.R.string.pdf_page)
        )
    }
    fun generatePresetLogoBitmap(presetKey: String, size: Int = 200): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = size / 2f
        val padding = size * 0.05f

        val (colorStart, colorEnd, iconSymbol) = when (presetKey) {
            "preset:cafe" -> Triple(Color.rgb(180, 83, 9), Color.rgb(120, 53, 15), "☕")
            "preset:retail" -> Triple(Color.rgb(16, 185, 129), Color.rgb(5, 150, 105), "🛍️")
            "preset:fashion" -> Triple(Color.rgb(236, 72, 153), Color.rgb(190, 24, 93), "💎")
            "preset:pharmacy" -> Triple(Color.rgb(6, 182, 212), Color.rgb(14, 116, 144), "⚕️")
            "preset:tech" -> Triple(Color.rgb(99, 102, 241), Color.rgb(67, 56, 202), "⚡")
            "preset:restaurant" -> Triple(Color.rgb(249, 115, 22), Color.rgb(194, 65, 12), "🍴")
            else -> Triple(Color.rgb(30, 58, 138), Color.rgb(15, 23, 42), "🏪") // superstore
        }

        val shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colorStart, colorEnd, Shader.TileMode.CLAMP)
        paint.shader = shader
        canvas.drawCircle(r, r, r - padding, paint)

        paint.shader = null
        paint.textSize = size * 0.46f
        paint.textAlign = Paint.Align.CENTER
        val textY = r - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(iconSymbol, r, textY, paint)

        return bmp
    }

    fun getCircularBitmap(bitmap: Bitmap, diameter: Int): Bitmap {
        val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val rectF = RectF(0f, 0f, diameter.toFloat(), diameter.toFloat())

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(diameter / 2f, diameter / 2f, diameter / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = if (bitmap.width > bitmap.height) {
            val offset = (bitmap.width - bitmap.height) / 2
            Rect(offset, 0, offset + bitmap.height, bitmap.height)
        } else {
            val offset = (bitmap.height - bitmap.width) / 2
            Rect(0, offset, bitmap.width, offset + bitmap.width)
        }
        canvas.drawBitmap(bitmap, srcRect, rectF, paint)
        return output
    }

    fun loadBusinessLogoBitmap(context: Context, logoUri: String?, targetSize: Int = 200): Bitmap? {
        if (logoUri.isNullOrBlank()) return null
        return try {
            if (logoUri.startsWith("preset:")) {
                generatePresetLogoBitmap(logoUri, targetSize)
            } else if (logoUri.startsWith("content://") || logoUri.startsWith("file://")) {
                val uri = Uri.parse(logoUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = File(logoUri)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun drawBusinessLogoBadge(
        canvas: Canvas,
        context: Context,
        logoUri: String?,
        bizName: String,
        x: Float,
        y: Float,
        size: Float = 54f
    ) {
        val radius = size / 2f
        val centerX = x + radius
        val centerY = y + radius

        val bgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        val bmp = loadBusinessLogoBitmap(context, logoUri, 250)
        if (bmp != null) {
            val innerPadding = 2.5f
            val innerDiameter = ((radius - innerPadding) * 2f).toInt().coerceAtLeast(1)
            val circularBmp = getCircularBitmap(bmp, innerDiameter)
            val drawLeft = centerX - (innerDiameter / 2f)
            val drawTop = centerY - (innerDiameter / 2f)
            canvas.drawBitmap(circularBmp, drawLeft, drawTop, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
        } else {
            val innerCirclePaint = Paint().apply {
                color = Color.rgb(238, 242, 255)
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius - 2.5f, innerCirclePaint)

            val initial = bizName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "L"
            val textPaint = Paint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = size * 0.44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(initial, centerX, textY, textPaint)
        }
    }

    private fun generateStyledQrCodeBitmap(text: String, targetPixelSize: Int = 400): Bitmap? {
        return try {
            val hints = mapOf(
                com.google.zxing.EncodeHintType.MARGIN to 1,
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val bmp = Bitmap.createBitmap(targetPixelSize, targetPixelSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            val moduleSize = targetPixelSize.toFloat() / matrixWidth

            // Charcoal dark slate minimalist color matching modern design guidelines
            val primaryPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            fun isFinderPattern(x: Int, y: Int): Boolean {
                if (x < 7 && y < 7) return true
                if (x >= matrixWidth - 7 && y < 7) return true
                if (x < 7 && y >= matrixHeight - 7) return true
                return false
            }

            val centerX = matrixWidth / 2
            val centerY = matrixHeight / 2
            val logoRadiusModules = 3.5 // Clear center area for logo

            fun isCenterArea(x: Int, y: Int): Boolean {
                val dx = x - centerX
                val dy = y - centerY
                return (dx * dx + dy * dy) <= (logoRadiusModules * logoRadiusModules)
            }

            // 1. Draw organic connected rounded capsule pill modules
            val r = moduleSize * 0.45f
            for (y in 0 until matrixHeight) {
                for (x in 0 until matrixWidth) {
                    if (isFinderPattern(x, y) || isCenterArea(x, y)) continue
                    if (bitMatrix.get(x, y)) {
                        val hasTop = (y > 0 && bitMatrix.get(x, y - 1) && !isFinderPattern(x, y - 1) && !isCenterArea(x, y - 1))
                        val hasBottom = (y < matrixHeight - 1 && bitMatrix.get(x, y + 1) && !isFinderPattern(x, y + 1) && !isCenterArea(x, y + 1))
                        val hasLeft = (x > 0 && bitMatrix.get(x - 1, y) && !isFinderPattern(x - 1, y) && !isCenterArea(x - 1, y))
                        val hasRight = (x < matrixWidth - 1 && bitMatrix.get(x + 1, y) && !isFinderPattern(x + 1, y) && !isCenterArea(x + 1, y))

                        val left = x * moduleSize
                        val top = y * moduleSize
                        val right = left + moduleSize
                        val bottom = top + moduleSize

                        val tl = if (!hasTop && !hasLeft) r else 0f
                        val tr = if (!hasTop && !hasRight) r else 0f
                        val br = if (!hasBottom && !hasRight) r else 0f
                        val bl = if (!hasBottom && !hasLeft) r else 0f

                        val radii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
                        val path = Path()
                        path.addRoundRect(RectF(left, top, right, bottom), radii, Path.Direction.CW)
                        canvas.drawPath(path, primaryPaint)
                    }
                }
            }

            // 2. Draw modern squircle Finder Eye Patterns at 3 corners
            fun drawFinderEye(startX: Int, startY: Int) {
                val left = startX * moduleSize
                val top = startY * moduleSize
                val eyeSize = 7 * moduleSize

                // Outer 7x7 rounded squircle
                val outerRect = RectF(left, top, left + eyeSize, top + eyeSize)
                val outerRadius = eyeSize * 0.32f
                canvas.drawRoundRect(outerRect, outerRadius, outerRadius, primaryPaint)

                // Inner white 5x5 cutout
                val whiteRect = RectF(left + moduleSize, top + moduleSize, left + 6 * moduleSize, top + 6 * moduleSize)
                val whitePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; style = Paint.Style.FILL }
                val whiteRadius = (5 * moduleSize) * 0.28f
                canvas.drawRoundRect(whiteRect, whiteRadius, whiteRadius, whitePaint)

                // Center 3x3 pupil squircle
                val pupilRect = RectF(left + 2 * moduleSize, top + 2 * moduleSize, left + 5 * moduleSize, top + 5 * moduleSize)
                val pupilRadius = (3 * moduleSize) * 0.35f
                canvas.drawRoundRect(pupilRect, pupilRadius, pupilRadius, primaryPaint)
            }

            drawFinderEye(0, 0)
            drawFinderEye(matrixWidth - 7, 0)
            drawFinderEye(0, matrixHeight - 7)

            // 3. Draw central aesthetic logo emblem overlay (matching reference design)
            val centerPxX = targetPixelSize / 2f
            val centerPxY = targetPixelSize / 2f
            val badgeRadius = logoRadiusModules.toFloat() * moduleSize

            // Outer white mask circle
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius, Paint().apply { color = Color.WHITE; isAntiAlias = true })

            // Outer dark ring
            val ringPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = moduleSize * 0.7f
            }
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius - (moduleSize * 0.35f), ringPaint)

            // Inner dark pupil circle / logo emblem core
            canvas.drawCircle(centerPxX, centerPxY, badgeRadius * 0.45f, primaryPaint)

            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildShiftReportQrText(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        currency: String,
        dateFormatter: SimpleDateFormat,
        language: AppLanguage = AppLanguage.fromCode(LanguagePreferences.getLanguage(context))
    ): String {
        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber ?: "310123456700003"
        val pStr = getPdfStrings(context, language)

        val dueCreditEntries = parseDueCreditEntries(report.dueCreditEntriesJson)
        val dueCollectionEntries = parseDueCollectionEntries(report.previousDueCollectionsJson)
        val staffAdvanceEntries = parseStaffAdvanceEntries(report.staffAdvancesJson)
        val walkoutEntries = parseWalkoutEntries(report.unpaidBillsJson)
        val purchasedItems = parsePurchasedItems(report.purchasedItemsJson)

        return buildString {
            appendLine("=== $bizName ===")
            appendLine("VAT ID: $vatNo")
            appendLine("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}")
            appendLine("${pStr.date}: ${dateFormatter.format(Date(report.dateInMillis))}")
            appendLine("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)} | ${pStr.cashier}: ${report.cashierName}")
            appendLine("--------------------------------")
            appendLine("${pStr.grossTotalSales}: %.2f %s".format(report.totalSales, currency))
            appendLine("${pStr.grossCash}: %.2f %s".format(report.grossCash, currency))
            appendLine("${pStr.madaBank}: %.2f %s".format(report.madaPayments, currency))
            appendLine("${pStr.digitalWallet}: %.2f %s".format(report.digitalWallet, currency))
            appendLine("${pStr.expenses}: %.2f %s".format(report.totalExpenses, currency))
            appendLine("${pStr.netCashInDrawer}: %.2f %s".format(report.netCash, currency))
            appendLine("${pStr.netMadaBank}: %.2f %s".format(report.madaPayments, currency))
            if (dueCreditEntries.isNotEmpty()) {
                appendLine("--- ${pStr.dueSales} (${dueCreditEntries.size}) ---")
                dueCreditEntries.forEach { appendLine("Rcpt ${it.receiptNo} (${it.customerName}): %.2f %s".format(it.amount, currency)) }
            }
            if (dueCollectionEntries.isNotEmpty()) {
                appendLine("--- ${pStr.dueCollection} (${dueCollectionEntries.size}) ---")
                dueCollectionEntries.forEach { appendLine("Rcpt ${it.receiptNo} (${it.paymentMode}): %.2f %s".format(it.amount, currency)) }
            }
            if (staffAdvanceEntries.isNotEmpty()) {
                appendLine("--- ${pStr.employerAdvances} (${staffAdvanceEntries.size}) ---")
                staffAdvanceEntries.forEach { appendLine("${it.staffName} (${it.paymentMode}): %.2f %s".format(it.amount, currency)) }
            }
            if (walkoutEntries.isNotEmpty()) {
                appendLine("--- ${pStr.walkoutBills} (${walkoutEntries.size}) ---")
                walkoutEntries.forEach { appendLine("Ref ${it.tableOrOrderRef}: %.2f %s".format(it.amount, currency)) }
            }
            if (purchasedItems.isNotEmpty()) {
                appendLine("--- ${pStr.paidOutItems} (${purchasedItems.size}) ---")
                purchasedItems.forEach { appendLine("${it.itemName} (${it.quantity.toInt()}x): %.2f %s".format(it.totalAmount, currency)) }
            }
            appendLine("--- ${pStr.operationalMetrics} ---")
            appendLine("${pStr.staffMeals}: ${report.staffMealsCount} | ${pStr.regularMuassel}: ${report.muasselQty.toInt()} | ${pStr.outdoorMuassel}: ${report.outdoorShishaQty.toInt()}")
            if (report.notes.isNotBlank()) {
                appendLine("${pStr.notes}: ${report.notes}")
            }
            appendLine("================================")
            appendLine(context.getString(R.string.verified_by_lojia_pos))
        }
    }

    data class ExportResult(
        val isSuccess: Boolean,
        val file: File?,
        val uri: Uri?,
        val displayPath: String,
        val errorMessage: String? = null
    )

    /**
     * Generates a comprehensive Multi-Entry Shift & Sales Summary PDF from database entries.
     */
    fun generateShiftSummaryPdf(
        context: Context,
        reports: List<ShiftReport>,
        businessProfile: BusinessProfile?,
        reportTitle: String = context.getString(R.string.monthly_sales_shift_summary_report),
        language: AppLanguage = AppLanguage.fromCode(LanguagePreferences.getLanguage(context))
    ): ExportResult {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val rawCurrency = businessProfile?.currency ?: "SAR"
        val currency = if (rawCurrency == "SAR") context.getString(R.string.currency_unit) else rawCurrency
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateOnlyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue #1E3A8A
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate #475569
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            isAntiAlias = true
        }
        val textBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            isAntiAlias = true
        }
        val zebraPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }
        val totalBoxPaint = Paint().apply {
            color = Color.rgb(238, 242, 255)
            isAntiAlias = true
        }
        val accentGreenPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val margin = 36f
        var currentY = margin

        // 1. Header Banner
        val headerHeight = 72f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber ?: "310123456700003"
        val phone = businessProfile?.phone ?: "+966 54 123 4567"

        val logoSize = 52f
        val logoX = PAGE_WIDTH - margin - logoSize - 12f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        canvas.drawText(bizName, margin + 14f, currentY + 24f, headerTextPaint)
        canvas.drawText(context.getString(R.string.pdf_vat_tel, vatNo, phone), margin + 14f, currentY + 40f, subheaderTextPaint)
        canvas.drawText(context.getString(R.string.pdf_address, businessProfile?.address ?: context.getString(R.string.pdf_default_address)), margin + 14f, currentY + 54f, subheaderTextPaint)

        // Right side badge (placed to the left of the logo)
        val dateGen = context.getString(R.string.pdf_generated_at, dateFormatter.format(Date()))
        val textWidth = subheaderTextPaint.measureText(dateGen)
        canvas.drawText(dateGen, logoX - textWidth - 12f, currentY + 24f, subheaderTextPaint)
        val entriesCount = context.getString(R.string.pdf_total_records, reports.size)
        val entriesWidth = subheaderTextPaint.measureText(entriesCount)
        canvas.drawText(entriesCount, logoX - entriesWidth - 12f, currentY + 40f, subheaderTextPaint)

        currentY += headerHeight + 20f

        // 2. Report Subheading
        canvas.drawText(reportTitle, margin, currentY, titlePaint)
        currentY += 16f

        // 3. KPI Highlights Cards (Row of 3 metric boxes)
        val totalGrossRevenue = reports.sumOf { it.totalSales }
        val totalGrossCash = reports.sumOf { it.grossCash }
        val totalMada = reports.sumOf { it.madaPayments }
        val totalWallet = reports.sumOf { it.digitalWallet }
        val totalExp = reports.sumOf { it.totalExpenses }
        val totalNet = reports.sumOf { it.netCash }

        val cardWidth = (PAGE_WIDTH - (margin * 2) - 20f) / 3f
        val cardHeight = 44f

        // Card 1: Total Sales
        drawKpiCard(canvas, margin, currentY, cardWidth, cardHeight, context.getString(R.string.total_revenue_label), "%.2f %s".format(totalGrossRevenue, currency), Color.rgb(16, 185, 129))
        // Card 2: Total Mada / Digital
        drawKpiCard(canvas, margin + cardWidth + 10f, currentY, cardWidth, cardHeight, context.getString(R.string.mada_cards_label), "%.2f %s".format(totalMada + totalWallet, currency), Color.rgb(99, 102, 241))
        // Card 3: Net Cash in Drawer
        drawKpiCard(canvas, margin + (cardWidth * 2) + 20f, currentY, cardWidth, cardHeight, context.getString(R.string.net_cash_in_drawer_label), "%.2f %s".format(totalNet, currency), Color.rgb(30, 58, 138))

        currentY += cardHeight + 22f

        // 4. Tabular Shift Entries Breakdown
        canvas.drawText(context.getString(R.string.detailed_shift_records), margin, currentY, textBoldPaint)
        currentY += 8f

        // Table Header
        val colDate = margin + 8f
        val colCashier = margin + 85f
        val colShift = margin + 190f
        val colCash = margin + 250f
        val colMada = margin + 315f
        val colExp = margin + 380f
        val colTotal = margin + 445f

        val tableHeaderRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 20f)
        canvas.drawRect(tableHeaderRect, tableHeaderPaint)

        val headerY = currentY + 14f
        canvas.drawText(context.getString(R.string.date_2), colDate, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.cashier_6), colCashier, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.shift_2), colShift, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.cash_1), colCash, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.madacard), colMada, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.expenses), colExp, headerY, textBoldPaint)
        canvas.drawText(context.getString(R.string.pdf_total_currency, currency), colTotal, headerY, textBoldPaint)

        currentY += 20f

        // Table Rows (Draw up to 18 rows to fit gracefully on standard page)
        val maxRows = 18
        val rowsToDraw = reports.take(maxRows)

        rowsToDraw.forEachIndexed { index, report ->
            val rowY = currentY + 14f
            if (index % 2 == 1) {
                canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f), zebraPaint)
            }

            canvas.drawText(dateOnlyFormatter.format(Date(report.dateInMillis)), colDate, rowY, textPaint)
            val truncatedCashier = if (report.cashierName.length > 15) report.cashierName.take(13) + ".." else report.cashierName
            canvas.drawText(truncatedCashier, colCashier, rowY, textPaint)
            canvas.drawText(getLocalizedShiftName(language, report.shift), colShift, rowY, textPaint)
            canvas.drawText(context.getString(R.string.msg_0f_5).format(report.grossCash), colCash, rowY, textPaint)
            canvas.drawText(context.getString(R.string.msg_0f_5).format(report.madaPayments + report.digitalWallet), colMada, rowY, textPaint)
            canvas.drawText(context.getString(R.string.msg_0f_5).format(report.totalExpenses), colExp, rowY, textPaint)
            canvas.drawText(context.getString(R.string.msg_2f_2).format(report.totalSales), colTotal, rowY, textBoldPaint)

            canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)
            currentY += 18f
        }

        if (reports.size > maxRows) {
            canvas.drawText(context.getString(R.string.pdf_more_entries, reports.size - maxRows), margin + 8f, currentY + 12f, secondaryPaint)
            currentY += 16f
        }

        // Totals Footer Row
        val totalRect = RectF(margin, currentY + 4f, PAGE_WIDTH - margin, currentY + 26f)
        canvas.drawRoundRect(totalRect, 4f, 4f, totalBoxPaint)

        val totalY = currentY + 19f
        canvas.drawText(context.getString(R.string.grand_totals), colDate, totalY, textBoldPaint)
        canvas.drawText(context.getString(R.string.msg_0f_5).format(totalGrossCash), colCash, totalY, textBoldPaint)
        canvas.drawText(context.getString(R.string.msg_0f_5).format(totalMada + totalWallet), colMada, totalY, textBoldPaint)
        canvas.drawText(context.getString(R.string.msg_0f_5).format(totalExp), colExp, totalY, textBoldPaint)
        canvas.drawText(context.getString(R.string.msg_2f_s_21).format(totalGrossRevenue, currency), colTotal, totalY, accentGreenPaint)

        currentY += 40f

        // 5. Operations & Breakdown Notes
        val totalStaffMeals = reports.sumOf { it.staffMealsCount }
        val totalMuassel = reports.sumOf { it.muasselQty }

        val opsBoxRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 36f)
        canvas.drawRoundRect(opsBoxRect, 6f, 6f, tableHeaderPaint)
        canvas.drawText(context.getString(R.string.pdf_operational_metrics, totalStaffMeals, totalMuassel), margin + 12f, currentY + 16f, textBoldPaint)
        canvas.drawText(context.getString(R.string.pdf_payment_split, (totalGrossCash / totalGrossRevenue.coerceAtLeast(1.0) * 100).toInt(), (totalMada / totalGrossRevenue.coerceAtLeast(1.0) * 100).toInt(), (totalWallet / totalGrossRevenue.coerceAtLeast(1.0) * 100).toInt()), margin + 12f, currentY + 28f, secondaryPaint)

        // 6. Signatures & Official Seal
        val signY = PAGE_HEIGHT - margin - 50f
        canvas.drawLine(margin + 20f, signY + 25f, margin + 180f, signY + 25f, linePaint)
        canvas.drawText(context.getString(R.string.manager_supervisor_signature), margin + 20f, signY + 38f, secondaryPaint)

        canvas.drawLine(PAGE_WIDTH - margin - 180f, signY + 25f, PAGE_WIDTH - margin - 20f, signY + 25f, linePaint)
        canvas.drawText(context.getString(R.string.finance_auditor_official_seal), PAGE_WIDTH - margin - 180f, signY + 38f, secondaryPaint)

        // 7. Page Footer
        val footerY = PAGE_HEIGHT - margin + 10f
        canvas.drawText(context.getString(R.string.lojia_pos_system_v10), margin, footerY, secondaryPaint)

        pdfDocument.finishPage(page)

        // Save to Storage
        val fileName = "Shift_Summary_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        return savePdfToStorage(context, pdfDocument, fileName)
    }

    /**
     * Generates an individual Shift Report PDF document with multi-entry breakdown tables,
     * multi-language support, and multi-page pagination.
     */
    fun generateSingleShiftReportPdf(
        context: Context,
        report: ShiftReport,
        businessProfile: BusinessProfile?,
        language: AppLanguage = AppLanguage.ENGLISH
    ): ExportResult {
        val pStr = getPdfStrings(context, language)
        val rawCurrency = businessProfile?.currency ?: "SAR"
        val currency = if (rawCurrency == "SAR") context.getString(R.string.currency_unit) else rawCurrency
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val dueCreditEntries = parseDueCreditEntries(report.dueCreditEntriesJson)
        val dueCollectionEntries = parseDueCollectionEntries(report.previousDueCollectionsJson)
        val staffAdvanceEntries = parseStaffAdvanceEntries(report.staffAdvancesJson)
        val walkoutEntries = parseWalkoutEntries(report.unpaidBillsJson)
        val purchasedItems = parsePurchasedItems(report.purchasedItemsJson)

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }
        val textBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val boxBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }
        val emeraldPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            isAntiAlias = true
        }
        val zebraPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }

        val margin = 36f
        var currentY = margin

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - 35f) {
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                currentY = margin

                // Draw Top Header on new page
                val miniBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 24f)
                canvas.drawRoundRect(miniBox, 4f, 4f, primaryPaint)
                canvas.drawText("${businessProfile?.businessName ?: context.getString(R.string.default_business_name)} — ${pStr.title} (${pStr.page} $pageNum)", margin + 10f, currentY + 16f, headerTextPaint)
                currentY += 34f
            }
        }

        // 1. Header Banner
        val headerHeight = 75f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber ?: "310123456700003"
        val phone = businessProfile?.phone ?: "+966 50 123 4567"

        val logoSize = 54f
        val logoX = PAGE_WIDTH - margin - logoSize - 14f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        canvas.drawText(bizName, margin + 16f, currentY + 26f, headerTextPaint)
        canvas.drawText(context.getString(R.string.pdf_vat_reg_no_tel, vatNo, phone), margin + 16f, currentY + 44f, subheaderTextPaint)
        canvas.drawText(context.getString(R.string.pdf_location, businessProfile?.address ?: context.getString(R.string.pdf_default_location)), margin + 16f, currentY + 60f, subheaderTextPaint)

        val badgeText = pStr.officialReport
        val badgeWidth = subheaderTextPaint.measureText(badgeText)
        canvas.drawText(badgeText, logoX - badgeWidth - 12f, currentY + 26f, subheaderTextPaint)

        currentY += headerHeight + 20f

        // 2. Title & Shift Details
        canvas.drawText(pStr.title, margin, currentY, titlePaint)
        currentY += 14f

        val infoBoxHeight = 86f
        val infoBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + infoBoxHeight)
        canvas.drawRoundRect(infoBox, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(infoBox, 8f, 8f, Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f })

        // Left shift metadata
        canvas.drawText("${pStr.reportId}: #${report.id.toString().padStart(6, '0')}", margin + 14f, currentY + 22f, textBoldPaint)
        canvas.drawText("${pStr.cashier}: ${report.cashierName}", margin + 14f, currentY + 44f, textBoldPaint)
        canvas.drawText("${pStr.shift}: ${getLocalizedShiftName(language, report.shift)}", margin + 14f, currentY + 66f, textBoldPaint)

        canvas.drawText("${pStr.date}: ${dateFormatter.format(Date(report.dateInMillis))}", margin + 175f, currentY + 22f, textPaint)
        val certText = context.getString(R.string.official_digital_verification)
        canvas.drawText(certText, margin + 175f, currentY + 44f, secondaryPaint)

        // Draw dedicated QR Code Card container on the right side of the info box
        val qrCardSize = 76f
        val qrCardRight = PAGE_WIDTH - margin - 6f
        val qrCardLeft = qrCardRight - qrCardSize
        val qrCardTop = currentY + (infoBoxHeight - qrCardSize) / 2f
        val qrCardBottom = qrCardTop + qrCardSize

        // White card background with sleek rounded corners & border for the QR code
        val qrCardBg = RectF(qrCardLeft, qrCardTop, qrCardRight, qrCardBottom)
        canvas.drawRoundRect(qrCardBg, 6f, 6f, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
        canvas.drawRoundRect(qrCardBg, 6f, 6f, Paint().apply { color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 1f })

        // Generate and draw the modern styled QR Code
        val qrText = buildShiftReportQrText(context, report, businessProfile, currency, dateFormatter)
        val qrBitmap = generateStyledQrCodeBitmap(qrText, 350)
        if (qrBitmap != null) {
            val qrPadding = 5f
            val qrImageSize = qrCardSize - (qrPadding * 2f) - 8f // space for caption at bottom
            val qrImageLeft = qrCardLeft + (qrCardSize - qrImageSize) / 2f
            val qrImageTop = qrCardTop + qrPadding

            val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val dstRectF = RectF(qrImageLeft, qrImageTop, qrImageLeft + qrImageSize, qrImageTop + qrImageSize)
            canvas.drawBitmap(qrBitmap, srcRect, dstRectF, null)

            // Modern micro badge label below QR Code
            val captionPaint = Paint().apply {
                color = Color.rgb(30, 58, 138)
                textSize = 5.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val scanLabel = context.getString(R.string.scan_data_label)
            val scanWidth = captionPaint.measureText(scanLabel)
            canvas.drawText(scanLabel, qrCardLeft + (qrCardSize - scanWidth) / 2f, qrCardBottom - 3f, captionPaint)
        }

        currentY += infoBoxHeight + 16f

        // 3. Financial Revenue Breakdown
        canvas.drawText(pStr.financialBreakdown, margin, currentY, textBoldPaint)
        currentY += 10f

        fun drawFinRow(label: String, valueStr: String, isBold: Boolean = false, isHighlight: Boolean = false) {
            ensureSpace(22f)
            val rowRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 20f)
            if (isBold) {
                canvas.drawRect(rowRect, tableHeaderPaint)
            }
            canvas.drawLine(margin, currentY + 20f, PAGE_WIDTH - margin, currentY + 20f, linePaint)

            val p = if (isBold) textBoldPaint else textPaint
            val vp = if (isHighlight) emeraldPaint else p

            canvas.drawText(label, margin + 12f, currentY + 14f, p)
            val vWidth = vp.measureText(valueStr)
            canvas.drawText(valueStr, PAGE_WIDTH - margin - vWidth - 12f, currentY + 14f, vp)
            currentY += 20f
        }

        drawFinRow(pStr.grossCash, "%.2f %s".format(report.grossCash, currency))
        drawFinRow(pStr.madaBank, "%.2f %s".format(report.madaPayments, currency))
        drawFinRow(pStr.digitalWallet, "%.2f %s".format(report.digitalWallet, currency))
        drawFinRow(pStr.grossTotalSales, "%.2f %s".format(report.totalSales, currency), isBold = true, isHighlight = true)
        drawFinRow(pStr.expenses, "%.2f %s".format(report.totalExpenses, currency))
        drawFinRow(pStr.netCashInDrawer, "%.2f %s".format(report.netCash, currency), isBold = true)
        drawFinRow(pStr.netMadaBank, "%.2f %s".format(report.madaPayments, currency), isBold = true)

        currentY += 16f

        // Helper to draw custom breakdown tables
        fun drawSectionTable(title: String, headers: List<String>, rows: List<List<String>>, totalAmountStr: String) {
            if (rows.isEmpty()) return
            ensureSpace(40f + (rows.size * 20f))

            canvas.drawText(title, margin, currentY, textBoldPaint)
            currentY += 8f

            // Header Row
            val tableH = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f)
            canvas.drawRect(tableH, primaryPaint)

            val col1X = margin + 10f
            val col2X = margin + 180f
            val col3X = PAGE_WIDTH - margin - 120f

            val headerP = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
            canvas.drawText(headers[0], col1X, currentY + 13f, headerP)
            if (headers.size > 1) canvas.drawText(headers[1], col2X, currentY + 13f, headerP)
            if (headers.size > 2) canvas.drawText(headers[2], col3X, currentY + 13f, headerP)

            currentY += 18f

            // Data Rows
            rows.forEachIndexed { idx, row ->
                ensureSpace(20f)
                if (idx % 2 == 1) {
                    canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 18f), zebraPaint)
                }
                canvas.drawText(row[0], col1X, currentY + 13f, textPaint)
                if (row.size > 1) canvas.drawText(row[1], col2X, currentY + 13f, textPaint)
                if (row.size > 2) {
                    val w = textBoldPaint.measureText(row[2])
                    canvas.drawText(row[2], PAGE_WIDTH - margin - w - 10f, currentY + 13f, textBoldPaint)
                }
                canvas.drawLine(margin, currentY + 18f, PAGE_WIDTH - margin, currentY + 18f, linePaint)
                currentY += 18f
            }

            // Subtotal row
            ensureSpace(20f)
            canvas.drawRect(RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 20f), tableHeaderPaint)
            canvas.drawText("${pStr.total}:", col1X, currentY + 14f, textBoldPaint)
            val totW = emeraldPaint.measureText(totalAmountStr)
            canvas.drawText(totalAmountStr, PAGE_WIDTH - margin - totW - 10f, currentY + 14f, emeraldPaint)
            canvas.drawLine(margin, currentY + 20f, PAGE_WIDTH - margin, currentY + 20f, linePaint)
            currentY += 26f
        }

        // Table 1: Due Sales (Credit Entries)
        drawSectionTable(
            title = pStr.dueSales,
            headers = listOf(pStr.receiptNo, pStr.customer, pStr.amount),
            rows = dueCreditEntries.map { listOf(it.receiptNo, it.customerName, "%.2f %s".format(it.amount, currency)) },
            totalAmountStr = "%.2f %s".format(dueCreditEntries.sumOf { it.amount }, currency)
        )

        // Table 2: Due Collection
        drawSectionTable(
            title = pStr.dueCollection,
            headers = listOf(pStr.receiptNo, pStr.mode, pStr.amount),
            rows = dueCollectionEntries.map { listOf(it.receiptNo, it.paymentMode, "%.2f %s".format(it.amount, currency)) },
            totalAmountStr = "%.2f %s".format(dueCollectionEntries.sumOf { it.amount }, currency)
        )

        // Table 3: Employer Advances
        drawSectionTable(
            title = pStr.employerAdvances,
            headers = listOf(pStr.staffName, pStr.mode, pStr.amount),
            rows = staffAdvanceEntries.map { listOf(it.staffName, it.paymentMode, "%.2f %s".format(it.amount, currency)) },
            totalAmountStr = "%.2f %s".format(staffAdvanceEntries.sumOf { it.amount }, currency)
        )

        // Table 4: Walkout Bills
        drawSectionTable(
            title = pStr.walkoutBills,
            headers = listOf(pStr.customer, pStr.amount),
            rows = walkoutEntries.map { listOf(it.tableOrOrderRef, "", "%.2f %s".format(it.amount, currency)) },
            totalAmountStr = "%.2f %s".format(walkoutEntries.sumOf { it.amount }, currency)
        )

        // Table 5: Paid Out Items / Purchases
        drawSectionTable(
            title = pStr.paidOutItems,
            headers = listOf(pStr.item, pStr.qty, pStr.total),
            rows = purchasedItems.map { listOf(it.itemName, "${it.quantity.toInt()}x", "%.2f %s".format(it.totalAmount, currency)) },
            totalAmountStr = "%.2f %s".format(purchasedItems.sumOf { it.totalAmount }, currency)
        )

        // Operational Metrics Section
        ensureSpace(50f)
        canvas.drawText(pStr.operationalMetrics, margin, currentY, textBoldPaint)
        currentY += 8f

        val opsBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 40f)
        canvas.drawRoundRect(opsBox, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(opsBox, 6f, 6f, Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText("${pStr.staffMeals}: ${report.staffMealsCount}", margin + 12f, currentY + 16f, textPaint)
        canvas.drawText("${pStr.regularMuassel}: ${report.muasselQty.toInt()}", margin + 180f, currentY + 16f, textPaint)
        canvas.drawText("${pStr.outdoorMuassel}: ${report.outdoorShishaQty.toInt()}", margin + 350f, currentY + 16f, textPaint)

        if (report.notes.isNotBlank()) {
            canvas.drawText("${pStr.notes}: ${report.notes}", margin + 12f, currentY + 32f, secondaryPaint)
        }
        currentY += 54f

        // Signatures Section
        ensureSpace(70f)
        val signY = currentY + 30f
        canvas.drawLine(margin + 20f, signY, margin + 200f, signY, linePaint)
        canvas.drawText("${pStr.cashierSign}: ${report.cashierName}", margin + 20f, signY + 16f, textBoldPaint)

        canvas.drawLine(PAGE_WIDTH - margin - 200f, signY, PAGE_WIDTH - margin - 20f, signY, linePaint)
        canvas.drawText(pStr.supervisorSign, PAGE_WIDTH - margin - 200f, signY + 16f, textBoldPaint)

        pdfDocument.finishPage(page)

        val fileName = "Shift_Report_${report.cashierName.replace(" ", "_")}_${report.shift}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        return savePdfToStorage(context, pdfDocument, fileName)
    }

    /**
     * Generates a POS Sale Tax Invoice PDF.
     */
    fun generateSaleInvoicePdf(
        context: Context,
        sale: POSSale,
        businessProfile: BusinessProfile?
    ): ExportResult {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val rawCurrency = businessProfile?.currency ?: "SAR"
        val currency = if (rawCurrency == "SAR") context.getString(R.string.currency_unit) else rawCurrency
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val primaryPaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            isAntiAlias = true
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subheaderTextPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            isAntiAlias = true
        }
        val textBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            isAntiAlias = true
        }
        val emeraldPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val secondaryPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            isAntiAlias = true
        }

        val margin = 40f
        var currentY = margin

        // Header
        val headerHeight = 70f
        val headerRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + headerHeight)
        canvas.drawRoundRect(headerRect, 8f, 8f, primaryPaint)

        val bizName = businessProfile?.businessName ?: context.getString(R.string.default_business_name)
        val vatNo = businessProfile?.vatNumber ?: "310123456700003"

        val logoSize = 50f
        val logoX = PAGE_WIDTH - margin - logoSize - 14f
        val logoY = currentY + (headerHeight - logoSize) / 2f
        drawBusinessLogoBadge(canvas, context, businessProfile?.logoUri, bizName, logoX, logoY, logoSize)

        canvas.drawText(bizName, margin + 16f, currentY + 26f, headerTextPaint)
        canvas.drawText(context.getString(R.string.pdf_vat_id_tel, vatNo, businessProfile?.phone ?: context.getString(R.string.pdf_default_phone)), margin + 16f, currentY + 44f, subheaderTextPaint)
        canvas.drawText(businessProfile?.address ?: context.getString(R.string.pdf_default_location), margin + 16f, currentY + 58f, subheaderTextPaint)

        val invoiceTag = context.getString(R.string.simplified_tax_invoice)
        val tagWidth = subheaderTextPaint.measureText(invoiceTag)
        canvas.drawText(invoiceTag, logoX - tagWidth - 12f, currentY + 26f, subheaderTextPaint)

        currentY += headerHeight + 24f

        // Invoice Meta
        canvas.drawText(context.getString(R.string.tax_invoice_details), margin, currentY, titlePaint)
        currentY += 14f

        val infoBox = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 48f)
        canvas.drawRoundRect(infoBox, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawRoundRect(infoBox, 6f, 6f, Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText(context.getString(R.string.pdf_invoice_number, sale.invoiceNumber), margin + 12f, currentY + 18f, textBoldPaint)
        canvas.drawText(context.getString(R.string.pdf_cashier_name, sale.cashierName), margin + 12f, currentY + 36f, textPaint)
        canvas.drawText(context.getString(R.string.pdf_date_val, dateFormatter.format(Date(sale.timestamp))), margin + 260f, currentY + 18f, textPaint)
        canvas.drawText(context.getString(R.string.pdf_payment_method, sale.paymentMethod), margin + 260f, currentY + 36f, textBoldPaint)

        currentY += 64f

        // Amount Table
        fun drawAmountRow(label: String, amount: Double, isTotal: Boolean = false) {
            val rowY = currentY + 16f
            canvas.drawLine(margin, currentY + 24f, PAGE_WIDTH - margin, currentY + 24f, linePaint)
            val paint = if (isTotal) textBoldPaint else textPaint
            val valPaint = if (isTotal) emeraldPaint else (if (isTotal) textBoldPaint else textPaint)

            canvas.drawText(label, margin + 12f, rowY, paint)
            val value = "%.2f %s".format(amount, currency)
            val valWidth = valPaint.measureText(value)
            canvas.drawText(value, PAGE_WIDTH - margin - valWidth - 12f, rowY, valPaint)
            currentY += 24f
        }

        drawAmountRow(context.getString(R.string.subtotal_exclusive_vat), sale.subtotal)
        drawAmountRow(context.getString(R.string.vat_amount_15), sale.vatAmount)
        drawAmountRow(context.getString(R.string.total_amount_due_inc_vat), sale.totalAmount, isTotal = true)

        currentY += 40f

        // Thank you note
        val noteY = currentY + 20f
        canvas.drawText(context.getString(R.string.pdf_thank_you_visit, businessProfile?.businessName ?: context.getString(R.string.pdf_default_business)), margin + 12f, noteY, textBoldPaint)
        canvas.drawText(context.getString(R.string.this_is_an_official), margin + 12f, noteY + 16f, secondaryPaint)

        pdfDocument.finishPage(page)

        val fileName = "Invoice_${sale.invoiceNumber}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        return savePdfToStorage(context, pdfDocument, fileName)
    }

    /**
     * Saves the generated PdfDocument to device storage (Downloads / Documents) and returns export details.
     */
    private fun savePdfToStorage(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): ExportResult {
        var outputStream: OutputStream? = null
        var targetFile: File? = null
        var fileUri: Uri? = null
        var displayPath = fileName

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LojiaReports")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    fileUri = uri
                    outputStream = resolver.openOutputStream(uri)
                    displayPath = "Downloads/LojiaReports/$fileName"
                }
            }

            // Fallback for older devices or if MediaStore insert returned null
            if (outputStream == null) {
                val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
                    ?: context.filesDir
                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }
                targetFile = File(docsDir, fileName)
                outputStream = FileOutputStream(targetFile)
                displayPath = targetFile.absolutePath
                fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
            }

            outputStream?.let {
                pdfDocument.writeTo(it)
                it.flush()
            }
            pdfDocument.close()

            return ExportResult(
                isSuccess = true,
                file = targetFile,
                uri = fileUri,
                displayPath = displayPath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
            return ExportResult(
                isSuccess = false,
                file = null,
                uri = null,
                displayPath = "",
                errorMessage = e.localizedMessage ?: "Failed to write PDF to storage"
            )
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        accentColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)
        val bgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = accentColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        canvas.drawText(title, x + 8f, y + 16f, titlePaint)
        canvas.drawText(value, x + 8f, y + 33f, valuePaint)
    }

    /**
     * Opens the exported PDF with the system viewer or any installed PDF application.
     */
    fun openPdfFile(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.no_pdf_viewer_found), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the exported PDF file via Intent chooser.
     */
    fun sharePdfFile(context: Context, uri: Uri, title: String = "Shift Sales Report PDF") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.could_not_share_pdf, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
    }
}
