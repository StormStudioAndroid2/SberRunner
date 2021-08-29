package xyz.fcr.sberrunner.data.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

/**
 * Класс конвертации изображений для Room
 */
class Converters {

    /**
     * Конвертация ByteArray в Bitmap
     *
     * @return Bitmap
     */
    @TypeConverter
    fun toBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Конвертация Bitmap в ByteArray
     *
     * @return ByteArray
     */
    @TypeConverter
    fun fromBitmap(bmp: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}