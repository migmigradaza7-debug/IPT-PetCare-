package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.PetPhotoStorage
import com.example.data.model.HealthRecord
import com.example.data.model.HealthRecordCategory
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetFormSheet(
  existingPet: Pet?,
  photoVersion: Int,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
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
  ) -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var name by remember(existingPet) { mutableStateOf(existingPet?.name ?: "") }
  var species by remember(existingPet) { mutableStateOf(existingPet?.speciesEnum ?: PetSpecies.DOG) }
  var breed by remember(existingPet) { mutableStateOf(existingPet?.breed ?: "") }
  var ageYearsText by remember(existingPet) { mutableStateOf(existingPet?.ageYears?.toString() ?: "1") }
  var ageMonthsText by remember(existingPet) { mutableStateOf(existingPet?.ageMonths?.toString() ?: "0") }
  var weightText by remember(existingPet) { mutableStateOf(existingPet?.weightKg?.toString() ?: "") }
  var microchipId by remember(existingPet) { mutableStateOf(existingPet?.microchipId ?: "") }
  var notes by remember(existingPet) { mutableStateOf(existingPet?.notes ?: "") }
  var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
  var removeExistingPhoto by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val existingPhotoFile = remember(existingPet?.id, photoVersion) {
    existingPet?.id?.let { PetPhotoStorage.getPhotoFile(context, it) }
  }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      pendingPhotoUri = uri
      removeExistingPhoto = false
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding()
        .imePadding(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (existingPet == null) "Add New Pet" else "Edit ${existingPet.name}",
        style = MaterialTheme.typography.headlineSmall,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            }
            .testTag("pick_pet_photo_button"),
          contentAlignment = Alignment.Center,
        ) {
          when {
            pendingPhotoUri != null -> {
              Image(
                painter = rememberAsyncImagePainter(pendingPhotoUri),
                contentDescription = "Selected pet photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
            !removeExistingPhoto && existingPhotoFile != null -> {
              Image(
                painter = rememberAsyncImagePainter(existingPhotoFile),
                contentDescription = "Current pet photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
            else -> {
              Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = "Choose pet photo",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          ) {
            Text(
              if (pendingPhotoUri != null || (!removeExistingPhoto && existingPhotoFile != null)) {
                "Change Photo"
              } else {
                "Add Photo"
              }
            )
          }
          if (pendingPhotoUri != null || (!removeExistingPhoto && existingPhotoFile != null)) {
            TextButton(
              onClick = {
                pendingPhotoUri = null
                removeExistingPhoto = true
              },
            ) {
              Text("Remove photo", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = {
          if (it.length <= 60) name = it
          validationError = null
        },
        label = { Text("Pet Name *") },
        placeholder = { Text("e.g., Milo") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pet_name_input"),
      )

      Text("Species *", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PetSpecies.entries.forEach { option ->
          FilterChip(
            selected = species == option,
            onClick = { species = option },
            label = { Text(option.label) },
            leadingIcon = if (species == option) {
              { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
              null
            },
          )
        }
      }

      OutlinedTextField(
        value = breed,
        onValueChange = {
          if (it.length <= 60) breed = it
          validationError = null
        },
        label = { Text("Breed *") },
        placeholder = { Text("e.g., Golden Retriever") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pet_breed_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = ageYearsText,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 2) {
              ageYearsText = input
              validationError = null
            }
          },
          label = { Text("Age (Years) *") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .weight(1f)
            .testTag("pet_age_years_input"),
        )
        OutlinedTextField(
          value = ageMonthsText,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 2) {
              ageMonthsText = input
              validationError = null
            }
          },
          label = { Text("Months (0-11)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier
            .weight(1f)
            .testTag("pet_age_months_input"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = weightText,
          onValueChange = { input ->
            if (input.length <= 6) {
              weightText = input
              validationError = null
            }
          },
          label = { Text("Weight (kg)") },
          placeholder = { Text("Optional") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = microchipId,
          onValueChange = {
            if (it.length <= 50) microchipId = it
          },
          label = { Text("Microchip ID") },
          placeholder = { Text("Optional") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = notes,
        onValueChange = {
          if (it.length <= 500) notes = it
        },
        label = { Text("Care Notes") },
        placeholder = { Text("Diet preferences, personality, temperament...") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      if (validationError != null) {
        Text(
          text = validationError ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isSaving,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            val trimmedName = name.trim()
            val trimmedBreed = breed.trim()
            val years = ageYearsText.toLongOrNull()
            val months = if (ageMonthsText.isBlank()) 0L else ageMonthsText.toLongOrNull()
            val weight = if (weightText.isBlank()) null else weightText.toDoubleOrNull()

            when {
              trimmedName.isEmpty() -> validationError = "Please enter your pet's name."
              trimmedBreed.isEmpty() -> validationError = "Please enter a breed."
              years == null || years !in 0L..50L -> validationError = "Years must be between 0 and 50."
              months == null || months !in 0L..11L -> validationError = "Months must be between 0 and 11."
              weightText.isNotBlank() && (weight == null || weight <= 0.0 || weight > 500.0) ->
                validationError = "Weight must be between 0.1 and 500 kg."
              else -> onSave(
                trimmedName,
                species,
                trimmedBreed,
                years,
                months,
                weight,
                microchipId.trim().takeIf { it.isNotEmpty() },
                notes.trim().takeIf { it.isNotEmpty() },
                pendingPhotoUri,
                removeExistingPhoto,
              )
            }
          },
          enabled = !isSaving,
          modifier = Modifier
            .weight(1f)
            .testTag("save_pet_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(if (existingPet == null) "Save Pet" else "Update Pet")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationFormSheet(
  existingRecord: VaccinationRecord?,
  petName: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    vaccineName: String,
    administeredDateEpochMs: Long,
    nextDueDateEpochMs: Long?,
    clinicOrVet: String?,
    notes: String?,
    status: VaccinationStatus,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var vaccineName by remember(existingRecord) { mutableStateOf(existingRecord?.vaccineName ?: "") }
  var status by remember(existingRecord) {
    mutableStateOf(existingRecord?.statusEnum ?: VaccinationStatus.COMPLETED)
  }
  var administeredMs by remember(existingRecord) {
    mutableStateOf(existingRecord?.administeredDateEpochMs ?: System.currentTimeMillis())
  }
  var nextDueMs by remember(existingRecord) { mutableStateOf(existingRecord?.nextDueDateEpochMs) }
  var clinicOrVet by remember(existingRecord) { mutableStateOf(existingRecord?.clinicOrVet ?: "") }
  var notes by remember(existingRecord) { mutableStateOf(existingRecord?.notes ?: "") }
  var validationError by remember { mutableStateOf<String?>(null) }

  var pickAdministeredDate by remember { mutableStateOf(false) }
  var pickNextDueDate by remember { mutableStateOf(false) }

  if (pickAdministeredDate) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = administeredMs)
    DatePickerDialog(
      onDismissRequest = { pickAdministeredDate = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { administeredMs = it }
            pickAdministeredDate = false
          },
        ) { Text("OK") }
      },
      dismissButton = {
        TextButton(onClick = { pickAdministeredDate = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (pickNextDueDate) {
    val defaultDue = nextDueMs ?: (administeredMs + 365L * 24L * 60L * 60L * 1000L)
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = defaultDue)
    DatePickerDialog(
      onDismissRequest = { pickNextDueDate = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { nextDueMs = it }
            pickNextDueDate = false
          },
        ) { Text("OK") }
      },
      dismissButton = {
        TextButton(onClick = { pickNextDueDate = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding()
        .imePadding(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (existingRecord == null) "Log Vaccination for $petName" else "Edit Vaccination",
        style = MaterialTheme.typography.headlineSmall,
      )

      OutlinedTextField(
        value = vaccineName,
        onValueChange = {
          if (it.length <= 80) vaccineName = it
          validationError = null
        },
        label = { Text("Vaccine Name *") },
        placeholder = { Text("e.g., Rabies, DHPP, FVRCP") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("vaccine_name_input"),
      )

      Text("Status *", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VaccinationStatus.entries.forEach { option ->
          FilterChip(
            selected = status == option,
            onClick = { status = option },
            label = { Text(option.label) },
            leadingIcon = if (status == option) {
              { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
              null
            },
          )
        }
      }

      DateSelectorField(
        label = if (status == VaccinationStatus.COMPLETED) "Date Given *" else "Scheduled Date *",
        value = formatEpochDate(administeredMs),
        onClick = { pickAdministeredDate = true },
        onClear = null,
      )

      DateSelectorField(
        label = "Next Booster Due (Optional)",
        value = nextDueMs?.let { formatEpochDate(it) } ?: "No booster date set",
        onClick = { pickNextDueDate = true },
        onClear = if (nextDueMs != null) { { nextDueMs = null } } else null,
      )

      OutlinedTextField(
        value = clinicOrVet,
        onValueChange = { if (it.length <= 80) clinicOrVet = it },
        label = { Text("Clinic / Veterinarian") },
        placeholder = { Text("Optional") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = notes,
        onValueChange = { if (it.length <= 500) notes = it },
        label = { Text("Notes / Batch #") },
        placeholder = { Text("Lot number, reactions, or reminders...") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      if (validationError != null) {
        Text(
          text = validationError ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isSaving,
          modifier = Modifier.weight(1f),
        ) { Text("Cancel") }
        Button(
          onClick = {
            val trimmed = vaccineName.trim()
            if (trimmed.isEmpty()) {
              validationError = "Please enter the vaccine name."
            } else {
              onSave(
                trimmed,
                administeredMs,
                nextDueMs,
                clinicOrVet.trim().takeIf { it.isNotEmpty() },
                notes.trim().takeIf { it.isNotEmpty() },
                status,
              )
            }
          },
          enabled = !isSaving,
          modifier = Modifier
            .weight(1f)
            .testTag("save_vaccine_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(if (existingRecord == null) "Save Record" else "Update")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordFormSheet(
  existingRecord: HealthRecord?,
  petName: String,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    title: String,
    category: HealthRecordCategory,
    recordDateEpochMs: Long,
    vetName: String?,
    details: String?,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember(existingRecord) { mutableStateOf(existingRecord?.title ?: "") }
  var category by remember(existingRecord) {
    mutableStateOf(existingRecord?.categoryEnum ?: HealthRecordCategory.CHECKUP)
  }
  var recordDateMs by remember(existingRecord) {
    mutableStateOf(existingRecord?.recordDateEpochMs ?: System.currentTimeMillis())
  }
  var vetName by remember(existingRecord) { mutableStateOf(existingRecord?.vetName ?: "") }
  var details by remember(existingRecord) { mutableStateOf(existingRecord?.details ?: "") }
  var validationError by remember { mutableStateOf<String?>(null) }
  var pickDate by remember { mutableStateOf(false) }

  if (pickDate) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = recordDateMs)
    DatePickerDialog(
      onDismissRequest = { pickDate = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { recordDateMs = it }
            pickDate = false
          },
        ) { Text("OK") }
      },
      dismissButton = {
        TextButton(onClick = { pickDate = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 24.dp)
        .navigationBarsPadding()
        .imePadding(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (existingRecord == null) "Log Health Record for $petName" else "Edit Health Record",
        style = MaterialTheme.typography.headlineSmall,
      )

      OutlinedTextField(
        value = title,
        onValueChange = {
          if (it.length <= 80) title = it
          validationError = null
        },
        label = { Text("Record Title *") },
        placeholder = { Text("e.g., Annual Checkup, Heartgard Dose") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("health_title_input"),
      )

      Text("Category *", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        HealthRecordCategory.entries.forEach { option ->
          FilterChip(
            selected = category == option,
            onClick = { category = option },
            label = { Text(option.label) },
            leadingIcon = {
              Icon(
                imageVector = healthCategoryIcon(option),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
          )
        }
      }

      DateSelectorField(
        label = "Record Date *",
        value = formatEpochDate(recordDateMs),
        onClick = { pickDate = true },
        onClear = null,
      )

      OutlinedTextField(
        value = vetName,
        onValueChange = { if (it.length <= 80) vetName = it },
        label = { Text("Veterinarian / Clinic") },
        placeholder = { Text("Optional") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = details,
        onValueChange = { if (it.length <= 1000) details = it },
        label = { Text("Health Notes & Dosage") },
        placeholder = { Text("Symptoms, diagnosis, medication schedule...") },
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
      )

      if (validationError != null) {
        Text(
          text = validationError ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isSaving,
          modifier = Modifier.weight(1f),
        ) { Text("Cancel") }
        Button(
          onClick = {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
              validationError = "Please enter a title for this health record."
            } else {
              onSave(
                trimmed,
                category,
                recordDateMs,
                vetName.trim().takeIf { it.isNotEmpty() },
                details.trim().takeIf { it.isNotEmpty() },
              )
            }
          },
          enabled = !isSaving,
          modifier = Modifier
            .weight(1f)
            .testTag("save_health_record_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Text(if (existingRecord == null) "Save Record" else "Update")
          }
        }
      }
    }
  }
}

@Composable
private fun DateSelectorField(
  label: String,
  value: String,
  onClick: () -> Unit,
  onClear: (() -> Unit)?,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CalendarToday,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Column {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      if (onClear != null) {
        IconButton(onClick = onClear) {
          Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = "Clear date",
          )
        }
      }
    }
  }
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  confirmLabel: String = "Remove",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
  displayName: String?,
  contactInfo: String?,
  petCount: Int,
  vaccineCount: Int,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 28.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(52.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(26.dp))
          }
        }
        Column {
          Text(
            text = displayName?.takeIf { it.isNotBlank() } ?: "PetCare Parent",
            style = MaterialTheme.typography.titleLarge,
          )
          if (!contactInfo.isNullOrBlank()) {
            Text(
              text = contactInfo,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Cloud Sync Active · $petCount pets, $vaccineCount vaccination records",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sign out")
      }
    }
  }
}
