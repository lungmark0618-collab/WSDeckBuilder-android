package com.mark.wsdeck.data

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withRotation
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.min

/**
 * 把牌組畫成一張圖分享，圖上帶 QR 讓對方（同一支 App，iOS 或 Android 都行）
 * 掃回牌組。用 Canvas 直接畫而不是拍 Compose 截圖——靜態版面用 Canvas 更好控制
 * 版面計算，也不必為了離屏渲染 Composable 另外處理生命週期。
 *
 * 對應 iOS 的 DeckImageExporter。QR 載荷格式兩邊共用同一份，
 * 這是唯二讓兩個平台真正互通的介面之一。
 */
object DeckImageExporter {

    /**
     * 圖片裡帶的牌組資料。純文字而非 JSON：QR 容量有限，
     * 而且掃出來的內容人眼可讀，出事時看得出哪裡壞掉。
     *
     *     WSD1|牌組名|BD/W54-001:4;BD/W54-002:3
     */
    object Payload {
        const val PREFIX = "WSD1"
        /** 包成 URL 而不是丟純文字進 QR：系統相機掃到純文字只會顯示文字，
         *  掃到看得懂的網址才會跳「用『WS 牌組管理器』開啟」，朋友不用特地
         *  開這個 App 的掃描功能，直接用內建相機掃就能跳轉預覽匯入。
         *  scheme 要跟 AndroidManifest 的 intent-filter 對得上。 */
        const val URL_SCHEME = "wsdeck"
        const val URL_HOST = "import"

        fun encode(deckName: String, entries: List<DeckEntryEntity>): String {
            val raw = encodeRaw(deckName, entries)
            return "$URL_SCHEME://$URL_HOST?d=${java.net.URLEncoder.encode(raw, "UTF-8")}"
        }

        private fun encodeRaw(deckName: String, entries: List<DeckEntryEntity>): String {
            val body = entries
                .sortedBy { it.printingId }
                .joinToString(";") { "${it.printingId}:${it.count}" }
            // 牌組名可能含分隔字元，換掉以免解析時被切斷
            val safeName = deckName.replace("|", "／").replace("\n", " ")
            return "$PREFIX|$safeName|$body"
        }

        data class Parsed(val name: String, val entries: List<Pair<String, Int>>)

        /** 回傳 null 表示不是本 App 的載荷。吃兩種格式：新的
         *  wsdeck://import?d=... 網址，跟舊版直接掃圖片時可能還留著的純文字
         *  格式（在 App 內用相簿選圖那條路還是會遇到）。
         *
         *  故意不用 android.net.Uri 解析——那是 Android 框架 API，純 JVM 的
         *  單元測試（testDebugUnitTest，沒套 Robolectric）呼叫它只會拿到
         *  stub 回傳的空殼，讓這裡的判斷永遠走不進 if 分支、整組測試靜靜
         *  失敗。這裡的網址格式固定是自己 encode() 出來的，手動字串處理
         *  就夠可靠，也才能在純 JVM 環境被測到。 */
        fun decode(text: String): Parsed? {
            val prefix = "$URL_SCHEME://$URL_HOST?"
            if (text.startsWith(prefix)) {
                val raw = text.removePrefix(prefix)
                    .split("&")
                    .map { it.split("=", limit = 2) }
                    .firstOrNull { it.size == 2 && it[0] == "d" }
                    ?.get(1)
                    ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() }
                    ?: return null
                return decodeRaw(raw)
            }
            return decodeRaw(text)
        }

