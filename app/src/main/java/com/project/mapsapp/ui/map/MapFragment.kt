package com.project.mapsapp.ui.map
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.project.mapsapp.MainActivity
import com.project.mapsapp.R
import com.project.mapsapp.databinding.FragmentMapBinding
import java.io.IOException
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private var locationPermissionGranted = false

    var searchThreadAvailability = false
    private val timeDelay = 1 // in seconds

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) {
            locationPermissionGranted = true
            getCurrentLocation()
        } else {
            locationPermissionGranted = false
            permissionDeniedAlert()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUp()

        listeners()
    }

    private fun setUp() {
        val apiKey = getString(R.string.maps_api_key)
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun listeners() {
        binding.mapSearchButton.setOnClickListener {
            val location : String = binding.mapSearch.text.toString()
            if(!location.isBlank()) {
                searchLocation(location)
            }
        }

        binding.mapClearMarker.setOnClickListener {
            map.clear()
        }

        binding.mapLocateButton.setOnClickListener {
            if(!searchThreadAvailability) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if(locationPermissionGranted) {
                        getCurrentLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    searchThreadAvailability = false
                }, (timeDelay * 1000).toLong())
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener { point ->
            addMarker(point)
        }
    }

    private fun getCurrentLocation() {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Use location
                        val latLng = LatLng(location.latitude, location.longitude)
                        // Do something with latLng, e.g. move camera to user's location
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        addMarker(latLng)
                    } else {
                        // Location is null, handle accordingly
                        Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener {
                    // Failed to get location, handle accordingly
                    Toast.makeText(requireContext(), "Failed to get location $it", Toast.LENGTH_SHORT)
                        .show()
                }
        } else {
            // Location permission not granted, request it
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(requireContext())
        var addressList: List<Address>? = null
        try {
            addressList = geocoder.getFromLocationName(location, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            val latLng = LatLng(address.latitude, address.longitude)
            addMarker(latLng)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            permissionDeniedAlert()
        }
    }

    private fun addMarker(latLng: LatLng) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val addressText = addresses?.get(0)?.getAddressLine(0)
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("lat:${latLng.latitude}, long:${latLng.longitude}")
                .snippet(addressText ?: "Unknown")
        )
    }

    private fun permissionDeniedAlert() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("You have to provide us Location permission to use this feature" )
        builder.setCancelable(true)
        builder.setPositiveButton("Agree") { dialog, id ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
            dialog.cancel()
        }
        builder.setNegativeButton("No") { dialog, id ->
            dialog.cancel()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
}