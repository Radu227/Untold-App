package com.untold.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.untold.android.ui.theme.*

data class GalleryItem(
    val id: Int,
    val imagePath: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen() {
    val context = LocalContext.current
    var selectedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var activeImageId by remember { mutableStateOf<Int?>(null) }

    val galleryItems = remember {
        listOf(
            GalleryItem(1, "file:///android_asset/gallery/1.jpg", "Miezul Nopții la Scena Principală", "Mainstage Midnight", UntoldNeonCyan),
            GalleryItem(2, "file:///android_asset/gallery/2.png", "Extazul", "The Drop", UntoldNeonPurple),
            GalleryItem(3, "file:///android_asset/gallery/3.jpg", "Mare de Stele", "Sea of Stars", UntoldNeonCyan),
            GalleryItem(4, "file:///android_asset/gallery/4.jpg", "Show Lasere", "Laser Show", UntoldNeonPurple),
            GalleryItem(5, "file:///android_asset/gallery/5.jpg", "Răsăritul", "Sunrise Energy", UntoldNeonCyan)
        )
    }

    val faqs = remember {
        listOf(
            Pair("Când se deschid porțile?", "Accesul în tărâm începe zilnic la ora 14:00. Sosește devreme pentru a experimenta transformările din timpul zilei înainte ca energia nocturnă să preia controlul."),
            Pair("Ce este sistemul cashless?", "Festivalul funcționează pe o monedă digitală bazată pe energie. Reîncarcă brățara la Energy Stations desemnate sau prin aplicația oficială. Nu se acceptă numerar în interiorul portalului."),
            Pair("Există stații de încărcare?", "Da, Power Hub-uri speciale sunt amplasate lângă scenele principale pentru a-ți menține dispozitivele încărcate.")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        // Blur gradient background
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(300.dp)
                .background(Brush.radialGradient(colors = listOf(UntoldNeonPurple.copy(alpha = 0.08f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Title Header
            Text(
                text = "AMINTIRI MAGICE",
                color = UntoldNeonCyan,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )
            
            Text(
                text = "Trăiește din nou momentele legendare și simte energia clipelor care au definit experiența Untold.",
                color = UntoldOnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Gallery Grid (Masonry simulation)
            galleryItems.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = if (activeImageId == item.id) item.accentColor else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            activeImageId = if (activeImageId == item.id) null else item.id
                        }
                ) {
                    // Loaded from assets folder via Coil
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Content overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = item.subtitle.uppercase(),
                            color = item.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Pulse indicator on selected image
                    if (activeImageId == item.id) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(item.accentColor.copy(alpha = 0.15f))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Section Header
            Text(
                text = "INFORMAȚII FESTIVAL",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Cluj-Arena Location Bento Card
            Card(
                colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(bottom = 24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDISUGy6dZAS_2NeTmvSNhp1xysMGQm8yKua1V6d8PD-K-3ZoaVgiCBcfu9pl354WKxa-lqIQSn1N5DKtP27g3a83D67mLOjZjaajZng0XYNSLPTtQZVpaK1OKefORGEnqV4p3sBqBz2OzKgXOILAmKcKtKkLxPBNP2Z5y-yn9ez-pN9VXHbNvelg3MiiJbRTDKp3HctxjKANtakHVy3ZCZZs93FTpLqhvxjXWTEqwSP73piwMuNmoWegQ5nzotCBiXvYt5ajBbAVE",
                            contentDescription = "Cluj-Arena",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            UntoldDarkSurface
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = UntoldNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cluj-Arena, Cluj-Napoca",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Epicentrul Magiei",
                            color = UntoldNeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Pregătește-ți călătoria în inima Transilvaniei. Portalul se deschide la Cluj-Arena, transformând orașul într-un tărâm al magiei și al sunetului pentru patru nopți de neuitat.",
                            color = UntoldOnSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to launch Google Maps
                        Button(
                            onClick = {
                                val mapUri = Uri.parse("geo:46.7684,23.5724?q=Cluj-Arena")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    // Fallback to browser
                                    val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=Cluj+Arena")
                                    val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                    context.startActivity(browserIntent)
                                    Toast.makeText(context, "Deschidem în browser...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonCyan.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, UntoldNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "OBȚINE INDICAȚII",
                                    color = UntoldNeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.NavigateNext,
                                    contentDescription = null,
                                    tint = UntoldNeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FAQ Accordion List
            Text(
                text = "ÎNTREBĂRI FRECVENTE",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            faqs.forEachIndexed { index, faq ->
                val isExpanded = selectedFaqIndex == index
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) UntoldNeonPurple.copy(alpha = 0.08f) else UntoldDarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(
                            width = 1.dp,
                            color = if (isExpanded) UntoldNeonPurple.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedFaqIndex = if (isExpanded) null else index
                        }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.first,
                                color = if (isExpanded) UntoldNeonPurple else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isExpanded) "Restrânge" else "Extinde",
                                tint = if (isExpanded) UntoldNeonPurple else UntoldOnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = faq.second,
                                color = UntoldOnSurfaceVariant,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
