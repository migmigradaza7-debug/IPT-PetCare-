package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class PetSpecies(val label: String) {
  DOG("Dog"),
  CAT("Cat"),
  BIRD("Bird"),
  RABBIT("Rabbit"),
  OTHER("Other");

  companion object {
    fun fromString(raw: String?): PetSpecies =
      entries.firstOrNull { it.name == raw } ?: OTHER
  }
}

enum class VaccinationStatus(val label: String) {
  SCHEDULED("Scheduled"),
  COMPLETED("Completed");

  companion object {
    fun fromString(raw: String?): VaccinationStatus =
      entries.firstOrNull { it.name == raw } ?: COMPLETED
  }
}

enum class HealthRecordCategory(val label: String) {
  CHECKUP("Checkup"),
  MEDICATION("Medication"),
  ALLERGY("Allergy"),
  WEIGHT("Weight"),
  NOTE("Note");

  companion object {
    fun fromString(raw: String?): HealthRecordCategory =
      entries.firstOrNull { it.name == raw } ?: NOTE
  }
}

enum class VaccinationUrgency(val label: String) {
  OVERDUE("Overdue"),
  DUE_SOON("Due Soon"),
  SCHEDULED("Scheduled"),
  UP_TO_DATE("Up to Date"),
}

@Keep
@IgnoreExtraProperties
data class Pet(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val species: String = PetSpecies.DOG.name,
  val breed: String = "",
  val ageYears: Long = 0L,
  val ageMonths: Long = 0L,
  val weightKg: Double? = null,
  val microchipId: String? = null,
  val notes: String? = null,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  val speciesEnum: PetSpecies
    get() = PetSpecies.fromString(species)

  fun formattedAge(): String {
    return when {
      ageYears > 0L && ageMonths > 0L -> "${ageYears}y ${ageMonths}m"
      ageYears > 0L -> if (ageYears == 1L) "1 year" else "$ageYears years"
      ageMonths > 0L -> if (ageMonths == 1L) "1 month" else "$ageMonths months"
      else -> "< 1 month"
    }
  }
}

@Keep
@IgnoreExtraProperties
data class VaccinationRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val petId: String = "",
  val vaccineName: String = "",
  val administeredDateEpochMs: Long = 0L,
  val nextDueDateEpochMs: Long? = null,
  val clinicOrVet: String? = null,
  val notes: String? = null,
  val status: String = VaccinationStatus.COMPLETED.name,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  val statusEnum: VaccinationStatus
    get() = VaccinationStatus.fromString(status)

  fun urgency(nowEpochMs: Long = System.currentTimeMillis()): VaccinationUrgency {
    val dueMs = nextDueDateEpochMs
    val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
    return when (statusEnum) {
      VaccinationStatus.SCHEDULED -> {
        val targetMs = dueMs ?: administeredDateEpochMs
        when {
          targetMs < nowEpochMs -> VaccinationUrgency.OVERDUE
          targetMs - nowEpochMs <= thirtyDaysMs -> VaccinationUrgency.DUE_SOON
          else -> VaccinationUrgency.SCHEDULED
        }
      }
      VaccinationStatus.COMPLETED -> {
        when {
          dueMs != null && dueMs < nowEpochMs -> VaccinationUrgency.OVERDUE
          dueMs != null && dueMs - nowEpochMs <= thirtyDaysMs -> VaccinationUrgency.DUE_SOON
          else -> VaccinationUrgency.UP_TO_DATE
        }
      }
    }
  }
}

@Keep
@IgnoreExtraProperties
data class HealthRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val petId: String = "",
  val title: String = "",
  val category: String = HealthRecordCategory.CHECKUP.name,
  val recordDateEpochMs: Long = 0L,
  val vetName: String? = null,
  val details: String? = null,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  val categoryEnum: HealthRecordCategory
    get() = HealthRecordCategory.fromString(category)
}
