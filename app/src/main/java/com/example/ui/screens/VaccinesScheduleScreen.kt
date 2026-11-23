package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Pet
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationRecord
import com.example.data.model.VaccinationStatus
import com.example.data.model.VaccinationUrgency
import com.example.ui.components.PetAvatar
import com.example.ui.components.VaccinationUrgencyBadge
import com.example.ui.components.formatEpochDate

private enum class VaccineFilter(val label: String) {
  ALL("All"),
  ACTION_NEEDED("Action Needed"),
  SCHEDULED("Upcoming"),
  UP_TO_DATE("Up to Date"),
}

@Composable
fun VaccinesScheduleScreen(
  pets: List<Pet>,
  vaccinations: List<VaccinationRecord>,
  photoVersion: Int,
  onSelectPet: (Pet) -> Unit,
  onToggleStatus: (VaccinationRecord) -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by rememberSaveable { mutableStateOf(VaccineFilter.ALL) }
  val petsById = remember(pets) { pets.associateBy { it.id } }

  val sortedAndFiltered = remember(vaccinations, filter) {
    val urgencyOrder = mapOf(
      VaccinationUrgency.OVERDUE to 0,
      VaccinationUrgency.DUE_SOON to 1,
      VaccinationUrgency.SCHEDULED to 2,
      VaccinationUrgency.UP_TO_DATE to 3,
    )
    vaccinations
      .filter { record ->
        val u = record.urgency()
        when (filter) {
          VaccineFilter.ALL -> true
          VaccineFilter.ACTION_NEEDED -> u == VaccinationUrgency.OVERDUE || u == VaccinationUrgency.DUE_SOON
          VaccineFilter.SCHEDULED -> u == VaccinationUrgency.SCHEDULED
          VaccineFilter.UP_TO_DATE -> u == VaccinationUrgency.UP_TO_DATE
        }
      }
      .sortedWith(
        compareBy<VaccinationRecord> { urgencyOrder[it.urgency()] ?: 4 }
          .thenBy { it.nextDueDateEpochMs ?: Long.MAX_VALUE }
      )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column {
        Text(
          text = "Vaccines",
          style = MaterialTheme.typography.displaySmall,
        )
        Text(
          text = "Household immunization timeline across all pets",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        VaccineFilter.entries.forEach { option ->
          FilterChip(
            selected = filter == option,
            onClick = { filter = option },
            label = { Text(option.label) },
          )
        }
      }
    }

    if (sortedAndFiltered.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(56.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Vaccines,
                contentDescription = null,
                modifier = Modifier.padding(14.dp),
              )
            }
            Text(
              text = if (vaccinations.isEmpty()) "No vaccinations logged" else "Nothing in this filter",
              style = MaterialTheme.typography.titleLarge,
              textAlign = TextAlign.Center,
            )
            Text(
              text = if (vaccinations.isEmpty()) {
                "Open any pet profile to log completed shots or schedule upcoming boosters."
              } else {
                "Try switching filters to view other immunization records."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(sortedAndFiltered, key = { it.id }) { record ->
        val pet = petsById[record.petId]
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = pet != null) { pet?.let(onSelectPet) }
            .testTag("schedule_vaccine_card_${record.id}"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              PetAvatar(
                petId = pet?.id ?: record.petId,
                petName = pet?.name ?: "Pet",
                species = pet?.speciesEnum ?: PetSpecies.OTHER,
                photoVersion = photoVersion,
                size = 44.dp,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = record.vaccineName,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = pet?.name ?: "Unknown Pet",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              VaccinationUrgencyBadge(urgency = record.urgency())
            }

            Text(
              text = buildString {
                append(if (record.statusEnum == VaccinationStatus.COMPLETED) "Given " else "Scheduled ")
                append(formatEpochDate(record.administeredDateEpochMs))
                record.nextDueDateEpochMs?.let {
                  append(" · Next due ")
                  append(formatEpochDate(it))
                }
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (record.statusEnum == VaccinationStatus.SCHEDULED) {
              OutlinedButton(
                onClick = { onToggleStatus(record) },
                modifier = Modifier.height(40.dp),
              ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark Completed")
              }
            }
          }
        }
      }
    }
  }
}
