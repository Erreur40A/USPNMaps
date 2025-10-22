package com.uspn.maps

import org.osmdroid.util.GeoPoint

data class Salle(
    val id: Int,
    val nom_salle: String,
    val etage: String,
    val description: String,
    val coord: GeoPoint
)
