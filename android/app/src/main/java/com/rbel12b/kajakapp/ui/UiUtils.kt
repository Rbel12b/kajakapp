package com.rbel12b.kajakapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun categoryColor(category: String): Color {
    val lower = category.lowercase()
    return when {
        lower.contains("world") -> Color(0xFFFFC107)
        lower.contains("european") -> Color(0xFF2196F3)
        lower.contains("olympic") -> Color(0xFF9C27B0)
        lower.contains("national") -> Color(0xFF4CAF50)
        else -> Color(0xFF00BCD4)
    }
}

fun positionMedal(pos: String): String = when (pos) {
    "1." -> "🥇"
    "2." -> "🥈"
    "3." -> "🥉"
    "DNS", "DNF", "DSQ" -> "❌"
    "" -> "⏳"
    else -> ""
}

fun formatDate(iso: String): String {
    return try {
        iso.substring(0, 10)
    } catch (_: Exception) {
        iso
    }
}

fun formatDateTime(iso: String): String {
    return try {
        val date = iso.substring(0, 10)
        val time = iso.substring(11, 16)
        "$date $time"
    } catch (_: Exception) {
        iso
    }
}
