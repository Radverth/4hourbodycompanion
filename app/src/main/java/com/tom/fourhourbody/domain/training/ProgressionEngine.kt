package com.tom.fourhourbody.domain.training

import kotlin.math.ceil
import kotlin.math.max

/** One exercise's result within a session, as the progression rules see it. */
data class ExerciseResult(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int
) {
    val hitTarget: Boolean get() = reps >= targetReps
    val repsShort: Int get() = max(0, targetReps - reps)
}

/** What the rules concluded about a finished (or abandoned) session. */
data class SessionEvaluation(
    val stalled: Boolean,
    val stalledOn: String?,
    /** Exercise name to next session's suggested weight. Only populated when nothing stalled. */
    val nextWeights: Map<String, Double>
)

/**
 * The book's progression and frequency rules. Pure functions, no Android and no database —
 * this is the layer the session player and the scheduler both defer to.
 */
object ProgressionEngine {

    /** More than one rep short of target. Exactly one rep short is not a stall. */
    fun isStall(reps: Int, targetReps: Int): Boolean =
        targetReps - reps > TrainingConstants.STALL_TOLERANCE_REPS

    fun isStall(result: ExerciseResult): Boolean = isStall(result.reps, result.targetReps)

    /**
     * +10 lb or +10%, whichever is greater. Rounded *up* to the nearest 0.5 kg, since the rule
     * sets a floor on the step and rounding down would quietly undercut it.
     */
    fun suggestNextWeight(currentKg: Double): Double {
        if (currentKg <= 0.0) return 0.0
        val stepped = max(
            currentKg + TrainingConstants.TEN_POUNDS_KG,
            currentKg * (1.0 + TrainingConstants.PROGRESSION_PERCENT)
        )
        return roundUpToHalfKg(stepped)
    }

    fun roundUpToHalfKg(value: Double): Double = ceil(value * 2.0 - 1e-9) / 2.0

    /**
     * Evaluate a whole session.
     *
     * If every exercise hit its target, each one gets a suggested increase for next time. If
     * any exercise came up more than a rep short, the session is stalled: no suggestions, and
     * the caller stops the session there rather than running the remaining exercises.
     */
    fun evaluate(results: List<ExerciseResult>): SessionEvaluation {
        val stalledResult = results.firstOrNull(::isStall)
        if (stalledResult != null) {
            return SessionEvaluation(
                stalled = true,
                stalledOn = stalledResult.exerciseName,
                nextWeights = emptyMap()
            )
        }
        val allHit = results.isNotEmpty() && results.all { it.hitTarget }
        val next = if (allHit) {
            results.associate { it.exerciseName to suggestNextWeight(it.weightKg) }
        } else {
            emptyMap()
        }
        return SessionEvaluation(stalled = false, stalledOn = null, nextWeights = next)
    }

    /**
     * What to put in front of the next set for a single exercise: last weight if the last
     * attempt fell short of target, the stepped-up weight if it hit.
     */
    fun openingWeightFor(lastWeightKg: Double?, lastReps: Int?, targetReps: Int): Double? {
        if (lastWeightKg == null || lastReps == null) return null
        return if (lastReps >= targetReps) suggestNextWeight(lastWeightKg) else lastWeightKg
    }
}
