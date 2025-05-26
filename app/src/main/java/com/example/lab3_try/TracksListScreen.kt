package com.example.lab3_try

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TracksListScreen(
    tracksViewModel: TracksViewModel = viewModel(),
    onTrackSelected: (Track) -> Unit, // Колбек при виборі треку
    onDismiss: () -> Unit // Колбек для закриття (якщо це діалог)
) {
    val tracksList by tracksViewModel.allTracks.observeAsState(initial = emptyList())

    Surface(modifier = Modifier.fillMaxSize()) { // Можна використовувати AlertDialog або інший контейнер
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Збережені треки", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss) {
                    Text("Закрити")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (tracksList.isEmpty()) {
                Text("Немає збережених треків.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tracksList) { track ->
                        TrackItem(
                            track = track,
                            onClick = { onTrackSelected(track) },
                            onDelete = { tracksViewModel.deleteTrack(track) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: Track, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(track.name, style = MaterialTheme.typography.titleMedium)
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            Text(
                "Початок: ${sdf.format(Date(track.startTime))}",
                style = MaterialTheme.typography.bodySmall
            )
            track.endTime?.let {
                Text(
                    "Кінець: ${sdf.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Видалити трек")
        }
    }
}