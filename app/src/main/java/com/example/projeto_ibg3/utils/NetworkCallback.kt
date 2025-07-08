package com.example.projeto_ibg3.utils

import android.net.ConnectivityManager
import android.net.Network

// uso mais avançado com callback
class NetworkCallback(private val onNetworkChanged: (Boolean) -> Unit) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        onNetworkChanged(true)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        onNetworkChanged(false)
    }
}