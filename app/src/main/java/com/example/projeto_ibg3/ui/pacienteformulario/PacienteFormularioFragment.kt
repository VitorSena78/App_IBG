package com.example.projeto_ibg3.ui.pacienteformulario

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.projeto_ibg3.databinding.FragmentPacienteFormularioBinding
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PacienteFormularioFragment : Fragment() {

    private var _binding: FragmentPacienteFormularioBinding? = null
    private val binding get() = _binding!!

    private var pacienteId: Long = 0L
    private var isEditMode = false
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacienteFormularioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar se é edição ou criação
        pacienteId = arguments?.getLong("pacienteId", 0L) ?: 0L
        isEditMode = pacienteId > 0L

        setupUI()
        if (isEditMode) {
            loadPacienteData()
        }
    }

    private fun setupUI() {
        // Mudar título e botão baseado no modo
        if (isEditMode) {
            binding.txtTitle.text = "Editar Paciente"
            binding.btnSalvar.text = "Atualizar"
        } else {
            binding.txtTitle.text = "Novo Paciente"
            binding.btnSalvar.text = "Salvar"
        }

        binding.btnSalvar.setOnClickListener {
            if (isEditMode) {
                updatePaciente()
            } else {
                createPaciente()
            }
        }

        binding.btnLimpar.setOnClickListener {
            clearForm()
        }

        // Configurar DatePicker para o campo de data
        binding.etDataNascimento.setOnClickListener {
            showMaterialDatePicker()
        }
    }

    private fun showMaterialDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione a data de nascimento")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            calendar.time = selectedDate
            binding.etDataNascimento.setText(dateFormat.format(selectedDate))
            calculateAge()
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun calculateAge() {
        val dataNascimento = binding.etDataNascimento.text.toString()

        if (dataNascimento.isNotEmpty()) {
            try {
                val birthDate = dateFormat.parse(dataNascimento)
                if (birthDate != null) {
                    val today = Calendar.getInstance()
                    val birth = Calendar.getInstance()
                    birth.time = birthDate

                    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                    // Verificar se ainda não fez aniversário este ano
                    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    binding.etIdade.setText(age.toString())
                }
            } catch (e: Exception) {
                // Em caso de erro na conversão da data
                binding.etIdade.setText("")
            }
        } else {
            binding.etIdade.setText("")
        }
    }

    private fun clearForm() {
        binding.etNome.setText("")
        binding.etNomeMae.setText("")
        binding.etDataNascimento.setText("")
        binding.etIdade.setText("")
        binding.etCpf.setText("")
        binding.etSus.setText("")
        binding.etTelefone.setText("")
        binding.etEndereco.setText("")
    }

    private fun loadPacienteData() {
        // Carregar dados do paciente e preencher campos
        // pacienteRepository.getPacienteById(pacienteId) { paciente ->
        //     binding.etNome.setText(paciente.name)
        //     binding.etIdade.setText(paciente.age.toString())
        //     // etc...
        // }
    }

    private fun createPaciente() {
        // Validar campos
        if (validateForm()) {
            // Lógica para criar novo paciente
            val nome = binding.etNome.text.toString()
            val nomeMae = binding.etNomeMae.text.toString()
            val dataNascimento = binding.etDataNascimento.text.toString()
            val cpf = binding.etCpf.text.toString()
            val sus = binding.etSus.text.toString()
            val telefone = binding.etTelefone.text.toString()
            val endereco = binding.etEndereco.text.toString()

            // Implementar lógica de salvamento
        }
    }

    private fun updatePaciente() {
        // Lógica para atualizar paciente existente
        if (validateForm()) {
            // Implementar lógica de atualização
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validar nome
        if (binding.etNome.text.toString().trim().isEmpty()) {
            binding.tilNome.error = "Nome é obrigatório"
            isValid = false
        } else {
            binding.tilNome.error = null
        }

        // Validar CPF
        if (binding.etCpf.text.toString().trim().isEmpty()) {
            binding.tilCpf.error = "CPF é obrigatório"
            isValid = false
        } else {
            binding.tilCpf.error = null
        }

        // Validar data de nascimento
        if (binding.etDataNascimento.text.toString().trim().isEmpty()) {
            binding.tilDataNascimento.error = "Data de nascimento é obrigatória"
            isValid = false
        } else {
            binding.tilDataNascimento.error = null
        }

        // Adicionar outras validações conforme necessário

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(pacienteId: Long = 0L): PacienteFormularioFragment {
            val fragment = PacienteFormularioFragment()
            val bundle = Bundle().apply {
                putLong("pacienteId", pacienteId)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}