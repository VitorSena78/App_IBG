package com.example.projeto_ibg3.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NetworkUtils"
    }

    /**
     * Verifica se o dispositivo tem conexão de rede (não necessariamente internet)
     * Ideal para redes locais e hotspots
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            val hasTransport = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            Log.d(TAG, "Network available: $hasTransport")
            Log.d(TAG, "WiFi: ${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
            Log.d(TAG, "Cellular: ${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}")
            Log.d(TAG, "Ethernet: ${networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}")

            hasTransport
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar conectividade", e)
            false
        }
    }

    /**
     * Versão que verifica se tem internet real (pode falhar em redes locais)
     * Use apenas quando necessário verificar internet externa
     */
    fun isInternetReachable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Versão específica para redes locais/hotspot
     * Não depende de validação de internet
     */
    fun isLocalNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            // Verifica apenas se há transporte, sem validação de internet
            val isAvailable = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            Log.d(TAG, "Local network available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar rede local", e)
            false
        }
    }

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

    /**
     * Debug detalhado da conectividade
     */
    fun debugNetworkState() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            Log.d(TAG, "=== DEBUG NETWORK STATE ===")
            Log.d(TAG, "Active network: $network")
            Log.d(TAG, "Capabilities: $capabilities")

            if (capabilities != null) {
                Log.d(TAG, "WiFi: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
                Log.d(TAG, "Cellular: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}")
                Log.d(TAG, "Ethernet: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}")
                Log.d(TAG, "Internet capability: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
                Log.d(TAG, "Validated: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
            }
            Log.d(TAG, "=== END DEBUG ===")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no debug", e)
        }
    }
}