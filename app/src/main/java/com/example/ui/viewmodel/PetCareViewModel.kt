package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PetPhotoStorage
import com.example.data.model.HealthRecord
import com.example.data.model.HealthRecordCategory
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus
import com.example.data.repository.PetCareRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class PetCareDashboardData(
  val pets: List<Pet> = emptyList(),
  val vaccinations: List<VaccinationRecord> = emptyList(),
  val healthRecords: List<HealthRecord> = emptyList(),
)

class PetCareViewModel(
  private val repository: PetCareRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _retryTrigger = MutableStateFlow(0)
  private val _photoVersion = MutableStateFlow(0)
  val photoVersion: StateFlow<Int> = _photoVersion.asStateFlow()

  private val _isMutating = MutableStateFlow(false)
  val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

  private val _feedbackMessage = MutableSharedFlow<String>()
  val feedbackMessage = _feedbackMessage.asSharedFlow()

  val uiState: StateFlow<UiState<PetCareDashboardData>> =
    combine(
      repository.observePets(currentUserId),
      repository.observeVaccinations(currentUserId),
      repository.observeHealthRecords(currentUserId),
      _retryTrigger,
    ) { pets, vaccinations, records, _ ->
      PetCareDashboardData(
        pets = pets,
        vaccinations = vaccinations,
        healthRecords = records,
      )
    }
      .mapToUiState()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private fun kotlinx.coroutines.flow.Flow<PetCareDashboardData>.mapToUiState():
    kotlinx.coroutines.flow.Flow<UiState<PetCareDashboardData>> =
    kotlinx.coroutines.flow.flow {
      try {
        collect { data -> emit(UiState.Success(data)) }
      } catch (error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing PetCare collections", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync pet records"))
      }
    }

  fun retrySync() {
    _retryTrigger.value += 1
  }

  fun savePet(
    appContext: Context,
    existingPetId: String?,
    name: String,
    species: PetSpecies,
    breed: String,
    ageYears: Long,
    ageMonths: Long,
    weightKg: Double?,
    microchipId: String?,
    notes: String?,
    pendingPhotoUri: Uri?,
    removeExistingPhoto: Boolean,
    onSuccess: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      try {
        if (existingPetId == null) {
          val result = repository.addPet(
            userId = currentUserId,
            name = name,
            species = species,
            breed = breed,
            ageYears = ageYears,
            ageMonths = ageMonths,
            weightKg = weightKg,
            microchipId = microchipId,
            notes = notes,
          )
          result.fold(
            onSuccess = { newPetId ->
              if (pendingPhotoUri != null) {
                PetPhotoStorage.savePhotoFromUri(appContext, newPetId, pendingPhotoUri)
                _photoVersion.value += 1
              }
              _feedbackMessage.emit("${name.trim()} added to PetCare")
              onSuccess(newPetId)
            },
            onFailure = { error ->
              _feedbackMessage.emit(error.localizedMessage ?: "Could not save pet")
            },
          )
        } else {
          val result = repository.updatePet(
            userId = currentUserId,
            petId = existingPetId,
            name = name,
            species = species,
            breed = breed,
            ageYears = ageYears,
            ageMonths = ageMonths,
            weightKg = weightKg,
            microchipId = microchipId,
            notes = notes,
          )
          result.fold(
            onSuccess = {
              if (removeExistingPhoto) {
                PetPhotoStorage.deletePhoto(appContext, existingPetId)
                _photoVersion.value += 1
              } else if (pendingPhotoUri != null) {
                PetPhotoStorage.savePhotoFromUri(appContext, existingPetId, pendingPhotoUri)
                _photoVersion.value += 1
              }
              _feedbackMessage.emit("${name.trim()} updated")
              onSuccess(existingPetId)
            },
            onFailure = { error ->
              _feedbackMessage.emit(error.localizedMessage ?: "Could not update pet")
            },
          )
        }
      } finally {
        _isMutating.value = false
      }
    }
  }

  fun updatePetPhoto(appContext: Context, petId: String, uri: Uri) {
    viewModelScope.launch {
      val saved = PetPhotoStorage.savePhotoFromUri(appContext, petId, uri)
      if (saved) {
        _photoVersion.value += 1
        _feedbackMessage.emit("Pet photo updated")
      } else {
        _feedbackMessage.emit("Unable to load selected photo")
      }
    }
  }

  fun deletePet(appContext: Context, pet: Pet, onDeleted: () -> Unit) {
    viewModelScope.launch {
      _isMutating.value = true
      try {
        val result = repository.deletePet(currentUserId, pet.id)
        result.fold(
          onSuccess = {
            PetPhotoStorage.deletePhoto(appContext, pet.id)
            _photoVersion.value += 1
            _feedbackMessage.emit("${pet.name} removed")
            onDeleted()
          },
          onFailure = { error ->
            _feedbackMessage.emit(error.localizedMessage ?: "Could not remove pet")
          },
        )
      } finally {
        _isMutating.value = false
      }
    }
  }

  fun saveVaccination(
    existingVaccinationId: String?,
    petId: String,
    vaccineName: String,
    administeredDateEpochMs: Long,
    nextDueDateEpochMs: Long?,
    clinicOrVet: String?,
    notes: String?,
    status: VaccinationStatus,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      try {
        val result = if (existingVaccinationId == null) {
          repository.addVaccination(
            userId = currentUserId,
            petId = petId,
            vaccineName = vaccineName,
            administeredDateEpochMs = administeredDateEpochMs,
            nextDueDateEpochMs = nextDueDateEpochMs,
            clinicOrVet = clinicOrVet,
            notes = notes,
            status = status,
          )
        } else {
          repository.updateVaccination(
            userId = currentUserId,
            vaccinationId = existingVaccinationId,
            vaccineName = vaccineName,
            administeredDateEpochMs = administeredDateEpochMs,
            nextDueDateEpochMs = nextDueDateEpochMs,
            clinicOrVet = clinicOrVet,
            notes = notes,
            status = status,
          )
        }
        result.fold(
          onSuccess = {
            _feedbackMessage.emit(
              if (existingVaccinationId == null) "Vaccination recorded" else "Vaccination updated"
            )
            onSuccess()
          },
          onFailure = { error ->
            _feedbackMessage.emit(error.localizedMessage ?: "Could not save vaccination")
          },
        )
      } finally {
        _isMutating.value = false
      }
    }
  }

  fun toggleVaccinationStatus(record: VaccinationRecord) {
    val nextStatus = if (record.statusEnum == VaccinationStatus.COMPLETED) {
      VaccinationStatus.SCHEDULED
    } else {
      VaccinationStatus.COMPLETED
    }
    viewModelScope.launch {
      repository.toggleVaccinationStatus(currentUserId, record.id, nextStatus).fold(
        onSuccess = {
          _feedbackMessage.emit("${record.vaccineName} marked ${nextStatus.label.lowercase()}")
        },
        onFailure = { error ->
          _feedbackMessage.emit(error.localizedMessage ?: "Could not update status")
        },
      )
    }
  }

  fun deleteVaccination(record: VaccinationRecord) {
    viewModelScope.launch {
      repository.deleteVaccination(currentUserId, record.id).fold(
        onSuccess = { _feedbackMessage.emit("Vaccination removed") },
        onFailure = { error ->
          _feedbackMessage.emit(error.localizedMessage ?: "Could not delete vaccination")
        },
      )
    }
  }

  fun saveHealthRecord(
    existingRecordId: String?,
    petId: String,
    title: String,
    category: HealthRecordCategory,
    recordDateEpochMs: Long,
    vetName: String?,
    details: String?,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      try {
        val result = if (existingRecordId == null) {
          repository.addHealthRecord(
            userId = currentUserId,
            petId = petId,
            title = title,
            category = category,
            recordDateEpochMs = recordDateEpochMs,
            vetName = vetName,
            details = details,
          )
        } else {
          repository.updateHealthRecord(
            userId = currentUserId,
            recordId = existingRecordId,
            title = title,
            category = category,
            recordDateEpochMs = recordDateEpochMs,
            vetName = vetName,
            details = details,
          )
        }
        result.fold(
          onSuccess = {
            _feedbackMessage.emit(
              if (existingRecordId == null) "Health record added" else "Health record updated"
            )
            onSuccess()
          },
          onFailure = { error ->
            _feedbackMessage.emit(error.localizedMessage ?: "Could not save health record")
          },
        )
      } finally {
        _isMutating.value = false
      }
    }
  }

  fun deleteHealthRecord(record: HealthRecord) {
    viewModelScope.launch {
      repository.deleteHealthRecord(currentUserId, record.id).fold(
        onSuccess = { _feedbackMessage.emit("Health record removed") },
        onFailure = { error ->
          _feedbackMessage.emit(error.localizedMessage ?: "Could not delete health record")
        },
      )
    }
  }

  private companion object {
    const val TAG = "PetCareVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
