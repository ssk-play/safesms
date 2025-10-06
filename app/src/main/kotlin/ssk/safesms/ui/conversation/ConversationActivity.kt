package ssk.safesms.ui.conversation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ssk.safesms.databinding.ActivityConversationBinding

class ConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationBinding
    private lateinit var viewModel: ConversationViewModel
    private lateinit var adapter: SmsMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val threadId = intent.getLongExtra("THREAD_ID", -1)
        val address = intent.getStringExtra("ADDRESS") ?: ""

        if (threadId == -1L) {
            finish()
            return
        }

        supportActionBar?.title = address

        viewModel = ViewModelProvider(this)[ConversationViewModel::class.java]

        setupRecyclerView()
        setupSendButton()
        observeViewModel()

        viewModel.loadMessages(threadId)
    }

    private fun setupRecyclerView() {
        adapter = SmsMessageAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConversationActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ConversationActivity.adapter
        }
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotEmpty()) {
                val address = intent.getStringExtra("ADDRESS") ?: return@setOnClickListener
                viewModel.sendMessage(address, message)
                binding.etMessage.text?.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }
}
