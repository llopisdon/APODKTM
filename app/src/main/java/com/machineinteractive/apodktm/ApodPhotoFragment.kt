package com.machineinteractive.apodktm

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.transition.MaterialContainerTransform
import com.machineinteractive.apodktm.databinding.FragmentApodPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApodPhotoFragment : Fragment() {

    private var _binding: FragmentApodPhotoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ApodViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.material_transition_duration_medium).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApodPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        binding.backButton.setOnClickListener {
            binding.backButton.hide()
            binding.shareLinkButton.hide()
            binding.sharePhotoButton.hide()
            findNavController().navigateUp()
        }

        binding.shareLinkButton.setOnClickListener {
            if (!isDetached) {
                (requireActivity() as MainActivity).doShareLink()
            }
        }

        binding.sharePhotoButton.setOnClickListener {
            if (!isDetached) {
                (requireActivity() as MainActivity).doSharePhoto()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedApod.collect { apod ->
                        Log.d(TAG, "SHOW APOD PHOTO: ${apod?.url.orEmpty()}")
                        apod?.let {
                            showApod(it)
                        }
                    }
                }
            }
        }
    }

    private fun enableButtons(view: View) {
        view.doOnPreDraw {
            binding.backButton.show()
            binding.shareLinkButton.show()
            binding.sharePhotoButton.show()
        }
    }

    private fun showApod(apod: Apod) {
        val imageUrl =
            if (apod.media_type == "image") apod.url.orEmpty() else apod.thumbnail_url.orEmpty()
        val data = imageUrl.takeUnless { it.isEmpty() }
            ?: R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur
        val request = ImageRequest.Builder(requireContext())
            .data(data)
            .placeholder(R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur)
            .target(
                onSuccess = { result ->
                    binding.apodImage.setImageDrawable(result)
                    (view?.parent as? ViewGroup)?.doOnPreDraw {
                        startPostponedEnterTransition()
                        enableButtons(it)
                    }
                }
            )
            .listener { request, metadata ->
                Log.d(TAG, "metadata: $metadata")
            }
            .build()
        requireContext().imageLoader.enqueue(request)
    }
}