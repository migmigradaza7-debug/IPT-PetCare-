package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrueltyFree
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.local.PetPhotoStorage
import com.example.data.model.HealthRecordCategory
import com.example.data.model.PetSpecies
import com.example.data.model.VaccinationUrgency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatEpochDate(epochMs: Long): String {
  if (epochMs <= 0L) return "Not set"
  val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
  return formatter.format(Date(epochMs))
}

fun speciesIcon(species: PetSpecies): ImageVector = when (species) {
  PetSpecies.DOG -> Icons.Filled.Pets
  PetSpecies.CAT -> Icons.Filled.Pets
  PetSpecies.BIRD -> Icons.Filled.FlutterDash
  PetSpecies.RABBIT -> Icons.Filled.CrueltyFree
  PetSpecies.OTHER -> Icons.Filled.Pets
}

fun healthCategoryIcon(category: HealthRecordCategory): ImageVector = when (category) {
  HealthRecordCategory.CHECKUP -> Icons.Filled.MedicalServices
  HealthRecordCategory.MEDICATION -> Icons.Filled.Medication
  HealthRecordCategory.ALLERGY -> Icons.Filled.Healing
  HealthRecordCategory.WEIGHT -> Icons.Filled.MonitorWeight
  HealthRecordCategory.NOTE -> Icons.AutoMirrored.Filled.Notes
}

@Composable
fun PetAvatar(
  petId: String,
  petName: String,
  species: PetSpecies,
  photoVersion: Int,
  size: Dp = 60.dp,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val photoFile = remember(petId, photoVersion) {
    PetPhotoStorage.getPhotoFile(context, petId)
  }

  val (containerColor, contentColor) = when (species) {
    PetSpecies.DOG -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    PetSpecies.CAT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    PetSpecies.BIRD -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    PetSpecies.RABBIT -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    PetSpecies.OTHER -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(containerColor)
      .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (photoFile != null) {
      val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
          .data(photoFile)
          .setParameter("v", photoVersion)
          .crossfade(true)
          .build()
      )
      Image(
        painter = painter,
        contentDescription = "$petName photo",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Icon(
        imageVector = speciesIcon(species),
        contentDescription = "$petName (${species.label})",
        tint = contentColor,
        modifier = Modifier.size(size * 0.46f),
      )
    }
  }
}

@Composable
fun VaccinationUrgencyBadge(
  urgency: VaccinationUrgency,
  modifier: Modifier = Modifier,
) {
  val (bg, fg, icon) = when (urgency) {
    VaccinationUrgency.OVERDUE -> Triple(
      MaterialTheme.colorScheme.errorContainer,
      MaterialTheme.colorScheme.onErrorContainer,
      Icons.Filled.Warning,
    )
    VaccinationUrgency.DUE_SOON -> Triple(
      MaterialTheme.colorScheme.secondaryContainer,
      MaterialTheme.colorScheme.onSecondaryContainer,
      Icons.Filled.Schedule,
    )
    VaccinationUrgency.SCHEDULED -> Triple(
      MaterialTheme.colorScheme.tertiaryContainer,
      MaterialTheme.colorScheme.onTertiaryContainer,
      Icons.Filled.Event,
    )
    VaccinationUrgency.UP_TO_DATE -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      Icons.Filled.CheckCircle,
    )
  }
  StatusPill(label = urgency.label, containerColor = bg, contentColor = fg, icon = icon, modifier = modifier)
}

@Composable
fun StatusPill(
  label: String,
  containerColor: Color,
  contentColor: Color,
  icon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
