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
import androidx.work.WorkInfo
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
    private lateinit var binding: FragmentPunchListBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoUriString: String
    private lateinit var punchList: List<Punch>

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
    ): View {
        binding = FragmentPunchListBinding
                                    .inflate(inflater, container, false)

        sharedViewModel.outputWorkInfos.observe(viewLifecycleOwner, workInfosObserver())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.punchRecyclerView

        val punchListObserver = Observer<List<Punch>> {
            punchList = sharedViewModel.punchList.value!!
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = PunchListAdapter(requireContext(), punchList!!)
        }
        sharedViewModel.punchList.observe(viewLifecycleOwner, punchListObserver)

        binding.apply {
            viewModel = sharedViewModel
        }

        sharedViewModel.startProcessing()
    }

    private fun workInfosObserver(): Observer<List<WorkInfo>> {
        return Observer {
            if (it.isNullOrEmpty())
                return@Observer
            val workInfo = it[0]
            if (workInfo.state.isFinished) {
                showWorkFinished()
            }
            else
                showWorkInProgress()
        }
    }

    private fun showWorkFinished(){
        with(binding) {
            progressBarLow.visibility = View.GONE
            cancelButton.visibility = View.GONE
            if (punchList.isEmpty()) noPunchText.text = "No one punch in this video"
            else noPunchText.visibility = View.GONE
        }
    }

    private fun showWorkInProgress(){
        with(binding) {
            progressBarLow.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            noPunchText.text = "Video processing, please wait"
            noPunchText.visibility = View.VISIBLE
        }
    }

}