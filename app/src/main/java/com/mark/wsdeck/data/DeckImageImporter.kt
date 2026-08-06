package com.mark.wsdeck.data

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * 從匯出的牌組圖片讀回牌組：找圖上的 QR，解出載荷。對應 iOS 的
 * DeckImageImporter——用同一套 Payload 格式，所以 iOS 出的圖這裡掃得回來，
 * 反過來也一樣。
 *
 * 用 zxing-core 的 QRCodeReader 而非相機掃描：這是從相簿選的靜態圖片，
 * 不需要即時預覽那一整套 CameraX + ML Kit。
 */
object DeckImageImporter {

    sealed class Result {
        data class Success(val parsed: DeckImageExporter.Payload.Parsed) : Result()
        object NoCode : Result()
        object Unrecognized : Result()
    }

    fun parse(bitmap: Bitmap): Result {
        val text = decodeQrText(bitmap) ?: return Result.NoCode
        val parsed = DeckImageExporter.Payload.decode(text) ?: return Result.Unrecognized
        return Result.Success(parsed)
    }

    private fun decodeQrText(bitmap: Bitmap): String? {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        return try {
            QRCodeReader().decode(binary).text
        } catch (e: NotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
