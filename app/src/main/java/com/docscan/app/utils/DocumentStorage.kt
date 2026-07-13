package com.docscan.app.utils

import android.content.Context
import android.graphics.BitmapFactory
import com.docscan.app.model.ScanDocument
import com.docscan.app.model.ScanPage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object DocumentStorage {

    private const val DOCS_DIR = "documents"
    private const val SCANS_DIR = "scans"
    private const val THUMBS_DIR = "thumbs"
    private const val PDFS_DIR = "pdfs"
    private const val META_FILE = "documents.json"

    private val gson = Gson()

    private fun getMetaFile(context: Context): File {
        return File(context.filesDir, "$DOCS_DIR/$META_FILE")
    }

    private fun ensureDirs(context: Context) {
        File(context.filesDir, DOCS_DIR).mkdirs()
        File(context.filesDir, SCANS_DIR).mkdirs()
        File(context.filesDir, THUMBS_DIR).mkdirs()
        File(context.filesDir, PDFS_DIR).mkdirs()
    }

    fun saveDocument(context: Context, document: ScanDocument) {
        ensureDirs(context)
        val docs = getAllDocuments(context).toMutableList()
        val existingIndex = docs.indexOfFirst { it.id == document.id }
        if (existingIndex >= 0) {
            docs[existingIndex] = document
        } else {
            docs.add(0, document)
        }
        val json = gson.toJson(docs)
        getMetaFile(context).writeText(json)
    }

    fun getAllDocuments(context: Context): List<ScanDocument> {
        val file = getMetaFile(context)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<ScanDocument>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDocument(context: Context, documentId: Long): ScanDocument? {
        return getAllDocuments(context).find { it.id == documentId }
    }

    fun deleteDocument(context: Context, documentId: Long) {
        val docs = getAllDocuments(context).filter { it.id != documentId }
        val json = gson.toJson(docs)
        getMetaFile(context).writeText(json)
    }

    fun saveScanImage(context: Context, bitmap: android.graphics.Bitmap, docId: Long, pageId: Long): String {
        ensureDirs(context)
        val file = File(context.filesDir, "$SCANS_DIR/${docId}_${pageId}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file.absolutePath
    }

    fun saveThumbImage(context: Context, bitmap: android.graphics.Bitmap, docId: Long, pageId: Long): String {
        ensureDirs(context)
        // Resize for thumbnail
        val maxSize = 400
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        val thumb = android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
        val file = File(context.filesDir, "$THUMBS_DIR/${docId}_${pageId}.jpg")
        file.outputStream().use { out ->
            thumb.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        }
        thumb.recycle()
        return file.absolutePath
    }

    fun loadBitmap(context: Context, path: String): android.graphics.Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun getPdfDir(context: Context): File {
        ensureDirs(context)
        return File(context.filesDir, PDFS_DIR)
    }
}