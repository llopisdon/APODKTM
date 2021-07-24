package com.machineinteractive.apodktm

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.google.android.material.snackbar.Snackbar
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

    private var snackbar: Snackbar? = null

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

    lateinit var adapter: ApodAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        adapter = ApodAdapter(this)

        binding.run {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.apodListUiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.apods.collect {
                        adapter.submitList(it)
                    }
                }
                viewModel.fetchApods()
            }
        }
    }



    private fun showProgressBar(value: Boolean) {
        binding.progressBar.visibility = when (value) {
            true -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }

    private fun updateUi(state: ApodListUiState) {
        when (state) {
            is ApodListUiState.Idle -> {
                // NO-OP
            }
            is ApodListUiState.Loading -> {
                Log.d(TAG, "loading...")
                showProgressBar(true)
                adapter.submitList(emptyList())
                binding.emptyText.visibility = View.INVISIBLE
            }
            is ApodListUiState.Error -> {
                Log.d(TAG, "error...")
                showProgressBar(false)
                toggleEmptyState(state.hasApods)
                snackbar = Snackbar.make(
                    binding.snackbarArea,
                    "${state.error.message}",
                    Snackbar.LENGTH_INDEFINITE
                )
                snackbar?.setAction(getString(R.string.retry)) {
                    snackbar?.dismiss()
                    snackbar = null
                    viewModel.fetchApods()
                }
                snackbar?.show()
            }
            is ApodListUiState.Success -> {
                Log.d(TAG, "success...")
                showProgressBar(false)
                toggleEmptyState(state.hasApods)
            }
        }
    }

    private fun toggleEmptyState(hasApods: Boolean) {
        binding.emptyText.visibility = if (hasApods) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    override fun onApodItemClick(view: View) {

        if (isDetached) return

        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.material_transition_duration_medium).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.material_transition_duration_medium).toLong()
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

class ApodAdapter(private val listener: ApodAdapter.Listener) :
    ListAdapter<Apod, ApodAdapter.ViewHolder>(ApodDiffCallback()) {

    interface Listener {
        fun onApodItemClick(view: View)
    }

    inner class ViewHolder(private val binding: ListItemApodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apod.setOnClickListener {
                listener.onApodItemClick(it)
            }
        }

        fun bind(apod: Apod, isLast: Boolean) {
            binding.title.text = "${apod.title}"
            binding.date.text = "${apod.date}"

            binding.image.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur))

            val (imageUrl, videoIndicatorVisibility) = if (apod.media_type == "image") {
                Pair(apod.url.orEmpty(), View.INVISIBLE)
            } else {
                Pair(apod.thumbnail_url.orEmpty(), View.VISIBLE)
            }
            val data = imageUrl.takeUnless { it.isEmpty() }
                ?: R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur

            val request = ImageRequest.Builder(itemView.context)
                .data(data)
                .error(R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur)
                .placeholder(R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur)
                .crossfade(true)
                .target(onSuccess = { result ->
                    binding.image.setImageDrawable(result)
                    binding.apod.isClickable = true
                    binding.apod.isCheckable = true
                },onError = {
                    binding.apod.isClickable = true
                    binding.apod.isCheckable = true
                })
                .build()
            itemView.context.imageLoader.enqueue(request)

            binding.videoIndicator.visibility = videoIndicatorVisibility

            binding.apod.tag = apod
            binding.apod.transitionName =
                itemView
                    .resources
                    .getString(R.string.list_item_apod_card_transition_name, apod.date)
            binding.bottomSpacer.visibility = if (isLast) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemApodBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isLast = position == itemCount - 1
        holder.bind(getItem(position), isLast)
    }
}

