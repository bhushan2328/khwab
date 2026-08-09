package com.toblad.khwab.ui.theme

import androidx.compose.ui.graphics.Color

// =======================================================
// KHWAB UI COLOR PALETTE — Black + Blue + Green
// =======================================================
// Base rule: pure black background, electric blue as the
// primary accent, teal-green as the secondary accent.
// All other UI draws from these two colours only.
// When Aura is active, AuraColorScheme overrides everything.
// =======================================================

// ── Backgrounds ─────────────────────────────────────────
val KhwabBackground     = Color(0xFF000000)   // Pure OLED black
val KhwabSurface        = Color(0xFF050505)   // Near-black surface
val KhwabCard           = Color(0xFF0A0F1A)   // Very dark blue-black for cards
val KhwabBorder         = Color(0xFF0F1E3A)   // Dark blue border

// ── Primary — Electric Blue ──────────────────────────────
val KhwabBlue           = Color(0xFF2979FF)   // Vivid electric blue
val KhwabBlueDark       = Color(0xFF1565C0)   // Deeper blue (unused in dark-only mode)
val KhwabBlueContainer  = Color(0xFF0A1F4D)   // Dark blue container
val KhwabBlueLight      = Color(0xFF82B1FF)   // Light blue for secondary text / hints

// ── Secondary — Teal Green ───────────────────────────────
val KhwabGreen          = Color(0xFF00E676)   // Vivid teal-green
val KhwabGreenContainer = Color(0xFF00291A)   // Dark green container
val KhwabGreenMuted     = Color(0xFF00BFA5)   // Muted teal for secondary uses

// ── Tertiary — kept for Aura compatibility ───────────────
val KhwabViolet         = Color(0xFF7C4DFF)   // Violet (Aura only)
val KhwabVioletContainer= Color(0xFF1A0A40)

// ── Danger ───────────────────────────────────────────────
val KhwabRed            = Color(0xFFFF1744)
val KhwabRedContainer   = Color(0xFF3D0010)

// ── Warning ──────────────────────────────────────────────
val KhwabYellow         = Color(0xFFFFD740)   // Amber-yellow

// ── Text ─────────────────────────────────────────────────
val KhwabWhite          = Color(0xFFFFFFFF)
// Primary text: bright blue-white — everything reads as "blue tinted"
val KhwabTextPrimary    = Color(0xFFE3F2FD)   // Very light blue-white
// Secondary text: mid blue-grey
val KhwabTextSecondary  = Color(0xFF4FC3F7)   // Sky blue for secondary/hint text
val KhwabGrayDark       = Color(0xFF1A2A4A)   // Placeholder / disabled

// ── Voice state accents ──────────────────────────────────
val KhwabListening      = Color(0xFF00E5FF)   // Cyan — listening
val KhwabProcessing     = Color(0xFF00E676)   // Green — thinking
val KhwabSpeaking       = Color(0xFF2979FF)   // Blue — speaking
val KhwabExecuting      = Color(0xFF00BCD4)   // Teal — executing
val KhwabOffline        = Color(0xFF1A2A3A)   // Dark slate — inactive
