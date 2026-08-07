package com.toblad.khwab.speech

/**
 * Represents the current state of the model download process.
 */
sealed class ModelDownloadState {

    /** Models are already present — no download needed. */
    object Ready : ModelDownloadState()

    /** Download has not started yet. */
    object Idle : ModelDownloadState()

    /** Download is in progress. */
    data class Downloading(
        val percent: Int,
        val currentFile: String
    ) : ModelDownloadState()

    /** Download completed successfully — models are ready. */
    object Completed : ModelDownloadState()

    /** Download failed with an error message. */
    data class Failed(val message: String) : ModelDownloadState()
}
