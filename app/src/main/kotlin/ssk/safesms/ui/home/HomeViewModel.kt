package ssk.safesms.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ssk.safesms.data.model.SmsThread
import ssk.safesms.data.repository.SmsRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _threads = MutableLiveData<List<SmsThread>>()
    val threads: LiveData<List<SmsThread>> = _threads

    fun loadThreads() {
        viewModelScope.launch(Dispatchers.IO) {
            val threadList = repository.getAllThreads()
            _threads.postValue(threadList)
        }
    }
}