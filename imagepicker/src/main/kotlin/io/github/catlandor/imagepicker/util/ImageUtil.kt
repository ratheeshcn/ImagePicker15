package io.github.catlandor.imagepicker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build

object ImageUtil {
    fun getBitmap(context: Context, imageUri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, imageUri)
                )
            } catch (e: ImageDecoder.DecodeException) {
                null
            }
        } else {
            context
                .contentResolver
                .openInputStream(imageUri)
                ?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
        }
}
