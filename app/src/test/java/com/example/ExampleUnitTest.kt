package com.example

import com.example.auth.isValidE164PhoneNumber
import com.example.auth.normalizePhoneNumber
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus
import com.example.data.model.VaccinationUrgency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun petFormattedAge_handlesYearsAndMonths() {
    assertEquals("3y 4m", Pet(ageYears = 3, ageMonths = 4).formattedAge())
    assertEquals("1 year", Pet(ageYears = 1, ageMonths = 0).formattedAge())
    assertEquals("5 months", Pet(ageYears = 0, ageMonths = 5).formattedAge())
    assertEquals("< 1 month", Pet(ageYears = 0, ageMonths = 0).formattedAge())
  }

  @Test
  fun petSpecies_defaultsSafelyOnUnknown() {
    assertEquals(PetSpecies.OTHER, PetSpecies.fromString("UNKNOWN_SPECIES"))
    assertEquals(PetSpecies.DOG, PetSpecies.fromString("DOG"))
  }

  @Test
  fun vaccinationUrgency_computesOverdueDueSoonAndUpToDate() {
    val now = 1_700_000_000_000L
    val dayMs = 24L * 60L * 60L * 1000L

    val overdue = VaccinationRecord(
      status = VaccinationStatus.COMPLETED.name,
      administeredDateEpochMs = now - 400 * dayMs,
      nextDueDateEpochMs = now - 2 * dayMs,
    )
    assertEquals(VaccinationUrgency.OVERDUE, overdue.urgency(now))

    val dueSoon = VaccinationRecord(
      status = VaccinationStatus.SCHEDULED.name,
      administeredDateEpochMs = now + 10 * dayMs,
    )
    assertEquals(VaccinationUrgency.DUE_SOON, dueSoon.urgency(now))

    val upToDate = VaccinationRecord(
      status = VaccinationStatus.COMPLETED.name,
      administeredDateEpochMs = now - 5 * dayMs,
      nextDueDateEpochMs = now + 180 * dayMs,
    )
    assertEquals(VaccinationUrgency.UP_TO_DATE, upToDate.urgency(now))
  }

  @Test
  fun phoneNormalization_andValidation_workCorrectly() {
    assertEquals("+15551234567", normalizePhoneNumber(" +1 (555) 123-4567 "))
    assertTrue(isValidE164PhoneNumber("+15551234567"))
    assertTrue(isValidE164PhoneNumber("+639171234567"))
    assertFalse(isValidE164PhoneNumber("5551234567"))
    assertFalse(isValidE164PhoneNumber("+0123456789"))
    assertFalse(isValidE164PhoneNumber("+12345"))
  }
}
