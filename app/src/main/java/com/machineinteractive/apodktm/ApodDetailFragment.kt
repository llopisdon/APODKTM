package com.machineinteractive.apodktm

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.machineinteractive.apodktm.databinding.FragmentApodDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ApodDetailFragment : Fragment() {

    private var _binding: FragmentApodDetailBinding? = null
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
        _binding = FragmentApodDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        fun enableButtons() {
            view.doOnPreDraw {
                binding.backButton.show()
                binding.shareLinkButton.show()
                binding.sharePhotoButton.show()
                binding.apodImage.isClickable = true
                binding.videoIndicator.isClickable = true
            }
        }

        if (reenterTransition != null || savedInstanceState != null) {
            enableButtons()
        }

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
                enableButtons()
            }
        })

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
                        Log.d(TAG, "SHOW APOD: $apod")
                        apod?.let { showApod(it) }
                    }
                }
            }
        }
    }

    private fun showApod(apod: Apod) {
        binding.run {

            binding.videoIndicator.visibility = if (apod.media_type == "video") {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

            binding.videoIndicator.setOnClickListener {
                doPlayVideo(apod.url)
            }

            val imageUrl =
                if (apod.media_type == "image") apod.url.orEmpty() else apod.thumbnail_url.orEmpty()
            val data = imageUrl.takeUnless { it.isEmpty() }
                ?: R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur

            val context = binding.root.context
            val request = ImageRequest.Builder(context)
                .data(data)
                .placeholder(R.drawable.donald_giannatti_very_large_array_socorro_usa_unsplash_1232x820_blur)
                .target(
                    onSuccess = { result ->
                        apodImage.setImageDrawable(result)
                        (view?.parent as? ViewGroup)?.doOnPreDraw {
                            startPostponedEnterTransition()
                        }
                        lifecycleScope.launch {
                            whenStarted {
                                withContext(Dispatchers.IO) {
                                    if (!isDetached) {
                                        (requireActivity() as MainActivity).buildShareIntents(apod, result.toBitmap())
                                    }
                                }
                            }
                        }
                    }
                )
                .listener { request, metadata ->
                    Log.d(TAG, "metadata: $metadata")
                }
                .build()
            context.imageLoader.enqueue(request)
            apodTitle.text = apod.title

            apodDateCopyright.text = if (apod.copyright.isNullOrEmpty()) {
                "${apod.date}"
            } else {
                "${apod.date} (${apod.copyright})"
            }

            apodExplanation.text = apod.explanation

            apodImage.transitionName = getString(R.string.apod_photo_transition_name)
            apodImage.setOnClickListener {
                if (isDetached) return@setOnClickListener

                exitTransition = MaterialElevationScale(false).apply {
                    duration = resources.getInteger(R.integer.material_transition_duration_medium).toLong()
                }
                reenterTransition = MaterialElevationScale(true).apply {
                    duration = resources.getInteger(R.integer.material_transition_duration_medium).toLong()
                }

                val apodPhotoTransitionName = getString(R.string.apod_photo_transition_name)
                val extras = FragmentNavigatorExtras(it to apodPhotoTransitionName)
                val directions = ApodDetailFragmentDirections.actionApodDetailFragmentToApodPhotoFragment()
                findNavController().navigate(directions, extras)
            }
        }
    }

    private fun doPlayVideo(url: String?) {
        Log.d(TAG, "doPlayerVideo: $url")
        if (isDetached) return
        try {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext().applicationContext,
                R.string.no_video_player_found,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}