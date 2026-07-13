package com.docscan.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.docscan.app.R
import com.docscan.app.databinding.ActivityPreviewBinding
import com.docscan.app.model.FilterType
import com.docscan.app.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var sourceBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var currentFilter = FilterType.ORIGINAL
    private var edgePoints: List<PointF> = emptyList()
    private var isCropped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            finish()
            return
        }

        sourceBitmap = loadBitmap(imagePath)
        if (sourceBitmap == null) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        displayBitmap(sourceBitmap!!)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Filter chips
        binding.filterOriginal.setOnClickListener { applyFilter(FilterType.ORIGINAL) }
        binding.filterGrayscale.setOnClickListener { applyFilter(FilterType.GRAYSCALE) }
        binding.filterBw.setOnClickListener { applyFilter(FilterType.BLACK_WHITE) }
        binding.filterSharpen.setOnClickListener { applyFilter(FilterType.SHARPEN) }
        binding.filterAuto.setOnClickListener { applyFilter(FilterType.AUTO_ENHANCE) }

        binding.btnDetect.setOnClickListener {
            detectAndCrop()
        }

        binding.btnConfirm.setOnClickListener {
            currentBitmap?.let { bmp ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_RESULT_PATH, saveTempBitmap(bmp))
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        binding.btnRetake.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun displayBitmap(bitmap: Bitmap) {
        currentBitmap = bitmap
        binding.imageView.setImageBitmap(bitmap)
        binding.imageView.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun applyFilter(filterType: FilterType) {
        currentFilter = filterType
        updateFilterChips()

        val bitmap = if (isCropped) currentBitmap else sourceBitmap
        bitmap ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val filtered = ImageProcessor.applyFilter(bitmap, filterType)
            displayBitmap(filtered)
        }
    }

    private fun detectAndCrop() {
        val bitmap = sourceBitmap ?: return
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                edgePoints = ImageProcessor.detectDocumentEdges(bitmap)
                if (edgePoints.isNotEmpty() && !isDefaultCorners(edgePoints, bitmap)) {
                    val cropped = ImageProcessor.perspectiveTransform(bitmap, edgePoints)
                    isCropped = true
                    displayBitmap(cropped)
                    Toast.makeText(this@PreviewActivity, R.string.crop_success, Toast.LENGTH_SHORT).show()
                } else {
                    displayBitmap(bitmap)
                    Toast.makeText(this@PreviewActivity, R.string.no_edges_detected, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                displayBitmap(bitmap)
                Toast.makeText(this@PreviewActivity, R.string.crop_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isDefaultCorners(points: List<PointF>, bitmap: Bitmap): Boolean {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val tolerance = 20f
        return points.size == 4 &&
                points[0].x < tolerance && points[0].y < tolerance &&
                points[1].x > w - tolerance && points[1].y < tolerance &&
                points[2].x > w - tolerance && points[2].y > h - tolerance &&
                points[3].x < tolerance && points[3].y > h - tolerance
    }

    private fun updateFilterChips() {
        val chips = listOf(
            binding.filterOriginal to FilterType.ORIGINAL,
            binding.filterGrayscale to FilterType.GRAYSCALE,
            binding.filterBw to FilterType.BLACK_WHITE,
            binding.filterSharpen to FilterType.SHARPEN,
            binding.filterAuto to FilterType.AUTO_ENHANCE
        )
        chips.forEach { (chip, filter) ->
            chip.isActivated = filter == currentFilter
        }
    }

    private fun loadBitmap(path: String): Bitmap? {
        val file = java.io.File(path)
        if (!file.exists()) return null
        // Sample down large images
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val maxSize = 2000
        var inSampleSize = 1
        if (options.outHeight > maxSize || options.outWidth > maxSize) {
            inSampleSize = Math.min(
                options.outHeight / maxSize,
                options.outWidth / maxSize
            ).coerceAtLeast(1)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun saveTempBitmap(bitmap: Bitmap): String {
        val file = java.io.File(cacheDir, "preview_result_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file.absolutePath
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_RESULT_PATH = "result_path"

        fun newIntent(activity: AppCompatActivity, imagePath: String): Intent {
            return Intent(activity, PreviewActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }
        }
    }
}