        private fun decodeRaw(text: String): Parsed? {
            val parts = text.split("|", limit = 3)
            if (parts.size != 3 || parts[0] != PREFIX) return null
            val entries = parts[2].split(";").mapNotNull { chunk ->
                val kv = chunk.split(":")
                if (kv.size != 2) return@mapNotNull null
                val count = kv[1].toIntOrNull() ?: return@mapNotNull null
                kv[0] to count
            }
            return if (entries.isEmpty()) null else Parsed(parts[1], entries)
        }
    }

    /** 錯誤更正用 M：容得下這種長度的載荷，被裁到或反光時也還讀得回來 */
    fun qrBitmap(text: String, sizePx: Int): Bitmap? = try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        createBitmap(sizePx, sizePx).apply {
            for (x in 0 until sizePx) for (y in 0 until sizePx) {
                setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    } catch (e: Exception) {
        null
    }

    // ── 版面常數（像素，非 dp——這是輸出圖檔，不是螢幕畫面）───────
    private const val CARD_W = 200f
    private const val CARD_H = CARD_W * 88f / 63f  // WS 卡片比例 63:88
    private const val GAP = 18f
    private const val PADDING = 48f
    private const val QR_SIZE = 260

    /**
     * 出圖只用已快取的卡圖，不在這裡連網下載——牌店現場網路差時
     * 至少還出得了圖，缺圖的位置畫卡名佔位（與 iOS 同樣的考量）。
     */
    suspend fun render(
        context: Context,
        deckName: String,
        entries: List<DeckEntryEntity>,
        items: List<CardCount>,
        imageLoader: ImageLoader,
    ): File? {
        if (items.isEmpty()) return null
        val columns = min(8, maxOf(4, items.size))
        val rows = ceil(items.size / columns.toDouble()).toInt()

        val sheetW = PADDING * 2 + columns * CARD_W + (columns - 1) * GAP
        val headerH = 70f
        val idTextH = 32f
        val tileH = CARD_H + 6f + idTextH
        val footerH = QR_SIZE + 24f
        val sheetH = PADDING * 2 + headerH + GAP + rows * tileH + (rows - 1) * GAP + GAP + footerH

        val bitmap = createBitmap(sheetW.toInt(), sheetH.toInt())
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        drawHeader(canvas, deckName, entries.sumOf { it.count })

        val artCache = mutableMapOf<String, Bitmap?>()
        items.forEachIndexed { index, item ->
            val col = index % columns
            val row = index / columns
            val x = PADDING + col * (CARD_W + GAP)
            val y = PADDING + headerH + GAP + row * (tileH + GAP)
            val printing = displayPrinting(item, entries)
            val art = artCache.getOrPut(printing.id) { loadArt(context, imageLoader, printing.imageURL) }
            drawTile(canvas, x, y, item, printing, art)
        }

        drawFooter(canvas, sheetH - PADDING - footerH, sheetW, deckName, entries)

        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val safeName = deckName.filterNot { it in "/\\:?%*|\"<>" }.ifEmpty { "deck" }
        val file = File(dir, "$safeName.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** 牌組中實際放的刷版優先，沒有才退回普卡 */
    private fun displayPrinting(item: CardCount, entries: List<DeckEntryEntity>): Printing {
        val playedId = entries.firstOrNull { e ->
            item.card.printings.any { it.id == e.printingId } && e.count > 0
        }?.printingId
        return item.card.printings.firstOrNull { it.id == playedId } ?: item.card.defaultPrinting
    }

    private suspend fun loadArt(context: Context, loader: ImageLoader, url: String): Bitmap? = try {
        // allowHardware(false) 是必要的：Coil 預設在 API 26+ 解成硬體 bitmap，
        // 畫到這裡用來出圖的軟體 Canvas 上一定會丟
        // IllegalArgumentException("Software rendering doesn't support hardware
        // bitmaps")——不是零星狀況，只要圖真的載到就一定炸，實機也一樣會中。
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
        val result = loader.execute(request)
        (result as? coil3.request.SuccessResult)?.image?.toBitmap()
    } catch (e: Exception) {
        null
    }

    private fun drawHeader(canvas: Canvas, deckName: String, total: Int) {
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 44f; isFakeBoldText = true
        }
        val countPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; textSize = 30f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(deckName, PADDING, PADDING + 44f, namePaint)
        canvas.drawText("$total 張", canvas.width - PADDING, PADDING + 40f, countPaint)
    }

    /** CX 是橫向卡；轉 90° 填滿同一格，尺寸就跟其他卡一致（與 iOS 同樣的處理） */
    private fun drawTile(canvas: Canvas, x: Float, y: Float, item: CardCount,
                         printing: Printing, art: Bitmap?) {
        val rect = RectF(x, y, x + CARD_W, y + CARD_H)
        val clipPath = Path().apply { addRoundRect(rect, 10f, 10f, Path.Direction.CW) }
        canvas.withClip(clipPath) {
            if (art != null) {
                val isClimax = item.card.cardType == CardType.CLIMAX
                if (isClimax) {
                    canvas.withRotation(90f, x + CARD_W / 2, y + CARD_H / 2) {
                        // 旋轉後這個框變成「橫的」，寬高互換，藝術圖才會擺正
                        val rotW = CARD_H
                        val rotH = CARD_W
                        val rx = x + CARD_W / 2 - rotW / 2
                        val ry = y + CARD_H / 2 - rotH / 2
                        drawBitmapFilled(canvas, art, RectF(rx, ry, rx + rotW, ry + rotH))
                    }
                } else {
                    drawBitmapFilled(canvas, art, rect)
                }
            } else {
                val bg = Paint().apply { color = Color.rgb(230, 230, 230) }
                canvas.drawRect(rect, bg)
                val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY; textSize = 20f; textAlign = Paint.Align.CENTER
                }
                canvas.drawText(item.card.nameZH.take(10), rect.centerX(), rect.centerY(), namePaint)
            }
        }

        // 張數徽章：黑底白字，貼在格子右下角
        val badgeText = "×${item.count}"
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 26f; isFakeBoldText = true
        }
        val badgeW = badgePaint.measureText(badgeText) + 16f
        val badgeRect = RectF(x + CARD_W - badgeW - 4f, y + CARD_H - 38f, x + CARD_W - 4f, y + CARD_H - 4f)
        canvas.drawRoundRect(badgeRect, 14f, 14f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 0, 0, 0) })
        canvas.drawText(badgeText, badgeRect.left + 8f, badgeRect.bottom - 9f, badgePaint)

        // 卡號
        val idPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 20f }
        canvas.drawText(printing.id, x, y + CARD_H + 24f, idPaint)
    }

    private fun drawBitmapFilled(canvas: Canvas, bmp: Bitmap, dst: RectF) {
        // scaledToFill：等比放大到蓋滿整個框，多的部分被 clip 裁掉
        val scale = maxOf(dst.width() / bmp.width, dst.height() / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        val left = dst.centerX() - w / 2
        val top = dst.centerY() - h / 2
        canvas.drawBitmap(bmp, null, RectF(left, top, left + w, top + h), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawFooter(canvas: Canvas, top: Float, sheetW: Float, deckName: String,
                           entries: List<DeckEntryEntity>) {
        val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 24f }
        canvas.drawText("用 WSDeckBuilder 掃這個 QR 就能匯入這副牌組", PADDING, top + 30f, hintPaint)

        val qr = qrBitmap(Payload.encode(deckName, entries), QR_SIZE)
        if (qr != null) {
            val qx = sheetW - PADDING - QR_SIZE
            canvas.drawBitmap(qr, qx, top, Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }
}
