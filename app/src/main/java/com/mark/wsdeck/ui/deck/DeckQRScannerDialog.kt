package com.mark.wsdeck.ui.deck

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * App 內建即時相機掃描：不用先開系統相機拍照存圖，鏡頭直接對準朋友出示
 * 的畫面就能掃到，對應 iOS 的 DeckQRScannerSheet。用 CameraX 抓畫面、
 * ZXing（已經是既有依賴，QR 產生也用它）逐幀解碼。
 */
@Composable
fun DeckQRScannerDialog(onDetect: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (hasPermission) {
                CameraPreviewWithAnalysis(onDetect = onDetect)
                Text(
                    "把朋友出示的 QR 對準鏡頭",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("沒有相機權限", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "請到系統設定開啟本 App 的相機權限才能掃描。",
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "取消", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(onDetect: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var didDetect by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val reader = MultiFormatReader().apply {
                    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    if (!didDetect) {
                        val text = decodeQr(imageProxy, reader)
                        if (text != null) {
                            didDetect = true
                            onDetect(text)
                        }
                    }
                    imageProxy.close()
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis,
                    )
                } catch (e: Exception) {
                    // 綁不了相機（例如模擬器沒鏡頭）就靜靜放棄，畫面留白不崩潰
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun decodeQr(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    val buffer = imageProxy.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    val source = PlanarYUVLuminanceSource(
        data, imageProxy.width, imageProxy.height, 0, 0, imageProxy.width, imageProxy.height, false,
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        reader.decodeWithState(bitmap).text
    } catch (e: NotFoundException) {
        null
    } finally {
        reader.reset()
    }
}
