package com.project.mapsapp.util

import android.net.ConnectivityManager
import android.net.Network

class NetworkCallBackImpl(private val onNetworkAvailable: () -> Unit, private val onNetworkLost: () -> Unit) :
        ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            onNetworkAvailable.invoke()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            onNetworkLost.invoke()
        }
    }