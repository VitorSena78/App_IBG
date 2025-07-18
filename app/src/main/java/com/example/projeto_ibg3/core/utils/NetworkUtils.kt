package com.example.projeto_ibg3.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Verifica se o dispositivo tem conexão com a internet
     * @return true se há conexão, false caso contrário
     */
    fun isNetworkAvailable(): Boolean {
        // 1. Pega o serviço de conectividade do Android
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 2. Pega a rede ativa (atual)
        val network = connectivityManager.activeNetwork ?: return false

        // 3. Pega as capacidades da rede (o que ela pode fazer)
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // 4. Verifica se tem algum tipo de conexão válida
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||      // WiFi
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||  // Dados móveis
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)     // Ethernet
    }

    // Versão mais avançada que também verifica se a conexão realmente funciona
    fun isInternetReachable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Verifica o tipo específico de conexão
    fun getConnectionType(): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }
    }
}