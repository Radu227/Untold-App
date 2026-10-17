package com.untold.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untold.android.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    username: String,
    onNavigateToLineup: () -> Unit,
    onNavigateToTickets: () -> Unit
) {
    // Target date: August 6, 2026 (Untold usually starts around early August)
    var timeLeft by remember { mutableStateOf(calculateTimeLeft()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeLeft = calculateTimeLeft()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        // Blur gradient circles in background
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(400.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonPurple.copy(alpha = 0.1f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Greeting
            Text(
                text = if (username.isNotBlank()) "BINE AI VENIT, $username!" else "WELCOME TO UNTOLD",
                color = UntoldNeonCyan,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "THE WORLD CAPITAL\nOF NIGHT MAGIC",
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Black
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Countdown Timer Panel
            Text(
                text = "COUNTDOWN TO THE MAGIC",
                style = MaterialTheme.typography.labelLarge,
                color = UntoldOnSurfaceVariant,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CountdownUnit(value = timeLeft.days, unit = "DAYS")
                CountdownUnit(value = timeLeft.hours, unit = "HOURS")
                CountdownUnit(value = timeLeft.minutes, unit = "MINS")
                CountdownUnit(value = timeLeft.seconds, unit = "SECS")
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Featured Call-to-Actions (Quick links)
            FeatureCtaCard(
                title = "EXPLORE THE LINEUP",
                subtitle = "Sting, Kygo, Martin Garrix & more",
                icon = Icons.Default.MusicNote,
                color = UntoldNeonPurple,
                onClick = onNavigateToLineup,
                modifier = Modifier.scale(pulseScale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureCtaCard(
                title = "SECURE YOUR TICKETS",
                subtitle = "Buy single day passes or full access",
                icon = Icons.Default.ConfirmationNumber,
                color = UntoldNeonCyan,
                onClick = onNavigateToTickets
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Info Card about the edition
            Card(
                colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, UntoldOutlineVariant, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = UntoldNeonPink,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "6 - 9 AUGUST 2026",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp)
                        )
                        Text(
                            text = "Cluj-Arena, Cluj-Napoca, Romania",
                            color = UntoldOnSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

data class TimeLeft(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

fun calculateTimeLeft(): TimeLeft {
    val targetTime = 1786046400000L // Approx August 6, 2026 12:00:00 GMT
    val diff = targetTime - System.currentTimeMillis()
    if (diff <= 0) return TimeLeft(0, 0, 0, 0)

    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60

    return TimeLeft(days, hours, minutes, seconds)
}

@Composable
fun CountdownUnit(value: Long, unit: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(75.dp)
            .border(1.dp, UntoldOutlineVariant, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d", value),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = unit,
                color = UntoldNeonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCtaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(colors = listOf(color, color.copy(alpha = 0.2f))),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .wrapContentSize(Alignment.Center)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitle,
                    color = UntoldOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = UntoldOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
