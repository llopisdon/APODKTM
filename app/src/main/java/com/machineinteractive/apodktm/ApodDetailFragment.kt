package com.machineinteractive.apodktm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.text.SimpleDateFormat
import java.util.*

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApodDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState?.getBoolean(STATE_BACK_BUTTON_VISIBLE, false) == true) {
            binding.backButton.show()
            binding.shareButton.show()
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
                    binding.shareButton.show()
                }
            }
        })

        binding.backButton.setOnClickListener {
            binding.backButton.hide()
            binding.shareButton.hide()
            findNavController().navigateUp()
        }

        binding.shareButton.setOnClickListener {
            doShare()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedApod.collect {
                    if (it != null) {
                        Log.d(TAG, "SHOW APOD: $it")
                        showApod(it)

                    } else {
                        Log.d(TAG, "GOT NULL APOD!")
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
            val context = binding.root.context
            val request = ImageRequest.Builder(context)
                .data(apod.url)
                .target(
                    onSuccess = { result ->
                        apodImage.setImageDrawable(result)
                        view?.doOnPreDraw {
                            startPostponedEnterTransition()
                        }
                        lifecycleScope.launch {
                            whenStarted {
                                withContext(Dispatchers.IO) {
                                    buildPhotoShareIntent(result.toBitmap())
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
                "${apod.date} ${"(" + apod.copyright + ")"?: ""}"
            }
             
            apodExplanation.text = apod.explanation
        }
    }

    private var shareIntent: Intent? = null

    //
    // see: https://developer.android.com/training/camera/photobasics
    // see: https://wares.commonsware.com/app/internal/book/Jetpack/page/chap-files-005.html
    //
    private fun buildPhotoShareIntent(bitmap: Bitmap) {

        if (isDetached) return

        try {
            val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
            val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile(
                "APOD_${timestamp}_",
                ".png",
                storageDir
            )

            FileOutputStream(photoFile).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val uri = FileProvider.getUriForFile(
                requireActivity(),
                AUTHORITY,
                photoFile
            )

            shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }

            Log.d(TAG, "shareIntent: $shareIntent")

        } catch (e: IOException) {
            Log.d(TAG, "Unable to create shareIntent. Create bitmap failed: $e")
            shareIntent = null
        }
    }

    private fun doShare() {
        shareIntent?.let {
            Log.d(TAG, "doShare...")
            val intent = Intent.createChooser(it, getString(R.string.open_photo_with))
            startActivity(intent)
        }
    }
}