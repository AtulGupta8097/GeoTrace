package com.geofencing.tracker.presentation.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VisitsScreen(
    viewModel: VisitsViewModel = hiltViewModel()
) {
    val visits by viewModel.visits.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
        }
    }

    val dateFormat = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    val timeFormat = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    }

    val currentTime = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        items(visits) { visit ->

            val durationMinutes = if (visit.exitTime == null) {
                ((currentTime - visit.entryTime) / 60_000).toInt()
            } else {
                visit.durationMinutes
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {

                    Text(
                        text = visit.geofenceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Entered: ${
                            timeFormat.format(Date(visit.entryTime))
                        }",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )

                    )

                    visit.exitTime?.let {
                        Text(
                            text = "Exited: ${
                                timeFormat.format(Date(it))
                            }",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )

                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (visit.exitTime == null)
                            "Still inside • $durationMinutes min"
                        else
                            "Spent $durationMinutes min",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

