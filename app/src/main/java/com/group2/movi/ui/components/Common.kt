package com.group2.movi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: String = "📭") {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Pill(text: String, color: Color, textColor: Color = Color.White) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Avatar(url: String?, name: String, sizeDp: Int = 40) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(initials.ifEmpty { "?" }, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
