package com.uspn.maps

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint

class ClientOsrm {
    private val baseUrl: String = "https://api-mla.cheikyadam.me"
    private val client = OkHttpClient()

    fun getRoute(latSrc: Double, lonSrc: Double, latDest: Double, lonDest: Double): List<GeoPoint>{
        val url = "${baseUrl}/route/v1/foot/${lonSrc},${latSrc};${lonDest},${latDest}?overview=full&geometries=geojson"
        val requete = Request.Builder().url(url).build()

        client.newCall(requete).execute().use { reponse ->
            if(!reponse.isSuccessful)
                return emptyList()

            val jsonString = reponse.body?.string() ?: return emptyList()
            val json = JSONObject(jsonString)

            val coordonnes = json
                .getJSONArray("routes")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            val noeud = mutableListOf<GeoPoint>()
            var lat: Double
            var lon: Double

            for(i in 0 until coordonnes.length()) {
                lon = coordonnes.getJSONArray(i).getDouble(0)
                lat = coordonnes.getJSONArray(i).getDouble(1)

                noeud.add(GeoPoint(lat, lon))
            }

            return noeud
        }
    }
}