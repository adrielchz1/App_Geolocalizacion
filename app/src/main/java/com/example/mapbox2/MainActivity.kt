package com.example.mapbox2

import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.location.*
import com.mapbox.common.location.LocationUpdatesReceiver.Companion.TAG
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.viewport.state.FollowPuckViewportState
import com.mapbox.maps.plugin.viewport.viewport
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.util.Calendar

class MainActivity : ComponentActivity(), PermissionsListener {
    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-98.0, 39.5))
                .pitch(0.0)
                .zoom(2.0)
                .bearing(0.0)
                .build()
        )

        permissionsManager = PermissionsManager(this)
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            mapView.location.apply {
                enabled = true
                pulsingEnabled = true
            }
        } else {
            permissionsManager.requestLocationPermissions(this)
        }
        with(mapView) {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            location.enabled = true
            location.pulsingEnabled = true
            location.puckBearing = PuckBearing.COURSE
            viewport.transitionTo(
                targetState = viewport.makeFollowPuckViewportState(),
                transition = viewport.makeImmediateViewportTransition()
            )
        }
        val viewportPlugin = mapView.viewport
        val followPuckViewportState: FollowPuckViewportState = viewportPlugin.makeFollowPuckViewportState(
            FollowPuckViewportStateOptions.Builder()
                .bearing(FollowPuckViewportStateBearing.Constant(0.0))
                .padding(EdgeInsets(200.0 * resources.displayMetrics.density, 0.0, 0.0, 0.0))
                .build()
        )
        viewportPlugin.transitionTo(followPuckViewportState) { success ->
            // the transition has been completed with a flag indicating whether the transition succeeded
        }
        val locationService : LocationService = LocationServiceFactory.getOrCreate()
        var locationProvider: DeviceLocationProvider? = null

        val request = LocationProviderRequest.Builder()
            .interval(IntervalSettings.Builder().interval(0L).minimumInterval(0L).maximumInterval(0L).build())
            .displacement(0F)
            .accuracy(AccuracyLevel.HIGHEST)
            .build();

        val result = locationService.getDeviceLocationProvider(request)
        if (result.isValue) {
            locationProvider = result.value!!
        }

        val locationObserver = object: LocationObserver {
            override fun onLocationUpdateReceived(locations: MutableList<Location>) {
                Log.e(TAG, "Location update received: " + locations)
                longitude  = locations[0].longitude
                latitude = locations[0].latitude
            }
        }
        locationProvider?.addLocationObserver(locationObserver)

        // Llamar a la función insertarDatos cada 5 segundos
        val handler = Handler()
        val delay = 5000L // 5 segundos
        handler.postDelayed(object : Runnable {
            override fun run() {
                insertarDatos(mapView)
                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        // Implementa la lógica necesaria para explicar al usuario por qué se necesitan los permisos
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapView.location.apply {
                enabled = true
                pulsingEnabled = true
            }
        } else {
            // El usuario denegó el permiso
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun insertarDatos(view: View) {
        try {
            val connection: Connection
            val url = "jdbc:mysql://207.244.255.46/ratiosof74bo_uv_bd"
            val usuario = "ratiosof74bo_uv_bd_user"
            val contra = "Estudiante@123"

            val politica: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(politica)
            connection = DriverManager.getConnection(url, usuario, contra)

            // Obtiene la hora actual del dispositivo
            val currentTime = Calendar.getInstance().timeInMillis

            val query = "INSERT INTO adrielmap (longitud,latitud, hora) VALUES (?, ?, ?)"
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setDouble(1, longitude)
            statement.setDouble(2, latitude)
            statement.setTimestamp(3, Timestamp(currentTime))

            statement.executeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Error al insertar datos: ${e.message}")
        }
    }

}
