package com.porter.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ── Brand & Accent ──────────────────────────────────────────────────────────────
/** Action Blue — the single interactive accent. All links, pill CTAs, focus rings. */
val ActionBlue = Color(0xFF0066CC)

/** Focus Blue — keyboard focus ring on buttons (outline: 2px solid). */
val FocusBlue = Color(0xFF0071E3)

/** Sky Link Blue — interactive links on dark tile surfaces only. */
val SkyLinkBlue = Color(0xFF2997FF)

// ── Surface ─────────────────────────────────────────────────────────────────────
/** Pure White — dominant canvas, utility cards, configurator grids. */
val CanvasWhite = Color(0xFFFFFFFF)

/** Parchment — signature Apple off-white; alternating tiles, footer, store sections. */
val Parchment = Color(0xFFF5F5F7)

/** Pearl Button — near-white fill for secondary ghost buttons on parchment surfaces. */
val PearlButton = Color(0xFFFAFAFC)

/** Near-Black Tile 1 — primary dark tile surface. */
val DarkTile1 = Color(0xFF272729)

/** Near-Black Tile 2 — micro-step lighter, adjacent dark tile separation. */
val DarkTile2 = Color(0xFF2A2A2C)

/** Near-Black Tile 3 — micro-step darker, bottom of stack / video frames. */
val DarkTile3 = Color(0xFF252527)

/** Pure Black — global nav bar, video player backgrounds, photographic overlays. */
val PureBlack = Color(0xFF000000)

/** Translucent Chip Gray — base hex for circular icon chips over photography. */
val ChipGray = Color(0xFFD2D2D7)

// ── Text ────────────────────────────────────────────────────────────────────────
/** Near-Black Ink — all headlines and body text on light surfaces. */
val InkNearBlack = Color(0xFF1D1D1F)

/** Body On Dark — all text on dark tiles and global nav. */
val BodyOnDark = Color(0xFFFFFFFF)

/** Body Muted — secondary copy on dark tiles. */
val BodyMuted = Color(0xFFCCCCCC)

/** Ink Muted 80 — body text on Pearl Button surface. */
val InkMuted80 = Color(0xFF333333)

/** Ink Muted 48 — disabled button text and legal fine-print. */
val InkMuted48 = Color(0xFF7A7A7A)

// ── Hairlines & Borders ──────────────────────────────────────────────────────────
/** Divider Soft — ring shadow on secondary buttons. */
val DividerSoft = Color(0xFFF0F0F0)

/** Hairline — 1px border on store utility cards and configurator chips. */
val Hairline = Color(0xFFE0E0E0)

// ── Status ───────────────────────────────────────────────────────────────────────
val StatusSuccess = Color(0xFF34C759)
val StatusWarning = Color(0xFFFF9500)
val StatusError = Color(0xFFFF3B30)
val StatusInfo = ActionBlue
val StatusNeutral = Color(0xFF8E8E93)
val StatusStale = Color(0xFFFF9500)
val StatusOffline = Color(0xFFFF3B30)
