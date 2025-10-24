package com.uspn.maps

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
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
import androidx.core.graphics.toColorInt
import com.uspn.maps.ui.theme.SearchManager

class MainActivity : ComponentActivity() {
    private  lateinit var map : UnivMapView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var searchManager: SearchManager

    /// Coordonnees de la fac
    val centreFac = GeoPoint(48.95713, 2.34127)
    val coordUniv = listOf(
        GeoPoint(48.96000, 2.33700),
        GeoPoint(48.96000, 2.34600),
        GeoPoint(48.95400, 2.34600),
        GeoPoint(48.95400, 2.33700)
    )

    ///fonction qui s'execute lorsqu'on lance l'apk
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialisations ici db, map, etc
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        dbHelper = DatabaseHelper(this)
        searchManager = SearchManager(this, dbHelper)

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


        // Ajout de la barre de recherche
        val searchLayout = searchManager.createSearchLayout()
        val searchParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        }

        // executer lorsqu'une salle est selectionnee
        // on doit afficher le pin
        searchManager.onSalleSelected = { salle ->
            Log.d("MainActivity", "Salle choisie: ${salle.nomSalle}")
            map.showSalleMarker(salle)
        }

        layout.addView(searchLayout, searchParams)
        setContentView(layout)

        val polygon = Polygon(map).apply {
            points = coordUniv

            //  outlinePaint.color = 0x6FFF0000
            outlinePaint.strokeWidth = 0.0f
            outlinePaint.style = android.graphics.Paint.Style.STROKE
            outlinePaint.color = Color.TRANSPARENT
            fillPaint.color = Color.TRANSPARENT

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
    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}