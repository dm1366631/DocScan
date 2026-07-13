package com.docscan.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import com.docscan.app.model.ScanDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfGenerator {

    suspend fun generatePdf(
        context: Context,
        document: ScanDocument
    ): String = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        document.pages.forEachIndexed { index, page ->
            val bitmap = android.graphics.BitmapFactory.decodeFile(page.processedUri?.toString()
                ?: page.originalUri.toString())

            if (bitmap != null) {
                // A4 size in points (72 dpi): 595 x 842
                val pageWidth = 595
                val pageHeight = 842

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                // Calculate scaling to fit the bitmap into the page with margins
                val margin = 36f // 0.5 inch margin
                val availableWidth = pageWidth - 2 * margin
                val availableHeight = pageHeight - 2 * margin

                val scaleX = availableWidth / bitmap.width
                val scaleY = availableHeight / bitmap.height
                val scale = minOf(scaleX, scaleY)

                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val left = (pageWidth - scaledWidth) / 2f
                val top = (pageHeight - scaledHeight) / 2f

                canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), null)
                pdfDocument.finishPage(pdfPage)
                bitmap.recycle()
            }
        }

        val fileName = "${document.name}_${System.currentTimeMillis()}.pdf"
        val file = File(context.filesDir, "pdfs/$fileName")
        file.parentFile?.mkdirs()

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        file.absolutePath
    }
}