package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.HealthRecord
import com.example.data.model.Pet
import com.example.data.model.VaccinationRecord
import com.example.data.repository.PetCareRepository
import com.example.ui.components.AccountSheet
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.HealthRecordFormSheet
import com.example.ui.components.PetFormSheet
import com.example.ui.components.VaccinationFormSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PetDetailScreen
import com.example.ui.screens.PetsDashboardScreen
import com.example.ui.screens.VaccinesScheduleScreen
import com.example.ui.theme.PetCareTheme
import com.example.ui.viewmodel.PetCareDashboardData
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class TopTab(val label: String) {
  PETS("Pets"),
  VACCINES("Vaccines"),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PetCareTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  var startupCheckDone by remember { mutableStateOf(auth.currentUser != null) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { startupCheckDone = true },
        onUnauthenticated = { startupCheckDone = true },
        scope = scope,
      )
    } else {
      startupCheckDone = true
    }
  }

  val user = currentUser
  if (user == null) {
    if (!startupCheckDone) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    } else {
      AuthScreen(onAuthSuccess = { startupCheckDone = true })
    }
  } else {
    val uid = user.uid
    key(uid) {
      PetCareAuthenticatedShell(
        currentUser = user,
        onSignOut = {
          signOut(credentialManager = credentialManager, scope = scope)
        },
      )
    }
  }
}

