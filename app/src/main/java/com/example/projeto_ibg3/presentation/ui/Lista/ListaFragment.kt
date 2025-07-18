package com.example.projeto_ibg3.presentation.ui.Lista

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.presentation.ui.Lista.adapter.PacienteAdapter
import com.example.projeto_ibg3.presentation.ui.Lista.adapter.PacienteAdapterCallback
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.presentation.common.viewmodel.PacienteViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ListaFragment : Fragment(), PacienteAdapterCallback {

    private lateinit var recyclerPacientes: RecyclerView
    private lateinit var btnAddPaciente: MaterialButton
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvTotalPacientes: TextView
    private lateinit var layoutEmptyState: LinearLayout

    private lateinit var pacienteAdapter: PacienteAdapter
    private val viewModel: PacienteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_paciente_lista, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        // Carregar dados iniciais
        viewModel.loadPacientes()
    }

    private fun setupViews(view: View) {
        recyclerPacientes = view.findViewById(R.id.recycler_pacientes)
        btnAddPaciente = view.findViewById(R.id.btn_add_paciente)
        etSearch = view.findViewById(R.id.et_search)
        tvTotalPacientes = view.findViewById(R.id.tv_total_pacientes)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)

        btnAddPaciente.setOnClickListener {
            navigateToAddPaciente()
        }
    }

    private fun setupRecyclerView() {
        pacienteAdapter = PacienteAdapter(this)

        recyclerPacientes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pacienteAdapter

            // Adicionar animações suaves
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener { text ->
            val query = text?.toString()?.trim() ?: ""
            viewModel.searchPacientes(query)
        }
    }

    private fun observeViewModel() {
        // Observar lista de pacientes
        viewModel.pacientes.onEach { pacientes ->
            pacienteAdapter.submitList(pacientes)
            updateEmptyState(pacientes.isEmpty())
            updateTotalCount(pacientes.size)
        }.launchIn(lifecycleScope)

        // Observar estados de loading e erro
        viewModel.isLoading.onEach { isLoading ->
            // Mostrar/ocultar indicador de loading se necessário
        }.launchIn(lifecycleScope)

        viewModel.error.onEach { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }.launchIn(lifecycleScope)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerPacientes.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            recyclerPacientes.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateTotalCount(count: Int) {
        val text = when (count) {
            0 -> "Nenhum paciente"
            1 -> "1 paciente"
            else -> "$count pacientes"
        }
        tvTotalPacientes.text = "Total: $text"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToAddPaciente() {
        findNavController().navigate(R.id.action_lista_to_addPaciente)
    }

    // Implementação das callbacks do adapter
    override fun onPacienteClick(paciente: Paciente) {
        // Usar o mesmo nome de constante do PacienteDetalheFragment
        val bundle = Bundle().apply {
            putString("paciente_id", paciente.localId.toString()) // Envia apenas o ID do paciente
        }
        findNavController().navigate(R.id.action_lista_to_pacienteDetalhe, bundle)
    }

    override fun onEditPaciente(paciente: Paciente) {
        val bundle = Bundle().apply {
            putString("pacienteLocalId", paciente.localId.toString()) // Mudança aqui também
        }
        findNavController().navigate(R.id.action_lista_to_editPaciente, bundle)
    }

    override fun onCallPaciente(paciente: Paciente) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${paciente.telefone}")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Não foi possível realizar a ligação", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewDetails(paciente: Paciente) {
        onPacienteClick(paciente)
    }

    companion object {
        fun newInstance() = ListaFragment()
    }
}