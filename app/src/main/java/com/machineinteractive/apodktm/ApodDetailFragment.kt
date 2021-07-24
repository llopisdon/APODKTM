package com.machineinteractive.apodktm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.material.transition.MaterialContainerTransform
import com.machineinteractive.apodktm.databinding.FragmentApodDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

        if (savedInstanceState?.getBoolean(STATE_BACK_BUTTON_VISIBLE, false) == true) {
            binding.backButton.show()
            binding.shareLinkButton.show()
            binding.sharePhotoButton.show()
        }

        postponeEnterTransition()

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
                view.doOnPreDraw {
                    binding.backButton.show()
                    binding.shareLinkButton.show()
                    binding.sharePhotoButton.show()
                }
            }
        })

        binding.backButton.setOnClickListener {
            binding.backButton.hide()
            binding.shareLinkButton.hide()
            binding.sharePhotoButton.hide()
            findNavController().navigateUp()
        }

        binding.shareLinkButton.setOnClickListener {
            doShareLink()
        }

        binding.sharePhotoButton.setOnClickListener {
            doSharePhoto()
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

    private val STATE_BACK_BUTTON_VISIBLE = "state_back_button_visible"

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_BACK_BUTTON_VISIBLE, binding.backButton.isOrWillBeShown)
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
                        view?.doOnPreDraw {
                            startPostponedEnterTransition()
                        }
                        lifecycleScope.launch {
                            whenStarted {
                                withContext(Dispatchers.IO) {
                                    buildShareIntents(apod, result.toBitmap())
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
                "${apod.date} ${"(" + apod.copyright + ")" ?: ""}"
            }

            apodExplanation.text = apod.explanation
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
            Toast.makeText(requireContext().applicationContext, R.string.no_video_player_found, Toast.LENGTH_SHORT).show()
        }
    }

    private var sharePhotoIntent: Intent? = null
    private var shareLinkIntent: Intent? = null

    //
    // see: https://developer.android.com/training/secure-file-sharing/setup-sharing
    // see: https://developer.android.com/training/data-storage/shared
    // see: https://developer.android.com/training/camera/photobasics
    // see: https://wares.commonsware.com/app/internal/book/Jetpack/page/chap-files-005.html
    //
    private fun buildShareIntents(apod: Apod, bitmap: Bitmap) {

        if (isDetached) return
        try {
            val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
            val storageDir: File? =
                requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile(
                "apod-${apod.date}",
                ".png",
                storageDir
            )

            FileOutputStream(photoFile).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val photoUri = FileProvider.getUriForFile(
                requireActivity(),
                AUTHORITY,
                photoFile
            )

            sharePhotoIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, photoUri)
            }

            shareLinkIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, apod.url ?: APOD_URL)
            }


        } catch (e: IOException) {
            Log.d(TAG, "Unable to create shareIntents. Create bitmap failed: $e")
            sharePhotoIntent = null
            shareLinkIntent = null
        }
    }

    private fun doSharePhoto() {
        if (isDetached) return
        sharePhotoIntent?.let {
            val intent = Intent.createChooser(it, null)
            startActivity(intent)
        }
    }

    private fun doShareLink() {
        if (isDetached) return
        shareLinkIntent?.let {
            val intent = Intent.createChooser(it, null)
            startActivity(intent)
        }
    }
}