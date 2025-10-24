package com.uspn.maps

import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import org.osmdroid.util.GeoPoint

class DatabaseHelper(context: Context) :
    SQLiteAssetHelper(context, "ig.db", null, 1) {

    init {
        // Forcer la mise à jour s'il y a un changement de version
        setForcedUpgrade()
    }

    fun getAllData(): List<Salle> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM salles", null)
        val dataList = mutableListOf<Salle>()

        cursor.use {
            while (it.moveToNext()) {
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val data = Salle(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    code = it.getString(it.getColumnIndexOrThrow("code")),
                    composante = it.getString(it.getColumnIndexOrThrow("composante")),
                    cheminPhoto = it.getString(it.getColumnIndexOrThrow("chemin_photo")),
                    nomSalle = it.getString(it.getColumnIndexOrThrow("nom_salle")),
                    etage = it.getString(it.getColumnIndexOrThrow("etage")),
                    batiment = it.getString(it.getColumnIndexOrThrow("batiment")),
                    description = it.getString(it.getColumnIndexOrThrow("description")),
                    coord = GeoPoint(latitude, longitude)
                )
                dataList.add(data)
            }
        }
        return dataList
    }
}
