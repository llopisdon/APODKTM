package com.machineinteractive.apodktm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import javax.inject.Inject

@HiltViewModel
class ApodViewModel @Inject constructor(private val repository: ApodRepository) : ViewModel() {

    val maxDate = Clock.System.todayAt(TimeZone.currentSystemDefault())

    private val curDate = MutableStateFlow(maxDate)
    private var pickerCurDate = maxDate
    val apods: Flow<List<Apod>> = curDate.flatMapLatest {
        repository.getApodsForCurMonth(it)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _apodListUiState = MutableStateFlow<ApodListUiState>(ApodListUiState.Idle)
    val apodListUiState: StateFlow<ApodListUiState> = _apodListUiState

    private val _selectedApod: MutableStateFlow<Apod?> = MutableStateFlow(null)
    val selectedApod: StateFlow<Apod?> = _selectedApod

    private val _pickerUiState = MutableStateFlow(PickerUiState(maxDate, pickerCurDate))
    val pickerUiState: StateFlow<PickerUiState> = _pickerUiState

    private val _bottomNavBarUiState = MutableStateFlow(BottomNavBarUiState(true, false))
    val bottomNavBarUiState: StateFlow<BottomNavBarUiState> = _bottomNavBarUiState

    init {
        Log.d(TAG, "ApodViewModel.init...")
    }

    fun select(apod: Apod) {
        _selectedApod.value = apod
    }

    var curJob: Job? = null

    fun fetchApods() {
        Log.d(TAG, "ApodViewModel.fetchApods - curDate: ${curDate.value} ...")
        curJob = viewModelScope.launch(Dispatchers.IO) {
            if (!isActive) return@launch
            var hasApods = repository.curMonthHasApods(curDate.value)
            Log.d(TAG, "hasApods: $hasApods")
            if (!isActive) return@launch
            if (repository.needsUpdate(curDate.value)) {
                if (!isActive) return@launch
                if (!hasApods) {
                    _apodListUiState.value = ApodListUiState.Loading
                }
                when (val result = repository.updateApodsForCurMonth(curDate.value)) {
                    is ApodResult.Error -> {
                        hasApods = repository.curMonthHasApods(curDate.value)
                        _apodListUiState.value = ApodListUiState.Error(hasApods, result)
                        return@launch
                    }
                }
            }
            hasApods = repository.curMonthHasApods(curDate.value)
            _apodListUiState.value = ApodListUiState.Success(hasApods)
        }
    }

    //
    // APOD list navbar methods
    //

    fun navBarPrevMonth() {
        var date = curDate.value
        date = date.minus(1, DateTimeUnit.MONTH)
        if (!(date.monthNumber < APOD_EPOCH_MONTH && date.year == APOD_EPOCH_YEAR)) {
            curDate.value = date
            pickerCurDate = date
            _pickerUiState.value = PickerUiState(date, pickerCurDate)
            viewModelScope.launch {
                curJob?.cancelAndJoin()
                fetchApods()
            }
            updateBottomNavBarUiState()
        }
    }

    fun navBarNextMonth() {
        var date = curDate.value
        date = date.plus(1, DateTimeUnit.MONTH)
        if (date <= maxDate) {
            curDate.value = date
            pickerCurDate = date
            _pickerUiState.value = PickerUiState(date, pickerCurDate)
            viewModelScope.launch {
                curJob?.cancelAndJoin()
                fetchApods()
            }
            updateBottomNavBarUiState()
        }
    }

    private fun updateBottomNavBarUiState() {
        curDate.value.let {
            val prevEnabled = it.monthNumber != APOD_EPOCH_MONTH || it.year != APOD_EPOCH_YEAR
            val nextEnabled = it.monthNumber != maxDate.monthNumber || it.year != maxDate.year
            _bottomNavBarUiState.value = BottomNavBarUiState(prevEnabled, nextEnabled)
        }
    }

    //
    // Picker Month Year Methods
    //

    fun setCurDateFromPicker() {
        curDate.value = pickerCurDate
        _pickerUiState.value = PickerUiState(curDate.value, pickerCurDate)
        updateBottomNavBarUiState()
        viewModelScope.launch {
            curJob?.cancelAndJoin()
            fetchApods()
        }
    }

    fun resetPicker() {
        pickerCurDate = curDate.value
        _pickerUiState.value = PickerUiState(curDate.value, pickerCurDate)
    }

    fun setPickerToMaxDate() {
        curDate.value = maxDate
        _pickerUiState.value = PickerUiState(curDate.value, pickerCurDate)
        updateBottomNavBarUiState()
        viewModelScope.launch {
            curJob?.cancelAndJoin()
            fetchApods()
        }
    }

    fun setPickerMonth(month: Int) {
        val prevDate = pickerCurDate
        val newDate = LocalDate(prevDate.year, month, 1)
        pickerCurDate = newDate
        _pickerUiState.value = PickerUiState(curDate.value, pickerCurDate)
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

        _pickerUiState.value = PickerUiState(curDate.value, pickerCurDate)
    }

    fun incrementPickYear() {
        setPickerYear(pickerCurDate.year + 1)
    }

    fun decrementPickYear() {
        setPickerYear(pickerCurDate.year - 1)
    }
}

data class BottomNavBarUiState(
    val prevMonthEnabled: Boolean,
    val nextMonthEnabled: Boolean
)

data class PickerUiState(val today: LocalDate, val curPickerDate: LocalDate)

sealed class ApodListUiState {
    object Idle : ApodListUiState()
    object Loading : ApodListUiState()
    class Error(val hasApods: Boolean, val error: ApodResult.Error) : ApodListUiState()
    class Success(val hasApods: Boolean) : ApodListUiState()
}