package com.docscan.app.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.docscan.app.R
import com.docscan.app.databinding.ActivityScannerBinding
import com.docscan.app.processing.ImageProcessor
import kotlinx.coroutines.launch

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraManager: CameraManager
    private var capturedBitmaps = mutableListOf<Bitmap>()
    private var isCapturing = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        if (cameraManager.hasCameraPermission()) {
            startCamera()
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun setupViews() {
        cameraManager = CameraManager(
            context = this,
            lifecycleOwner = this,
            previewView = binding.previewView,
            onImageCaptured = { bitmap ->
                capturedBitmaps.add(bitmap)
                binding.btnPageCount.text = getString(R.string.page_count, capturedBitmaps.size)
                binding.btnPageCount.visibility = View.VISIBLE
                isCapturing = false
            }
        )

        binding.btnCapture.setOnClickListener {
            if (!isCapturing) {
                isCapturing = true
                binding.shutterOverlay.visibility = View.VISIBLE
                binding.shutterOverlay.postDelayed({
                    binding.shutterOverlay.visibility = View.GONE
                }, 150)
                cameraManager.captureImage() {
                    isCapturing = false
                }
            }
        }

        binding.btnFlash.setOnClickListener {
            val isOn = cameraManager.toggleFlash()
            binding.btnFlash.setImageResource(
                if (isOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }

        if (!cameraManager.isFlashSupported()) {
            binding.btnFlash.visibility = View.GONE
        }

        binding.btnDone.setOnClickListener {
            if (capturedBitmaps.isEmpty()) {
                Toast.makeText(this, R.string.capture_at_least_one, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDoneDialog()
        }

        binding.btnPageCount.setOnClickListener {
            if (capturedBitmaps.isNotEmpty()) {
                showDoneDialog()
            }
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun startCamera() {
        cameraManager.startCamera()
    }

    private fun showDoneDialog() {
        val items = arrayOf(
            getString(R.string.done_save),
            getString(R.string.done_add_more),
            getString(R.string.done_discard)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.done_title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> returnResults()
                    1 -> { /* Continue scanning, do nothing */ }
                    2 -> {
                        capturedBitmaps.clear()
                        binding.btnPageCount.visibility = View.GONE
                        Toast.makeText(this, R.string.discarded, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun returnResults() {
        val intent = Intent().apply {
            putExtra("page_count", capturedBitmaps.size)
            // Store bitmaps in a static holder for the calling activity
            CapturedImagesHolder.images = ArrayList(capturedBitmaps)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }

    companion object {
        fun newIntent(activity: AppCompatActivity) =
            Intent(activity, ScannerActivity::class.java)
    }
}

object CapturedImagesHolder {
    var images: ArrayList<Bitmap> = ArrayList()
    fun clear() { images.clear() }
}