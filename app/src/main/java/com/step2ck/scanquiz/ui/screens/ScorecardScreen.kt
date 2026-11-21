package com.step2ck.scanquiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.step2ck.scanquiz.quiz.QuizScorecard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    scorecard: QuizScorecard,
    isProUnlocked: Boolean,
    onExportAnki: () -> Unit,
    onReQuizWeakAreas: () -> Unit,
    onBackHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Scorecard & Review") },
                actions = {
                    IconButton(onClick = onBackHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overall Score Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${scorecard.scorePercentage}%",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (scorecard.scorePercentage >= 70) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                        Text(
                            text = "${scorecard.correctCount} of ${scorecard.totalQuestions} Questions Correct",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (scorecard.scorePercentage >= 70) "Solid Step 2 CK clinical mastery!" else "Targeted review recommended below.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 2. Weak-Area Diagnostic Attribution (Mapping missed concepts to source elements)
            if (scorecard.weakAreas.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Targeted Weak-Area Attribution",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Based on your mistakes, review the following specific textbook sections:",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                            Spacer(Modifier.height(10.dp))

                            for (area in scorecard.weakAreas) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = area.sourceLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = area.reviewRecommendation,
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Export to Anki & Re-Quiz Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onExportAnki,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isProUnlocked) "Export Missed Concepts to Anki (.apkg)" else "Export to Anki (.apkg) [Pro]",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scorecard.weakAreas.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onReQuizWeakAreas,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isProUnlocked) "Re-Quiz Weak Areas" else "Re-Quiz Weak Areas [Pro]",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 4. Detailed Question-by-Question Review with Rationales
            item {
                Text(
                    text = "Question-by-Question Review",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(scorecard.questions) { q ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Question ${q.id} • ${q.subtopic}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (q.isCorrect) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (q.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (q.isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (q.isCorrect) "Correct" else "Missed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (q.isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = q.questionStem,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        // Selected vs Correct info
                        Text(
                            text = "Your Answer: ${q.selectedAnswer ?: "None"} | Correct Answer: ${q.correctAnswer}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(Modifier.height(8.dp))

                        Text("Rationales:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        for (opt in q.options) {
                            val isCorrect = opt.letter.equals(q.correctAnswer, ignoreCase = true)
                            val isChosen = opt.letter.equals(q.selectedAnswer, ignoreCase = true)
                            val textColor = if (isCorrect) Color(0xFF16A34A) else if (isChosen) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    text = "${opt.letter}. ${opt.text}",
                                    fontWeight = if (isCorrect || isChosen) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = opt.explanation,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Source: ${q.sourceType} &mdash; ${q.sourceLabel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

