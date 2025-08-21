package com.example.projeto_ibg3.presentation.ui.formulario

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
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.databinding.FragmentPacienteFormularioBinding
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.fragment.app.viewModels
import kotlinx.coroutines.delay

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

    private val viewModel: PacienteFormularioViewModel by viewModels()

    private var pacienteId: String = ""
    private var isEditMode = false
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentPaciente: PacienteEntity? = null

    // Componentes para especialidades
    private lateinit var chipGroupEspecialidades: ChipGroup
    private lateinit var sharedPreferences: SharedPreferences

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
        pacienteId = arguments?.getString("pacienteLocalId", "") ?: ""
        isEditMode = pacienteId.isNotEmpty()

        Log.d("PacienteForm", "onCreate: pacienteLocalId=$pacienteId, isEditMode=$isEditMode")

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
                createPaciente() // Corrigido: chamando a função que existe
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("PacienteForm", "Carregando dados do paciente ID: $pacienteId")

                // Buscar paciente pelo ID (agora String)
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
                    binding.etIdade.setText(paciente.idade?.toString() ?: "")

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
                // Buscar especialidades do paciente usando localId
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

            // Verificar se o chip corresponde à especialidade (removendo a parte das fichas)
            val chipText = chip.text.toString()
            val especialidadeNome = if (chipText.contains("(")) {
                chipText.substring(0, chipText.indexOf("(")).trim()
            } else {
                chipText
            }

            if (especialidadeNome == especialidade) {
                // Só marcar se o chip estiver habilitado (especialidade disponível)
                if (chip.isEnabled) {
                    chip.isChecked = true
                    Log.d("PacienteForm", "Especialidade marcada: $especialidade")
                } else {
                    Log.w("PacienteForm", "Tentativa de marcar especialidade esgotada: $especialidade")
                    // Opcional: Mostrar toast informando que a especialidade não está mais disponível
                    Toast.makeText(
                        requireContext(),
                        "A especialidade '$especialidade' não está mais disponível",
                        Toast.LENGTH_LONG
                    ).show()
                }
                break
            }
        }
    }

    private fun updatePaciente() {
        Log.d("PacienteForm", "Iniciando atualização do paciente ID: $pacienteId")

        if (validateForm()) {
            lifecycleScope.launch {
                try {
                    // Validar campos únicos (excluindo o paciente atual)
                    val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
                    val sus = binding.etSus.text?.toString()?.trim() ?: ""

                    if (!validateUniqueFieldsForUpdate(cpf, sus.ifEmpty { null })) {
                        return@launch
                    }

                    // Obter dados do formulário
                    val nome = binding.etNome.text?.toString()?.trim() ?: ""
                    val nomeMae = binding.etNomeMae.text?.toString()?.trim() ?: ""
                    val dataNascimento = binding.etDataNascimento.text?.toString()?.trim() ?: ""
                    val telefone = binding.etTelefone.text?.toString()?.trim() ?: ""
                    val endereco = binding.etEndereco.text?.toString()?.trim() ?: ""
                    val especialidadesSelecionadas = getSelectedEspecialidades()

                    // Converter data de nascimento para timestamp
                    val dataNascimentoTimestamp = convertDateToTimestamp(dataNascimento)
                    val idade = calculateAgeFromDate(dataNascimento)

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
                            updatedAt = System.currentTimeMillis()
                        )

                        // Atualizar no banco LOCAL PRIMEIRO ***
                        pacienteDao.updatePaciente(pacienteAtualizado)
                        Log.d("PacienteForm", "✅ Paciente atualizado no banco local")

                        // Atualizar relacionamentos LOCAL ***
                        updateEspecialidadesRelationships(pacienteId, especialidadesSelecionadas)
                        Log.d("PacienteForm", "✅ Relacionamentos atualizados")

                        // SINCRONIZAÇÃO COMPLETA E SEQUENCIAL
                        try {
                            Log.d("PacienteForm", "🔄 Iniciando sincronização COMPLETA do paciente atualizado...")

                            Toast.makeText(
                                requireContext(),
                                "Paciente atualizado! Sincronizando...",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Usar o novo método de sincronização completa
                            viewModel.syncPacienteAtualizadoCompleto(pacienteId)

                            // Aguardar um tempo para a sincronização
                            delay(5000) // 5 segundos para completar todo o processo

                            Toast.makeText(
                                requireContext(),
                                "Sincronização completa finalizada!",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (syncError: Exception) {
                            Log.w("PacienteForm", "⚠️ Erro na sincronização", syncError)
                            Toast.makeText(
                                requireContext(),
                                "Dados salvos localmente! Serão sincronizados automaticamente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Voltar para a tela anterior
                        findNavController().navigateUp()
                    }

                } catch (e: Exception) {
                    Log.e("PacienteForm", "💥 Erro ao atualizar paciente", e)
                    Toast.makeText(
                        requireContext(),
                        "Erro ao atualizar paciente: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun updateEspecialidadesRelationships(pacienteId: String, especialidadesSelecionadas: List<String>) {
        try {
            Log.d("PacienteForm", "=== INICIANDO ATUALIZAÇÃO DE RELACIONAMENTOS COM CONTROLE DE FICHAS ===")
            Log.d("PacienteForm", "Paciente ID: $pacienteId")
            Log.d("PacienteForm", "Especialidades selecionadas: $especialidadesSelecionadas")

            // 1. Buscar relacionamentos atuais do paciente
            val relacionamentosAtuais = pacienteEspecialidadeDao.getByPacienteId(pacienteId)
            Log.d("PacienteForm", "Relacionamentos atuais: ${relacionamentosAtuais.size}")

            // 2. Buscar especialidades atuais por nome
            val especialidadesAtuais = relacionamentosAtuais.mapNotNull { relacionamento ->
                especialidadeDao.getEspecialidadeById(relacionamento.especialidadeLocalId)?.nome
            }
            Log.d("PacienteForm", "Especialidades atuais: $especialidadesAtuais")

            // 3. Determinar o que foi removido e o que foi adicionado
            val especialidadesParaRemover = especialidadesAtuais - especialidadesSelecionadas.toSet()
            val especialidadesParaAdicionar = especialidadesSelecionadas - especialidadesAtuais.toSet()

            Log.d("PacienteForm", "Para remover: $especialidadesParaRemover")
            Log.d("PacienteForm", "Para adicionar: $especialidadesParaAdicionar")

            // 4. REMOVER relacionamentos (usando controle de fichas)
            especialidadesParaRemover.forEach { nomeEspecialidade ->
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)
                if (especialidade != null) {
                    // USAR MÉTODO COM CONTROLE DE FICHAS
                    pacienteEspecialidadeDao.deleteWithFichasControl(
                        pacienteLocalId = pacienteId,
                        especialidadeLocalId = especialidade.localId
                    )

                    // Log das fichas após remoção
                    val fichasAtuais = especialidadeDao.getFichasCount(especialidade.localId)
                    Log.d("PacienteForm", "✅ Relacionamento removido com incremento de fichas: ${especialidade.nome} agora tem $fichasAtuais fichas")
                }
            }

            // 5. ADICIONAR novos relacionamentos (com validação de fichas)
            especialidadesParaAdicionar.forEach { nomeEspecialidade ->
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                if (especialidade != null) {
                    // VERIFICAR SE TEM FICHAS DISPONÍVEIS
                    val fichasDisponiveis = especialidadeDao.getFichasCount(especialidade.localId)

                    if (fichasDisponiveis <= 0) {
                        Log.w("PacienteForm", "⚠️ Tentativa de adicionar especialidade esgotada: ${especialidade.nome}")

                        // Mostrar aviso na UI
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "A especialidade '${especialidade.nome}' não está mais disponível",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@forEach
                    }

                    // Verificar se já existe um relacionamento (mesmo que deletado)
                    val relacionamentoExistente = pacienteEspecialidadeDao.getById(pacienteId, especialidade.localId)

                    if (relacionamentoExistente != null && relacionamentoExistente.isDeleted) {
                        // Se existe mas está deletado, restaurar COM CONTROLE DE FICHAS
                        pacienteEspecialidadeDao.restoreWithFichasControl(
                            pacienteLocalId = pacienteId,
                            especialidadeLocalId = especialidade.localId,
                            timestamp = System.currentTimeMillis()
                        )

                        val fichasRestantes = especialidadeDao.getFichasCount(especialidade.localId)
                        Log.d("PacienteForm", "✅ Relacionamento RESTAURADO com controle de fichas: ${especialidade.nome} agora tem $fichasRestantes fichas")

                    } else if (relacionamentoExistente == null) {
                        // Se não existe, criar novo COM CONTROLE DE FICHAS
                        val novoRelacionamento = PacienteEspecialidadeEntity(
                            pacienteLocalId = pacienteId,
                            especialidadeLocalId = especialidade.localId,
                            dataAtendimento = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING_UPLOAD,
                            isDeleted = false,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )

                        pacienteEspecialidadeDao.insertWithFichasControl(novoRelacionamento)

                        val fichasRestantes = especialidadeDao.getFichasCount(especialidade.localId)
                        Log.d("PacienteForm", "✅ Novo relacionamento CRIADO com controle de fichas: ${especialidade.nome} agora tem $fichasRestantes fichas")

                    } else {
                        Log.d("PacienteForm", "⚪ Relacionamento já existe e está ativo: ${especialidade.nome}")
                    }
                } else {
                    Log.w("PacienteForm", "⚠️ Especialidade não encontrada: $nomeEspecialidade")
                }
            }

            // 6. NOVO: Recarregar especialidades para atualizar a UI
            viewModel.refreshEspecialidades()

            // 7. Verificar resultado final
            val relacionamentosFinais = pacienteEspecialidadeDao.getByPacienteId(pacienteId)
            Log.d("PacienteForm", "=== RESULTADO FINAL ===")
            Log.d("PacienteForm", "Total de relacionamentos ativos: ${relacionamentosFinais.size}")

            relacionamentosFinais.forEach { rel ->
                val esp = especialidadeDao.getEspecialidadeById(rel.especialidadeLocalId)
                val fichas = esp?.let { especialidadeDao.getFichasCount(it.localId) } ?: 0
                Log.d("PacienteForm", "  - ${esp?.nome} (${fichas} fichas restantes, status: ${rel.syncStatus})")
            }

            Log.d("PacienteForm", "=== ATUALIZAÇÃO DE RELACIONAMENTOS CONCLUÍDA ===")

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
        if (existingPacienteByCpf != null && existingPacienteByCpf.localId != pacienteId) {
            binding.tilCpf.error = "CPF já cadastrado por outro paciente"
            return false
        }

        // Verificar se SUS já existe (excluindo o paciente atual)
        if (!sus.isNullOrEmpty()) {
            val existingPacienteBySus = pacienteDao.getPacienteBySus(sus)
            if (existingPacienteBySus != null && existingPacienteBySus.localId != pacienteId) {
                binding.tilSus.error = "SUS já cadastrado por outro paciente"
                return false
            }
        }

        return true
    }

    private fun setupEspecialidades() {
        Log.d("PacienteForm", "Setup inicial das especialidades")

        // Observar mudanças nas especialidades
        observeEspecialidades()

        // Observar estado de carregamento
        observeLoadingState()

        // Observar erros
        observeErrors()
    }

    private fun observeEspecialidades() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.especialidades.collect { especialidades ->
                Log.d("PacienteForm", "Especialidades recebidas: ${especialidades.size}")
                updateEspecialidadesUI(especialidades)
            }
        }
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoadingEspecialidades.collect { isLoading ->
                Log.d("PacienteForm", "Estado de carregamento: $isLoading")
                // indicador de carregamento
            }
        }
    }

    private fun observeErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Log.e("PacienteForm", "Erro: $it")
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun updateEspecialidadesUI(especialidades: List<EspecialidadeEntity>) {
        chipGroupEspecialidades.removeAllViews()

        val filteredEspecialidades = viewModel.getFilteredEspecialidades(sharedPreferences)

        Log.d("PacienteForm", "Criando chips para ${filteredEspecialidades.size} especialidades")

        // Separar especialidades disponíveis e esgotadas
        val especialidadesDisponiveis = filteredEspecialidades.filter { it.isAvailable() }
        val especialidadesEsgotadas = filteredEspecialidades.filter { it.isEsgotada() && !it.isDeleted }

        Log.d("PacienteForm", "Disponíveis: ${especialidadesDisponiveis.size}, Esgotadas: ${especialidadesEsgotadas.size}")

        // Primeiro adicionar as disponíveis
        especialidadesDisponiveis.forEach { especialidade ->
            val chip = createEspecialidadeChip(especialidade.nome, especialidade.fichas, true)
            chipGroupEspecialidades.addView(chip)
            Log.d("PacienteForm", "Chip criado (disponível): ${especialidade.nome} (${especialidade.fichas} fichas)")
        }

        // Depois adicionar as esgotadas (desabilitadas)
        especialidadesEsgotadas.forEach { especialidade ->
            val chip = createEspecialidadeChip(especialidade.nome, especialidade.fichas, false)
            chipGroupEspecialidades.addView(chip)
            Log.d("PacienteForm", "Chip criado (esgotado): ${especialidade.nome} (${especialidade.fichas} fichas)")
        }

        // Se estiver em modo de edição, recarregar as especialidades selecionadas
        if (isEditMode && currentPaciente != null) {
            loadPacienteEspecialidades()
        }

        // Mostrar aviso se não há especialidades disponíveis
        if (especialidadesDisponiveis.isEmpty()) {
            showEspecialidadesWarning("Todas as especialidades estão esgotadas no momento")
        } else {
            hideEspecialidadesWarning()
        }
    }

    // Novos métodos auxiliares para avisos
    private fun showEspecialidadesWarning(message: String) {
        // Se você tiver um TextView para avisos na UI
        binding.tvEspecialidadesWarning?.let {
            it.text = message
            it.visibility = View.VISIBLE
            it.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
        }
    }

    private fun hideEspecialidadesWarning() {
        binding.tvEspecialidadesWarning?.visibility = View.GONE
    }

    private fun createEspecialidadeChip(especialidade: String, fichas: Int, isAvailable: Boolean): Chip {
        val chip = Chip(requireContext())
        chip.apply {
            // Texto do chip com informação das fichas
            text = if (isAvailable) {
                "$especialidade ($fichas)"
            } else {
                "$especialidade (ESGOTADA)"
            }

            isCheckable = true
            isChecked = false

            // Configuração visual baseada na disponibilidade
            if (isAvailable) {
                // Especialidade disponível - comportamento normal
                isEnabled = true
                chipCornerRadius = 32f
                chipStrokeWidth = 4f
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.white)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.primary))
                alpha = 1.0f

                setOnCheckedChangeListener { _, _ ->
                    hideEspecialidadesError()
                }
            } else {
                // Especialidade esgotada - desabilitada
                isEnabled = false
                isCheckable = false
                chipCornerRadius = 32f
                chipStrokeWidth = 2f
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.gray_400)
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.gray_100)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.gray_600))
                alpha = 0.6f

                // Adicionar um listener para mostrar mensagem quando tentar clicar
                setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Esta especialidade está esgotada no momento",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        // Obter a data atual ou a data já preenchida no campo
        val currentDateText = binding.etDataNascimento.text?.toString()?.trim()
        val initialSelection = if (currentDateText.isNullOrEmpty()) {
            // Para data inicial, usar hoje em UTC
            System.currentTimeMillis()
        } else {
            try {
                // Converter a data do campo para timestamp
                val parsedDate = dateFormat.parse(currentDateText)
                parsedDate?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecione a data de nascimento")
            .setSelection(initialSelection)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Criar Calendar em UTC e converter para data local
            val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = selection

            // Obter ano, mês e dia da data selecionada em UTC
            val year = utcCalendar.get(Calendar.YEAR)
            val month = utcCalendar.get(Calendar.MONTH)
            val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

            // Criar Calendar local com a mesma data
            val localCalendar = Calendar.getInstance()
            localCalendar.set(year, month, day, 0, 0, 0)
            localCalendar.set(Calendar.MILLISECOND, 0)

            val selectedDate = localCalendar.time
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

                    // Garantir que ambos estão no mesmo fuso horário e hora
                    birth.set(Calendar.HOUR_OF_DAY, 0)
                    birth.set(Calendar.MINUTE, 0)
                    birth.set(Calendar.SECOND, 0)
                    birth.set(Calendar.MILLISECOND, 0)

                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)

                    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                    // Verificar se o aniversário já passou este ano
                    if (today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
                        (today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                                today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
                        age--
                    }

                    binding.etIdade.setText(age.toString())
                }
            } catch (_: Exception) {
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

    // Função unificada para criação de paciente
    private fun createPaciente() {
        if (validateForm()) {
            lifecycleScope.launch {
                try {
                    // Validar campos únicos
                    val cpf = binding.etCpf.text?.toString()?.trim() ?: ""
                    val sus = binding.etSus.text?.toString()?.trim() ?: ""

                    if (!validateUniqueFields(cpf, sus.ifEmpty { null })) {
                        return@launch
                    }

                    // Obter dados do formulário
                    val nome = binding.etNome.text?.toString()?.trim() ?: ""
                    val nomeMae = binding.etNomeMae.text?.toString()?.trim() ?: ""
                    val dataNascimento = binding.etDataNascimento.text?.toString()?.trim() ?: ""
                    val telefone = binding.etTelefone.text?.toString()?.trim() ?: ""
                    val endereco = binding.etEndereco.text?.toString()?.trim() ?: ""
                    val especialidadesSelecionadas = getSelectedEspecialidades()

                    val dataNascimentoTimestamp = convertDateToTimestamp(dataNascimento)
                    val idade = calculateAgeFromDate(dataNascimento)

                    // Obter deviceId do SharedPreferences ou usar um valor padrão
                    val deviceId = sharedPreferences.getString("device_id", android.os.Build.MODEL) ?: android.os.Build.MODEL

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
                        deviceId = deviceId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    // 1. Inserir paciente no banco LOCAL PRIMEIRO
                    pacienteDao.insertPaciente(pacienteEntity)
                    Log.d("PacienteForm", "✅ Novo paciente inserido no banco local")

                    // 2. Buscar o paciente recém-criado para obter o localId
                    val pacienteCriado = pacienteDao.getPacienteByCpf(cpf)

                    if (pacienteCriado != null) {
                        // 3. Salvar relacionamentos com especialidades
                        saveEspecialidadesRelationships(pacienteCriado.localId, especialidadesSelecionadas)
                        Log.d("PacienteForm", "✅ Relacionamentos salvos")

                        // 4. SINCRONIZAÇÃO COMPLETA E SEQUENCIAL
                        try {
                            Log.d("PacienteForm", "🚀 Iniciando sincronização COMPLETA do novo paciente...")

                            // Mostrar feedback imediato
                            Toast.makeText(
                                requireContext(),
                                "Paciente salvo! Sincronizando dados...",
                                Toast.LENGTH_SHORT
                            ).show()

                            // 4.1 PRIMEIRO: Sincronizar o paciente
                            Log.d("PacienteForm", "📋 Sincronizando dados básicos do paciente...")
                            viewModel.syncNovoPaciente()

                            // Aguardar um pouco para o paciente ser sincronizado
                            delay(3000) // 3 segundos

                            // 4.2 SEGUNDO: Sincronizar os relacionamentos especificamente
                            Log.d("PacienteForm", "🔗 Sincronizando relacionamentos do paciente...")
                            viewModel.syncPacienteRelationships(pacienteCriado.localId)

                            // Aguardar um pouco para completar
                            delay(2000) // 2 segundos

                            // 4.3 TERCEIRO: Atualizar as especialidades na UI para refletir fichas consumidas
                            Log.d("PacienteForm", "🔄 Atualizando especialidades na UI...")
                            viewModel.refreshEspecialidades()

                            Toast.makeText(
                                requireContext(),
                                "Paciente criado e sincronizado com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (syncError: Exception) {
                            Log.w("PacienteForm", "⚠️ Erro na sincronização", syncError)
                            Toast.makeText(
                                requireContext(),
                                "Paciente salvo localmente! Será sincronizado automaticamente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.e("PacienteForm", "❌ Erro: Paciente não encontrado após criação")
                        Toast.makeText(
                            requireContext(),
                            "Erro interno: Paciente não encontrado após criação",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    // 5. Limpar formulário e navegar
                    clearForm()

                    // Opcional: Voltar para tela anterior após sucesso
                    // findNavController().navigateUp()

                } catch (e: Exception) {
                    Log.e("PacienteForm", "💥 Erro ao salvar paciente", e)
                    Toast.makeText(
                        requireContext(),
                        "Erro ao salvar paciente: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Corrigido: função agora recebe String para pacienteLocalId
    private suspend fun saveEspecialidadesRelationships(pacienteId: String, especialidadesSelecionadas: List<String>) {
        try {
            Log.d("PacienteForm", "Salvando relacionamentos para paciente: $pacienteId")
            Log.d("PacienteForm", "Especialidades selecionadas: $especialidadesSelecionadas")

            especialidadesSelecionadas.forEach { nomeEspecialidade ->
                // Buscar especialidade pelo nome no banco
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                if (especialidade != null) {
                    // NOVO: Verificar se a especialidade ainda tem fichas disponíveis
                    val fichasDisponiveis = especialidadeDao.getFichasCount(especialidade.localId)

                    if (fichasDisponiveis <= 0) {
                        Log.w("PacienteForm", "⚠️ Especialidade ${especialidade.nome} está esgotada (${fichasDisponiveis} fichas)")

                        // Mostrar aviso na UI
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "A especialidade '${especialidade.nome}' ficou esgotada",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@forEach
                    }

                    Log.d("PacienteForm", "Especialidade ${especialidade.nome} tem $fichasDisponiveis fichas disponíveis")

                    val pacienteEspecialidade = PacienteEspecialidadeEntity(
                        pacienteLocalId = pacienteId,
                        especialidadeLocalId = especialidade.localId,
                        dataAtendimento = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING_UPLOAD
                    )

                    // USAR O MÉTODO COM CONTROLE DE FICHAS
                    pacienteEspecialidadeDao.insertWithFichasControl(pacienteEspecialidade)

                    Log.d("PacienteForm", "✅ Relacionamento salvo com controle de fichas: Paciente $pacienteId - Especialidade ${especialidade.localId}")

                    // Log das fichas após a operação
                    val fichasRestantes = especialidadeDao.getFichasCount(especialidade.localId)
                    Log.d("PacienteForm", "Fichas restantes para ${especialidade.nome}: $fichasRestantes")

                } else {
                    Log.w("PacienteForm", "Especialidade não encontrada: $nomeEspecialidade")

                    // Tentar recarregar especialidades se alguma não foi encontrada
                    Log.d("PacienteForm", "Tentando recarregar especialidades...")
                    viewModel.refreshEspecialidades()

                    // Fazer uma segunda tentativa após o refresh
                    delay(1000) // Aguardar um pouco para o refresh
                    val especialidadeRetry = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)

                    if (especialidadeRetry != null) {
                        // Verificar fichas novamente
                        val fichasDisponiveis = especialidadeDao.getFichasCount(especialidadeRetry.localId)

                        if (fichasDisponiveis > 0) {
                            val pacienteEspecialidade = PacienteEspecialidadeEntity(
                                pacienteLocalId = pacienteId,
                                especialidadeLocalId = especialidadeRetry.localId,
                                dataAtendimento = System.currentTimeMillis(),
                                syncStatus = SyncStatus.PENDING_UPLOAD
                            )

                            pacienteEspecialidadeDao.insertWithFichasControl(pacienteEspecialidade)
                            Log.d("PacienteForm", "✅ Relacionamento salvo na segunda tentativa com controle de fichas: Paciente $pacienteId - Especialidade ${especialidadeRetry.localId} (${especialidadeRetry.nome})")
                        } else {
                            Log.w("PacienteForm", "Especialidade ${especialidadeRetry.nome} esgotada na segunda tentativa")
                        }
                    } else {
                        Log.e("PacienteForm", "Especialidade ainda não encontrada após refresh: $nomeEspecialidade")
                    }
                }
            }

            // NOVO: Recarregar especialidades na UI para refletir mudanças nas fichas
            viewModel.refreshEspecialidades()

        } catch (e: Exception) {
            Log.e("PacienteForm", "Erro ao salvar relacionamentos com especialidades", e)
            throw e
        }
    }

    private fun convertDateToTimestamp(dateString: String): Long {
        return try {
            val date = dateFormat.parse(dateString)
            if (date != null) {
                // Garantir que estamos pegando o timestamp no fuso horário local
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            } else {
                0L
            }
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

                // Garantir que ambos estão no mesmo fuso horário
                birth.set(Calendar.HOUR_OF_DAY, 0)
                birth.set(Calendar.MINUTE, 0)
                birth.set(Calendar.SECOND, 0)
                birth.set(Calendar.MILLISECOND, 0)

                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)

                var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                // Verificar se o aniversário já passou este ano
                if (today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
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
        // Limpar erros anteriores
        binding.tilCpf.error = null
        binding.tilSus.error = null

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
            if (chip.isChecked && chip.isEnabled) { // Só considerar se estiver marcado E habilitado
                // Extrair apenas o nome da especialidade (sem a parte das fichas)
                val chipText = chip.text.toString()
                val especialidadeNome = if (chipText.contains("(")) {
                    chipText.substring(0, chipText.indexOf("(")).trim()
                } else {
                    chipText
                }
                selectedEspecialidades.add(especialidadeNome)
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
            // Verificar se há especialidades disponíveis
            val hasAvailableEspecialidades = viewModel.hasEspecialidadesDisponiveis(sharedPreferences)

            if (hasAvailableEspecialidades) {
                showEspecialidadesError("Selecione pelo menos uma especialidade")
            } else {
                showEspecialidadesError("Não há especialidades disponíveis no momento")
            }
            isValid = false
        } else {
            hideEspecialidadesError()
        }

        return isValid
    }


    override fun onDestroyView() {
        // Cancelar qualquer operação de validação pendente

        // Limpar listeners dos chips
        for (i in 0 until chipGroupEspecialidades.childCount) {
            val chip = chipGroupEspecialidades.getChildAt(i) as? Chip
            chip?.setOnCheckedChangeListener(null)
        }

        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(pacienteId: String = ""): PacienteFormularioFragment {
            val fragment = PacienteFormularioFragment()
            val bundle = Bundle().apply {
                putString("pacienteLocalId", pacienteId) // Passar String diretamente
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}