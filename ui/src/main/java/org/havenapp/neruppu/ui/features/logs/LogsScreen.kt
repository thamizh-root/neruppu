package org.havenapp.neruppu.ui.features.logs

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.havenapp.neruppu.core.ui.theme.*
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    events: Flow<PagingData<Event>>,
    onClearLogs: () -> Unit
) {
    val pagingItems = events.collectAsLazyPagingItems()
    var selectedFilter by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Events", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onClearLogs, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            // Tags
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                EventTag("All", active = selectedFilter == "All", onClick = { selectedFilter = "All" })
                EventTag("Motion", active = selectedFilter == "Motion", onClick = { selectedFilter = "Motion" })
                EventTag("Sound", active = selectedFilter == "Sound", onClick = { selectedFilter = "Sound" })
                EventTag("Light", active = selectedFilter == "Light", onClick = { selectedFilter = "Light" })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pagingItems.itemCount) { index ->
                    pagingItems[index]?.let { event ->
                        val matchesFilter = when (selectedFilter) {
                            "All" -> true
                            "Motion" -> event.sensorType == SensorType.CAMERA_MOTION
                            "Sound" -> event.sensorType == SensorType.MICROPHONE
                            "Light" -> event.sensorType == SensorType.LIGHT
                            else -> true
                        }

                        if (matchesFilter) {
                            EventItem(event)
                        }
                    }
                }

                pagingItems.apply {
                    if (loadState.refresh is LoadState.Loading) {
                        item { Box(Modifier.fillParentMaxSize(), Alignment.Center) { CircularProgressIndicator() } }
                    }
                }
            }
        }
    }
}

@Composable
fun EventTag(text: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) NeruppuOrangeSoft else BackgroundSecondary)
            .border(0.5.dp, if (active) NeruppuOrangeBorder else BorderTertiary, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = if (active) NeruppuOrange else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EventItem(event: Event) {
    var expanded by remember { mutableStateOf(false) }
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
    val color = when (event.sensorType) {
        SensorType.CAMERA_MOTION -> NeruppuOrange
        SensorType.MICROPHONE -> NeruppuBlue
        SensorType.LIGHT -> NeruppuAmber
        else -> NeruppuGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundSecondary, RoundedCornerShape(12.dp))
            .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // Dot
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(event.sensorType) {
                            SensorType.CAMERA_MOTION -> "Motion detected"
                            SensorType.MICROPHONE -> "Loud noise burst"
                            SensorType.LIGHT -> "Light change"
                            else -> event.sensorType.name
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (event.mediaUri != null) {
                        Badge("📷 Photo", NeruppuOrangeSoft, NeruppuOrange)
                    } else if (event.audioUri != null) {
                        Badge("🎙 Audio", NeruppuBlueSoft, NeruppuBlue)
                    }
                }
                
                val meta = when(event.sensorType) {
                    SensorType.CAMERA_MOTION -> "Camera · ${timeFormatter.format(event.timestamp)} · High confidence"
                    SensorType.MICROPHONE -> "Mic · ${timeFormatter.format(event.timestamp)} · 78 dB peak"
                    SensorType.LIGHT -> "Ambient sensor · ${timeFormatter.format(event.timestamp)} · 40→310 lx"
                    else -> "${event.sensorType.name} · ${timeFormatter.format(event.timestamp)}"
                }
                Text(meta, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))

                if (event.sensorType == SensorType.MICROPHONE && !expanded) {
                    Waveform(color = NeruppuBlue)
                }
            }
        }

        // Expanded Content
        if (expanded) {
            if (event.mediaUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(event.mediaUri),
                    contentDescription = "Event Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentScale = ContentScale.Fit
                )
            }

            if (event.audioUri != null) {
                val audioUri = event.audioUri!!
                Spacer(modifier = Modifier.height(12.dp))
                AudioPlayer(uriString = audioUri)
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
        if (isPlaying) {
            while (isPlaying && mediaPlayer?.isPlaying == true) {
                currentPosition = mediaPlayer?.currentPosition ?: 0
                delay(100)
            }
            if (mediaPlayer?.isPlaying == false) {
                isPlaying = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
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
                                setOnPreparedListener {
                                    duration = it.duration
                                    it.start()
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
fun Badge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
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
