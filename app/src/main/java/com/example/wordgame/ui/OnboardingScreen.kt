package com.example.wordgame.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline // Icon for TextField
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.wordgame.R // Assuming your R file is correctly referenced
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper function to create a MediaPlayer instance safely
fun createMediaPlayer(context: Context, resourceId: Int): MediaPlayer? {
    return try {
        MediaPlayer.create(context, resourceId)?.apply {
            isLooping = true // Loop the music
        }
    } catch (e: Exception) {
        // Log error or handle missing resource
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onNameSaved: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // --- Background Music State ---
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    // Initialize MediaPlayer here, but don't start yet if elements are not visible
                }
                Lifecycle.Event.ON_START -> {
                    // Start playing only if not already playing and elements are meant to be visible
                    // Delaying start slightly to sync with animations
                    if (mediaPlayer.value == null) { // Create if not exists
                        mediaPlayer.value = createMediaPlayer(context, R.raw.onboarding_music) // Replace with your music file
                    }
                    coroutineScope.launch {
                        delay(300) // Small delay to sync with element appearance
                        try {
                            mediaPlayer.value?.takeIf { !it.isPlaying }?.start()
                        } catch (e: IllegalStateException) {
                            // MediaPlayer might not be in a valid state
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mediaPlayer.value?.takeIf { it.isPlaying }?.pause()
                    } catch (e: IllegalStateException) { /*Ignore*/ }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mediaPlayer.value?.stop()
                    mediaPlayer.value?.release()
                    mediaPlayer.value = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Ensure media player is released on dispose if not already handled by ON_DESTROY
            mediaPlayer.value?.release()
            mediaPlayer.value = null
        }
    }


    var elementsVisible by remember { mutableStateOf(false) }
    var cardElementsVisible by remember { mutableStateOf(false) } // For staggered effect

    LaunchedEffect(Unit) {
        delay(200) // Initial delay before anything appears
        elementsVisible = true
        delay(300) // Stagger the card appearance
        cardElementsVisible = true
    }

    // --- Dynamic Background Gradient ---
    // You could animate these colors if desired using animateColorAsState
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface
    )

    // --- Logo Animation ---
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_scale_anim"
    )
    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_alpha_anim"
    )


    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors)),
        color = Color.Transparent // Surface color is handled by the background modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp), // Adjusted vertical padding
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.3f)) // Adjust weight for spacing

            // --- Animated App Logo ---
            AnimatedVisibility(
                visible = elementsVisible,
                enter = fadeIn(animationSpec = tween(1000, easing = EaseOutCubic)) +
                        scaleIn(animationSpec = tween(1000, easing = EaseOutBack), initialScale = 0.5f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_word_game_logo), // Make sure you have this
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(140.dp) // Slightly larger
                        .padding(bottom = 20.dp)
                        .scale(logoScale) // Apply pulsing scale
                        .alpha(logoAlpha) // Apply pulsing alpha
                        .shadow(elevation = 8.dp, shape = CircleShape, clip = false), // Add a subtle shadow
                    contentScale = ContentScale.Fit
                )
            }

            // --- Animated Welcome Text ---
            AnimatedVisibility(
                visible = elementsVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = EaseOutCubic)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = EaseOutBack)
                        )
            ) {
                Text(
                    text = "Welcome to Word Quest!", // Changed text slightly for more flair
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp), // Adjusted size
                    fontWeight = FontWeight.ExtraBold, // Bolder
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // --- Animated Subtitle Text ---
            AnimatedVisibility(
                visible = elementsVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 500, easing = EaseOutCubic)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(durationMillis = 800, delayMillis = 500, easing = EaseOutBack)
                        )
            ) {
                Text(
                    text = "Embark on your word adventure!", // Changed text
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            // --- Animated Input Card ---
            val density = LocalDensity.current
            AnimatedVisibility(
                visible = cardElementsVisible,
                enter = slideInVertically {
                    with(density) { +40.dp.roundToPx() } // Slide in from bottom
                } + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 700, delayMillis = 200, easing = EaseOutExpo)
                ) + fadeIn(
                    initialAlpha = 0.3f,
                    animationSpec = tween(durationMillis = 700, delayMillis = 200)
                ),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp)), // More pronounced shadow
                    shape = RoundedCornerShape(24.dp), // More rounded
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) // Slightly more opaque
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.filter { char -> char.isLetterOrDigit() }.take(15) }, // Limit length and filter
                            label = { Text("Enter Your Name") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.PersonOutline,
                                    contentDescription = "Name Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (name.trim().isNotBlank()) {
                                    onNameSaved(name.trim())
                                    keyboardController?.hide()
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), // More rounded TextField
                            colors = TextFieldDefaults.colors( // Corrected line
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Transparent, // Or MaterialTheme.colorScheme.surface for a slight bg
                                unfocusedContainerColor = Color.Transparent, // Or MaterialTheme.colorScheme.surface
                                disabledContainerColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                // You can add other color customizations here if needed:
                                // unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                // textColor = MaterialTheme.colorScheme.onSurface,
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val buttonScale = remember { Animatable(1f) }
                        Button(
                            onClick = {
                                if (name.trim().isNotBlank()) {
                                    coroutineScope.launch { // Animate button press
                                        buttonScale.animateTo(0.9f, animationSpec = tween(100))
                                        buttonScale.animateTo(1f, animationSpec = tween(100))
                                        onNameSaved(name.trim())
                                        keyboardController?.hide()
                                    }
                                }
                            },
                            enabled = name.trim().isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp) // Taller button
                                .scale(buttonScale.value) // Apply animated scale
                                .graphicsLayer { // Add a glow effect on press if desired (more complex)
                                    // shadowElevation = if (buttonScale.value < 1f) 12f else 6f
                                },
                            shape = RoundedCornerShape(16.dp), // More rounded button
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp, disabledElevation = 2.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(ButtonDefaults.IconSize * 1.2f) // Larger icon
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Let's Play!", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.7f)) // Adjust weight for spacing
        }
    }
}

