package com.zkc.androidplate

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zkc.plate.PlateRecognizer
import com.zkc.androidplate.ui.theme.AndroidPlateTheme
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlateRecog"
    }

    private var plateNumber by mutableStateOf("等待拍照...")
    private var isInitialized = false
    private var frameCount = 0
    private var showCaptured by mutableStateOf(false)
    private var capturedBitmap by mutableStateOf<Bitmap?>(null)
    private var captureRequested by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            initHyperLPR()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            initHyperLPR()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            AndroidPlateTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title
                    Text(
                        text = "车牌识别",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Preview area ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showCaptured && capturedBitmap != null) {
                                // Show captured photo
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "captured",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Live camera preview
                                CameraPreview(
                                    onFrame = { imageProxy -> analyzeFrame(imageProxy) },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Guide box overlay — plate-shaped green frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.78f)
                                        .aspectRatio(3.5f)
                                        .border(
                                            BorderStroke(3.dp, Color(0xFF4CAF50)),
                                            RoundedCornerShape(8.dp)
                                        )
                                )

                                // Hint text below guide box
                                Text(
                                    text = "将车牌对准框内",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Result card ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (plateNumber.contains("等待") || plateNumber.contains("失败") || plateNumber.contains("未识别"))
                                Color(0xFF2A2A2A)
                            else
                                Color(0xFF1B5E20)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (plateNumber.contains("拍照")) "请先拍照"
                                    else if (plateNumber.contains("识别中")) "识别中..."
                                    else "识别结果",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = plateNumber,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 36.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Action button ──
                    if (showCaptured) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2196F3))
                                .clickable {
                                    showCaptured = false
                                    capturedBitmap = null
                                    plateNumber = "等待拍照..."
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "重新拍照",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2196F3))
                                .clickable {
                                    captureRequested = true
                                    plateNumber = "识别中..."
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "拍照识别",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // HyperLPR3 init
    // ──────────────────────────────────────────────

    private fun initHyperLPR() {
        try {
            PlateRecognizer.init(this)
            isInitialized = true
            Log.i(TAG, "PlateRecognizer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "initHyperLPR failed", e)
            plateNumber = "初始化失败"
        }
    }

    // ──────────────────────────────────────────────
    // Frame analysis
    // ──────────────────────────────────────────────

    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (!isInitialized) {
            imageProxy.close()
            return
        }

        try {
            // Skip frames when not actively capturing
            if (!captureRequested) {
                return
            }

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap == null) {
                captureRequested = false
                return
            }

            val rotation = imageProxy.imageInfo.rotationDegrees

            // Rotate bitmap to upright orientation so model always sees correct orientation
            val uprightBitmap = if (rotation == 0) bitmap else rotateBitmap(bitmap, rotation)

            // Save upright bitmap for display
            capturedBitmap = uprightBitmap.copy(Bitmap.Config.ARGB_8888, false)

            Log.i(TAG, "Capture src=${bitmap.width}x${bitmap.height} rot=$rotation")

            val codes = PlateRecognizer.getInstance().recognize(uprightBitmap)
            if (codes.isNotEmpty()) {
                plateNumber = codes.joinToString("\n")
                Log.i(TAG, "Recognized ${codes.size} plate(s): $codes")
                showCaptured = true
                return
            }

            // No plate detected
            plateNumber = "未识别到车牌"
            showCaptured = true
        } catch (e: Exception) {
            Log.e(TAG, "analyzeFrame error", e)
            plateNumber = "识别出错"
            showCaptured = true
        } finally {
            captureRequested = false
            imageProxy.close()
        }
    }

    // Rotate bitmap to specified degrees (counter-clockwise: 90, 180, 270)
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ──────────────────────────────────────────────
    // ImageProxy → Bitmap using YuvImage (system API)
    // ──────────────────────────────────────────────

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val nv21 = yuv420888ToNv21(image)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 95, out)
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            Log.e(TAG, "imageProxyToBitmap failed", e)
            null
        }
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val nv21 = ByteArray(width * height * 3 / 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        // Copy Y plane: simple contiguous case, handling row stride
        val yBuf = yPlane.buffer
        val yRowStride = yPlane.rowStride
        var yPos = 0
        if (yRowStride == width) {
            val yLen = yBuf.remaining().coerceAtMost(width * height)
            yBuf.get(nv21, 0, yLen)
            yPos = yLen
        } else {
            val limit = yBuf.limit()
            for (row in 0 until height) {
                val pos = row * yRowStride
                if (pos + width > limit) break
                yBuf.position(pos)
                yBuf.get(nv21, yPos, width)
                yPos += width
            }
        }

        // Read U and V bytes into arrays (before rewind, get sizes)
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        uBuf.rewind()
        vBuf.rewind()
        val uBytes = ByteArray(uBuf.remaining())
        val vBytes = ByteArray(vBuf.remaining())
        uBuf.get(uBytes)
        vBuf.get(vBytes)

        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        var uvPos = width * height
        val uvH = height / 2
        val uvW = width / 2

        if (uPixelStride == 1 && vPixelStride == 1) {
            var uOff = 0
            var vOff = 0
            for (row in 0 until uvH) {
                for (col in 0 until uvW) {
                    nv21[uvPos++] = vBytes[vOff++]
                    nv21[uvPos++] = uBytes[uOff++]
                }
                uOff += uRowStride - uvW
                vOff += vRowStride - uvW
            }
        } else {
            var uOff = 0
            var vOff = 0
            for (row in 0 until uvH) {
                for (col in 0 until uvW) {
                    nv21[uvPos++] = vBytes[vOff]; vOff += vPixelStride
                    nv21[uvPos++] = uBytes[uOff]; uOff += uPixelStride
                }
                uOff += uRowStride - uvW * uPixelStride
                vOff += vRowStride - uvW * vPixelStride
            }
        }

        return nv21
    }
}

// ──────────────────────────────────────────────
// Camera preview composable
// ──────────────────────────────────────────────

@androidx.compose.runtime.Composable
fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                            onFrame(proxy)
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as androidx.lifecycle.LifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "bind failed", e)
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = modifier
    )
}
