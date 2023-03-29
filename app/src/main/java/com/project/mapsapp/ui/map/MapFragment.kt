package com.project.mapsapp.ui.map
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
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
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.project.mapsapp.R
import com.project.mapsapp.databinding.FragmentMapBinding
import java.io.IOException
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var searchAdapter: ArrayAdapter<String>
    private lateinit var placesClient: PlacesClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
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

        val apiKey = getString(R.string.maps_api_key)
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())

//        adapters()

        listeners()
    }

//    private fun adapters() {
//        searchAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line)
//        binding.mapSearch.setAdapter(searchAdapter)
//    }

    private fun listeners() {
        binding.mapSearchButton.setOnClickListener {
            val location = binding.mapSearch.text.toString()
            searchLocation(location)
        }

        binding.mapClearMarker.setOnClickListener {
            map.clear()
        }

        binding.mapLocateButton.setOnClickListener {
            getCurrentLocation()
        }

//        binding.mapSearch.addTextChangedListener {
//            val TAG = "/@/"
//
//            if (it != null && it.length > 2) {
//                val request = FindAutocompletePredictionsRequest.builder()
//                    .setTypeFilter(TypeFilter.ADDRESS)
//                    .setQuery(it.toString())
//                    .build()
//
//                placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
//                    searchAdapter.clear()
//                    response.autocompletePredictions.forEach {
//                        searchAdapter.add(it.toString())
//                    }
//                    searchAdapter.notifyDataSetChanged()
//                }.addOnFailureListener { exception ->
//                    Log.e(TAG, "Autocomplete prediction request failed: $exception")
//                    Toast.makeText(requireContext(), exception.toString(), Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                searchAdapter.clear()
//                searchAdapter.notifyDataSetChanged()
//            }
//        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener { point ->
            addMarker(point)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                LatLng(location.latitude, location.longitude).let {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(it, 15f)
                    )
                    addMarker(it)
                }
            }
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
            Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
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
}