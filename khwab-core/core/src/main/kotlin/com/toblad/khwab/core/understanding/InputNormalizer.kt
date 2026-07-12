package com.toblad.khwab.core.understanding

/**
 * Normalizes raw user input before analysis.
 */
class InputNormalizer {

    fun normalize(input: String): String {
        return input
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
