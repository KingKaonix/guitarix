package com.kaonixx.guitarix.ui.transcribe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.TabNoteData
import com.kaonixx.guitarix.WavLoader
import com.kaonixx.guitarix.MicRecorder
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLifecycleOwner

private val Bg         = Color(0xFF0A0A0E)
private val S1         = Color(0xFF1A1A22)
private val S2         = Color(0xFF22222E)
private val Border     = Color(0xFF2A2A3A)
private val Cyan       = Color(0xFF22D3EE)
private val CyanDim    = Color(0xFF1BA3BB)
private val Green      = Color(0xFF22C55E)
private val TPrimary   = Color(0xFFF1F1F5)
private val TSecondary = Color(0xFF8888A0)
private val TMuted     = Color(0xFF555570)
private val STRING_NAMES = listOf("e", "B", "G", "D", "A", "E")

@Composable
fun TranscribeScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            loading = true; errorMsg = ""
            try {
                val result = WavLoader.load(context, uri)
                if (result != null) {
                    val monoData = if (result.numChannels == 1) result.samples
                    else FloatArray(result.samples.size / result.numChannels) { i ->
                        var s = 0f; for (c in 0 until result.numChannels) s += result.samples[i * result.numChannels + c]
                        s / result.numChannels
                    }
                    fileName = uri.lastPathSegment ?: "Loaded"
                    runCatching { vm.transcribeAudio(monoData, result.sampleRate) }
                } else errorMsg = "Could not read audio file"
            } catch (e: Exception) { errorMsg = "Error: ${e.message}" }
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        // Section header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(S1)
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SONG TRANSCRIPTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TSecondary,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (vm.transcribeHasResult) Green else TMuted)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Load card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = S1),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Cyan.copy(alpha = 0.1f))
                        .border(1.dp, Cyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "♪",
                        fontSize = 24.sp,
                        color = Cyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "LOAD AUDIO FILE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TPrimary,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Generate tablature from guitar audio",
                    fontSize = 11.sp,
                    color = TSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                if (fileName.isNotEmpty()) {
                    Text(
                        "FILE: $fileName",
                        fontSize = 10.sp,
                        color = Cyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (errorMsg.isNotEmpty()) {
                    Text(
                        errorMsg,
                        fontSize = 10.sp,
                        color = Color(0xFFFF6B6B),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan,
                        contentColor = Bg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (loading) "PROCESSING..." else "SELECT AUDIO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Results
        if (vm.transcribeHasResult) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = S1),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TABLATURE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TSecondary,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${vm.transcribeNumMeasures} measures · ${vm.transcribeNotes.size} notes",
                            fontSize = 10.sp,
                            color = Cyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TabView(vm.transcribeNotes)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = S1.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "ABOUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TSecondary,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "• Load a WAV file with monophonic guitar audio",
                    fontSize = 11.sp,
                    color = TSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Text(
                    "• System detects notes and maps to fret positions",
                    fontSize = 11.sp,
                    color = TSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Text(
                    "• Best results with solo guitar or bass parts",
                    fontSize = 11.sp,
                    color = TSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "* ML source separation for multi-instrument coming soon",
                    fontSize = 10.sp,
                    color = TMuted,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TabView(notes: List<TabNoteData>) {
    if (notes.isEmpty()) {
        Text(
            "No notes detected",
            fontSize = 12.sp,
            color = TMuted,
            fontFamily = FontFamily.Monospace
        )
        return
    }

    val displayNotes = notes.take(60)
    val maxTime = displayNotes.maxOfOrNull { it.startTime + it.duration } ?: 1f

    // 6-string tab rendering
    for (stringIdx in 0..5) {
        val matchingNotes = displayNotes.filter {
            it.stringNum == stringIdx || (it.stringNum == -1 && stringIdx == 0)
        }
        Row(
            Modifier.fillMaxWidth().height(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // String label
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(S2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    STRING_NAMES.getOrElse(stringIdx) { "?" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanDim,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(4.dp))
            // Tab line
            Canvas(Modifier.fillMaxSize()) {
                val h = size.height
                val w = size.width

                // String line
                val lineY = h / 2f
                drawLine(
                    color = TSecondary.copy(alpha = 0.2f),
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 1f
                )

                // Note markers
                for (note in matchingNotes) {
                    val safeStringNum = note.stringNum.coerceIn(0, 5)
                    if (safeStringNum != stringIdx) continue

                    val x = ((note.startTime / maxTime) * w).coerceIn(2f, w - 14f)
                    // Fret number
                    val fretText = note.fret.toString()

                    // Note circle
                    drawCircle(
                        color = Cyan.copy(alpha = 0.8f),
                        radius = 7.dp.toPx(),
                        center = Offset(x, lineY)
                    )
                    drawCircle(
                        color = Cyan.copy(alpha = 0.3f),
                        radius = 9.dp.toPx(),
                        center = Offset(x, lineY)
                    )
                }
            }
        }
    }
}
