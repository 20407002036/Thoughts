package com.example.thoughts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlin.math.roundToInt

private val ProcessingSurfaceLow = Color(0xFFFCF9F3)
private val ProcessingSurfaceHigh = Color(0xFFEAE9DD)
private val ProcessingText = Color(0xFF373831)
private val ProcessingTextVariant = Color(0xFF64655C)
private val ProcessingPrimary = Color(0xFF7B5E53)
private val ProcessingSecondary = Color(0xFF586A45)

@Composable
fun ProcessingScreen(navController: NavHostController, journalViewModel: JournalViewModel) {
    val uploadState by journalViewModel.uploadState.collectAsState()

    LaunchedEffect(uploadState) {
        if (uploadState == AudioUploadState.Uploaded) {
            navController.navigate(Screen.Review.route)
        }
    }

    Scaffold(
        topBar = { MindfulTopAppBar() },
        bottomBar = { MindfulBottomNavigation(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProcessingStatus(uploadState = uploadState)
            Spacer(modifier = Modifier.height(28.dp))
            ProcessingPreview(uploadState = uploadState)
            Spacer(modifier = Modifier.height(22.dp))
            ProcessingProgressAction(
                uploadState = uploadState,
                onRetry = journalViewModel::retryUpload
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    journalViewModel.discardRecording()
                    navController.navigate(Screen.Record.route)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = ProcessingPrimary)
            ) {
                Text(
                    "Cancel Transcription",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ProcessingStatus(uploadState: AudioUploadState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BreathingAudioRing(isFailed = uploadState == AudioUploadState.Failed)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (uploadState == AudioUploadState.Failed) "Processing Paused" else "Processing Audio...",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = ProcessingText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = when (uploadState) {
                    AudioUploadState.Uploading -> "Sending your recording into a quiet space for transcription."
                    AudioUploadState.Processing -> "Taking a moment to shape your words into a journal entry."
                    AudioUploadState.Uploaded -> "Your entry is ready."
                    AudioUploadState.Failed -> "We could not finish this pass. Your recording is still here."
                    else -> "Preparing your recording for transcription."
                },
                modifier = Modifier.padding(horizontal = 18.dp),
                style = MaterialTheme.typography.bodySmall.copy(color = ProcessingTextVariant),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BreathingAudioRing(isFailed: Boolean) {
    val transition = rememberInfiniteTransition(label = "processing-ring")
    val breath by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath-scale"
    )
    val sweep by transition.animateFloat(
        initialValue = 18f,
        targetValue = 378f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring-sweep"
    )
    val ringColor = if (isFailed) MaterialTheme.colorScheme.error else ProcessingPrimary

    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(168.dp)
                .scale(breath)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(138.dp)
                .scale((breath + 0.04f).coerceAtMost(1.16f))
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.07f))
        )
        Canvas(modifier = Modifier.size(152.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = ProcessingSurfaceHigh.copy(alpha = 0.7f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = sweep,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun ProcessingPreview(uploadState: AudioUploadState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(ProcessingSurfaceLow)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "REAL-TIME PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ProcessingTextVariant.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PreviewDot(active = uploadState != AudioUploadState.Failed)
                PreviewDot(active = false)
                PreviewDot(active = false)
            }
        }
        Text(
            text = when (uploadState) {
                AudioUploadState.Failed -> "\"The draft paused before it could finish forming. You can try again when the connection feels steadier.\""
                AudioUploadState.Uploading -> "\"Your voice note is on its way. We are holding the thread gently while it uploads.\""
                else -> "\"Your words are being arranged into a first draft, with the quiet pauses and turns of thought preserved...\""
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = ProcessingText.copy(alpha = 0.9f),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )
        )
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            ShimmerBar(widthFraction = 1f)
            ShimmerBar(widthFraction = 0.72f)
        }
    }
}

@Composable
private fun PreviewDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(if (active) ProcessingSecondary else ProcessingTextVariant.copy(alpha = 0.25f))
    )
}

@Composable
private fun ShimmerBar(widthFraction: Float) {
    val transition = rememberInfiniteTransition(label = "preview-shimmer")
    val shimmer by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview-shimmer-offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(10.dp)
            .clip(CircleShape)
            .background(ProcessingSurfaceHigh.copy(alpha = 0.45f))
            .drawWithCache {
                val shimmerWidth = size.width * 0.45f
                val startX = size.width * shimmer - shimmerWidth
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.58f),
                        Color.Transparent
                    ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + shimmerWidth, size.height)
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush)
                }
            }
    )
}

@Composable
private fun ProcessingProgressAction(uploadState: AudioUploadState, onRetry: () -> Unit) {
    val progress = when (uploadState) {
        AudioUploadState.Uploading -> 0.48f
        AudioUploadState.Processing -> 0.82f
        AudioUploadState.Uploaded -> 1f
        AudioUploadState.Failed -> 1f
        else -> 0.2f
    }

    if (uploadState == AudioUploadState.Failed) {
        TextButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "Try Again",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ProcessingSurfaceHigh.copy(alpha = 0.58f))
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = ProcessingTextVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Review Full Entry",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = ProcessingTextVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Text(
                "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ProcessingTextVariant,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress)
                .height(3.dp)
                .clip(CircleShape)
                .background(ProcessingPrimary)
                .graphicsLayer { alpha = 0.95f }
        )
    }
}
