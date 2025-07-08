package com.example.projeto_ibg3.ui.slideshow

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projeto_ibg3.databinding.FragmentConfigBinding

class ConfigFragment : Fragment() {

    private lateinit var binding: FragmentConfigBinding
    private lateinit var sharedPreferences: SharedPreferences

    // Mesma lista de especialidades
    private val especialidades = listOf(
        "Cardiologia", "Pediatria", "Clínico Geral", "Neurologia",
        "Ginecologia", "Dermatologia", "Ortopedia", "Endocrinologia",
        "Oftalmologia", "Psiquiatria"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentConfigBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        setupSwitches()
        return binding.root
    }

    private fun setupSwitches() {
        especialidades.forEach { especialidade ->
            val switch = SwitchCompat(requireContext()).apply {
                text = especialidade
                isChecked = sharedPreferences.getBoolean("especialidade_${especialidade.lowercase().replace(" ", "_")}", true)

                setOnCheckedChangeListener { _, isChecked ->
                    sharedPreferences.edit()
                        .putBoolean("especialidade_${especialidade.lowercase().replace(" ", "_")}", isChecked)
                        .apply()
                }
            }
            binding.containerSwitches.addView(switch)
        }
    }
}