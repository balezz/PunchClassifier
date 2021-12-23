package com.punchlab.punchclassifier.workers

import android.content.Context
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.converters.VideoToPersonConverter

class VideoToPersonWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val pfd = appContext.contentResolver.openFileDescriptor(
            Uri.parse(
                inputData.getString(KEY_VIDEO_URI)
            ), "r")
        val converter = VideoToPersonConverter(appContext)
        if (pfd != null) {
            converter.syncProcessing(pfd)
        }
        return Result.success()
    }
}