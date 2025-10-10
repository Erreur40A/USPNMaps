package com.uspn.maps
import android.content.Context
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

class DatabaseHelper(context: Context):

    SQLiteAssetHelper(context, "ig.db", null, 1) {

        init {
            // Options disponibles
            setForcedUpgrade() // Forcer la mise à jour si version change
        }

        // Vos méthodes personnalisées
        fun getAllData(): List<Salle> {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM your_table", null)
            val dataList = mutableListOf<Salle>()

            cursor.use {
                while (it.moveToNext()) {
                    val data = Salle(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        nom_salle = it.getString(it.getColumnIndexOrThrow("nom_salle")),
                        etage = it.getString(it.getColumnIndexOrThrow("etage")),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        longitude = it.getFloat(it.getColumnIndexOrThrow("longitude")),
                        latitude = it.getFloat(it.getColumnIndexOrThrow("latitude"))
                    )
                    dataList.add(data)
                }
            }
            return dataList
        }
}