package com.example.wakemeup

import android.content.Context
import android.content.ContextWrapper
import com.example.wakemeup.data.DefaultLocationSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchRepositoryTest {

    private val dummyContext: Context = object : ContextWrapper(null) {}

    @Test
    fun testNominatimForwardSearchWithPOI() = runTest {
        var requestedUserAgent: String? = null
        var requestedUrl: String? = null

        val jsonResponse = """
            [
              {
                "place_id": 100,
                "lat": "40.7580",
                "lon": "-73.9855",
                "class": "tourism",
                "type": "hotel",
                "name": "Grand Central Hotel",
                "display_name": "Grand Central Hotel, 100 E 42nd St, New York, NY 10017, USA"
              },
              {
                "place_id": 101,
                "lat": "40.7527",
                "lon": "-73.9772",
                "class": "amenity",
                "type": "fuel",
                "name": "Shell Fuel Station",
                "display_name": "Shell Fuel Station, 400 E 42nd St, New York, NY 10017, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            requestedUserAgent = request.header("User-Agent")
            requestedUrl = request.url.toString()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val results = repository.searchLocations("hotel", maxResults = 5)

        assertEquals("WakeMeUpApp/1.0 (contact: support@wakemeup.app)", requestedUserAgent)
        assertNotNull(requestedUrl)
        val url = requestedUrl ?: ""
        assertTrue(url.contains("https://nominatim.openstreetmap.org/search"))
        assertTrue(url.contains("q=hotel"))
        assertTrue(url.contains("format=json"))
        assertTrue(url.contains("addressdetails=1"))

        assertEquals(2, results.size)

        val hotelDest = results[0]
        assertEquals("Grand Central Hotel", hotelDest.title)
        assertEquals("Grand Central Hotel, 100 E 42nd St, New York, NY 10017, USA", hotelDest.address)
        assertEquals(40.7580, hotelDest.latitude, 0.0001)
        assertEquals(-73.9855, hotelDest.longitude, 0.0001)
        assertEquals("HOTEL", hotelDest.category)
        assertEquals("hotel", hotelDest.type)

        val fuelDest = results[1]
        assertEquals("Shell Fuel Station", fuelDest.title)
        assertEquals("Shell Fuel Station, 400 E 42nd St, New York, NY 10017, USA", fuelDest.address)
        assertEquals("PETROL PUMP", fuelDest.category)
        assertEquals("fuel", fuelDest.type)
    }

    @Test
    fun testNominatimExtraTagsAndNameDetailsExtraction() = runTest {
        val jsonResponse = """
            [
              {
                "place_id": 105,
                "lat": "40.7128",
                "lon": "-74.0060",
                "class": "highway",
                "type": "bus_stop",
                "name": null,
                "namedetails": { "name:en": "Downtown Bus Stop" },
                "extratags": { "highway": "bus_stop" },
                "display_name": "Downtown Bus Stop, Main St, NY, USA"
              },
              {
                "place_id": 106,
                "lat": "40.7130",
                "lon": "-74.0065",
                "class": "shop",
                "type": "bakery",
                "name": null,
                "namedetails": null,
                "extratags": { "shop": "bakery", "brand": "Sweet Treats Bakery" },
                "display_name": "Sweet Treats Bakery, Main St, NY, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val results = repository.searchLocations("shop", maxResults = 5)

        assertEquals(2, results.size)
        assertEquals("Downtown Bus Stop", results[0].title)
        assertEquals("BUS STOP", results[0].category)

        assertEquals("Sweet Treats Bakery", results[1].title)
        assertEquals("BAKERY", results[1].category)
    }

    @Test
    fun testNominatimForwardSearchWithoutName() = runTest {
        val jsonResponse = """
            [
              {
                "place_id": 102,
                "lat": "37.7749",
                "lon": "-122.4194",
                "class": "place",
                "type": "house",
                "name": null,
                "display_name": "742 Evergreen Terrace, Springfield, OR 97477, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val results = repository.searchLocations("742 Evergreen", maxResults = 5)

        assertEquals(1, results.size)
        val dest = results[0]
        assertEquals("742 Evergreen Terrace", dest.title)
        assertEquals("742 Evergreen Terrace, Springfield, OR 97477, USA", dest.address)
    }

    @Test
    fun testNominatimNetworkFailureFallback() = runTest {
        val mockInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Server Error")
                .body("Internal Server Error".toResponseBody("text/plain".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val results = repository.searchLocations("unknown place", maxResults = 5)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testNominatimForwardSearchWithMapCenterNearby() = runTest {
        var requestedUrl: String? = null

        val jsonResponse = """
            [
              {
                "place_id": 200,
                "lat": "40.7580",
                "lon": "-73.9855",
                "class": "amenity",
                "type": "hospital",
                "name": "City Hospital",
                "display_name": "City Hospital, 100 E 42nd St, New York, NY 10017, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            requestedUrl = request.url.toString()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val center = org.osmdroid.util.GeoPoint(40.7500, -73.9800)
        val results = repository.searchLocations("hospital", mapCenter = center, maxResults = 5)

        assertNotNull(requestedUrl)
        val url = requestedUrl ?: ""
        assertTrue(url.contains("q=hospital"))
        assertTrue(url.contains("extratags=1"))
        assertTrue(url.contains("namedetails=1"))
        assertTrue(url.contains("limit=25"))
        assertTrue(url.contains("lat=40.75"))
        assertTrue(url.contains("lon=-73.98"))
        assertTrue(url.contains("viewbox="))
        assertTrue(url.contains("bounded=1"))
        assertEquals(1, results.size)
        assertEquals("City Hospital", results[0].title)
        assertEquals("HOSPITAL", results[0].category)
        assertNotNull(results[0].distanceMeters)
        assertEquals("1.0 km away", results[0].formattedDistance)
    }

    @Test
    fun testNominatimForwardSearchFallbackToBoundedZero() = runTest {
        val requestedUrls = mutableListOf<String>()

        val jsonResponse = """
            [
              {
                "place_id": 201,
                "lat": "41.0000",
                "lon": "-74.0000",
                "class": "amenity",
                "type": "hospital",
                "name": "Distant Hospital",
                "display_name": "Distant Hospital, Far Away St, NY, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            val urlStr = request.url.toString()
            requestedUrls.add(urlStr)

            val bodyText = if (urlStr.contains("bounded=1")) "[]" else jsonResponse

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(bodyText.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val center = org.osmdroid.util.GeoPoint(40.7500, -73.9800)
        val results = repository.searchLocations("hospital", mapCenter = center, maxResults = 5)

        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls[0].contains("bounded=1"))
        assertTrue(requestedUrls[1].contains("bounded=0"))

        assertEquals(1, results.size)
        assertEquals("Distant Hospital", results[0].title)
        assertNotNull(results[0].distanceMeters)
    }

    @Test
    fun testFilterOutSuggestionsOver500km() = runTest {
        val jsonResponse = """
            [
              {
                "place_id": 300,
                "lat": "40.7580",
                "lon": "-73.9855",
                "class": "amenity",
                "type": "cafe",
                "name": "Nearby Cafe",
                "display_name": "Nearby Cafe, 100 E 42nd St, New York, NY 10017, USA"
              },
              {
                "place_id": 301,
                "lat": "41.5000",
                "lon": "-74.0000",
                "class": "tourism",
                "type": "hotel",
                "name": "Regional Hotel",
                "display_name": "Regional Hotel, Albany, NY, USA"
              },
              {
                "place_id": 302,
                "lat": "34.0522",
                "lon": "-118.2437",
                "class": "aeroway",
                "type": "aerodrome",
                "name": "Far Away Airport",
                "display_name": "Far Away Airport, Los Angeles, CA, USA"
              }
            ]
        """.trimIndent()

        val mockInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val repository = DefaultLocationSearchRepository(
            context = dummyContext,
            okHttpClient = testOkHttpClient
        )
        val center = org.osmdroid.util.GeoPoint(40.7500, -73.9800)
        val results = repository.searchLocations("spot", mapCenter = center, maxResults = 5)

        assertEquals(2, results.size)
        assertEquals("Nearby Cafe", results[0].title)
        assertEquals("Regional Hotel", results[1].title)
        assertTrue(results.none { it.title == "Far Away Airport" })
        assertTrue(results.all { (it.distanceMeters ?: Float.MAX_VALUE) <= 500_000f })
    }
}
