package ssk.kidssms.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ssk.kidssms.data.model.SmsThread
import ssk.kidssms.data.repository.SmsRepository

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _threads = MutableLiveData<List<SmsThread>>()
    val threads: LiveData<List<SmsThread>> = _threads

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadThreads() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            val threadList = repository.getAllThreads()
            _threads.postValue(threadList)
            _isLoading.postValue(false)
        }
    }
}