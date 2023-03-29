package com.project.mapsapp.ui.map
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.project.mapsapp.R
import com.project.mapsapp.databinding.FragmentMapBinding
import com.project.mapsapp.ui.login.LoginFragment
import com.project.mapsapp.ui.map.viewModel.MapsViewModel
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapBinding
    val mapsViewModel: MapsViewModel by activityViewModels()
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

//    To tackle multiple clicks
    var searchThreadAvailability = false
    private val timeDelay = 2 // in seconds

//    All the even positions have latitude and odd positions have longitude
    private var markerHistory: ArrayList<LatLng> = arrayListOf()

    companion object {
        val KEY_INSTANCE_SAVED = "instance_saved"
    }

    private lateinit var permissionLauncher : ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if(isGranted) {
                getCurrentLocation()
            } else {
                permissionDeniedAlert()
            }
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

        Toast.makeText(requireContext(), arguments?.getString(LoginFragment.KEY_EMAIL,"")?:"", Toast.LENGTH_SHORT).show()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(KEY_INSTANCE_SAVED)) {
                map = mapsViewModel.getMapInstance()!!

                setUp()

                listeners()

                markerHistory.addAll(mapsViewModel.getMapMarkers())
                markerHistory.forEach {
                    addMarkerWithoutHistory(it)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

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

        map.setOnMapClickListener { point ->
            addMarker(point)
        }

        binding.mapSearchButton.setOnClickListener {
            val location : String = binding.mapSearch.text.toString()
            if(!location.isBlank()) {
                searchLocationByName(location)
            }
        }

        binding.mapClearMarker.setOnClickListener {
            map.clear()
        }

        binding.mapLocateButton.setOnClickListener {
            if(!searchThreadAvailability) {
                Handler(Looper.getMainLooper()).postDelayed({
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    searchThreadAvailability = false
                }, (timeDelay * 1000).toLong())
            }
        }

    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        addMarker(latLng)
                    } else {
                        Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
                    }
                    map.isMyLocationEnabled = true
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to get location $it", Toast.LENGTH_SHORT).show()
                }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun searchLocationByName(location: String) {
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
            Toast.makeText(requireContext(), "Cant find requested Location", Toast.LENGTH_SHORT)
                .show()
        }
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

    private fun addMarker(latLng: LatLng) {
        markerHistory.add(latLng)
        addMarkerWithoutHistory(latLng)
    }

    private fun addMarkerWithoutHistory(latLng: LatLng) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: arrayListOf()
        val addressText = addresses[0]?.getAddressLine(0) ?: ""
        map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("lat:${latLng.latitude}, long:${latLng.longitude}")
                .snippet(addressText)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INSTANCE_SAVED,true)
        mapsViewModel.setMapInstance(map)
        mapsViewModel.setMapMarkers(markerHistory)
    }
}