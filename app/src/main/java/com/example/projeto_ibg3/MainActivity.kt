package com.example.projeto_ibg3

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.projeto_ibg3.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

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
                        android.util.Log.e("Navigation", "Erro ao navegar para configurações", e)
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