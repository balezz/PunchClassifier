package com.punchlab.punchclassifier.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.punchlab.punchclassifier.R
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.PunchClass


class PunchListAdapter(val context: Context) :
    ListAdapter<Punch, PunchListAdapter.PunchViewHolder>(DiffCallback) {

    class PunchViewHolder(view: View): RecyclerView.ViewHolder(view){
        val punchImage: ImageView = view.findViewById(R.id.image_punch)
        val punchName: TextView = view.findViewById(R.id.punch_name)
        val punchDuration: TextView = view.findViewById(R.id.punch_duration)
        val punchQuality: TextView = view.findViewById(R.id.text_quality)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.punch_item, parent, false)

        return PunchViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
        val punch = getItem(position)
        val imageResource = when (punch.punchTypeIndex) {
            1 -> R.drawable.p1_jab
            2 -> R.drawable.p2_cross
            3 -> R.drawable.p3_l_hook
            4 -> R.drawable.p4_r_hook
            5 -> R.drawable.p5_l_upper
            6 -> R.drawable.p6_r_upper
            else -> R.drawable.icon
        }

        val src = BitmapFactory.decodeResource(context.resources, imageResource)
        val roundedDrawable = RoundedBitmapDrawableFactory.create(context.resources, src)
        roundedDrawable.cornerRadius = src.width / 2f
        holder.punchImage.setImageDrawable(roundedDrawable)
        holder.punchName.text = PunchClass.fromInt(punch.punchTypeIndex).toString()

        val resources = context.resources
        holder.punchDuration.text =
            resources?.getString(R.string.duration, punch.duration.toString())
        holder.punchQuality.text =
            resources?.getString(R.string.quality, punch.quality.toString())
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Punch>(){
            override fun areItemsTheSame(oldItem: Punch, newItem: Punch): Boolean {
                return oldItem.punchId == newItem.punchId
            }

            override fun areContentsTheSame(oldItem: Punch, newItem: Punch): Boolean {
                return oldItem.punchTypeIndex == newItem.punchTypeIndex
            }
        }
    }

}