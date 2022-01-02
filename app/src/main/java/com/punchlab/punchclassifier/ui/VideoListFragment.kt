package com.punchlab.punchclassifier.ui

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.VideoSample
import com.punchlab.punchclassifier.databinding.FragmentVideoListBinding


class VideoListFragment : Fragment() {

    private var binding: FragmentVideoListBinding? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoList: List<VideoSample>

    private val sharedViewModel: SharedViewModel by activityViewModels{
        SharedViewModel.SharedViewModelFactory(activity?.application!! as PunchApplication)
    }

    private val getVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ activityResult ->
            val intent = activityResult.data
            intent?.let {
                val uri = it.data!!
                val action = VideoListFragmentDirections
                    .actionStartFragmentToPunchListFragment(uri.toString())
                findNavController().navigate(action)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding!!.videoRecyclerView

        val videoListObserver = Observer<List<VideoSample>> {
            videoList = sharedViewModel.allVideoSamples.value!!
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = VideoListAdapter(videoList){
                sharedViewModel.getPunchListFromDatabase(it.videoId)
                val action = VideoListFragmentDirections
                    .actionStartFragmentToPunchListFragment(it.uri)
                findNavController().navigate(action)
            }
        }
        sharedViewModel.allVideoSamples.observe(viewLifecycleOwner, videoListObserver)

        binding?.apply {
            viewModel = sharedViewModel
            fabRecord.setOnClickListener{ dispatchRecordVideoIntent() }
            fabOpen.setOnClickListener { dispatchOpenVideoIntent() }
        }
    }

    private fun dispatchOpenVideoIntent() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        getVideo.launch(intent)
    }

    private fun dispatchRecordVideoIntent() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 5)
        getVideo.launch(intent)
    }
}
