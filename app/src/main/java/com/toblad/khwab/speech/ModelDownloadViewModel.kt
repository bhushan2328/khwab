package com.toblad.khwab.speech

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelDownloadViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    init {
        // If both native libs and model files are already on disk, skip straight to Ready
        if (ModelDownloadManager.allReady(context)) {
            _state.value = ModelDownloadState.Ready
        }
    }

    fun startDownload() {
        if (_state.value is ModelDownloadState.Downloading) return

        viewModelScope.launch {
            _state.value = ModelDownloadState.Downloading(0, "")
            try {
                ModelDownloadManager.downloadAll(context) { percent, fileName ->
                    _state.value = ModelDownloadState.Downloading(percent, fileName)
                }
                _state.value = ModelDownloadState.Completed
            } catch (e: Exception) {
                _state.value = ModelDownloadState.Failed(
                    e.message ?: "Download failed. Check your internet connection."
                )
            }
        }
    }

    fun retry() {
        _state.value = ModelDownloadState.Idle
        startDownload()
    }
}
