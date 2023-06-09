package com.project.mapsapp.ui.map.viewModel

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class MapsViewModel: ViewModel() {

    private var mapInstance : GoogleMap? = null

    fun getMapInstance() = mapInstance

    fun saveMapInstance(map :GoogleMap) {
        mapInstance = map
    }

}