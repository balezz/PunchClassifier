package com.punchlab.punchclassifier.ui

import android.media.MediaExtractor
import android.net.Uri
import androidx.lifecycle.*
import androidx.work.*
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.WORKER_TAG
import com.punchlab.punchclassifier.converters.ConverterWorker
import com.punchlab.punchclassifier.data.VideoSample
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class SharedViewModel(private val application: PunchApplication): ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    private val videoSampleDao = application.database.videoSampleDao()
    private val punchDao = application.database.punchDao()
    val outputWorkInfos = workManager.getWorkInfosByTagLiveData(WORKER_TAG)
    val allVideoSamples:
            LiveData<List<VideoSample>> = videoSampleDao.getVideoSamples().asLiveData()

    var punchList = MutableLiveData<List<Punch>>()

    private var currentVideoUri: String? = null

    fun setCurrentVideoUri(uriString: String){ currentVideoUri = uriString }

    fun startProcessing() {
        for (vs in allVideoSamples.value!!){
            if (vs.uri == currentVideoUri.toString()) return
        }
        startWorker()
    }


    private fun startWorker(){
        val inputData = createInputDataForUri()
        val videoToPunchRequest = OneTimeWorkRequest.Builder(ConverterWorker::class.java)
            .setInputData(inputData)
            .addTag(WORKER_TAG)
            .build()
        workManager.enqueue(videoToPunchRequest)
    }

    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        currentVideoUri?.let {
            builder.putString(KEY_VIDEO_URI, currentVideoUri.toString())
        }
        return builder.build()
    }

    fun getPunchListFromDatabase(videoId: Long) {
        viewModelScope.launch {
            punchList.value = punchDao.getPunchListByVideoId(videoId)
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


