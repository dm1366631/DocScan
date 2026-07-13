package com.docscan.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.docscan.app.R
import com.docscan.app.camera.CapturedImagesHolder
import com.docscan.app.camera.ScannerActivity
import com.docscan.app.databinding.ActivityMainBinding
import com.docscan.app.model.ScanDocument
import com.docscan.app.model.ScanPage
import com.docscan.app.processing.ImageProcessor
import com.docscan.app.processing.PdfGenerator
import com.docscan.app.utils.DocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DocumentAdapter
    private val documents = mutableListOf<ScanDocument>()

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pageCount = result.data?.getIntExtra("page_count", 0) ?: 0
            if (pageCount > 0) {
                processCapturedImages()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        loadDocuments()
    }

    override fun onResume() {
        super.onResume()
        loadDocuments()
    }

    private fun setupRecyclerView() {
        adapter = DocumentAdapter(
            documents = documents,
            onItemClick = { doc -> openPdfViewer(doc) },
            onDeleteClick = { doc -> confirmDelete(doc) },
            onShareClick = { doc -> shareDocument(doc) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabScan.setOnClickListener {
            val intent = ScannerActivity.newIntent(this)
            scanLauncher.launch(intent)
        }
    }

    private fun loadDocuments() {
        lifecycleScope.launch {
            val docs = withContext(Dispatchers.IO) {
                DocumentStorage.getAllDocuments(this@MainActivity)
            }
            documents.clear()
            documents.addAll(docs)
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    private fun processCapturedImages() {
        val images = CapturedImagesHolder.images
        if (images.isEmpty()) return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.fabScan.isEnabled = false

            try {
                val doc = ScanDocument(
                    name = "文档_${System.currentTimeMillis().toString().takeLast(6)}"
                )

                withContext(Dispatchers.Default) {
                    images.forEach { bitmap ->
                        val page = ScanPage(
                            id = System.currentTimeMillis(),
                            originalUri = Uri.parse(
                                DocumentStorage.saveScanImage(
                                    this@MainActivity, bitmap, doc.id, System.currentTimeMillis()
                                )
                            )
                        )
                        val processed = ImageProcessor.autoEnhance(bitmap)
                        val processedPath = ImageProcessor.saveBitmap(
                            this@MainActivity, processed, "${doc.id}_${page.id}_processed"
                        )
                        page.processedUri = Uri.parse(processedPath)
                        doc.pages.add(page)
                        processed.recycle()
                    }
                }

                val pdfPath = withContext(Dispatchers.IO) {
                    PdfGenerator.generatePdf(this@MainActivity, doc)
                }
                doc.pdfUri = Uri.parse(pdfPath)

                withContext(Dispatchers.IO) {
                    DocumentStorage.saveDocument(this@MainActivity, doc)
                }

                loadDocuments()
                Toast.makeText(this@MainActivity, R.string.document_saved, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.error_saving, Toast.LENGTH_SHORT).show()
            } finally {
                CapturedImagesHolder.clear()
                binding.progressBar.visibility = View.GONE
                binding.fabScan.isEnabled = true
            }
        }
    }

    private fun openPdfViewer(doc: ScanDocument) {
        val intent = PdfViewerActivity.newIntent(this, doc.id)
        startActivity(intent)
    }

    private fun confirmDelete(doc: ScanDocument) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_message)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        DocumentStorage.deleteDocument(this@MainActivity, doc.id)
                    }
                    loadDocuments()
                    Toast.makeText(this@MainActivity, R.string.document_deleted, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun shareDocument(doc: ScanDocument) {
        val pdfPath = doc.pdfUri?.toString()
        if (pdfPath == null) {
            Toast.makeText(this, R.string.no_pdf, Toast.LENGTH_SHORT).show()
            return
        }

        val file = java.io.File(pdfPath)
        if (!file.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_pdf)))
    }

    private fun updateEmptyState() {
        binding.emptyState.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (documents.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(getString(R.string.about_message, "1.0.0"))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}