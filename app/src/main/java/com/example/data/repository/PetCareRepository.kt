package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.HealthRecord
import com.example.data.model.HealthRecordCategory
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PetCareRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    val uid = auth.currentUser?.uid
    check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Firestore." }
    return uid
  }

  private fun petsCollection(userId: String) =
    db.collection("users").document(userId).collection("pets")

  private fun vaccinationsCollection(userId: String) =
    db.collection("users").document(userId).collection("vaccinations")

  private fun healthRecordsCollection(userId: String) =
    db.collection("users").document(userId).collection("healthRecords")

  fun observePets(userId: String): Flow<List<Pet>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch." }
    val path = "users/$userId/pets"
    emitAll(
      petsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Pet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.name.lowercase() }
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  fun observeVaccinations(userId: String): Flow<List<VaccinationRecord>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch." }
    val path = "users/$userId/vaccinations"
    emitAll(
      vaccinationsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              VaccinationRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.administeredDateEpochMs }
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  fun observeHealthRecords(userId: String): Flow<List<HealthRecord>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Signed-in user mismatch." }
    val path = "users/$userId/healthRecords"
    emitAll(
      healthRecordsCollection(userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              HealthRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.recordDateEpochMs }
        }
        .catch { error ->
          logStreamErrorIfActive(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  private fun logStreamErrorIfActive(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }

  suspend fun addPet(
    userId: String,
    name: String,
    species: PetSpecies,
    breed: String,
    ageYears: Long,
    ageMonths: Long,
    weightKg: Double?,
    microchipId: String?,
    notes: String?,
  ): Result<String> {
    var docPath = "users/$userId/pets"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      val trimmedName = name.trim()
      val trimmedBreed = breed.trim()
      val trimmedChip = microchipId?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedName.length in 1..60) { "Pet name must be 1 to 60 characters." }
      require(trimmedBreed.length in 1..60) { "Breed must be 1 to 60 characters." }
      require(ageYears in 0L..50L) { "Age years must be between 0 and 50." }
      require(ageMonths in 0L..11L) { "Age months must be between 0 and 11." }
      if (weightKg != null) {
        require(weightKg > 0.0 && weightKg <= 500.0) { "Weight must be between 0 and 500 kg." }
      }
      if (trimmedChip != null) {
        require(trimmedChip.length <= 50) { "Microchip ID must be at most 50 characters." }
      }
      if (trimmedNotes != null) {
        require(trimmedNotes.length <= 500) { "Notes must be at most 500 characters." }
      }

      val docRef = petsCollection(userId).document()
      docPath = docRef.path
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("name", trimmedName)
        put("species", species.name)
        put("breed", trimmedBreed)
        put("ageYears", ageYears)
        put("ageMonths", ageMonths)
        if (weightKg != null) put("weightKg", weightKg)
        if (trimmedChip != null) put("microchipId", trimmedChip)
        if (trimmedNotes != null) put("notes", trimmedNotes)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updatePet(
    userId: String,
    petId: String,
    name: String,
    species: PetSpecies,
    breed: String,
    ageYears: Long,
    ageMonths: Long,
    weightKg: Double?,
    microchipId: String?,
    notes: String?,
  ): Result<Unit> {
    var docPath = "users/$userId/pets/$petId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(petId.isNotBlank()) { "Pet ID must not be blank." }
      val trimmedName = name.trim()
      val trimmedBreed = breed.trim()
      val trimmedChip = microchipId?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedName.length in 1..60) { "Pet name must be 1 to 60 characters." }
      require(trimmedBreed.length in 1..60) { "Breed must be 1 to 60 characters." }
      require(ageYears in 0L..50L) { "Age years must be between 0 and 50." }
      require(ageMonths in 0L..11L) { "Age months must be between 0 and 11." }
      if (weightKg != null) {
        require(weightKg > 0.0 && weightKg <= 500.0) { "Weight must be between 0 and 500 kg." }
      }
      if (trimmedChip != null) {
        require(trimmedChip.length <= 50) { "Microchip ID must be at most 50 characters." }
      }
      if (trimmedNotes != null) {
        require(trimmedNotes.length <= 500) { "Notes must be at most 500 characters." }
      }

      val docRef = petsCollection(userId).document(petId)
      docPath = docRef.path
      val updates = mapOf<String, Any>(
        "name" to trimmedName,
        "species" to species.name,
        "breed" to trimmedBreed,
        "ageYears" to ageYears,
        "ageMonths" to ageMonths,
        "weightKg" to (weightKg ?: FieldValue.delete()),
        "microchipId" to (trimmedChip ?: FieldValue.delete()),
        "notes" to (trimmedNotes ?: FieldValue.delete()),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deletePet(userId: String, petId: String): Result<Unit> {
    val petPath = "users/$userId/pets/$petId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(petId.isNotBlank()) { "Pet ID must not be blank." }

      val linkedVaccines = vaccinationsCollection(userId)
        .whereEqualTo("petId", petId)
        .get()
        .await()
        .documents
        .map { it.reference }

      val linkedRecords = healthRecordsCollection(userId)
        .whereEqualTo("petId", petId)
        .get()
        .await()
        .documents
        .map { it.reference }

      val allRefs = linkedVaccines + linkedRecords + petsCollection(userId).document(petId)
      allRefs.chunked(450).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { ref -> batch.delete(ref) }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, petPath)
      Result.failure(e)
    }
  }

  suspend fun addVaccination(
    userId: String,
    petId: String,
    vaccineName: String,
    administeredDateEpochMs: Long,
    nextDueDateEpochMs: Long?,
    clinicOrVet: String?,
    notes: String?,
    status: VaccinationStatus,
  ): Result<Unit> {
    var docPath = "users/$userId/vaccinations"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(petId.isNotBlank()) { "Pet ID is required." }
      val trimmedName = vaccineName.trim()
      val trimmedClinic = clinicOrVet?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedName.length in 1..80) { "Vaccine name must be 1 to 80 characters." }
      require(administeredDateEpochMs > 0L) { "Administered date is required." }
      if (nextDueDateEpochMs != null) {
        require(nextDueDateEpochMs > 0L) { "Next due date must be valid." }
      }
      if (trimmedClinic != null) {
        require(trimmedClinic.length <= 80) { "Clinic name must be at most 80 characters." }
      }
      if (trimmedNotes != null) {
        require(trimmedNotes.length <= 500) { "Notes must be at most 500 characters." }
      }

      val docRef = vaccinationsCollection(userId).document()
      docPath = docRef.path
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("petId", petId)
        put("vaccineName", trimmedName)
        put("administeredDateEpochMs", administeredDateEpochMs)
        if (nextDueDateEpochMs != null) put("nextDueDateEpochMs", nextDueDateEpochMs)
        if (trimmedClinic != null) put("clinicOrVet", trimmedClinic)
        if (trimmedNotes != null) put("notes", trimmedNotes)
        put("status", status.name)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateVaccination(
    userId: String,
    vaccinationId: String,
    vaccineName: String,
    administeredDateEpochMs: Long,
    nextDueDateEpochMs: Long?,
    clinicOrVet: String?,
    notes: String?,
    status: VaccinationStatus,
  ): Result<Unit> {
    var docPath = "users/$userId/vaccinations/$vaccinationId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(vaccinationId.isNotBlank()) { "Vaccination ID must not be blank." }
      val trimmedName = vaccineName.trim()
      val trimmedClinic = clinicOrVet?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedName.length in 1..80) { "Vaccine name must be 1 to 80 characters." }
      require(administeredDateEpochMs > 0L) { "Administered date is required." }
      if (nextDueDateEpochMs != null) {
        require(nextDueDateEpochMs > 0L) { "Next due date must be valid." }
      }
      if (trimmedClinic != null) {
        require(trimmedClinic.length <= 80) { "Clinic name must be at most 80 characters." }
      }
      if (trimmedNotes != null) {
        require(trimmedNotes.length <= 500) { "Notes must be at most 500 characters." }
      }

      val docRef = vaccinationsCollection(userId).document(vaccinationId)
      docPath = docRef.path
      val updates = mapOf<String, Any>(
        "vaccineName" to trimmedName,
        "administeredDateEpochMs" to administeredDateEpochMs,
        "nextDueDateEpochMs" to (nextDueDateEpochMs ?: FieldValue.delete()),
        "clinicOrVet" to (trimmedClinic ?: FieldValue.delete()),
        "notes" to (trimmedNotes ?: FieldValue.delete()),
        "status" to status.name,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleVaccinationStatus(
    userId: String,
    vaccinationId: String,
    newStatus: VaccinationStatus,
  ): Result<Unit> {
    val docPath = "users/$userId/vaccinations/$vaccinationId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(vaccinationId.isNotBlank()) { "Vaccination ID must not be blank." }
      vaccinationsCollection(userId).document(vaccinationId).update(
        mapOf(
          "status" to newStatus.name,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteVaccination(userId: String, vaccinationId: String): Result<Unit> {
    val docPath = "users/$userId/vaccinations/$vaccinationId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(vaccinationId.isNotBlank()) { "Vaccination ID must not be blank." }
      vaccinationsCollection(userId).document(vaccinationId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addHealthRecord(
    userId: String,
    petId: String,
    title: String,
    category: HealthRecordCategory,
    recordDateEpochMs: Long,
    vetName: String?,
    details: String?,
  ): Result<Unit> {
    var docPath = "users/$userId/healthRecords"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(petId.isNotBlank()) { "Pet ID is required." }
      val trimmedTitle = title.trim()
      val trimmedVet = vetName?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedDetails = details?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedTitle.length in 1..80) { "Title must be 1 to 80 characters." }
      require(recordDateEpochMs > 0L) { "Record date is required." }
      if (trimmedVet != null) {
        require(trimmedVet.length <= 80) { "Vet name must be at most 80 characters." }
      }
      if (trimmedDetails != null) {
        require(trimmedDetails.length <= 1000) { "Details must be at most 1000 characters." }
      }

      val docRef = healthRecordsCollection(userId).document()
      docPath = docRef.path
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("petId", petId)
        put("title", trimmedTitle)
        put("category", category.name)
        put("recordDateEpochMs", recordDateEpochMs)
        if (trimmedVet != null) put("vetName", trimmedVet)
        if (trimmedDetails != null) put("details", trimmedDetails)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateHealthRecord(
    userId: String,
    recordId: String,
    title: String,
    category: HealthRecordCategory,
    recordDateEpochMs: Long,
    vetName: String?,
    details: String?,
  ): Result<Unit> {
    var docPath = "users/$userId/healthRecords/$recordId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(recordId.isNotBlank()) { "Record ID must not be blank." }
      val trimmedTitle = title.trim()
      val trimmedVet = vetName?.trim()?.takeIf { it.isNotEmpty() }
      val trimmedDetails = details?.trim()?.takeIf { it.isNotEmpty() }

      require(trimmedTitle.length in 1..80) { "Title must be 1 to 80 characters." }
      require(recordDateEpochMs > 0L) { "Record date is required." }
      if (trimmedVet != null) {
        require(trimmedVet.length <= 80) { "Vet name must be at most 80 characters." }
      }
      if (trimmedDetails != null) {
        require(trimmedDetails.length <= 1000) { "Details must be at most 1000 characters." }
      }

      val docRef = healthRecordsCollection(userId).document(recordId)
      docPath = docRef.path
      val updates = mapOf<String, Any>(
        "title" to trimmedTitle,
        "category" to category.name,
        "recordDateEpochMs" to recordDateEpochMs,
        "vetName" to (trimmedVet ?: FieldValue.delete()),
        "details" to (trimmedDetails ?: FieldValue.delete()),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteHealthRecord(userId: String, recordId: String): Result<Unit> {
    val docPath = "users/$userId/healthRecords/$recordId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch." }
      require(recordId.isNotBlank()) { "Record ID must not be blank." }
      healthRecordsCollection(userId).document(recordId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }
}
