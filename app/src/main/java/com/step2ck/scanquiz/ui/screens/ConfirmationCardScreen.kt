package com.step2ck.scanquiz.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.step2ck.scanquiz.scanner.AggregatedScanSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationCardScreen(
    summary: AggregatedScanSummary,
    isProUnlocked: Boolean,
    isGenerating: Boolean,
    isPrefetching: Boolean,
    onBack: () -> Unit,
    onGenerateQuiz: (includeTables: Boolean, includeFigures: Boolean, questionCount: Int) -> Unit
) {
    var includeTables by remember { mutableStateOf(summary.tableCount > 0) }
    var includeFigures by remember { mutableStateOf(summary.figureCount > 0) }
    var selectedQuestionCount by remember { mutableStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Scanned Elements") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isGenerating) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Detected Elements Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detected on ${summary.totalPages} Pages:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Prose Text", fontSize = 12.sp)
                                Text("${summary.totalTextChars} chars", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Tables", fontSize = 12.sp)
                                Text("${summary.tableCount} found", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Figures", fontSize = 12.sp)
                                Text("${summary.figureCount} found", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 2. Selective Extraction Toggles
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Extraction Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        // Include Tables Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include Tables as Markdown", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "Reconstructs row/col bounding boxes into minimal-token Markdown pipes.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = includeTables,
                                onCheckedChange = { includeTables = it },
                                enabled = summary.tableCount > 0 && !isGenerating
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Include Figures Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include Figures & Captions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "Crops detected figures (max 2 images, <=768px) and captions.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = includeFigures,
                                onCheckedChange = { includeFigures = it },
                                enabled = summary.figureCount > 0 && !isGenerating
                            )
                        }
                    }
                }

                // 3. Question Count Selection
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Question Block Size", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = selectedQuestionCount == 5,
                                onClick = { selectedQuestionCount = 5 },
                                label = { Text("5 Questions (Standard)") },
                                enabled = !isGenerating,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedQuestionCount == 10,
                                onClick = {
                                    if (isProUnlocked) {
                                        selectedQuestionCount = 10
                                    }
                                },
                                label = {
                                    Text(if (isProUnlocked) "10 Questions (Pro)" else "10 Qs (Pro Locked)")
                                },
                                enabled = isProUnlocked && !isGenerating,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Prefetch status indicator
                AnimatedVisibility(visible = isPrefetching && !isGenerating) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Quiz is being prepared in background…",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // 4. Generate Quiz CTA Button (Triggers single OpenRouter call) (Triggers single OpenRouter call)
                Button(
                    onClick = {
                        onGenerateQuiz(includeTables, includeFigures, selectedQuestionCount)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Generating Board-Style Quiz...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate $selectedQuestionCount-Question Step 2 CK Quiz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

