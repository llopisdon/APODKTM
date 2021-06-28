package com.machineinteractive.apodktm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ApodViewModel @Inject constructor(private val repository: ApodRepository) : ViewModel() {

    private var today = Clock.System.todayAt(TimeZone.currentSystemDefault())
    private val maxDay = Clock.System.todayAt(TimeZone.currentSystemDefault())

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedApod: MutableStateFlow<Apod?> = MutableStateFlow(null)
    val selectedApod: StateFlow<Apod?> = _selectedApod

    init {
        Log.d(TAG, "ApodViewModel.init...")
        fetchApods()
    }

    fun select(apod: Apod) {
        _selectedApod.value = apod
    }

    fun setCurrentMonthYear(fromDate: LocalDate) {
        // TODO
    }

    fun fetchApods() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading

            if (repository.needsUpdate(today)) {
                repository.updateApods(today)
            }

            val apods = repository.getApods(today).first()
            if (apods.isEmpty()) {
                _uiState.value = UiState.Empty
            } else {
                _uiState.value = UiState.Success(apods)
            }
        }
    }

    fun prevMonth() {
        val prevDate = today
        today = today.minus(1, DateTimeUnit.MONTH)
        if (today.monthNumber < APOD_EPOCH_MONTH && today.year == APOD_EPOCH_YEAR) {
            today = prevDate
        } else {
            fetchApods()
        }
        Log.d(TAG, "prevMonth - prev: $prevDate cur: $today")
    }

    fun nextMonth() {
        val prevDate = today
        today = today.plus(1, DateTimeUnit.MONTH)
        if (today > maxDay) {
            today = prevDate
        } else {
            fetchApods()
        }
        Log.d(TAG, "nextMonth - prev: $prevDate cur: $today")

    }
}

sealed class UiState {
    object Idle : UiState()
    object Empty : UiState()
    object Loading : UiState()
    object Error : UiState()
    class Success(val apods: List<Apod>) : UiState()
}