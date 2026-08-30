package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.GlassBorderEnd
import com.example.ui.theme.GlassBorderStart
import com.example.ui.theme.LocalGlassTypography
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.HapticUtils
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassVoiceAssistantDialog(
    cityName: String,
    question: String,
    isListening: Boolean,
    isGeminiAnswering: Boolean,
    geminiAnswer: String?,
    lastAnsweredQuestion: String?,
    onQuestionChange: (String) -> Unit,
    onListeningChange: (Boolean) -> Unit,
    onAskGemini: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val typo = LocalGlassTypography.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var speechError by remember { mutableStateOf<String?>(null) }

    // Speech Intent Launcher
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onListeningChange(false)
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onQuestionChange(spokenText)
                onAskGemini(spokenText)
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            startSpeechRecognition(
                context = context,
                speechIntentLauncher = { speechIntentLauncher.launch(it) },
                onListeningChange = onListeningChange,
                onError = { speechError = it }
            )
        } else {
            speechError = "Microphone permission is needed for speech-to-text."
        }
    }

    // Pulsing Animation for Mic Ring
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("voice_assistant_dialog"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentCyan.copy(alpha = 0.25f), AccentAmber.copy(alpha = 0.25f))
                                    )
                                )
                                .border(1.dp, AccentCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Gemini Sky Intelligence",
                                color = TextPrimary,
                                style = typo.conditionHeadline.copy(fontSize = (17f * min(typo.fontScale, 1.25f)).sp)
                            )
                            Text(
                                text = "Voice Q&A for $cityName",
                                color = TextMuted,
                                style = typo.subText.copy(fontSize = (11f * min(typo.fontScale, 1.25f)).sp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticUtils.performTick(view)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .testTag("close_voice_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Microphone Interactive Orb
                Box(
                    modifier = Modifier
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Outer Aura when Listening
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = pulseAlpha))
                        )
                    }

                    // Main Mic Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.linearGradient(listOf(AccentCyan, Color(0xFF00E5FF)))
                                else Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = if (isListening) Brush.linearGradient(listOf(Color.White, AccentCyan))
                                else Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd)),
                                shape = CircleShape
                            )
                            .clickable {
                                HapticUtils.performClick(view)
                                speechError = null
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    startSpeechRecognition(
                                        context = context,
                                        speechIntentLauncher = { speechIntentLauncher.launch(it) },
                                        onListeningChange = onListeningChange,
                                        onError = { speechError = it }
                                    )
                                }
                            }
                            .testTag("mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                            contentDescription = "Tap to speak",
                            tint = if (isListening) Color.Black else AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when {
                        isListening -> "Listening... Speak your weather question"
                        isGeminiAnswering -> "Gemini is analyzing atmospheric data..."
                        else -> "Tap microphone to ask with voice"
                    },
                    color = if (isListening) AccentCyan else if (isGeminiAnswering) AccentAmber else TextSecondary,
                    style = typo.subText.copy(
                        fontSize = (12f * min(typo.fontScale, 1.25f)).sp,
                        fontWeight = if (isListening || isGeminiAnswering) FontWeight.Medium else FontWeight.Normal
                    )
                )

                if (speechError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = speechError ?: "",
                        color = Color(0xFFFF8080),
                        style = typo.subText.copy(fontSize = 11.sp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Voice / Text Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = onQuestionChange,
                        placeholder = {
                            Text(
                                text = "e.g. Will it rain today?",
                                color = TextMuted,
                                style = typo.bodyBriefing.copy(fontSize = 13.sp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_input_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = GlassBorderStart,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                if (question.isNotBlank()) {
                                    HapticUtils.performClick(view)
                                    onAskGemini(question)
                                }
                            }
                        ),
                        textStyle = typo.bodyBriefing.copy(fontSize = 13.5.sp)
                    )

                    GlassCard(
                        cornerRadius = 16.dp,
                        onClick = {
                            keyboardController?.hide()
                            if (question.isNotBlank()) {
                                HapticUtils.performClick(view)
                                onAskGemini(question)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ask_gemini_button")
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isGeminiAnswering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Ask Gemini",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Question Suggestions
                Text(
                    text = "SUGGESTED WEATHER QUESTIONS",
                    color = TextMuted,
                    style = typo.sectionHeader,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val suggestions = listOf(
                    "🌧️ Will it rain today?",
                    "🧥 What should I wear?",
                    "🏃 Best time for a run?",
                    "💨 How windy is it?",
                    "☀️ Is UV dangerous today?",
                    "🌅 When is sunset?"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.forEach { prompt ->
                        val cleanText = prompt.substringAfter(" ")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, GlassBorderStart, RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticUtils.performClick(view)
                                    onQuestionChange(cleanText)
                                    onAskGemini(cleanText)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = prompt,
                                color = TextSecondary,
                                style = typo.badgeText.copy(fontSize = 11.5.sp)
                            )
                        }
                    }
                }

                // AI Response Area
                AnimatedVisibility(
                    visible = isGeminiAnswering || geminiAnswer != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp,
                            tintColor = AccentCyan
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Psychology,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = lastAnsweredQuestion ?: "Gemini Sky Analysis",
                                        color = TextPrimary,
                                        style = typo.metricLabel.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (isGeminiAnswering) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = "Consulting Gemini AI meteorology models...",
                                            color = TextSecondary,
                                            style = typo.bodyBriefing.copy(fontSize = 12.5.sp)
                                        )
                                    }
                                } else if (geminiAnswer != null) {
                                    Text(
                                        text = geminiAnswer,
                                        color = TextPrimary,
                                        style = typo.bodyBriefing.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.testTag("gemini_answer_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startSpeechRecognition(
    context: android.content.Context,
    speechIntentLauncher: (Intent) -> Unit,
    onListeningChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about today's weather...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        onListeningChange(true)
        speechIntentLauncher(intent)
    } catch (e: Exception) {
        onListeningChange(false)
        onError("Speech recognizer is not available on this device. You can type your question above.")
    }
}
