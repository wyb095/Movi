package com.group2.movi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group2.movi.domain.model.TrustBadge

@Composable
fun TrustBadgeDisplay(
    badge: TrustBadge,
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = trustBadgeColor(badge)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            "${badge.name.lowercase().replaceFirstChar { it.uppercase() }} · $score",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun trustBadgeColor(badge: TrustBadge): Color = when (badge) {
    TrustBadge.BRONZE -> Color(0xFF8A5A3B)
    TrustBadge.SILVER -> Color(0xFF60717F)
    TrustBadge.GOLD -> Color(0xFF9D7A00)
    TrustBadge.PLATINUM -> Color(0xFF3C6E71)
}
