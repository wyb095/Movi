package com.group2.movi.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.group2.movi.domain.model.Review
import com.group2.movi.ui.components.EmptyState
import com.group2.movi.ui.theme.MoviWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val reviews by vm.reviews.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (reviews.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                EmptyState(
                    title = "No reviews yet",
                    subtitle = "Complete a task to start collecting reviews.",
                    icon = "⭐"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reviews, key = { it.reviewId.ifBlank { it.createdAt?.seconds.toString() } }) { r ->
                    ReviewCard(r)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(r: Review) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Stars(rating = r.rating)
                Spacer(Modifier.width(8.dp))
                Text(r.reviewerName.ifBlank { "User" }, fontWeight = FontWeight.SemiBold)
            }
            if (r.comment.isNotBlank()) {
                Spacer(Modifier.size(6.dp))
                Text(r.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Stars(rating: Int) {
    Row {
        (1..5).forEach { i ->
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MoviWarning else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
