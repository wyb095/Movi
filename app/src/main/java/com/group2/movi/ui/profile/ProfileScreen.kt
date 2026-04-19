package com.group2.movi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.group2.movi.ui.theme.MoviAccent
import com.group2.movi.ui.theme.MoviWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSchedule: () -> Unit,
    onEarnings: () -> Unit,
    onReviews: () -> Unit,
    onLogout: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsState()
    val tasks by vm.myCarrierTasks.collectAsState()
    val (computedCompleted, computedEarnings) = vm.computedStats(tasks)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.profilePhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user!!.profilePhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            val initials = (user?.displayName ?: "?")
                                .split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .take(2).joinToString("")
                            Text(initials.ifBlank { "?" }, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.displayName ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = MoviWarning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${"%.1f".format(user?.rating ?: 0.0)} (${user?.totalReviews ?: 0})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(24.dp).clickable(onClick = onEditProfile),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Tasks done", "$computedCompleted", Modifier.weight(1f))
                StatCard("Earnings", "HK$ ${computedEarnings.toInt()}", Modifier.weight(1f))
                StatCard("Rating", "★ ${"%.1f".format(user?.rating ?: 0.0)}", Modifier.weight(1f))
            }

            ProfileRow(Icons.Filled.Schedule, "My commute schedule", onClick = onSchedule)
            ProfileRow(Icons.Filled.Payments, "Earnings", onClick = onEarnings)
            ProfileRow(Icons.Filled.RateReview, "Reviews", onClick = onReviews)
            ProfileRow(Icons.Filled.Logout, "Log out", onClick = {
                vm.logout()
                onLogout()
            }, destructive = true)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MoviAccent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
