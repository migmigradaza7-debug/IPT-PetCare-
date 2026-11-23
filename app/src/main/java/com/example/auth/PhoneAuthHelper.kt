package com.example.auth

import android.app.Activity
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val PHONE_AUTH_TAG = "PhoneAuth"
private val E164_REGEX = Regex("^\\+[1-9]\\d{7,14}$")

sealed interface PhoneAuthStep {
  data object EnterPhone : PhoneAuthStep
  data class EnterCode(
    val phoneNumber: String,
    val verificationId: String,
    val resendToken: PhoneAuthProvider.ForceResendingToken?,
  ) : PhoneAuthStep
}

fun normalizePhoneNumber(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isEmpty()) return ""
  val sb = StringBuilder()
  trimmed.forEachIndexed { index, ch ->
    when {
      index == 0 && ch == '+' -> sb.append(ch)
      ch.isDigit() -> sb.append(ch)
    }
  }
  return sb.toString()
}

fun isValidE164PhoneNumber(normalized: String): Boolean = E164_REGEX.matches(normalized)

fun startPhoneVerification(
  activity: Activity,
  phoneNumber: String,
  resendToken: PhoneAuthProvider.ForceResendingToken?,
  scope: CoroutineScope,
  onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
  onAutoVerified: () -> Unit,
  onError: (String) -> Unit,
) {
  val normalized = normalizePhoneNumber(phoneNumber)
  if (!isValidE164PhoneNumber(normalized)) {
    onError("Enter a valid phone number with country code (e.g., +15551234567).")
    return
  }

  val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
      scope.launch {
        try {
          Firebase.auth.signInWithCredential(credential).await()
          onAutoVerified()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(PHONE_AUTH_TAG, "Auto phone sign-in failed", e)
          onError(mapPhoneAuthError(e))
        }
      }
    }

    override fun onVerificationFailed(e: FirebaseException) {
      Log.w(PHONE_AUTH_TAG, "Phone verification failed: ${e.message}", e)
      onError(mapPhoneAuthError(e))
    }

    override fun onCodeSent(
      verificationId: String,
      token: PhoneAuthProvider.ForceResendingToken,
    ) {
      onCodeSent(verificationId, token)
    }
  }

  val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
    .setPhoneNumber(normalized)
    .setTimeout(60L, TimeUnit.SECONDS)
    .setActivity(activity)
    .setCallbacks(callbacks)

  if (resendToken != null) {
    builder.setForceResendingToken(resendToken)
  }

  PhoneAuthProvider.verifyPhoneNumber(builder.build())
}

fun verifyPhoneCode(
  verificationId: String,
  smsCode: String,
  scope: CoroutineScope,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val trimmedCode = smsCode.trim()
  if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
    onError("Enter the 6-digit verification code sent to your phone.")
    return
  }

  scope.launch {
    try {
      val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
      Firebase.auth.signInWithCredential(credential).await()
      onSuccess()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(PHONE_AUTH_TAG, "OTP verification failed", e)
      onError(mapPhoneAuthError(e))
    }
  }
}

private fun mapPhoneAuthError(e: Exception): String {
  return when (e) {
    is FirebaseAuthInvalidCredentialsException ->
      "Invalid phone number or verification code. Please check and try again."
    is FirebaseTooManyRequestsException ->
      "Too many verification attempts. Please wait a moment before trying again."
    is FirebaseAuthException -> {
      if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
        "Phone sign-in is not enabled yet in Firebase Console. Enable Phone under Authentication → Sign-in method."
      } else {
        e.localizedMessage ?: "Phone authentication failed."
      }
    }
    else -> e.localizedMessage ?: "Phone verification failed. Please try again."
  }
}
