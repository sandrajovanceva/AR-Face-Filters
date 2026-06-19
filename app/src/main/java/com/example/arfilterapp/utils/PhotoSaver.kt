package com.example.arfilterapp.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.provider.MediaStore
import android.view.TextureView
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun captureSurface(view: TextureView): Bitmap =
    view.getBitmap(view.width, view.height)
        ?: throw IllegalStateException("Surface not ready for capture")

/**
 * Draws a small Google I/O-style badge (a blue "I" bar + a blue "O" ring) into
 * the bottom-right corner of the photo. Returns a mutable copy with the stamp.
 */
fun stampIoLogo(src: Bitmap): Bitmap {
    val bmp =
        if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bmp)
    val w = bmp.width.toFloat()
    val h = bmp.height.toFloat()

    val logoH = h * 0.085f          // overall badge height
    val ro = logoH * 0.5f           // "O" outer radius
    val strokeW = logoH * 0.20f     // "O" ring thickness
    val barW = logoH * 0.26f        // "I" bar width
    val gap = logoH * 0.24f         // space between "I" and "O"
    val margin = h * 0.03f          // distance from the image edges

    val blue = Color.rgb(10, 112, 222)   // matches the app's I/O blue

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = blue
        setShadowLayer(logoH * 0.14f, 0f, logoH * 0.05f, Color.argb(130, 0, 0, 0))
    }

    // "O" ring — bottom-right
    val oCx = w - margin - ro
    val oCy = h - margin - ro
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokeW
    canvas.drawCircle(oCx, oCy, ro - strokeW / 2f, paint)

    // "I" bar — sits to the left of the "O"
    paint.style = Paint.Style.FILL
    val barRight = oCx - ro - gap
    val barLeft = barRight - barW
    val barTop = h - margin - logoH
    val barBottom = h - margin
    val r = barW / 2f
    canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), r, r, paint)

    return bmp
}

suspend fun shareImage(context: Context, bitmap: Bitmap): Boolean {
    val uri = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "ar_filter_share.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    } ?: return false

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, "Share your AR photo").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(chooser)
        true
    } catch (e: Exception) {
        false
    }
}

suspend fun saveToGallery(context: Context, bitmap: Bitmap): Boolean =
    withContext(Dispatchers.IO) {
        val name = "ARFilter_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ARFilterApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    return@withContext false
                }
            } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
