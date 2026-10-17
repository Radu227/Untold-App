package com.untold.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untold.android.ui.theme.*

data class Artist(
    val name: String,
    val stage: String,
    val day: Int,
    val isHeadliner: Boolean = false
)

val artistsData = listOf(
    // Mainstage
    Artist("Sting", "Mainstage", 1, true),
    Artist("Kygo", "Mainstage", 1, true),
    Artist("Lewis Capaldi", "Mainstage", 2, true),
    Artist("The Chainsmokers", "Mainstage", 2, true),
    Artist("Martin Garrix", "Mainstage", 3, true),
    Artist("Marshmello", "Mainstage", 3, true),
    Artist("Sebastian Ingrosso", "Mainstage", 4, true),
    Artist("Alan Walker", "Mainstage", 4, true),
    Artist("Zara Larsson", "Mainstage", 1),
    Artist("Flo Rida", "Mainstage", 2),
    Artist("MEDUZA 3 Live", "Mainstage", 3),
    Artist("Solomun", "Mainstage", 4),
    Artist("Tujamo", "Mainstage", 1),
    Artist("R3HAB", "Mainstage", 2),
    Artist("Lost Frequencies", "Mainstage", 3),
    Artist("James Hype", "Mainstage", 4),
    // Galaxy
    Artist("Carl Cox", "Galaxy", 1, true),
    Artist("Joseph Capriati", "Galaxy", 2),
    Artist("Mau P", "Galaxy", 3),
    Artist("Gordo", "Galaxy", 4),
    Artist("Maceo Plex", "Galaxy", 1),
    // Alchemy
    Artist("Denzel Curry", "Alchemy", 1, true),
    Artist("Pendulum", "Alchemy", 2),
    Artist("Gramatik", "Alchemy", 3),
    Artist("Grasu XXL", "Alchemy", 4)
)

@Composable
fun LineupScreen() {
    var selectedDay by remember { mutableStateOf<Int?>(null) } // null means "All Days"
    var selectedStage by remember { mutableStateOf<String?>(null) } // null means "All Stages"

    val filteredArtists = remember(selectedDay, selectedStage) {
        artistsData.filter { artist ->
            (selectedDay == null || artist.day == selectedDay) &&
            (selectedStage == null || artist.stage.equals(selectedStage, ignoreCase = true))
        }
    }

    val stages = listOf("Mainstage", "Galaxy", "Alchemy")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "THE LINEUP",
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayFilterButton(
                    text = "ALL DAYS",
                    isSelected = selectedDay == null,
                    onClick = { selectedDay = null },
                    modifier = Modifier.weight(1.2f)
                )
                (1..4).forEach { day ->
                    DayFilterButton(
                        text = "DAY $day",
                        isSelected = selectedDay == day,
                        onClick = { selectedDay = day },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stage Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StageFilterButton(
                    text = "ALL STAGES",
                    isSelected = selectedStage == null,
                    onClick = { selectedStage = null },
                    modifier = Modifier.weight(1.2f)
                )
                stages.forEach { stage ->
                    StageFilterButton(
                        text = stage.uppercase(),
                        isSelected = selectedStage == stage,
                        onClick = { selectedStage = stage },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of Artists
            if (filteredArtists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No artists found with current filters.",
                        color = UntoldOnSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(
                        items = filteredArtists,
                        key = { "${it.name}_${it.stage}_${it.day}" },
                        span = { artist ->
                            // Headliners span the whole width, others take half
                            if (artist.isHeadliner) {
                                GridItemSpan(2)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { artist ->
                        ArtistCard(artist = artist)
                    }
                }
            }
        }
    }
}

@Composable
fun DayFilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) UntoldNeonPurple else UntoldDarkSurface)
            .border(
                1.dp,
                if (isSelected) UntoldNeonPurple else UntoldOutlineVariant,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else UntoldOnSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun StageFilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) UntoldNeonCyan.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) UntoldNeonCyan else UntoldOutlineVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = text,
            color = if (isSelected) UntoldNeonCyan else UntoldOnSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ArtistCard(artist: Artist) {
    Card(
        colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (artist.isHeadliner) 1.5.dp else 1.dp,
                brush = if (artist.isHeadliner) {
                    Brush.linearGradient(colors = listOf(UntoldNeonCyan, UntoldNeonPurple))
                } else {
                    Brush.linearGradient(colors = listOf(UntoldOutlineVariant, UntoldOutlineVariant))
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(UntoldNeonPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = artist.stage.uppercase(),
                        color = UntoldNeonPurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "DAY ${artist.day}",
                    color = UntoldNeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.name.uppercase(),
                color = Color.White,
                style = if (artist.isHeadliner) {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                letterSpacing = if (artist.isHeadliner) 1.sp else 0.5.sp
            )

            if (artist.isHeadliner) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "HEADLINER",
                    color = UntoldNeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
