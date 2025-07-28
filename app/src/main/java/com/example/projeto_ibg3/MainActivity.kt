package com.example.projeto_ibg3

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.projeto_ibg3.databinding.ActivityMainBinding
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.model.SyncState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var syncRepository: SyncRepository

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Apenas destinos principais do drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_paciente_formulario,
                R.id.nav_lista
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Iniciar sincronização silenciosa
        startSilentSync()
    }

    private fun startSilentSync() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "=== INICIANDO SINCRONIZAÇÃO AUTOMÁTICA ===")

                // Observar o estado da sincronização apenas para logs
                syncRepository.startSync().collect { syncState ->
                    handleSyncState(syncState)
                }

            } catch (e: Exception) {
                Log.e(TAG, "=== ERRO INESPERADO NA SINCRONIZAÇÃO ===", e)
                Log.e(TAG, "Erro: ${e.message}")
                Log.e(TAG, "Causa: ${e.cause}")
                Log.e(TAG, "=== FIM DO ERRO ===")
            }
        }
    }

    private fun handleSyncState(syncState: SyncState) {
        when {
            syncState.isLoading -> {
                // Log de progresso
                val progress = if (syncState.totalItems > 0) {
                    " [${syncState.processedItems}/${syncState.totalItems}]"
                } else {
                    ""
                }
                Log.d(TAG, "SYNC: ${syncState.message}$progress")
            }

            syncState.error != null -> {
                // Log de erro
                Log.e(TAG, "=== SINCRONIZAÇÃO FALHOU ===")
                Log.e(TAG, "Erro: ${syncState.error}")
                Log.e(TAG, "Mensagem: ${syncState.message}")
                Log.e(TAG, "=== FIM DO ERRO DE SINCRONIZAÇÃO ===")
            }

            !syncState.isLoading && syncState.error == null -> {
                // Log de sucesso
                Log.i(TAG, "=== SINCRONIZAÇÃO CONCLUÍDA COM SUCESSO ===")
                Log.i(TAG, "Mensagem: ${syncState.message}")
                Log.i(TAG, "Horário: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(syncState.lastSyncTime))}")
                Log.i(TAG, "Items processados: ${syncState.processedItems}/${syncState.totalItems}")
                Log.i(TAG, "=== FIM DA SINCRONIZAÇÃO BEM-SUCEDIDA ===")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)

                // Verificar se já não estamos na tela de configuração
                if (navController.currentDestination?.id != R.id.nav_config) {
                    try {
                        // Usar a action específica baseada no destino atual
                        val actionId = when (navController.currentDestination?.id) {
                            R.id.nav_paciente_formulario -> R.id.action_formulario_to_config
                            R.id.nav_lista -> R.id.action_lista_to_config
                            R.id.nav_paciente_detalhe -> R.id.action_detalhe_to_config
                            else -> null
                        }

                        if (actionId != null) {
                            navController.navigate(actionId)
                        } else {
                            // Fallback para navegação direta
                            navController.navigate(R.id.nav_config)
                        }
                    } catch (e: Exception) {
                        Log.e("Navigation", "Erro ao navegar para configurações", e)
                    }
                }

                binding.drawerLayout.closeDrawers()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}