package com.tom.fourhourbody.data.reference

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A bundled reference document. Content lives in assets, never in hardcoded strings. */
enum class ReferenceDoc(val title: String, val fileName: String) {
    SLOW_CARB_RULES("Slow-Carb rules", "slow_carb_rules.md"),
    HYBRID_RULES("Hybrid rules", "hybrid_rules.md"),
    MEAL_PLAN("Current meal plan", "meal_plan.md"),
    SAUCES("Sauces reference", "sauces.md"),
    GRAB_AND_GO("Grab-and-go & soup", "grab_and_go.md"),
    DAMAGE_CONTROL("Damage control tactics", "damage_control.md"),
    SLEEP("Sleep reference", "sleep_reference.md")
}

/**
 * Reads reference content from assets, with an editable override in the app's own files
 * directory. Editing in-app writes the override, so rules and the meal plan can change
 * without a rebuild; reverting deletes it and falls back to the bundled text.
 */
class ReferenceRepository(private val context: Context) {

    private val overrideDir: File get() = File(context.filesDir, "reference")

    private fun overrideFile(doc: ReferenceDoc) = File(overrideDir, doc.fileName)

    suspend fun read(doc: ReferenceDoc): String = withContext(Dispatchers.IO) {
        val override = overrideFile(doc)
        if (override.exists()) {
            runCatching { override.readText() }.getOrElse { readAsset(doc) }
        } else {
            readAsset(doc)
        }
    }

    suspend fun isEdited(doc: ReferenceDoc): Boolean =
        withContext(Dispatchers.IO) { overrideFile(doc).exists() }

    suspend fun write(doc: ReferenceDoc, content: String) = withContext(Dispatchers.IO) {
        if (!overrideDir.exists()) overrideDir.mkdirs()
        overrideFile(doc).writeText(content)
    }

    suspend fun revert(doc: ReferenceDoc) = withContext(Dispatchers.IO) {
        overrideFile(doc).delete()
        Unit
    }

    private fun readAsset(doc: ReferenceDoc): String = runCatching {
        context.assets.open("reference/${doc.fileName}").bufferedReader().use { it.readText() }
    }.getOrElse { "Reference content unavailable (${doc.fileName})." }
}
