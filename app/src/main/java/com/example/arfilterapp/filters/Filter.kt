package com.example.arfilterapp.filters

enum class FilterType(val displayName: String, val modelPath: String?) {
    NONE("None", null),
    GLASSES("Glasses", "models/glasses.glb"),
//    DOG_EARS("Dog Ears", "models/dog_ears.glb"),
//    CAT_FACE("Cat Face", "models/cat_face.glb"),
//    FUNNY_MASK("Funny Mask", "models/mask.glb")
}
