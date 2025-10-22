package com.uspn.maps

import org.osmdroid.util.GeoPoint

data class Salle(
    val id:Int,
   val code: String,
   val nomSalle: String,
   val composante:String,
   val etage: String,
   val batiment:String,
   val cheminPhoto:String?,
    val description: String?,
   val coord: GeoPoint
)
