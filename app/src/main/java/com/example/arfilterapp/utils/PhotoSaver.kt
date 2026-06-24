package com.example.arfilterapp.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
 * Draws a Google I/O badge (a dark-navy "I" bar + a lighter-blue "O" ring),
 * centered near the top of the photo (over the graduation cap). Returns a
 * mutable copy with the stamp.
 */
fun stampIoLogo(src: Bitmap): Bitmap {
    val bmp =
        if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bmp)
    val w = bmp.width.toFloat()
    val h = bmp.height.toFloat()

    val logoH = h * 0.075f          // overall badge height
    val ro = logoH * 0.5f           // "O" outer radius
    val strokeW = logoH * 0.20f     // "O" ring thickness
    val barW = logoH * 0.26f        // "I" bar width
    val gap = logoH * 0.22f         // space between "I" and "O"
    val topMargin = h * 0.05f       // distance from the top edge

    val navy = Color.rgb(26, 40, 120)    // darker "I"
    val blue = Color.rgb(45, 112, 210)   // lighter "O"

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        setShadowLayer(logoH * 0.14f, 0f, logoH * 0.05f, Color.argb(130, 0, 0, 0))
    }

    // Center the "I  O" group horizontally, near the top
    val groupW = barW + gap + 2f * ro
    val startX = (w - groupW) / 2f
    val top = topMargin
    val bottom = topMargin + logoH

    // "I" bar — darker navy
    paint.color = navy
    paint.style = Paint.Style.FILL
    val r = barW / 2f
    canvas.drawRoundRect(RectF(startX, top, startX + barW, bottom), r, r, paint)

    // "O" ring — lighter blue
    paint.color = blue
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokeW
    val oCx = startX + barW + gap + ro
    val oCy = top + ro
    canvas.drawCircle(oCx, oCy, ro - strokeW / 2f, paint)

    return bmp
}

private val CONFETTI_RGB = intArrayOf(
    Color.rgb(255, 200, 40), Color.rgb(45, 112, 210), Color.rgb(220, 60, 70),
    Color.rgb(60, 190, 110), Color.rgb(170, 90, 220), Color.rgb(245, 130, 190)
)

private fun drawIoLogo(
    canvas: Canvas, cx: Float, cy: Float, angle: Float, s: Float,
    navy: Int, blue: Int, paint: Paint
) {
    val ro = s * 0.45f
    val strokeW = s * 0.20f
    val barW = s * 0.24f
    val gap = s * 0.16f
    val groupW = barW + gap + 2f * ro
    val left = cx - groupW / 2f
    canvas.save()
    canvas.rotate(angle, cx, cy)
    paint.style = Paint.Style.FILL
    paint.color = navy
    val r = barW / 2f
    canvas.drawRoundRect(RectF(left, cy - s / 2f, left + barW, cy + s / 2f), r, r, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokeW
    paint.color = blue
    canvas.drawCircle(left + barW + gap + ro, cy, ro - strokeW / 2f, paint)
    canvas.restore()
}

/**
 * Draws a "FINKI GRADUATES" banner near the top of the photo plus a scatter of
 * confetti across the upper half. Returns a mutable copy with the overlay.
 */
fun stampGraduationBanner(src: Bitmap): Bitmap {
    val bmp =
        if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bmp)
    val w = bmp.width.toFloat()
    val h = bmp.height.toFloat()

    val navy = Color.rgb(26, 40, 120)
    val blue = Color.rgb(60, 134, 224)

    val rnd = java.util.Random(7)  // fixed seed → stable, reproducible scatter
    val confetti = Paint(Paint.ANTI_ALIAS_FLAG)
    val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    repeat(120) {
        val cx = rnd.nextFloat() * w
        val cy = rnd.nextFloat() * h        // across the whole frame, like the live view
        val angle = rnd.nextFloat() * 360f
        if (rnd.nextInt(100) < 32) {        // ~32% are falling IO logos
            val s = w * (0.04f + rnd.nextFloat() * 0.025f)
            drawIoLogo(canvas, cx, cy, angle, s, navy, blue, logoPaint)
        } else {
            val cw = w * (0.012f + rnd.nextFloat() * 0.018f)
            val ch = cw * (0.5f + rnd.nextFloat() * 0.8f)
            confetti.color = CONFETTI_RGB[rnd.nextInt(CONFETTI_RGB.size)]
            canvas.save()
            canvas.rotate(angle, cx, cy)
            canvas.drawRect(cx - cw / 2f, cy - ch / 2f, cx + cw / 2f, cy + ch / 2f, confetti)
            canvas.restore()
        }
    }

    val ty = h * 0.10f
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = w * 0.065f               // ~matches the live 24.sp banner
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(w * 0.012f, 0f, w * 0.004f, Color.argb(170, 0, 0, 0))
    }
    canvas.drawText("🎓 FINKI GRADUATES 🎓", w / 2f, ty, textPaint)
    return bmp
}

/**
 * Draws a "HARDEST SUBJECT" card near the top of the photo with the chosen
 * subject. Auto-shrinks the subject text to fit. Returns a mutable copy.
 */
fun stampHardestSubject(src: Bitmap, subject: String): Bitmap {
    val bmp =
        if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bmp)
    val w = bmp.width.toFloat()
    val h = bmp.height.toFloat()

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 200, 40)
        textAlign = Paint.Align.CENTER
        textSize = w * 0.04f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(w * 0.01f, 0f, w * 0.003f, Color.argb(180, 0, 0, 0))
    }
    val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = w * 0.058f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(w * 0.012f, 0f, w * 0.004f, Color.argb(190, 0, 0, 0))
    }
    while (subjectPaint.measureText(subject) > w * 0.86f && subjectPaint.textSize > w * 0.02f) {
        subjectPaint.textSize -= w * 0.003f
    }

    val titleY = h * 0.09f
    val subjectY = titleY + w * 0.075f
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 0, 0, 0) }
    canvas.drawRoundRect(
        RectF(w * 0.06f, titleY - w * 0.05f, w * 0.94f, subjectY + w * 0.025f),
        w * 0.03f, w * 0.03f, bg
    )
    canvas.drawText("🔥 HARDEST SUBJECT 🔥", w / 2f, titleY, titlePaint)
    canvas.drawText(subject, w / 2f, subjectY, subjectPaint)
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
