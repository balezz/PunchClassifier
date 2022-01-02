package com.punchlab.punchclassifier.ui

import android.app.Application
import android.media.MediaExtractor
import android.net.Uri
import androidx.lifecycle.*
import androidx.work.*
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.converters.ConverterWorker
import com.punchlab.punchclassifier.data.VideoSample
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class SharedViewModel(private val application: Application): ViewModel() {

    private val workManager = WorkManager.getInstance(application)
    private val videoSampleDao = (application as PunchApplication).database.videoSampleDao()
    private val punchDao = (application as PunchApplication).database.punchDao()

    val allVideoSamples:
            LiveData<List<VideoSample>> = videoSampleDao.getVideoSamples().asLiveData()

    var punchList = MutableLiveData<List<Punch>>()

    private var _progress = MutableLiveData<Int>()
    val progress get() = _progress

    private var _notification = MutableLiveData("")
    val notification get() = _notification

    private var currentVideoUri: String? = null

    fun setCurrentVideoUri(uriString: String){ currentVideoUri = uriString }


    fun putVideoSampleInDatabase(uri: Uri): VideoSample {
        val videoSample = getNewVideoSample(uri)
        viewModelScope.launch { videoSampleDao.insert(videoSample) }
        return videoSample
    }

    // 0 - 100
    fun setProgress(progress: Int){
        _progress.postValue(progress)
    }

    fun startProcessing() {
        for (vs in allVideoSamples.value!!){
            if (vs.uri == currentVideoUri.toString()) return
        }
        startWorker()
    }

    fun makeToast(s: String) {
        _notification.postValue(s)
    }

    private fun startWorker(){
        val inputData = createInputDataForUri()
        val videoToPunchRequest = OneTimeWorkRequest.Builder(ConverterWorker::class.java)
            .setInputData(inputData)
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

    private fun getNewVideoSample(uri: Uri): VideoSample{
        val extractor = MediaExtractor()
        extractor.setDataSource(application.applicationContext, uri, null)
        extractor.selectTrack(0)
        val outputFormat = extractor.getTrackFormat(0)
        val frameTotalNumber = outputFormat.getInteger("frame-count")

        return VideoSample(
            uri = uri.toString(),
            duration = frameTotalNumber)
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


