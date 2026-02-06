package com.skigpxrecorder.util

import com.skigpxrecorder.data.local.UserPreferences

/**
 * Utility for converting and formatting values between metric and imperial units
 */
object UnitConverter {

    /**
     * Format speed with appropriate unit
     */
    fun formatSpeed(speedKmh: Float, unitSystem: UserPreferences.UnitSystem): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> String.format("%.1f km/h", speedKmh)
            UserPreferences.UnitSystem.IMPERIAL -> {
                val speedMph = speedKmh * 0.621371f
                String.format("%.1f mph", speedMph)
            }
        }
    }

    /**
     * Format distance with appropriate unit
     */
    fun formatDistance(distanceMeters: Float, unitSystem: UserPreferences.UnitSystem): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> {
                if (distanceMeters >= 1000) {
                    String.format("%.2f km", distanceMeters / 1000)
                } else {
                    String.format("%.0f m", distanceMeters)
                }
            }
            UserPreferences.UnitSystem.IMPERIAL -> {
                val distanceFeet = distanceMeters * 3.28084f
                if (distanceFeet >= 5280) {
                    String.format("%.2f mi", distanceFeet / 5280)
                } else {
                    String.format("%.0f ft", distanceFeet)
                }
            }
        }
    }

    /**
     * Format elevation with appropriate unit
     */
    fun formatElevation(elevationMeters: Float, unitSystem: UserPreferences.UnitSystem): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> String.format("%.0f m", elevationMeters)
            UserPreferences.UnitSystem.IMPERIAL -> {
                val elevationFeet = elevationMeters * 3.28084f
                String.format("%.0f ft", elevationFeet)
            }
        }
    }

    /**
     * Get speed unit label
     */
    fun getSpeedUnit(unitSystem: UserPreferences.UnitSystem): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> "km/h"
            UserPreferences.UnitSystem.IMPERIAL -> "mph"
        }
    }

    /**
     * Get distance unit label
     */
    fun getDistanceUnit(unitSystem: UserPreferences.UnitSystem, meters: Float): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> if (meters >= 1000) "km" else "m"
            UserPreferences.UnitSystem.IMPERIAL -> {
                val feet = meters * 3.28084f
                if (feet >= 5280) "mi" else "ft"
            }
        }
    }

    /**
     * Get elevation unit label
     */
    fun getElevationUnit(unitSystem: UserPreferences.UnitSystem): String {
        return when (unitSystem) {
            UserPreferences.UnitSystem.METRIC -> "m"
            UserPreferences.UnitSystem.IMPERIAL -> "ft"
        }
    }

    /**
     * Format time duration
     */
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("0:%02d", secs)
        }
    }

    /**
     * Format slope percentage
     */
    fun formatSlope(slopePercent: Float): String {
        return String.format("%.1f%%", slopePercent)
    }
}
