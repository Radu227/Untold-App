package com.untold.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.untold.android.ui.screens.*
import com.untold.android.ui.theme.*
import com.untold.android.viewmodel.AuthViewModel
import com.untold.android.viewmodel.TicketViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "HOME", Icons.Default.Home)
    object Lineup : Screen("lineup", "LINEUP", Icons.Default.MusicNote)
    object Tickets : Screen("tickets", "TICKETS", Icons.Default.ConfirmationNumber)
    object VenueMap : Screen("venue_map", "MAP", Icons.Default.Map)
    object Media : Screen("media", "MEDIA", Icons.Default.Image)
    object Portal : Screen("portal", "PORTAL", Icons.Default.Person)
}

data class Track(val title: String, val assetName: String)

val TRACKS = listOf(
    Track("Dimitri Vegas & Mike - Melody", "music/Dimitri Vegas & Like Mike, Steve Aoki vs Ummet Ozcan - Melody.mp3"),
    Track("J Balvin - Mi Gente (Aoki Remix)", "music/J Balvin & Willy William - Mi Gente (Steve Aoki Remix) [Official Music Video].mp3"),
    Track("Marshmello - Alone", "music/Marshmello - Alone (Official Music Video).mp3"),
    Track("Marshmello & Bastille - Happier", "music/Marshmello ft. Bastille - Happier (Official Music Video).mp3"),
    Track("Martin Garrix - In The Name Of Love", "music/Martin Garrix & Bebe Rexha - In The Name Of Love (Official Video).mp3"),
    Track("Martin Garrix ft. Khalid - Ocean", "music/Martin Garrix feat. Khalid - Ocean (Official Video).mp3")
)

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val ticketViewModel: TicketViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            UntoldTheme {
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                // Ticket State and Announcements SSE subscription
                val ticketState by ticketViewModel.ticketState.collectAsState()
                var activeAnnouncement by remember { mutableStateOf<com.untold.android.data.model.Announcement?>(null) }
                var sseConnectionAttempt by remember { mutableStateOf(0) }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        ticketViewModel.loadTickets()
                    }
                }

                DisposableEffect(sseConnectionAttempt, isLoggedIn) {
                    val isDisposed = java.util.concurrent.atomic.AtomicBoolean(false)
                    val client = okhttp3.OkHttpClient.Builder()
                        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build()
                    val request = okhttp3.Request.Builder()
                        .url("http://10.0.2.2:8080/api/stream/announcements")
                        .header("Accept", "text/event-stream")
                        .build()

                    val eventSource = okhttp3.sse.EventSources.createFactory(client).newEventSource(request, object : okhttp3.sse.EventSourceListener() {
                        override fun onOpen(eventSource: okhttp3.sse.EventSource, response: okhttp3.Response) {
                            android.util.Log.i("AnnouncementsSSE", "Attendee connected to announcements SSE")
                        }

                        override fun onEvent(eventSource: okhttp3.sse.EventSource, id: String?, type: String?, data: String) {
                            if (isDisposed.get()) return
                            if (type == "announcement") {
                                this@MainActivity.lifecycleScope.launch {
                                    if (isDisposed.get()) return@launch
                                    try {
                                        val announcement = com.google.gson.Gson().fromJson(data, com.untold.android.data.model.Announcement::class.java)
                                        
                                        // Run validation checks on active tickets
                                        if (isLoggedIn) {
                                            val currentTicketState = ticketViewModel.ticketState.value
                                            if (currentTicketState is com.untold.android.viewmodel.TicketState.Success) {
                                                val checkedInTickets = currentTicketState.tickets.filter { it.checkInCompleted && it.attendee != null }
                                                
                                                val showNotification = when (announcement.targetAudience) {
                                                    "All Festival Attendees" -> {
                                                        checkedInTickets.isNotEmpty()
                                                    }
                                                    "VIP Ticket Holders" -> {
                                                        checkedInTickets.any { it.ticketType.contains("VIP", ignoreCase = true) }
                                                    }
                                                    else -> false // E.g., "Staff & Security Only" is silently ignored
                                                }
                                                
                                                if (showNotification) {
                                                    activeAnnouncement = announcement
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AnnouncementsSSE", "Failed to parse announcement data", e)
                                    }
                                }
                            }
                        }

                        override fun onFailure(eventSource: okhttp3.sse.EventSource, t: Throwable?, response: okhttp3.Response?) {
                            if (isDisposed.get()) return
                            android.util.Log.e("AnnouncementsSSE", "Announcements stream failed, reconnecting...", t)
                            this@MainActivity.lifecycleScope.launch {
                                kotlinx.coroutines.delay(5000L)
                                if (!isDisposed.get()) {
                                    sseConnectionAttempt++
                                }
                            }
                        }

                        override fun onClosed(eventSource: okhttp3.sse.EventSource) {
                            if (isDisposed.get()) return
                            android.util.Log.i("AnnouncementsSSE", "Announcements stream closed, reconnecting...")
                            this@MainActivity.lifecycleScope.launch {
                                kotlinx.coroutines.delay(5000L)
                                if (!isDisposed.get()) {
                                    sseConnectionAttempt++
                                }
                            }
                        }
                    })

                    onDispose {
                        isDisposed.set(true)
                        eventSource.cancel()
                        client.dispatcher.executorService.shutdown()
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    Screen.Home,
                    Screen.Lineup,
                    Screen.Tickets,
                    Screen.VenueMap,
                    Screen.Media,
                    Screen.Portal
                )

                // Global Audio Player States
                val context = LocalContext.current
                val mediaPlayer = remember { android.media.MediaPlayer() }
                var currentTrackIndex by remember { mutableStateOf(0) }
                var isPlaying by remember { mutableStateOf(false) }
                var isExpanded by remember { mutableStateOf(false) }
                var trackProgress by remember { mutableStateOf(0f) }
                var visualizerIntensity by remember { mutableStateOf(0f) }

                // Safe track player function
                val playTrack = remember(context, mediaPlayer) {
                    { index: Int ->
                        try {
                            mediaPlayer.reset()
                            val assetFileDescriptor = context.assets.openFd(TRACKS[index].assetName)
                            mediaPlayer.setDataSource(
                                assetFileDescriptor.fileDescriptor,
                                assetFileDescriptor.startOffset,
                                assetFileDescriptor.length
                            )
                            assetFileDescriptor.close()
                            mediaPlayer.prepare()
                            mediaPlayer.start()
                            isPlaying = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            isPlaying = false
                        }
                    }
                }

                // Handle complete and release
                DisposableEffect(Unit) {
                    mediaPlayer.setOnCompletionListener {
                        val nextIndex = (currentTrackIndex + 1) % TRACKS.size
                        currentTrackIndex = nextIndex
                        playTrack(nextIndex)
                    }
                    onDispose {
                        mediaPlayer.release()
                    }
                }

                // Manage play/pause state change reactive triggers
                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        if (!mediaPlayer.isPlaying) {
                            try {
                                if (mediaPlayer.currentPosition == 0) {
                                    playTrack(currentTrackIndex)
                                } else {
                                    mediaPlayer.start()
                                }
                            } catch (e: Exception) {
                                playTrack(currentTrackIndex)
                            }
                        }
                    } else {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                        }
                    }
                }

                // Manage track selection changes reactive triggers
                LaunchedEffect(currentTrackIndex) {
                    if (isPlaying) {
                        playTrack(currentTrackIndex)
                    }
                }

                // Smoothly update track duration slider and simulate active audio visualizer heights
                LaunchedEffect(isPlaying, currentTrackIndex) {
                    if (isPlaying) {
                        while (true) {
                            kotlinx.coroutines.delay(100)
                            try {
                                val current = mediaPlayer.currentPosition.toFloat()
                                val total = mediaPlayer.duration.toFloat()
                                if (total > 0) {
                                    trackProgress = current / total
                                }
                                visualizerIntensity = 0.2f + kotlin.random.Random.nextFloat() * 0.8f
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    } else {
                        trackProgress = 0f
                        visualizerIntensity = 0f
                    }
                }

                Scaffold(
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UntoldDarkSurface)
                        ) {
                            // Expandable Global Audio Player Bar
                            MusicPlayerWidget(
                                isPlaying = isPlaying,
                                isExpanded = isExpanded,
                                currentTrackIndex = currentTrackIndex,
                                trackProgress = trackProgress,
                                visualizerIntensity = visualizerIntensity,
                                onPlayPauseToggle = { isPlaying = !isPlaying },
                                onExpandToggle = { isExpanded = !isExpanded },
                                onNextTrack = { currentTrackIndex = (currentTrackIndex + 1) % TRACKS.size },
                                onPrevTrack = { currentTrackIndex = if (currentTrackIndex - 1 < 0) TRACKS.size - 1 else currentTrackIndex - 1 },
                                onSelectTrack = { index ->
                                    currentTrackIndex = index
                                    isPlaying = true
                                }
                            )

                            // Navigation Bottom Bar
                            NavigationBar(
                                containerColor = UntoldDarkSurface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.height(72.dp)
                            ) {
                                items.forEach { screen ->
                                    val isSelected = currentDestination?.route == screen.route ||
                                            (screen == Screen.Portal && currentDestination?.route == "auth")

                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = screen.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = UntoldNeonCyan,
                                            selectedTextColor = UntoldNeonCyan,
                                            indicatorColor = UntoldNeonPurple.copy(alpha = 0.2f),
                                            unselectedIconColor = UntoldOnSurfaceVariant,
                                            unselectedTextColor = UntoldOnSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(UntoldDarkBackground)
                                .padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    username = if (isLoggedIn) authViewModel.getUsername() else "",
                                    onNavigateToLineup = { navController.navigate(Screen.Lineup.route) },
                                    onNavigateToTickets = { navController.navigate(Screen.Tickets.route) }
                                )
                            }

                            composable(Screen.Lineup.route) {
                                LineupScreen()
                            }

                            composable(Screen.Tickets.route) {
                                TicketScreen(
                                    isLoggedIn = isLoggedIn,
                                    viewModel = ticketViewModel,
                                    onNavigateToAuth = { navController.navigate("auth") }
                                )
                            }

                            composable(Screen.VenueMap.route) {
                                VenueMapScreen()
                            }

                            composable(Screen.Media.route) {
                                MediaScreen()
                            }

                            composable(Screen.Portal.route) {
                                if (isLoggedIn) {
                                    MyAccountScreen(
                                        viewModel = ticketViewModel,
                                        onNavigateToTickets = { navController.navigate(Screen.Tickets.route) },
                                        onLogout = {
                                            authViewModel.logout()
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(0)
                                            }
                                        }
                                    )
                                } else {
                                    // Redirect to auth screen
                                    LaunchedEffect(Unit) {
                                        navController.navigate("auth") {
                                            popUpTo(Screen.Home.route)
                                        }
                                    }
                                }
                            }

                            composable("auth") {
                                AuthScreen(
                                    viewModel = authViewModel,
                                    onAuthSuccess = {
                                        navController.navigate(Screen.Portal.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    }
                                )
                            }
                        }

                        // SLIDING OVERLAY BANNER WITH COSMIC BRANDING
                        AnimatedVisibility(
                            visible = activeAnnouncement != null,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .zIndex(9999f)
                        ) {
                            if (activeAnnouncement != null) {
                                AttendeeAnnouncementOverlay(
                                    announcement = activeAnnouncement!!,
                                    onDismiss = { activeAnnouncement = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val data: android.net.Uri? = intent?.data
        if (data != null && (data.host == "localhost" || data.host == "127.0.0.1") && data.port == 5173) {
            val path = data.path
            if (path != null) {
                if (path.contains("/payment/success")) {
                    val sessionId = data.getQueryParameter("session_id")
                    if (sessionId != null) {
                        ticketViewModel.confirmPayment(sessionId) { success, error ->
                            runOnUiThread {
                                if (success) {
                                    android.widget.Toast.makeText(
                                        this,
                                        "Plată confirmată cu succes! Brățara / Biletele tale au fost generate.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        this,
                                        "Eroare la confirmarea plății: $error",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                } else if (path.contains("/payment/cancel")) {
                    android.widget.Toast.makeText(
                        this,
                        "Plata a fost anulată.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

@Composable
fun MusicPlayerWidget(
    isPlaying: Boolean,
    isExpanded: Boolean,
    currentTrackIndex: Int,
    trackProgress: Float,
    visualizerIntensity: Float,
    onPlayPauseToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onSelectTrack: (Int) -> Unit
) {
    val activeTrack = TRACKS[currentTrackIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.95f),
                        UntoldDarkSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (isPlaying) UntoldNeonPurple.copy(alpha = visualizerIntensity * 0.4f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    ) {
        if (isExpanded) {
            // Expanded Player Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UNTOLD MAGIC RADIO",
                        color = UntoldNeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Restrânge",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive simulated spectrum visualizer bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Generate 12 premium animated bars reacting to visualizerIntensity
                    for (i in 0 until 12) {
                        val factor = remember(i) { 0.3f + kotlin.random.Random.nextFloat() * 1.2f }
                        val heightMultiplier = if (isPlaying) (visualizerIntensity * factor).coerceIn(0.1f, 1f) else 0.05f
                        val barHeight = (heightMultiplier * 70).dp
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            UntoldNeonPurple,
                                            UntoldNeonCyan
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Track Title and progress slider
                Text(
                    text = activeTrack.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Live Radio Stream Asset",
                    color = UntoldOnSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = trackProgress,
                    color = UntoldNeonCyan,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevTrack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Piesa anterioară",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(UntoldNeonCyan.copy(alpha = 0.15f))
                            .border(2.dp, UntoldNeonCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pauză" else "Redă",
                            tint = UntoldNeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onNextTrack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Piesa următoare",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Full Playlist Selection Area
                Text(
                    text = "PLAYLIST SELECTABIL",
                    color = UntoldOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        itemsIndexed(TRACKS) { index, track ->
                            val isSelected = currentTrackIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) UntoldNeonCyan.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { onSelectTrack(index) }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = track.title,
                                    color = if (isSelected) UntoldNeonCyan else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected && isPlaying) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Redă",
                                        tint = UntoldNeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Collapsed Player Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small local spectrum visualizer icon
                    Row(
                        modifier = Modifier
                            .width(24.dp)
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 3) {
                            val heightFactor = when (i) {
                                0 -> 12 + (visualizerIntensity * 12)
                                1 -> 8 + ((1f - visualizerIntensity) * 16)
                                else -> 14 + (visualizerIntensity * 10)
                            }
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(heightFactor.dp)
                                    .background(if (isPlaying) UntoldNeonPurple else UntoldOnSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = activeTrack.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPlaying) "Se redă acum Untold Magic..." else "Muzica este oprită",
                            color = UntoldOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPlayPauseToggle) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pauză" else "Redă",
                            tint = UntoldNeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Extinde",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendeeAnnouncementOverlay(announcement: com.untold.android.data.model.Announcement, onDismiss: () -> Unit) {
    val isVip = announcement.targetAudience.contains("VIP", ignoreCase = true)
    val isLineup = announcement.title.contains("Lineup", ignoreCase = true)

    val borderBrush = if (isVip) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)) // Gold gradient
        )
    } else if (isLineup) {
        Brush.linearGradient(
            colors = listOf(UntoldNeonCyan, UntoldNeonPurple) // Cosmic blue/purple
        )
    } else {
        Brush.linearGradient(
            colors = listOf(UntoldNeonPink, UntoldNeonPurple) // Hot pink/purple
        )
    }

    val iconColor = if (isVip) Color(0xFFFFD700) else if (isLineup) UntoldNeonCyan else UntoldNeonPink
    val icon = if (isVip) Icons.Default.Stars else if (isLineup) Icons.Default.CalendarToday else Icons.Default.NotificationsActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = androidx.compose.foundation.BorderStroke(2.dp, borderBrush),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF060614).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = announcement.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = announcement.content,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isVip) "EXCLUSIVE VIP BROADCAST ✨" else "FESTIVAL ALERT ⚡",
                    color = iconColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
