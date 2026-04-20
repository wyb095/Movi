package com.group2.movi.ui.tasks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.group2.movi.domain.model.Message
import com.group2.movi.ui.theme.MoviAccent
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val messages by vm.messages.collectAsState()
    val composer by vm.composer.collectAsState()
    val listState = rememberLazyListState()
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        vm.setAttachment(uri)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
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
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    composer.attachmentUri?.let { uri ->
                        AttachmentPreview(
                            uri = uri,
                            label = composer.attachmentLabel,
                            onSelectLabel = vm::setAttachmentLabel,
                            onRemove = { vm.setAttachment(null) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = composer.draft,
                        onValueChange = vm::setDraft,
                        placeholder = { Text("Type a message or send invoice / item photos…") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        enabled = !composer.sending
                    )
                    composer.success?.let {
                        Text(
                            it,
                            color = MoviAccent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    composer.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.size(6.dp))
                IconButton(
                    onClick = { pickImage.launch("image/*") },
                    enabled = !composer.sending
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Send photo",
                        tint = if (composer.sending) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = vm::send,
                    enabled = !composer.sending && (composer.draft.trim().isNotEmpty() || composer.attachmentUri != null)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (!composer.sending && (composer.draft.trim().isNotEmpty() || composer.attachmentUri != null)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Say hello — messages and photos will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    messages,
                    key = { msg ->
                        msg.messageId.ifBlank {
                            val secs = msg.sentAt?.seconds ?: 0L
                            val nanos = msg.sentAt?.nanoseconds ?: 0
                            "pending-${msg.senderId}-$secs-$nanos-${msg.text.hashCode()}"
                        }
                    }
                ) { msg ->
                    MessageBubble(msg = msg, mine = msg.senderId == vm.currentUid)
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    uri: Uri,
    label: String,
    onSelectLabel: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Photo ready to send", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = label == "Item photo",
                        onClick = { onSelectLabel("Item photo") },
                        label = { Text("Item photo") }
                    )
                    FilterChip(
                        selected = label == "Invoice",
                        onClick = { onSelectLabel("Invoice") },
                        label = { Text("Invoice") }
                    )
                }
            }
            Text(
                "Remove",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onRemove)
            )
        }
        Text(
            "Tap Remove above if you want to change the attachment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun MessageBubble(msg: Message, mine: Boolean) {
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (mine) Color.White else MaterialTheme.colorScheme.onSurface
    val align = if (mine) Alignment.End else Alignment.Start
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!mine) Text(msg.senderName, fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                if (!msg.imageLabel.isNullOrBlank()) {
                    Text(msg.imageLabel, fontSize = 11.sp, color = textColor.copy(alpha = 0.75f))
                }
                msg.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (msg.text.isNotBlank()) {
                    Text(msg.text, color = textColor)
                }
                msg.sentAt?.let {
                    Text(sdf.format(it.toDate()), fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}
