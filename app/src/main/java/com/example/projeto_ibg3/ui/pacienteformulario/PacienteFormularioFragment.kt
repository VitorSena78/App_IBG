package com.example.projeto_ibg3.ui.pacienteformulario

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.widget.Toast
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.data.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.dao.PacienteDao
import com.example.projeto_ibg3.data.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.data.entity.PacienteEntity
import com.example.projeto_ibg3.data.entity.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.databinding.FragmentPacienteFormularioBinding
import com.example.projeto_ibg3.model.SyncStatus
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PacienteFormularioFragment : Fragment() {

    private var _binding: FragmentPacienteFormularioBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var pacienteDao: PacienteDao

    @Inject
    lateinit var pacienteEspecialidadeDao: PacienteEspecialidadeDao

    @Inject
    lateinit var especialidadeDao: EspecialidadeDao

    private var pacienteId: Long = 0L
    private var isEditMode = false
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentPaciente: PacienteEntity? = null

    // Componentes para especialidades
    private lateinit var chipGroupEspecialidades: ChipGroup
    private lateinit var sharedPreferences: SharedPreferences

    // Lista de especialidades disponíveis
    private val especialidadesDisponiveis = listOf(
        "Cardiologia", "Pediatria", "Clínico Geral", "Neurologia", "Ginecologia",
        "Dermatologia", "Ortopedia", "Endocrinologia", "Oftalmologia", "Psiquiatria"
    )

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

        // Inicializar especialidades ANTES de configurar a UI
        initializeEspecialidades()

        // Verificar se é edição ou criação
        pacienteId = arguments?.getLong("pacienteId", 0L) ?: 0L
        isEditMode = pacienteId > 0L

        Log.d("PacienteForm", "onCreate: pacienteId=$pacienteId, isEditMode=$isEditMode")

        // Inicializar componentes
        setupComponents()
        setupUI()
        setupEspecialidades()

        // IMPORTANTE: Só carregar dados se estiver em modo de edição
        if (isEditMode) {
            loadPacienteData()
        }
    }

    private fun setupComponents() {
        chipGroupEspecialidades = binding.chipGroupEspecialidades
        sharedPreferences = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    private fun setupUI() {
        // Mudar título e botão baseado no modo
        if (isEditMode) {
            binding.txtTitle.text = getString(R.string.editar_paciente)
            binding.btnSalvar.text = getString(R.string.atualizar)
        } else {
            binding.txtTitle.text = getString(R.string.novo_paciente)
            binding.btnSalvar.text = getString(R.string.salvar)
        }

        binding.btnSalvar.setOnClickListener {
            if (isEditMode) {
                updatePaciente()
            } else {
                createPacienteWithValidation()
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

    private fun loadPacienteData() {
        lifecycleScope.launch {
            try {
                Log.d("PacienteForm", "Carregando dados do paciente ID: $pacienteId")

                // Buscar paciente pelo ID
                val paciente = pacienteDao.getPacienteById(pacienteId)

                if (paciente != null) {
                    currentPaciente = paciente
                    Log.d("PacienteForm", "Paciente encontrado: ${paciente.nome}")

                    // Preencher campos com os dados do paciente
                    binding.etNome.setText(paciente.nome)
                    binding.etNomeMae.setText(paciente.nomeDaMae)
                    binding.etCpf.setText(paciente.cpf)
                    binding.etSus.setText(paciente.sus)
                    binding.etTelefone.setText(paciente.telefone)
                    binding.etEndereco.setText(paciente.endereco)
                    binding.etIdade.setText(paciente.idade.toString())

                    // Formatar e exibir data de nascimento
                    val dataFormatada = dateFormat.format(Date(paciente.dataNascimento))
                    binding.etDataNascimento.setText(dataFormatada)

                    // Carregar especialidades do paciente
                    loadPacienteEspecialidades()

                } else {
                    Log.e("PacienteForm", "Paciente não encontrado com ID: $pacienteId")
                    Toast.makeText(requireContext(), "Paciente não encontrado", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }

            } catch (e: Exception) {
                Log.e("PacienteForm", "Erro ao carregar dados do paciente", e)
                Toast.makeText(requireContext(), "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadPacienteEspecialidades() {
        lifecycleScope.launch {
            try {
                // Buscar especialidades do paciente
                val especialidadesDoPaciente = pacienteEspecialidadeDao.getEspecialidadeEntitiesByPacienteId(pacienteId)

                Log.d("PacienteForm", "Especialidades do paciente: ${especialidadesDoPaciente.size}")

                // Marcar as especialidades que o paciente possui
                especialidadesDoPaciente.forEach { especialidade ->
                    Log.d("PacienteForm", "Marcando especialidade: ${especialidade.nome}")
                    markEspecialidadeAsSelected(especialidade.nome)
                }

            } catch (e: Exception) {
                Log.e("PacienteForm", "Erro ao carregar especialidades do paciente", e)
            }
        }
    }

    private fun markEspecialidadeAsSelected(especialidade: String) {
        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as Chip
            if (chip.text.toString() == especialidade) {
                chip.isChecked = true
                Log.d("PacienteForm", "Especialidade marcada: $especialidade")
                break
            }
        }
    }

    private fun updatePaciente() {
        Log.d("PacienteForm", "Iniciando atualização do paciente ID: $pacienteId")

        if (validateForm()) {
            // Obter dados do formulário
            val nome = binding.etNome.text?.toString()?.trim() ?: ""
            val nomeMae = binding.etNomeMae.text?.toString()?.trim() ?: ""
            val dataNascimento = binding.etDataNascimento.text?.toString()?.trim() ?: ""
            val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
            val sus = binding.etSus.text?.toString()?.trim() ?: ""
            val telefone = binding.etTelefone.text?.toString()?.trim() ?: ""
            val endereco = binding.etEndereco.text?.toString()?.trim() ?: ""
            val especialidadesSelecionadas = getSelectedEspecialidades()

            // Converter data de nascimento para timestamp
            val dataNascimentoTimestamp = convertDateToTimestamp(dataNascimento)
            val idade = calculateAgeFromDate(dataNascimento)

            lifecycleScope.launch {
                try {
                    // Validar campos únicos (excluindo o paciente atual)
                    if (!validateUniqueFieldsForUpdate(cpf, sus.ifEmpty { null })) {
                        return@launch
                    }

                    // Atualizar entidade do paciente
                    currentPaciente?.let { paciente ->
                        val pacienteAtualizado = paciente.copy(
                            nome = nome,
                            nomeDaMae = nomeMae,
                            dataNascimento = dataNascimentoTimestamp,
                            idade = idade,
                            cpf = cpf,
                            sus = sus,
                            telefone = telefone,
                            endereco = endereco,
                            syncStatus = SyncStatus.PENDING_UPLOAD,
                            lastModified = System.currentTimeMillis()
                        )

                        // Atualizar no banco
                        pacienteDao.updatePaciente(pacienteAtualizado)

                        // Atualizar relacionamentos com especialidades
                        updateEspecialidadesRelationships(pacienteId, especialidadesSelecionadas)

                        Toast.makeText(
                            requireContext(),
                            "Paciente atualizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Voltar para a tela anterior
                        findNavController().navigateUp()
                    }

                } catch (e: Exception) {
                    Log.e("PacienteForm", "Erro ao atualizar paciente", e)
                    Toast.makeText(
                        requireContext(),
                        "Erro ao atualizar paciente: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun updateEspecialidadesRelationships(pacienteId: Long, especialidadesSelecionadas: List<String>) {
        try {
            // Remover todas as especialidades atuais do paciente
            pacienteEspecialidadeDao.deleteByPacienteId(pacienteId)

            // Adicionar as novas especialidades selecionadas
            especialidadesSelecionadas.forEach { nomeEspecialidade ->
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                if (especialidade != null) {
                    val pacienteEspecialidade = PacienteEspecialidadeEntity(
                        pacienteId = pacienteId,
                        especialidadeId = especialidade.id,
                        dataAtendimento = System.currentTimeMillis()
                    )

                    pacienteEspecialidadeDao.insertPacienteEspecialidade(pacienteEspecialidade)
                    Log.d("PacienteForm", "Relacionamento atualizado: Paciente $pacienteId - Especialidade ${especialidade.id}")
                }
            }

        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao atualizar relacionamentos com especialidades", e)
            throw e
        }
    }

    private suspend fun validateUniqueFieldsForUpdate(cpf: String, sus: String?): Boolean {
        // Limpar erros anteriores
        binding.tilCpf.error = null
        binding.tilSus.error = null

        // Verificar se CPF já existe (excluindo o paciente atual)
        val existingPacienteByCpf = pacienteDao.getPacienteByCpf(cpf)
        if (existingPacienteByCpf != null && existingPacienteByCpf.id != pacienteId) {
            binding.tilCpf.error = "CPF já cadastrado por outro paciente"
            return false
        }

        // Verificar se SUS já existe (excluindo o paciente atual)
        if (!sus.isNullOrEmpty()) {
            val existingPacienteBySus = pacienteDao.getPacienteBySus(sus)
            if (existingPacienteBySus != null && existingPacienteBySus.id != pacienteId) {
                binding.tilSus.error = "SUS já cadastrado por outro paciente"
                return false
            }
        }

        return true
    }

    // Restante do código permanece igual...
    private fun initializeEspecialidades() {
        lifecycleScope.launch {
            try {
                val count = especialidadeDao.getEspecialidadesCount()

                if (count == 0) {
                    Log.d("PacienteForm", "Inserindo especialidades iniciais...")

                    val especialidadesEntities = especialidadesDisponiveis.map { nome ->
                        EspecialidadeEntity(
                            nome = nome,
                            serverId = null,
                            syncStatus = SyncStatus.SYNCED,
                            lastModified = System.currentTimeMillis(),
                            isDeleted = false
                        )
                    }

                    especialidadeDao.insertEspecialidades(especialidadesEntities)
                    Log.d("PacienteForm", "Especialidades inseridas com sucesso: ${especialidadesEntities.size}")
                }

            } catch (e: Exception) {
                Log.e("PacienteForm", "Erro ao inicializar especialidades", e)
            }
        }
    }

    private fun setupEspecialidades() {
        chipGroupEspecialidades.removeAllViews()

        especialidadesDisponiveis.forEach { especialidade ->
            val isEnabled = sharedPreferences.getBoolean(
                "especialidade_${especialidade.lowercase().replace(" ", "_")}",
                true
            )

            if (isEnabled) {
                val chip = createEspecialidadeChip(especialidade)
                chipGroupEspecialidades.addView(chip)
            }
        }
    }

    private fun createEspecialidadeChip(especialidade: String): Chip {
        val chip = Chip(requireContext())
        chip.apply {
            text = especialidade
            isCheckable = true
            isChecked = false

            chipCornerRadius = 32f
            chipStrokeWidth = 4f
            chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.white)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.primary))

            setOnCheckedChangeListener { _, _ ->
                hideEspecialidadesError()
            }
        }
        return chip
    }

    private fun hideEspecialidadesError() {
        binding.tvEspecialidadesError.visibility = View.GONE
    }

    private fun showEspecialidadesError(message: String) {
        binding.tvEspecialidadesError.text = message
        binding.tvEspecialidadesError.visibility = View.VISIBLE
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
        val dataNascimento = binding.etDataNascimento.text?.toString()?.trim() ?: ""

        if (dataNascimento.isNotEmpty()) {
            try {
                val birthDate = dateFormat.parse(dataNascimento)
                if (birthDate != null) {
                    val today = Calendar.getInstance()
                    val birth = Calendar.getInstance()
                    birth.time = birthDate

                    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }

                    binding.etIdade.setText(age.toString())
                }
            } catch (e: Exception) {
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

        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as Chip
            chip.isChecked = false
        }

        hideEspecialidadesError()
    }

    private fun createPacienteWithValidation() {
        if (validateForm()) {
            val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
            val sus = binding.etSus.text?.toString()?.trim() ?: ""

            lifecycleScope.launch {
                try {
                    if (!validateUniqueFields(cpf, sus.ifEmpty { null })) {
                        return@launch
                    }

                    createPaciente()

                } catch (e: Exception) {
                    Log.e("PacienteForm", "Erro na validação", e)
                    Toast.makeText(
                        requireContext(),
                        "Erro na validação: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createPaciente() {
        if (validateForm()) {
            val nome = binding.etNome.text?.toString()?.trim() ?: ""
            val nomeMae = binding.etNomeMae.text?.toString()?.trim() ?: ""
            val dataNascimento = binding.etDataNascimento.text?.toString()?.trim() ?: ""
            val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
            val sus = binding.etSus.text?.toString()?.trim() ?: ""
            val telefone = binding.etTelefone.text?.toString()?.trim() ?: ""
            val endereco = binding.etEndereco.text?.toString()?.trim() ?: ""
            val especialidadesSelecionadas = getSelectedEspecialidades()

            val dataNascimentoTimestamp = convertDateToTimestamp(dataNascimento)
            val idade = calculateAgeFromDate(dataNascimento)

            val pacienteEntity = PacienteEntity(
                nome = nome,
                nomeDaMae = nomeMae.ifEmpty { "" },
                dataNascimento = dataNascimentoTimestamp,
                idade = idade,
                cpf = cpf,
                sus = sus.ifEmpty { "" },
                telefone = telefone.ifEmpty { "" },
                endereco = endereco.ifEmpty { "" },
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastModified = System.currentTimeMillis(),
                isDeleted = false
            )

            lifecycleScope.launch {
                try {
                    val pacienteId = pacienteDao.insertPaciente(pacienteEntity)
                    saveEspecialidadesRelationships(pacienteId, especialidadesSelecionadas)

                    Toast.makeText(
                        requireContext(),
                        "Paciente salvo com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    clearForm()

                } catch (e: Exception) {
                    Log.e("PacienteForm", "Erro ao salvar paciente", e)
                    Toast.makeText(
                        requireContext(),
                        "Erro ao salvar paciente: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun saveEspecialidadesRelationships(pacienteId: Long, especialidadesSelecionadas: List<String>) {
        try {
            especialidadesSelecionadas.forEach { nomeEspecialidade ->
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                if (especialidade != null) {
                    val pacienteEspecialidade = PacienteEspecialidadeEntity(
                        pacienteId = pacienteId,
                        especialidadeId = especialidade.id,
                        dataAtendimento = System.currentTimeMillis()
                    )

                    pacienteEspecialidadeDao.insertPacienteEspecialidade(pacienteEspecialidade)
                    Log.d("PacienteForm", "Relacionamento salvo: Paciente $pacienteId - Especialidade ${especialidade.id}")
                } else {
                    Log.w("PacienteForm", "Especialidade não encontrada: $nomeEspecialidade")
                }
            }
        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao salvar relacionamentos com especialidades", e)
            throw e
        }
    }

    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val date = dateFormat.parse(dateString)
            date?.time ?: 0L
        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao converter data: $dateString", e)
            0L
        }
    }

    private fun calculateAgeFromDate(dateString: String): Int {
        return try {
            val birthDate = dateFormat.parse(dateString)
            if (birthDate != null) {
                val today = Calendar.getInstance()
                val birth = Calendar.getInstance()
                birth.time = birthDate

                var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                age
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao calcular idade: $dateString", e)
            0
        }
    }

    private suspend fun validateUniqueFields(cpf: String, sus: String?): Boolean {
        val existingPacienteByCpf = pacienteDao.getPacienteByCpf(cpf)
        if (existingPacienteByCpf != null) {
            binding.tilCpf.error = "CPF já cadastrado"
            return false
        }

        if (!sus.isNullOrEmpty()) {
            val existingPacienteBySus = pacienteDao.getPacienteBySus(sus)
            if (existingPacienteBySus != null) {
                binding.tilSus.error = "SUS já cadastrado"
                return false
            }
        }

        return true
    }

    private fun getSelectedEspecialidades(): List<String> {
        val selectedEspecialidades = mutableListOf<String>()

        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedEspecialidades.add(chip.text.toString())
            }
        }

        return selectedEspecialidades
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Limpar erros anteriores
        binding.tilNome.error = null
        binding.tilCpf.error = null
        binding.tilDataNascimento.error = null

        if (binding.etNome.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilNome.error = "Nome é obrigatório"
            isValid = false
        }

        if (binding.etCpf.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilCpf.error = "CPF é obrigatório"
            isValid = false
        }

        if (binding.etDataNascimento.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilDataNascimento.error = "Data de nascimento é obrigatória"
            isValid = false
        }

        val selectedEspecialidades = getSelectedEspecialidades()
        if (selectedEspecialidades.isEmpty()) {
            showEspecialidadesError("Selecione pelo menos uma especialidade")
            isValid = false
        } else {
            hideEspecialidadesError()
        }

        return isValid
    }

    fun refreshEspecialidades() {
        setupEspecialidades()
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