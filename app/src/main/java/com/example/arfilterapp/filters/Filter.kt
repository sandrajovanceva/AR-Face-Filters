package com.example.arfilterapp.filters

import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.Pose
import io.github.sceneview.math.Position
import kotlin.math.cos
import kotlin.math.sin

data class FilterAttachment(
    val modelPath: String,
    val region: RegionType? = null,
    val offset: Position = Position(0f, 0f, 0f),
    val rotation: Position = Position(0f, 0f, 0f), // Euler степени (x, y, z)
    val scaleToUnits: Float? = null
) {
    private val offsetPose: Pose by lazy {
        Pose(
            floatArrayOf(offset.x, offset.y, offset.z),
            eulerToQuaternion(rotation.x, rotation.y, rotation.z)
        )
    }

    fun poseOn(face: AugmentedFace): Pose {
        val base = region?.let { face.getRegionPose(it) } ?: face.centerPose
        return base.compose(offsetPose)
    }
}

private fun eulerToQuaternion(xDeg: Float, yDeg: Float, zDeg: Float): FloatArray {
    val rx = Math.toRadians(xDeg.toDouble())
    val ry = Math.toRadians(yDeg.toDouble())
    val rz = Math.toRadians(zDeg.toDouble())
    val cx = cos(rx / 2); val sx = sin(rx / 2)
    val cy = cos(ry / 2); val sy = sin(ry / 2)
    val cz = cos(rz / 2); val sz = sin(rz / 2)
    val w = cx * cy * cz + sx * sy * sz
    val x = sx * cy * cz - cx * sy * sz
    val y = cx * sy * cz + sx * cy * sz
    val z = cx * cy * sz - sx * sy * cz
    return floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
}

enum class FilterType(
    val displayName: String,
    val icon: String,
    val attachments: List<FilterAttachment>
) {
    NONE("None", "✕", emptyList()),

    GRADUATION(
        "Graduation", "🎓", listOf(
            FilterAttachment(
                modelPath = "models/graduation_cap.glb",
                region = null, // centerPose = центар на главата
                offset = Position(0f, 0.078f, -0.015f),
                rotation = Position(0f, 0f, 18f) // навалена капа
            ),
            FilterAttachment(
                modelPath = "models/io_logo.glb",
                region = null, // центрирано на предниот дел од капата
                offset = Position(-0.005f, 0.085f, 0.05f),
                rotation = Position(0f, 0f, 18f), // се навалува заедно со капата
                scaleToUnits = 0.03f
            )
        )
    ),

    // Спин-тркало за најтежок предмет — нема 3D модел, само 2D overlay
    HARDEST("Hardest", "🎲", emptyList()),

    GLASSES(
        "Glasses", "👓", listOf(
            FilterAttachment(
                modelPath = "models/glasses.glb",
                region = RegionType.NOSE_TIP,
                offset = Position(0f, 0.01f, 0f),
                scaleToUnits = 0.16f
            )
        )
    ),

    DOG(
        "Dog", "🐶", listOf(
            FilterAttachment(
                modelPath = "models/dog_ear_l.glb",
                region = RegionType.FOREHEAD_LEFT,
                offset = Position(0f, 0.025f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/dog_ear_r.glb",
                region = RegionType.FOREHEAD_RIGHT,
                offset = Position(0f, 0.025f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/dog_nose.glb",
                region = RegionType.NOSE_TIP,
                offset = Position(0f, 0f, 0.005f)
            )
        )
    ),

    CAT(
        "Cat", "🐱", listOf(
            FilterAttachment(
                modelPath = "models/cat_ear_l.glb",
                region = RegionType.FOREHEAD_LEFT,
                offset = Position(0f, 0.03f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/cat_ear_r.glb",
                region = RegionType.FOREHEAD_RIGHT,
                offset = Position(0f, 0.03f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/cat_nose.glb",
                region = RegionType.NOSE_TIP,
                offset = Position(0f, 0f, 0.003f)
            )
        )
    ),

    BUNNY(
        "Bunny", "🐰", listOf(
            FilterAttachment(
                modelPath = "models/bunny_ear_l.glb",
                region = RegionType.FOREHEAD_LEFT,
                offset = Position(0f, 0.025f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/bunny_ear_r.glb",
                region = RegionType.FOREHEAD_RIGHT,
                offset = Position(0f, 0.025f, 0f)
            )
        )
    ),

    CROWN(
        "Crown", "👑", listOf(
            FilterAttachment(
                modelPath = "models/crown.glb",
                region = null,
                offset = Position(0f, 0.08f, -0.005f)
            )
        )
    ),

    DEVIL(
        "Devil", "😈", listOf(
            FilterAttachment(
                modelPath = "models/devil_horn_l.glb",
                region = RegionType.FOREHEAD_LEFT,
                offset = Position(0f, 0.03f, 0f)
            ),
            FilterAttachment(
                modelPath = "models/devil_horn_r.glb",
                region = RegionType.FOREHEAD_RIGHT,
                offset = Position(0f, 0.03f, 0f)
            )
        )
    ),

    CLOWN(
        "Clown", "🤡", listOf(
            FilterAttachment(
                modelPath = "models/clown_nose.glb",
                region = RegionType.NOSE_TIP,
                offset = Position(0f, 0f, 0.005f)
            ),
            FilterAttachment(
                modelPath = "models/mustache.glb",
                region = RegionType.NOSE_TIP,
                offset = Position(0f, -0.022f, 0.008f)
            ),
            FilterAttachment(
                modelPath = "models/clown_hat.glb",
                region = null,
                offset = Position(0f, 0.075f, -0.005f)
            )
        )
    )
}
