package com.uspn.maps

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

class UnivMapView @JvmOverloads constructor (
    context : Context,
    attributes : AttributeSet? = null
) : MapView(context, attributes) {

    var centreFac: GeoPoint? = null
    private var isRecentering = false

    private val handler = Handler(Looper.getMainLooper())
    private var polygon: Polygon? = null

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
}