@Composable
private fun PetCareAuthenticatedShell(
  currentUser: FirebaseUser,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val currentUserId = currentUser.uid

  val viewModel: PetCareViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        PetCareViewModel(PetCareRepository(db), currentUserId)
      }
    },
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val photoVersion by viewModel.photoVersion.collectAsStateWithLifecycle()
  val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.feedbackMessage.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  var activeTab by rememberSaveable { mutableStateOf(TopTab.PETS) }
  var selectedPetId by rememberSaveable { mutableStateOf<String?>(null) }

  var showPetForm by remember { mutableStateOf(false) }
  var editingPet by remember { mutableStateOf<Pet?>(null) }
  var petPendingDelete by remember { mutableStateOf<Pet?>(null) }

  var vaccinationTargetPet by remember { mutableStateOf<Pet?>(null) }
  var editingVaccination by remember { mutableStateOf<VaccinationRecord?>(null) }
  var vaccinationPendingDelete by remember { mutableStateOf<VaccinationRecord?>(null) }

  var healthTargetPet by remember { mutableStateOf<Pet?>(null) }
  var editingHealthRecord by remember { mutableStateOf<HealthRecord?>(null) }
  var healthPendingDelete by remember { mutableStateOf<HealthRecord?>(null) }

  var showAccountSheet by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWideScreen = maxWidth >= 840.dp
    val dashboardData = (uiState as? UiState.Success<PetCareDashboardData>)?.data
    val pets = dashboardData?.pets.orEmpty()
    val vaccinations = dashboardData?.vaccinations.orEmpty()
    val healthRecords = dashboardData?.healthRecords.orEmpty()
    val selectedPet = pets.firstOrNull { it.id == selectedPetId }

    val showCompactDetail = !isWideScreen && activeTab == TopTab.PETS && selectedPet != null

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      contentWindowInsets = WindowInsets.safeDrawing,
      floatingActionButton = {
        if (uiState is UiState.Success && activeTab == TopTab.PETS && (!showCompactDetail)) {
          ExtendedFloatingActionButton(
            onClick = {
              editingPet = null
              showPetForm = true
            },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add Pet") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("add_pet_fab"),
          )
        }
      },
      bottomBar = {
        if (!isWideScreen && !showCompactDetail) {
          NavigationBar {
            TopTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = {
                  activeTab = tab
                  if (tab != TopTab.PETS) selectedPetId = null
                },
                icon = {
                  Icon(
                    imageVector = when (tab) {
                      TopTab.PETS -> if (selected) Icons.Filled.Pets else Icons.Outlined.Pets
                      TopTab.VACCINES -> if (selected) Icons.Filled.Vaccines else Icons.Outlined.Vaccines
                    },
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (isWideScreen) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            Spacer(Modifier.height(16.dp))
            TopTab.entries.forEach { tab ->
              val selected = activeTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { activeTab = tab },
                icon = {
                  Icon(
                    imageVector = when (tab) {
                      TopTab.PETS -> if (selected) Icons.Filled.Pets else Icons.Outlined.Pets
                      TopTab.VACCINES -> if (selected) Icons.Filled.Vaccines else Icons.Outlined.Vaccines
                    },
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
              )
            }
          }
          VerticalDivider()
        }

        when (val state = uiState) {
          UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                CircularProgressIndicator()
                Text(
                  "Syncing your PetCare records...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text("Unable to load PetCare data", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
                Button(onClick = { viewModel.retrySync() }) {
                  Text("Retry")
                }
              }
            }
          }

          is UiState.Success -> {
            if (activeTab == TopTab.VACCINES) {
              VaccinesScheduleScreen(
                pets = pets,
                vaccinations = vaccinations,
                photoVersion = photoVersion,
                onSelectPet = { pet ->
                  selectedPetId = pet.id
                  activeTab = TopTab.PETS
                },
                onToggleStatus = { record -> viewModel.toggleVaccinationStatus(record) },
              )
            } else if (isWideScreen) {
              Row(modifier = Modifier.fillMaxSize()) {
                PetsDashboardScreen(
                  pets = pets,
                  vaccinations = vaccinations,
                  healthRecords = healthRecords,
                  selectedPetId = selectedPet?.id,
                  photoVersion = photoVersion,
                  userDisplayName = currentUser.displayName,
                  onSelectPet = { pet -> selectedPetId = pet.id },
                  onAddPetClick = {
                    editingPet = null
                    showPetForm = true
                  },
                  onOpenAccountClick = { showAccountSheet = true },
                  modifier = Modifier.weight(0.45f),
                )
                VerticalDivider()
                val detailPet = selectedPet ?: pets.firstOrNull()
                if (detailPet != null) {
                  PetDetailScreen(
                    pet = detailPet,
                    vaccinations = vaccinations.filter { it.petId == detailPet.id },
                    healthRecords = healthRecords.filter { it.petId == detailPet.id },
                    photoVersion = photoVersion,
                    showBackButton = false,
                    onBack = { selectedPetId = null },
                    onEditPet = {
                      editingPet = detailPet
                      showPetForm = true
                    },
                    onDeletePet = { petPendingDelete = detailPet },
                    onUpdatePhoto = { uri -> viewModel.updatePetPhoto(appContext, detailPet.id, uri) },
                    onAddVaccination = {
                      editingVaccination = null
                      vaccinationTargetPet = detailPet
                    },
                    onEditVaccination = { rec ->
                      editingVaccination = rec
                      vaccinationTargetPet = detailPet
                    },
                    onToggleVaccinationStatus = { rec -> viewModel.toggleVaccinationStatus(rec) },
                    onDeleteVaccination = { rec -> vaccinationPendingDelete = rec },
                    onAddHealthRecord = {
                      editingHealthRecord = null
                      healthTargetPet = detailPet
                    },
                    onEditHealthRecord = { rec ->
                      editingHealthRecord = rec
                      healthTargetPet = detailPet
                    },
                    onDeleteHealthRecord = { rec -> healthPendingDelete = rec },
                    modifier = Modifier.weight(0.55f),
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .weight(0.55f)
                      .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      "Select or add a pet to view care records",
                      style = MaterialTheme.typography.bodyLarge,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            } else if (selectedPet != null) {
              PetDetailScreen(
                pet = selectedPet,
                vaccinations = vaccinations.filter { it.petId == selectedPet.id },
                healthRecords = healthRecords.filter { it.petId == selectedPet.id },
                photoVersion = photoVersion,
                showBackButton = true,
                onBack = { selectedPetId = null },
                onEditPet = {
                  editingPet = selectedPet
                  showPetForm = true
                },
                onDeletePet = { petPendingDelete = selectedPet },
                onUpdatePhoto = { uri -> viewModel.updatePetPhoto(appContext, selectedPet.id, uri) },
                onAddVaccination = {
                  editingVaccination = null
                  vaccinationTargetPet = selectedPet
                },
                onEditVaccination = { rec ->
                  editingVaccination = rec
                  vaccinationTargetPet = selectedPet
                },
                onToggleVaccinationStatus = { rec -> viewModel.toggleVaccinationStatus(rec) },
                onDeleteVaccination = { rec -> vaccinationPendingDelete = rec },
                onAddHealthRecord = {
                  editingHealthRecord = null
                  healthTargetPet = selectedPet
                },
                onEditHealthRecord = { rec ->
                  editingHealthRecord = rec
                  healthTargetPet = selectedPet
                },
                onDeleteHealthRecord = { rec -> healthPendingDelete = rec },
              )
            } else {
              PetsDashboardScreen(
                pets = pets,
                vaccinations = vaccinations,
                healthRecords = healthRecords,
                selectedPetId = null,
                photoVersion = photoVersion,
                userDisplayName = currentUser.displayName,
                onSelectPet = { pet -> selectedPetId = pet.id },
                onAddPetClick = {
                  editingPet = null
                  showPetForm = true
                },
                onOpenAccountClick = { showAccountSheet = true },
              )
            }
          }
        }
      }
    }

    if (showPetForm) {
      PetFormSheet(
        existingPet = editingPet,
        photoVersion = photoVersion,
        isSaving = isMutating,
        onDismiss = {
          if (!isMutating) {
            showPetForm = false
            editingPet = null
          }
        },
        onSave = { name, species, breed, ageYears, ageMonths, weightKg, microchipId, notes, pendingPhotoUri, removeExistingPhoto ->
          viewModel.savePet(
            appContext = appContext,
            existingPetId = editingPet?.id,
            name = name,
            species = species,
            breed = breed,
            ageYears = ageYears,
            ageMonths = ageMonths,
            weightKg = weightKg,
            microchipId = microchipId,
            notes = notes,
            pendingPhotoUri = pendingPhotoUri,
            removeExistingPhoto = removeExistingPhoto,
            onSuccess = { savedId ->
              showPetForm = false
              editingPet = null
              if (selectedPetId != null) selectedPetId = savedId
            },
          )
        },
      )
    }

    vaccinationTargetPet?.let { targetPet ->
      VaccinationFormSheet(
        existingRecord = editingVaccination,
        petName = targetPet.name,
        isSaving = isMutating,
        onDismiss = {
          if (!isMutating) {
            vaccinationTargetPet = null
            editingVaccination = null
          }
        },
        onSave = { vaccineName, administeredMs, nextDueMs, clinicOrVet, notes, status ->
          viewModel.saveVaccination(
            existingVaccinationId = editingVaccination?.id,
            petId = targetPet.id,
            vaccineName = vaccineName,
            administeredDateEpochMs = administeredMs,
            nextDueDateEpochMs = nextDueMs,
            clinicOrVet = clinicOrVet,
            notes = notes,
            status = status,
            onSuccess = {
              vaccinationTargetPet = null
              editingVaccination = null
            },
          )
        },
      )
    }

    healthTargetPet?.let { targetPet ->
      HealthRecordFormSheet(
        existingRecord = editingHealthRecord,
        petName = targetPet.name,
        isSaving = isMutating,
        onDismiss = {
          if (!isMutating) {
            healthTargetPet = null
            editingHealthRecord = null
          }
        },
        onSave = { title, category, recordDateMs, vetName, details ->
          viewModel.saveHealthRecord(
            existingRecordId = editingHealthRecord?.id,
            petId = targetPet.id,
            title = title,
            category = category,
            recordDateEpochMs = recordDateMs,
            vetName = vetName,
            details = details,
            onSuccess = {
              healthTargetPet = null
              editingHealthRecord = null
            },
          )
        },
      )
    }

    petPendingDelete?.let { pet ->
      ConfirmDeleteDialog(
        title = "Remove ${pet.name}?",
        message = "This will permanently delete ${pet.name}'s profile along with all linked vaccination and health records.",
        confirmLabel = "Remove Pet",
        onConfirm = {
          petPendingDelete = null
          viewModel.deletePet(appContext, pet) {
            if (selectedPetId == pet.id) selectedPetId = null
          }
        },
        onDismiss = { petPendingDelete = null },
      )
    }

    vaccinationPendingDelete?.let { rec ->
      ConfirmDeleteDialog(
        title = "Delete vaccination?",
        message = "Remove '${rec.vaccineName}' from vaccination history?",
        onConfirm = {
          vaccinationPendingDelete = null
          viewModel.deleteVaccination(rec)
        },
        onDismiss = { vaccinationPendingDelete = null },
      )
    }

    healthPendingDelete?.let { rec ->
      ConfirmDeleteDialog(
        title = "Delete health record?",
        message = "Remove '${rec.title}' from health records?",
        onConfirm = {
          healthPendingDelete = null
          viewModel.deleteHealthRecord(rec)
        },
        onDismiss = { healthPendingDelete = null },
      )
    }

    if (showAccountSheet) {
      AccountSheet(
        displayName = currentUser.displayName,
        contactInfo = currentUser.email ?: currentUser.phoneNumber,
        petCount = pets.size,
        vaccineCount = vaccinations.size,
        onSignOut = {
          showAccountSheet = false
          onSignOut()
        },
        onDismiss = { showAccountSheet = false },
      )
    }
  }
}
