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
import com.kaonixx.guitarix.MainViewModel

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
                Icon(
                    imageVector = androidx.compose.material.icons.filled.MusicNote,
                    contentDescription = "Upload",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF22D3EE)
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
                
                Spacer(Modifier.height(20.dp))
                
                // File picker button (simplified - would need actual implementation)
                Button(
                    onClick = { /* Load audio file action */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22D3EE),
                        contentColor = Color(0xFF0A0A0E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "SELECT WAV FILE",
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
            "Distortion" to vm.toneMatcherRecommendedDistortionDrive to "Drive",
            "Distortion" to vm.toneMatcherRecommendedDistortionTone to "Tone",
            "Distortion" to vm.toneMatcherRecommendedDistortionLevel to "Level",
            "Amp Sim" to vm.toneMatcherRecommendedAmpSimGain to "Gain",
            "Amp Sim" to vm.toneMatcherRecommendedAmpSimTone to "Tone",
            "Amp Sim" to vm.toneMatcherRecommendedAmpSimMaster to "Master",
            "EQ" to vm.toneMatcherRecommendedEqBass to "Bass",
            "EQ" to vm.toneMatcherRecommendedEqMid to "Mid",
            "EQ" to vm.toneMatcherRecommendedEqTreble to "Treble",
            "Chorus" to vm.toneMatcherRecommendedChorusRate to "Rate",
            "Chorus" to vm.toneMatcherRecommendedChorusDepth to "Depth",
            "Chorus" to vm.toneMatcherRecommendedChorusMix to "Mix",
            "Delay" to vm.toneMatcherRecommendedDelayMix to "Mix",
            "Delay" to vm.toneMatcherRecommendedDelayFeedback to "Feedback",
            "Delay" to vm.toneMatcherRecommendedDelayTime to "Time",
            "Reverb" to vm.toneMatcherRecommendedReverbSize to "Size",
            "Reverb" to vm.toneMatcherRecommendedReverbMix to "Mix"
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
                        RecommendationCard(effectName, paramName, value, color = getEffectColor(effectName))
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
private fun RecommendationCard(effectName: String, paramName: String, value: Float, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
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

// ── Get effect color ──
private fun getEffectColor(effectName: String): Color {
    return when (effectName) {
        "Distortion" -> Color(0xFFFF6B6B)
        "Amp Sim" -> Color(0xFF4ECDC4)
        "EQ" -> Color(0xFF45B7D1)
        "Chorus" -> Color(0xFF96CEB4)
        "Delay" -> Color(0xFFF9E79F)
        "Reverb" -> Color(0xFFFFD700)
        else -> Color(0xFF22D3EE)
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
