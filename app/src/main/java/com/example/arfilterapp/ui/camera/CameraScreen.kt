package com.example.arfilterapp.ui.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arfilterapp.filters.FilterAttachment
import com.example.arfilterapp.filters.FilterType
import com.example.arfilterapp.utils.RequestCameraPermission
import com.example.arfilterapp.utils.captureSurface
import com.example.arfilterapp.utils.saveToGallery
import com.example.arfilterapp.utils.shareImage
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberScene
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen() {
    RequestCameraPermission {
        ARContent()
    }
}

private class AttachedNode(val node: ModelNode, val attachment: FilterAttachment)

private fun mirrorInCameraSpace(pose: Pose, cameraPose: Pose): Pose {
    val local = cameraPose.inverse().compose(pose)
    val mirrored = Pose(
        floatArrayOf(-local.tx(), local.ty(), local.tz()),
        floatArrayOf(local.qx(), -local.qy(), -local.qz(), local.qw())
    )
    return cameraPose.compose(mirrored)
}

@Composable
private fun ARContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val scene = rememberScene(engine)
    val childNodes = rememberNodes()

    var selectedFilter by remember { mutableStateOf(FilterType.NONE) }
    var attachedNodes by remember { mutableStateOf<List<AttachedNode>>(emptyList()) }
    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }

    val flashAlpha = remember { Animatable(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedPhoto by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(selectedFilter) {
        attachedNodes.forEach {
            childNodes.remove(it.node)
            it.node.destroy()
        }
        attachedNodes = selectedFilter.attachments.map { attachment ->
            val instance = modelLoader.createModelInstance(attachment.modelPath)
            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits = attachment.scaleToUnits
            ).apply { isVisible = false }
            childNodes.add(node)
            AttachedNode(node, attachment)
        }
    }

    fun capturePhoto() {
        val view = sceneView ?: return
        if (isCapturing || capturedPhoto != null) return
        isCapturing = true
        scope.launch {
            launch {
                flashAlpha.snapTo(0.7f)
                flashAlpha.animateTo(0f, tween(durationMillis = 350))
            }
            capturedPhoto = try {
                captureSurface(view)
            } catch (e: Exception) {
                statusMessage = "Couldn't capture photo"
                null
            }
            isCapturing = false
            if (capturedPhoto == null) {
                delay(2000)
                statusMessage = null
            }
        }
    }

    fun saveCapturedPhoto() {
        val photo = capturedPhoto ?: return
        scope.launch {
            val saved = saveToGallery(context, photo)
            capturedPhoto = null
            statusMessage = if (saved) "✓ Saved to gallery" else "Couldn't save photo"
            delay(2000)
            statusMessage = null
        }
    }

    fun shareCapturedPhoto() {
        val photo = capturedPhoto ?: return
        scope.launch {
            val shared = shareImage(context, photo)
            if (!shared) {
                statusMessage = "Couldn't share photo"
                delay(2000)
                statusMessage = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            scene = scene,
            childNodes = childNodes,
            sessionFeatures = setOf(Session.Feature.FRONT_CAMERA),
            sessionConfiguration = { _, config ->
                config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            },
            onViewCreated = { sceneView = this },
            onSessionUpdated = { session, frame ->
                val trackedFace = session.getAllTrackables(AugmentedFace::class.java)
                    .firstOrNull { it.trackingState == TrackingState.TRACKING }
                val cameraPose = frame.camera.displayOrientedPose

                attachedNodes.forEach {
                    if (trackedFace != null) {
                        val pose = mirrorInCameraSpace(
                            it.attachment.poseOn(trackedFace), cameraPose
                        )
                        it.node.worldPosition = Position(pose.tx(), pose.ty(), pose.tz())
                        it.node.worldQuaternion =
                            Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                        it.node.isVisible = true
                    } else {
                        it.node.isVisible = false
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    )
                )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = selectedFilter.displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(FilterType.entries) { filter ->
                    FilterIcon(
                        filter = filter,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ShutterButton(enabled = !isCapturing, onClick = ::capturePhoto)
            Spacer(modifier = Modifier.height(28.dp))
        }

        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        capturedPhoto?.let { photo ->
            PhotoPreview(
                photo = photo,
                onRetake = { capturedPhoto = null },
                onShare = ::shareCapturedPhoto,
                onSave = ::saveCapturedPhoto
            )
        }

        AnimatedVisibility(
            visible = statusMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = statusMessage ?: "",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PhotoPreview(
    photo: Bitmap,
    onRetake: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = "Captured photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "✕",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onRetake)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "↺  Retake",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onRetake)
                    .padding(horizontal = 20.dp, vertical = 13.dp)
            )
            Text(
                text = "Save  ⤓",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onSave)
                    .padding(horizontal = 20.dp, vertical = 13.dp)
            )
            Text(
                text = "Share  ↗",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onShare)
                    .padding(horizontal = 22.dp, vertical = 13.dp)
            )
        }
    }
}

@Composable
private fun FilterIcon(
    filter: FilterType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val size by animateDpAsState(if (selected) 62.dp else 52.dp, label = "filterSize")
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (selected) 0.55f else 0.35f))
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = filter.icon, fontSize = if (selected) 28.sp else 22.sp)
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val innerScale by animateFloatAsState(if (pressed) 0.82f else 1f, label = "shutterScale")

    Box(
        modifier = Modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .scale(innerScale)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
