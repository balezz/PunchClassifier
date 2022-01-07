package com.punchlab.punchclassifier.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val uriString = it.getString("videoUri").toString()
            sharedViewModel.setPunchListByUri(uriString)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPunchListBinding
                                    .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PunchListAdapter(requireContext())
        binding.punchRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.punchRecyclerView.adapter = adapter

        val punchListObserver = Observer<List<Punch>> {
            if (it.isNullOrEmpty()) return@Observer
            adapter.submitList(it)
        }
        sharedViewModel.punchList.observe(viewLifecycleOwner, punchListObserver)
    }




}