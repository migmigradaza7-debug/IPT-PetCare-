package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.PhoneAuthStep
import com.example.auth.normalizePhoneNumber
import com.example.auth.onGoogleSignInClicked
import com.example.auth.startPhoneVerification
import com.example.auth.verifyPhoneCode

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isGoogleLoading by remember { mutableStateOf(false) }
  var isPhoneLoading by remember { mutableStateOf(false) }
  var showPhoneForm by remember { mutableStateOf(false) }
  var phoneNumberInput by remember { mutableStateOf("") }
  var otpCodeInput by remember { mutableStateOf("") }
  var phoneStep by remember { mutableStateOf<PhoneAuthStep>(PhoneAuthStep.EnterPhone) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val anyLoading = isGoogleLoading || isPhoneLoading

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .imePadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Pets,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "PetCare Journal",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(Modifier.height(18.dp))

        Image(
          painter = painterResource(id = R.drawable.img_auth_hero),
          contentDescription = "PetCare dog and cat care journal illustration",
          modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp)),
          contentScale = ContentScale.Crop,
        )

        Spacer(Modifier.height(20.dp))

        Text(
          text = "Every pet's health,\nin one calm place.",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
          text = "Keep profiles, vaccination timelines, and veterinary notes organized and synced safely in the cloud.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))

        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FeatureHighlightRow(
              icon = Icons.Filled.Vaccines,
              title = "Vaccination tracking",
              subtitle = "Spot upcoming boosters and overdue shots at a glance.",
            )
            FeatureHighlightRow(
              icon = Icons.Filled.MedicalServices,
              title = "Health & vet records",
              subtitle = "Log checkups, medications, allergies, and weight history.",
            )
            FeatureHighlightRow(
              icon = Icons.Filled.CloudDone,
              title = "Private cloud sync",
              subtitle = "Encrypted per-account storage with Google or phone sign-in.",
            )
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isGoogleLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isGoogleLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isGoogleLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isGoogleLoading = false
              },
            )
          },
          enabled = !anyLoading,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("sign_in_google_button"),
        ) {
          if (isGoogleLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text("Signing in...", style = MaterialTheme.typography.labelLarge)
          } else {
            Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = "or",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        if (!showPhoneForm) {
          OutlinedButton(
            onClick = {
              errorMessage = null
              showPhoneForm = true
            },
            enabled = !anyLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("open_phone_sign_in_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Phone,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Continue with Phone Number", style = MaterialTheme.typography.labelLarge)
          }
        }

        AnimatedVisibility(visible = showPhoneForm) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              when (val step = phoneStep) {
                PhoneAuthStep.EnterPhone -> {
                  Text(
                    text = "Sign up or log in with SMS",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  OutlinedTextField(
                    value = phoneNumberInput,
                    onValueChange = {
                      phoneNumberInput = it
                      errorMessage = null
                    },
                    label = { Text("Phone Number (with country code)") },
                    placeholder = { Text("+1 555 123 4567") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("phone_number_input"),
                  )
                  Text(
                    text = "Include '+' and your country code (for example, +1 for US/Canada or +63 for Philippines).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    TextButton(
                      onClick = {
                        showPhoneForm = false
                        errorMessage = null
                      },
                      enabled = !anyLoading,
                      modifier = Modifier.weight(0.38f),
                    ) {
                      Text("Cancel")
                    }
                    Button(
                      onClick = {
                        val activity = context as? Activity
                        if (activity == null) {
                          errorMessage = "Unable to start phone verification from current context."
                          return@Button
                        }
                        errorMessage = null
                        isPhoneLoading = true
                        val normalized = normalizePhoneNumber(phoneNumberInput)
                        startPhoneVerification(
                          activity = activity,
                          phoneNumber = normalized,
                          resendToken = null,
                          scope = scope,
                          onCodeSent = { verificationId, token ->
                            isPhoneLoading = false
                            otpCodeInput = ""
                            phoneStep = PhoneAuthStep.EnterCode(
                              phoneNumber = normalized,
                              verificationId = verificationId,
                              resendToken = token,
                            )
                          },
                          onAutoVerified = {
                            isPhoneLoading = false
                            onAuthSuccess()
                          },
                          onError = { msg ->
                            isPhoneLoading = false
                            errorMessage = msg
                          },
                        )
                      },
                      enabled = !anyLoading,
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier
                        .weight(0.62f)
                        .height(48.dp)
                        .testTag("send_otp_button"),
                    ) {
                      if (isPhoneLoading) {
                        CircularProgressIndicator(
                          modifier = Modifier.size(18.dp),
                          color = MaterialTheme.colorScheme.onPrimary,
                          strokeWidth = 2.dp,
                        )
                      } else {
                        Text("Send Code")
                      }
                    }
                  }
                }

                is PhoneAuthStep.EnterCode -> {
                  Text(
                    text = "Enter verification code",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "We sent a 6-digit SMS code to ${step.phoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  OutlinedTextField(
                    value = otpCodeInput,
                    onValueChange = { input ->
                      if (input.all { it.isDigit() } && input.length <= 6) {
                        otpCodeInput = input
                        errorMessage = null
                      }
                    },
                    label = { Text("6-Digit Code") },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Filled.Sms, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("otp_code_input"),
                  )
                  Button(
                    onClick = {
                      errorMessage = null
                      isPhoneLoading = true
                      verifyPhoneCode(
                        verificationId = step.verificationId,
                        smsCode = otpCodeInput,
                        scope = scope,
                        onSuccess = {
                          isPhoneLoading = false
                          onAuthSuccess()
                        },
                        onError = { msg ->
                          isPhoneLoading = false
                          errorMessage = msg
                        },
                      )
                    },
                    enabled = !anyLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .testTag("verify_otp_button"),
                  ) {
                    if (isPhoneLoading) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                      )
                    } else {
                      Text("Verify & Continue")
                    }
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    TextButton(
                      onClick = {
                        phoneStep = PhoneAuthStep.EnterPhone
                        otpCodeInput = ""
                        errorMessage = null
                      },
                      enabled = !anyLoading,
                    ) {
                      Text("Change number")
                    }
                    TextButton(
                      onClick = {
                        val activity = context as? Activity ?: return@TextButton
                        errorMessage = null
                        isPhoneLoading = true
                        startPhoneVerification(
                          activity = activity,
                          phoneNumber = step.phoneNumber,
                          resendToken = step.resendToken,
                          scope = scope,
                          onCodeSent = { newId, newToken ->
                            isPhoneLoading = false
                            phoneStep = step.copy(
                              verificationId = newId,
                              resendToken = newToken,
                            )
                          },
                          onAutoVerified = {
                            isPhoneLoading = false
                            onAuthSuccess()
                          },
                          onError = { msg ->
                            isPhoneLoading = false
                            errorMessage = msg
                          },
                        )
                      },
                      enabled = !anyLoading,
                    ) {
                      Text("Resend code")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.size(36.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
