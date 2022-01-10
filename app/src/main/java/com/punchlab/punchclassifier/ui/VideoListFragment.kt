package com.punchlab.punchclassifier.ui

import android.app.AlertDialog
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
import androidx.work.WorkInfo
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.R
import com.punchlab.punchclassifier.VIDEO_LIMIT_S
import com.punchlab.punchclassifier.data.VideoSample
import com.punchlab.punchclassifier.databinding.FragmentVideoListBinding


class VideoListFragment : Fragment() {

    private lateinit var binding: FragmentVideoListBinding
    private lateinit var alertDialog : AlertDialog
    private var currentUriString = ""

    private val sharedViewModel: SharedViewModel by activityViewModels{
        SharedViewModel.SharedViewModelFactory(activity?.application!! as PunchApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        binding = FragmentVideoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = VideoListAdapter{
            val action = VideoListFragmentDirections
                .actionStartFragmentToPunchListFragment(it.uri)
            findNavController().navigate(action)
        }
        binding.videoRecyclerView.adapter = adapter
        binding.videoRecyclerView.layoutManager = LinearLayoutManager(context)

        val videoListObserver = Observer<List<VideoSample>> {
            adapter.submitList(it)
        }
        sharedViewModel.videoSamplesList.observe(viewLifecycleOwner, videoListObserver)

        val builder = activity.let { AlertDialog.Builder(it) }
        builder
            .setMessage(R.string.processing_message)
            .setPositiveButton(R.string.cancel_work){ d, w -> sharedViewModel.stopProcessing() }

        alertDialog = builder.create()

        binding.apply {
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
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, VIDEO_LIMIT_S)
        getVideo.launch(intent)
    }

    private val getVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ activityResult ->
        val intent = activityResult.data
        intent?.let {
            val uri = it.data!!
            currentUriString = uri.toString()
            sharedViewModel.startProcessing(uri)
            sharedViewModel.outputWorkInfo.observe(viewLifecycleOwner, workInfoObserver())
        }
    }

    private fun workInfoObserver(): Observer<WorkInfo> {
        return Observer {
            Log.d(TAG, it.state.toString())
            val progress = it.progress.getInt("Progress", 0)
            Log.d(TAG, "Progress: $progress")
            if (it.state == WorkInfo.State.ENQUEUED){
                binding.progressBar.visibility = View.VISIBLE
                alertDialog.show()
            }
            if (it.state == WorkInfo.State.RUNNING) {
                binding.progressBar.progress = progress
            }
            if (it.state.isFinished){
                showWorkFinished()
            }
        }
    }

    private fun showWorkFinished() {
        Log.d(TAG, "Work is finished")
        alertDialog.dismiss()
        binding.progressBar.visibility = View.GONE
        val action = VideoListFragmentDirections
            .actionStartFragmentToPunchListFragment(currentUriString)
        findNavController().navigate(action)
    }

    companion object{
        const val TAG = "VideoListFragment"
    }

}
