package com.sosring.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosring.android.ui.MainViewModel
import com.sosring.android.ui.theme.*
import com.sosring.ble.BleConnectionState
import com.sosring.sos.SosState

@Composable
fun DashboardScreen(
    vm: MainViewModel,
    onManageContacts: () -> Unit
) {
    val state by vm.appState.collectAsStateWithLifecycle()

    val isSosActive = state.sosState !is SosState.Idle &&
                      state.sosState !is SosState.Cancelled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SOS Ring",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onManageContacts) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = "Manage contacts",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Status row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bluetooth,
                    label = when (state.bleState) {
                        BleConnectionState.Connected    -> "Ring Connected"
                        BleConnectionState.Scanning     -> "Scanning…"
                        BleConnectionState.Connecting   -> "Connecting…"
                        BleConnectionState.Disconnected -> "Ring Offline"
                        is BleConnectionState.Error     -> "BLE Error"
                    },
                    active = state.bleState == BleConnectionState.Connected
                )
                StatusChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    label = if (state.isGpsEnabled) "GPS On" else "GPS Off",
                    active = state.isGpsEnabled,
                    warnIfInactive = true
                )
            }

            // ── GPS warning ───────────────────────────────────────────────────
            if (!state.isGpsEnabled) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarnAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = WarnAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Enable location services for accurate SOS alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarnAmber
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── SOS state display ─────────────────────────────────────────────
            AnimatedContent(
                targetState = state.sosState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sos_state"
            ) { sosState ->
                when (sosState) {
                    is SosState.Countdown -> CountdownDisplay(sosState.secondsRemaining)
                    is SosState.Active    -> ActiveAlertDisplay()
                    is SosState.Cancelled -> CancelledDisplay()
                    else                  -> Spacer(Modifier.height(120.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Main SOS button ───────────────────────────────────────────────
            SosButton(
                isActive = isSosActive,
                onTrigger = { vm.triggerSos() },
                onCancel  = { vm.cancelSos() }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    warnIfInactive: Boolean = false
) {
    val color = when {
        active          -> SafeGreen
        warnIfInactive  -> WarnAmber
        else            -> Subtle
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun CountdownDisplay(seconds: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "SOS in",
            style = MaterialTheme.typography.titleMedium,
            color = WarnAmber
        )
        Text(
            "$seconds",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
            color = SosRed,
            fontWeight = FontWeight.Black
        )
        Text(
            "seconds – tap Cancel to stop",
            style = MaterialTheme.typography.bodyMedium,
            color = Subtle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActiveAlertDisplay() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f, targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "scale"
        )
        Icon(
            Icons.Default.Warning,
            null,
            tint = SosRed,
            modifier = Modifier.size(64.dp).scale(pulse)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "SOS ALERT SENT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SosRed
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Emergency services will be notified in 2 min if not cancelled",
            style = MaterialTheme.typography.bodySmall,
            color = Subtle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CancelledDisplay() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        Text("Alert Cancelled", style = MaterialTheme.typography.titleMedium, color = SafeGreen)
    }
}

@Composable
private fun SosButton(
    isActive: Boolean,
    onTrigger: () -> Unit,
    onCancel: () -> Unit
) {
    val label  = if (isActive) "CANCEL SOS" else "SOS"
    val color  = if (isActive) WarnAmber else SosRed
    val size   = if (isActive) 140.dp else 180.dp

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        if (!isActive) {
            val pulse by rememberInfiniteTransition(label = "ring_pulse").animateFloat(
                initialValue = 0.85f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                label = "ring_scale"
            )
            Box(
                modifier = Modifier
                    .size(size + 32.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(SosRed.copy(alpha = 0.15f))
            )
        }

        Button(
            onClick = if (isActive) onCancel else onTrigger,
            modifier = Modifier.size(size),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}
