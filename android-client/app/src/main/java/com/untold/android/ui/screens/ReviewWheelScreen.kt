package com.untold.android.ui.screens

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untold.android.data.model.TicketDto
import com.untold.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Prize structure definition
data class AndroidPrize(
    val id: String,
    val label: String,
    val probability: Double, // percentage (0.1, 1.0, 20.0, 50.0, 5.0, 23.9)
    val color: Color,
    val isBigWin: Boolean = false,
    val codePrefix: String
)

val ANDROID_PRIZES = listOf(
    AndroidPrize("ticket_4_days", "Bilet 4 Zile Anul Viitor", 0.1, Color(0xFFFFD700), true, "PASS4D-2027-"),
    AndroidPrize("ticket_1_day", "Bilet 1 Zi Random", 1.0, Color(0xFFFF007F), true, "DAYPASS-2027-"),
    AndroidPrize("discount_10", "10% Reducere Bilet", 20.0, Color(0xFF00E5FF), false, "DISC10-"),
    AndroidPrize("discount_5", "5% Reducere Bilet", 50.0, Color(0xFF00FFA3), false, "DISC5-"),
    AndroidPrize("merch", "Merch UNTOLD", 5.0, Color(0xFF7000FF), true, "MERCH-"),
    AndroidPrize("no_win", "Necâștigător 😢", 23.9, Color(0xFF3a3a4a), false, "TRYAGAIN-")
)

// 10 Visual sectors on the wheel, shuffled to put bad prizes next to great prizes
data class WheelSector(
    val index: Int,
    val prizeId: String,
    val label: String,
    val color: Color
)

