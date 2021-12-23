package com.punchlab.punchclassifier.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.RecyclerView
import com.punchlab.punchclassifier.R
import com.punchlab.punchclassifier.data.Punch

const val TAG = "PunchApplication"

class PunchListAdapter(val context: Context, punchList: List<Punch>
) : RecyclerView.Adapter<PunchListAdapter.PunchViewHolder>() {

    val data = punchList

    class PunchViewHolder(view: View): RecyclerView.ViewHolder(view){
        val punchImage: ImageView = view.findViewById(R.id.image_punch)
        val punchName: TextView = view.findViewById(R.id.text_name)
        val punchDuration: TextView = view.findViewById(R.id.text_duration)
        val punchQuality: TextView = view.findViewById(R.id.text_quality)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {

        val adapterLayout = LayoutInflater.from(parent.context).inflate(
            R.layout.punch_item, parent, false)

        return PunchViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
        val punch = data[position]
        Log.d(TAG, "Binding $punch")
        val imageResource = when (punch.punchTypeIndex) {
            1 -> R.drawable.punch_1
            2 -> R.drawable.punch_2
            3 -> R.drawable.punch_3
            4 -> R.drawable.punch_4
            5 -> R.drawable.punch_5
            6 -> R.drawable.punch_6
            else -> R.drawable.punch_0
        }

        val src = BitmapFactory.decodeResource(context.resources, imageResource)
        val roundedDrawable = RoundedBitmapDrawableFactory.create(context.resources, src)
        roundedDrawable.cornerRadius = src.width / 2f
        holder.punchImage.setImageDrawable(roundedDrawable)
        holder.punchName.text = punch.name

        val resources = context.resources
        holder.punchDuration.text =
            resources?.getString(R.string.duration, punch.duration.toString())
        holder.punchQuality.text =
            resources?.getString(R.string.quality, punch.quality.toString())
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }
}