package com.step2ck.scanquiz.export

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.FileProvider
import com.step2ck.scanquiz.quiz.QuestionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Offline Anki .apkg deck generator.
 * Creates standard Anki 2.0 SQLite collection database and bundles it
 * into a valid .apkg zip archive without any external server or network dependency.
 */
class AnkiApkgExporter(private val context: Context) {

    /**
     * Exports missed questions into an Anki .apkg deck and triggers the Android Share Sheet.
     */
    suspend fun exportMissedQuestionsToAnki(
        deckTitle: String,
        missedQuestions: List<QuestionModel>
    ): Result<File> = withContext(Dispatchers.IO) {
        if (missedQuestions.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No missed questions to export."))
        }

        try {
            val exportDir = File(context.cacheDir, "apkg").apply { mkdirs() }
            val dbFile = File(exportDir, "collection.anki2")
            if (dbFile.exists()) dbFile.delete()

            // 1. Create SQLite database for collection.anki2
            val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
            createAnkiSchema(db, deckTitle)
            populateAnkiNotesAndCards(db, missedQuestions)
            db.close()

            // 2. Package database and media into .apkg zip archive
            val apkgFile = File(exportDir, "Step2CK_Review_${System.currentTimeMillis()}.apkg")
            if (apkgFile.exists()) apkgFile.delete()

            val zipOut = ZipOutputStream(FileOutputStream(apkgFile))

            // Add collection.anki2
            val dbEntry = ZipEntry("collection.anki2")
            zipOut.putNextEntry(dbEntry)
            val dbIn = FileInputStream(dbFile)
            dbIn.copyTo(zipOut)
            dbIn.close()
            zipOut.closeEntry()

            // Add media JSON map ({})
            val mediaEntry = ZipEntry("media")
            zipOut.putNextEntry(mediaEntry)
            zipOut.write("{}".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.close()
            dbFile.delete() // clean up intermediate SQLite file

            Result.success(apkgFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createAnkiSchema(db: SQLiteDatabase, deckTitle: String) {
        db.execSQL("""
            CREATE TABLE col (
                id integer primary key,
                crt integer not null,
                mod integer not null,
                scm integer not null,
                ver integer not null,
                dty integer not null,
                usn integer not null,
                ls integer not null,
                conf text not null,
                models text not null,
                decks text not null,
                dconf text not null,
                tags text not null
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE notes (
                id integer primary key,
                guid text not null,
                mid integer not null,
                mod integer not null,
                usn integer not null,
                tags text not null,
                flds text not null,
                sfld text not null,
                csum integer not null,
                flags integer not null,
                data text not null
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE cards (
                id integer primary key,
                nid integer not null,
                did integer not null,
                ord integer not null,
                mod integer not null,
                usn integer not null,
                type integer not null,
                queue integer not null,
                due integer not null,
                ivl integer not null,
                factor integer not null,
                reps integer not null,
                lapses integer not null,
                left integer not null,
                odue integer not null,
                odid integer not null,
                flags integer not null,
                data text not null
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE graves (
                usn integer not null,
                oid integer not null,
                type integer not null
            );
        """.trimIndent())

        val now = System.currentTimeMillis() / 1000
        val modelId = 1600000000000L
        val deckId = 1L

        val modelsJson = JSONObject().apply {
            put(modelId.toString(), JSONObject().apply {
                put("id", modelId)
                put("name", "Step 2 CK Clinical Vignette")
                put("type", 0)
                put("mod", now)
                put("usn", -1)
                put("sortf", 0)
                put("did", deckId)
                put("tmpls", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "Card 1")
                        put("ord", 0)
                        put("qfmt", "{{Front}}")
                        put("afmt", "{{FrontSide}}<hr id=answer>{{Back}}")
                        put("did", null)
                    })
                })
                put("flds", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "Front")
                        put("ord", 0)
                        put("sticky", false)
                        put("rtl", false)
                        put("font", "Arial")
                        put("size", 16)
                    })
                    put(JSONObject().apply {
                        put("name", "Back")
                        put("ord", 1)
                        put("sticky", false)
                        put("rtl", false)
                        put("font", "Arial")
                        put("size", 16)
                    })
                })
                put("css", ".card { font-family: arial; font-size: 16px; text-align: left; color: black; background-color: white; }")
            })
        }

        val decksJson = JSONObject().apply {
            put(deckId.toString(), JSONObject().apply {
                put("id", deckId)
                put("name", "USMLE Step 2 CK::$deckTitle")
                put("mod", now)
                put("usn", -1)
                put("desc", "High-yield Step 2 CK textbook review flashcards.")
            })
        }

        val insertColSql = """
            INSERT INTO col VALUES (
                1, ?, ?, ?, 11, 0, 0, 0, '{}', ?, ?, '{}', '{}'
            );
        """.trimIndent()

        db.execSQL(insertColSql, arrayOf(now, now, now, modelsJson.toString(), decksJson.toString()))
    }

    private fun populateAnkiNotesAndCards(db: SQLiteDatabase, questions: List<QuestionModel>) {
        val now = System.currentTimeMillis() / 1000
        val modelId = 1600000000000L
        val deckId = 1L

        for ((index, q) in questions.withIndex()) {
            val noteId = System.currentTimeMillis() + index * 10
            val cardId = noteId + 1
            val guid = UUID.randomUUID().toString().substring(0, 10)

            // Front: Clinical Vignette + Stem + Options A-E
            val frontHtml = buildString {
                append("<div style='font-size:17px; line-height:1.4;'><b>[USMLE Step 2 CK - ${q.subtopic}]</b><br><br>")
                append(q.vignette.replace("\n", "<br>"))
                append("<br><br><b>${q.questionStem}</b><br><br>")
                for (opt in q.options) {
                    append("<b>${opt.letter}.</b> ${opt.text}<br>")
                }
                append("</div>")
            }

            // Back: Correct Answer + Rationale + Distractor Explanations + Source Attribution
            val backHtml = buildString {
                append("<div style='font-size:16px;'>")
                append("<b style='color:#16A34A; font-size:18px;'>Correct Answer: ${q.correctAnswer}</b><br><br>")
                append("<b>Option Explanations:</b><br>")
                for (opt in q.options) {
                    val color = if (opt.letter.equals(q.correctAnswer, ignoreCase = true)) "#15803D" else "#64748B"
                    append("<p><b style='color:$color;'>${opt.letter}:</b> ${opt.explanation}</p>")
                }
                append("<hr><p style='font-size:13px; color:#475569;'><b>Source:</b> ${q.sourceType} &mdash; <i>${q.sourceLabel}</i></p>")
                append("</div>")
            }

            // Anki fields are separated by \u001f
            val flds = "$frontHtml\u001f$backHtml"
            val sfld = "[Step 2 CK] ${q.subtopic}"
            val tags = " USMLE_Step2_CK ${q.subtopic.replace(" ", "_")} ${q.sourceType} "

            db.execSQL(
                "INSERT INTO notes VALUES (?, ?, ?, ?, -1, ?, ?, ?, 0, 0, '');",
                arrayOf(noteId, guid, modelId, now, tags, flds, sfld)
            )

            db.execSQL(
                "INSERT INTO cards VALUES (?, ?, ?, 0, ?, -1, 0, 0, 0, 0, 2500, 0, 0, 0, 0, 0, 0, '');",
                arrayOf(cardId, noteId, deckId, now)
            )
        }
    }

    /**
     * Creates an Android Share Sheet intent so the student can send the deck directly to AnkiDroid.
     */
    fun createShareIntent(apkgFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkgFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/apkg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

