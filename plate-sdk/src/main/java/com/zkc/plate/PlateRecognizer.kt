package com.zkc.plate

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.hyperai.hyperlpr3.HyperLPR3
import com.hyperai.hyperlpr3.bean.HyperLPRParameter

/**
 * Lightweight license plate recognition SDK.
 *
 * Usage:
 * ```
 * val recognizer = PlateRecognizer.init(context)
 * val plates = recognizer.recognize(bitmap)  // List<String>
 * ```
 *
 * With custom config:
 * ```
 * val recognizer = PlateRecognizer.init(context, PlateConfig(maxPlates = 3))
 * ```
 */
class PlateRecognizer private constructor(private val config: PlateConfig) {

    companion object {
        private const val TAG = "PlateRecognizer"
        private var instance: PlateRecognizer? = null

        /**
         * Initialize the SDK. Safe to call multiple times — subsequent calls return the existing instance.
         * @param context  any Context (Application context is used internally)
         * @param config   optional configuration; defaults are used when not specified
         */
        fun init(context: Context, config: PlateConfig = PlateConfig()): PlateRecognizer {
            val existing = instance
            if (existing != null) {
                Log.w(TAG, "Already initialized — returning existing instance")
                return existing
            }
            val parameter = HyperLPRParameter()
                .setDetLevel(config.detectionLevel)
                .setMaxNum(config.maxPlates)
                .setRecConfidenceThreshold(config.confidenceThreshold)
            HyperLPR3.getInstance().init(context.applicationContext, parameter)
            val recognizer = PlateRecognizer(config)
            instance = recognizer
            Log.i(TAG, "Initialized — $config")
            return recognizer
        }

        /** Retrieve the singleton instance. Throws if not initialized. */
        fun getInstance(): PlateRecognizer {
            return instance
                ?: throw IllegalStateException("PlateRecognizer not initialized. Call PlateRecognizer.init(context) first.")
        }
    }

    /**
     * Recognize license plates from a bitmap.
     * The bitmap should be pre-rotated to upright orientation by the caller.
     *
     * @param bitmap  input image (any resolution, will be scaled down automatically)
     * @return list of plate numbers (e.g. ["粤A12345", "京B67890"]), empty if none found
     */
    fun recognize(bitmap: Bitmap): List<String> {
        val scaledW = config.roiImageWidth
        val scaledH = (bitmap.height.toFloat() / bitmap.width * scaledW).toInt().coerceAtLeast(1)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)

        val plates = HyperLPR3.getInstance().plateRecognition(
            scaledBitmap,
            0,  // bitmap should be upright — caller handles rotation
            HyperLPR3.STREAM_BGRA
        )

        return plates.map { it.code }.filter { it.isNotBlank() }
    }
}
