package com.example.wakemeup.data

// Author: TronShark
// Version: v1.0

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.wakemeup.model.Destination
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

interface LocationSearchRepository {
    suspend fun searchLocations(query: String, mapCenter: GeoPoint? = null, maxResults: Int = 5): List<Destination>
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Destination
}

data class NominatimResultDto(
    @Json(name = "display_name") val displayName: String? = null,
    val name: String? = null,
    val lat: String? = null,
    val lon: String? = null,
    val type: String? = null,
    @Json(name = "class") val categoryClass: String? = null,
    val namedetails: Map<String, String>? = null,
    val extratags: Map<String, String>? = null,
)

class DefaultLocationSearchRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) : LocationSearchRepository {

    private val geocoder: Geocoder by lazy {
        Geocoder(context, Locale.getDefault())
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    override suspend fun searchLocations(
        query: String,
        mapCenter: GeoPoint?,
        maxResults: Int
    ): List<Destination> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@withContext emptyList()

        val nominatimResults = try {
            searchNominatim(trimmedQuery, mapCenter, maxResults)
        } catch (_: Exception) {
            emptyList()
        }

        val rawResults = if (nominatimResults.isNotEmpty()) {
            nominatimResults
        } else {
            searchGeocoderFallback(trimmedQuery, maxResults)
        }

        if (mapCenter != null && rawResults.isNotEmpty()) {
            rawResults.map { dest ->
                val dist = calculateDistanceMeters(
                    mapCenter.latitude, mapCenter.longitude,
                    dest.latitude, dest.longitude
                )
                dest.copy(distanceMeters = dist)
            }
            .filter { (it.distanceMeters ?: Float.MAX_VALUE) <= 500_000f }
            .sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
        } else {
            rawResults
        }
    }

    private fun searchNominatim(query: String, mapCenter: GeoPoint?, maxResults: Int): List<Destination> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val limit = if (maxResults > 25) maxResults else 25

