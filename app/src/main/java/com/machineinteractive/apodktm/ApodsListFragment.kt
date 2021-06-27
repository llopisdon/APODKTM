package com.machineinteractive.apodktm

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.transition.MaterialElevationScale
import com.machineinteractive.apodktm.databinding.FragmentApodsListBinding
import com.machineinteractive.apodktm.databinding.ListItemApodBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ApodsListFragment : Fragment(), ApodAdapter.Listener {

    private var _binding: FragmentApodsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApodViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApodsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        val adapter = ApodAdapter(this)

        binding.run {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apods.collect {
                    Log.d(TAG, "apods: $it")
                    when (it) {
                        is UiState.Loading -> {
                            Log.d(TAG, "loading...")
                        }
                        is UiState.Empty -> {
                            Log.d(TAG, "empty...")
                        }
                        is UiState.Error -> {
                            Log.d(TAG, "error...")
                        }
                        is UiState.Success -> {
                            Log.d(TAG, "success...")
                            adapter.submitList(it.data)
                        }
                    }
                }
            }
        }
    }

    override fun onClick(view: View) {

        exitTransition = MaterialElevationScale(false).apply {
            duration = 250L
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = 250L
        }

        val apodDetailTransitionName = getString(R.string.apod_detail_transition_name)
        val extras = FragmentNavigatorExtras(view to apodDetailTransitionName)
        val apod = view.tag as Apod
        viewModel.select(apod)
        val directions = ApodsListFragmentDirections.actionApodsListFragmentToApodDetailFragment()
        findNavController().navigate(directions, extras)
    }
}

class ApodDiffCallback : DiffUtil.ItemCallback<Apod>() {
    override fun areItemsTheSame(oldItem: Apod, newItem: Apod): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Apod, newItem: Apod): Boolean {
        return oldItem.date == newItem.date
    }
}

class ApodAdapter(private val listener: ApodAdapter.Listener) : ListAdapter<Apod, ApodAdapter.ViewHolder>(ApodDiffCallback()) {

    interface Listener {
        fun onClick(view: View)
    }

    inner class ViewHolder(private val binding: ListItemApodBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apod.setOnClickListener {
                listener.onClick(it)
            }
        }

        fun bind(apod: Apod) {
            binding.title.text = apod.title
            binding.image.load(apod.url)
            binding.apod.tag = apod
            binding.apod.transitionName =
                itemView
                    .resources
                    .getString(R.string.list_item_apod_card_transition_name, apod.date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemApodBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

