package com.example.projeto_ibg3.presentation.ui.config

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.projeto_ibg3.databinding.FragmentConfigBinding
import com.example.projeto_ibg3.domain.model.Especialidade
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfigViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setupObservers()
        setupRefreshButton()

        return binding.root
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar especialidades
            viewModel.especialidades.collect { especialidades ->
                setupSwitches(especialidades)
            }
        }

        lifecycleScope.launch {
            // Observar loading
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.containerSwitches.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            // Observar erros
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshEspecialidades()
        }
    }

    private fun setupSwitches(especialidades: List<Especialidade>) {
        // Limpar switches anteriores
        binding.containerSwitches.removeAllViews()

        // Se não há especialidades da API, usar lista padrão
        if (especialidades.isEmpty()) {
            Log.d("ConfigFragment", "Usando lista padrão de especialidades")
        } else {
            val especialidadesToShow = especialidades.map { it.nome }
            especialidadesToShow.forEach { nomeEspecialidade ->
                val switch = createSwitch(nomeEspecialidade)
                binding.containerSwitches.addView(switch)
            }
        }


    }

    private fun createSwitch(nomeEspecialidade: String): SwitchCompat {
        return SwitchCompat(requireContext()).apply {
            text = nomeEspecialidade
            val prefsKey = "especialidade_${nomeEspecialidade.lowercase().replace(" ", "_")}"
            isChecked = sharedPreferences.getBoolean(prefsKey, true)

            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit()
                    .putBoolean(prefsKey, isChecked)
                    .apply()
            }

            // Styling
            setPadding(16, 16, 16, 16)
            textSize = 16f
        }
    }

    private fun getDefaultEspecialidades(): List<String> {
        return listOf(
            "Cardiologia", "Pediatria", "Clínico Geral", "Neurologia",
            "Ginecologia", "Dermatologia", "Ortopedia", "Endocrinologia",
            "Oftalmologia", "Psiquiatria"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}