package com.example.inventoryapp.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * A safe executor wrapper that suppresses RejectedExecutionException during teardown.
 * Prevents camera teardown crashes on worker threads.
 */
class SafeCameraExecutor : Executor {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var isShutdown = false

    override fun execute(command: Runnable) {
        if (isShutdown) return
        try {
            executor.execute(command)
        } catch (e: RejectedExecutionException) {
            // Drop queued frames during teardown
        }
    }

    fun shutdown() {
        isShutdown = true
        executor.shutdown()
    }
}

@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreview(onBarcodeScanned = onBarcodeScanned)
            ScannerOverlay(
                onClose = onClose,
                onSimulateResult = onBarcodeScanned
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please grant camera permission to scan product barcodes and auto-fetch inventory details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onClose) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val safeExecutor = remember { SafeCameraExecutor() }

    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var hasDispatchedBarcode by remember { mutableStateOf(false) }
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysisBuilder = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    val analyzer = MlKitAnalyzer(
                        listOf(barcodeScanner),
                        ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                        safeExecutor
                    ) { result ->
                        if (hasDispatchedBarcode) return@MlKitAnalyzer
                        val barcodes = result?.getValue(barcodeScanner)
                        if (!barcodes.isNullOrEmpty()) {
                            // Target the barcode closest to the viewfinder center
                            val targetBarcode = if (barcodes.size > 1) {
                                barcodes.minByOrNull { b ->
                                    val box = b.boundingBox
                                    if (box != null) {
                                        val cx = box.centerX()
                                        val cy = box.centerY()
                                        cx * cx + cy * cy
                                    } else {
                                        Int.MAX_VALUE
                                    }
                                } ?: barcodes.first()
                            } else {
                                barcodes.first()
                            }

                            val exactCode = extractExactBarcodeNumber(targetBarcode)
                            if (!exactCode.isNullOrBlank() && !hasDispatchedBarcode) {
                                hasDispatchedBarcode = true
                                mainExecutor.execute {
                                    try {
                                        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                    } catch (e: Exception) {
                                        // Ignore tone generator errors if unsupported or muted
                                    }
                                    try {
                                        cameraProvider.unbindAll()
                                    } catch (e: Exception) {
                                        Log.e("CameraPreview", "Error unbinding camera upon scan", e)
                                    }
                                    currentOnBarcodeScanned(exactCode)
                                }
                            }
                        }
                    }

                    val imageAnalysis = imageAnalysisBuilder.build().also {
                        it.setAnalyzer(safeExecutor, analyzer)
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Failed to bind camera lifecycle", e)
                }
            }, mainExecutor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error on camera unbind", e)
            }
            safeExecutor.shutdown()
        }
    }
}

@Composable
private fun ScannerOverlay(
    onClose: () -> Unit,
    onSimulateResult: (String) -> Unit
) {
    var testInputText by remember { mutableStateOf("") }
    var showTestInput by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Toolbar: Close button & Test input toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Scanner",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Barcode Scanner",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = { showTestInput = !showTestInput },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Simulate Scan",
                    tint = Color.White
                )
            }
        }

        // Center Viewfinder Target Frame
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1.4f)
                .align(Alignment.Center)
                .border(
                    2.5.dp,
                    Color(0xFF2563EB),
                    RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            // Animated Scanning Laser Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (laserOffsetY * 200).dp)
                    .background(Color.Red)
            )
        }

        // Bottom Instruction
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Align barcode inside frame to scan exact barcode number",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Test Manual Input Barcode Field
            if (showTestInput) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = { testInputText = it },
                            placeholder = { Text("e.g. 049000000443", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (testInputText.isNotBlank()) {
                                    onSimulateResult(testInputText.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Scan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Accurately extracts the exact digits / characters printed on the physical barcode.
 * Handles UPC-E expansion suppression, UPC-A 12-digit preservation, EAN-13 padding avoidance,
 * and Code 128 / Code 39 formatting.
 */
internal fun extractExactBarcodeNumber(barcode: Barcode): String? {
    val display = barcode.displayValue?.trim()
    val raw = barcode.rawValue?.trim()

    val candidate = when (barcode.format) {
        Barcode.FORMAT_UPC_E -> {
            // UPC-E is printed with 8 digits (or 6 digits compressed) on retail packaging.
            // ML Kit displayValue holds the exact digits physically printed on the package.
            if (!display.isNullOrBlank() && (display.length == 8 || display.length == 6)) {
                display
            } else if (!raw.isNullOrBlank() && (raw.length == 8 || raw.length == 6)) {
                raw
            } else {
                display?.ifBlank { null } ?: raw
            }
        }
        Barcode.FORMAT_UPC_A -> {
            // Standard UPC-A is 12 digits.
            // If ML Kit or system bridge promoted it to 13 digits with a leading "0", preserve the exact 12-digit UPC.
            if (raw != null && raw.length == 13 && raw.startsWith("0")) {
                raw.substring(1)
            } else if (display != null && display.length == 13 && display.startsWith("0")) {
                display.substring(1)
            } else {
                display?.ifBlank { null } ?: raw
            }
        }
        Barcode.FORMAT_EAN_13 -> {
            // If display is 12 digits (UPC-A) while raw was padded with leading 0 to 13 digits,
            // the printed barcode is the 12-digit UPC-A.
            if (!display.isNullOrBlank() && display.length == 12 && raw != null && raw.length == 13 && raw.startsWith("0")) {
                display
            } else {
                display?.ifBlank { null } ?: raw
            }
        }
        Barcode.FORMAT_EAN_8 -> {
            display?.ifBlank { null } ?: raw
        }
        else -> {
            display?.ifBlank { null } ?: raw
        }
    }

    return candidate?.trim()?.ifBlank { null }
}
