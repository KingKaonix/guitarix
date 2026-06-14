package com.kaonixx.guitarix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.GuitarEngine

private val S0 = Color(0xFF121216)
private val S1 = Color(0xFF1A1A22)
private val S2 = Color(0xFF22222E)
private val S3 = Color(0xFF2A2A36)
private val Border = Color(0xFF2A2A3A)
private val Cyan = Color(0xFF22D3EE)
private val TPrimary = Color(0xFFF1F1F5)
private val TSecondary = Color(0xFF8888A0)
private val TMuted = Color(0xFF555570)

@Composable
fun PresetRow(vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GuitarEngine.presetNames.forEachIndexed { idx, name ->
            val sel = idx == vm.currentPresetIndex
            val bg by animateColorAsState(if (sel) Cyan else S1, spring(Spring.DampingRatioMediumBouncy))
            val tc by animateColorAsState(if (sel) S0 else TSecondary, spring(Spring.DampingRatioMediumBouncy))
            Surface(
                onClick = { vm.loadPreset(idx) },
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = bg,
                tonalElevation = if (sel) 4.dp else 0.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tc, letterSpacing = 1.2.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun EffectCards(vm: MainViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        val allEffects = listOf(
            EffectData("Amp Sim", vm.ampSimOn, { vm.toggleAmpSim() }, KrankColors.AmpSim, listOf(
                ParamDef("Drive", vm.ampSimGain, { vm.updateAmpSimGain(it) }),
                ParamDef("Tone", vm.ampSimTone, { vm.updateAmpSimTone(it) }),
                ParamDef("Level", vm.ampSimMaster, { vm.updateAmpSimMaster(it) })
            )),
            EffectData("Distortion", vm.distortionOn, { vm.toggleDistortion() }, KrankColors.Distortion, listOf(
                ParamDef("Drive", vm.distortionDrive, { vm.updateDistortionDrive(it) }),
                ParamDef("Tone", vm.distortionTone, { vm.updateDistortionTone(it) }),
                ParamDef("Level", vm.distortionLevel, { vm.updateDistortionLevel(it) })
            )),
            EffectData("EQ", vm.eqOn, { vm.toggleEq() }, KrankColors.EQ, listOf(
                ParamDef("Bass", vm.eqBass, { vm.updateEqBass(it) }),
                ParamDef("Mid", vm.eqMid, { vm.updateEqMid(it) }),
                ParamDef("Treble", vm.eqTreble, { vm.updateEqTreble(it) })
            )),
            EffectData("Chorus", vm.chorusOn, { vm.toggleChorus() }, KrankColors.Chorus, listOf(
                ParamDef("Rate", vm.chorusRate, { vm.updateChorusRate(it) }),
                ParamDef("Depth", vm.chorusDepth, { vm.updateChorusDepth(it) }),
                ParamDef("Mix", vm.chorusMix, { vm.updateChorusMix(it) })
            )),
            EffectData("Noise Gate", vm.noiseGateOn, { vm.toggleNoiseGate() }, KrankColors.NoiseGate, listOf(
                ParamDef("Threshold", vm.noiseGateThreshold, { vm.updateNoiseGateThreshold(it) }),
                ParamDef("Attack", vm.noiseGateAttack, { vm.updateNoiseGateAttack(it) }),
                ParamDef("Release", vm.noiseGateRelease, { vm.updateNoiseGateRelease(it) })
            )),
            EffectData("Compressor", vm.compressorOn, { vm.toggleCompressor() }, KrankColors.Compressor, listOf(
                ParamDef("Threshold", vm.compressorThreshold, { vm.updateCompressorThreshold(it) }),
                ParamDef("Ratio", vm.compressorRatio, { vm.updateCompressorRatio(it) }),
                ParamDef("Attack", vm.compressorAttack, { vm.updateCompressorAttack(it) }),
                ParamDef("Release", vm.compressorRelease, { vm.updateCompressorRelease(it) })
            )),
            EffectData("Delay", vm.delayOn, { vm.toggleDelay() }, KrankColors.Delay, listOf(
                ParamDef("Mix", vm.delayMix, { vm.updateDelayMix(it) }),
                ParamDef("Feedback", vm.delayFeedback, { vm.updateDelayFeedback(it) }),
                ParamDef("Time", vm.delayMs / 400f, { vm.updateDelayMs(it * 400f) })
            )),
            EffectData("Reverb", vm.reverbOn, { vm.toggleReverb() }, KrankColors.Reverb, listOf(
                ParamDef("Size", vm.reverbRoomSize, { vm.updateReverbRoomSize(it) }),
                ParamDef("Mix", vm.reverbMix, { vm.updateReverbMix(it) })
            ))
        )

        allEffects.forEach { effect ->
            AmpStyleCard(
                name = effect.name,
                enabled = effect.enabled,
                accent = effect.accent,
                onToggle = effect.onToggle,
                params = effect.params
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

private data class EffectData(val name: String, val enabled: Boolean, val onToggle: () -> Unit, val accent: Color, val params: List<ParamDef>)
private data class ParamDef(val label: String, val value: Float, val onChange: (Float) -> Unit)

@Composable
private fun AmpStyleCard(name: String, enabled: Boolean, accent: Color, onToggle: () -> Unit, params: List<ParamDef>) {
    val bg by animateColorAsState(if (enabled) S1 else S0, spring(Spring.DampingRatioMediumBouncy))
    val border by animateColorAsState(if (enabled) Border.copy(alpha = 0.8f) else Border.copy(alpha = 0.4f), spring(Spring.DampingRatioMediumBouncy))

    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = bg, tonalElevation = 0.dp) {
        Column(Modifier.border(1.dp, border, RoundedCornerShape(16.dp)).padding(16.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LED
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (enabled) accent else TMuted).border(0.5.dp, if (enabled) accent.copy(alpha = 0.5f) else Color.Transparent, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (enabled) TPrimary else TSecondary, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (enabled) "ON" else "OFF", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = if (enabled) accent else TMuted, letterSpacing = 1.sp)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = enabled, onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.3f), uncheckedThumbColor = Color(0xFF3A3A4A), uncheckedTrackColor = Color(0xFF1A1A22)))
                }
            }
            Spacer(Modifier.height(14.dp))
            // Sliders
            params.forEach { param ->
                ParamSlider(label = param.label, value = param.value, onChange = param.onChange, enabled = enabled, accent = accent)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ParamSlider(label: String, value: Float, onChange: (Float) -> Unit, enabled: Boolean, accent: Color) {
    val displayValue = remember(value) {
        when (label) {
            "Time" -> "${(value * 2000).toInt()} ms"
            "Ratio" -> "%.1f:1".format(1f + value * 19f)
            "Threshold" -> "%.0f dB".format(-60f + value * 60f)
            "Drive", "Tone", "Level", "Gain", "Master" -> "${(value * 10).toInt()}"
            else -> "${(value * 100).toInt()}%"
        }
    }
    val activeColor = if (enabled) accent else TMuted

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TSecondary, letterSpacing = 0.5.sp)
            Text(displayValue, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = activeColor, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(2.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = S3,
                disabledThumbColor = TMuted,
                disabledActiveTrackColor = TMuted,
                disabledInactiveTrackColor = S3
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
