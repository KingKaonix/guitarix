package com.kaonixx.guitarix.ui.tone_matcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.kaonixx.guitarix.MainViewModel
import com.kaonixx.guitarix.WavLoader
import com.kaonixx.guitarix.ui.effectColor as getEffectColor

// Data class for tone recommendations
data class ToneRecommendation(val effectName: String, val value: Float, val paramName: String)

// ── Tone Matcher Screen ──
@Composable
fun ToneMatcherScreen(vm: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        
        // Title
        Text(
            "TONE MATCHER",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F1F5),
            letterSpacing = 2.sp
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            "Auto-tune effects from WAV samples",
            fontSize = 14.sp,
            color = Color(0xFF8888A0)
        )
        
        Spacer(Modifier.height(30.dp))
        
        // Sample loader
        SampleLoaderSection(vm)
        
        Spacer(Modifier.height(30.dp))
        
        // Results display
        if (vm.toneMatcherHasProfile) {
            ToneMatcherResults(vm)
            Spacer(Modifier.height(30.dp))
        }
        
        // Instructions
        Instructions()
    }
}

// ── Sample Loader Section ──
@Composable
private fun SampleLoaderSection(vm: MainViewModel) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            loading = true
            errorMsg = ""
            try {
                val result = WavLoader.load(context, uri)
                if (result != null) {
                    // Use first 4096 samples for analysis (or less if file is shorter)
                    val numFrames = minOf(result.samples.size, 4096)
                    val monoData = if (result.numChannels == 1) {
                        result.samples.take(numFrames).toFloatArray()
                    } else {
                        // Downmix to mono
                        FloatArray(numFrames / result.numChannels) { i ->
                            var sum = 0f
                            for (c in 0 until result.numChannels) {
                                sum += result.samples[i * result.numChannels + c]
                            }
                            sum / result.numChannels
                        }
                    }
                    vm.loadAudioForToneMatcher(monoData, monoData.size, 1)
                    vm.updateToneMatcherRecommendations()
                    fileName = uri.lastPathSegment ?: "Loaded"
                } else {
                    errorMsg = "Invalid or unsupported WAV file"
                }
            } catch (e: Exception) {
                errorMsg = "Error: ${e.message}"
            }
            loading = false
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A22)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "♫",
                    fontSize = 36.sp,
                    color = Color(0xFF22D3EE)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Load Audio Sample",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F1F5)
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Load a WAV file to analyze its tone and generate effect recommendations",
                    fontSize = 12.sp,
                    color = Color(0xFF8888A0),
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 16.sp
                )
                
                if (fileName.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Loaded: $fileName",
                        fontSize = 12.sp,
                        color = Color(0xFF22D3EE),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMsg,
                        fontSize = 12.sp,
                        color = Color(0xFFFF6B6B)
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("audio/wav", "audio/x-wav"))
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22D3EE),
                        contentColor = Color(0xFF0A0A0E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (loading) "LOADING..." else "SELECT WAV FILE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Tone Matcher Results ──
@Composable
private fun ToneMatcherResults(vm: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Recommended Effects:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F1F5),
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Create a grid of recommended effect parameters
        val recommendations = listOf(
            ToneRecommendation("Distortion", vm.toneMatcherRecommendedDistortionDrive, "Drive"),
            ToneRecommendation("Distortion", vm.toneMatcherRecommendedDistortionTone, "Tone"),
            ToneRecommendation("Distortion", vm.toneMatcherRecommendedDistortionLevel, "Level"),
            ToneRecommendation("Amp Sim", vm.toneMatcherRecommendedAmpSimGain, "Gain"),
            ToneRecommendation("Amp Sim", vm.toneMatcherRecommendedAmpSimTone, "Tone"),
            ToneRecommendation("Amp Sim", vm.toneMatcherRecommendedAmpSimMaster, "Master"),
            ToneRecommendation("EQ", vm.toneMatcherRecommendedEqBass, "Bass"),
            ToneRecommendation("EQ", vm.toneMatcherRecommendedEqMid, "Mid"),
            ToneRecommendation("EQ", vm.toneMatcherRecommendedEqTreble, "Treble"),
            ToneRecommendation("Chorus", vm.toneMatcherRecommendedChorusRate, "Rate"),
            ToneRecommendation("Chorus", vm.toneMatcherRecommendedChorusDepth, "Depth"),
            ToneRecommendation("Chorus", vm.toneMatcherRecommendedChorusMix, "Mix"),
            ToneRecommendation("Delay", vm.toneMatcherRecommendedDelayMix, "Mix"),
            ToneRecommendation("Delay", vm.toneMatcherRecommendedDelayFeedback, "Feedback"),
            ToneRecommendation("Delay", vm.toneMatcherRecommendedDelayTime, "Time"),
            ToneRecommendation("Reverb", vm.toneMatcherRecommendedReverbSize, "Size"),
            ToneRecommendation("Reverb", vm.toneMatcherRecommendedReverbMix, "Mix")
        )
        
        // Display in a grid
        val columns = 2
        val rows = (recommendations.size + columns - 1) / columns
        
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < recommendations.size) {
                        val (effectName, value, paramName) = recommendations[index]
                        RecommendationCard(effectName, paramName, value, color = getEffectColor(effectName), modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Recommendation Card ──
@Composable
private fun RecommendationCard(effectName: String, paramName: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A22)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    effectName.take(1),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                effectName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF1F1F5)
            )
            
            Text(
                paramName,
                fontSize = 10.sp,
                color = Color(0xFF8888A0)
            )
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ── Instructions ──
@Composable
private fun Instructions() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        
        Text(
            "About Tone Matcher:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F1F5),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        InstructionStep("1", "Load a WAV audio file", "Choose any guitar tone sample")
        InstructionStep("2", "Analyze the sample", "System analyzes pitch, harmonics, and texture")
        InstructionStep("3", "Get recommendations", "AI suggests optimal effect settings")
        
        Spacer(Modifier.height(20.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3A).copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "* Tone Matcher analyzes spectral content to automatically tune effects for perfect match with your tone",
                fontSize = 11.sp,
                color = Color(0xFFF1F1F5),
                modifier = Modifier.padding(12.dp),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun InstructionStep(step: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF22D3EE)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A0A0E)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFF1F1F5)
            )
            Text(
                description,
                fontSize = 12.sp,
                color = Color(0xFF8888A0)
            )
        }
    }
}
