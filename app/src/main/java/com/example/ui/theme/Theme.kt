package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = SagePrimary,
  onPrimary = OnSagePrimary,
  primaryContainer = SageContainer,
  onPrimaryContainer = OnSageContainer,
  secondary = OchreSecondary,
  onSecondary = OnOchreSecondary,
  secondaryContainer = OchreContainer,
  onSecondaryContainer = OnOchreContainer,
  tertiary = SlateTertiary,
  onTertiary = OnSlateTertiary,
  tertiaryContainer = SlateContainer,
  onTertiaryContainer = OnSlateContainer,
  error = TerracottaError,
  onError = OnTerracottaError,
  errorContainer = TerracottaContainer,
  onErrorContainer = OnTerracottaContainer,
  background = AlabasterBackground,
  onBackground = OnAlabasterText,
  surface = CreamSurface,
  onSurface = OnAlabasterText,
  surfaceVariant = CreamSurfaceContainer,
  onSurfaceVariant = WarmOnSurfaceVariant,
  surfaceContainerLowest = CreamSurfaceLowest,
  surfaceContainerLow = CreamSurfaceLow,
  surfaceContainer = CreamSurfaceContainer,
  surfaceContainerHigh = CreamSurfaceHigh,
  outline = WarmOutline,
  outlineVariant = WarmOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
  primary = SagePrimaryDark,
  onPrimary = OnSagePrimaryDark,
  primaryContainer = SageContainerDark,
  onPrimaryContainer = OnSageContainerDark,
  secondary = OchreSecondaryDark,
  onSecondary = OnOchreSecondaryDark,
  secondaryContainer = OchreContainerDark,
  onSecondaryContainer = OnOchreContainerDark,
  tertiary = SlateTertiaryDark,
  onTertiary = OnSlateTertiaryDark,
  tertiaryContainer = SlateContainerDark,
  onTertiaryContainer = OnSlateContainerDark,
  error = TerracottaErrorDark,
  onError = OnTerracottaErrorDark,
  errorContainer = TerracottaContainerDark,
  onErrorContainer = OnTerracottaContainerDark,
  background = PineBackgroundDark,
  onBackground = OnPineTextDark,
  surface = PineBackgroundDark,
  onSurface = OnPineTextDark,
  surfaceVariant = PineSurfaceContainerDark,
  onSurfaceVariant = PineOnSurfaceVariantDark,
  surfaceContainerLowest = PineSurfaceLowestDark,
  surfaceContainerLow = PineSurfaceLowDark,
  surfaceContainer = PineSurfaceContainerDark,
  surfaceContainerHigh = PineSurfaceHighDark,
  outline = PineOutlineDark,
  outlineVariant = PineOutlineVariantDark,
)

val PetCareShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PetCareTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = PetCareTypography,
    shapes = PetCareShapes,
    content = content,
  )
}
