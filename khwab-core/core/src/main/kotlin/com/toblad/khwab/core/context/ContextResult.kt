package com.toblad.khwab.core.context

/**
 * Result produced by the ContextEngine.
 */
data class ContextResult(

    val resolved: Boolean,

    val resolvedReference: String? = null,

    val message: String = ""

)
