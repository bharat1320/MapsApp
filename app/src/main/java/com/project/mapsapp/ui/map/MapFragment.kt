package com.project.mapsapp.ui.map

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.project.mapsapp.ui.util.NetworkCallBackImpl
import com.project.mapsapp.ui.util.customExtensionDialog
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapBinding
    val mapsViewModel: MapsViewModel by viewModels()
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: NetworkCallBackImpl
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var dialog: AlertDialog.Builder
    private var TAG = "MapFragment"

//    To tackle multiple clicks
    var searchThreadAvailability = false
    private val timeDelay = 2 // in seconds

//    All the even positions have latitude and odd positions have longitude
    private var markerHistory: ArrayList<LatLng> = arrayListOf()

    companion object {
        val KEY_INSTANCE_SAVED = "KEY_INSTANCE_SAVED"
        val KEY_EMAIL = "KEY_EMAIL"
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
                locationPermissionDeniedAlert()
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

        dialog = AlertDialog.Builder(context)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setUp()

        registerNetworkListener()
    }

    private fun registerNetworkListener() {
        if(!isNetworkConnected(requireContext())) {
            binding.mapNoInternetButton.visibility = View.VISIBLE
            offlineAlert()
        } else {
            binding.mapNoInternetButton.visibility = View.GONE
        }

        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = NetworkCallBackImpl(
            {
                Toast.makeText(requireContext(), resources.getString(R.string.connected_dialog), Toast.LENGTH_LONG).show()
                binding.mapNoInternetButton.visibility = View.GONE
            },
            {
                offlineAlert()
                binding.mapNoInternetButton.visibility = View.VISIBLE
            }
        )
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if(savedInstanceState != null) {
            if(savedInstanceState.getBoolean(KEY_INSTANCE_SAVED)) {
                map = mapsViewModel.getMapInstance()!!

                binding.mapTitle.text = savedInstanceState.getString(KEY_EMAIL)

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

        listeners()
    }

    private fun setUp() {
        val apiKey = getString(R.string.maps_api_key)
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        binding.mapTitle.text = arguments?.getString(LoginFragment.KEY_EMAIL,"")
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

        binding.mapNoInternetButton.setOnClickListener {
            offlineAlert()
        }

    }

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
                        gpsOffAlert()
                    }
                    map.isMyLocationEnabled = true
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), resources.getString(R.string.failed_location) + it, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), resources.getString(R.string.failed_location_search), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun locationPermissionDeniedAlert() {
        dialog.customExtensionDialog(resources.getString(R.string.request_location), acceptBlock = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        })
    }

    private fun gpsOffAlert() {
        dialog.customExtensionDialog(resources.getString(R.string.request_gps), acceptBlock =  {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        })
    }

    private fun offlineAlert() {
        dialog.customExtensionDialog(resources.getString(R.string.request_internet),
            resources.getString(R.string.wifi),
            resources.getString(R.string.mobile_data),{
//                For wifi
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            },{
//                For mobile data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
                    startActivity(intent)
                } else {
                    val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                    startActivity(intent)
                }
            })
    }

    private fun addMarker(latLng: LatLng) {
        markerHistory.add(latLng)
        addMarkerWithoutHistory(latLng)
    }

    private fun addMarkerWithoutHistory(latLng: LatLng) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses =
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: arrayListOf()
            val addressText = addresses[0]?.getAddressLine(0) ?: ""
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("lat:${latLng.latitude}, long:${latLng.longitude}")
                    .snippet(addressText)
            )
        } catch (e :Exception) {
            Log.e("${TAG}/Marker_Exception", e.toString())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INSTANCE_SAVED,true)
        outState.putString(KEY_EMAIL,binding.mapTitle.text.toString())
        mapsViewModel.saveMapInstance(map)
        mapsViewModel.saveMapMarkers(markerHistory)
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}