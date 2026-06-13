package com.kaonixx.guitarix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.ui.tuner.TunerScreen
import com.kaonixx.guitarix.ui.tone_matcher.ToneMatcherScreen
import com.kaonixx.guitarix.ui.transcribe.TranscribeScreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

// ── Palette ──
private val Bg          = Color(0xFF0A0A0E)
private val S0          = Color(0xFF121216)
private val S1          = Color(0xFF1A1A22)
private val BorderOn    = Color(0xFF2A2A3A)
private val BorderOff   = Color(0xFF2E2E3A)
private val Cyan        = Color(0xFF22D3EE)
private val CyanDim     = Color(0xFF1BA3BB)
private val CyanGlow    = Color(0x3322D3EE)
private val TPrimary    = Color(0xFFF1F1F5)
private val TSecondary  = Color(0xFF8888A0)
private val TMuted      = Color(0xFF555570)
private val Disabled    = Color(0xFF2E2E3A)

// Tab icons that exist in material-icons-core
// Settings, Search, Star, List are verified core icons
private val tabIcons = listOf(Icons.Filled.Settings, Icons.Filled.Search, Icons.Filled.Star, Icons.Filled.List)
private val tabLabels = listOf("EFFECTS", "TUNER", "MATCH", "TAB")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Bg,
        topBar = { HardwareTopBar(vm) },
        bottomBar = { HardwareNavBar(vm) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "effects",
            modifier = Modifier.padding(padding)
        ) {
            composable("effects") { EffectsScreen(vm) }
            composable("tuner") { TunerScreen(vm) }
            composable("tone_matcher") { ToneMatcherScreen(vm) }
            composable("transcribe") { TranscribeScreen(vm) }
        }
    }

    LaunchedEffect(vm.currentTab) {
        when (vm.currentTab) {
            0 -> navController.navigate("effects") { popUpTo("effects") { inclusive = true } }
            1 -> navController.navigate("tuner") { popUpTo("tuner") { inclusive = true } }
            2 -> navController.navigate("tone_matcher") { popUpTo("tone_matcher") { inclusive = true } }
            3 -> navController.navigate("transcribe") { popUpTo("transcribe") { inclusive = true } }
        }
    }
}

// ── Hardware-Inspired Bottom Navigation Bar ──
@Composable
private fun HardwareNavBar(vm: MainViewModel) {
    NavigationBar(
        containerColor = S0,
        tonalElevation = 0.dp,
        modifier = Modifier
            .height(72.dp)
            .border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        tabLabels.forEachIndexed { index, label ->
            val selected = vm.currentTab == index
            val accentColor = when (index) {
                0 -> Cyan
                1 -> Color(0xFF22C55E)
                2 -> Color(0xFFF59E0B)
                3 -> Color(0xFFA78BFA)
                else -> Cyan
            }
            val itemBg by animateColorAsState(
                if (selected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                spring(Spring.DampingRatioMediumBouncy)
            )
            val iconTint by animateColorAsState(
                if (selected) accentColor else TMuted,
                spring(Spring.DampingRatioMediumBouncy)
            )

            // Glow animation for selected tab
            val infiniteTransition = rememberInfiniteTransition(label = "tabGlow$index")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(itemBg)
                    .clickable { vm.setTab(index) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tabIcons[index],
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    // Hardware LED dot indicator
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = glowAlpha))
                                .border(0.5.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    label,
                    fontSize = 9.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = iconTint,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Hardware-Inspired Top Bar ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HardwareTopBar(vm: MainViewModel) {
    val ledCol by animateColorAsState(
        if (vm.isRunning) Color(0xFF22C55E) else TMuted,
        spring(Spring.DampingRatioMediumBouncy)
    )

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Power LED
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ledCol)
                        .border(1.dp, if (vm.isRunning) ledCol.copy(alpha = 0.4f) else Color.Transparent, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "KRANK",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TPrimary,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "STUDIO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    color = TSecondary,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // VU Meter mini indicator
                Canvas(modifier = Modifier.size(40.dp, 16.dp)) {
                    val barW = 4.dp.toPx()
                    val gap = 2.dp.toPx()
                    val bars = 6
                    for (i in 0 until bars) {
                        val x = i * (barW + gap)
                        val h = (size.height * (1f - i.toFloat() / bars)).coerceAtLeast(2.dp.toPx())
                        val col = when {
                            i >= 5 -> Color(0xFFFF6B6B)
                            i >= 3 -> Color(0xFFF59E0B)
                            else -> Color(0xFF22C55E)
                        }
                        drawRoundRect(
                            color = if (vm.isRunning) col.copy(alpha = 0.8f) else col.copy(alpha = 0.15f),
                            topLeft = Offset(x, size.height - h),
                            size = Size(barW, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Power status text
                Text(
                    if (vm.isRunning) "ON" else "OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (vm.isRunning) Color(0xFF22C55E) else TMuted,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(8.dp))
                // Hardware-style switch
                Switch(
                    checked = vm.isRunning,
                    onCheckedChange = { vm.toggleEngine() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF22C55E),
                        checkedTrackColor = Color(0xFF22C55E).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF3A3A4A),
                        uncheckedTrackColor = Color(0xFF1A1A22)
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = S0,
            titleContentColor = TPrimary
        ),
        modifier = Modifier.border(1.dp, Color(0xFF1E1E2A), RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
    )
}

// ── Effects Tab Wrapper ──
@Composable
private fun EffectsScreen(vm: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        // Channel strip header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(S1)
                .border(1.dp, Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CHANNEL STRIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TSecondary,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (vm.isRunning) Color(0xFF22C55E) else TMuted)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (vm.isRunning) "SIGNAL FLOW" else "BYPASS",
                        fontSize = 8.sp,
                        color = if (vm.isRunning) Color(0xFF22C55E) else TMuted,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PresetRow(vm)
        Spacer(Modifier.height(16.dp))
        EffectCards(vm)
        Spacer(Modifier.height(40.dp))
    }
}
