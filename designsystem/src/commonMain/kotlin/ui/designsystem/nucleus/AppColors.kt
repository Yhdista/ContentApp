package ui.designsystem.nucleus

import androidx.compose.ui.graphics.Color

object AppColors {
    // =========================================================================
    // 🌙 DARK MODE PALETTE (OLED Slate & Electric Cyan - Hevy Dark Style)
    // =========================================================================
    val DarkPrimary = Color(0xFF00E5FF)
    val DarkOnPrimary = Color(0xFF00363D)
    val DarkPrimaryContainer = Color(0xFF004F58)
    val DarkOnPrimaryContainer = Color(0xFF8BF3FF)

    val DarkBackground = Color(0xFF090D16)
    val DarkSurface = Color(0xFF131B2E)
    val DarkSurfaceVariant = Color(0xFF1E293B)
    val DarkOnBackground = Color(0xFFF1F5F9)
    val DarkOnSurface = Color(0xFFF8FAFC)
    val DarkOnSurfaceVariant = Color(0xFF94A3B8)
    val DarkOutline = Color(0xFF334155)
    val DarkOutlineVariant = Color(0xFF1E293B)

    val DarkStatusPending = Color(0xFFFB923C) // Orange
    val DarkOnStatusPending = Color(0xFF451A03)
    val DarkStatusSynced = Color(0xFF4ADE80)  // Green
    val DarkOnStatusSynced = Color(0xFF052E16)
    val DarkStatusError = Color(0xFFF87171)   // Red
    val DarkOnStatusError = Color(0xFF450A0A)

    val DarkPrGold = Color(0xFFFBBF24)
    val DarkPrGoldBg = Color(0xFF451A03)
    val DarkPrGoldBorder = Color(0xFFFBBF24)

    // =========================================================================
    // ☀️ LIGHT MODE PALETTE (Crisp Athletic Modern Style)
    // =========================================================================
    // Brand Primary: Sky 600 - energetická sportovní azurová s perfektním kontrastem 4.6:1
    val LightPrimary = Color(0xFF0284C7)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFE0F2FE) // Sky 100
    val LightOnPrimaryContainer = Color(0xFF0369A1) // Sky 700

    val LightBackground = Color(0xFFF8FAFC)      // Slate 50 - jemný čistý světlý podklad
    val LightSurface = Color(0xFFFFFFFF)         // Čistě bílé karty
    val LightSurfaceVariant = Color(0xFFF1F5F9)  // Slate 100 - vnitřní vstupy a hlavičky tabulek
    val LightOnBackground = Color(0xFF0F172A)    // Slate 900 - maximální čitelnost textu
    val LightOnSurface = Color(0xFF0F172A)       // Slate 900
    val LightOnSurfaceVariant = Color(0xFF64748B)// Slate 500 - sekundární popisky
    val LightOutline = Color(0xFFCBD5E1)         // Slate 300 - ohraničení tlačítek a polí
    val LightOutlineVariant = Color(0xFFE2E8F0)  // Slate 200 - jemné oddělovače karet

    val LightStatusPending = Color(0xFFD97706)   // Amber 600
    val LightOnStatusPending = Color(0xFFFEF3C7) // Amber 100
    val LightStatusSynced = Color(0xFF16A34A)    // Green 600
    val LightOnStatusSynced = Color(0xFFDCFCE7)  // Green 100
    val LightStatusError = Color(0xFFDC2626)     // Red 600
    val LightOnStatusError = Color(0xFFFEE2E2)   // Red 100

    val LightPrGold = Color(0xFFB45309)          // Amber 700 - vysoce kontrastní zlatý text
    val LightPrGoldBg = Color(0xFFFEF3C7)        // Amber 100 - teplý jemný zlatavý podklad
    val LightPrGoldBorder = Color(0xFFF59E0B)    // Amber 500 - zlatý rámeček

    // =========================================================================
    // Zpětná kompatibilita pro stávající reference
    // =========================================================================
    val Primary = DarkPrimary
    val OnPrimary = DarkOnPrimary
    val PrimaryContainer = DarkPrimaryContainer
    val OnPrimaryContainer = DarkOnPrimaryContainer

    val Background = DarkBackground
    val Surface = DarkSurface
    val SurfaceVariant = DarkSurfaceVariant
    val OnBackground = DarkOnBackground
    val OnSurface = DarkOnSurface
    val OnSurfaceVariant = DarkOnSurfaceVariant

    val StatusPending = DarkStatusPending
    val OnStatusPending = DarkOnStatusPending
    val StatusSynced = DarkStatusSynced
    val OnStatusSynced = DarkOnStatusSynced
    val StatusError = DarkStatusError
    val OnStatusError = DarkOnStatusError

    val Outline = DarkOutline
    val OutlineVariant = DarkOutlineVariant
}
