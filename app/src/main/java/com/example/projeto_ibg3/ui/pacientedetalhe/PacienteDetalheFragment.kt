package com.example.projeto_ibg3.ui.pacientedetalhe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.model.Especialidade
import com.example.projeto_ibg3.model.SyncStatus
import com.example.projeto_ibg3.viewmodel.PacienteDetalheViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
    private lateinit var tvPatientId: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvUpdatedAt: TextView
    private lateinit var btnEdit: MaterialButton
    private lateinit var chipGroupSpecialties: ChipGroup

    // Views para status de sincronização
    private lateinit var tvSyncStatus: TextView
    private lateinit var ivSyncStatus: ImageView
    private lateinit var viewSyncIndicator: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Usar o nome correto do argumento
        pacienteId = arguments?.getLong(ARG_PACIENTE_ID) ?: -1

        // Debug: Verificar se o ID está sendo recebido
        println("PacienteDetalheFragment: ID recebido = $pacienteId")
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
            println("PacienteDetalheFragment: Carregando paciente com ID = $pacienteId")
            viewModel.loadPaciente(pacienteId)
            viewModel.loadPacienteEspecialidades(pacienteId)
        } else {
            println("PacienteDetalheFragment: ID inválido!")
            Toast.makeText(requireContext(), "ID do paciente inválido", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
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
        tvPatientId = view.findViewById(R.id.tv_patient_id)
        tvCreatedAt = view.findViewById(R.id.tv_created_at)
        tvUpdatedAt = view.findViewById(R.id.tv_updated_at)
        btnEdit = view.findViewById(R.id.btn_edit)
        chipGroupSpecialties = view.findViewById(R.id.chip_group_specialties)

        //views para status de sincronização
        tvSyncStatus = view.findViewById(R.id.tv_sync_status)
        ivSyncStatus = view.findViewById(R.id.iv_sync_status)
        viewSyncIndicator = view.findViewById(R.id.view_sync_indicator)


        btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putLong(ARG_PACIENTE_ID, pacienteId)
            }
            findNavController().navigate(R.id.action_pacienteDetail_to_editPaciente, bundle)
        }
    }

    private fun observeViewModel() {
        viewModel.paciente.onEach { paciente ->
            println("PacienteDetalheFragment: Paciente recebido = $paciente")
            paciente?.let {
                displayPacienteData(it)
            } ?: run {
                println("PacienteDetalheFragment: Paciente é null!")
            }
        }.launchIn(lifecycleScope)

        // Observar especialidades do paciente
        viewModel.pacienteEspecialidades.onEach { especialidades ->
            println("PacienteDetalheFragment: Especialidades recebidas = $especialidades")
            displayEspecialidades(especialidades)
        }.launchIn(lifecycleScope)

        viewModel.error.onEach { error ->
            error?.let {
                println("PacienteDetalheFragment: Erro = $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }.launchIn(lifecycleScope)
    }

    private fun displaySyncStatus(paciente: Paciente) {
        when (paciente.syncStatus) {
            SyncStatus.SYNCED -> {
                tvSyncStatus.text = "Sincronizado"
                ivSyncStatus.setImageResource(R.drawable.ic_sync)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.success)
            }
            SyncStatus.PENDING_UPLOAD -> {
                tvSyncStatus.text = "Pendente upload"
                ivSyncStatus.setImageResource(R.drawable.ic_sync_disabled)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.warning)
            }
            SyncStatus.CONFLICT -> {
                tvSyncStatus.text = "Conflito"
                ivSyncStatus.setImageResource(R.drawable.ic_sync_problem)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error)
            }
            // Adicione outros status conforme necessário
            SyncStatus.PENDING_DELETE -> {
                tvSyncStatus.text = "Pendente exclusão"
                ivSyncStatus.setImageResource(R.drawable.ic_delete)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.warning)
            }
            SyncStatus.UPLOAD_FAILED -> {
                tvSyncStatus.text = "Falha no upload"
                ivSyncStatus.setImageResource(R.drawable.ic_error)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error)
            }
            SyncStatus.DELETE_FAILED -> {
                tvSyncStatus.text = "Falha na exclusão"
                ivSyncStatus.setImageResource(R.drawable.ic_error)
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.error)
            }
            SyncStatus.SYNCING -> {
                tvSyncStatus.text = "Sincronizando..."
                ivSyncStatus.setImageResource(R.drawable.ic_sync) // ou android.R.drawable.ic_popup_sync
                viewSyncIndicator.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)

                // Adicionar animação de rotação para o ícone
                val rotateAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
                ivSyncStatus.startAnimation(rotateAnimation)
            }
        }
    }

    private fun displayPacienteData(paciente: Paciente) {
        println("=== DEBUG PACIENTE ===")
        println("ID: ${paciente.id}")
        println("Nome: ${paciente.nome}")
        println("CreatedAt: ${paciente.createdAt}")
        println("UpdatedAt: ${paciente.updatedAt}")
        println("SyncStatus: ${paciente.syncStatus}")
        println("======================")

        // Debug das datas
        println("PacienteDetalheFragment: createdAt = ${paciente.createdAt}")
        println("PacienteDetalheFragment: updatedAt = ${paciente.updatedAt}")

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

        tvPatientId.text = "#${String.format("%06d", paciente.id)}"

        // Formatear datas de criação e atualização se existirem
        paciente.createdAt?.let {
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
            val formattedDate = dateTimeFormat.format(it)
            println("PacienteDetalheFragment: Data criação formatada = $formattedDate")
            tvCreatedAt.text = formattedDate
        } ?: run {
            println("PacienteDetalheFragment: createdAt é null")
            tvCreatedAt.text = "Não informado"
        }

        paciente.updatedAt?.let {
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
            val formattedDate = dateTimeFormat.format(it)
            println("PacienteDetalheFragment: Data atualização formatada = $formattedDate")
            tvUpdatedAt.text = formattedDate
        } ?: run {
            println("PacienteDetalheFragment: updatedAt é null")
            tvUpdatedAt.text = "Não informado"
        }

        displaySyncStatus(paciente)
    }

    private fun displayEspecialidades(especialidades: List<Especialidade>) {
        // Limpar chips existentes
        chipGroupSpecialties.removeAllViews()

        if (especialidades.isEmpty()) {
            // Se não há especialidades, adicionar um chip indicando isso
            val emptyChip = createChip("Nenhuma especialidade")
            emptyChip.apply {
                isEnabled = false
                chipBackgroundColor = resources.getColorStateList(android.R.color.darker_gray, null)
                setTextColor(resources.getColor(android.R.color.white, null))
            }
            chipGroupSpecialties.addView(emptyChip)
        } else {
            // Adicionar chips para cada especialidade
            especialidades.forEach { especialidade ->
                val chip = createChip(especialidade.nome)
                chipGroupSpecialties.addView(chip)
            }
        }
    }

    private fun createChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            textSize = 12f
            setTextColor(resources.getColor(R.color.primary, null))
            chipBackgroundColor = resources.getColorStateList(android.R.color.white, null)
            chipStrokeWidth = 0f
            chipCornerRadius = 24f
            isCloseIconVisible = false
            isClickable = false
            isCheckable = false

            // Definir padding interno do chip
            setPadding(24, 12, 24, 12)
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