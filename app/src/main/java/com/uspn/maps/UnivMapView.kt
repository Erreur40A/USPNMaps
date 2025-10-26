package com.uspn.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.net.URL

class UnivMapView @JvmOverloads constructor (
    context : Context,
    attributes : AttributeSet? = null
) : MapView(context, attributes) {

    var centreFac: GeoPoint? = null
    private var isRecentering = false
    private var currentMarker: Marker? = null

    private val handler = Handler(Looper.getMainLooper())
    private var polygon: Polygon? = null

    // Configuration du serveur OSRM local
    private val osrmServerUrl = "http://192.168.0.24:8000"

    fun Polygon.containsPoint(point: GeoPoint): Boolean {
        val pts = this.actualPoints
        val x = point.longitude
        val y = point.latitude
        var inside = false
        var j = pts.size - 1

        for (i in pts.indices) {
            val xi = pts[i].longitude
            val yi = pts[i].latitude
            val xj = pts[j].longitude
            val yj = pts[j].latitude

            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 0.0) + xi)

            if (intersect) inside = !inside

            j = i
        }
        return inside
    }


    fun centrage(zoomLevel : Double) {
        val centreCarte = mapCenter
        val centre = centreFac ?: return
        val poly = polygon ?: return

        if (isRecentering) return

        val positionCarte = GeoPoint(
            centreCarte.latitude,
            centreCarte.longitude
        )

        val estEnDehors = !poly.containsPoint(positionCarte)

        if (estEnDehors) {
            isRecentering = true
            controller.animateTo(centre, zoomLevel, 500L)
            handler.postDelayed({ isRecentering = false }, 500)
        }
    }

    fun setPolygon(p: Polygon) {
        polygon = p
    }

    fun showSalleMarker(salle: Salle) {
        val point = GeoPoint(salle.coord.latitude, salle.coord.longitude)

        // Supprimer le marqueur précédent s'il existe
        currentMarker?.let { overlays.remove(it) }

        // Créer le nouveau marqueur
        val marker = Marker(this).apply {
            position = point
            title = salle.nomSalle
            subDescription = "Bâtiment : ${salle.batiment}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = false

            // Action au clic : afficher les infos
            setOnMarkerClickListener { _, _ ->
                showSalleInfoDialog(salle)
                true
            }
        }

        // Ajouter sur la carte et sauvegarder la référence
        currentMarker = marker
        overlays.add(marker)

        // Centrer la carte sur la salle
        controller.animateTo(point)
        invalidate()
    }


    @SuppressLint("SetTextI18n")
    fun showSalleInfoDialog(salle: Salle) {
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // === Conteneur principal ===
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // === Image de la salle ===
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply {
                bottomMargin = 24
            }
            scaleType = ImageView.ScaleType.CENTER_CROP

            if (salle.cheminPhoto != null && salle.cheminPhoto.isNotBlank()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(salle.cheminPhoto)
                    setImageBitmap(bitmap)
                } catch (_: Exception) {
                    setImageResource(android.R.drawable.ic_menu_report_image)
                }
            } else {
                setImageResource(android.R.drawable.ic_menu_report_image)
            }
        }

        // === Nom de la salle ===
        val nomView = TextView(context).apply {
            text = salle.nomSalle
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        // === Bâtiment, composante, étage ===
        val detailsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 8)
        }

        val batimentView = TextView(context).apply {
            text = "🏢 Bâtiment : ${salle.batiment}"
            textSize = 16f
            setTextColor(Color.DKGRAY)
        }

        val composanteView = TextView(context).apply {
            text = "🏛 Composante : ${salle.composante}"
            textSize = 16f
            setTextColor(Color.DKGRAY)
        }

        val etageView = TextView(context).apply {
            text = "🧭 Étage : ${salle.etage}"
            textSize = 16f
            setTextColor(Color.DKGRAY)
        }

        // === Description ===
        val descView = TextView(context).apply {
            text = salle.description ?: "Aucune description disponible."
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 0)
        }

        // === Bouton de fermeture ===
        val closeButton = Button(context).apply {
            text = "Fermer"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 48f
                setColor("#293358".toColorInt())
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = params
            setPadding(20, 8, 20, 8)

            setOnClickListener { dialog.dismiss() }
        }

        // === Assemblage des vues ===
        detailsLayout.addView(batimentView)
        detailsLayout.addView(composanteView)
        detailsLayout.addView(etageView)

        container.addView(imageView)
        container.addView(nomView)
        container.addView(detailsLayout)
        container.addView(descView)
        container.addView(closeButton)

        dialog.setContentView(container)

        // Style de la fenêtre
        dialog.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    fun createOSRMRoute(salle: Salle, locationUser: GeoPoint?, map: MapView) {
        if(locationUser == null){
            return
        }

        // Utilisation du serveur OSRM local sur campus.osm.pbf
        val url = "$osrmServerUrl/route/v1/foot/${locationUser.longitude},${locationUser.latitude};${salle.coord.longitude},${salle.coord.latitude}?overview=full&geometries=geojson"

        Thread {
            try {
                val json = URL(url).readText()
                val coords = JSONObject(json)
                    .getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")

                val roadPoints = ArrayList<GeoPoint>()
                for (i in 0 until coords.length()) {
                    val point = coords.getJSONArray(i)
                    roadPoints.add(GeoPoint(point.getDouble(1), point.getDouble(0)))
                }

                map.post {
                    val line = Polyline().apply {
                        setPoints(roadPoints)
                        outlinePaint.color = Color.BLUE
                        outlinePaint.strokeWidth = 5f
                    }
                    map.overlays.add(line)
                    map.invalidate()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Log l'erreur pour le débogage
                Log.e("OSRM", "Erreur de routage: ${e.message}")
            }
        }.start()
    }

}