package ui.designsystem.nucleus

import androidx.compose.ui.graphics.Color

object AppColors {
    // Brand & Accent (Electric Cyan & Deep Slate)
    val Primary = Color(0xFF00E5FF)
    val OnPrimary = Color(0xFF00363D)
    val PrimaryContainer = Color(0xFF004F58)
    val OnPrimaryContainer = Color(0xFF8BF3FF)

    // Dark Mode Surfaces (OLED Slate)
    val Background = Color(0xFF090D16)
    val Surface = Color(0xFF131B2E)
    val SurfaceVariant = Color(0xFF1E293B)
    val OnBackground = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFFF8FAFC)
    val OnSurfaceVariant = Color(0xFF94A3B8)

    // Light Mode Surfaces
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE2E8F0)
    val LightOnBackground = Color(0xFF0F172A)
    val LightOnSurface = Color(0xFF1E293B)
    val LightOnSurfaceVariant = Color(0xFF64748B)

    // Status Colors
    val StatusPending = Color(0xFFFB923C) // Orange
    val OnStatusPending = Color(0xFF451A03)
    val StatusSynced = Color(0xFF4ADE80)  // Green
    val OnStatusSynced = Color(0xFF052E16)
    val StatusError = Color(0xFFF87171)   // Red
    val OnStatusError = Color(0xFF450A0A)

    // Outlines & Borders (Contrast min 3:1)
    val Outline = Color(0xFF334155)
    val OutlineVariant = Color(0xFF1E293B)
}
