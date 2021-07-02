package com.machineinteractive.apodktm

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialContainerTransform
import com.machineinteractive.apodktm.databinding.FragmentNavOverlayBinding

class NavOverlayFragment : Fragment(), NavController.OnDestinationChangedListener {

    private var _binding: FragmentNavOverlayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApodViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavOverlayBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {

            bottomNavBar.prevMonthButton.setOnClickListener {
                Log.d(TAG, "prevMonthButton...")
                viewModel.prevMonth()
            }

            bottomNavBar.nextMonthButton.setOnClickListener {
                Log.d(TAG, "nextMonthButton...")
                viewModel.nextMonth()
            }

            bottomNavBar.monthButton.setOnClickListener {
                openPicker()
            }

            monthYearPicker.closeIcon.setOnClickListener {
                closePicker()
            }

            view.doOnPreDraw {
                findNavController().addOnDestinationChangedListener(this@NavOverlayFragment)
            }
        }
    }

    private fun openPicker() {
        val transform = MaterialContainerTransform().apply {
            startView = binding.bottomNavBar.monthButton
            endView = binding.monthYearPicker.pickMonthYearView
            scrimColor = Color.TRANSPARENT
            addTarget(binding.monthYearPicker.pickMonthYearView)
        }
        TransitionManager.beginDelayedTransition(binding.navOverylayLayout, transform)
        binding.bottomNavBar.monthButton.visibility = View.INVISIBLE
        binding.monthYearPicker.pickMonthYearView.visibility = View.VISIBLE
    }

    private fun closePicker() {
        val transform = MaterialContainerTransform().apply {
            startView = binding.monthYearPicker.pickMonthYearView
            endView = binding.bottomNavBar.monthButton
            scrimColor = Color.TRANSPARENT
            addTarget(binding.bottomNavBar.monthButton)
        }
        TransitionManager.beginDelayedTransition(binding.navOverylayLayout, transform)
        binding.bottomNavBar.monthButton.visibility = View.VISIBLE
        binding.monthYearPicker.pickMonthYearView.visibility = View.INVISIBLE
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {

        val bottomNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_bar_height).toFloat()

        when (destination.id) {
            R.id.apodsListFragment -> {
                ObjectAnimator.ofFloat(binding.bottomNavBar.bottomNavBar, "translationY", 0f).apply {
                    duration = 250
                    start()
                }
            }
            R.id.apodDetailFragment -> {
                ObjectAnimator.ofFloat(binding.bottomNavBar.bottomNavBar, "translationY", bottomNavHeight).apply {
                    duration = 250
                    start()
                }
            }
        }
    }
}