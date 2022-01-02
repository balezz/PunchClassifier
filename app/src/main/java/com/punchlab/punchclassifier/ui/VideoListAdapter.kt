package com.punchlab.punchclassifier.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.punchlab.punchclassifier.R
import com.punchlab.punchclassifier.data.VideoSample
import com.punchlab.punchclassifier.databinding.ItemVideoSampleBinding
import java.lang.Exception

class VideoListAdapter(
    private val videoList: List<VideoSample>,
    private val onItemClicked: (VideoSample) -> Unit) :
     RecyclerView.Adapter<VideoListAdapter.VideoSampleViewHolder>(){

    class VideoSampleViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val videoImage: ImageView = view.findViewById(R.id.image_video_sample)
        val videoName: TextView = view.findViewById(R.id.video_name)
        val videoDuration: TextView = view.findViewById(R.id.video_duration)

        private fun getThumbnail(uriString: String): Bitmap? {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
                val mediaMetadataRetriever = MediaMetadataRetriever()
                return try {
                    mediaMetadataRetriever.setDataSource(uriString, null)
                    mediaMetadataRetriever.getFrameAtIndex(42)
                } catch (e: Exception) {
                    null
                } finally {
                    mediaMetadataRetriever.release()
                }
            }
            else {
                val uri = Uri.parse(uriString)
                return ThumbnailUtils.createVideoThumbnail(
                    uri.path!!, MediaStore.Images.Thumbnails.MINI_KIND)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoSampleViewHolder {
        val adapterLayout = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_video_sample, parent, false)
        return VideoSampleViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: VideoSampleViewHolder, position: Int) {
        val current = videoList[position]
        holder.itemView.setOnClickListener{ onItemClicked(current) }
        holder.videoName.text = current.uri
        holder.videoDuration.text = current.duration.toString()
    }

    override fun getItemCount(): Int {
        return videoList.size
    }



}