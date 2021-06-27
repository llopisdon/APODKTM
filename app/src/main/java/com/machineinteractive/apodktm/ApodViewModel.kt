package com.machineinteractive.apodktm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ApodViewModel @Inject constructor(private val repository: ApodRepository) : ViewModel() {

    init {
        Log.d(TAG, "ApodViewModel.init")
    }

    val apods = repository.getApods().transformLatest { apods ->
        withContext(Dispatchers.IO) {
            val lastUpdate = repository.getLastUpdate()
            val diff = System.currentTimeMillis() - lastUpdate
            if (lastUpdate == -1L) {
                Log.d(TAG, "initial load -- fetching apods...")
                emit(UiState.Loading)
                repository.updateApods()
            } else{
                Log.d(TAG, "we have APODs -- displaying...")
                if (apods.isEmpty()) {
                    emit(UiState.Empty)
                } else {
                    emit(UiState.Success(apods))
                }
                if (diff > 15_000L) {
                    Log.d(TAG, "need to refresh APODS...")
                    repository.updateApods()
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    private val _selectedApod: MutableStateFlow<Apod?> = MutableStateFlow(null)
    val selectedApod: StateFlow<Apod?> = _selectedApod

    fun select(apod: Apod) {
        viewModelScope.launch {
            _selectedApod.value = apod
        }
    }
}

sealed class UiState<T> {
    object Empty : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    object Error : UiState<Nothing>()
    class Success<T>(val data: T) : UiState<T>()
}