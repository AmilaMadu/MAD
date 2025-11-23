package com.example.wordgame.ui // Or your preferred package for UI screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents // For top player
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wordgame.data.RemoteLeaderboardEntry
import com.example.wordgame.data.ScoreEntry
import java.util.concurrent.TimeUnit

// A data class to unify local and global entries for the UI
data class DisplayScoreEntry(
    val id: String,
    val playerName: String,
    val score: Int,
    val timeTakenSeconds: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    // THIS IS THE CORRECTED PART
    leaderboardViewModel: LeaderboardViewModel = viewModel(
        factory = ViewModelFactory.create(
            LocalContext.current.applicationContext as Application
        )
    ),
    onNavigateBack: () -> Unit
) {
    // --- Collect ALL states from the ViewModel ---
    val localScores by leaderboardViewModel.scores.collectAsState()
    val isLocalLoading by leaderboardViewModel.isLoading.collectAsState()

    val globalScores by leaderboardViewModel.globalScores.collectAsState()
    val isGlobalLoading by leaderboardViewModel.isGlobalLoading.collectAsState()
    val globalError by leaderboardViewModel.globalError.collectAsState()

    // State for the selected tab (0 for Local, 1 for Global)
    var selectedTabIndex by remember { mutableStateOf(0) }

    // --- BACKGROUND STYLE ---
    val useGradientBackground = false // Set to true for a gradient
    val screenBackgroundModifier = if (useGradientBackground) {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    } else {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    }
    // --- ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = screenBackgroundModifier.padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- NEW: TabRow for Local/Global toggle ---
            LeaderboardTabRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )

            // Conditionally display content based on the selected tab
            when (selectedTabIndex) {
                0 -> // Local Leaderboard
                    LeaderboardContent(
                        isLoading = isLocalLoading,
                        scores = localScores.map { it.toDisplayScoreEntry() },
                        error = null
                    )
                1 -> // Global Leaderboard
                    LeaderboardContent(
                        isLoading = isGlobalLoading,
                        scores = globalScores.map { it.toDisplayScoreEntry() },
                        error = globalError,
                        onRetry = { leaderboardViewModel.fetchGlobalLeaderboard() }
                    )
            }
        }
    }
}

@Composable
fun LeaderboardContent(
    isLoading: Boolean,
    scores: List<DisplayScoreEntry>,
    error: String?,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 64.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (error != null) {
            ErrorState(message = error, onRetry = onRetry)
        } else if (scores.isEmpty()) {
            EmptyLeaderboardState()
        } else {
            LeaderboardList(scores = scores)
        }
    }
}

@Composable
fun LeaderboardTabRow(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Local") }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Global") }
        )
    }
}

@Composable
fun EmptyLeaderboardState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = "No Scores Yet",
            modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Text(
            text = "No scores yet!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Be the first to make it to the leaderboard.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Error",
            modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        Text(
            text = "An Error Occurred",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun LeaderboardList(scores: List<DisplayScoreEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LeaderboardHeader()
        }
        itemsIndexed(scores, key = { _, entry -> entry.id }) { index, entry ->
            ScoreRowCard(rank = index + 1, entry = entry)
        }
    }
}

@Composable
fun LeaderboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Rank", modifier = Modifier.weight(0.15f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Player", modifier = Modifier.weight(0.40f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Score", modifier = Modifier.weight(0.20f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Time", modifier = Modifier.weight(0.25f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ScoreRowCard(rank: Int, entry: DisplayScoreEntry) {
    val isTopPlayer = rank == 1
    val cardColors = if (isTopPlayer) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopPlayer) 4.dp else 2.dp),
        colors = cardColors
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Display
            Box(modifier = Modifier.weight(0.15f).wrapContentWidth(Alignment.Start)) {
                Text(text = "$rank.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isTopPlayer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
            }

            // Player Name and Icon
            Row(modifier = Modifier.weight(0.40f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTopPlayer) {
                    Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = "Top Player", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                } else {
                    Spacer(modifier = Modifier.width(if (rank > 9) 0.dp else 4.dp))
                }
                Text(text = entry.playerName, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isTopPlayer) FontWeight.SemiBold else FontWeight.Normal, color = if (isTopPlayer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Score
            Text(text = "${entry.score}", modifier = Modifier.weight(0.20f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (isTopPlayer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary)

            // Time
            Text(text = formatSecondsToTime(entry.timeTakenSeconds), modifier = Modifier.weight(0.25f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, color = if (isTopPlayer) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSecondsToTime(totalSeconds: Long): String {
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// --- NEW: Mapper functions to convert to a common display type ---
private fun ScoreEntry.toDisplayScoreEntry() = DisplayScoreEntry(
    id = this.id,
    playerName = this.playerName,
    score = this.score,
    timeTakenSeconds = this.timeTakenSeconds
)

private fun RemoteLeaderboardEntry.toDisplayScoreEntry() = DisplayScoreEntry(
    id = this.id ?: System.currentTimeMillis().toString(), // Use a fallback ID if null
    playerName = this.playerName,
    score = this.score,
    timeTakenSeconds = this.timeTakenSeconds
)
