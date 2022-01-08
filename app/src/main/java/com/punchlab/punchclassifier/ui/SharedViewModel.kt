package com.punchlab.punchclassifier.ui

import android.net.Uri
import androidx.lifecycle.*
import androidx.work.*
import com.google.common.util.concurrent.ListenableFuture
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.converters.ConverterWorker
import com.punchlab.punchclassifier.data.VideoSample
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import kotlin.coroutines.coroutineContext

class SharedViewModel(private val application: PunchApplication): ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    private val videoSampleDao = application.database.videoSampleDao()
    private val punchDao = application.database.punchDao()
    lateinit var outputWorkInfo: LiveData<WorkInfo>

    val allVideoSamples:
            LiveData<List<VideoSample>> = videoSampleDao.getVideoSamples().asLiveData()

    var punchList = MutableLiveData<List<Punch>>()

//    private var currentVideoUri: String? = null
//    fun setCurrentVideoUri(uriString: String){ currentVideoUri = uriString }

    fun startProcessing(uri: Uri) {
        val uriString = uri.toString()
        for (vs in allVideoSamples.value!!){
            if (vs.uri == uriString) return
        }
        startWorker(uri)
    }


    private fun startWorker(uri: Uri){
        val inputData = createInputDataForUri(uri)
        val videoToPunchRequest = OneTimeWorkRequest
            .Builder(ConverterWorker::class.java)
            .setInputData(inputData)
            .build()
        outputWorkInfo = workManager.getWorkInfoByIdLiveData(videoToPunchRequest.id)
        workManager.enqueue(videoToPunchRequest)
    }

    private fun createInputDataForUri(uri: Uri): Data {
        val builder = Data.Builder()
        builder.putString(KEY_VIDEO_URI, uri.toString())
        return builder.build()
    }

//    fun getPunchListFromDatabase(videoId: Long) {
//        viewModelScope.launch {
//            punchList.value = punchDao.getPunchListByVideoId(videoId)
//        }
//    }

    fun stopProcessing() {
        try {
            workManager.cancelAllWork()
        } catch (e: Exception){
            e.printStackTrace()
            return
        }
    }

    fun setPunchListByUri(uriString: String) {
        viewModelScope.launch {
            val videoSample = videoSampleDao.getVideoSampleByUri(uriString)
            videoSample.let {
                punchList.value = punchDao.getPunchListByVideoId(videoSample.videoId)
            }
        }
    }

    companion object {
        private const val TAG = "SharedViewModel"

    }

    class SharedViewModelFactory(private val application: PunchApplication): ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
                SharedViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}


