package com.fuellogger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.fuellogger.databinding.ActivityCameraBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_RECEIPT = "receipt"
        const val MODE_ODOMETER = "odometer"
        const val MODE_BOTH = "both"
        const val EXTRA_ENTRY_JSON = "entry_json"
        const val EXTRA_SCANNED_TEXT = "scanned_text"
        private const val TAG = "CameraActivity"
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var mode = MODE_RECEIPT
    private var existingEntryJson: String? = null

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_RECEIPT
        existingEntryJson = intent.getStringExtra(EXTRA_ENTRY_JSON)

        binding.tvInstruction.text = when (mode) {
            MODE_RECEIPT -> "Point camera at fuel receipt"
            MODE_ODOMETER -> "Point camera at odometer display"
            else -> "Point camera at receipt"
        }

        binding.btnCapture.setOnClickListener { capturePhoto() }
        binding.btnClose.setOnClickListener { finish() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        binding.btnCapture.isEnabled = false
        binding.tvInstruction.text = "Processing..."

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            imageProxy.close()
                            onTextRecognized(visionText.text)
                        }
                        .addOnFailureListener { e ->
                            imageProxy.close()
                            Toast.makeText(this@CameraActivity, "OCR failed: ${e.message}", Toast.LENGTH_LONG).show()
                            binding.btnCapture.isEnabled = true
                            binding.tvInstruction.text = when (mode) {
                                MODE_RECEIPT -> "Point camera at fuel receipt"
                                else -> "Point camera at odometer display"
                            }
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                    runOnUiThread {
                        binding.btnCapture.isEnabled = true
                        Toast.makeText(this@CameraActivity, "Capture failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun onTextRecognized(text: String) {
        Log.d(TAG, "OCR text: $text")

        // Parse based on mode
        val entry = when (mode) {
            MODE_RECEIPT -> {
                OcrParser.parseReceiptText(text)
            }
            MODE_ODOMETER -> {
                val existing = existingEntryJson?.let { FuelEntry.fromJson(it) } ?: FuelEntry()
                val (odo, trip) = OcrParser.parseOdometerText(text)
                val temp = OcrParser.parseTemperature(text)
                existing.odometer = if (odo > 0) odo else existing.odometer
                existing.tripMiles = if (trip > 0) trip else existing.tripMiles
                existing.temperature = if (temp > 0) temp else existing.temperature
                existing
            }
            else -> OcrParser.parseReceiptText(text)
        }

        // Launch review screen
        val intent = Intent(this, ReviewActivity::class.java).apply {
            putExtra(ReviewActivity.EXTRA_ENTRY, entry.toJson())
            putExtra(ReviewActivity.EXTRA_MODE, mode)
            putExtra(ReviewActivity.EXTRA_RAW_TEXT, text)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
