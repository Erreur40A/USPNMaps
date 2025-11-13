package com.uspn.maps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.events.MapListener
import org.osmdroid.views.overlay.Polygon
import com.uspn.maps.ui.theme.SearchManager
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.app.AlertDialog
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var map : UnivMapView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var searchManager: SearchManager
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var localisationProvider:  GpsMyLocationProvider
    private lateinit var localisationManager: LocationManager
    private lateinit var userPos: GeoPoint

    // Coordonnees de la fac
    val coordUniv = listOf(
        GeoPoint(48.96000, 2.33700),
        GeoPoint(48.96000, 2.34600),
        GeoPoint(48.95400, 2.34600),
        GeoPoint(48.95400, 2.33700)
    )

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showUserLocation()
        }
    }

    //fonction qui s'execute lorsqu'on lance l'apk
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //vérification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

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

            myLocationOverlay = MyLocationNewOverlay(localisationProvider, map)

            try {
                map.createRoute(salle, userPos)
            } catch (_: UninitializedPropertyAccessException) { }
        }

        layout.addView(searchLayout, searchParams)
        setContentView(layout)

        val polygon = Polygon(map).apply {
            points = coordUniv
            outlinePaint.strokeWidth = 0.0f
            outlinePaint.style = android.graphics.Paint.Style.STROKE
            outlinePaint.color = Color.TRANSPARENT
            fillPaint.color = Color.TRANSPARENT
        }

        map.overlays.add(polygon)
        map.setPolygon(polygon)

        map.controller.setZoom(17.0)
        map.controller.setCenter(map.centreFac)

        map.addMapListener (object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                map.centrage(map.zoomLevelDouble)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                return true
            }
        })

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                InfoWindow.closeAllInfoWindowsOn(map)
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return true
            }
        }
        map.overlays.add(MapEventsOverlay(mapEventsReceiver))

        localisationProvider = GpsMyLocationProvider(this).apply{
            locationUpdateMinTime = 2000
            locationUpdateMinDistance = 5f
        }
        localisationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        myLocationOverlay = MyLocationNewOverlay(localisationProvider, map)

        /*Commentaire à retirer si on est dans la fac*/
         checkGpsEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()

        /*Commentaire à retirer si on est dans la fac*/
        val isGpsEnabled = localisationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (isGpsEnabled) {
            showUserLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun showUserLocation() {
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        map.overlays.add(myLocationOverlay)

        myLocationOverlay.runOnFirstFix {
            runOnUiThread {
                myLocationOverlay.enableFollowLocation()
                val location = myLocationOverlay.myLocation
                if (location != null) {
                    map.controller.animateTo(location)
                    map.controller.setZoom(18.0)
                    userPos = GeoPoint(location.latitude, location.longitude)
                } else {
                    Log.w("MainActivity", "Première position GPS non disponible")
                }
            }
        }
    }

    private fun checkGpsEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Activer la localisation")
            builder.setMessage("Le GPS est désactivé. Voulez-vous l’activer pour afficher votre position ?")
            builder.setPositiveButton("Oui") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            builder.setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Le GPS doit être activé pour afficher votre position.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            builder.setCancelable(false)
            builder.show()
        } else {
            showUserLocation()
        }
    }
}