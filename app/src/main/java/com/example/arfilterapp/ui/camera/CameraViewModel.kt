package com.example.arfilterapp.ui.camera

import androidx.lifecycle.ViewModel
import com.example.arfilterapp.filters.FilterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow(FilterType.NONE)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _landmarks = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val landmarks: StateFlow<List<Pair<Float, Float>>> = _landmarks.asStateFlow()

    private val _debugOverlayEnabled = MutableStateFlow(true)
    val debugOverlayEnabled: StateFlow<Boolean> = _debugOverlayEnabled.asStateFlow()

    fun selectFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun updateLandmarks(landmarks: List<Pair<Float, Float>>) {
        _landmarks.value = landmarks
    }

    fun toggleDebugOverlay() {
        _debugOverlayEnabled.value = !_debugOverlayEnabled.value
    }
}
