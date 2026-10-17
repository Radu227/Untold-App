package com.untold.android.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.untold.android.data.api.ApiClient
import com.untold.android.data.model.*
import com.untold.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Category Tab definition
data class MapCategory(
    val id: String,
    val labelRo: String,
    val labelEn: String,
    val icon: ImageVector,
    val types: List<String>? = null
)

val mapCategories = listOf(
    MapCategory("ALL", "Toate Zonele", "All Zones", Icons.Default.Explore),
    MapCategory("STAGES", "Scene & Show-uri", "Stages & Shows", Icons.Default.MusicNote, listOf("STAGE", "FANTASIA", "WELLNESS")),
    MapCategory("FOOD_BAR", "Mâncare & Baruri", "Food & Bars", Icons.Default.Restaurant, listOf("FOOD_COURT", "BAR", "CREDIT_POINT", "VENDOR_ACTIVATION")),
    MapCategory("UTILITIES", "Utilități & Porți", "Utilities & Gates", Icons.Default.LocalHospital, listOf("ENTRANCE", "EXIT", "TOILET", "FIRST_AID"))
)

// UI metadata per Density level
data class DensityMeta(
    val labelRo: String,
    val labelEn: String,
    val color: Color,
    val bgColor: Color,
    val borderColor: Color,
    val badgeBg: Color,
    val queueTimeRo: String,
    val queueTimeEn: String,
    val stageStatusRo: String,
    val stageStatusEn: String
)

val DENSITY_METAS = mapOf(
    DensityLevel.LOW to DensityMeta(
        labelRo = "Aglomerație Scăzută",
        labelEn = "Low Crowd",
        color = Color(0xFF10B981), // Emerald Green
        bgColor = Color(0xFF10B981).copy(alpha = 0.15f),
        borderColor = Color(0xFF10B981).copy(alpha = 0.5f),
        badgeBg = Color(0xFF10B981).copy(alpha = 0.25f),
        queueTimeRo = "0 - 5 min așteptare",
        queueTimeEn = "0 - 5 min wait",
        stageStatusRo = "Acces Liber / Rapid",
        stageStatusEn = "Clear / Easy Access"
    ),
    DensityLevel.MODERATE to DensityMeta(
        labelRo = "Aglomerație Moderată",
        labelEn = "Moderate Crowd",
        color = Color(0xFFF59E0B), // Amber Yellow
        bgColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
        borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
        badgeBg = Color(0xFFF59E0B).copy(alpha = 0.25f),
        queueTimeRo = "5 - 15 min așteptare",
        queueTimeEn = "5 - 15 min wait",
        stageStatusRo = "Aglomerație Medie",
        stageStatusEn = "Moderate Crowd"
    ),
    DensityLevel.HIGH to DensityMeta(
        labelRo = "Aglomerație Mare",
        labelEn = "High Surge",
        color = Color(0xFFF97316), // Orange
        bgColor = Color(0xFFF97316).copy(alpha = 0.18f),
        borderColor = Color(0xFFF97316).copy(alpha = 0.6f),
        badgeBg = Color(0xFFF97316).copy(alpha = 0.3f),
        queueTimeRo = "15 - 30 min așteptare",
        queueTimeEn = "15 - 30 min wait",
        stageStatusRo = "Mulțime Desă",
        stageStatusEn = "Dense Crowd"
    ),
    DensityLevel.CRITICAL to DensityMeta(
        labelRo = "Aglomerație Critică",
        labelEn = "Critical Surge",
        color = Color(0xFFEF4444), // Red
        bgColor = Color(0xFFEF4444).copy(alpha = 0.22f),
        borderColor = Color(0xFFEF4444).copy(alpha = 0.8f),
        badgeBg = Color(0xFFEF4444).copy(alpha = 0.35f),
        queueTimeRo = "30+ min așteptare",
        queueTimeEn = "30+ min wait",
        stageStatusRo = "Capacitate Maximă",
        stageStatusEn = "Near Capacity"
    )
)

