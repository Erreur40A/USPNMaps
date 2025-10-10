package com.uspn.maps

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        val searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }

        ViewCompat.setOnApplyWindowInsetsListener(searchLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top + 16,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        val searchBar = EditText(this).apply {
            hint = "Rechercher une salle..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val searchButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setBackgroundColor(Color.TRANSPARENT)
        }

        searchLayout.addView(searchBar)
        searchLayout.addView(searchButton)

        val searchParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }

        layout.addView(searchLayout, searchParams)
        setContentView(layout)

        val polygon = Polygon(map).apply {
            points = coordUniv

            outlinePaint.color = 0x6FFF0000
            outlinePaint.strokeWidth = 0f
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