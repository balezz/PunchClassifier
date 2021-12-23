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
import androidx.navigation.fragment.findNavController
import com.punchlab.punchclassifier.databinding.FragmentStartBinding


class StartFragment : Fragment() {

    private var binding: FragmentStartBinding? = null
    private val sharedViewModel: SharedViewModel by activityViewModels{
        SharedViewModel.SharedViewModelFactory(activity?.application!!)
    }

    private val getVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ activityResult ->
            val intent = activityResult.data
            intent?.let {
                val uri = it.data!!
                sharedViewModel.setCurrentVideoUri(uri)
                Log.d(TAG, "Video URI: $uri")
                val action = StartFragmentDirections.actionStartFragmentToPunchListFragment()
                findNavController().navigate(action)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        binding = FragmentStartBinding
            .inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            // set onClick listeners and viewModel
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
