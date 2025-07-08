package com.example.projeto_ibg3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.projeto_ibg3.data.converters.SyncStatusConverter
import com.example.projeto_ibg3.model.SyncStatus

// Entidade atualizada com campos de sincronização
@Entity(tableName = "pacientes")
@TypeConverters(SyncStatusConverter::class)
data class PacienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,  // ID no servidor remoto

    @ColumnInfo(name = "nome")
    val nome: String,

    @ColumnInfo(name = "data_nascimento")
    val dataNascimento: Long, // Armazenado como timestamp

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

    // Dados médicos
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

    // Campos para sincronização offline
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)

