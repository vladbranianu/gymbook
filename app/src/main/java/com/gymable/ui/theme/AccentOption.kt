package com.gymable.ui.theme

import androidx.compose.ui.graphics.Color

enum class AccentOption(
    val label: String,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val requiredTier: Int = 0
) {
    MINT_GREEN(
        label             = "Mint Green",
        accent            = Color(0xFF3DDC97),
        onAccent          = Color(0xFF06160F),
        accentContainer   = Color(0xFF123022),
        onAccentContainer = Color(0xFF8DF0C5)
    ),
    LAVENDER_PURPLE(
        label             = "Lavender Purple",
        accent            = Color(0xFFA06CD5),
        onAccent          = Color(0xFF120826),
        accentContainer   = Color(0xFF1E0D38),
        onAccentContainer = Color(0xFFC89EEA)
    ),
    GRAPEFRUIT_PINK(
        label             = "Grapefruit Pink",
        accent            = Color(0xFFFC7375),
        onAccent          = Color(0xFF2E0809),
        accentContainer   = Color(0xFF3D1010),
        onAccentContainer = Color(0xFFFAABB0)
    ),
    BLUE_BELL(
        label             = "Blue Bell",
        accent            = Color(0xFF129FF8),
        onAccent          = Color(0xFF001626),
        accentContainer   = Color(0xFF002D4D),
        onAccentContainer = Color(0xFF7AC8FA)
    ),
    STRAW_GOLD(
        label             = "Straw Gold",
        accent            = Color(0xFFEFD56B),
        onAccent          = Color(0xFF1C1600),
        accentContainer   = Color(0xFF3A2D00),
        onAccentContainer = Color(0xFFF5E4A0)
    ),

    // Tier 1 — Supporter
    BLOOD_ORANGE(
        label             = "Blood Orange",
        accent            = Color(0xFFFF6B35),
        onAccent          = Color(0xFF1A0900),
        accentContainer   = Color(0xFF3D1500),
        onAccentContainer = Color(0xFFFFAD8A),
        requiredTier      = 1
    ),
    ARCTIC_BLUE(
        label             = "Arctic Blue",
        accent            = Color(0xFF00BFFF),
        onAccent          = Color(0xFF001926),
        accentContainer   = Color(0xFF00304D),
        onAccentContainer = Color(0xFF80DFFF),
        requiredTier      = 1
    ),

    // Tier 2 — Gym Rat
    HOT_PINK(
        label             = "Hot Pink",
        accent            = Color(0xFFFF4DA6),
        onAccent          = Color(0xFF220011),
        accentContainer   = Color(0xFF4D0026),
        onAccentContainer = Color(0xFFFF99D3),
        requiredTier      = 2
    ),
    EMERALD(
        label             = "Emerald",
        accent            = Color(0xFF00C853),
        onAccent          = Color(0xFF002010),
        accentContainer   = Color(0xFF004020),
        onAccentContainer = Color(0xFF70FFA8),
        requiredTier      = 2
    ),

    // Tier 3 — Legend
    ROYAL_PURPLE(
        label             = "Royal Purple",
        accent            = Color(0xFF7C3AED),
        onAccent          = Color(0xFF1A0040),
        accentContainer   = Color(0xFF2D0060),
        onAccentContainer = Color(0xFFC4A4FF),
        requiredTier      = 3
    ),
    GOLDEN_AMBER(
        label             = "Golden Amber",
        accent            = Color(0xFFFFB800),
        onAccent          = Color(0xFF1F1400),
        accentContainer   = Color(0xFF3D2800),
        onAccentContainer = Color(0xFFFFD980),
        requiredTier      = 3
    )
}
