package com.docscan.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.docscan.app.R
import com.docscan.app.databinding.ActivityPdfViewerBinding
import com.docscan.app.model.ScanDocument
import com.docscan.app.model.ScanPage
import com.docscan.app.utils.DocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var document: ScanDocument? = null
    private lateinit var pageAdapter: PageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val docId = intent.getLongExtra(EXTRA_DOC_ID, -1)
        if (docId == -1L) {
            finish()
            return
        }

        loadDocument(docId)
    }

    private fun loadDocument(docId: Long) {
        lifecycleScope.launch {
            document = withContext(Dispatchers.IO) {
                DocumentStorage.getDocument(this@PdfViewerActivity, docId)
            }

            document?.let { doc ->
                binding.textDocTitle.text = doc.name
                binding.textPageInfo.text = getString(R.string.pages_format, doc.pages.size)

                pageAdapter = PageAdapter(
                    context = this@PdfViewerActivity,
                    pages = doc.pages,
                    onReorder = { /* TODO: drag to reorder */ }
                )
                binding.recyclerViewPages.layoutManager =
                    LinearLayoutManager(this@PdfViewerActivity, LinearLayoutManager.HORIZONTAL, false)
                binding.recyclerViewPages.adapter = pageAdapter
            } ?: run {
                Toast.makeText(this@PdfViewerActivity, R.string.document_not_found, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_share -> {
                sharePdf()
                true
            }
            R.id.action_delete -> {
                document?.let { doc ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle(R.string.delete_title)
                        .setMessage(R.string.delete_message)
                        .setPositiveButton(R.string.delete_confirm) { _, _ ->
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    DocumentStorage.deleteDocument(this@PdfViewerActivity, doc.id)
                                }
                                Toast.makeText(this@PdfViewerActivity, R.string.document_deleted, Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sharePdf() {
        val pdfPath = document?.pdfUri?.toString()
        if (pdfPath == null) {
            Toast.makeText(this, R.string.no_pdf, Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(pdfPath)
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

    companion object {
        const val EXTRA_DOC_ID = "doc_id"

        fun newIntent(activity: AppCompatActivity, docId: Long): Intent {
            return Intent(activity, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_DOC_ID, docId)
            }
        }
    }
}