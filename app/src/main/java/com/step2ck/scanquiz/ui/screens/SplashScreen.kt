package com.step2ck.scanquiz.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// What phase the splash is in
sealed class SplashPhase {
    object Connecting : SplashPhase()           // Checking existing key
    object ReadyConnected : SplashPhase()        // Key found & verified — go!
    object NoKey : SplashPhase()                 // No key → show setup options
    data class KeyError(val msg: String) : SplashPhase()  // Key failed validation
}

@Composable
fun SplashScreen(
    phase: SplashPhase,
    onRetryConnect: () -> Unit,
    onPasteKey: (String) -> Unit,
    onOpenRouterGuide: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    // Pulsing dot animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── App Icon & Title ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Step 2 CK Companion",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Scan textbook pages · Generate board-style quizzes",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Main Phase Content ────────────────────────────────────────────
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "phase_anim"
            ) { currentPhase ->

                when (currentPhase) {

                    // ── CONNECTING ───────────────────────────────────────────
                    is SplashPhase.Connecting -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ConnectingDots(alpha = pulse)
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "Connecting to OpenRouter…",
                                color = Color(0xFFCBD5E1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Verifying your API key",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // ── READY ────────────────────────────────────────────────
                    is SplashPhase.ReadyConnected -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Connected!",
                                color = Color(0xFF4ADE80),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "OpenRouter is ready. Let's study.",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = onContinue,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Start Scanning", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // ── KEY ERROR ────────────────────────────────────────────
                    is SplashPhase.KeyError -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Connection Failed",
                                color = Color(0xFFF87171),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E0A0A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentPhase.msg,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRetryConnect,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry", fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { /* triggers NoKey path below by clearing */ onPasteKey("") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("New Key", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // ── NO KEY — Setup Wizard ─────────────────────────────────
                    is SplashPhase.NoKey -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Text(
                                text = "Connect to OpenRouter",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This app needs an OpenRouter API key to generate quizzes.\nFree tier gives you 50 AI quiz generations per day.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            // Option A: Paste key
                            SetupOptionCard(
                                icon = Icons.Default.Key,
                                iconColor = Color(0xFF6366F1),
                                title = "I already have a key",
                                subtitle = "Paste your sk-or-v1-... key below"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = keyInput,
                                        onValueChange = { keyInput = it },
                                        label = { Text("OpenRouter API Key") },
                                        placeholder = { Text("sk-or-v1-...", color = Color(0xFF475569)) },
                                        singleLine = true,
                                        visualTransformation = if (showKey) VisualTransformation.None
                                                               else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                keyboardController?.hide()
                                                if (keyInput.isNotBlank()) onPasteKey(keyInput.trim())
                                            }
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = { showKey = !showKey }) {
                                                Icon(
                                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color(0xFFCBD5E1),
                                            focusedBorderColor = Color(0xFF6366F1),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedLabelColor = Color(0xFF818CF8),
                                            unfocusedLabelColor = Color(0xFF64748B)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            if (keyInput.isNotBlank()) onPasteKey(keyInput.trim())
                                        },
                                        enabled = keyInput.trim().let {
                                            it.isNotBlank() && it.length > 10
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6366F1)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(46.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Connect & Verify Key", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Option B: Get a free key (step-by-step guide)
                            SetupOptionCard(
                                icon = Icons.Default.OpenInNew,
                                iconColor = Color(0xFF0EA5E9),
                                title = "I don't have a key yet",
                                subtitle = "Get a free one from openrouter.ai (takes 2 minutes)"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                    OutlinedButton(
                                        onClick = {
                                            showGuide = !showGuide
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(
                                            if (showGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (showGuide) "Hide guide" else "Show step-by-step guide",
                                            fontSize = 13.sp
                                        )
                                    }

                                    AnimatedVisibility(visible = showGuide) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StepRow(
                                                step = "1",
                                                text = "Open a browser and go to openrouter.ai"
                                            )
                                            StepRow(
                                                step = "2",
                                                text = "Tap 'Sign In' — create a free account with Google or email"
                                            )
                                            StepRow(
                                                step = "3",
                                                text = "Tap your profile icon → 'API Keys' → 'Create Key'"
                                            )
                                            StepRow(
                                                step = "4",
                                                text = "Name it anything (e.g. 'Step2CK App'), tap Create"
                                            )
                                            StepRow(
                                                step = "5",
                                                text = "Copy the key (starts with sk-or-v1-...)"
                                            )
                                            StepRow(
                                                step = "6",
                                                text = "Come back here, paste it in the field above and tap Connect"
                                            )

                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0C1A2E),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                                    )
                                                    Text(
                                                        text = "Free tier: 50 AI quiz generations per day at \$0 cost. No credit card required.",
                                                        color = Color(0xFF7DD3FC),
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Also offer OAuth button
                                    Button(
                                        onClick = onOpenRouterGuide,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0369A1)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Sign in with OpenRouter (Browser)", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Footer
            Text(
                text = "Keys are stored encrypted on your device only.\nNothing is sent to any server except OpenRouter.",
                color = Color(0xFF475569),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun ConnectingDots(alpha: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val dotAlpha = when (i) {
                0 -> alpha
                1 -> (alpha + 0.3f).coerceAtMost(1f)
                else -> (alpha + 0.6f).coerceAtMost(1f)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = dotAlpha))
            )
        }
    }
}

@Composable
private fun SetupOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(subtitle, color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun StepRow(step: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF1D4ED8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

