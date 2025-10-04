package com.uspn.maps.data

import com.uspn.maps.models.Building

object BuildingRepository {
    // Coordonnées du campus de Villetaneuse (Université Sorbonne Paris Nord)
    // Centre approximatif du campus
    val CAMPUS_CENTER_LAT = 48.9577
    val CAMPUS_CENTER_LON = 2.3407

    // Limites du campus (boundingbox)
    val CAMPUS_MIN_LAT = 48.9540
    val CAMPUS_MAX_LAT = 48.9614
    val CAMPUS_MIN_LON = 2.3360
    val CAMPUS_MAX_LON = 2.3454

    fun getAllBuildings(): List<Building> {
        return listOf(
            Building(
                name = "Bâtiment U",
                latitude = 48.9577,
                longitude = 2.3407,
                description = "Bâtiment principal - Administration et services centraux",
                type = "Administration"
            ),
            Building(
                name = "IUT",
                latitude = 48.9565,
                longitude = 2.3420,
                description = "Institut Universitaire de Technologie - Formations technologiques",
                type = "Enseignement"
            ),
            Building(
                name = "Institut Galilée",
                latitude = 48.9590,
                longitude = 2.3395,
                description = "Institut Galilée - Sciences et Ingénierie",
                type = "Recherche"
            ),
            Building(
                name = "Bibliothèque",
                latitude = 48.9570,
                longitude = 2.3390,
                description = "Bibliothèque universitaire - Ressources documentaires et espaces de travail",
                type = "Services"
            )
        )
    }

    fun searchBuildings(query: String): List<Building> {
        return getAllBuildings().filter {
            it.name.contains(query, ignoreCase = true)
        }
    }
}