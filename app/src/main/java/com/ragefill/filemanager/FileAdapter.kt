package com.ragefill.filemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ragefill.filemanager.databinding.ItemFileBinding
import java.io.File

class FileAdapter(
    private val onClick: (File) -> Unit
) : ListAdapter<File, FileAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<File>() {
            override fun areItemsTheSame(old: File, new: File) = old.absolutePath == new.absolutePath
            override fun areContentsTheSame(old: File, new: File) = old.lastModified() == new.lastModified() && old.length() == new.length()
        }
    }

    inner class ViewHolder(val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File) {
            binding.tvName.text = file.name
            binding.tvMeta.text = if (file.isDirectory) "Folder" else "${file.length()} bytes"
            binding.root.setOnClickListener { onClick(file) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
