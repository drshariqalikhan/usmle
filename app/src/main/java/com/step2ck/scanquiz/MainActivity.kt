package com.step2ck.scanquiz

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.step2ck.scanquiz.auth.OpenRouterAuthManager
import com.step2ck.scanquiz.auth.QuotaInfo
import com.step2ck.scanquiz.auth.QuotaManager
import com.step2ck.scanquiz.debug.AppLogger
import com.step2ck.scanquiz.export.AnkiApkgExporter
import com.step2ck.scanquiz.llm.OpenRouterClient
import com.step2ck.scanquiz.llm.ValidatedQuiz
import com.step2ck.scanquiz.quiz.QuizEngine
import com.step2ck.scanquiz.quiz.QuizScorecard
import com.step2ck.scanquiz.quiz.data.QuestionEntity
import com.step2ck.scanquiz.quiz.data.QuizDatabase
import com.step2ck.scanquiz.quiz.data.QuizEntity
import com.step2ck.scanquiz.quiz.toQuestionModels
import com.step2ck.scanquiz.scanner.AggregatedScanSummary
import com.step2ck.scanquiz.scanner.DocumentScannerManager
import com.step2ck.scanquiz.scanner.LayoutDetector
import com.step2ck.scanquiz.ui.screens.*
import com.step2ck.scanquiz.ui.theme.Step2CKScanQuizTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    HOME,
    CONFIRMATION,
    EXAM_PLAYER,
    SCORECARD,
    PRO_PAYWALL,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val authManager by lazy { OpenRouterAuthManager(this) }
    private val quotaManager by lazy { QuotaManager(authManager) }
    private val scannerManager by lazy { DocumentScannerManager(this) }
    private val layoutDetector by lazy { LayoutDetector(this) }
    private val openRouterClient by lazy { OpenRouterClient(authManager, quotaManager) }
    private val quizEngine by lazy { QuizEngine() }
    private val database by lazy { QuizDatabase.getInstance(this) }
    private val ankiExporter by lazy { AnkiApkgExporter(this) }

    private lateinit var docScannerLauncher: ActivityResultLauncher<IntentSenderRequest>

    // Compose State
    private var currentScreen by mutableStateOf(AppScreen.HOME)
    private var quotaInfoState by mutableStateOf<QuotaInfo?>(null)
    private var isProUnlockedState by mutableStateOf(false)
    private var isTestModePro by mutableStateOf(false)
    private var recentQuizzesState by mutableStateOf<List<QuizEntity>>(emptyList())
    private var activeScanSummary by mutableStateOf<AggregatedScanSummary?>(null)
    private var isGeneratingQuiz by mutableStateOf(false)
    private var activeScorecard by mutableStateOf<QuizScorecard?>(null)
    private var examTitle by mutableStateOf("USMLE Step 2 CK Examination")
    private var examQuestions by mutableStateOf<List<com.step2ck.scanquiz.quiz.QuestionModel>>(emptyList())
    private var activeErrorDetails by mutableStateOf<String?>(null)

    // Background prefetch state
    private var prefetchJob: Job? = null
    private var prefetchResult by mutableStateOf<Result<ValidatedQuiz>?>(null)
    private var isPrefetching by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register ML Kit Document Scanner result callback
        docScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val pageUris = scannerManager.parseResult(result.resultCode, result.data)
            if (pageUris.isNotEmpty()) {
                lifecycleScope.launch {
                    val summary = layoutDetector.processPages(pageUris)
                    activeScanSummary = summary
                    currentScreen = AppScreen.CONFIRMATION
                    // Start background prefetch immediately with default settings
                    startPrefetch(summary)
                }
            }
        }

        // Handle initial OAuth deep link if launched via intent
        handleOAuthDeepLink(intent)

        // Observe recent quizzes from Room database
        lifecycleScope.launch {
            database.quizDao().getRecentQuizzesFlow(5).collectLatest { list ->
                recentQuizzesState = list
            }
        }

        // Refresh quota pre-check
        refreshQuota()

        setContent {
            Step2CKScanQuizTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            quotaInfo = quotaInfoState,
                            isProUnlocked = isProUnlockedState,
                            recentQuizzes = recentQuizzesState,
                            onConnectOpenRouter = { startOAuthLogin() },
                            onSaveManualApiKey = { key ->
                                authManager.saveApiKey(key)
                                Toast.makeText(this@MainActivity, "OpenRouter API Key saved!", Toast.LENGTH_SHORT).show()
                                refreshQuota()
                            },
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                            onStartScan = { launchDocumentScanner() },
                            onOpenPaywall = { currentScreen = AppScreen.PRO_PAYWALL },
                            onSelectRecentQuiz = { quizId -> loadSavedQuiz(quizId) }
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            authManager = authManager,
                            quotaManager = quotaManager,
                            isTestModePro = isTestModePro,
                            onToggleTestModePro = { enabled ->
                                isTestModePro = enabled
                                AppLogger.d("SETTINGS", "Test Mode Pro = $enabled")
                            },
                            onBack = {
                                refreshQuota()
                                currentScreen = AppScreen.HOME
                            }
                        )

                        AppScreen.CONFIRMATION -> activeScanSummary?.let { summary ->
                            ConfirmationCardScreen(
                                summary = summary,
                                isProUnlocked = isProUnlockedState || isTestModePro,
                                isGenerating = isGeneratingQuiz,
                                isPrefetching = isPrefetching,
                                onBack = {
                                    // Cancel any running prefetch when user goes back
                                    prefetchJob?.cancel()
                                    prefetchResult = null
                                    isPrefetching = false
                                    currentScreen = AppScreen.HOME
                                },
                                onGenerateQuiz = { includeTables, includeFigures, questionCount ->
                                    generateQuiz(summary, includeTables, includeFigures, questionCount)
                                }
                            )
                        } ?: run {
                            currentScreen = AppScreen.HOME
                        }

                        AppScreen.EXAM_PLAYER -> ExamPlayerScreen(
                            quizTitle = examTitle,
                            questions = examQuestions,
                            onFinishExam = { gradedQuestions ->
                                finishAndGradeQuiz(examTitle, gradedQuestions)
                            }
                        )

                        AppScreen.SCORECARD -> activeScorecard?.let { scorecard ->
                            ScorecardScreen(
                                scorecard = scorecard,
                                isProUnlocked = isProUnlockedState || isTestModePro,
                                onExportAnki = { exportToAnki(scorecard) },
                                onReQuizWeakAreas = { requizWeakAreas(scorecard) },
                                onBackHome = { currentScreen = AppScreen.HOME }
                            )
                        } ?: run {
                            currentScreen = AppScreen.HOME
                        }

                        AppScreen.PRO_PAYWALL -> ProPaywallScreen(
                            onBack = { currentScreen = AppScreen.HOME },
                            onPurchasePro = {
                                // Simulate one-time purchase success
                                isProUnlockedState = true
                                Toast.makeText(this, "Pro Unlocked! Unlimited history and Anki export enabled.", Toast.LENGTH_LONG).show()
                                currentScreen = AppScreen.HOME
                            },
                            onRestorePurchase = {
                                Toast.makeText(this, "No previous purchases found.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    activeErrorDetails?.let { errText ->
                        AlertDialog(
                            onDismissRequest = { activeErrorDetails = null },
                            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            title = { Text("Quiz Generation Error") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "OpenRouter returned an error. Tap 'Copy Error' to paste it into chat, or 'View Logs' for details:",
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                    ) {
                                        Text(
                                            text = errText,
                                            color = Color(0xFFF87171),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Step2CK Error", errText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(this@MainActivity, "Error details copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Copy Error")
                                }
                            },
                            dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        activeErrorDetails = null
                                        currentScreen = AppScreen.SETTINGS
                                    }) {
                                        Text("View Logs")
                                    }
                                    TextButton(onClick = { activeErrorDetails = null }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthDeepLink(intent)
    }

    private fun handleOAuthDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "step2ck" && uri.host == "oauth-callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                lifecycleScope.launch {
                    val result = authManager.exchangeCodeForApiKey(code)
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, "OpenRouter Connected!", Toast.LENGTH_SHORT).show()
                        refreshQuota()
                    } else {
                        Toast.makeText(this@MainActivity, "OAuth Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun startOAuthLogin() {
        val authUri = authManager.createAuthorizationUri()
        val browserIntent = Intent(Intent.ACTION_VIEW, authUri)
        startActivity(browserIntent)
    }

    private fun refreshQuota() {
        if (!authManager.isConnected()) return
        lifecycleScope.launch {
            val res = quotaManager.checkQuota()
            if (res.isSuccess) {
                quotaInfoState = res.getOrNull()
            }
        }
    }

    private fun launchDocumentScanner() {
        if (!authManager.isConnected()) {
            Toast.makeText(this, "Please connect OpenRouter first.", Toast.LENGTH_SHORT).show()
            startOAuthLogin()
            return
        }
        scannerManager.getStartScanIntent(
            onSuccess = { intentSenderRequest ->
                docScannerLauncher.launch(intentSenderRequest)
            },
            onFailure = { e ->
                Toast.makeText(this, "Scanner Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun generateQuiz(
        summary: AggregatedScanSummary,
        includeTables: Boolean,
        includeFigures: Boolean,
        questionCount: Int
    ) {
        // Check if prefetch already finished with matching defaults (tables=true, figures=true, 5q)
        val cached = prefetchResult
        val matchesDefaults = includeTables && includeFigures && questionCount == 5
        if (cached != null && matchesDefaults) {
            AppLogger.d("PREFETCH", "Using cached prefetch result instantly!")
            prefetchJob?.cancel()
            prefetchResult = null
            isPrefetching = false
            handleQuizResult(cached)
            return
        }

        // Cancel running prefetch — user chose different settings or prefetch wasn't ready
        if (prefetchJob?.isActive == true) {
            AppLogger.d("PREFETCH", "User settings differ from prefetch defaults — cancelling prefetch, doing fresh call.")
            prefetchJob?.cancel()
            prefetchResult = null
            isPrefetching = false
        }

        isGeneratingQuiz = true
        lifecycleScope.launch {
            val result = openRouterClient.generateQuiz(
                summary = summary,
                includeTables = includeTables,
                includeFigures = includeFigures,
                questionCount = questionCount
            )
            isGeneratingQuiz = false
            handleQuizResult(result)
        }
    }

    private fun handleQuizResult(result: Result<ValidatedQuiz>) {
        if (result.isSuccess) {
            val validatedQuiz = result.getOrThrow()
            examTitle = validatedQuiz.quizTitle
            examQuestions = validatedQuiz.toQuestionModels()
            currentScreen = AppScreen.EXAM_PLAYER
        } else {
            val errMsg = result.exceptionOrNull()?.message ?: "Unknown error"
            activeErrorDetails = errMsg
        }
    }

    /** Starts a background coroutine that fires the LLM call with default settings.
     *  Result is cached in [prefetchResult] and consumed instantly when the user taps Generate
     *  with matching settings (tables=on, figures=on, 5 questions). */
    private fun startPrefetch(summary: AggregatedScanSummary) {
        prefetchJob?.cancel()
        prefetchResult = null
        isPrefetching = true
        prefetchJob = lifecycleScope.launch {
            AppLogger.d("PREFETCH", "Background quiz prefetch started (defaults: tables=on, figures=on, 5q)…")
            val result = openRouterClient.generateQuiz(
                summary = summary,
                includeTables = true,
                includeFigures = true,
                questionCount = 5
            )
            isPrefetching = false
            prefetchResult = result
            if (result.isSuccess) {
                AppLogger.d("PREFETCH", "Prefetch complete! Quiz ready to display instantly.")
            } else {
                AppLogger.e("PREFETCH", "Prefetch failed: ${result.exceptionOrNull()?.message}")
                // Don't show error — user will retry by tapping Generate normally
            }
        }
    }

    private fun finishAndGradeQuiz(
        title: String,
        gradedQuestions: List<com.step2ck.scanquiz.quiz.QuestionModel>
    ) {
        val scorecard = quizEngine.gradeQuiz(title, gradedQuestions)
        activeScorecard = scorecard

        // Persist to local SQLite Room DB
        lifecycleScope.launch(Dispatchers.IO) {
            val quizId = database.quizDao().insertQuiz(
                QuizEntity(
                    title = scorecard.quizTitle,
                    totalQuestions = scorecard.totalQuestions,
                    correctCount = scorecard.correctCount,
                    scorePercentage = scorecard.scorePercentage,
                    isProSession = isProUnlockedState || isTestModePro
                )
            )

            val questionEntities = scorecard.questions.map { q ->
                QuestionEntity(
                    quizId = quizId,
                    sourceLabel = q.sourceLabel,
                    sourceType = q.sourceType,
                    subtopic = q.subtopic,
                    vignette = q.vignette,
                    questionStem = q.questionStem,
                    optionsJson = q.options.joinToString(";") { "${it.letter}:${it.text}" },
                    correctAnswer = q.correctAnswer,
                    selectedAnswer = q.selectedAnswer,
                    isCorrect = q.isCorrect,
                    explanationsJson = q.options.joinToString(";") { "${it.letter}:${it.explanation}" }
                )
            }
            database.quizDao().insertQuestions(questionEntities)
        }

        currentScreen = AppScreen.SCORECARD
    }

    private fun exportToAnki(scorecard: QuizScorecard) {
        if (!isProUnlockedState && !isTestModePro) {
            Toast.makeText(this, "Anki Export is a Pro feature.", Toast.LENGTH_SHORT).show()
            currentScreen = AppScreen.PRO_PAYWALL
            return
        }

        val missed = scorecard.questions.filter { !it.isCorrect }
        if (missed.isEmpty()) {
            Toast.makeText(this, "100% Score! No missed questions to export.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val res = ankiExporter.exportMissedQuestionsToAnki(scorecard.quizTitle, missed)
            if (res.isSuccess) {
                val file = res.getOrThrow()
                val shareIntent = ankiExporter.createShareIntent(file)
                startActivity(Intent.createChooser(shareIntent, "Import into AnkiDroid"))
            } else {
                Toast.makeText(this@MainActivity, "Export failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requizWeakAreas(scorecard: QuizScorecard) {
        if (!isProUnlockedState && !isTestModePro) {
            currentScreen = AppScreen.PRO_PAYWALL
            return
        }
        val missed = scorecard.questions.filter { !it.isCorrect }
        if (missed.isEmpty()) return
        examTitle = "Re-Quiz: Weak Areas (${scorecard.quizTitle})"
        examQuestions = missed.map { it.copy(selectedAnswer = null) }
        currentScreen = AppScreen.EXAM_PLAYER
    }

    private fun loadSavedQuiz(quizId: Long) {
        lifecycleScope.launch {
            val questions = withContext(Dispatchers.IO) {
                database.quizDao().getQuestionsForQuiz(quizId)
            }
            if (questions.isNotEmpty()) {
                val models = questions.map { q ->
                    val opts = q.optionsJson.split(";").mapNotNull { part ->
                        val tokens = part.split(":", limit = 2)
                        if (tokens.size == 2) {
                            com.step2ck.scanquiz.quiz.OptionItem(
                                letter = tokens[0],
                                text = tokens[1],
                                explanation = ""
                            )
                        } else null
                    }
                    com.step2ck.scanquiz.quiz.QuestionModel(
                        id = q.id.toInt(),
                        sourceType = q.sourceType,
                        sourceLabel = q.sourceLabel,
                        subtopic = q.subtopic,
                        vignette = q.vignette,
                        questionStem = q.questionStem,
                        options = opts,
                        correctAnswer = q.correctAnswer,
                        selectedAnswer = q.selectedAnswer
                    )
                }
                activeScorecard = quizEngine.gradeQuiz("Past Quiz Review", models)
                currentScreen = AppScreen.SCORECARD
            }
        }
    }
}

