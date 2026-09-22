package com.tom.fourhourbody.domain.training

import kotlin.math.ceil
import kotlin.math.max

/**
 * One exercise's result within a session, as the progression rules see it.
 *
 * [previousWeightKg]/[previousTulSec] are that same exercise's last logged set — the
 * comparison [isPlateau] needs. Both are null on an exercise's first-ever session, when there
 * is nothing yet to compare against.
 */
data class ExerciseResult(
    val exerciseName: String,
    val weightKg: Double,
    val tulSec: Int,
    val previousWeightKg: Double? = null,
    val previousTulSec: Int? = null
) {
    /** The set ran past the book's 90-second ceiling — next time earns more weight. */
    val hitCeiling: Boolean get() = tulSec >= TrainingConstants.TUL_CEILING_SEC

    /**
     * No better than last time, at no more weight than last time. The book's progression rule
     * is "matching or bettering your time under load at an increasing resistance"; this is
     * that rule's negation — the signal that it is time to insert another rest day rather than
     * push the same gap harder.
     */
    val isPlateau: Boolean get() =
        previousWeightKg != null && previousTulSec != null &&
            weightKg <= previousWeightKg && tulSec < previousTulSec
}

/** What the rules concluded about a finished session. */
data class SessionEvaluation(
    val plateaued: Boolean,
    val plateauedOn: String?,
    /** Exercise name to next session's suggested weight — only the exercises that earned one. */
    val nextWeights: Map<String, Double>
)

/**
 * The book's progression and frequency rules. Pure functions, no Android and no database —
 * this is the layer the session player and the scheduler both defer to.
 */
object ProgressionEngine {

    /**
     * +5%, rounded *up* to the nearest 0.5 kg, since the rule sets a floor on the step and
     * rounding down would quietly undercut it.
     */
    fun suggestNextWeight(currentKg: Double): Double {
        if (currentKg <= 0.0) return 0.0
        return roundUpToHalfKg(currentKg * (1.0 + TrainingConstants.PROGRESSION_STEP_PERCENT))
    }

    fun roundUpToHalfKg(value: Double): Double = ceil(value * 2.0 - 1e-9) / 2.0

    /**
     * Evaluate a whole session. Nothing in the book supports cutting a session short over one
     * exercise's result, so every exercise always runs — this only decides what happens *after*.
     *
     * A plateau on any exercise closes the run: the book's frequency mechanism kicks in and
     * the next block trains on one more rest day. Independently, every exercise that crossed
     * the TUL ceiling earns a heavier next session, whether or not the session plateaued —
     * those two things are not the same signal.
     */
    fun evaluate(results: List<ExerciseResult>): SessionEvaluation {
        val plateaued = results.firstOrNull { it.isPlateau }
        val next = results
            .filter { it.hitCeiling }
            .associate { it.exerciseName to suggestNextWeight(it.weightKg) }
        return SessionEvaluation(
            plateaued = plateaued != null,
            plateauedOn = plateaued?.exerciseName,
            nextWeights = next
        )
    }

    /**
     * What to put in front of the next set for a single exercise: bumped weight if the last
     * attempt ran past the ceiling, the same weight otherwise — beat the clock, not the bar.
     */
    fun openingWeightFor(lastWeightKg: Double?, lastTulSec: Int?): Double? {
        if (lastWeightKg == null || lastTulSec == null) return null
        return if (lastTulSec >= TrainingConstants.TUL_CEILING_SEC) {
            suggestNextWeight(lastWeightKg)
        } else {
            lastWeightKg
        }
    }
}
