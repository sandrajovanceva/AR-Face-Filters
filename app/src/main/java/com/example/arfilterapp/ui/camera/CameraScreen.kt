package com.example.arfilterapp.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arfilterapp.filters.FilterType
import com.example.arfilterapp.utils.RequestCameraPermission
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARScene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberScene

@Composable
fun CameraScreen() {
    RequestCameraPermission {
        ARContent()
    }
}

@Composable
private fun ARContent() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val scene = rememberScene(engine)
    val childNodes = rememberNodes()

    var selectedFilter by remember { mutableStateOf(FilterType.NONE) }
    var activeFilter by remember { mutableStateOf(FilterType.NONE) }
    var faceNode by remember { mutableStateOf<ModelNode?>(null) }

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
            onSessionUpdated = { session, _ ->
                // Земи ги сите тековно следени лица преку session
                val faces = session.getAllTrackables(AugmentedFace::class.java)
                val trackedFace = faces.firstOrNull { it.trackingState == TrackingState.TRACKING }

                if (trackedFace != null) {
                    // Ако се сменил филтерот — избриши стар нод и додај нов
                    if (activeFilter != selectedFilter) {
                        faceNode?.let {
                            childNodes.remove(it)
                            it.destroy()
                        }
                        faceNode = null

                        val modelPath = selectedFilter.modelPath
                        if (modelPath != null) {
                            val instance = modelLoader.createModelInstance(modelPath)
                            faceNode = ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 1.0f
                            ).also { childNodes.add(it) }
                        }
                        activeFilter = selectedFilter
                    }

                    // Секој frame — позиционирај го нодот на лицето
                    faceNode?.apply {
                        val pose = trackedFace.getCenterPose()
                        worldPosition = Position(pose.tx(), pose.ty(), pose.tz())
                        worldQuaternion = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
                        isVisible = true
                    }
                } else {
                    faceNode?.isVisible = false
                }
            }
        )

        // Selector за филтри на дното
        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 8.dp, end = 8.dp)
                .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(FilterType.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.displayName) }
                )
            }
        }
    }
}
