package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val title: String,
    val subtitle: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val isDarkBase: Boolean
) {
    SULTAN_GOLD(
        title = "Sultan Gold & Obsidian",
        subtitle = "Signature luxury dark with 24K gold accents",
        primaryColorHex = 0xFFD4AF37,
        accentColorHex = 0xFFFFB300,
        isDarkBase = true
    ),
    MIDNIGHT_AMOLED(
        title = "Midnight OLED",
        subtitle = "Pure #000000 black for maximum battery savings",
        primaryColorHex = 0xFFFACC15,
        accentColorHex = 0xFF38BDF8,
        isDarkBase = true
    ),
    CYBER_NEON(
        title = "Cyberpunk Neon",
        subtitle = "Electric neon cyan with vivid purple highlights",
        primaryColorHex = 0xFF00F0FF,
        accentColorHex = 0xFFA855F7,
        isDarkBase = true
    ),
    EMERALD_ROYALE(
        title = "Emerald Royale",
        subtitle = "Imperial velvet jade and radiant mint green",
        primaryColorHex = 0xFF10B981,
        accentColorHex = 0xFF34D399,
        isDarkBase = true
    ),
    SUNSET_AMBER(
        title = "Sunset Crimson",
        subtitle = "Deep espresso with burning amber and ruby glow",
        primaryColorHex = 0xFFF59E0B,
        accentColorHex = 0xFFF43F5E,
        isDarkBase = true
    ),
    NORDIC_AURORA(
        title = "Nordic Aurora",
        subtitle = "Deep polar night, arctic teal and indigo aura",
        primaryColorHex = 0xFF2DD4BF,
        accentColorHex = 0xFF6366F1,
        isDarkBase = true
    ),
    FROSTED_PEARL(
        title = "Frosted Pearl (Light)",
        subtitle = "Ultra-clean modern daylight with royal sapphire accents",
        primaryColorHex = 0xFF2563EB,
        accentColorHex = 0xFF4F46E5,
        isDarkBase = false
    )
}

enum class AppBackgroundStyle(
    val title: String,
    val description: String
) {
    SOLID(
        title = "Pure Solid",
        description = "Clean, distraction-free solid background"
    ),
    AMBIENT_GLOW(
        title = "Ambient Glow",
        description = "Subtle luxury aura light behind top headers & cards"
    ),
    MESH_GRADIENT(
        title = "Cosmic Gradient",
        description = "Smooth multidimensional flowing gradient"
    ),
    GLASS_ACCENT(
        title = "Glassmorphism",
        description = "Frosted translucent cards with radiant borders"
    )
}
