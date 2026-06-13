package com.kaonixx.guitarix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.GuitarEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ──
private val Bg          = Color(0xFF0A0A0E)
private val S0          = Color(0xFF121216)
private val S1          = Color(0xFF1A1A22)
private val S2          = Color(0xFF22222E)
private val BorderOn    = Color(0xFF2A2A3A)
private val BorderOff   = Color(0xFF2E2E3A)
private val BorderBright = Color(0xFF3A3A4E)
private val Cyan        = Color(0xFF22D3EE)
private val CyanDim     = Color(0xFF1BA3BB)
private val TPrimary    = Color(0xFFF1F1F5)
private val TSecondary  = Color(0xFF8888A0)
private val TMuted      = Color(0xFF555570)
private val Disabled    = Color(0xFF2E2E3A)

// ── Preset Row ──
@Composable
fun PresetRow(vm: MainViewModel) {
    val presetNames = GuitarEngine.presetNames
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        presetNames.forEachIndexed { idx, name ->
            val sel = idx == vm.currentPresetIndex
            val bg by animateColorAsState(
                if (sel) Cyan else S1,
                spring(Spring.DampingRatioMediumBouncy)
            )
            val tc by animateColorAsState(
                if (sel) Bg else TSecondary,
                spring(Spring.DampingRatioMediumBouncy)
            )
            Button(
                onClick = { vm.loadPreset(idx) },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = bg,
                    contentColor = tc
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (sel) 6.dp else 2.dp
                )
            ) {
                Text(
                    name.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── Effect Cards Container ──
@Composable
fun EffectCards(vm: MainViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        AmpSimCard(vm)
        Spacer(Modifier.height(10.dp))
        DistortionCard(vm)
        Spacer(Modifier.height(10.dp))
        EQCard(vm)
        Spacer(Modifier.height(10.dp))
        ChorusCard(vm)
        Spacer(Modifier.height(10.dp))
        NoiseGateCard(vm)
        Spacer(Modifier.height(10.dp))
        CompressorCard(vm)
        Spacer(Modifier.height(10.dp))
        DelayCard(vm)
        Spacer(Modifier.height(10.dp))
        ReverbCard(vm)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun AmpSimCard(vm: MainViewModel) {
    EffectCard(
        name = "AMP SIM", enabled = vm.ampSimOn,
        accent = KrankColors.AmpSim, onToggle = { vm.toggleAmpSim() },
        knobs = listOf(
            KnobDef("DRIVE", vm.ampSimGain, { vm.updateAmpSimGain(it) }, "gain"),
            KnobDef("TONE", vm.ampSimTone, { vm.updateAmpSimTone(it) }, "tone"),
            KnobDef("LEVEL", vm.ampSimMaster, { vm.updateAmpSimMaster(it) }, "level")
        )
    )
}

@Composable
private fun DistortionCard(vm: MainViewModel) {
    EffectCard(
        name = "DISTORTION", enabled = vm.distortionOn,
        accent = KrankColors.Distortion, onToggle = { vm.toggleDistortion() },
        knobs = listOf(
            KnobDef("DRIVE", vm.distortionDrive, { vm.updateDistortionDrive(it) }, "gain"),
            KnobDef("TONE", vm.distortionTone, { vm.updateDistortionTone(it) }, "tone"),
            KnobDef("LEVEL", vm.distortionLevel, { vm.updateDistortionLevel(it) }, "level")
        )
    )
}

@Composable
private fun EQCard(vm: MainViewModel) {
    EffectCard(
        name = "EQUALIZER", enabled = vm.eqOn,
        accent = KrankColors.EQ, onToggle = { vm.toggleEq() },
        knobs = listOf(
            KnobDef("BASS", vm.eqBass, { vm.updateEqBass(it) }, "eq"),
            KnobDef("MID", vm.eqMid, { vm.updateEqMid(it) }, "eq"),
            KnobDef("TREBLE", vm.eqTreble, { vm.updateEqTreble(it) }, "eq")
        )
    )
}

@Composable
private fun ChorusCard(vm: MainViewModel) {
    EffectCard(
        name = "CHORUS", enabled = vm.chorusOn,
        accent = KrankColors.Chorus, onToggle = { vm.toggleChorus() },
        knobs = listOf(
            KnobDef("RATE", vm.chorusRate, { vm.updateChorusRate(it) }, "rate"),
            KnobDef("DEPTH", vm.chorusDepth, { vm.updateChorusDepth(it) }, "depth"),
            KnobDef("MIX", vm.chorusMix, { vm.updateChorusMix(it) }, "mix")
        )
    )
}

@Composable
private fun NoiseGateCard(vm: MainViewModel) {
    EffectCard(
        name = "NOISE GATE", enabled = vm.noiseGateOn,
        accent = KrankColors.NoiseGate, onToggle = { vm.toggleNoiseGate() },
        knobs = listOf(
            KnobDef("THRESH", vm.noiseGateThreshold, { vm.updateNoiseGateThreshold(it) }, "threshold"),
            KnobDef("ATTACK", vm.noiseGateAttack, { vm.updateNoiseGateAttack(it) }, "time"),
            KnobDef("RELEASE", vm.noiseGateRelease, { vm.updateNoiseGateRelease(it) }, "time")
        )
    )
}

@Composable
private fun CompressorCard(vm: MainViewModel) {
    EffectCard(
        name = "COMPRESSOR", enabled = vm.compressorOn,
        accent = KrankColors.Compressor, onToggle = { vm.toggleCompressor() },
        knobs = listOf(
            KnobDef("THRESH", vm.compressorThreshold, { vm.updateCompressorThreshold(it) }, "threshold"),
            KnobDef("RATIO", vm.compressorRatio, { vm.updateCompressorRatio(it) }, "ratio"),
            KnobDef("ATTACK", vm.compressorAttack, { vm.updateCompressorAttack(it) }, "time"),
            KnobDef("RELEASE", vm.compressorRelease, { vm.updateCompressorRelease(it) }, "time")
        )
    )
}

@Composable
private fun DelayCard(vm: MainViewModel) {
    EffectCard(
        name = "DELAY", enabled = vm.delayOn,
        accent = KrankColors.Delay, onToggle = { vm.toggleDelay() },
        knobs = listOf(
            KnobDef("MIX", vm.delayMix, { vm.updateDelayMix(it) }, "mix"),
            KnobDef("FEEDBK", vm.delayFeedback, { vm.updateDelayFeedback(it) }, "feedback"),
            KnobDef("TIME", vm.delayMs / 400f, { vm.updateDelayMs(it * 400f) }, "time")
        )
    )
}

@Composable
private fun ReverbCard(vm: MainViewModel) {
    EffectCard(
        name = "REVERB", enabled = vm.reverbOn,
        accent = KrankColors.Reverb, onToggle = { vm.toggleReverb() },
        knobs = listOf(
            KnobDef("SIZE", vm.reverbRoomSize, { vm.updateReverbRoomSize(it) }, "size"),
            KnobDef("MIX", vm.reverbMix, { vm.updateReverbMix(it) }, "mix")
        )
    )
}

// ── Hardware Amp Effect Card ──
@Composable
private fun EffectCard(
    name: String,
    enabled: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    knobs: List<KnobDef>
) {
    val cardBg by animateColorAsState(
        if (enabled) S1 else S0,
        spring(Spring.DampingRatioMediumBouncy)
    )
    val borderColor by animateColorAsState(
        if (enabled) BorderOn else BorderOff,
        spring(Spring.DampingRatioMediumBouncy)
    )

    // Pulsing glow for active effects
    val infiniteTransition = rememberInfiniteTransition(label = "glow$name")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = S0
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
        ) {
            if (enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(1.5.dp, accent.copy(alpha = pulseAlpha * 0.4f), RoundedCornerShape(14.dp))
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (enabled) accent.copy(alpha = 0.9f) else TMuted.copy(alpha = 0.3f))
                                    .border(
                                        0.5.dp,
                                        if (enabled) accent.copy(alpha = 0.5f) else Color.Transparent,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) TPrimary else TSecondary,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (enabled) "ON" else "OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) accent else TMuted,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { onToggle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accent,
                                    checkedTrackColor = accent.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF3A3A4A),
                                    uncheckedTrackColor = Color(0xFF1A1A22)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        knobs.forEach { knob ->
                            HardwareKnob(
                                label = knob.label,
                                value = knob.value,
                                onChange = knob.onChange,
                                enabled = enabled,
                                accent = accent,
                                knobType = knob.type
                            )
                        }
                        if (knobs.size == 2) {
                            Spacer(Modifier.width(72.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── VU-Meter Hardware Knob ──
@Composable
private fun HardwareKnob(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    enabled: Boolean,
    accent: Color,
    knobType: String
) {
    val displayValue = remember(value) {
        when (knobType) {
            "gain" -> "${(value * 10).toInt()}"
            "tone" -> "${(value * 10).toInt()}"
            "level" -> "${(value * 10).toInt()}"
            "time" -> "${(value * 2000).toInt()}ms"
            "ratio" -> "%.1f:1".format(1f + value * 19f)
            "threshold" -> "%.0f".format(-60f + value * 60f)
            "feedback" -> "${(value * 100).toInt()}%"
            "mix" -> "${(value * 100).toInt()}%"
            "rate" -> "%.1f".format(0.1f + value * 9.9f)
            "depth" -> "${(value * 100).toInt()}%"
            "size" -> "${(value * 100).toInt()}%"
            "eq" -> "${(value * 10).toInt()}"
            else -> "${(value * 100).toInt()}%"
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        var acc by remember { mutableFloatStateOf(value) }
        LaunchedEffect(value) { acc = value }

        Box(Modifier.size(60.dp)) {
            Canvas(Modifier.size(60.dp).pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures { _, dragAmount ->
                    val sens = 200f
                    acc = (acc - dragAmount / sens).coerceIn(0f, 1f)
                    onChange(acc)
                }
            }) {
                val stroke = 4.dp.toPx()
                val r = (60.dp.toPx() - stroke) / 2f
                val c = Offset(30.dp.toPx(), 30.dp.toPx())
                val sa = 135f
                val sw = 270f

                drawArc(
                    Disabled, sa, sw, false,
                    Offset(stroke / 2, stroke / 2),
                    Size(60.dp.toPx() - stroke, 60.dp.toPx() - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )

                val activeColor = if (enabled) accent else Disabled
                drawArc(
                    activeColor, sa, sw * value, false,
                    Offset(stroke / 2, stroke / 2),
                    Size(60.dp.toPx() - stroke, 60.dp.toPx() - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )

                val ang = (sa + sw * value) * PI / 180f
                val inner = r * 0.55f
                val outer = r * 0.9f
                val needleColor = if (enabled) TPrimary else TMuted
                drawLine(
                    needleColor,
                    Offset(c.x + cos(ang).toFloat() * inner, c.y + sin(ang).toFloat() * inner),
                    Offset(c.x + cos(ang).toFloat() * outer, c.y + sin(ang).toFloat() * outer),
                    2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    if (enabled) accent.copy(alpha = 0.8f) else Disabled,
                    radius = 3.5.dp.toPx(),
                    center = c
                )

                for (i in 0..10) {
                    val ta = (sa + sw * (i / 10f)) * PI / 180f
                    val tickInner = r * 0.7f
                    val tickOuter = if (i % 5 == 0) r * 0.82f else r * 0.76f
                    val tickColor = if (enabled && value >= i / 10f) activeColor.copy(alpha = 0.4f) else Disabled.copy(alpha = 0.3f)
                    drawLine(
                        tickColor,
                        Offset(c.x + cos(ta).toFloat() * tickInner, c.y + sin(ta).toFloat() * tickInner),
                        Offset(c.x + cos(ta).toFloat() * tickOuter, c.y + sin(ta).toFloat() * tickOuter),
                        1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = TSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Text(
            displayValue,
            fontSize = 11.sp,
            color = if (enabled) accent else TMuted,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

data class KnobDef(
    val label: String,
    val value: Float,
    val onChange: (Float) -> Unit,
    val type: String = "default"
)
