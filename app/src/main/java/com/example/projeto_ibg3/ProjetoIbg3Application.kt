package com.example.projeto_ibg3

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ProjetoIbg3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializações globais futuras aqui
    }
}