// Fallback pins identical to Web
val fallbackPins = listOf(
    MapPinDto(101, "STAGE", "Mainstage Arena", "Central arena for world-class headliners", 50f, 35f),
    MapPinDto(102, "STAGE", "Alchemy Stage", "Bass and hip-hop underground stage", 22f, 60f),
    MapPinDto(103, "STAGE", "Daydreaming Stage", "Deep house, techno, and ambient vibe perimeters", 78f, 55f),
    MapPinDto(104, "FOOD_COURT", "Food Court North", "Gourmet food trucks and local transylvanian culinary area", 38f, 72f),
    MapPinDto(105, "BAR", "Cocktail Bar East", "Craft cocktails, cold beer, and soft drinks points", 65f, 42f),
    MapPinDto(106, "ENTRANCE", "Main Entrance Gate A", "Fast track wristband scan and main pedestrian flow entry", 15f, 20f),
    MapPinDto(107, "TOILET", "Central Restrooms", "Sanitation stations, water refills, and ecological restrooms", 48f, 80f),
    MapPinDto(108, "CREDIT_POINT", "Cashless Credit Point", "Wristband top-up kiosk and physical cash refund point", 82f, 25f),
    MapPinDto(109, "FIRST_AID", "First Aid Tent", "24/7 Red Cross medical assistance and emergency tent", 30f, 40f)
)

