package com.example.arfilterapp.ui.camera

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arfilterapp.ar.FaceTracker
import com.example.arfilterapp.camera.CameraManager
import com.example.arfilterapp.filters.FilterType
import com.example.arfilterapp.ui.theme.ARFilterAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        CameraScreenContent(viewModel)
    } else {
        PermissionRequestUI(
            shouldShowRationale = cameraPermission.status.shouldShowRationale,
            onRequest = { cameraPermission.launchPermissionRequest() }
        )
    }
}

@Composable
private fun CameraScreenContent(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }
    val faceTracker = remember {
        FaceTracker(context) { points -> viewModel.updateLandmarks(points) }
    }

    LaunchedEffect(Unit) { faceTracker.initialize() }
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.shutdown()
            faceTracker.close()
        }
    }

    val landmarks by viewModel.landmarks.collectAsStateWithLifecycle()
    val debugEnabled by viewModel.debugOverlayEnabled.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    cameraManager.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        imageAnalyzer = faceTracker::detect,
                        onCameraReady = {}
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        FilterOverlay(
            filter = selectedFilter,
            landmarks = landmarks,
            modifier = Modifier.fillMaxSize()
        )

        if (debugEnabled) {
            FaceLandmarksOverlay(
                landmarks = landmarks,
                modifier = Modifier.fillMaxSize()
            )
        }

        TopBar(
            faceDetected = landmarks.isNotEmpty(),
            debugEnabled = debugEnabled,
            onToggleDebug = viewModel::toggleDebugOverlay,
            onGalleryClick = { /* TODO: open gallery in capture/gallery task */ },
            onSettingsClick = { /* TODO: open settings */ },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        BottomControls(
            filters = FilterType.entries,
            selectedFilter = selectedFilter,
            onSelectFilter = viewModel::selectFilter,
            onCapture = { /* TODO: photo/video capture in next task */ },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun TopBar(
    faceDetected: Boolean,
    debugEnabled: Boolean,
    onToggleDebug: () -> Unit,
    onGalleryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBubble(icon = Icons.Default.List, contentDescription = "Gallery", onClick = onGalleryClick)
            IconBubble(icon = Icons.Default.Settings, contentDescription = "Settings", onClick = onSettingsClick)
        }

        FaceDetectionBadge(faceDetected = faceDetected)

        DebugToggleButton(enabled = debugEnabled, onToggle = onToggleDebug)
    }
}

@Composable
private fun IconBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x99000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun FaceDetectionBadge(faceDetected: Boolean, modifier: Modifier = Modifier) {
    val text = if (faceDetected) "Face detected" else "Searching..."
    val bg = if (faceDetected) Color(0xCC2E7D32) else Color(0x99000000)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DebugToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (enabled) Color(0xCC1565C0) else Color(0x99000000)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (enabled) "Debug" else "Debug",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BottomControls(
    filters: List<FilterType>,
    selectedFilter: FilterType,
    onSelectFilter: (FilterType) -> Unit,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilterPicker(
            filters = filters,
            selectedFilter = selectedFilter,
            onSelectFilter = onSelectFilter
        )
        Spacer(modifier = Modifier.height(20.dp))
        CaptureButton(onClick = onCapture)
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 780)
@Composable
private fun CameraScreenLayoutPreview() {
    ARFilterAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C2C2C))
        ) {
            // Fake "camera feed" gradient circle (simulates a face)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xFF5C5C5C))
            )

            TopBar(
                faceDetected = true,
                debugEnabled = true,
                onToggleDebug = {},
                onGalleryClick = {},
                onSettingsClick = {},
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            BottomControls(
                filters = FilterType.entries,
                selectedFilter = FilterType.GLASSES,
                onSelectFilter = {},
                onCapture = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun PermissionRequestUI(shouldShowRationale: Boolean, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (shouldShowRationale)
                "Camera access is required to display AR filters."
            else
                "Camera permission is needed to use this app.",
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text("Grant Camera Permission")
        }
    }
}
