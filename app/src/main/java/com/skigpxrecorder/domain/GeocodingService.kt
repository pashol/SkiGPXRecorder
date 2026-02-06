package com.skigpxrecorder.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for reverse geocoding coordinates to location names
 * Uses OpenStreetMap Nominatim API (free, no API key required)
 */
@Singleton
class GeocodingService @Inject constructor() {

    private val cache = mutableMapOf<Pair<Double, Double>, String>()

    /**
     * Get location name from GPS coordinates
     * Returns "City, Country" or "Unknown Location" if geocoding fails
     * Results are cached to minimize API calls
     */
    suspend fun getLocationName(latitude: Double, longitude: Double): String {
        // Round to 2 decimal places (~1km precision) for caching
        val roundedLat = (latitude * 100).toInt() / 100.0
        val roundedLon = (longitude * 100).toInt() / 100.0
        val cacheKey = Pair(roundedLat, roundedLon)

        // Check cache first
        cache[cacheKey]?.let { return it }

        return try {
            val locationName = reverseGeocode(latitude, longitude)
            // Cache the result
            cache[cacheKey] = locationName
            locationName
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("GeocodingService", "Geocoding failed", e)
            "Unknown Location"
        }
    }

    /**
     * Call Nominatim reverse geocoding API
     * https://nominatim.openstreetmap.org/reverse
     */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        val url = buildNominatimUrl(latitude, longitude)
        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SkiGPXRecorder/1.0") // Nominatim requires User-Agent
                connectTimeout = 5000 // 5 seconds
                readTimeout = 5000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                parseNominatimResponse(response)
            } else {
                "Unknown Location"
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Build Nominatim API URL for reverse geocoding
     */
    private fun buildNominatimUrl(latitude: Double, longitude: Double): String {
        return "https://nominatim.openstreetmap.org/reverse?" +
                "format=json&" +
                "lat=${latitude}&" +
                "lon=${longitude}&" +
                "zoom=10&" + // City level
                "addressdetails=1"
    }

    /**
     * Parse Nominatim JSON response to extract city and country
     * Returns "City, Country" format
     */
    private fun parseNominatimResponse(jsonResponse: String): String {
        return try {
            val json = JSONObject(jsonResponse)
            val address = json.optJSONObject("address")

            if (address != null) {
                // Try to get city name (multiple possible keys)
                val city = address.optString("city")
                    .ifEmpty { address.optString("town") }
                    .ifEmpty { address.optString("village") }
                    .ifEmpty { address.optString("municipality") }
                    .ifEmpty { address.optString("county") }

                val country = address.optString("country")

                when {
                    city.isNotEmpty() && country.isNotEmpty() -> "$city, $country"
                    city.isNotEmpty() -> city
                    country.isNotEmpty() -> country
                    else -> "Unknown Location"
                }
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            android.util.Log.e("GeocodingService", "Failed to parse geocoding response", e)
            "Unknown Location"
        }
    }

    /**
     * Clear the cache (useful if memory needs to be freed)
     */
    fun clearCache() {
        cache.clear()
    }
}
