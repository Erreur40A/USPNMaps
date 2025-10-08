package com.uspn.maps

import org.osmdroid.util.GeoPoint

data class Salle(
    val nom: String,
    val etage: Int,
    val description: String,
    val position: GeoPoint
)
