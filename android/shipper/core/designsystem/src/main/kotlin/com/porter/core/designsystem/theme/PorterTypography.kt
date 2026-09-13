package com.porter.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Font Family ────────────────────────────────────────────────────────────────
// Inter — closest open-source substitute for SF Pro.
// On macOS/iOS, system-ui resolves to SF Pro. Inter is our fallback for Android.
// Per design.md: "Inter at weight 600 with font-feature-settings: 'ss03'"
val InterFontFamily = FontFamily.SansSerif

// ── Typography Tokens ─────────────────────────────────────────────────────────
// Exactly matching design.md type scale.
// Note: Android letterSpacing in sp; design.md values in px → divide by base font-size

/** hero-display — 56sp / W600 / lh 1.07 / ls -0.28px ≈ -0.005em */
val HeroDisplay = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 56.sp,
    lineHeight = (56 * 1.07).sp,
    letterSpacing = (-0.3).sp
)

/** display-lg — 40sp / W600 / lh 1.10 / ls 0 */
val DisplayLg = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp,
    lineHeight = (40 * 1.10).sp,
    letterSpacing = 0.sp
)

/** display-md — 34sp / W600 / lh 1.47 / ls -0.374px ≈ -0.011em */
val DisplayMd = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    lineHeight = (34 * 1.47).sp,
    letterSpacing = (-0.37).sp
)

/** lead — 28sp / W400 / lh 1.14 / ls 0.196px */
val Lead = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = (28 * 1.14).sp,
    letterSpacing = 0.2.sp
)

/** lead-airy — 24sp / W300 / lh 1.5 / ls 0 (the rare light weight) */
val LeadAiry = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 24.sp,
    lineHeight = (24 * 1.5).sp,
    letterSpacing = 0.sp
)

/** tagline — 21sp / W600 / lh 1.19 / ls 0.231px */
val Tagline = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    lineHeight = (21 * 1.19).sp,
    letterSpacing = 0.23.sp
)

/** body-strong — 17sp / W600 / lh 1.24 / ls -0.374px */
val BodyStrong = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = (17 * 1.24).sp,
    letterSpacing = (-0.37).sp
)

/** body — 17sp / W400 / lh 1.47 / ls -0.374px  ← the canonical body style */
val BodyDefault = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = (17 * 1.47).sp,
    letterSpacing = (-0.37).sp
)

/** dense-link — 17sp / W400 / lh 2.41 / ls 0 (relaxed footer link leading) */
val DenseLink = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = (17 * 2.41).sp,
    letterSpacing = 0.sp
)

/** caption — 14sp / W400 / lh 1.43 / ls -0.224px */
val Caption = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = (14 * 1.43).sp,
    letterSpacing = (-0.22).sp
)

/** caption-strong — 14sp / W600 / lh 1.29 / ls -0.224px */
val CaptionStrong = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = (14 * 1.29).sp,
    letterSpacing = (-0.22).sp
)

/** button-large — 18sp / W300 / lh 1.0 / ls 0 (rare W300 on store hero CTAs) */
val ButtonLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 18.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

/** button-utility — 14sp / W400 / lh 1.29 / ls -0.224px */
val ButtonUtility = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = (14 * 1.29).sp,
    letterSpacing = (-0.22).sp
)

/** fine-print — 12sp / W400 / lh 1.0 / ls -0.12px */
val FinePrint = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 12.sp,
    letterSpacing = (-0.12).sp
)

/** nav-link — 12sp / W400 / lh 1.0 / ls -0.12px */
val NavLink = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 12.sp,
    letterSpacing = (-0.12).sp
)

/** micro-legal — 10sp / W400 / lh 1.3 / ls -0.08px */
val MicroLegal = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    lineHeight = (10 * 1.3).sp,
    letterSpacing = (-0.08).sp
)

// ── Material 3 Typography mapping ─────────────────────────────────────────────
// Maps Porter tokens to M3 text styles for components that use Material defaults.
val PorterTypography = Typography(
    displayLarge = HeroDisplay,
    displayMedium = DisplayLg,
    displaySmall = DisplayMd,
    headlineLarge = Lead,
    headlineMedium = LeadAiry,
    headlineSmall = Tagline,
    titleLarge = BodyStrong,
    titleMedium = BodyDefault,
    titleSmall = Caption,
    bodyLarge = BodyDefault,
    bodyMedium = Caption,
    bodySmall = FinePrint,
    labelLarge = ButtonUtility,
    labelMedium = CaptionStrong,
    labelSmall = NavLink,
)
