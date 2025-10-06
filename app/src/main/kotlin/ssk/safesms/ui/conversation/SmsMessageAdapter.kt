package ssk.safesms.ui.conversation

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ssk.safesms.R
import ssk.safesms.data.model.SmsMessage
import ssk.safesms.databinding.ItemSmsMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class SmsMessageAdapter : ListAdapter<SmsMessage, SmsMessageAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSmsMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSmsMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SmsMessage) {
            binding.tvMessage.text = message.body
            binding.tvTime.text = formatTime(message.date)

            val layoutParams = binding.tvMessage.layoutParams as ViewGroup.MarginLayoutParams

            if (message.type == SmsMessage.TYPE_SENT) {
                // 보낸 메시지 (오른쪽 정렬)
                (binding.tvMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    horizontalBias = 1f
                }
                binding.tvMessage.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_blue_light)
                )
                layoutParams.marginStart = 80
                layoutParams.marginEnd = 0
            } else {
                // 받은 메시지 (왼쪽 정렬)
                (binding.tvMessage.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    horizontalBias = 0f
                }
                binding.tvMessage.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                )
                layoutParams.marginStart = 0
                layoutParams.marginEnd = 80
            }
        }

        private fun formatTime(timestamp: Long): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SmsMessage>() {
        override fun areItemsTheSame(oldItem: SmsMessage, newItem: SmsMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SmsMessage, newItem: SmsMessage): Boolean {
            return oldItem == newItem
        }
    }
}