        if (mapCenter != null) {
            val delta = 0.15
            val minLon = mapCenter.longitude - delta
            val maxLon = mapCenter.longitude + delta
            val minLat = mapCenter.latitude - delta
            val maxLat = mapCenter.latitude + delta
            val viewbox = "$minLon,$maxLat,$maxLon,$minLat"

            val boundedUrl = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&extratags=1&namedetails=1&addressdetails=1&limit=$limit&viewbox=$viewbox&bounded=1&lat=${mapCenter.latitude}&lon=${mapCenter.longitude}"
            val boundedResults = fetchNominatimResults(boundedUrl)
            if (boundedResults.isNotEmpty()) {
                return boundedResults
            }

            val fallbackUrl = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&extratags=1&namedetails=1&addressdetails=1&limit=$limit&viewbox=$viewbox&bounded=0&lat=${mapCenter.latitude}&lon=${mapCenter.longitude}"
            return fetchNominatimResults(fallbackUrl)
        } else {
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&extratags=1&namedetails=1&addressdetails=1&limit=$limit"
            return fetchNominatimResults(url)
        }
    }

    private fun fetchNominatimResults(url: String): List<Destination> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WakeMeUpApp/1.0 (contact: support@wakemeup.app)")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val responseBody = response.body.string()
        if (responseBody.isBlank()) return emptyList()

        val listType = Types.newParameterizedType(List::class.java, NominatimResultDto::class.java)
        val adapter = moshi.adapter<List<NominatimResultDto>>(listType)
        val dtoList = adapter.fromJson(responseBody) ?: return emptyList()

        return dtoList.mapNotNull { item ->
            val lat = item.lat?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = item.lon?.toDoubleOrNull() ?: return@mapNotNull null
            val rawDisplayName = item.displayName?.trim() ?: return@mapNotNull null
            if (rawDisplayName.isBlank()) return@mapNotNull null

            val title = extractTitle(item, rawDisplayName)
            val category = mapCategoryDisplay(item)

            Destination(
                title = title,
                address = rawDisplayName,
                latitude = lat,
                longitude = lon,
                category = category,
                type = item.type
            )
        }
    }

    private fun extractTitle(item: NominatimResultDto, rawDisplayName: String): String {
        val nameStr = item.name?.trim()
        if (!nameStr.isNullOrBlank()) return nameStr

        item.namedetails?.let { details ->
            val nameVal = details["name"]?.trim()
                ?: details["name:en"]?.trim()
                ?: details.values.firstOrNull { it.isNotBlank() }?.trim()
            if (!nameVal.isNullOrBlank()) return nameVal
        }

        item.extratags?.let { tags ->
            val tagVal = tags["name"]?.trim()
                ?: tags["brand"]?.trim()
                ?: tags["operator"]?.trim()
                ?: tags["title"]?.trim()
            if (!tagVal.isNullOrBlank()) return tagVal
        }

        val cleanTitle = rawDisplayName.split(",").firstOrNull()?.trim() ?: rawDisplayName
        return cleanTitle
    }

    private fun mapCategoryDisplay(item: NominatimResultDto): String {
        val catClass = item.categoryClass?.lowercase(Locale.ROOT) ?: ""
        val type = item.type?.lowercase(Locale.ROOT) ?: ""
        val extraShop = item.extratags?.get("shop")?.lowercase(Locale.ROOT)
        val extraAmenity = item.extratags?.get("amenity")?.lowercase(Locale.ROOT)
        val extraTourism = item.extratags?.get("tourism")?.lowercase(Locale.ROOT)
        val extraHistoric = item.extratags?.get("historic")?.lowercase(Locale.ROOT)
        val extraBuilding = item.extratags?.get("building")?.lowercase(Locale.ROOT)
        val extraHighway = item.extratags?.get("highway")?.lowercase(Locale.ROOT)
        val extraLandmark = item.extratags?.get("landmark")?.lowercase(Locale.ROOT)

        return when {
            type == "bus_stop" || extraHighway == "bus_stop" || (catClass == "highway" && type == "bus_stop") -> "BUS STOP"
            type in listOf("supermarket", "convenience") || extraShop in listOf("supermarket", "convenience") -> "SUPERMARKET"
            type == "bakery" || extraShop == "bakery" -> "BAKERY"
            type in listOf("pharmacy", "chemist") || extraAmenity == "pharmacy" -> "PHARMACY"
            type == "cafe" || extraAmenity == "cafe" -> "CAFE"
            type in listOf("fuel", "gas_station", "petrol_pump", "charging_station") || extraAmenity == "fuel" -> "PETROL PUMP"
            type in listOf("hotel", "motel", "hostel", "guest_house", "lodging", "resort") || extraTourism in listOf("hotel", "motel", "hostel", "guest_house") -> "HOTEL"
            type in listOf("restaurant", "fast_food", "food_court", "bar", "pub") || extraAmenity in listOf("restaurant", "fast_food", "food_court") -> "RESTAURANT"
            type in listOf("hospital", "clinic", "doctors", "dentist") || extraAmenity in listOf("hospital", "clinic") -> "HOSPITAL"
            catClass in listOf("tourism", "historic", "landmark", "leisure") ||
                    type in listOf("monument", "memorial", "attraction", "viewpoint", "castle", "museum", "theme_park", "zoo") ||
                    extraTourism != null || extraHistoric != null || extraLandmark != null -> "LANDMARK"
            extraBuilding in listOf("commercial", "retail") || catClass == "building" ||
                    type in listOf("commercial", "retail", "store", "department_store", "mall") -> "STORE"
            catClass == "shop" || extraShop != null || type in listOf("shop", "kiosk", "clothes", "shoes", "hardware", "boutique") -> "SHOP"
            else -> item.categoryClass?.uppercase(Locale.ROOT) ?: "LOCATION"
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        try {
            android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            if (results[0] > 0f || (lat1 == lat2 && lon1 == lon2)) {
                return results[0]
            }
        } catch (_: Exception) {}

        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val aClamped = a.coerceIn(0.0, 1.0)
        val c = 2 * Math.atan2(Math.sqrt(aClamped), Math.sqrt(1 - aClamped))
        return (earthRadius * c).toFloat()
    }

    private suspend fun searchGeocoderFallback(query: String, maxResults: Int): List<Destination> {
        if (!Geocoder.isPresent()) return emptyList()

        val limit = if (maxResults > 0) maxResults else 5

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(
                        query,
                        limit,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: List<Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(addresses.mapToDestinations())
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(emptyList())
                                }
                            }
                        }
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, limit) ?: emptyList()
                addresses.mapToDestinations()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Destination =
        withContext(Dispatchers.IO) {
            val fallbackTitle = "Pinned Location"
            val fallbackAddress = "%.4f, %.4f".format(latitude, longitude)

            if (!Geocoder.isPresent()) {
                return@withContext Destination(
                    title = fallbackTitle,
                    address = fallbackAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: List<Address>) {
                                    if (continuation.isActive) {
                                        val dest = addresses.firstOrNull()?.toDestination()
                                            ?: Destination(
                                                title = fallbackTitle,
                                                address = fallbackAddress,
                                                latitude = latitude,
                                                longitude = longitude
                                            )
                                        continuation.resume(dest)
                                    }
                                }

                                override fun onError(errorMessage: String?) {
                                    if (continuation.isActive) {
                                        continuation.resume(
                                            Destination(
                                                title = fallbackTitle,
                                                address = fallbackAddress,
                                                latitude = latitude,
                                                longitude = longitude
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.toDestination() ?: Destination(
                        title = fallbackTitle,
                        address = fallbackAddress,
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            } catch (_: Exception) {
                Destination(
                    title = fallbackTitle,
                    address = fallbackAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            }
        }

    private fun List<Address>.mapToDestinations(): List<Destination> {
        return this.map { it.toDestination() }
    }

    private fun Address.toDestination(): Destination {
        val titleName = featureName ?: locality ?: subAdminArea ?: adminArea ?: "Location"
        val fullAddressLines = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }
        val addressText = if (fullAddressLines.isNotEmpty()) {
            fullAddressLines.joinToString(", ")
        } else {
            listOfNotNull(subLocality, locality, adminArea, countryName).joinToString(", ")
        }

        return Destination(
            title = titleName,
            address = addressText.ifBlank { "%.4f, %.4f".format(latitude, longitude) },
            latitude = latitude,
            longitude = longitude
        )
    }
}
