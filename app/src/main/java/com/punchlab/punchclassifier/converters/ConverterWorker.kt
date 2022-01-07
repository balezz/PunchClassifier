package com.punchlab.punchclassifier.converters

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.punchlab.punchclassifier.KEY_VIDEO_URI
import com.punchlab.punchclassifier.THUMB_HEIGHT
import com.punchlab.punchclassifier.THUMB_WIDTH
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.VideoSample
import com.punchlab.punchclassifier.database.PersonPunchDatabase
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.nio.ByteBuffer

class ConverterWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val uriString = inputData.getString(KEY_VIDEO_URI)
        val uri = Uri.parse(uriString)
        val pfd:ParcelFileDescriptor?
        try {
            pfd = appContext.contentResolver.openFileDescriptor(uri, "r")!!
        } catch (e: Exception) {
            return Result.failure()
        }
        val converterPerson = VideoToPersonFrameConverter.getInstance(appContext)
        val personList = converterPerson.syncProcessing(pfd)

        Log.d(TAG, "Person list size: ${personList.size}")

        val converterPunch = PersonToPunchConverter.getInstance(appContext)
        val punchIndexes = converterPunch.convertPersonsToPunchIndices(personList)
        val punchList = converterPunch.convertIndicesToPunch()
        Log.d(TAG, "Punch Indexes: $punchIndexes")

        val imageBytes = getThumb(converterPerson.mThumbnail!!)
        insertInDatabase(uri, imageBytes, personList, punchList)
        return Result.success()
    }


    private fun insertInDatabase(uri: Uri, imageBytes: ByteArray,
                                 personList: List<Person>, punchList: List<Punch>) {
        runBlocking {
            val database = PersonPunchDatabase.getDatabase(applicationContext)
            val videoSampleDao = database.videoSampleDao()
            val personDao = database.personDao()
            val punchDao = database.punchDao()

            val videoSample = VideoSample(
                uri = uri.toString(),
                duration = 0,
                image = imageBytes
            )
            val videoSampleId = videoSampleDao.insert(videoSample)
            for (person in personList) {
                person.videoSampleId = videoSampleId
                personDao.insert(person)
            }
            for (punch in punchList) {
                punch.videoSampleId = videoSampleId
                punchDao.insert(punch)
            }
            Log.d(TAG, "Done insert person list in database")
        }
    }

    private fun getThumb(bitmap: Bitmap): ByteArray{
        val outStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
        return outStream.toByteArray()
    }

    companion object {
        const val TAG = "VideoToPersonWorker"
    }
}