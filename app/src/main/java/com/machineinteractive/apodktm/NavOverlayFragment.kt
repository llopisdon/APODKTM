package com.machineinteractive.apodktm

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.chip.Chip
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.machineinteractive.apodktm.databinding.FragmentNavOverlayBinding
import io.ktor.client.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.util.*

class NavOverlayFragment : Fragment(), NavController.OnDestinationChangedListener {

    private var _binding: FragmentNavOverlayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApodViewModel by activityViewModels()

    val monthChips = mutableListOf<Chip>()

    private lateinit var callback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            closePicker()
        }
        callback.isEnabled = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavOverlayBinding.inflate(layoutInflater, container, false)
        binding.bottomNavBar.prevMonthButton.hide()
        binding.bottomNavBar.nextMonthButton.hide()
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
                viewModel.navBarPrevMonth()
            }

            bottomNavBar.nextMonthButton.setOnClickListener {
                viewModel.navBarNextMonth()
            }

            bottomNavBar.monthButton.setOnClickListener {
                viewModel.resetPicker()
                openPicker()
            }

            monthYearPicker.closeIcon.setOnClickListener {
                closePicker()
            }

            monthYearPicker.todayButton.setOnClickListener {
                viewModel.setPickerToMaxDate()
                closePicker()
            }

            monthYearPicker.okButton.setOnClickListener {
                viewModel.setCurDateFromPicker()
                closePicker()
            }

            monthYearPicker.prevYearButton.setOnClickListener {
                viewModel.decrementPickYear()
            }

            monthYearPicker.nextYearButton.setOnClickListener {
                viewModel.incrementPickYear()
            }

            monthYearPicker.yearSlider.addOnChangeListener { _, value, _ ->
                viewModel.setPickerYear(value.toInt())
            }

            monthChips.clear()
            monthChips.add(monthYearPicker.chipJan)
            monthChips.add(monthYearPicker.chipFeb)
            monthChips.add(monthYearPicker.chipMar)
            monthChips.add(monthYearPicker.chipApr)
            monthChips.add(monthYearPicker.chipMay)
            monthChips.add(monthYearPicker.chipJun)
            monthChips.add(monthYearPicker.chipJul)
            monthChips.add(monthYearPicker.chipAug)
            monthChips.add(monthYearPicker.chipSep)
            monthChips.add(monthYearPicker.chipOct)
            monthChips.add(monthYearPicker.chipNov)
            monthChips.add(monthYearPicker.chipDec)

            resources.getStringArray(R.array.months).forEachIndexed { index, value ->
                monthChips[index].text = value
            }

            monthYearPicker.monthChips.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.chip_jan -> viewModel.setPickerMonth(1)
                    R.id.chip_feb -> viewModel.setPickerMonth(2)
                    R.id.chip_mar -> viewModel.setPickerMonth(3)
                    R.id.chip_apr -> viewModel.setPickerMonth(4)
                    R.id.chip_may -> viewModel.setPickerMonth(5)
                    R.id.chip_jun -> viewModel.setPickerMonth(6)
                    R.id.chip_jul -> viewModel.setPickerMonth(7)
                    R.id.chip_aug -> viewModel.setPickerMonth(8)
                    R.id.chip_sep -> viewModel.setPickerMonth(9)
                    R.id.chip_oct -> viewModel.setPickerMonth(10)
                    R.id.chip_nov -> viewModel.setPickerMonth(11)
                    R.id.chip_dec -> viewModel.setPickerMonth(12)
                }
            }

            view.doOnPreDraw {
                findNavController().addOnDestinationChangedListener(this@NavOverlayFragment)
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.pickerUiState.collect {
                            setBottomNavBarMonthButtonText(it.today)
                            setupPickerViews(it.curPickerDate)
                        }
                    }
                    launch {
                        viewModel.bottomNavBarUiState.collect { state ->
                            with(bottomNavBar) {
                                prevMonthButton.apply {
                                    if (state.prevMonthEnabled) show() else hide()
                                }
                                nextMonthButton.apply {
                                    if (state.nextMonthEnabled) show() else hide()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupPickerViews(curPickerDate: LocalDate) {

        with(binding.monthYearPicker) {

            val curYear = curPickerDate.year
            val curMonthText =
                resources.getStringArray(R.array.months)[curPickerDate.monthNumber - 1]

            curMonthYear.text = resources.getString(R.string.cur_month_year, curMonthText, curYear)

            // setup year slider
            yearSlider.value = curYear.toFloat()
            yearSlider.valueFrom = APOD_EPOCH_YEAR.toFloat()
            yearSlider.valueTo = viewModel.maxDate.year.toFloat()
            yearSlider.stepSize = 1f

            monthChips.forEach {
                (it as Chip).isEnabled = true
            }

            // setup month chips
            if (curYear == APOD_EPOCH_YEAR) {
                for (i in 0..4) {
                    monthChips[i].isEnabled = false
                }
            } else if (curYear == viewModel.maxDate.year) {
                val nextMonth = viewModel.maxDate.monthNumber
                for (i in nextMonth..11) {
                    monthChips[i].isEnabled = false
                }
            }

            (monthChips[curPickerDate.monthNumber - 1] as Chip).isChecked = true
        }
    }

    private fun setBottomNavBarMonthButtonText(today: LocalDate) {
        val curMonthText = resources.getStringArray(R.array.months)[today.monthNumber - 1]
        binding.bottomNavBar.monthButton.text =
            resources.getString(R.string.cur_month_year, curMonthText, today.year)
    }

    private fun openPicker() {
        callback.isEnabled = true
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
        callback.isEnabled = false
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.apodsListFragment -> {
                ObjectAnimator.ofFloat(binding.bottomNavBar.bottomNavBar, "translationY", 0f)
                    .apply {
                        duration =
                            resources.getInteger(R.integer.material_transition_duration_medium)
                                .toLong()
                        interpolator = DecelerateInterpolator()
                        start()
                    }
            }
            R.id.apodDetailFragment,
            R.id.apodPhotoFragment -> {
                val bottomNavHeight =
                    resources.getDimensionPixelSize(R.dimen.bottom_nav_bar_height).toFloat()
                ObjectAnimator.ofFloat(
                    binding.bottomNavBar.bottomNavBar,
                    "translationY",
                    bottomNavHeight
                ).apply {
                    duration =
                        resources.getInteger(R.integer.material_transition_duration_medium).toLong()
                    interpolator = FastOutLinearInInterpolator()
                    start()
                }
            }
        }
    }
}