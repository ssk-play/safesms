package ssk.safesms.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ssk.safesms.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: SmsThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerView()
        observeViewModel()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.loadThreads()
    }

    private fun setupRecyclerView() {
        adapter = SmsThreadAdapter { thread ->
            navigateToConversation(thread.threadId, thread.address)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun navigateToConversation(threadId: Long, address: String) {
        val intent = android.content.Intent(requireContext(), ssk.safesms.ui.conversation.ConversationActivity::class.java)
        intent.putExtra("THREAD_ID", threadId)
        intent.putExtra("ADDRESS", address)
        startActivity(intent)
    }

    private fun observeViewModel() {
        homeViewModel.threads.observe(viewLifecycleOwner) { threads ->
            adapter.submitList(threads)
            binding.tvEmpty.isVisible = threads.isEmpty()
            binding.recyclerView.isVisible = threads.isNotEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}