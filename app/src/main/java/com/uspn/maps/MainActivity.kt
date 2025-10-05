package com.uspn.maps

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import org.osmdroid.config.Configuration
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.views.overlay.Polygon

class MainActivity : ComponentActivity() {
    private  lateinit var map : UnivMapView
    val centreFac = GeoPoint(48.95713, 2.34127)

    val coordUniv = listOf(
        GeoPoint(48.96000, 2.33700),
        GeoPoint(48.96000, 2.34600),
        GeoPoint(48.95400, 2.34600),
        GeoPoint(48.95400, 2.33700)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        map = UnivMapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 17.0
            maxZoomLevel = 21.0
        }

        val layout = FrameLayout(this)
        layout.addView(map, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(layout)

        val polygon = Polygon(map).apply {
            points = coordUniv

            outlinePaint.color = 0x6FFF0000
            outlinePaint.strokeWidth = 10f
            outlinePaint.style = android.graphics.Paint.Style.STROKE
        }

        map.overlays.add(polygon)
        map.setPolygon(polygon)

        map.centreFac = centreFac
        map.controller.setZoom(17.0)
        map.controller.setCenter(centreFac)

        map.addMapListener (object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                map.centrage(map.zoomLevelDouble)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return true
            }
        })
    }
}