@Composable
fun VenueMapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiClient.getApiService(context) }

    // State Variables
    var mapLayout by remember { mutableStateOf<MapLayoutDto?>(null) }
    var densityList by remember { mutableStateOf<List<ZoneDensityDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPin by remember { mutableStateOf<MapPinDto?>(null) }
    var showWaitTimes by remember { mutableStateOf(true) }
    var lastRefreshedTime by remember { mutableStateOf(Calendar.getInstance().time) }

    // Emergency alert states
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedEmergencyService by remember { mutableStateOf<String?>(null) }
    var isSendingAlert by remember { mutableStateOf(false) }

    val sendAlert: (String, MapPinDto) -> Unit = { serviceType, pin ->
        scope.launch {
            isSendingAlert = true
            try {
                val response = apiService.sendEmergencyAlert(EmergencyAlertRequest(serviceType, pin.id, pin.name))
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(context, "S-a trimis o alertă către ${if(serviceType == "POLICE") "Poliție" else if(serviceType == "AMBULANCE") "Ambulanță" else "Pompieri"} pentru ${pin.name}!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Eroare: " + (response.body()?.message ?: "Server error"), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Eroare de rețea. Verificați conexiunea!", Toast.LENGTH_LONG).show()
            } finally {
                isSendingAlert = false
            }
        }
    }

    // Map gesture control states
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Functions to fetch data
    val fetchDensityOnly: suspend () -> Unit = {
        isRefreshing = true
        try {
            val response = apiService.getCrowdDensityAggregation(windowMinutes = 60)
            if (response.isSuccessful && response.body()?.success == true) {
                densityList = response.body()!!.data
                lastRefreshedTime = Calendar.getInstance().time
            }
        } catch (e: Exception) {
            // Ignore background refresh errors, keep fallback
        } finally {
            isRefreshing = false
        }
    }

    val fetchData: () -> Unit = {
        scope.launch {
            isLoading = true
            try {
                // Fetch active map layout
                val layoutRes = apiService.getActiveMapLayout()
                if (layoutRes.isSuccessful && layoutRes.body()?.success == true) {
                    mapLayout = layoutRes.body()!!.data
                }
                // Fetch density aggregation
                fetchDensityOnly()
            } catch (e: Exception) {
                Toast.makeText(context, "Mod Offline: Folosim harta implicită", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Initialize & 30s auto-refresh
    LaunchedEffect(Unit) {
        fetchData()
        while (true) {
            delay(30000)
            fetchDensityOnly()
        }
    }

    // Syncing pins list with custom map layout or fallback
    val pins = remember(mapLayout) {
        if (mapLayout != null && mapLayout!!.pins.isNotEmpty()) {
            mapLayout!!.pins
        } else {
            fallbackPins
        }
    }

    // Mapping pin densities
    val densityMap = remember(densityList) {
        densityList.associateBy { it.mapPinId }
    }

    val getPinDensityLevel = { pinId: Long ->
        densityMap[pinId]?.currentLevel ?: DensityLevel.LOW
    }

    // Filter pins
    val filteredPins = remember(pins, activeCategory, searchQuery) {
        pins.filter { pin ->
            // Category filter
            val matchesCategory = if (activeCategory == "ALL") {
                true
            } else {
                val catObj = mapCategories.find { it.id == activeCategory }
                catObj?.types?.contains(pin.type) ?: true
            }

            // Search query filter
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.lowercase(Locale.getDefault())
                pin.name.lowercase(Locale.getDefault()).contains(q) ||
                        pin.type.lowercase(Locale.getDefault()).contains(q) ||
                        (pin.description ?: "").lowercase(Locale.getDefault()).contains(q)
            }

            matchesCategory && matchesSearch
        }
    }

    // Count of high/critical zones
    val highSurgeCount = remember(pins, densityMap) {
        pins.count { getPinDensityLevel(it.id) == DensityLevel.HIGH || getPinDensityLevel(it.id) == DensityLevel.CRITICAL }
    }

    // Infinite transition for heat pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        // Aesthetic ambient glow circles
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(350.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonPurple.copy(alpha = 0.08f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(350.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonCyan.copy(alpha = 0.08f), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HARTĂ INTERACTIVĂ",
                        color = UntoldNeonCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "CROWD HEATMAP",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                // Sync button
                IconButton(
                    onClick = { scope.launch { fetchDensityOnly() } },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = UntoldNeonCyan,
                        modifier = Modifier.graphicsLayer {
                            if (isRefreshing) {
                                rotationZ = (System.currentTimeMillis() % 1000) / 1000f * 360f
                            }
                        }
                    )
                }
            }

            // Quick Stats Bento Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("PUNCTE INTERES", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${pins.size} Zone", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("AGLOMERARE MARE", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("$highSurgeCount Zone", color = if (highSurgeCount > 0) Color(0xFFF97316) else Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("UPDATAT LA", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(lastRefreshedTime),
                            color = UntoldNeonPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Search Bar & Wait Kiosk Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Kiosk
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Caută scenă, bar, poartă...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = UntoldNeonCyan.copy(alpha = 0.5f),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )

                // Toggle wait time badges
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            if (showWaitTimes) UntoldNeonPurple.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (showWaitTimes) UntoldNeonPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showWaitTimes = !showWaitTimes }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Toggle Wait",
                        tint = if (showWaitTimes) UntoldNeonPurple else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Categories list tabs scrollable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapCategories.forEach { cat ->
                    val isSelected = activeCategory == cat.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) UntoldNeonCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) UntoldNeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { activeCategory = cat.id }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.labelRo,
                                tint = if (isSelected) UntoldNeonCyan else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = cat.labelRo.substringBefore(" "), // Shorten
                                color = if (isSelected) UntoldNeonCyan else Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Interactive Map canvas container with Pinch-to-Zoom & Pan
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .background(Color(0xFF03010F))
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                // Transform gestures receiver
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3.5f)
                                offset = if (scale > 1f) {
                                    Offset(
                                        x = (offset.x + pan.x).coerceIn(
                                            -boxWidth.toPx() * (scale - 1) / 2,
                                            boxWidth.toPx() * (scale - 1) / 2
                                        ),
                                        y = (offset.y + pan.y).coerceIn(
                                            -boxHeight.toPx() * (scale - 1) / 2,
                                            boxHeight.toPx() * (scale - 1) / 2
                                        )
                                    )
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                ) {
                    // Container applying scaling and panning transformation Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background image OR beautiful fall-back blueprint canvas
                        if (mapLayout?.imageUrl != null) {
                            val imgUrl = if (mapLayout!!.imageUrl!!.startsWith("/")) {
                                ApiClient.BASE_URL.removeSuffix("/") + mapLayout!!.imageUrl
                            } else {
                                mapLayout!!.imageUrl!!
                            }
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Untold Venue Map",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                            )
                        } else {
                            // Beautiful, Premium Vector Fallback Canvas (Blueprint look)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF131035),
                                                Color(0xFF060417)
                                            )
                                        )
                                    )
                            ) {
                                val widthPx = size.width
                                val heightPx = size.height

                                // Draw grids lines
                                val gridSpacing = 40.dp.toPx()
                                val gridPaintColor = Color(0xFF00F5D4).copy(alpha = 0.05f)

                                var x = 0f
                                while (x < widthPx) {
                                    drawLine(gridPaintColor, Offset(x, 0f), Offset(x, heightPx), strokeWidth = 1f)
                                    x += gridSpacing
                                }
                                var y = 0f
                                while (y < heightPx) {
                                    drawLine(gridPaintColor, Offset(0f, y), Offset(widthPx, y), strokeWidth = 1f)
                                    y += gridSpacing
                                }

                                // Concentric arena perimeters
                                drawCircle(
                                    color = Color(0xFF9D4EDD).copy(alpha = 0.07f),
                                    radius = widthPx * 0.2f,
                                    center = Offset(widthPx * 0.5f, heightPx * 0.45f)
                                )
                                drawCircle(
                                    color = Color(0xFF00F5D4).copy(alpha = 0.05f),
                                    radius = widthPx * 0.32f,
                                    center = Offset(widthPx * 0.5f, heightPx * 0.45f)
                                )
                            }
                        }

                        // Map Blueprint Decorative Logo watermark (only on fallback)
                        if (mapLayout?.imageUrl == null) {
                            Text(
                                text = "UNTOLD FESTIVAL GROUND",
                                color = Color.White.copy(alpha = 0.08f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                            )
                        }

                        // Render PINS on the Map
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                        ) {
                            val mapWidth = maxWidth
                            val mapHeight = maxHeight

                            filteredPins.forEach { pin ->
                                val level = getPinDensityLevel(pin.id)
                                val meta = DENSITY_METAS[level]!!
                                val isSelected = selectedPin?.id == pin.id

                                val pinSize = if (isSelected) 36.dp else 28.dp
                                val xOffset = mapWidth * (pin.xpos / 100f)
                                val yOffset = mapHeight * (pin.ypos / 100f)

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset - pinSize / 2, y = yOffset - pinSize / 2)
                                        .size(pinSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Heatmap Glowing Pulsing Ring (Green/Yellow/Orange/Red)
                                    if (level == DensityLevel.HIGH || level == DensityLevel.CRITICAL) {
                                        Box(
                                            modifier = Modifier
                                                .scale(pulseScale)
                                                .size(pinSize)
                                                .background(meta.color.copy(alpha = pulseAlpha), CircleShape)
                                        )
                                    }

                                    // Outer Pin Ring Ring Circle
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(meta.bgColor, CircleShape)
                                            .border(
                                                if (isSelected) 2.dp else 1.5.dp,
                                                if (isSelected) Color.White else meta.color,
                                                CircleShape
                                            )
                                            .clickable { selectedPin = pin },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getPinIcon(pin.type),
                                            contentDescription = pin.name,
                                            tint = meta.color,
                                            modifier = Modifier.size(if (isSelected) 18.dp else 14.dp)
                                        )
                                    }

                                    // Small mini float wait-time badge above pin
                                    if (showWaitTimes) {
                                        val waitStr = if (listOf("FOOD_COURT", "BAR", "CREDIT_POINT", "TOILET", "ENTRANCE").contains(pin.type)) {
                                            meta.queueTimeRo.substringBefore(" ") // Shorten e.g. "5-15"
                                        } else {
                                            if (level == DensityLevel.LOW) "Liber" else "Aglom"
                                        }

                                        Box(
                                            modifier = Modifier
                                                .offset(y = (-20).dp)
                                                .background(meta.badgeBg, RoundedCornerShape(4.dp))
                                                .border(0.5.dp, meta.borderColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = waitStr,
                                                color = Color.White,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Zoom Reset Controls Floating Indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scale = (scale + 0.2f).coerceAtMost(3.5f) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, "Zoom In", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        "${(scale * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            scale = (scale - 0.2f).coerceAtLeast(1f)
                            if (scale == 1f) offset = Offset.Zero
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, "Zoom Out", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Pin Details Drawer Panel at Bottom (Animated slide in/out)
            AnimatedVisibility(
                visible = selectedPin != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                if (selectedPin != null) {
                    val pin = selectedPin!!
                    val level = getPinDensityLevel(pin.id)
                    val meta = DENSITY_METAS[level]!!
                    val isService = listOf("FOOD_COURT", "BAR", "CREDIT_POINT", "TOILET", "ENTRANCE").contains(pin.type)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF131131), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            // Detail Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(meta.bgColor, CircleShape)
                                            .border(1.dp, meta.borderColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getPinIcon(pin.type),
                                            contentDescription = null,
                                            tint = meta.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pin.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = pin.type,
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { selectedPin = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Close", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Description
                            Text(
                                text = pin.description ?: "Oficial punct de interes pe harta Untold.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Crowd Status Card Bento
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(meta.bgColor, RoundedCornerShape(12.dp))
                                    .border(1.dp, meta.borderColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "STATUS COADĂ LIVE",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(meta.color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .border(0.5.dp, meta.color, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                meta.labelRo.uppercase(Locale.getDefault()),
                                                color = meta.color,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isService) "Timp estimat de așteptare:" else "Densitate perimetru scenă:",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = if (isService) meta.queueTimeRo else meta.stageStatusRo,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Display staff reporting stats if database holds active reports
                                    val density = densityMap[pin.id]
                                    if (density != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = Color.White.copy(alpha = 0.05f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${density.reportCount} rapoarte înregistrate de staff",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 8.sp
                                            )
                                            if (density.lastReportedAt != null) {
                                                Text(
                                                    "Actualizat: ${density.lastReportedAt.substringBefore("T")}", // simplified
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // EMERGENCY ALERT SECTION
                            Text(
                                "SEMNALEAZĂ ALERTA DE URGENȚĂ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Police Button
                                Button(
                                    onClick = {
                                        selectedEmergencyService = "POLICE"
                                        showConfirmationDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF3B82F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Poliție", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Ambulance Button
                                Button(
                                    onClick = {
                                        selectedEmergencyService = "AMBULANCE"
                                        showConfirmationDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalHospital,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Ambulanță", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Firefighters Button
                                Button(
                                    onClick = {
                                        selectedEmergencyService = "FIREFIGHTERS"
                                        showConfirmationDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C).copy(alpha = 0.2f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Whatshot,
                                            contentDescription = null,
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Pompieri", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DENSITY_METAS.forEach { (_, meta) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(meta.color, CircleShape)
                        )
                        Text(
                            meta.labelRo.substringBefore(" "), // Shorten label
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showConfirmationDialog && selectedEmergencyService != null && selectedPin != null) {
        val serviceName = when (selectedEmergencyService) {
            "POLICE" -> "POLIȚIA"
            "AMBULANCE" -> "AMBULANȚA"
            "FIREFIGHTERS" -> "POMPIERII"
            else -> selectedEmergencyService!!
        }
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = "CONFIRMARE ALERTĂ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Ești sigur că dorești să trimiți o alertă de urgență către $serviceName pentru obiectivul \"${selectedPin!!.name}\"?\n\nAceastă acțiune va fi stocată în baza de date și va fi raportată autorităților festivalului.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val svc = selectedEmergencyService!!
                        val pin = selectedPin!!
                        showConfirmationDialog = false
                        sendAlert(svc, pin)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("TRIMITE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text("ANULEAZĂ", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1B4B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Helper: Custom Icons that may not exist in default package (simplified)

// Helper to map category types to native icons
fun getPinIcon(type: String): ImageVector {
    return when (type) {
        "STAGE" -> Icons.Default.MusicNote
        "BAR" -> Icons.Default.Restaurant
        "FOOD_COURT" -> Icons.Default.Restaurant
        "CREDIT_POINT" -> Icons.Default.Payment
        "ENTRANCE" -> Icons.Default.Login
        "TOILET" -> Icons.Default.Wc
        "FIRST_AID" -> Icons.Default.LocalHospital
        else -> Icons.Default.Place
    }
}
