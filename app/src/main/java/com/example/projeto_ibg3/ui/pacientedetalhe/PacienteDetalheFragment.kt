package com.example.projeto_ibg3.ui.pacientedetalhe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.viewmodel.PacienteDetalheViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import java.text.SimpleDateFormat

@AndroidEntryPoint
class PacienteDetalheFragment : Fragment() {

    private val viewModel: PacienteDetalheViewModel by viewModels()
    private var pacienteId: Long = -1

    // Views
    private lateinit var tvName: TextView
    private lateinit var tvInitials: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvMotherName: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvCpf: TextView
    private lateinit var tvSus: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnCall: MaterialButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var chipCpf: Chip
    private lateinit var chipSus: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pacienteId = arguments?.getLong(ARG_PACIENTE_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_paciente_detalhe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        observeViewModel()

        if (pacienteId != -1L) {
            viewModel.loadPaciente(pacienteId)
        }
    }

    private fun setupViews(view: View) {
        tvName = view.findViewById(R.id.tv_name)
        tvInitials = view.findViewById(R.id.tv_initials)
        tvAge = view.findViewById(R.id.tv_age)
        tvMotherName = view.findViewById(R.id.tv_mother_name)
        tvBirthDate = view.findViewById(R.id.tv_birth_date)
        tvCpf = view.findViewById(R.id.tv_cpf)
        tvSus = view.findViewById(R.id.tv_sus)
        tvPhone = view.findViewById(R.id.tv_phone)
        tvAddress = view.findViewById(R.id.tv_address)
        btnCall = view.findViewById(R.id.btn_call)
        btnEdit = view.findViewById(R.id.btn_edit)
        btnBack = view.findViewById(R.id.btn_back)
        chipCpf = view.findViewById(R.id.chip_cpf)
        chipSus = view.findViewById(R.id.chip_sus)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putLong(ARG_PACIENTE_ID, pacienteId)
            }

            findNavController().navigate(R.id.action_pacienteDetail_to_editPaciente, bundle)
        }
    }

    private fun observeViewModel() {
        viewModel.paciente.onEach { paciente ->
            paciente?.let { displayPacienteData(it) }
        }.launchIn(lifecycleScope)

        viewModel.error.onEach { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }.launchIn(lifecycleScope)
    }

    private fun displayPacienteData(paciente: Paciente) {
        tvName.text = paciente.nome
        tvInitials.text = paciente.iniciais
        tvAge.text = "${paciente.idade} anos"
        tvMotherName.text = paciente.nomeDaMae

        // Formatar data de nascimento
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvBirthDate.text = dateFormat.format(paciente.dataNascimento)

        tvCpf.text = paciente.cpfFormatado
        tvSus.text = if (paciente.sus.isNotEmpty()) paciente.sus else "Não informado"
        tvPhone.text = paciente.telefoneFormatado
        tvAddress.text = paciente.endereco

        // Configurar chips
        chipCpf.text = "CPF: ${paciente.cpfFormatado}"
        chipSus.text = "SUS"
        chipSus.visibility = if (paciente.sus.isNotEmpty()) View.VISIBLE else View.GONE

        // Configurar botão de ligação
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${paciente.telefone}")
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Não foi possível realizar a ligação", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ARG_PACIENTE_ID = "paciente_id"

        fun newInstance(pacienteId: Long) = PacienteDetalheFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PACIENTE_ID, pacienteId)
            }
        }
    }
}