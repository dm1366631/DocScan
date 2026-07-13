package com.docscan.app.model

import android.net.Uri
import java.io.Serializable

data class ScanDocument(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val pages: MutableList<ScanPage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var pdfUri: Uri? = null
) : Serializable

data class ScanPage(
    val id: Long = System.currentTimeMillis(),
    val originalUri: Uri,
    val processedUri: Uri? = null,
    val thumbUri: Uri? = null
) : Serializable

enum class FilterType(val displayName: String) {
    ORIGINAL("原图"),
    GRAYSCALE("灰度"),
    BLACK_WHITE("黑白"),
    SHARPEN("锐化"),
    AUTO_ENHANCE("自动增强")
}