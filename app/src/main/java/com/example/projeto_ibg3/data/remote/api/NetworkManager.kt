package com.example.projeto_ibg3.data.remote.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "NetworkManager"
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Verifica conexão de rede (ideal para redes locais)
     */
    fun checkConnection(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            // Para redes locais, não usar NET_CAPABILITY_VALIDATED
            val isConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            Log.d(TAG, "Network check result: $isConnected")
            _isConnected.value = isConnected
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar conexão", e)
            false
        }
    }

    /**
     * Testa conectividade com o servidor
     */
    suspend fun testServerConnection(): Boolean {
        return try {
            Log.d(TAG, "Testando conexão com servidor...")
            val response = apiService.healthCheck()

            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response successful: ${response.isSuccessful}")
            Log.d(TAG, "Response body: ${response.body()}")

            val isSuccess = response.isSuccessful
            Log.d(TAG, "Conexão com servidor: $isSuccess")

            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao conectar com servidor", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            false
        }
    }

    /**
     * Verificação completa: rede + servidor
     */
    suspend fun isFullyConnected(): Boolean {
        val hasNetwork = checkConnection()
        if (!hasNetwork) {
            Log.d(TAG, "Sem rede disponível")
            return false
        }

        val hasServer = testServerConnection()
        Log.d(TAG, "Conectividade completa: rede=$hasNetwork, servidor=$hasServer")
        return hasServer
    }

    /**
     * Debug completo da conectividade
     */
    fun debugConnectivity() {
        try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            Log.d(TAG, "=== CONNECTIVITY DEBUG ===")
            Log.d(TAG, "Active network: $network")
            Log.d(TAG, "Network capabilities: $capabilities")

            if (capabilities != null) {
                Log.d(TAG, "WiFi: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
                Log.d(TAG, "Cellular: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}")
                Log.d(TAG, "Ethernet: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}")
                Log.d(TAG, "Internet: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
                Log.d(TAG, "Validated: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
                Log.d(TAG, "Not metered: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)}")
            }
            Log.d(TAG, "========================")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no debug de conectividade", e)
        }
    }
}