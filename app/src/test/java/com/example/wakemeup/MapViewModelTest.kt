package com.example.wakemeup

import com.example.wakemeup.data.LocationSearchRepository
import com.example.wakemeup.model.Destination
import com.example.wakemeup.ui.map.MapViewModel
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: LocationSearchRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeLocationSearchRepository()
        viewModel = MapViewModel(fakeRepository, geofenceManager = null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertNull(state.selectedDestination)
        assertEquals(1000f, state.radiusMeters, 0.01f)
        assertFalse(state.isAlarmActive)
        assertFalse(state.hasLocationPermission)
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun testSelectDestinationUpdatesState() {
        val destination = Destination(
            title = "Test Location",
            address = "123 Main St",
            latitude = 37.7749,
            longitude = -122.4194
        )

        viewModel.selectDestination(destination)

        val state = viewModel.uiState.value
        assertEquals(destination, state.selectedDestination)
        assertEquals("Test Location", state.searchQuery)
        assertEquals(GeoPoint(37.7749, -122.4194), state.cameraTargetPosition)
    }

    @Test
    fun testRadiusSliderClamping() {
        viewModel.onRadiusChanged(300f)
        assertEquals(500f, viewModel.uiState.value.radiusMeters, 0.01f)

        viewModel.onRadiusChanged(2500f)
        assertEquals(2500f, viewModel.uiState.value.radiusMeters, 0.01f)

        viewModel.onRadiusChanged(8000f)
        assertEquals(5000f, viewModel.uiState.value.radiusMeters, 0.01f)
    }

    @Test
    fun testClearDestinationResetsDestinationState() {
        val destination = Destination(
            title = "Test Location",
            address = "123 Main St",
            latitude = 37.7749,
            longitude = -122.4194
        )

        viewModel.selectDestination(destination)
        viewModel.clearDestination()

        val state = viewModel.uiState.value
        assertNull(state.selectedDestination)
        assertEquals("", state.searchQuery)
        assertNull(state.distanceToDestinationMeters)
    }

    @Test
    fun testPermissionStatusChanged() {
        viewModel.onPermissionStatusChanged(true)
        assertTrue(viewModel.uiState.value.hasLocationPermission)

        viewModel.onPermissionStatusChanged(false)
        assertFalse(viewModel.uiState.value.hasLocationPermission)
    }

    @Test
    fun testSelectLocationFromGeoPoint() {
        val point = GeoPoint(37.7749, -122.4194)
        viewModel.selectLocation(point)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Pinned Location", state.selectedDestination?.title)
        assertEquals(point, state.selectedDestination?.geoPoint)
    }

    @Test
    fun testOnMapClickCallsSelectLocation() {
        val point = GeoPoint(37.7749, -122.4194)
        viewModel.onMapClick(point)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Pinned Location", state.selectedDestination?.title)
        assertEquals(point, state.selectedDestination?.geoPoint)
    }

    @Test
    fun testPerformSearchPassesMapCenterToRepository() {
        var searchedCenter: GeoPoint? = null
        val customRepo = object : LocationSearchRepository {
            override suspend fun searchLocations(query: String, mapCenter: GeoPoint?, maxResults: Int): List<Destination> {
                searchedCenter = mapCenter
                return listOf(Destination(title = "Hospital", address = "123 Main", latitude = 40.0, longitude = -74.0))
            }
            override suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Destination {
                return Destination(title = "Pinned Location", address = "$latitude, $longitude", latitude = latitude, longitude = longitude)
            }
        }
        val customVm = MapViewModel(customRepo)
        val center = GeoPoint(40.75, -73.98)
        customVm.performSearch("hospital", mapCenter = center)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(center, searchedCenter)
        assertEquals(1, customVm.uiState.value.searchResults.size)
        assertEquals("Hospital", customVm.uiState.value.searchResults[0].title)
    }
}

class FakeLocationSearchRepository : LocationSearchRepository {
    override suspend fun searchLocations(query: String, mapCenter: GeoPoint?, maxResults: Int): List<Destination> {
        return emptyList()
    }

    override suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Destination {
        return Destination(
            title = "Pinned Location",
            address = "$latitude, $longitude",
            latitude = latitude,
            longitude = longitude
        )
    }
}
