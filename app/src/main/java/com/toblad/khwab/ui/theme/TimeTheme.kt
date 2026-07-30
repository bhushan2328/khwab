package com.toblad.khwab.ui.theme

import java.time.LocalTime

/**
 * Represents the current time period used by the Khwab theme engine.
 */
enum class TimeTheme {

    SUNRISE,
    MORNING,
    AFTERNOON,
    SUNSET,
    EVENING,
    NIGHT;

    companion object {

        /**
         * Returns the current TimeTheme based on the local device time.
         */
        fun current(
            time: LocalTime = LocalTime.now()
        ): TimeTheme {

            return when {

                time >= LocalTime.of(5, 0) &&
                        time < LocalTime.of(7, 0) ->
                    SUNRISE

                time >= LocalTime.of(7, 0) &&
                        time < LocalTime.of(12, 0) ->
                    MORNING

                time >= LocalTime.of(12, 0) &&
                        time < LocalTime.of(17, 0) ->
                    AFTERNOON

                time >= LocalTime.of(17, 0) &&
                        time < LocalTime.of(19, 0) ->
                    SUNSET

                time >= LocalTime.of(19, 0) &&
                        time < LocalTime.of(22, 0) ->
                    EVENING

                else ->
                    NIGHT
            }
        }
    }
}