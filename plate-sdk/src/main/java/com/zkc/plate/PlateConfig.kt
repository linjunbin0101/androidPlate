package com.zkc.plate

import com.hyperai.hyperlpr3.HyperLPR3

/**
 * Configuration for plate recognition.
 * All fields have sensible defaults — only override what you need.
 */
data class PlateConfig(
    /** Detection sensitivity: DETECT_LEVEL_LOW / MEDIUM / HIGH */
    val detectionLevel: Int = HyperLPR3.DETECT_LEVEL_HIGH,

    /** Maximum number of plates to return per image */
    val maxPlates: Int = 5,

    /** Confidence threshold for recognition (0.0 – 1.0) */
    val confidenceThreshold: Float = 0.7f,

    /** Image width to scale input bitmap to before recognition */
    val roiImageWidth: Int = 640,
)
