package com.uspn.maps

import org.osmdroid.util.GeoPoint

data class Batiment(
    val id: Int,
    val nom: String,
    val code: String,
    val description: String?,
    val coord: GeoPoint,
    var images :List<ImageBatiment>
)
