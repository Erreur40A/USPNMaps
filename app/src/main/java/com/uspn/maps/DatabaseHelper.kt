package com.uspn.maps

import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import org.osmdroid.util.GeoPoint

class DatabaseHelper(context: Context) :
    SQLiteAssetHelper(context, "campus.db", null, 1) {

    init {
        // Forcer la mise à jour s'il y a un changement de version
        setForcedUpgrade()
    }

    fun getAllData(): List<Salle> {
        val db = readableDatabase
        val cursor = db.rawQuery("""
        SELECT
            salles.id as salle_id,
            salles.code as salle_code,
            salles.nom as salle_nom,
            salles.composante,
            salles.etage,
            salles.description as salle_description,
            salles.long as salle_long,
            salles.lat as salle_lat,
            batiments.id as batiment_id,
            batiments.nom as batiment_nom,
            batiments.code as batiment_code,
            batiments.description as batiment_description,
            batiments.long as batiment_long,
            batiments.lat as batiment_lat,
            ImagesBatiments.id as image_id,
            ImagesBatiments.image as image_url,
            ImagesBatiments.description as image_description
        FROM salles
        INNER JOIN batiments ON salles.batiment_id = batiments.id
        LEFT JOIN ImagesBatiments ON batiments.id = ImagesBatiments.batiment_id
    """, null)

        // Map pour regrouper les salles par ID (pour éviter les doublons)
        val sallesMap = mutableMapOf<Int, Salle>()
        // Map pour regrouper les images par batiment_id
        val imagesMap = mutableMapOf<Int, MutableList<ImageBatiment>>()

        cursor.use {
            while (it.moveToNext()) {
                val salleId = it.getInt(it.getColumnIndexOrThrow("salle_id"))
                val batimentId = it.getInt(it.getColumnIndexOrThrow("batiment_id"))

                // Récupère ou crée la liste d'images pour ce bâtiment
                val images = imagesMap.getOrPut(batimentId) { mutableListOf() }
                if (!it.isNull(it.getColumnIndexOrThrow("image_id"))) {
                    images.add(ImageBatiment(
                        id = it.getInt(it.getColumnIndexOrThrow("image_id")),
                        image = it.getString(it.getColumnIndexOrThrow("image_url")),
                        description = it.getString(it.getColumnIndexOrThrow("image_description")),
                        batimentId = batimentId
                    ))
                }

                // Si la salle n'est pas encore dans la map, on la crée
                if (!sallesMap.containsKey(salleId)) {
                    val salleLongitude = it.getDouble(it.getColumnIndexOrThrow("salle_long"))
                    val salleLatitude = it.getDouble(it.getColumnIndexOrThrow("salle_lat"))
                    val batimentLongitude = it.getDouble(it.getColumnIndexOrThrow("batiment_long"))
                    val batimentLatitude = it.getDouble(it.getColumnIndexOrThrow("batiment_lat"))

                    val batiment = Batiment(
                        id = batimentId,
                        nom = it.getString(it.getColumnIndexOrThrow("batiment_nom")),
                        code = it.getString(it.getColumnIndexOrThrow("batiment_code")),
                        description = it.getString(it.getColumnIndexOrThrow("batiment_description")),
                        coord = GeoPoint(batimentLatitude, batimentLongitude),
                        images = emptyList() // On remplira après
                    )

                    val salle = Salle(
                        id = salleId,
                        code = it.getString(it.getColumnIndexOrThrow("salle_code")),
                        composante = it.getString(it.getColumnIndexOrThrow("composante")),
                        nom = it.getString(it.getColumnIndexOrThrow("salle_nom")),
                        etage = it.getString(it.getColumnIndexOrThrow("etage")),
                        batiment = batiment,
                        description = it.getString(it.getColumnIndexOrThrow("salle_description")),
                        coord = GeoPoint(salleLatitude, salleLongitude)
                    )
                    sallesMap[salleId] = salle
                }
            }
        }

        // On associe les images à chaque bâtiment
        sallesMap.values.forEach { salle ->
            salle.batiment.images = imagesMap[salle.batiment.id] ?: emptyList()
        }

        return sallesMap.values.toList()
    }

}
