package com.step2ck.scanquiz.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.step2ck.scanquiz.auth.OpenRouterAuthManager
import com.step2ck.scanquiz.auth.QuotaManager
import com.step2ck.scanquiz.debug.AppLogger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authManager: OpenRouterAuthManager,
    quotaManager: QuotaManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var inputKey by remember { mutableStateOf("") }
    var isCheckingQuota by remember { mutableStateOf(false) }
    var customModelInput by remember { mutableStateOf(authManager.getCustomModel() ?: "") }

    val currentKey = authManager.getApiKey()
    val maskedKey = if (!currentKey.isNullOrBlank()) {
        if (currentKey.length > 10) "${currentKey.take(8)}••••••••${currentKey.takeLast(4)}" else "••••••••"
    } else {
        "Not Configured"
    }

    // Auto-scroll logs to bottom on new log entry
    LaunchedEffect(AppLogger.entries.size) {
        if (AppLogger.entries.isNotEmpty()) {
            listState.animateScrollToItem(AppLogger.entries.size - 1)
        }
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Update OpenRouter API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your OpenRouter API key (sk-or-v1-...):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it },
                        label = { Text("OpenRouter Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputKey.isNotBlank()) {
                            authManager.saveApiKey(inputKey.trim())
                            Toast.makeText(context, "API Key updated!", Toast.LENGTH_SHORT).show()
                            showKeyDialog = false
                        }
                    },
                    enabled = inputKey.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Debug Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Step2CK App Logs", AppLogger.getAllLogs())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "All logs copied to clipboard!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. API Key Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OpenRouter API Key", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (authManager.isConnected()) "Connected" else "Missing",
                            color = if (authManager.isConnected()) Color(0xFF16A34A) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Key: $maskedKey",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                inputKey = ""
                                showKeyDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Change Key", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isCheckingQuota = true
                                scope.launch {
                                    val res = quotaManager.checkQuota()
                                    isCheckingQuota = false
                                    if (res.isSuccess) {
                                        val info = res.getOrThrow()
                                        Toast.makeText(context, "Key Valid! Daily free left: ${info.dailyRemaining}/50", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Quota check error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = authManager.isConnected() && !isCheckingQuota,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCheckingQuota) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Test Key", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Model Configuration Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model Priority & Fallback", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Default chain: Llama 3.3 70B :free -> Gemini 2.0 Flash :free -> Gemma 3 27B :free -> Qwen 2.5 72B :free -> openrouter/free",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = {
                            customModelInput = it
                            authManager.setCustomModel(it.ifBlank { null })
                        },
                        label = { Text("Override Model (Optional, e.g. google/gemini-2.0-flash-exp:free)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Event & Error Log Console
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Live Event & Error Log", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${AppLogger.entries.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Step2CK Logs", AppLogger.getAllLogs())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Logs copied! Paste into chat.", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy", fontSize = 12.sp)
                    }
                    TextButton(onClick = { AppLogger.clear() }) {
                        Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A), // Dark terminal slate
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 12.dp)
            ) {
                if (AppLogger.entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No events logged yet.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        items(AppLogger.entries) { logLine ->
                            val color = when {
                                logLine.contains("ERROR") -> Color(0xFFF87171) // Red
                                logLine.contains("SUCCESS") || logLine.contains("OK") -> Color(0xFF4ADE80) // Green
                                logLine.contains("HTTP") -> Color(0xFFFBBF24) // Yellow
                                else -> Color(0xFFE2E8F0) // Light slate
                            }
                            Text(
                                text = logLine,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