val WHEEL_SECTORS = listOf(
    WheelSector(0, "discount_5", "Reducere 5%", Color(0xFF00FFA3)),
    WheelSector(1, "no_win", "Mai încearcă 😢", Color(0xFF1f1f2e)),
    WheelSector(2, "discount_10", "Reducere 10%", Color(0xFF00E5FF)),
    WheelSector(3, "ticket_1_day", "Bilet 1 Zi 🎫", Color(0xFFFF007F)),
    WheelSector(4, "discount_5", "Reducere 5%", Color(0xFF00FFA3)),
    WheelSector(5, "no_win", "Mai încearcă 😢", Color(0xFF1f1f2e)),
    WheelSector(6, "merch", "Merch Untold 👕", Color(0xFF7000FF)),
    WheelSector(7, "discount_5", "Reducere 5%", Color(0xFF00FFA3)),
    WheelSector(8, "ticket_4_days", "Abonament 4Z ⭐", Color(0xFFFFD700)),
    WheelSector(9, "no_win", "Mai încearcă 😢", Color(0xFF1f1f2e))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewWheelScreen(
    tickets: List<TicketDto>,
    userId: Long
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("untold_review_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // 1. Verify eligibility (must have checked-in 4 day pass or VIP)
    val isEligible = remember(tickets) {
        tickets.any { ticket ->
            ticket.checkInCompleted && (
                ticket.ticketType.contains("4_DAYS") ||
                ticket.ticketType.contains("VIP") ||
                ticket.ticketType.contains("BACKSTAGE")
            )
        }
    }

    // State parameters
    var hasSubmittedReview by remember { mutableStateOf(sharedPrefs.getBoolean("review_submitted_$userId", false)) }
    var wonPrizeId by remember { mutableStateOf(sharedPrefs.getString("prize_id_$userId", null)) }
    var wonVoucherCode by remember { mutableStateOf(sharedPrefs.getString("voucher_code_$userId", "")) }

    // Navigation sub-state inside the screen: 'review' -> 'wheel' -> 'result'
    var activeStage by remember {
        mutableStateOf(
            if (!hasSubmittedReview) "review"
            else if (wonPrizeId == null) "wheel"
            else "result"
        )
    }

    // List of selected artists (must be exactly 3)
    var selectedArtists by remember { mutableStateOf(listOf<Artist>()) }
    var searchQuery by remember { mutableStateOf("") }
    var opinionText by remember { mutableStateOf("") }
    var suggestionsText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Wheel spin states
    var isSpinning by remember { mutableStateOf(false) }

    // Confetti / particle celebration after winning
    var showConfetti by remember { mutableStateOf(false) }

    // Filtering artistsData based on user search query
    val filteredArtists = remember(searchQuery) {
        artistsData.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.stage.contains(searchQuery, ignoreCase = true)
        }
    }

    // Animation configuration for Wheel spin
    val spinAnimSpec = tween<Float>(
        durationMillis = 5000,
        easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1f)
    )

    // Check-In and ticket eligibility blocked state
    if (!isEligible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(UntoldNeonPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = UntoldNeonPink,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ACCES RESTRICȚIONAT",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            val eligibilityReason = if (tickets.isEmpty()) {
                "Nu ai niciun bilet asociat contului tău în acest moment."
            } else if (tickets.all { it.ticketType.contains("DAY") }) {
                "Deții doar bilete de o zi. Roata norocului este exclusivă pentru posesorii de abonamente pe 4 zile (General Access / VIP)."
            } else {
                "Trebuie să finalizezi online Check-In-ul pentru abonamentul tău în tab-ul TICKETS pentru a debloca această secțiune."
            }

            Text(
                text = eligibilityReason,
                color = UntoldOnSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "CUM POȚI DEBLOCA ROATA?",
                        color = UntoldNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "• ", color = UntoldNeonPurple, fontWeight = FontWeight.Bold)
                        Text(text = "Cumpără sau asociază un abonament pe 4 zile sau bilet VIP în contul tău.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "• ", color = UntoldNeonPurple, fontWeight = FontWeight.Bold)
                        Text(text = "Mergi în tab-ul TICKETS, completează datele și dă 'Perform Check-In'.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = "• ", color = UntoldNeonPurple, fontWeight = FontWeight.Bold)
                        Text(text = "Reîntoarce-te pe această pagină pentru a lăsa un review și a învârti roata!", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    // MAIN CONTENT CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        // Glowing Background gradients
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(300.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonPurple.copy(alpha = 0.08f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Tab header title
            Text(
                text = "REVIEW & ROATA NOROCULUI",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic stages depending on flowState
            when (activeStage) {
                "review" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Ajută-ne să facem UNTOLD mai magic! Spune-ne ce artiști ți-au plăcut cel mai mult și deblochează o rotire garantată la marea Roată a Premiilor.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // 1. SELECT TOP 3 ARTISTS
                        Text(
                            text = "1. SELECTEAZĂ CEI MAI FAINI 3 ARTIȘTI",
                            color = UntoldNeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Tags of selected artists
                        if (selectedArtists.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedArtists.forEach { artist ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(UntoldNeonPurple.copy(alpha = 0.25f))
                                            .border(1.dp, UntoldNeonPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = artist.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Sterge",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        selectedArtists = selectedArtists.filter { it.name != artist.name }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Search & Filter Dropdown (rendered only if selected size < 3)
                        if (selectedArtists.size < 3) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Caută artist preferat...", color = Color.White.copy(alpha = 0.4f)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UntoldNeonPurple,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = UntoldNeonPurple,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // Quick suggestion grid
                            Text(
                                text = "ALEGE DIN LISTĂ:",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // We can use a simplified Grid with items since inside Scroll, we set fixed height
                            Box(modifier = Modifier.height(180.dp).fillMaxWidth().padding(bottom = 16.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredArtists) { artist ->
                                        val isSelected = selectedArtists.any { it.name == artist.name }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) UntoldNeonPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.02f))
                                                .border(1.dp, if (isSelected) UntoldNeonPurple else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedArtists = selectedArtists.filter { it.name != artist.name }
                                                    } else if (selectedArtists.size < 3) {
                                                        selectedArtists = selectedArtists + artist
                                                    }
                                                }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Column {
                                                Text(
                                                    text = artist.name,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = artist.stage,
                                                    color = UntoldOnSurfaceVariant,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. OPINION TEXT AREA
                        Text(
                            text = "2. CARE ESTE PĂREREA TA DESPRE FESTIVAL?",
                            color = UntoldNeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = opinionText,
                            onValueChange = { opinionText = it },
                            placeholder = { Text("Atmosfera a fost extraordinară, timpul de așteptare la bar scurt...", color = Color.White.copy(alpha = 0.25f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UntoldNeonPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // 3. SUGGESTIONS
                        Text(
                            text = "3. CE SUGESTII DE ÎMBUNĂTĂȚIRE AI?",
                            color = UntoldNeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = suggestionsText,
                            onValueChange = { suggestionsText = it },
                            placeholder = { Text("Mai multe puncte de apă gratuită, extinderea zonelor de lounge...", color = Color.White.copy(alpha = 0.25f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UntoldNeonPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Errors indicator
                        if (errorText != null) {
                            Text(
                                text = errorText!!,
                                color = UntoldNeonPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (selectedArtists.size != 3) {
                                    errorText = "Te rugăm să alegi exact 3 artiști preferați."
                                } else if (opinionText.isBlank()) {
                                    errorText = "Te rugăm să completezi opinia despre festival."
                                } else if (suggestionsText.isBlank()) {
                                    errorText = "Te rugăm să completezi câteva sugestii de îmbunătățire."
                                } else {
                                    errorText = null
                                    isSubmitting = true
                                    scope.launch {
                                        delay(1200) // Mock API saving
                                        isSubmitting = false
                                        hasSubmittedReview = true
                                        sharedPrefs.edit().putBoolean("review_submitted_$userId", true).apply()
                                        activeStage = "wheel"
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(top = 12.dp, bottom = 24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("TRIMITE REVIEW ȘI DEBLOCHEAZĂ ROATA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                "wheel" -> {
                    // Wheel spin logic
                    val rotationAnim = remember { Animatable(0f) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REVIEW TRIMIS CU SUCCES! ROATA ESTE DEBLOCATĂ.", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // THE WHEEL DESIGN
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(140.dp))
                                .border(4.dp, UntoldNeonCyan.copy(alpha = 0.6f), RoundedCornerShape(140.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Top Pointer Triangle overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-8).dp)
                                    .size(width = 24.dp, height = 24.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(size.width / 2f, size.height)
                                        lineTo(0f, 0f)
                                        lineTo(size.width, 0f)
                                        close()
                                    }
                                    drawPath(path, color = UntoldNeonCyan)
                                }
                            }

                            // Spinning wheel Canvas
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                val currentRotate = rotationAnim.value
                                rotate(currentRotate) {
                                    val numSectors = 10
                                    val sectorSweep = 36f

                                    for (i in 0 until numSectors) {
                                        val sector = WHEEL_SECTORS[i]
                                        val startAngle = i * sectorSweep - 90f // Offset so sector 0 starts at top/right alignment

                                        // Draw slice
                                        drawArc(
                                            color = sector.color.copy(alpha = 0.25f),
                                            startAngle = startAngle,
                                            sweepAngle = sectorSweep,
                                            useCenter = true
                                        )

                                        // Draw slice outer border highlight
                                        drawArc(
                                            color = sector.color,
                                            startAngle = startAngle,
                                            sweepAngle = sectorSweep,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                        )

                                        // Draw sector label rotated
                                        val textAngle = startAngle + 18f
                                        val textAngleRad = Math.toRadians(textAngle.toDouble())
                                        val r = size.width / 2.8f
                                        val x = size.width / 2f + r * cos(textAngleRad).toFloat()
                                        val y = size.height / 2f + r * sin(textAngleRad).toFloat()

                                        rotate(degrees = textAngle + 90f, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                                            val paint = android.graphics.Paint().apply {
                                                color = if (sector.prizeId == "ticket_4_days") android.graphics.Color.YELLOW else android.graphics.Color.WHITE
                                                textSize = 24f
                                                isFakeBoldText = true
                                                textAlign = android.graphics.Paint.Align.CENTER
                                            }
                                            drawContext.canvas.nativeCanvas.drawText(
                                                sector.label,
                                                x,
                                                y,
                                                paint
                                            )
                                        }
                                    }

                                    // Inner decorative borders
                                    drawCircle(
                                        color = Color.Black,
                                        radius = size.width / 8f
                                    )
                                    drawCircle(
                                        color = UntoldNeonPurple,
                                        radius = size.width / 8f,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                                    )
                                }
                            }

                            // Center static button/cap
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(27.dp))
                                    .background(UntoldDarkSurface)
                                    .border(2.dp, UntoldNeonCyan, RoundedCornerShape(27.dp))
                                    .clickable(!isSpinning) {
                                        isSpinning = true
                                        
                                        // Pick winning prize
                                        val rand = Random.nextDouble() * 100.0
                                        val winner: AndroidPrize = when {
                                            rand < 0.1 -> ANDROID_PRIZES[0] // 0.1% Bilet 4 Zile
                                            rand < 1.1 -> ANDROID_PRIZES[1] // 1.0% Bilet 1 Zi (0.1 + 1.0 = 1.1)
                                            rand < 6.1 -> ANDROID_PRIZES[4] // 5.0% Merch (1.1 + 5.0 = 6.1)
                                            rand < 26.1 -> ANDROID_PRIZES[2] // 20.0% Reducere 10% (6.1 + 20.0 = 26.1)
                                            rand < 76.1 -> ANDROID_PRIZES[3] // 50.0% Reducere 5% (26.1 + 50.0 = 76.1)
                                            else -> ANDROID_PRIZES[5] // 23.9% Necastigator
                                        }

                                        // Find sector indexes containing winner prizeId
                                        val sectors = WHEEL_SECTORS.filter { it.prizeId == winner.id }
                                        val chosenSector = sectors.random()
                                        val chosenIndex = chosenSector.index

                                        // Target rotation computation
                                        // Center of chosen wedge: chosenIndex * 36 + 18.
                                        // To land under top pointer (270 degrees):
                                        val targetWedgeAngle = (360f - (chosenIndex * 36f + 18f) + 270f) % 360f
                                        val extraSpins = 8 * 360f // 8 full spins
                                        val totalRotation = extraSpins + targetWedgeAngle

                                        // Start spin audio clicking loop
                                        scope.launch {
                                            val toneGenerator = try { ToneGenerator(AudioManager.STREAM_SYSTEM, 15) } catch (e: Exception) { null }
                                            // Run a simple timer loop to play ticks
                                            for (tick in 1..40) {
                                                if (toneGenerator != null) {
                                                    try {
                                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                                                    } catch (e: Exception) {}
                                                }
                                                // Exponential delay interval as wheel decelerates
                                                val progress = tick / 40f
                                                val delayTime = (40 + progress * progress * 800).toLong()
                                                delay(delayTime)
                                            }
                                        }

                                        scope.launch {
                                            rotationAnim.animateTo(
                                                targetValue = totalRotation,
                                                animationSpec = spinAnimSpec
                                            )

                                            // Animation complete
                                            isSpinning = false
                                            wonPrizeId = winner.id
                                            
                                            val generatedCode = if (winner.id == "no_win") {
                                                ""
                                            } else {
                                                winner.codePrefix + Random.nextInt(100000, 999999).toString()
                                            }
                                            wonVoucherCode = generatedCode

                                            // Save to SharedPreferences
                                            sharedPrefs.edit()
                                                .putString("prize_id_$userId", winner.id)
                                                .putString("voucher_code_$userId", generatedCode)
                                                .apply()

                                            showConfetti = winner.id != "no_win"
                                            activeStage = "result"
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSpinning) "SPIN..." else "SPIN",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Apasă butonul SPIN pentru a-ți asigura premiul!",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "GARANTAT O SINGURĂ ÎNVÂRTIRE",
                            color = UntoldNeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                "result" -> {
                    val prize = remember(wonPrizeId) {
                        ANDROID_PRIZES.find { it.id == wonPrizeId } ?: ANDROID_PRIZES[5]
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(55.dp))
                                .background(
                                    if (prize.id != "no_win") UntoldNeonCyan.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (prize.id != "no_win") Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied,
                                contentDescription = null,
                                tint = if (prize.id != "no_win") UntoldNeonCyan else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (prize.id != "no_win") "FELICITĂRI!" else "MULȚUMIM PENTRU PARTICIPARE!",
                            color = if (prize.id != "no_win") UntoldNeonCyan else Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = prize.label,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )

                        val description = if (prize.id != "no_win") {
                            "Premiul tău a fost securizat! Salvează codul unic de voucher de mai jos pentru a revendica reducerea sau cadoul la e-mailul de suport sau la biroul de Info Point Untold."
                        } else {
                            "De data aceasta nu ai fost extras ca și câștigător. Părerea ta ne ajută enorm să facem ediția următoare de Untold și mai magică!"
                        }

                        Text(
                            text = description,
                            color = UntoldOnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (prize.id != "no_win" && !wonVoucherCode.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, UntoldNeonCyan.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "COD VOUCHER UNIC",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = wonVoucherCode ?: "",
                                        color = UntoldNeonCyan,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "ID Participant: $userId\nSursă: Asymmetric Validation Code",
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
