package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.HealthRecord
import com.example.data.model.Pet
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus
import com.example.ui.components.PetAvatar
import com.example.ui.components.VaccinationUrgencyBadge
import com.example.ui.components.formatEpochDate
import com.example.ui.components.healthCategoryIcon

@Composable
fun PetDetailScreen(
  pet: Pet,
  vaccinations: List<VaccinationRecord>,
  healthRecords: List<HealthRecord>,
  photoVersion: Int,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEditPet: () -> Unit,
  onDeletePet: () -> Unit,
  onUpdatePhoto: (Uri) -> Unit,
  onAddVaccination: () -> Unit,
  onEditVaccination: (VaccinationRecord) -> Unit,
  onToggleVaccinationStatus: (VaccinationRecord) -> Unit,
  onDeleteVaccination: (VaccinationRecord) -> Unit,
  onAddHealthRecord: () -> Unit,
  onEditHealthRecord: (HealthRecord) -> Unit,
  onDeleteHealthRecord: (HealthRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (showBackButton) {
    BackHandler(onBack = onBack)
  }

  var selectedTab by rememberSaveable(pet.id) { mutableIntStateOf(0) }

  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) onUpdatePhoto(uri)
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (showBackButton) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to pets",
            )
          }
        } else {
          Spacer(Modifier.width(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = onEditPet,
            modifier = Modifier
              .size(48.dp)
              .testTag("edit_pet_button"),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit ${pet.name}")
          }
          IconButton(
            onClick = onDeletePet,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_pet_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Remove ${pet.name}",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }

    item {
      PetProfileHeaderCard(
        pet = pet,
        photoVersion = photoVersion,
        onPickPhoto = {
          photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
      )
    }

    item {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          icon = {
            Icon(Icons.Filled.Vaccines, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          modifier = Modifier.testTag("tab_vaccinations"),
        ) {
          Text("Vaccines (${vaccinations.size})")
        }
        SegmentedButton(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          icon = {
            Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          modifier = Modifier.testTag("tab_health_records"),
        ) {
          Text("Health (${healthRecords.size})")
        }
      }
    }

    if (selectedTab == 0) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Vaccination Records", style = MaterialTheme.typography.titleLarge)
          Button(
            onClick = onAddVaccination,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_vaccine_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Shot")
          }
        }
      }

      if (vaccinations.isEmpty()) {
        item {
          EmptySectionCard(
            title = "No vaccinations logged yet",
            subtitle = "Keep ${pet.name}'s rabies, booster, and annual shots tracked here.",
            actionLabel = "Log First Vaccine",
            onAction = onAddVaccination,
          )
        }
      } else {
        items(vaccinations, key = { it.id }) { record ->
          VaccinationCard(
            record = record,
            onToggleStatus = { onToggleVaccinationStatus(record) },
            onEdit = { onEditVaccination(record) },
            onDelete = { onDeleteVaccination(record) },
          )
        }
      }
    } else {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Health Records", style = MaterialTheme.typography.titleLarge)
          Button(
            onClick = onAddHealthRecord,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("add_health_record_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Record")
          }
        }
      }

      if (healthRecords.isEmpty()) {
        item {
          EmptySectionCard(
            title = "No health records yet",
            subtitle = "Track checkups, prescriptions, allergies, or weight notes for ${pet.name}.",
            actionLabel = "Add Health Note",
            onAction = onAddHealthRecord,
          )
        }
      } else {
        items(healthRecords, key = { it.id }) { record ->
          HealthRecordCard(
            record = record,
            onEdit = { onEditHealthRecord(record) },
            onDelete = { onDeleteHealthRecord(record) },
          )
        }
      }
    }
  }
}

@Composable
private fun PetProfileHeaderCard(
  pet: Pet,
  photoVersion: Int,
  onPickPhoto: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(modifier = Modifier.clickable(onClick = onPickPhoto)) {
          PetAvatar(
            petId = pet.id,
            petName = pet.name,
            species = pet.speciesEnum,
            photoVersion = photoVersion,
            size = 80.dp,
          )
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
              .size(26.dp)
              .align(Alignment.BottomEnd),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "Update pet photo",
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = pet.name,
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = "${pet.speciesEnum.label} · ${pet.breed}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Age: ${pet.formattedAge()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        pet.weightKg?.let { weight ->
          ProfileDetailChip(label = "Weight", value = "$weight kg", modifier = Modifier.weight(1f))
        }
        pet.microchipId?.let { chip ->
          ProfileDetailChip(label = "Microchip", value = chip, modifier = Modifier.weight(1f))
        }
      }

      pet.notes?.let { careNotes ->
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Care Notes",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = careNotes,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileDetailChip(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}

@Composable
private fun VaccinationCard(
  record: VaccinationRecord,
  onToggleStatus: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("vaccination_card_${record.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = record.vaccineName,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        VaccinationUrgencyBadge(urgency = record.urgency())
      }

      Text(
        text = buildString {
          append(if (record.statusEnum == VaccinationStatus.COMPLETED) "Given: " else "Scheduled: ")
          append(formatEpochDate(record.administeredDateEpochMs))
          record.nextDueDateEpochMs?.let {
            append(" · Next due: ")
            append(formatEpochDate(it))
          }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      record.clinicOrVet?.let { clinic ->
        Text(
          text = "Clinic: $clinic",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      record.notes?.let { notes ->
        Text(
          text = notes,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onToggleStatus) {
          Icon(
            imageVector = if (record.statusEnum == VaccinationStatus.COMPLETED) {
              Icons.Filled.CheckCircle
            } else {
              Icons.Filled.RadioButtonUnchecked
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            if (record.statusEnum == VaccinationStatus.COMPLETED) {
              "Completed"
            } else {
              "Mark Completed"
            }
          )
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit vaccination")
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              Icons.Filled.Delete,
              contentDescription = "Delete vaccination",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HealthRecordCard(
  record: HealthRecord,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("health_record_card_${record.id}"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = healthCategoryIcon(record.categoryEnum),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Column {
            Text(text = record.title, style = MaterialTheme.typography.titleMedium)
            Text(
              text = "${record.categoryEnum.label} · ${formatEpochDate(record.recordDateEpochMs)}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit health record")
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
            Icon(
              Icons.Filled.Delete,
              contentDescription = "Delete health record",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      record.vetName?.let { vet ->
        Text(
          text = "Vet / Clinic: $vet",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      record.details?.let { details ->
        Text(
          text = details,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun EmptySectionCard(
  title: String,
  subtitle: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(4.dp))
      OutlinedButton(onClick = onAction) {
        Text(actionLabel)
      }
    }
  }
}
