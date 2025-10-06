package ssk.safesms.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ssk.safesms.data.model.SmsThread
import ssk.safesms.databinding.ItemSmsThreadBinding
import java.text.SimpleDateFormat
import java.util.*

class SmsThreadAdapter(
    private val onThreadClick: (SmsThread) -> Unit
) : ListAdapter<SmsThread, SmsThreadAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSmsThreadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSmsThreadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: SmsThread) {
            binding.tvAddress.text = thread.address
            binding.tvSnippet.text = thread.snippet
            binding.tvDate.text = formatDate(thread.date)

            binding.root.setOnClickListener {
                onThreadClick(thread)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val now = Date()
            val diff = now.time - date.time

            return when {
                diff < 60000 -> "방금 전"
                diff < 3600000 -> "${diff / 60000}분 전"
                diff < 86400000 -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                }
                else -> {
                    SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SmsThread>() {
        override fun areItemsTheSame(oldItem: SmsThread, newItem: SmsThread): Boolean {
            return oldItem.threadId == newItem.threadId
        }

        override fun areContentsTheSame(oldItem: SmsThread, newItem: SmsThread): Boolean {
            return oldItem == newItem
        }
    }
}
