package com.punchlab.punchclassifier.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.lang.Exception

class VideoListAdapter(
    private val onItemClicked: (VideoSample) -> Unit) :
     ListAdapter<VideoSample, VideoListAdapter.VideoSampleViewHolder>(DiffCallback){

    class VideoSampleViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val videoImage: ImageView = view.findViewById(R.id.image_video_sample)
        val videoName: TextView = view.findViewById(R.id.video_name)
        val videoDuration: TextView = view.findViewById(R.id.video_duration)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoSampleViewHolder {
        val adapterLayout = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_video_sample, parent, false)
        return VideoSampleViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: VideoSampleViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener{ onItemClicked(current) }
        holder.videoName.text = current.uri
        holder.videoDuration.text = current.duration.toString()
        val bitmap = BitmapFactory.decodeByteArray(current.image, 0, current.image.size)
        holder.videoImage.setImageBitmap(bitmap)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<VideoSample>(){
            override fun areItemsTheSame(oldItem: VideoSample, newItem: VideoSample): Boolean {
                return oldItem.videoId == newItem.videoId
            }

            override fun areContentsTheSame(oldItem: VideoSample, newItem: VideoSample): Boolean {
                return oldItem.uri == newItem.uri
            }
        }
    }


}