package com.luminens.android.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminens.android.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs,
)

private val InterFontName = GoogleFont("Inter")

private val InterFont = FontFamily(
    Font(googleFont = InterFontName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFontName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFontName, fontProvider = provider, weight = FontWeight.Bold),
)

val LuminensTypography = Typography(
    displayLarge   = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Bold,     fontSize = 45.sp),
    displaySmall   = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 36.sp),
    headlineLarge  = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall  = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 16.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 14.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Normal,   fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = InterFont, fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 0.5.sp),
)

val LuminensShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val DarkColorScheme = darkColorScheme(
    primary            = BrandPrimary,
    onPrimary          = BrandOnPrimary,
    primaryContainer   = BrandPrimaryDark,
    onPrimaryContainer = BrandOnPrimary,
    secondary          = SurfaceSecondary,
    onSecondary        = OnSurface,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = OnSurface,
    background         = SurfaceDark,
    onBackground       = OnSurface,
    surface            = SurfaceCard,
    onSurface          = OnSurface,
    surfaceVariant     = SurfaceElevated,
    onSurfaceVariant   = OnSurfaceVariant,
    outline            = SurfaceBorder,
    outlineVariant     = SurfaceBorder,
    error              = ErrorColor,
    onError            = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary            = BrandPrimaryDark,
    onPrimary          = Color.White,
    primaryContainer   = BrandAccent,
    onPrimaryContainer = Color(0xFF3D2800),
    secondary          = Color(0xFFEEEEEE),
    onSecondary        = Color(0xFF111111),
    background         = Color(0xFFF9F6F0),
    onBackground       = Color(0xFF111111),
    surface            = Color.White,
    onSurface          = Color(0xFF111111),
    surfaceVariant     = Color(0xFFF0EDE8),
    onSurfaceVariant   = Color(0xFF555555),
    outline            = Color(0xFFDDD8D0),
    error              = ErrorColor,
    onError            = Color.White,
)

@Composable
fun LuminensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = LuminensTypography,
        shapes      = LuminensShapes,
        content     = content,
    )
}
