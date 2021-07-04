package com.machineinteractive.apodktm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

@HiltViewModel
class ApodViewModel @Inject constructor(private val repository: ApodRepository) : ViewModel() {

    val maxDate = Clock.System.todayAt(TimeZone.currentSystemDefault())

    private var curDate = Clock.System.todayAt(TimeZone.currentSystemDefault())
    private var pickerCurDate = curDate

    private val _apodListUiState = MutableStateFlow<ApodListUiState>(ApodListUiState.Idle)
    val apodListUiState: StateFlow<ApodListUiState> = _apodListUiState

    private val _selectedApod: MutableStateFlow<Apod?> = MutableStateFlow(null)
    val selectedApod: StateFlow<Apod?> = _selectedApod

    private val _pickerUiState = MutableStateFlow(PickerUiState(curDate, pickerCurDate))
    val pickerUiState: StateFlow<PickerUiState> = _pickerUiState

    private val _bottomNavBarUiState = MutableStateFlow(BottomNavBarUiState(true, true, false))
    val bottomNavBarUiState: StateFlow<BottomNavBarUiState> = _bottomNavBarUiState

    init {
        Log.d(TAG, "ApodViewModel.init...")
        fetchApods()
    }

    fun select(apod: Apod) {
        _selectedApod.value = apod
    }

    fun fetchApods() {
        viewModelScope.launch(Dispatchers.IO) {
            _apodListUiState.value = ApodListUiState.Loading

            if (repository.needsUpdate(curDate)) {
                repository.updateApods(curDate)
            }

            val apods = repository.getApods(curDate).first()
            if (apods.isEmpty()) {
                _apodListUiState.value = ApodListUiState.Empty
            } else {
                _apodListUiState.value = ApodListUiState.Success(apods)
            }
        }
    }

    //
    // APOD list navbar methods
    //

    fun navBarPrevMonth() {
        val prevDate = curDate
        curDate = curDate.minus(1, DateTimeUnit.MONTH)
        pickerCurDate = curDate
        if (curDate.monthNumber < APOD_EPOCH_MONTH && curDate.year == APOD_EPOCH_YEAR) {
            curDate = prevDate
            pickerCurDate = prevDate
        } else {
            fetchApods()
            _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
        }
    }

    fun navBarNextMonth() {
        val prevDate = curDate
        curDate = curDate.plus(1, DateTimeUnit.MONTH)
        pickerCurDate = curDate
        if (curDate > maxDate) {
            curDate = prevDate
            pickerCurDate = prevDate
        } else {
            fetchApods()
            _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
        }
    }

    //
    // Picker Month Year Methods
    //

    fun setTodayToPickerCurMonthYear() {
        curDate = pickerCurDate
        _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
        fetchApods()
    }

    fun resetPicker() {
        pickerCurDate = curDate
        _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
    }

    fun setPickerCurDateToMaxDate() {
        pickerCurDate = maxDate
        _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
    }

    fun setPickerMonth(month: Int) {
        val prevDate = pickerCurDate
        val newDate = LocalDate(prevDate.year, month, 1)
        pickerCurDate = newDate
        _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
    }

    fun setPickerYear(year: Int) {
        if (year < APOD_EPOCH_YEAR || year > maxDate.year) {
            return
        }

        val prevDate = pickerCurDate

        val month = if (year == APOD_EPOCH_YEAR) {
            APOD_EPOCH_MONTH
        } else {
            if (year == maxDate.year && prevDate.monthNumber > maxDate.monthNumber) {
                maxDate.monthNumber
            } else {
                prevDate.monthNumber
            }
        }

        val newDate = LocalDate(year, month, 1)

        pickerCurDate = newDate

        _pickerUiState.value = PickerUiState(curDate, pickerCurDate)
    }

    fun incrementPickYear() {
        setPickerYear(pickerCurDate.year + 1)
    }

    fun decrementPickYear() {
        setPickerYear(pickerCurDate.year - 1)
    }
}

class BottomNavBarUiState(
    val prevMonthEnabled: Boolean,
    val monthEnabled: Boolean,
    val nextMonthEnabled: Boolean
)

class PickerUiState(val today: LocalDate, val curPickerDate: LocalDate)

sealed class ApodListUiState {
    object Idle : ApodListUiState()
    object Empty : ApodListUiState()
    object Loading : ApodListUiState()
    object Error : ApodListUiState()
    class Success(val apods: List<Apod>) : ApodListUiState()
}