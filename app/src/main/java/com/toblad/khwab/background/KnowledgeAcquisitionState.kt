package com.toblad.khwab.background

/**
 * Represents the current state of an ongoing knowledge acquisition job.
 */
sealed class KnowledgeAcquisitionState {

    /** No acquisition in progress. */
    object Idle : KnowledgeAcquisitionState()

    /** Acquisition job has been enqueued and is waiting for network / processing. */
    data class Acquiring(val query: String) : KnowledgeAcquisitionState()

    /** Acquisition completed successfully. New knowledge is available in local store. */
    data class Completed(val query: String) : KnowledgeAcquisitionState()

    /** Acquisition failed (network error, API error, etc.). */
    data class Failed(val query: String, val reason: String) : KnowledgeAcquisitionState()
}
