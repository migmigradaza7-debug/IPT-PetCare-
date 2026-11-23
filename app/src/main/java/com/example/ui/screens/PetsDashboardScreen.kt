package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HealthRecord
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationUrgency
import com.example.ui.components.PetAvatar
import com.example.ui.components.VaccinationUrgencyBadge

@Composable
fun PetsDashboardScreen(
  pets: List<Pet>,
  vaccinations: List<VaccinationRecord>,
  healthRecords: List<HealthRecord>,
  selectedPetId: String?,
  photoVersion: Int,
  userDisplayName: String?,
  onSelectPet: (Pet) -> Unit,
  onAddPetClick: () -> Unit,
  onOpenAccountClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedSpecies by rememberSaveable { mutableStateOf<PetSpecies?>(null) }

  val filteredPets = remember(pets, searchQuery, selectedSpecies) {
    pets.filter { pet ->
      val matchesSpecies = selectedSpecies == null || pet.speciesEnum == selectedSpecies
      val q = searchQuery.trim()
      val matchesQuery = q.isEmpty() ||
        pet.name.contains(q, ignoreCase = true) ||
        pet.breed.contains(q, ignoreCase = true)
      matchesSpecies && matchesQuery
    }
  }

  val vaccinationsByPet = remember(vaccinations) { vaccinations.groupBy { it.petId } }
  val recordsByPet = remember(healthRecords) { healthRecords.groupBy { it.petId } }

  val attentionCount = remember(vaccinations) {
    vaccinations.count {
      val u = it.urgency()
      u == VaccinationUrgency.OVERDUE || u == VaccinationUrgency.DUE_SOON
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "My Pets",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          val firstName = userDisplayName?.trim()?.substringBefore(" ")?.takeIf { it.isNotEmpty() }
          Text(
            text = if (firstName != null) "$firstName's care journal" else "Household care journal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onOpenAccountClick,
          modifier = Modifier
            .size(48.dp)
            .testTag("account_menu_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "Account and cloud sync",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
          )
        }
      }
    }

    if (pets.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SummaryMetricCard(
            label = "Pets",
            value = pets.size.toString(),
            icon = Icons.Filled.Pets,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = if (attentionCount == 1) "Shot due" else "Shots due",
            value = attentionCount.toString(),
            icon = Icons.Filled.Vaccines,
            containerColor = if (attentionCount > 0) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = if (attentionCount > 0) {
              MaterialTheme.colorScheme.onSecondaryContainer
            } else {
              MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = "Health logs",
            value = healthRecords.size.toString(),
            icon = Icons.Filled.MedicalServices,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by pet name or breed...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pet_search_input"),
        )
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedSpecies == null,
            onClick = { selectedSpecies = null },
            label = { Text("All (${pets.size})") },
          )
          PetSpecies.entries.forEach { species ->
            val count = pets.count { it.speciesEnum == species }
            if (count > 0) {
              FilterChip(
                selected = selectedSpecies == species,
                onClick = {
                  selectedSpecies = if (selectedSpecies == species) null else species
                },
                label = { Text("${species.label} ($count)") },
              )
            }
          }
        }
      }
    }

    if (pets.isEmpty()) {
      item {
        EmptyPetsCard(onAddPetClick = onAddPetClick)
      }
    } else if (filteredPets.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text("No matching pets", style = MaterialTheme.typography.titleLarge)
            Text(
              "Try clearing the search query or species filter.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(filteredPets, key = { it.id }) { pet ->
        val petVaccines = vaccinationsByPet[pet.id].orEmpty()
        val petRecords = recordsByPet[pet.id].orEmpty()
        PetProfileCard(
          pet = pet,
          vaccinations = petVaccines,
          healthRecordCount = petRecords.size,
          isSelected = pet.id == selectedPetId,
          photoVersion = photoVersion,
          onClick = { onSelectPet(pet) },
        )
      }
    }
  }
}

@Composable
private fun SummaryMetricCard(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  containerColor: androidx.compose.ui.graphics.Color,
  contentColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(18.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PetProfileCard(
  pet: Pet,
  vaccinations: List<VaccinationRecord>,
  healthRecordCount: Int,
  isSelected: Boolean,
  photoVersion: Int,
  onClick: () -> Unit,
) {
  val topUrgency = remember(vaccinations) {
    val urgencies = vaccinations.map { it.urgency() }
    when {
      VaccinationUrgency.OVERDUE in urgencies -> VaccinationUrgency.OVERDUE
      VaccinationUrgency.DUE_SOON in urgencies -> VaccinationUrgency.DUE_SOON
      VaccinationUrgency.SCHEDULED in urgencies -> VaccinationUrgency.SCHEDULED
      vaccinations.isNotEmpty() -> VaccinationUrgency.UP_TO_DATE
      else -> null
    }
  }

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
      },
    ),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .testTag("pet_card_${pet.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PetAvatar(
        petId = pet.id,
        petName = pet.name,
        species = pet.speciesEnum,
        photoVersion = photoVersion,
        size = 64.dp,
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = pet.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = pet.speciesEnum.label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }

        Text(
          text = buildString {
            append(pet.breed)
            append(" · ")
            append(pet.formattedAge())
            pet.weightKg?.let { append(" · ${it} kg") }
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(2.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (topUrgency != null) {
            VaccinationUrgencyBadge(urgency = topUrgency)
          } else {
            Surface(
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = "No vaccines yet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }

          Text(
            text = "${vaccinations.size} shots · $healthRecordCount notes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Icon(
        imageVector = Icons.Filled.ChevronRight,
        contentDescription = "Open ${pet.name}",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun EmptyPetsCard(onAddPetClick: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_pet_empty_state),
        contentDescription = "No pets added yet",
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(18.dp)),
        contentScale = ContentScale.Crop,
      )
      Text(
        text = "Start your pet journal",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "Add your dog, cat, or companion pet to track breed info, vaccinations, and veterinary records.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onAddPetClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("empty_add_pet_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add Your First Pet")
      }
    }
  }
}
