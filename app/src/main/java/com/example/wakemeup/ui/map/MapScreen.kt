package com.example.wakemeup.ui.map

import android.Manifest
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wakemeup.model.Destination
import com.example.wakemeup.ui.map.components.DestinationControlCard
import com.example.wakemeup.ui.map.components.LocationSearchBar
import com.example.wakemeup.ui.map.components.PermissionBannerCard
import com.example.wakemeup.ui.theme.WakeMeUpTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.provideFactory(context))
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Notify viewModel of location permission status
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        viewModel.onPermissionStatusChanged(permissionsState.allPermissionsGranted)
    }

    // Handle error messages via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    MapScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
        onQueryChange = { viewModel.onQueryChanged(it) },
        onSearchSubmit = { viewModel.performSearch() },
        onDestinationSelect = { viewModel.selectDestination(it) },
        onMapClick = { viewModel.onMapClick(it) },
        onMarkerDragEnd = { viewModel.onMarkerDragEnd(it) },
        onRadiusChange = { viewModel.onRadiusChanged(it) },
        onToggleAlarm = { viewModel.toggleAlarm(context) },
        onClearDestination = { viewModel.clearDestination() },
        onCenterUserLocation = { viewModel.centerOnUserLocation() },
        onCameraMoved = { viewModel.onCameraMoved() },
        modifier = modifier
    )
}

