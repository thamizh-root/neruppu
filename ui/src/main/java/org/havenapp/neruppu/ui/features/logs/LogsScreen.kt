package org.havenapp.neruppu.ui.features.logs

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.havenapp.neruppu.core.ui.theme.*
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.ui.R
import org.havenapp.neruppu.ui.components.ScreenHeader
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    onClearLogs: (Boolean) -> Unit
) {
    val pagingItems = viewModel.events.collectAsLazyPagingItems()
    val selectedFilter by viewModel.filter.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // Header
        ScreenHeader(title = "Events") {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.DeleteSweep, 
                    contentDescription = "Clear all", 
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = { deleteFiles ->
                    onClearLogs(deleteFiles)
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventTag("All", active = selectedFilter == "All", onClick = { viewModel.setFilter("All") })
            EventTag("Motion", active = selectedFilter == "Motion", onClick = { viewModel.setFilter("Motion") })
            EventTag("Sound", active = selectedFilter == "Sound", onClick = { viewModel.setFilter("Sound") })
            EventTag("Light", active = selectedFilter == "Light", onClick = { viewModel.setFilter("Light") })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(pagingItems.itemCount) { index ->
                pagingItems[index]?.let { event ->
                    EventItem(event)
                }
            }

            pagingItems.apply {
                if (loadState.refresh is LoadState.Loading) {
                    item {
                        Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = NeruppuOrange)
                        }
                    }
                } else if (itemCount == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.neruppu_brand_logo),
                                contentDescription = null,
                                tint = BorderTertiary,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No events recorded",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "When sensors are triggered during monitoring, events will be listed here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventTag(text: String, active: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) NeruppuOrange else Color.Transparent,
        border = if (active) null else BorderStroke(1.dp, BorderTertiary),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                color = if (active) Color.White else TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EventItem(event: Event) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
    val (color, icon) = when (event.sensorType) {
        SensorType.CAMERA_MOTION -> NeruppuOrange to Icons.Default.CameraAlt
        SensorType.MICROPHONE -> NeruppuBlue to Icons.Default.Mic
        SensorType.LIGHT -> NeruppuAmber to Icons.Default.WbSunny
        else -> NeruppuGreen to Icons.Default.OpenWith
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundSecondary, RoundedCornerShape(12.dp))
            .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when(event.sensorType) {
                        SensorType.CAMERA_MOTION -> "Motion detected"
                        SensorType.MICROPHONE -> "Loud noise burst"
                        SensorType.LIGHT -> "Light change"
                        else -> event.sensorType.name
                    },
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormatter.format(event.timestamp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (event.mediaUri != null) {
                        Badge("📷 Photo", NeruppuOrangeSoft, NeruppuOrange)
                    } else if (event.audioUri != null) {
                        Badge("🎙 Audio", NeruppuBlueSoft, NeruppuBlue)
                    }
                }
            }
            
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded Content
        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .padding(top = 4.dp)
            ) {
                if (event.mediaUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(event.mediaUri),
                        contentDescription = "Event Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (event.audioUri != null) {
                    AudioPlayer(uriString = event.audioUri!!)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (event.sensorType == SensorType.MICROPHONE) {
                    Waveform(color = NeruppuBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                val detailMeta = when(event.sensorType) {
                    SensorType.CAMERA_MOTION -> "High confidence motion detected via CameraX analysis."
                    SensorType.MICROPHONE -> "Sound level exceeded threshold."
                    SensorType.LIGHT -> "Ambient light shifted significantly."
                    else -> "Sensor trigger event."
                }
                Text(detailMeta, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AudioPlayer(uriString: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                currentPosition = mp.currentPosition
            } else if (mp != null && !mp.isPlaying) {
                break
            }
            delay(100)
        }
    }

    DisposableEffect(uriString) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF0F0F0),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    try {
                        if (mediaPlayer == null) {
                            val uri = Uri.parse(uriString)
                            val contentUri = if (uri.scheme == "file") {
                                val file = File(uri.path ?: "")
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                uri
                            }
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(context, contentUri)
                                setOnPreparedListener { mp ->
                                    duration = mp.duration
                                    mp.start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0
                                    it.seekTo(0)
                                }
                                prepareAsync()
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    } catch (e: Exception) {
                        Log.e("LogsScreen", "Error playing audio", e)
                    }
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF555555),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Slider(
                value = if (duration > 0) currentPosition.toFloat() else 0f,
                onValueChange = {
                    currentPosition = it.toInt()
                    mediaPlayer?.seekTo(it.toInt())
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF075E54),
                    activeTrackColor = Color(0xFF075E54),
                    inactiveTrackColor = Color(0xFFCCCCCC)
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatTime(if (isPlaying) currentPosition else duration),
            fontSize = 11.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deleteFiles by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Clear all events?",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This will remove all recorded security events from your log history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteFiles = !deleteFiles }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = deleteFiles,
                        onCheckedChange = { deleteFiles = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeruppuOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Also delete media files from device storage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteFiles) },
                colors = ButtonDefaults.buttonColors(containerColor = NeruppuRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear All", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun LogsScreenPreview() {
    // Note: This preview will not be functional as it doesn't have a real ViewModel
    // but we can fix the compilation by not calling LogsScreen here if needed,
    // or providing a mock/stub. For now, let's just comment it out to unblock build.
}

@Preview(showBackground = true)
@Composable
fun LogsScreenEmptyPreview() {
}

@Preview
@Composable
fun DeleteConfirmationDialogPreview() {
    NeruppuTheme {
        DeleteConfirmationDialog(onConfirm = {}, onDismiss = {})
    }
}

@Composable
fun Badge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun Waveform(color: Color) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(4, 9, 16, 18, 11, 7, 14, 6, 3)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
