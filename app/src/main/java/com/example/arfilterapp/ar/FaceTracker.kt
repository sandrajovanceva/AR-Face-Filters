package com.example.arfilterapp.ar

import android.content.Context
import android.graphics.RectF

data class FaceLandmarks(
    val boundingBox: RectF,
    val keypoints: List<Pair<Float, Float>>
)

class FaceTracker(private val context: Context) {

    private var isInitialized = false

    fun initialize() {
        // TODO: set up MediaPipe FaceLandmarker in face tracking task
    }

    fun detectFaces(
        imageProxy: androidx.camera.core.ImageProxy,
        onResult: (List<FaceLandmarks>) -> Unit
    ) {
        // TODO: implement in face tracking task
        imageProxy.close()
    }

    fun close() {
        // TODO: release MediaPipe resources
    }
}
