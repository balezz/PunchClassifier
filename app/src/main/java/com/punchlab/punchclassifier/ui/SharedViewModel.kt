package com.punchlab.punchclassifier.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.converters.VideoToPersonConverter
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.workers.VideoToPersonWorker
import java.lang.IllegalArgumentException

class SharedViewModel(application: Application): ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    val videoProcessor = VideoToPersonConverter.getInstance(application)

    private var _punchList = MutableLiveData<List<Punch>>()
    val punchList get() = _punchList

    private var _progress = MutableLiveData<Int>()
    val progress get() = _progress

    private var _notification = MutableLiveData<String>("")
    val notification get() = _notification

    private var currentVideoUri: Uri? = null
    fun setCurrentVideoUri(uri: Uri?) {
        currentVideoUri = uri
    }

    fun setProgress(progress: Int){
        _progress.postValue(progress)
    }

    fun startProcessing() {
        _punchList.value = mutableListOf()
        val inputData = createInputDataForUri()
        val videoToPunchRequest = OneTimeWorkRequest.Builder(VideoToPersonWorker::class.java)
            .setInputData(inputData)
            .build()
        val continuation = workManager.beginWith(videoToPunchRequest)
        continuation.enqueue()
    }

    fun makeToast(s: String) {
        _notification.postValue(s)
    }

    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        currentVideoUri?.let {
            builder.putString(KEY_VIDEO_URI, currentVideoUri.toString())
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "SharedViewModel"

    }

    class SharedViewModelFactory(private val application: Application): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                SharedViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}


