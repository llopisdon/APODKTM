package com.machineinteractive.apodktm

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.machineinteractive.apodktm.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ApodViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    val currentNavigationFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager
            ?.fragments
            ?.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private var sharePhotoIntent: Intent? = null
    private var shareLinkIntent: Intent? = null

    //
    // see: https://developer.android.com/training/secure-file-sharing/setup-sharing
    // see: https://developer.android.com/training/data-storage/shared
    // see: https://developer.android.com/training/camera/photobasics
    // see: https://wares.commonsware.com/app/internal/book/Jetpack/page/chap-files-005.html
    //
    fun buildShareIntents(apod: Apod, bitmap: Bitmap) {
        try {
            val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
            val storageDir: File? =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile(
                "apod-${apod.date}",
                ".png",
                storageDir
            )

            FileOutputStream(photoFile).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            val photoUri = FileProvider.getUriForFile(
                this,
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

    fun doSharePhoto() {
        sharePhotoIntent?.let {
            val intent = Intent.createChooser(it, null)
            startActivity(intent)
        }
    }

    fun doShareLink() {
        shareLinkIntent?.let {
            val intent = Intent.createChooser(it, null)
            startActivity(intent)
        }
    }
}