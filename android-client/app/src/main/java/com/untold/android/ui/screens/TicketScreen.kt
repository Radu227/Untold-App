package com.untold.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.widget.Toast
import com.untold.android.viewmodel.TicketViewModel

data class TicketOffer(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val features: List<String>,
    val isVIP: Boolean = false,
    val isSingleDay: Boolean = false
)

val ticketOffers = listOf(
    TicketOffer(
        id = "GA_4_DAYS_PHASE_5",
        name = "GENERAL ACCESS PASS",
        price = 1155.31,
        description = "4 Days of Magic",
        features = listOf("Access to all standard stages", "General food court access", "Standard entry gates")
    ),
    TicketOffer(
        id = "VIP_4_DAYS_PHASE_5",
        name = "VIP ACCESS PASS",
        price = 2380.83,
        description = "Premium Experience",
        features = listOf("Access to all stages + VIP platforms", "Dedicated premium bars", "Fast-lane entry", "Exclusive dining areas"),
        isVIP = true
    ),
    TicketOffer(
        id = "GA_SINGLE_DAY",
        name = "GA SINGLE DAY PASS",
        price = 544.96,
        description = "1 Day Access Choice",
        features = listOf("Access to all standard stages for 1 selected day", "General food court access"),
        isSingleDay = true
    ),
    TicketOffer(
        id = "VIP_SINGLE_DAY",
        name = "VIP SINGLE DAY PASS",
        price = 835.82,
        description = "VIP Day Experience Choice",
        features = listOf("Access to all stages + VIP platforms for 1 day", "Dedicated premium bars", "Fast-lane entry"),
        isVIP = true,
        isSingleDay = true
    )
)

data class CartItem(
    val offer: TicketOffer,
    val day: Int? = null, // for single day tickets
    val quantity: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    isLoggedIn: Boolean,
    viewModel: TicketViewModel,
    onNavigateToAuth: () -> Unit
) {
    var cartList = remember { mutableStateListOf<CartItem>() }
    var configuringOffer by remember { mutableStateOf<TicketOffer?>(null) }
    var selectedDayForConfig by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UntoldDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TICKETS & SHOP",
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cart Indicator at top
            if (cartList.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = UntoldNeonPurple.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, UntoldNeonPurple, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = UntoldNeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CART SUMMARY (${cartList.sumOf { it.quantity }} items)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val total = cartList.sumOf { (it.day ?: 1) * 0 + it.offer.price * it.quantity }
                            Text(
                                text = "Total: ${String.format("%.2f", total)} lei",
                                color = UntoldNeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        val context = LocalContext.current
                        val uriHandler = LocalUriHandler.current

                        Button(
                            onClick = {
                                if (!isLoggedIn) {
                                    onNavigateToAuth()
                                } else {
                                    Toast.makeText(context, "Preparing Stripe Checkout...", Toast.LENGTH_SHORT).show()
                                    viewModel.initiateCheckout(
                                        cartItems = cartList,
                                        onSuccess = { url ->
                                            cartList.clear()
                                            uriHandler.openUri(url)
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Checkout failed: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isLoggedIn) "BUY" else "LOGIN TO BUY",
                                color = UntoldDarkBackground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Offers List
            ticketOffers.forEach { offer ->
                TicketOfferCard(
                    offer = offer,
                    onAddToCart = {
                        if (offer.isSingleDay) {
                            configuringOffer = offer
                            selectedDayForConfig = 1
                        } else {
                            val existingIndex = cartList.indexOfFirst { it.offer.id == offer.id }
                            if (existingIndex != -1) {
                                val item = cartList[existingIndex]
                                cartList[existingIndex] = item.copy(quantity = item.quantity + 1)
                            } else {
                                cartList.add(CartItem(offer = offer))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Single Day Configurator Modal (Dialog)
        if (configuringOffer != null) {
            val offer = configuringOffer!!
            AlertDialog(
                onDismissRequest = { configuringOffer = null },
                containerColor = UntoldDarkSurface,
                title = {
                    Text(
                        text = "CHOOSE YOUR DAY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = offer.name,
                            color = if (offer.isVIP) UntoldNeonCyan else UntoldNeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..4).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedDayForConfig == d) UntoldNeonPurple else UntoldDarkBackground)
                                        .border(
                                            1.dp,
                                            if (selectedDayForConfig == d) UntoldNeonPurple else UntoldOutlineVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDayForConfig = d }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        text = "$d",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${offer.price} lei",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val existingIndex = cartList.indexOfFirst { it.offer.id == offer.id && it.day == selectedDayForConfig }
                            if (existingIndex != -1) {
                                val item = cartList[existingIndex]
                                cartList[existingIndex] = item.copy(quantity = item.quantity + 1)
                            } else {
                                cartList.add(CartItem(offer = offer, day = selectedDayForConfig))
                            }
                            configuringOffer = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple)
                    ) {
                        Text("ADD TO CART", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { configuringOffer = null }) {
                        Text("CANCEL", color = UntoldOnSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun TicketOfferCard(
    offer: TicketOffer,
    onAddToCart: () -> Unit
) {
    val borderColor = if (offer.isVIP) UntoldNeonCyan else UntoldOutlineVariant
    val accentColor = if (offer.isVIP) UntoldNeonCyan else UntoldNeonPurple

    Card(
        colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = offer.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Black),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = offer.description,
                        color = UntoldOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (offer.isVIP) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(UntoldNeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SELLING FAST",
                            color = UntoldNeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.2f", offer.price),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " lei",
                    color = UntoldOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = UntoldOutlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Features Checklist
            offer.features.forEach { feat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = feat,
                        color = UntoldOnSurface,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                Text(
                    text = "ADD TO CART",
                    color = if (offer.isVIP) UntoldDarkBackground else Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
