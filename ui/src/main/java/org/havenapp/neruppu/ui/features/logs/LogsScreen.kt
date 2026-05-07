package org.havenapp.neruppu.ui.features.logs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    events: List<Event>,
    onClearLogs: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Logs") },
                actions = {
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(events) { event ->
                EventItem(event)
                Divider()
            }
        }
    }
}

@Composable
fun EventItem(event: Event) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = event.sensorType.name, style = MaterialTheme.typography.titleMedium)
            Text(text = formatter.format(event.timestamp), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
        
        event.mediaUri?.let { uriString ->
            Spacer(modifier = Modifier.height(8.dp))
            if (uriString.endsWith(".mp4")) {
                Button(
                    onClick = {
                        Log.d("LogsScreen", "Play button clicked for: $uriString")
                        try {
                            val uri = Uri.parse(uriString)
                            val contentUri = if (uri.scheme == "file") {
                                val file = File(uri.path!!)
                                if (!file.exists()) {
                                    Log.e("LogsScreen", "File does not exist: ${file.absolutePath}")
                                }
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } else {
                                uri
                            }
                            Log.d("LogsScreen", "Playing content URI: $contentUri")
                            val mediaPlayer = MediaPlayer()
                            mediaPlayer.setDataSource(context, contentUri)
                            mediaPlayer.setOnPreparedListener { 
                                Log.d("LogsScreen", "MediaPlayer prepared, starting playback")
                                it.start() 
                            }
                            mediaPlayer.setOnErrorListener { mp, what, extra ->
                                Log.e("LogsScreen", "MediaPlayer error: what=$what, extra=$extra")
                                true
                            }
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnCompletionListener { 
                                Log.d("LogsScreen", "Playback completed")
                                it.release() 
                            }
                        } catch (e: Exception) {
                            Log.e("LogsScreen", "Error setting up MediaPlayer", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play Audio Recording")
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(uriString),
                    contentDescription = "Captured Evidence",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
