package com.untold.android.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.untold.android.data.model.PersonDto
import com.untold.android.data.model.TicketDto
import com.untold.android.ui.theme.*
import com.untold.android.viewmodel.ActionState
import com.untold.android.viewmodel.TicketState
import com.untold.android.viewmodel.TicketViewModel

@Composable
fun MyAccountScreen(
    viewModel: TicketViewModel,
    onNavigateToTickets: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val ticketState by viewModel.ticketState.collectAsState()
    val checkInState by viewModel.checkInState.collectAsState()
    val topUpState by viewModel.topUpState.collectAsState()

    var activeTab by remember { mutableStateOf("tickets") }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadTickets()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handles redirection to stripe checkout session URL on successful Top Up request
    LaunchedEffect(topUpState) {
        if (topUpState is ActionState.Success) {
            val url = (topUpState as ActionState.Success).payload
            if (!url.isNullOrBlank()) {
                Toast.makeText(context, "Opening secure payment gateway...", Toast.LENGTH_LONG).show()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            viewModel.resetActionStates()
        } else if (topUpState is ActionState.Error) {
            Toast.makeText(context, (topUpState as ActionState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetActionStates()
        }
    }

    // Handles feedback toast on successful check-in
    LaunchedEffect(checkInState) {
        if (checkInState is ActionState.Success) {
            Toast.makeText(context, "Check-in completed successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetActionStates()
        } else if (checkInState is ActionState.Error) {
            Toast.makeText(context, (checkInState as ActionState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetActionStates()
        }
    }

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

            // Screen Header with Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY PORTAL",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )

                IconButton(
                    onClick = onLogout,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = UntoldNeonPink)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body depending on Ticket State
            when (val state = ticketState) {
                is TicketState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = UntoldNeonPurple)
                    }
                }
                is TicketState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = state.message, color = UntoldNeonPink, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadTickets() },
                            colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple)
                        ) {
                            Text("RETRY", color = Color.White)
                        }
                    }
                }
                is TicketState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Tab Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Tickets Tab Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (activeTab == "tickets") UntoldNeonPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, if (activeTab == "tickets") UntoldNeonPurple else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .clickable { activeTab = "tickets" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = if (activeTab == "tickets") UntoldNeonCyan else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "BILETE",
                                        color = if (activeTab == "tickets") Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Review Tab Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (activeTab == "review") UntoldNeonPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, if (activeTab == "review") UntoldNeonPurple else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .clickable { activeTab = "review" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = if (activeTab == "review") UntoldNeonCyan else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ROATĂ",
                                        color = if (activeTab == "review") Color.White else Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeTab == "tickets") {
                            if (state.tickets.isEmpty()) {
                                // Encourage ticket buying
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
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(UntoldNeonPurple.copy(alpha = 0.15f))
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        Icon(
                                            Icons.Default.ConfirmationNumber,
                                            contentDescription = null,
                                            tint = UntoldNeonPurple,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "YOU DON'T HAVE TICKETS YET",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Step into the night magic. Secure your pass to unlock online check-in and cashless top-up features.",
                                        color = UntoldOnSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Button(
                                        onClick = onNavigateToTickets,
                                        colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp).padding(horizontal = 24.dp)
                                    ) {
                                        Text("SECURE ACCESS", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Render Scrollable tickets
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    state.tickets.forEach { ticket ->
                                        TicketManagementCard(
                                            ticket = ticket,
                                            isActionLoading = checkInState is ActionState.Loading || topUpState is ActionState.Loading,
                                            onCheckIn = { details -> viewModel.checkInTicket(ticket.id, details) },
                                            onTopUp = { amount -> viewModel.initiateTopUp(ticket.id, amount) }
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                        } else {
                            ReviewWheelScreen(
                                tickets = state.tickets,
                                userId = viewModel.getUserId()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketManagementCard(
    ticket: TicketDto,
    isActionLoading: Boolean,
    onCheckIn: (PersonDto) -> Unit,
    onTopUp: (Double) -> Unit
) {
    val isCheckedIn = ticket.checkInCompleted
    val borderColor = if (isCheckedIn) UntoldNeonCyan else UntoldNeonPurple

    Card(
        colors = CardDefaults.cardColors(containerColor = UntoldDarkSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column {
            // Card Header Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(UntoldNeonPurple.copy(alpha = 0.2f), UntoldNeonCyan.copy(alpha = 0.15f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${ticket.ticketType.uppercase()} PASS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = ticket.displayName.uppercase(),
                            color = UntoldNeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCheckedIn) UntoldNeonCyan.copy(alpha = 0.15f) else UntoldNeonPink.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isCheckedIn) "ACTIVE PASS" else "CHECK-IN REQ",
                            color = if (isCheckedIn) UntoldNeonCyan else UntoldNeonPink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Card Body content
            if (!isCheckedIn) {
                // STAGE 1: CHECK-IN FORM
                PreCheckInForm(isActionLoading = isActionLoading, onCheckIn = onCheckIn)
            } else {
                // STAGE 2: ACCESS QR & WALLET TOP-UP
                ActivePortalView(ticket = ticket, isActionLoading = isActionLoading, onTopUp = onTopUp)
            }
        }
    }
}

@Composable
fun PreCheckInForm(
    isActionLoading: Boolean,
    onCheckIn: (PersonDto) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var country by remember { mutableStateOf("Romania") }
    var city by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("NATIONAL_ID") }
    var docId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isDataCorrect by remember { mutableStateOf(false) }

    var isDocTypeDropdownExpanded by remember { mutableStateOf(false) }
    val docTypeOptions = listOf(
        "NATIONAL_ID" to "National ID",
        "PASSPORT" to "Passport",
        "DRIVERS_LICENSE" to "Driver's License",
        "OTHER" to "Other"
    )
    val currentDocTypeLabel = docTypeOptions.firstOrNull { it.first == docType }?.second ?: docType

    var isGenderDropdownExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf(
        "Male" to "Male",
        "Female" to "Female",
        "Other" to "Other"
    )
    val currentGenderLabel = genderOptions.firstOrNull { it.first == gender }?.second ?: gender

    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            dob.isNotBlank() &&
            country.isNotBlank() &&
            city.isNotBlank() &&
            docId.isNotBlank() &&
            phone.isNotBlank() &&
            isDataCorrect

    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = "NOMINAL CHECK-IN",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Completează datele de identificare pentru a asocia acest bilet cu actul tău de identitate și a activa brățara cashless.",
            color = UntoldOnSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Row 1: First Name & Last Name
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Birthdate & Gender Dropdown
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Birthdate (YYYY-MM-DD)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.weight(1.2f)
            )

            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = currentGenderLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = {
                        IconButton(onClick = { isGenderDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand gender selection",
                                tint = Color.White
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGenderDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = isGenderDropdownExpanded,
                    onDismissRequest = { isGenderDropdownExpanded = false },
                    modifier = Modifier.background(UntoldDarkSurfaceVariant)
                ) {
                    genderOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = {
                                gender = value
                                isGenderDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Row 3: Doc Type & Doc ID
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = currentDocTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Doc Type") },
                    trailingIcon = {
                        IconButton(onClick = { isDocTypeDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand document type selection",
                                tint = Color.White
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDocTypeDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = isDocTypeDropdownExpanded,
                    onDismissRequest = { isDocTypeDropdownExpanded = false },
                    modifier = Modifier.background(UntoldDarkSurfaceVariant)
                ) {
                    docTypeOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            onClick = {
                                docType = value
                                isDocTypeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = docId,
                onValueChange = { docId = it },
                label = { Text("Document ID") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
        }

        // Row 4: Phone Number
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        // Row 5: Country & City
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = UntoldNeonPurple, unfocusedBorderColor = UntoldOutlineVariant),
                singleLine = true,
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Confirmation Checkbox
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isDataCorrect,
                onCheckedChange = { isDataCorrect = it },
                colors = CheckboxDefaults.colors(checkedColor = UntoldNeonPurple)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Declar pe propria răspundere că datele introduse sunt reale și corecte. Înțeleg că neconcordanța acestora cu actul de identitate fizic prezentat la porți va duce la refuzul accesului.",
                color = UntoldOnSurface,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isFormValid) return@Button
                onCheckIn(
                    PersonDto(
                        firstName = firstName,
                        lastName = lastName,
                        dateOfBirth = dob,
                        gender = gender,
                        country = country,
                        city = city,
                        documentType = docType,
                        documentId = docId,
                        phoneNumber = phone
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonPurple),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isActionLoading && isFormValid
        ) {
            if (isActionLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("COMPLETE ONLINE CHECK-IN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ActivePortalView(
    ticket: TicketDto,
    isActionLoading: Boolean,
    onTopUp: (Double) -> Unit
) {
    var topUpAmountStr by remember { mutableStateOf("50") }

    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(UntoldNeonCyan.copy(alpha = 0.1f))
                .border(1.dp, UntoldNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = UntoldNeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Check-in finalizat cu succes!",
                        color = UntoldNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Datele participantului au fost asociate în siguranță cu acest bilet.",
                        color = UntoldOnSurfaceVariant,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Attendee Details Card (Aligned with Web frontend details)
        Card(
            colors = CardDefaults.cardColors(containerColor = UntoldDarkSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, UntoldOutlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Attendee",
                        tint = UntoldNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PARTICIPANT ÎNREGISTRAT",
                        color = UntoldNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${ticket.attendee?.firstName?.uppercase()} ${ticket.attendee?.lastName?.uppercase()}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = UntoldOutlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "📅 Născut la:", color = UntoldOnSurfaceVariant, fontSize = 11.sp)
                        Text(text = ticket.attendee?.dateOfBirth ?: "", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "📞 Telefon:", color = UntoldOnSurfaceVariant, fontSize = 11.sp)
                        Text(text = ticket.attendee?.phoneNumber ?: "", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "📍 Locație:", color = UntoldOnSurfaceVariant, fontSize = 11.sp)
                        Text(text = "${ticket.attendee?.city?.uppercase()}, ${ticket.attendee?.country?.uppercase()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "💳 Document:", color = UntoldOnSurfaceVariant, fontSize = 11.sp)
                        Text(text = "${ticket.attendee?.documentType} - ${ticket.attendee?.documentId}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "👤 Gen:", color = UntoldOnSurfaceVariant, fontSize = 11.sp)
                        Text(text = ticket.attendee?.gender ?: "", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QR Code & Identification section
        Text(
            text = "COD DE ACCES SMART PASS",
            color = UntoldOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockSize = size.width / 5
                // Outer corners
                drawRect(Color.Black, size = androidx.compose.ui.geometry.Size(blockSize * 1.5f, blockSize * 1.5f))
                drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 0.3f, blockSize * 0.3f), size = androidx.compose.ui.geometry.Size(blockSize * 0.9f, blockSize * 0.9f))
                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 0.5f, blockSize * 0.35f), size = androidx.compose.ui.geometry.Size(blockSize * 0.5f, blockSize * 0.5f))

                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size.width - blockSize * 1.5f, 0f), size = androidx.compose.ui.geometry.Size(blockSize * 1.5f, blockSize * 1.5f))
                drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(size.width - blockSize * 1.2f, blockSize * 0.3f), size = androidx.compose.ui.geometry.Size(blockSize * 0.9f, blockSize * 0.9f))

                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - blockSize * 1.5f), size = androidx.compose.ui.geometry.Size(blockSize * 1.5f, blockSize * 1.5f))

                // Random noise dots in center/bottom right
                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 2f, blockSize * 2f), size = androidx.compose.ui.geometry.Size(blockSize * 0.8f, blockSize * 0.5f))
                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 3f, blockSize * 2.5f), size = androidx.compose.ui.geometry.Size(blockSize * 0.5f, blockSize * 0.8f))
                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 4f, blockSize * 3.5f), size = androidx.compose.ui.geometry.Size(blockSize * 0.7f, blockSize * 0.7f))
                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(blockSize * 2.5f, blockSize * 4f), size = androidx.compose.ui.geometry.Size(blockSize * 1f, blockSize * 0.4f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // RFID Monospace Tag Badge
        val rfidTagText = ticket.bracelet?.rfidTag ?: ticket.rfidTag ?: "NOT_GENERATED"
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rfidTagText,
                color = UntoldNeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = UntoldOutlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))

        // RFID WRISTBAND CARD (CASHLESS PORTFOLIO)
        Card(
            colors = CardDefaults.cardColors(containerColor = UntoldDarkSurfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.1f), UntoldNeonCyan.copy(alpha = 0.2f))),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = UntoldNeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "PORTOFEL CASHLESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        text = "RFID ACTIV",
                        color = UntoldNeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "BALANȚĂ ACTIVĂ", color = UntoldOnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    text = "${String.format("%.2f", ticket.bracelet?.balance ?: 0.0)} lei",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Load Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("20", "50", "100", "200").forEach { amount ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (topUpAmountStr == amount) UntoldNeonCyan.copy(alpha = 0.15f) else UntoldDarkBackground)
                                .border(
                                    1.dp,
                                    if (topUpAmountStr == amount) UntoldNeonCyan else UntoldOutlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { topUpAmountStr = amount }
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(text = "$amount lei", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = topUpAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onTopUp(amt)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UntoldNeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    enabled = !isActionLoading
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(color = UntoldDarkBackground, modifier = Modifier.size(20.dp))
                    } else {
                        Text("TOP-UP ONLINE", color = UntoldDarkBackground, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
