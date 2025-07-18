package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.projeto_ibg3.data.local.database.converters.SyncStatusConverter
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.util.UUID

// Entidade adaptada seguindo o padrão do exemplo
@Entity(tableName = "pacientes")
@TypeConverters(SyncStatusConverter::class)
data class PacienteEntity(
    // Chave primária local (UUID para evitar conflitos)
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String = UUID.randomUUID().toString(),

    // ID no servidor remoto (mudança: usar Long? como no seu DTO)
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    // Dados básicos do paciente
    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "data_nascimento")
    val dataNascimento: Long, // Mantendo como timestamp

    @ColumnInfo(name = "idade")
    val idade: Int? = null,

    @ColumnInfo(name = "nome_da_mae")
    val nomeDaMae: String,

    @ColumnInfo(name = "cpf")
    val cpf: String,

    @ColumnInfo(name = "sus")
    val sus: String,

    @ColumnInfo(name = "telefone")
    val telefone: String,

    @ColumnInfo(name = "endereco")
    val endereco: String,

    // Dados médicos específicos do seu app
    @ColumnInfo(name = "pa_x_mmhg")
    val pressaoArterial: String? = null,

    @ColumnInfo(name = "fc_bpm")
    val frequenciaCardiaca: Float? = null,

    @ColumnInfo(name = "fr_ibpm")
    val frequenciaRespiratoria: Float? = null,

    @ColumnInfo(name = "temperatura_c")
    val temperatura: Float? = null,

    @ColumnInfo(name = "hgt_mgld")
    val glicemia: Float? = null,

    @ColumnInfo(name = "spo2")
    val saturacaoOxigenio: Float? = null,

    @ColumnInfo(name = "peso")
    val peso: Float? = null,

    @ColumnInfo(name = "altura")
    val altura: Float? = null,

    @ColumnInfo(name = "imc")
    val imc: Float? = null,

    // Campos para sincronização
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,

    // NOVA: Adicionar deviceId para controle de origem
    @ColumnInfo(name = "device_id")
    val deviceId: String = "", // Será preenchido no Repository

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    // NOVA: Adicionar versão para controle de conflitos
    @ColumnInfo(name = "version")
    val version: Int = 1,

    // NOVA: Armazenar dados conflitantes como JSON
    @ColumnInfo(name = "conflict_data")
    val conflictData: String? = null,

    // NOVA: Contador de tentativas de sincronização
    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0,

    // NOVA: Última tentativa de sincronização
    @ColumnInfo(name = "last_sync_attempt")
    val lastSyncAttempt: Long = 0,

    // NOVA: Mensagem de erro da última tentativa
    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
)