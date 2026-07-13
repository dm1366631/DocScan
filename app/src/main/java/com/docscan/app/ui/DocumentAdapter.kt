package com.docscan.app.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.docscan.app.R
import com.docscan.app.databinding.ItemDocumentBinding
import com.docscan.app.model.ScanDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentAdapter(
    private val documents: MutableList<ScanDocument>,
    private val onItemClick: (ScanDocument) -> Unit,
    private val onDeleteClick: (ScanDocument) -> Unit,
    private val onShareClick: (ScanDocument) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    inner class DocViewHolder(val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocViewHolder, position: Int) {
        val doc = documents[position]
        with(holder.binding) {
            textDocName.text = doc.name
            textPageCount.text = root.context.getString(R.string.pages_format, doc.pages.size)
            textDate.text = dateFormat.format(Date(doc.createdAt))

            // Load thumbnail from first page
            val thumbPath = doc.pages.firstOrNull()?.processedUri?.toString()
                ?: doc.pages.firstOrNull()?.originalUri?.toString()
            if (thumbPath != null) {
                val file = File(thumbPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        imageThumb.setImageBitmap(bitmap)
                    } else {
                        imageThumb.setImageResource(R.drawable.ic_document)
                    }
                } else {
                    imageThumb.setImageResource(R.drawable.ic_document)
                }
            } else {
                imageThumb.setImageResource(R.drawable.ic_document)
            }

            root.setOnClickListener { onItemClick(doc) }
            btnDelete.setOnClickListener { onDeleteClick(doc) }
            btnShare.setOnClickListener { onShareClick(doc) }
        }
    }

    override fun getItemCount(): Int = documents.size

    fun updateData(newDocs: List<ScanDocument>) {
        documents.clear()
        documents.addAll(newDocs)
        notifyDataSetChanged()
    }
}