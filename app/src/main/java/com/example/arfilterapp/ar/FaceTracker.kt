package com.example.arfilterapp.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

class FaceTracker(
    private val context: Context,
    private val onResult: (List<Pair<Float, Float>>) -> Unit
) {

    private var faceLandmarker: FaceLandmarker? = null

    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setResultListener { result, _ ->
                    val points = result.faceLandmarks().firstOrNull()
                        ?.map { lm -> lm.x() to lm.y() }
                        ?: emptyList()
                    onResult(points)
                }
                .setErrorListener { e -> Log.e(TAG, "FaceLandmarker error", e) }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d(TAG, "FaceLandmarker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FaceLandmarker — is $MODEL_PATH in assets?", e)
        }
    }

    fun detect(imageProxy: ImageProxy) {
        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotated = rotateAndMirror(bitmap, imageProxy.imageInfo.rotationDegrees)
            val mpImage = BitmapImageBuilder(rotated).build()
            val timestampUs = imageProxy.imageInfo.timestamp / 1000
            landmarker.detectAsync(mpImage, timestampUs)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting face", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun rotateAndMirror(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            postScale(-1f, 1f) // mirror for front camera
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    companion object {
        private const val TAG = "FaceTracker"
        private const val MODEL_PATH = "face_landmarker.task"
    }
}
