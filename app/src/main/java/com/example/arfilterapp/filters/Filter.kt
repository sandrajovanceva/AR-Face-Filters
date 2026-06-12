package com.example.arfilterapp.filters

import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.Pose
import io.github.sceneview.math.Position

/**
 * Еден 3D модел закачен на одреден регион од лицето.
 *
 * @param modelPath патека до .glb во assets
 * @param region регион на лицето; null = center pose (центар на главата)
 * @param offset поместување во метри, во локалниот простор на регионот
 * @param scaleToUnits скалирај го моделот да собере во толку метри;
 *        null = моделот е веќе авториран во метри
 */
data class FilterAttachment(
    val modelPath: String,
    val region: RegionType? = null,
    val offset: Position = Position(0f, 0f, 0f),
    val scaleToUnits: Float? = null
) {
    private val offsetPose: Pose by lazy {
        Pose.makeTranslation(offset.x, offset.y, offset.z)
    }

    fun poseOn(face: AugmentedFace): Pose {
        val base = region?.let { face.getRegionPose(it) } ?: face.centerPose
        return base.compose(offsetPose)
    }
}

enum class FilterType(
    val displayName: String,
    val icon: String,
    val attachments: List<FilterAttachment>
) {
    NONE("None", "✕", emptyList()),

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
                region = null, // центар на главата
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
                region = null, // врв на главата
                offset = Position(0f, 0.075f, -0.005f)
            )
        )
    )
}
