package com.docscan.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.docscan.app.databinding.ItemPageBinding
import com.docscan.app.model.ScanPage
import java.io.File

class PageAdapter(
    private val context: Context,
    private val pages: List<ScanPage>
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    inner class PageViewHolder(val binding: ItemPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        val path = page.processedUri?.toString() ?: page.originalUri.toString()
        val file = File(path)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.binding.imagePage.setImageBitmap(bitmap)
        }
        holder.binding.textPageNumber.text = "${position + 1}"
    }

    override fun getItemCount(): Int = pages.size
}