package org.openscanner.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

// ---- Color tokens (fixed Field Console palette, ADR 0001; no dynamic color) ----
// Surfaces and structure.
val ScannerBackground = Color(0xFF0D0F10)
val ScannerSurface = Color(0xFF171A1C)
val ScannerSurfaceRaised = Color(0xFF1D2124)
val ScannerBorder = Color(0xFF353B3F)

// Text.
val ScannerText = Color(0xFFF7F8F9)
val ScannerMuted = Color(0xFFADB4B9)

// Accent family (teal/lime/amber instrument roles).
val ScannerCyan = Color(0xFF45C9E9)
val ScannerAmber = Color(0xFFFFB10F)
val ScannerGreen = Color(0xFF6BD28A)
val ScannerPurple = Color(0xFFB68BE1)
val ScannerOrange = Color(0xFFF4A340)

// Foreground printed on top of accent fills (must stay dark for contrast).
val ScannerOnCyan = Color(0xFF071013)
val ScannerOnAmber = Color(0xFF151515)

// Recessed well behind cyan instrument icons (was a duplicated hardcoded hex).
val ScannerIconWell = Color(0xFF18333A)

// Positive/ok banner surface pair.
val ScannerPositiveSurface = Color(0xFF16231B)
val ScannerPositiveBorder = Color(0xFF31523D)

// ---- Spacing tokens (4 dp grid) ----
object ScannerSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp

    /** Minimum interactive target per the accessibility release gate. */
    val MinTouchTarget = 48.dp
}

// ---- Type scale ----
// Deliberate sizes; do not scatter ad-hoc sp values in screens.
// labelSmall (11sp) is the floor: axis tick labels and the densest captions.
val ScannerTypography = Typography(
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Medium),
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

private val DarkColors = darkColorScheme(
    primary = ScannerCyan,
    onPrimary = ScannerOnCyan,
    secondary = ScannerAmber,
    onSecondary = ScannerOnAmber,
    tertiary = ScannerGreen,
    background = ScannerBackground,
    onBackground = ScannerText,
    surface = ScannerSurface,
    onSurface = ScannerText,
    surfaceVariant = ScannerSurfaceRaised,
    onSurfaceVariant = ScannerMuted,
    outline = ScannerBorder,
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00677A),
    onPrimary = Color.White,
    secondary = Color(0xFF805500),
    onSecondary = Color.White,
    tertiary = Color(0xFF196B38),
    background = Color(0xFFF7FAFA),
    onBackground = Color(0xFF111415),
    surface = Color.White,
    onSurface = Color(0xFF111415),
    surfaceVariant = Color(0xFFE8EDEF),
    onSurfaceVariant = Color(0xFF41484C),
    outline = Color(0xFF737C80),
)

@Composable
fun OpenScannerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ScannerTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
