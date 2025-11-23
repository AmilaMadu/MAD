package com.example.wordgame.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Keep for specific overrides if needed
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Assuming you have these constants defined somewhere accessible (e.g., ViewModel or a constants file)
// const val HINT_COST = 10
// const val MAX_ATTEMPTS = 6 // Or whatever your actual max attempts is
// const val THESAURUS_HINT_MIN_ATTEMPTS_MADE = 2
// const val THESAURUS_HINT_COST = 20


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    playerName: String,
    gameViewModel: GameViewModel = viewModel(),
    onNavigateToLeaderboard: () -> Unit
) {
    // Existing states from ViewModel
    val secretWord = gameViewModel.secretWord
    val isLoading = gameViewModel.isLoading
    val error = gameViewModel.error
    val currentGuess = gameViewModel.currentGuess
    val score = gameViewModel.score
    val attemptsLeft = gameViewModel.attemptsLeft
    val gameStatus = gameViewModel.gameStatus
    val feedbackMessage = gameViewModel.feedbackMessage

    val letterToCheck = gameViewModel.letterToCheck
    val letterOccurrenceMessage = gameViewModel.letterOccurrenceMessage
    val wordLengthHintActive = gameViewModel.wordLengthHintActive
    val wordLengthHintMessage = gameViewModel.wordLengthHintMessage
    val thesaurusHint = gameViewModel.thesaurusHint
    val thesaurusHintMessage = gameViewModel.thesaurusHintMessage
    val thesaurusHintConsumed = gameViewModel.thesaurusHintConsumed
    val isThesaurusHintLoading = gameViewModel.isThesaurusHintLoading
    val currentTimeElapsedString = gameViewModel.currentTimeElapsedString
    val timeTakenSeconds = gameViewModel.timeTakenSeconds

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(playerName) {
        gameViewModel.setPlayerName(playerName)
    }

    LaunchedEffect(error, secretWord, gameStatus) {
        // Your existing logic here
    }

    Column(

        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Use theme background color
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top

    ) {
        Text(
            text = "Word Game Challenge!",
            fontSize = 28.sp, // Slightly larger for emphasis
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary, // Use primary color for the main title
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Player: $playerName",
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyMedium, // Adjusted for consistency
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f) // Slightly subdued
        )
        Spacer(modifier = Modifier.height(16.dp)) // Increased spacing

        // --- MODIFIED Row for Score, Timer, and Attempts in a Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Use surfaceVariant for cards
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: $score",
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Text color for onSurfaceVariant
                    fontWeight = FontWeight.SemiBold
                )
                if (gameStatus == GameStatus.PLAYING || (gameStatus == GameStatus.WON && timeTakenSeconds > 0)) {
                    Text(
                        text = "Time: $currentTimeElapsedString",
                        fontSize = 18.sp,
                        color = if (gameStatus == GameStatus.WON)
                            MaterialTheme.colorScheme.tertiary // Use tertiary for win time, makes it distinct
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "Time: --:--",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Subdued if not active
                    )
                }
                Text(
                    text = "Attempts: $attemptsLeft",
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                    color = if (attemptsLeft < 3 && attemptsLeft > 0) MaterialTheme.colorScheme.error // Highlight low attempts
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (attemptsLeft < 3 && attemptsLeft > 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Increased spacing

        if (isLoading && gameStatus == GameStatus.LOADING_WORD) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) // Color for indicator
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Fetching a new word...",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else if (gameStatus == GameStatus.ERROR && error != null) {
            Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error, // Use theme error color
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { gameViewModel.startNewGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer, // Button bg for error state
                    contentColor = MaterialTheme.colorScheme.onErrorContainer  // Text color on error bg
                )
            ) {
                Text("Try Again")
            }
        } else if (secretWord == null && gameStatus != GameStatus.PLAYING && gameStatus != GameStatus.LOADING_WORD) {
            Text(
                "Loading game...",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { gameViewModel.startNewGame() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) // Button for secondary action
            ) {
                Text("Start Game")
            }
        } else if (secretWord != null || gameStatus == GameStatus.LOST) {

            // --- SECTION for Feature 4: Word Length Hint ---
            if (secretWord != null) {
                val wordDisplayColor = if (wordLengthHintActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                Text(
                    text = if (wordLengthHintActive) "_ ".repeat(secretWord.length).trim() + " (${secretWord.length} letters)" else "??? letters",
                    fontSize = 28.sp, // Larger for better visibility
                    style = MaterialTheme.typography.headlineSmall, // More prominent style
                    letterSpacing = 6.sp, // More spacing for the blanks
                    color = wordDisplayColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                if (!wordLengthHintActive && gameStatus == GameStatus.PLAYING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { gameViewModel.requestWordLengthHint() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E58)) // Pink
                    ) {
                        Text("Show Word Length (Cost: $HINT_COST)")
                    }
                }
                wordLengthHintMessage?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = if (it.startsWith("Not enough")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, // Use secondary for positive hint messages
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else if (gameStatus != GameStatus.LOADING_WORD) {
                Text("Waiting for word...", fontSize = 20.sp, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(16.dp)) // Adjusted spacing
            // --- End of SECTION for Feature 4 ---

            // --- UI for Feature 3: Letter Occurrence ---
            if (gameStatus == GameStatus.PLAYING) {
                Text(
                    "Hint: Check letter occurrence (Cost: ${HINT_COST} points)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f) // Slightly less prominent than main text
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = letterToCheck,
                        onValueChange = { gameViewModel.updateLetterToCheck(it) },
                        label = { Text("Letter") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.None
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            gameViewModel.checkLetterOccurrence()
                            keyboardController?.hide()
                        }),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary, // For the border when focused
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // For the border when unfocused
                            // You can also customize other colors here if needed:
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                            // textColor = ...,
                            // disabledTextColor = ...,
                            // errorIndicatorColor = ...,
                            // ... and many more
                        )

                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            gameViewModel.checkLetterOccurrence()
                            keyboardController?.hide()
                        },
                        enabled = letterToCheck.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) // Consistent hint button color
                    ) {
                        Text("Check")
                    }
                }
                letterOccurrenceMessage?.let {
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary, // Positive hint message
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp)) // Adjusted spacing
            }
            // --- End of UI for Feature 3 ---

            // --- UI for Feature 5: Thesaurus Hint ---
            if (gameStatus == GameStatus.PLAYING) {
                val attemptsMade = MAX_ATTEMPTS - attemptsLeft
                val canOfferThesaurusHintButton = attemptsMade >= THESAURUS_HINT_MIN_ATTEMPTS_MADE && !thesaurusHintConsumed
                val showAvailabilityMessage = attemptsMade < THESAURUS_HINT_MIN_ATTEMPTS_MADE && !thesaurusHintConsumed

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (!thesaurusHintConsumed) {
                        if (canOfferThesaurusHintButton) {
                            Button(
                                onClick = { gameViewModel.requestThesaurusHint() },
                                enabled = !isThesaurusHintLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) // Consistent hint button
                            ) {
                                if (isThesaurusHintLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                } else {
                                    Text("Get Similar Word (Cost: $THESAURUS_HINT_COST)")
                                }
                            }
                        }
                        if (showAvailabilityMessage) {
                            Text(
                                "Similar word hint available after $THESAURUS_HINT_MIN_ATTEMPTS_MADE made guesses.",
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), // Subdued availability message
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    if (thesaurusHintConsumed && thesaurusHint != null) {
                        Text(
                            text = "Hint: A similar word is '${thesaurusHint}'.",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.tertiary, // Tertiary for the revealed hint itself
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    thesaurusHintMessage?.let {
                        if (!(thesaurusHintConsumed && thesaurusHint != null && it.contains(
                                thesaurusHint, ignoreCase = true))) {
                            Text(
                                text = it,
                                fontSize = 16.sp,
                                color = if (it.startsWith("Not enough") || it.startsWith("Error") || it.startsWith("Could not")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = if (thesaurusHintConsumed && thesaurusHint != null) 0.dp else 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp)) // Adjusted spacing
            }
            // --- End of UI for Feature 5 ---

            if (gameStatus == GameStatus.PLAYING) {
                OutlinedTextField(
                    value = currentGuess,
                    onValueChange = { gameViewModel.updateCurrentGuess(it) },
                    label = { Text("Enter your guess") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        gameViewModel.submitGuess()
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        gameViewModel.submitGuess()
                        keyboardController?.hide()
                    },
                    enabled = currentGuess.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Main action button
                ) {
                    Text("Submit Guess", color = MaterialTheme.colorScheme.onPrimary) // Ensure text is visible on primary
                }
            }

            feedbackMessage?.let {
                Spacer(modifier = Modifier.height(20.dp)) // Increased spacing for feedback
                // Using MaterialTheme.colorScheme.primary for WON instead of hardcoded green
                val messageColor = when (gameStatus) {
                    GameStatus.WON -> MaterialTheme.colorScheme.primary // Or consider a specific "success" color from your theme if you define one
                    GameStatus.LOST -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                }
                Text(
                    text = it,
                    fontSize = 18.sp,
                    color = messageColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = if(gameStatus == GameStatus.WON || gameStatus == GameStatus.LOST) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (gameStatus == GameStatus.WON || gameStatus == GameStatus.LOST) {
                Spacer(modifier = Modifier.height(20.dp)) // Spacing before play again
                Button(
                    onClick = { gameViewModel.startNewGame() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (gameStatus == GameStatus.WON) MaterialTheme.colorScheme.secondary // Different color for "Next Level"
                        else MaterialTheme.colorScheme.primaryContainer // Or errorContainer for "Play Again" after loss
                    )
                ) {
                    Text(if (gameStatus == GameStatus.WON) "Play Next Level!" else "Play Again?")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        } // End of the main "else if (secretWord != null || ...)" block

        // Push Leaderboard button to the bottom if content doesn't fill the screen
        if (gameStatus != GameStatus.WON && gameStatus != GameStatus.LOST &&
            (secretWord != null || gameStatus == GameStatus.PLAYING)
        ) {
            Spacer(Modifier.weight(1f, fill = true))
        }

        // --- Leaderboard Button ---
        // Using OutlinedButton for a secondary action look
        OutlinedButton( // Changed to OutlinedButton
            onClick = onNavigateToLeaderboard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (gameStatus == GameStatus.WON || gameStatus == GameStatus.LOST) 8.dp else 16.dp), // Adjust top padding based on context
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary), // Use secondary color for outline
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) // Explicit border
        ) {
            Text("View Leaderboard")
        }
        Spacer(modifier = Modifier.height(8.dp)) // Optional: padding at the very bottom of the screen
    } // End of Column
}
