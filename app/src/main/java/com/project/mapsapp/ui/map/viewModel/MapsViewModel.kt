package com.project.mapsapp.ui.map.viewModel

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class MapsViewModel: ViewModel() {

    private var mapInstance : GoogleMap? = null

    fun getMapInstance() = mapInstance

    fun setMapInstance(map :GoogleMap) {
        mapInstance = map
    }

    private var mapMarkers : ArrayList<LatLng> = arrayListOf()

    fun getMapMarkers() = mapMarkers

    fun setMapMarkers(markers :ArrayList<LatLng>) {
        mapMarkers.clear()
        mapMarkers.addAll(markers)
    }

}