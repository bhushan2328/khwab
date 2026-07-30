package com.toblad.khwab.ui.theme

import java.time.LocalDate
import java.time.Month

/**
 * Represents the current season used by the Khwab theme engine.
 *
 * The default implementation is based on meteorological seasons.
 * It can later be customized for the user's region.
 */
enum class SeasonTheme {

    SPRING,
    SUMMER,
    MONSOON,
    AUTUMN,
    WINTER;

    companion object {

        /**
         * Returns the current season.
         *
         * Default mapping:
         * Spring   : March - April
         * Summer   : May - June
         * Monsoon  : July - September
         * Autumn   : October - November
         * Winter   : December - February
         */
        fun current(
            date: LocalDate = LocalDate.now()
        ): SeasonTheme {

            return when (date.month) {

                Month.MARCH,
                Month.APRIL ->
                    SPRING

                Month.MAY,
                Month.JUNE ->
                    SUMMER

                Month.JULY,
                Month.AUGUST,
                Month.SEPTEMBER ->
                    MONSOON

                Month.OCTOBER,
                Month.NOVEMBER ->
                    AUTUMN

                Month.DECEMBER,
                Month.JANUARY,
                Month.FEBRUARY ->
                    WINTER
            }
        }
    }
}