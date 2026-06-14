package com.kaonixx.guitarix.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.GuitarEngine
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Bg          = Color(0xFF0A0A0E)
private val S0          = Color(0xFF121216)
private val S1          = Color(0xFF1A1A22)
private val S2          = Color(0xFF22222E)
private val BorderOn    = Color(0xFF2A2A3A)
private val BorderOff   = Color(0xFF2E2E3A)
private val Cyan        = Color(0xFF22D3EE)
private val TPrimary    = Color(0xFFF1F1F5)
private val TSecondary  = Color(0xFF8888A0)
private val TMuted      = Color(0xFF555570)

@Composable
fun PresetRow(vm: MainViewModel) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GuitarEngine.presetNames.forEachIndexed { idx, name ->
            val sel = idx == vm.currentPresetIndex
            val bg by animateColorAsState(if (sel) Cyan else S1, spring(Spring.DampingRatioMediumBouncy))
            val tc by animateColorAsState(if (sel) Bg else TSecondary, spring(Spring.DampingRatioMediumBouncy))
            Button(onClick = { vm.loadPreset(idx) }, modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = tc),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (sel) 6.dp else 2.dp)) {
                Text(name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun EffectCards(vm: MainViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        // Just show the 8 effects as simple toggle rows
        val effects = listOf(
            "Amp Sim" to vm.ampSimOn,
            "Distortion" to vm.distortionOn,
            "EQ" to vm.eqOn,
            "Chorus" to vm.chorusOn,
            "Noise Gate" to vm.noiseGateOn,
            "Compressor" to vm.compressorOn,
            "Delay" to vm.delayOn,
            "Reverb" to vm.reverbOn
        )
        val toggles = listOf(
            { vm.toggleAmpSim() }, { vm.toggleDistortion() }, { vm.toggleEq() }, { vm.toggleChorus() },
            { vm.toggleNoiseGate() }, { vm.toggleCompressor() }, { vm.toggleDelay() }, { vm.toggleReverb() }
        )

        effects.forEachIndexed { i, (name, enabled) ->
            val accent = effectColor(name)
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = if (enabled) S1 else S0) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (enabled) accent else TMuted))
                        Spacer(Modifier.width(10.dp))
                        Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) TPrimary else TSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Switch(checked = enabled, onCheckedChange = { toggles[i]() },
                        colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.3f)))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