@Composable
fun MapScreenContent(
    uiState: MapUiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onDestinationSelect: (Destination) -> Unit,
    onMapClick: (GeoPoint) -> Unit,
    onMarkerDragEnd: (GeoPoint) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onToggleAlarm: () -> Unit,
    onClearDestination: () -> Unit,
    onCenterUserLocation: () -> Unit,
    onCameraMoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize OSMDroid Configuration with specified User-Agent
    remember {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = "WakeMeUpApp/1.0 (contact: support@wakemeup.app)"
        true
    }

    // Define OSM Germany Tile Source (https://tile.openstreetmap.de/)
    val osmGermanyTileSource = remember {
        XYTileSource(
            "OSM_GERMANY",
            0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.de/")
        )
    }

    val defaultLocation = remember { GeoPoint(37.7749, -122.4194) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    // Handle lifecycle for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }

    // Smooth camera movement to target position
    LaunchedEffect(uiState.cameraTargetPosition) {
        uiState.cameraTargetPosition?.let { target ->
            mapViewRef.value?.let { mapView ->
                mapView.controller.animateTo(target, 16.0, 1000L)
            }
            onCameraMoved()
        }
    }

    // Initialize map camera center to uiState.userLocation (if available) at zoom level 16.0 on startup
    var hasCenteredOnStartup by remember { mutableStateOf(false) }
    val currentMapView = mapViewRef.value
    LaunchedEffect(uiState.userLocation, currentMapView) {
        val userLoc = uiState.userLocation
        if (!hasCenteredOnStartup && userLoc != null && currentMapView != null) {
            currentMapView.controller.setZoom(16.0)
            currentMapView.controller.setCenter(userLoc)
            hasCenteredOnStartup = true
        }
    }

    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val fillColorArgb = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f).toArgb()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // OSMDroid MapView via AndroidView
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(osmGermanyTileSource)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(16.0)
                        controller.setCenter(uiState.userLocation ?: uiState.selectedDestination?.geoPoint ?: defaultLocation)
                        mapViewRef.value = this
                    }
                },
                update = { mapView ->
                    mapViewRef.value = mapView
                    mapView.overlays.clear()

                    // Single-tap & long-press gesture listener to drop pin marker
                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onMapClick(p)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean {
                            onMapClick(p)
                            return true
                        }
                    }
                    mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

                    // My Location Overlay if location permission is granted
                    if (uiState.hasLocationPermission) {
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(mapView.context), mapView)
                        locationOverlay.enableMyLocation()
                        mapView.overlays.add(locationOverlay)
                    }

                    // User Location Pinpoint Marker when userLocation is present
                    uiState.userLocation?.let { userPos ->
                        val userLocationMarker = Marker(mapView).apply {
                            position = userPos
                            title = "Current Location"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            val iconDrawable = androidx.core.content.ContextCompat.getDrawable(
                                mapView.context,
                                org.osmdroid.library.R.drawable.person
                            ) ?: androidx.core.content.ContextCompat.getDrawable(
                                mapView.context,
                                android.R.drawable.ic_menu_myplaces
                            )
                            if (iconDrawable != null) {
                                icon = iconDrawable
                            }
                        }
                        mapView.overlays.add(userLocationMarker)
                    }

                    // Selected Destination Pin Marker and Radius Polygon Circle Overlay
                    uiState.selectedDestination?.let { destination ->
                        // Geofence alert radius circle overlay
                        val circlePolygon = Polygon(mapView).apply {
                            points = Polygon.pointsAsCircle(destination.geoPoint, uiState.radiusMeters.toDouble())
                            outlinePaint.color = primaryColorArgb
                            outlinePaint.strokeWidth = 4f
                            fillPaint.color = fillColorArgb
                        }
                        mapView.overlays.add(circlePolygon)

                        // Marker for selected destination pin
                        val marker = Marker(mapView).apply {
                            position = destination.geoPoint
                            title = destination.title
                            snippet = destination.address
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            isDraggable = true
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) {}
                                override fun onMarkerDragStart(marker: Marker) {}
                                override fun onMarkerDragEnd(marker: Marker) {
                                    onMarkerDragEnd(marker.position)
                                }
                            })
                        }
                        mapView.overlays.add(marker)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Controls Layout: Top Search Bar & Bottom Destination Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search Bar at Top
                LocationSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    onSearchSubmit = onSearchSubmit,
                    searchResults = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    onDestinationSelect = onDestinationSelect,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                // Action FABs: Center User Location & Destination
                Column(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (uiState.selectedDestination != null) {
                        FloatingActionButton(
                            onClick = {
                                onDestinationSelect(uiState.selectedDestination)
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = "Center on Destination"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.hasLocationPermission) {
                        FloatingActionButton(
                            onClick = {
                                onCenterUserLocation()
                                uiState.userLocation?.let { userLoc ->
                                    mapViewRef.value?.let { mapView ->
                                        mapView.controller.animateTo(userLoc, 16.0, 1000L)
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Center on My Location"
                            )
                        }
                    }
                }

                // Permission Warning Banner if permission not granted
                AnimatedVisibility(
                    visible = !uiState.hasLocationPermission,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PermissionBannerCard(onRequestPermission = onRequestPermission)
                }

                // Bottom Destination Control Card (Radius Slider & Alarm Button)
                DestinationControlCard(
                    destination = uiState.selectedDestination,
                    radiusMeters = uiState.radiusMeters,
                    onRadiusChange = onRadiusChange,
                    distanceMeters = uiState.distanceToDestinationMeters,
                    isAlarmActive = uiState.isAlarmActive,
                    onToggleAlarm = onToggleAlarm,
                    onClearDestination = onClearDestination
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    WakeMeUpTheme {
        MapScreenContent(
            uiState = MapUiState(
                selectedDestination = Destination(
                    title = "Central Park",
                    address = "New York, NY 10022",
                    latitude = 40.785091,
                    longitude = -73.968285
                ),
                radiusMeters = 1500f,
                distanceToDestinationMeters = 2400f,
                hasLocationPermission = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onRequestPermission = {},
            onQueryChange = {},
            onSearchSubmit = {},
            onDestinationSelect = {},
            onMapClick = {},
            onMarkerDragEnd = {},
            onRadiusChange = {},
            onToggleAlarm = {},
            onClearDestination = {},
            onCenterUserLocation = {},
            onCameraMoved = {}
        )
    }
}
