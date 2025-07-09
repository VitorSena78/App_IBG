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
import kotlinx.coroutines.flow.forEach
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PacienteFormularioFragment : Fragment() {

    private var _binding: FragmentPacienteFormularioBinding? = null
    private val binding get() = _binding!!

    // Injeções de dependência DENTRO da classe
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

    // Componentes para especialidades
    private lateinit var chipGroupEspecialidades: ChipGroup
    private lateinit var sharedPreferences: SharedPreferences

    // Lista de especialidades disponíveis
    private val especialidadesDisponiveis = listOf(
        "Cardiologia",
        "Pediatria",
        "Clínico Geral",
        "Neurologia",
        "Ginecologia",
        "Dermatologia",
        "Ortopedia",
        "Endocrinologia",
        "Oftalmologia",
        "Psiquiatria"
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

        // Inicializar componentes
        setupComponents()
        setupUI()
        setupEspecialidades()

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

    private fun initializeEspecialidades() {
        lifecycleScope.launch {
            try {
                val count = especialidadeDao.getEspecialidadesCount()

                if (count == 0) {
                    Log.d("PacienteForm", "Inserindo especialidades iniciais...")

                    val especialidadesParaInserir = listOf(
                        "Cardiologia",
                        "Pediatria",
                        "Clínico Geral",
                        "Neurologia",
                        "Ginecologia",
                        "Dermatologia",
                        "Ortopedia",
                        "Endocrinologia",
                        "Oftalmologia",
                        "Psiquiatria"
                    )

                    val especialidadesEntities = especialidadesParaInserir.map { nome ->
                        EspecialidadeEntity(
                            nome = nome,
                            serverId = null, // Será preenchido quando sincronizar com a API
                            syncStatus = SyncStatus.SYNCED, // Como são dados locais iniciais
                            lastModified = System.currentTimeMillis(),
                            isDeleted = false
                        )
                    }

                    // Inserir todas as especialidades de uma vez
                    especialidadeDao.insertEspecialidades(especialidadesEntities)

                    Log.d("PacienteForm", "Especialidades inseridas com sucesso: ${especialidadesEntities.size}")

                    // Verificar se foram inseridas corretamente
                    val novoCount = especialidadeDao.getEspecialidadesCount()
                    Log.d("PacienteForm", "Total de especialidades no banco: $novoCount")

                } else {
                    Log.d("PacienteForm", "Especialidades já existem no banco: $count")
                }

            } catch (e: Exception) {
                Log.e("PacienteForm", "Erro ao inicializar especialidades", e)
            }
        }
    }

    // Método auxiliar para debug (opcional)
    private fun debugEspecialidades() {
        lifecycleScope.launch {
            try {
                val count = especialidadeDao.getEspecialidadesCount()
                Log.d("PacienteForm", "=== DEBUG ESPECIALIDADES ===")
                Log.d("PacienteForm", "Total no banco: $count")

                // Como getAllEspecialidades() retorna Flow, precisamos coletar
                especialidadeDao.getAllEspecialidades().collect { especialidades ->
                    especialidades.forEach { esp ->
                        Log.d("PacienteForm", "ID: ${esp.id}, Nome: ${esp.nome}, Deleted: ${esp.isDeleted}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PacienteForm", "Erro no debug de especialidades", e)
            }
        }
    }

    private fun setupEspecialidades() {
        // Limpar chips existentes
        chipGroupEspecialidades.removeAllViews()

        // Adicionar chips dinamicamente baseado nas especialidades habilitadas
        especialidadesDisponiveis.forEach { especialidade ->
            val isEnabled = sharedPreferences.getBoolean(
                "especialidade_${especialidade.lowercase().replace(" ", "_")}",
                true // Por padrão, todas estão habilitadas
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

            // Estilo do chip (você pode personalizar no styles.xml)
            chipCornerRadius = 32f
            chipStrokeWidth = 4f
            chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.white)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.primary))

            // Listener para mudanças de seleção
            setOnCheckedChangeListener { _, isChecked ->
                hideEspecialidadesError()
                Log.d("PacienteForm", "Especialidade $especialidade ${if (isChecked) "selecionada" else "desmarcada"}")
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
        // Limpar campos de texto
        binding.etNome.setText("")
        binding.etNomeMae.setText("")
        binding.etDataNascimento.setText("")
        binding.etIdade.setText("")
        binding.etCpf.setText("")
        binding.etSus.setText("")
        binding.etTelefone.setText("")
        binding.etEndereco.setText("")

        // Desmarcar todos os chips de especialidades
        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as Chip
            chip.isChecked = false
        }

        // Esconder erro das especialidades
        hideEspecialidadesError()
    }

    private fun loadPacienteData() {
        // Carregar dados do paciente e preencher campos
        // pacienteRepository.getPacienteById(pacienteId) { paciente ->
        //     binding.etNome.setText(paciente.name)
        //     binding.etIdade.setText(paciente.age.toString())
        //
        //     // Marcar especialidades selecionadas
        //     paciente.especialidades?.forEach { especialidade ->
        //         markEspecialidadeAsSelected(especialidade)
        //     }
        // }
    }

    private fun markEspecialidadeAsSelected(especialidade: String) {
        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as Chip
            if (chip.text.toString() == especialidade) {
                chip.isChecked = true
                break
            }
        }
    }

    private fun createPacienteWithValidation() {
        if (validateForm()) {
            val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
            val sus = binding.etSus.text?.toString()?.trim() ?: ""

            lifecycleScope.launch {
                try {
                    // Validar campos únicos
                    if (!validateUniqueFields(cpf, sus.ifEmpty { null })) {
                        return@launch
                    }

                    // Continuar com o salvamento...
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
        // Validar campos
        if (validateForm()) {
            // Obter dados do formulário - CORREÇÃO: garantir que não sejam null
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

            // Calcular idade
            val idade = calculateAgeFromDate(dataNascimento)

            // Criar entidade do paciente
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

            // Salvar no banco de dados usando coroutines
            lifecycleScope.launch {
                try {
                    // Inserir paciente e obter o ID gerado
                    val pacienteId = pacienteDao.insertPaciente(pacienteEntity)

                    // Salvar relacionamentos com especialidades
                    saveEspecialidadesRelationships(pacienteId, especialidadesSelecionadas)

                    // Mostrar mensagem de sucesso
                    Toast.makeText(
                        requireContext(),
                        "Paciente salvo com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Limpar formulário ou navegar para outra tela
                    clearForm()

                    // Navegar de volta ou para lista de pacientes
                    // findNavController().navigateUp()

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
            //Para cada especialidade selecionada, buscar o ID da especialidade e criar o relacionamento
            especialidadesSelecionadas.forEach { nomeEspecialidade ->
                //Buscar a especialidade pelo nome
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                if (especialidade != null) {
                    //Criar relacionamento
                    val pacienteEspecialidade = PacienteEspecialidadeEntity(
                        pacienteId = pacienteId,
                        especialidadeId = especialidade.id,
                        //converter para timestamp
                        dataAtendimento = System.currentTimeMillis()
                        // dataAtendimento = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )

                    // Inserir relacionamento
                    pacienteEspecialidadeDao.insertPacienteEspecialidade(pacienteEspecialidade)

                    val todas = pacienteEspecialidadeDao.getAll()
                    todas.forEach {
                        Log.d("PacienteForm", "Relacionamento salvo: ${it.pacienteId} -> ${it.especialidadeId}")
                    }

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

    // Função auxiliar para converter data string para timestamp
    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val date = dateFormat.parse(dateString)
            date?.time ?: 0L
        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao converter data: $dateString", e)
            0L
        }
    }

    // Função auxiliar para calcular idade a partir da data string
    private fun calculateAgeFromDate(dateString: String): Int {
        return try {
            val birthDate = dateFormat.parse(dateString)
            if (birthDate != null) {
                val today = Calendar.getInstance()
                val birth = Calendar.getInstance()
                birth.time = birthDate

                var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                // Verificar se ainda não fez aniversário este ano
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

    // Função para validar se CPF e SUS não estão duplicados (opcional)
    private suspend fun validateUniqueFields(cpf: String, sus: String?): Boolean {
        // Verificar se CPF já existe
        val existingPacienteByCpf = pacienteDao.getPacienteByCpf(cpf)
        if (existingPacienteByCpf != null) {
            binding.tilCpf.error = "CPF já cadastrado"
            return false
        }

        // Verificar se SUS já existe (se não for vazio)
        if (!sus.isNullOrEmpty()) {
            val existingPacienteBySus = pacienteDao.getPacienteBySus(sus)
            if (existingPacienteBySus != null) {
                binding.tilSus.error = "SUS já cadastrado"
                return false
            }
        }

        return true
    }

    private fun updatePaciente() {
        // Lógica para atualizar paciente existente
        if (validateForm()) {
            val especialidadesSelecionadas = getSelectedEspecialidades()
            // Implementar lógica de atualização com especialidades
            Log.d("PacienteForm", "Atualizando com especialidades: $especialidadesSelecionadas")
        }
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

        // Validar nome
        if (binding.etNome.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilNome.error = "Nome é obrigatório"
            isValid = false
        } else {
            binding.tilNome.error = null
        }

        // Validar CPF
        if (binding.etCpf.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilCpf.error = "CPF é obrigatório"
            isValid = false
        } else {
            binding.tilCpf.error = null
        }

        // Validar data de nascimento
        if (binding.etDataNascimento.text?.toString()?.trim().isNullOrEmpty()) {
            binding.tilDataNascimento.error = "Data de nascimento é obrigatória"
            isValid = false
        } else {
            binding.tilDataNascimento.error = null
        }

        // Validar especialidades (pelo menos uma deve ser selecionada)
        val selectedEspecialidades = getSelectedEspecialidades()
        if (selectedEspecialidades.isEmpty()) {
            showEspecialidadesError("Selecione pelo menos uma especialidade")
            isValid = false
        } else {
            hideEspecialidadesError()
        }

        return isValid
    }

    // Método público para atualizar especialidades (chamado quando configurações mudam)
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