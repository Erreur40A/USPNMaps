package com.uspn.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable
import androidx.viewpager2.widget.ViewPager2
import com.uspn.maps.ui.theme.ImagePagerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Polyline

class UnivMapView @JvmOverloads constructor (
    context : Context,
    attributes : AttributeSet? = null
) : MapView(context, attributes) {

    var centreFac: GeoPoint = GeoPoint(48.95713, 2.34127)
    private var isRecentering = false
    private var currentMarker: Marker? = null

    private val handler = Handler(Looper.getMainLooper())
    private var polygon: Polygon? = null
    private var currentRoute: Polyline? = null

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

    fun centrage(zoomLevel: Double) {
        val centreCarte = mapCenter
        val poly = polygon ?: return

        if (isRecentering) return

        val positionCarte = GeoPoint(
            centreCarte.latitude,
            centreCarte.longitude
        )

        val estEnDehors = !poly.containsPoint(positionCarte)

        if (estEnDehors) {
            isRecentering = true
            controller.animateTo(centreFac, zoomLevel, 500L)
            handler.postDelayed({ isRecentering = false }, 500)
        }
    }

    fun setPolygon(p: Polygon) {
        polygon = p
    }

    fun showSalleMarker(salle: Salle) {
        val point = GeoPoint(salle.coord.latitude, salle.coord.longitude)

        // Supprimer le marqueur précédent s’il existe
        currentMarker?.let { overlays.remove(it) }

        // Créer le nouveau marqueur
        val marker = Marker(this).apply {
            position = point
            title = salle.nom
            // TODO A REVOID BATIMENID

            subDescription = "Bâtiment : ${salle.batiment.code}"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = false

            // Action au clic : afficher les infos
            setOnMarkerClickListener { m, _ ->
                showSalleInfoDialog(salle)
                //Toast.makeText(context, "${salle.nomSalle} (${salle.batiment})", Toast.LENGTH_SHORT).show()
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
    // a ameliorer
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
        val imageSlider = createImageSlider(context, salle.batiment.images)

        // === Nom de la salle ===
        val nomView = TextView(context).apply {
            text = salle.nom
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
            text = "🏢 Bâtiment : ${salle.batiment.nom}"
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
            text = if (salle.description.isNullOrEmpty()) "" else salle.description
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

        container.addView(imageSlider)
        container.addView(nomView)
        container.addView(detailsLayout)
        container.addView(descView)
        container.addView(closeButton)

        dialog.setContentView(container)

        // Style de la fenêtre
        dialog.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            setLayout(width, LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setGravity(Gravity.CENTER)
        }

        dialog.show()
    }

    fun showRoute(points: List<GeoPoint>) {
        currentRoute?.let { overlays.remove(it) }

        val route = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 5f
        }

        overlays.add(route)
        currentRoute = route
        invalidate()
    }

    private fun isConnected(): Boolean {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun createRoute(salle: Salle, locationUser: GeoPoint) {
        val osrm = ClientOsrm()

        CoroutineScope(Dispatchers.IO).launch {
            val latSrc = locationUser.latitude
            val lonSrc = locationUser.longitude
            val latDest = salle.coord.latitude
            val lonDest = salle.coord.longitude

            var points: List<GeoPoint>
            if (isConnected()) {
                points = osrm.getRoute(latSrc, lonSrc, latDest, lonDest)
            } else {
                points = emptyList()
                Log.w("UNIVMAPVIEW", "pas de connexion")
            }

            withContext(Dispatchers.Main) {
                showRoute(points)
            }
        }
    }


    fun createImageSlider(context: Context, images: List<ImageBatiment>): LinearLayout {
        // Conteneur principal
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        // ViewPager2
        val viewPager = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply { bottomMargin = 24 }
        }

        // Adapter
        val adapterImages = if (images.isEmpty()) {
            Log.d("EMPTY", "IMAGES")
            listOf(ImageBatiment(0, "default-img", null, 0))
        } else {
            Log.d("NOT EMPTY", "IMAGES")
            images
        }
        viewPager.adapter = ImagePagerAdapter(context, adapterImages)

        // Flèches (si plusieurs images)
        if (adapterImages.size > 1) {
            val arrowsLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val leftArrow = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_media_previous)  // Flèche gauche système
                setOnClickListener { viewPager.currentItem = viewPager.currentItem - 1 }
                layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                    marginEnd = 16
                }
            }

            val rightArrow = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_media_next)  // Flèche droite système
                setOnClickListener { viewPager.currentItem = viewPager.currentItem + 1 }
                layoutParams = LinearLayout.LayoutParams(48, 48)
            }

            arrowsLayout.addView(leftArrow)
            arrowsLayout.addView(rightArrow)
            container.addView(arrowsLayout)
        }

        container.addView(viewPager)
        return container
    }

}