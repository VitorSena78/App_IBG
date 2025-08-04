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
import androidx.core.content.ContextCompat
import com.example.projeto_ibg3.R

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

        return binding.root
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                // Observar especialidades
                viewModel.especialidades.collect { especialidades ->
                    setupSwitches(especialidades)
                }
            }

            launch {
                // Observar loading
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.containerSwitches.visibility = if (isLoading) View.GONE else View.VISIBLE
                }
            }

            launch {
                // Observar erros
                viewModel.error.collect { error ->
                    error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
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
                // Encontrar a especialidade completa para verificar fichas
                val especialidadeCompleta = especialidades.find { it.nome == nomeEspecialidade }
                val switch = createSwitch(nomeEspecialidade, especialidadeCompleta)
                binding.containerSwitches.addView(switch)
            }
        }
    }

    private fun createSwitch(nomeEspecialidade: String, especialidade: Especialidade?): SwitchCompat {
        return SwitchCompat(requireContext()).apply {
            // Verificar se a especialidade tem fichas disponíveis
            val temFichas = especialidade?.let { it.fichas > 0 } ?: true
            val fichasDisponiveis = especialidade?.fichas ?: 0

            // Texto do switch com informação das fichas
            text = if (temFichas) {
                "$nomeEspecialidade ($fichasDisponiveis fichas)"
            } else {
                "$nomeEspecialidade (ESGOTADA)"
            }

            val prefsKey = "especialidade_${nomeEspecialidade.lowercase().replace(" ", "_")}"

            // Se não tem fichas, forçar o switch como desabilitado e desmarcado
            if (!temFichas) {
                isChecked = false
                isEnabled = false
                // Salvar como desabilitado nas preferências
                sharedPreferences.edit()
                    .putBoolean(prefsKey, false)
                    .apply()

                // Estilo visual para indicar que está esgotado
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_disabled))
                alpha = 0.6f
            } else {
                // Comportamento normal para especialidades com fichas
                isChecked = sharedPreferences.getBoolean(prefsKey, true)
                isEnabled = true
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                alpha = 1.0f

                setOnCheckedChangeListener { _, isChecked ->
                    sharedPreferences.edit()
                        .putBoolean(prefsKey, isChecked)
                        .apply()
                }
            }

            // Styling
            setPadding(16, 16, 16, 16)
            textSize = 16f
        }
    }

    override fun onDestroyView() {
        // Limpar listeners antes de destruir
        for (i in 0 until binding.containerSwitches.childCount) {
            val switch = binding.containerSwitches.getChildAt(i) as? SwitchCompat
            switch?.setOnCheckedChangeListener(null)
        }

        binding.containerSwitches.removeAllViews()

        super.onDestroyView()
        _binding = null
    }
}