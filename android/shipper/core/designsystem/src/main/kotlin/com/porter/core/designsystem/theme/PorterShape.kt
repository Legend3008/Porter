package com.porter.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Border Radius Scale (from design.md) ────────────────────────────────────────
/** rounded.none — 0dp — full-bleed tiles (never rounded) */
val ShapeNone = RoundedCornerShape(0.dp)

/** rounded.xs — 5dp — inline links as subtle chips (rare) */
val ShapeXs = RoundedCornerShape(5.dp)

/** rounded.sm — 8dp — dark utility buttons, inline card imagery */
val ShapeSm = RoundedCornerShape(8.dp)

/** rounded.md — 11dp — white Pearl Button capsules */
val ShapeMd = RoundedCornerShape(11.dp)

/** rounded.lg — 18dp — store utility cards, accessories grid cards */
val ShapeLg = RoundedCornerShape(18.dp)

/** rounded.xl — 24dp — hero cards */
val ShapeXl = RoundedCornerShape(24.dp)

/** rounded.pill — full pill — primary blue CTAs, search, configurator chips */
val ShapePill = RoundedCornerShape(9999.dp)

/** rounded.full — circular — icon chips floating over photography */
val ShapeFull = RoundedCornerShape(50)

// ── Material 3 Shapes mapping ────────────────────────────────────────────────────
val PorterShapes = Shapes(
    extraSmall = ShapeXs,
    small = ShapeSm,
    medium = ShapeMd,
    large = ShapeLg,
    extraLarge = ShapePill
)
