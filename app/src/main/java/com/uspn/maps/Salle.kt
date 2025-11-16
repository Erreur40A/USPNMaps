package com.uspn.maps

import org.osmdroid.util.GeoPoint

data class Salle(
    val id:Int,
    val code: String,
    val nom: String,
    val composante:String,
    val etage: String,
    val batiment:Batiment,
    val description: String?,
    val coord: GeoPoint
)
