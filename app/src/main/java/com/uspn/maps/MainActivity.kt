package com.uspn.maps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.uspn.maps.data.BuildingRepository
import com.uspn.maps.models.Building
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MaterialTheme {
                CampusMapScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen() {
    var selectedBuilding by remember { mutableStateOf<Building?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var showBuildingInfo by remember { mutableStateOf(false) }

    val buildings = remember { BuildingRepository.getAllBuildings() }
    val filteredBuildings = buildings.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Carte OpenStreetMap (sans pins au départ)
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(
                        BuildingRepository.CAMPUS_CENTER_LAT,
                        BuildingRepository.CAMPUS_CENTER_LON
                    ))

                    val boundingBox = BoundingBox(
                        BuildingRepository.CAMPUS_MAX_LAT,
                        BuildingRepository.CAMPUS_MAX_LON,
                        BuildingRepository.CAMPUS_MIN_LAT,
                        BuildingRepository.CAMPUS_MIN_LON
                    )
                    setScrollableAreaLimitDouble(boundingBox)

                    minZoomLevel = 16.0
                    maxZoomLevel = 19.0

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Barre de recherche
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showDropdown && searchText.isNotEmpty(),
                onExpandedChange = { }
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        showDropdown = it.isNotEmpty()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Rechercher un bâtiment...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Recherche") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                if (filteredBuildings.isNotEmpty() && searchText.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        filteredBuildings.forEach { building ->
                            DropdownMenuItem(
                                text = { Text(building.name) },
                                onClick = {
                                    // Supprimer l'ancien pin
                                    currentMarker?.let { marker ->
                                        mapView?.overlays?.remove(marker)
                                    }

                                    // Créer un nouveau pin
                                    val newMarker = Marker(mapView).apply {
                                        position = GeoPoint(building.latitude, building.longitude)
                                        title = building.name
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        setOnMarkerClickListener { _, _ ->
                                            selectedBuilding = building
                                            showBuildingInfo = true
                                            true
                                        }
                                    }

                                    mapView?.overlays?.add(newMarker)
                                    currentMarker = newMarker
                                    mapView?.invalidate()

                                    searchText = ""
                                    showDropdown = false

                                    // Centrer sur le bâtiment
                                    mapView?.controller?.animateTo(
                                        GeoPoint(building.latitude, building.longitude),
                                        18.0,
                                        1000L
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Overlay d'informations (uniquement au clic sur le pin)
        if (showBuildingInfo && selectedBuilding != null) {
            val building = selectedBuilding!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = building.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )

                        IconButton(onClick = {
                            showBuildingInfo = false
                            selectedBuilding = null
                        }) {
                            Icon(Icons.Default.Close, "Fermer")
                        }
                    }

                    Text(
                        text = "Type: ${building.type}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = building.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}