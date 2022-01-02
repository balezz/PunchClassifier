package com.punchlab.punchclassifier.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.punchlab.punchclassifier.PunchApplication
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.databinding.FragmentPunchListBinding

/**
 * todo Implement loading progress bar
 * */
class PunchListFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by activityViewModels{
        SharedViewModel.SharedViewModelFactory(activity?.application!! as PunchApplication)
    }
    private var binding: FragmentPunchListBinding? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoUriString: String
    private var punchList: List<Punch>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val uriString = it.getString("videoUri").toString()
            videoUriString = uriString
            sharedViewModel.setCurrentVideoUri(uriString)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPunchListBinding
                                    .inflate(inflater, container, false)

        val progressObserver = Observer<Int> {
            binding!!.progressBar.setProgressCompat(it, true)
        }
        sharedViewModel.progress.observe(viewLifecycleOwner, progressObserver)

        val notificationObserver = Observer<String> {
            if (it.isNotEmpty())
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }
        sharedViewModel.notification.observe(viewLifecycleOwner, notificationObserver)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding!!.punchRecyclerView

        val punchListObserver = Observer<List<Punch>> {
            punchList = sharedViewModel.punchList.value
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PunchListAdapter(requireContext(), punchList!!)
        }
        sharedViewModel.punchList.observe(viewLifecycleOwner, punchListObserver)

        binding?.apply {
            viewModel = sharedViewModel
        }
        sharedViewModel.startProcessing()
    }
}