package ssk.safesms.ui.conversation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ssk.safesms.data.model.SmsMessage
import ssk.safesms.data.repository.SmsRepository

class ConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _messages = MutableLiveData<List<SmsMessage>>()
    val messages: LiveData<List<SmsMessage>> = _messages

    private var currentThreadId: Long = -1

    fun loadMessages(threadId: Long) {
        currentThreadId = threadId
        viewModelScope.launch(Dispatchers.IO) {
            val messageList = repository.getMessagesInThread(threadId)
            _messages.postValue(messageList)
        }
    }

    fun sendMessage(phoneNumber: String, message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.sendSms(phoneNumber, message)
            if (success && currentThreadId != -1L) {
                // Reload messages after sending
                kotlinx.coroutines.delay(1000) // Wait for SMS to be saved
                loadMessages(currentThreadId)
            }
        }
    }
}
