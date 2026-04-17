package com.example.arfilterapp.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer,
        onCameraReady: (VideoCapture<Recorder>) -> Unit
    ) {
        // TODO: implement in camera task
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
    }
}
