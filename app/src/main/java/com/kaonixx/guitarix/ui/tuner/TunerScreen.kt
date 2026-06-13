package com.kaonixx.guitarix.ui.tuner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaonixx.guitarix.MainViewModel

@Composable
fun TunerScreen(vm: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0E)).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Text("CHROMATIC TUNER", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F1F5), letterSpacing = 2.sp)
        Spacer(Modifier.height(20.dp))
        TuningPresetSelector(vm)
        Spacer(Modifier.height(30.dp))
        TunerDisplay(vm)
        Spacer(Modifier.height(30.dp))
        Instructions()
    }
}

private val tuningNames = listOf("Standard", "Drop D", "Drop C", "Open D", "Open G", "Open E", "DADGAD",
    "Half-Step Down", "Full-Step Down", "Drop B", "Open A", "Custom")

@Composable
private fun TuningPresetSelector(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Tuning Preset", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8888A0))
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF1F1F5))) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(tuningNames[vm.tunerCurrentTuning], fontSize = 14.sp)
                    Icon(ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                tuningNames.forEachIndexed { i, name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { vm.setTunerTuning(i); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun TunerDisplay(vm: MainViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1A1A22))
            .border(2.dp, Color(0xFF2A2A3A), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (vm.isTunerNoteDetected) {
                    Text("${vm.tunerNoteIndex}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE))
                    Text(vm.engine.getTunerNoteName(vm.tunerNoteIndex), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color(0xFFF1F1F5))
                    Text("${vm.tunerOctave}", fontSize = 16.sp, color = Color(0xFF8888A0))
                } else {
                    Text("--", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8888A0))
                    Text("NO SIGNAL", fontSize = 16.sp, color = Color(0xFF8888A0))
                }
                Spacer(Modifier.height(10.dp))
                Text(if (vm.isTunerNoteDetected) "%.1f Hz".format(vm.tunerFrequency) else "--- Hz",
                    fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF1F1F5))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Card(modifier = Modifier.width(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A22)),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CENT", fontSize = 12.sp, color = Color(0xFF8888A0))
                    Text("%+.0f\u00A2".format(vm.tunerCents), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22D3EE))
                }
            }
            Card(modifier = Modifier.width(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A22)),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("STRING", fontSize = 12.sp, color = Color(0xFF8888A0))
                    Text("--", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F1F5))
                }
            }
        }
    }
}

@Composable
private fun Instructions() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("How to Use:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F1F5))
        Spacer(Modifier.height(8.dp))
        Text("1. Connect your instrument and ensure audio is enabled", fontSize = 12.sp, color = Color(0xFF8888A0))
        Text("2. Select a tuning preset that matches your guitar setup", fontSize = 12.sp, color = Color(0xFF8888A0))
        Text("3. Play a note on each string and tune until it reads 0\u00A2", fontSize = 12.sp, color = Color(0xFF8888A0))
        Spacer(Modifier.height(10.dp))
        Text("* Works best with clean, sustained notes", fontSize = 11.sp, color = Color(0xFF8888A0), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    }
}
