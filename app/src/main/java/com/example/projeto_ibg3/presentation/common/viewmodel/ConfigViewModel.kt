package com.example.projeto_ibg3.presentation.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val networkManager: NetworkManager
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    private val _serverStatus = MutableStateFlow(false)
    val serverStatus: StateFlow<Boolean> = _serverStatus.asStateFlow()

    fun checkConnection() {
        viewModelScope.launch {
            _connectionStatus.value = networkManager.checkConnection()
            _serverStatus.value = networkManager.testServerConnection()
        }
    }
}