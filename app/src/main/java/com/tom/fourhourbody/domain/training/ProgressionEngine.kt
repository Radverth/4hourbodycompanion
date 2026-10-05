package com.tom.fourhourbody.domain.training

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * The ladder a bodyweight exercise climbs instead of adding plates.
 *
 * On the push-up board a handle position change is the load change, and it is a much coarser
 * one than 5% — moving from standard to narrow is not a nudge. That is why the position step
 * asks for more evidence than a weight step does.
 */
enum class BoardPosition(val label: String) {
    WIDE("Wide"),
    STANDARD("Standard"),
    NARROW("Narrow"),
    DECLINE("Decline");

    fun next(): BoardPosition? = entries.getOrNull(ordinal + 1)

    companion object {
        fun from(label: String?): BoardPosition? =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }
    }
}

/**
 * What a set was performed against.
 *
 * Weight alone is not enough to make two sets comparable. The book is explicit that a seat an
 * inch or two out changes the leverage, and on the board the handle position *is* the load —
 * so the thing a TUL gets compared against is the pair, not the number.
 */
data class Load(val weightKg: Double, val position: String? = null) {

    private val normalisedPosition: String? get() = position?.trim()?.takeIf { it.isNotEmpty() }

    fun sameAs(other: Load): Boolean =
        abs(weightKg - other.weightKg) < 0.01 && normalisedPosition == other.normalisedPosition
}

/** The last set logged for an exercise — the only thing this session's TUL is judged against. */
data class PreviousSet(val load: Load, val tulSeconds: Int)

/** Where a set landed relative to the 60–90s window. */
enum class TulVerdict { BELOW_WINDOW, IN_WINDOW, ABOVE_WINDOW }

/** What to change for this exercise next session. */
sealed interface NextStep {

    /** Cleared the window: 5–10% more load. Both ends are given; the machine decides which. */
    data class Heavier(val fromKg: Double, val lowKg: Double, val highKg: Double) : NextStep

    /** Bodyweight equivalent of adding plates. */
    data class NextPosition(val from: String, val to: String) : NextStep

    /** Inside the window, or not yet clear of it often enough. Same load again. */
    data object Hold : NextStep

    /**
     * Under 60 seconds. Reported rather than acted on: the protocol specifies the increase
     * and says nothing about an automatic decrease, and inventing one would be this app
     * making up training advice.
     */
    data class BelowWindow(val tulSeconds: Int) : NextStep
}

/** One exercise's result, as the rules see it. */
data class ExerciseResult(
    val exerciseName: String,
    val load: Load,
    val tulSeconds: Int,
    val previous: PreviousSet? = null,
    val targetMinSec: Int = TrainingConstants.TARGET_TUL_MIN_SEC,
    val targetMaxSec: Int = TrainingConstants.TARGET_TUL_MAX_SEC,
    val isBodyweight: Boolean = false,
    /**
     * Consecutive sessions at this exact load, including this one, that cleared [targetMaxSec].
     * Derived from the log rather than stored, and only consulted for a position step.
     */
    val clearancesAtThisLoad: Int = 1
)

/** What the rules concluded about a finished — or abandoned — session. */
data class SessionEvaluation(
    val stalled: Boolean,
    val stalledOn: String?,
    /** Exercise name to what changes next session. Empty when the session stalled. */
    val steps: Map<String, NextStep>,
    val totalTulSeconds: Int
)

/**
 * Body by Science's progression and frequency rules. Pure functions, no Android and no
 * database — the session player and the scheduler both defer to this.
 */
object ProgressionEngine {

    /** A position step is a big jump, so it wants two clear sessions rather than one. */
    const val POSITION_CLEARANCES_REQUIRED = 2

    fun verdict(
        tulSeconds: Int,
        targetMinSec: Int = TrainingConstants.TARGET_TUL_MIN_SEC,
        targetMaxSec: Int = TrainingConstants.TARGET_TUL_MAX_SEC
    ): TulVerdict = when {
        tulSeconds > targetMaxSec -> TulVerdict.ABOVE_WINDOW
        tulSeconds < targetMinSec -> TulVerdict.BELOW_WINDOW
        else -> TulVerdict.IN_WINDOW
    }

    fun verdict(result: ExerciseResult): TulVerdict =
        verdict(result.tulSeconds, result.targetMinSec, result.targetMaxSec)

    /**
     * A stall is failing to match the previous TUL **at the same load** — and only then.
     *
     * This condition carries the whole rule. A shorter TUL on heavier weight is the protocol
     * working exactly as designed; that is what adding load does. Comparing across loads would
     * flag every successful increase as a stall, hand out a rest day for it, and teach you
     * that progressing is a mistake.
     *
     * With no previous set at this load there is nothing to fail against, so a first attempt
     * never stalls.
     */
    fun isStall(result: ExerciseResult): Boolean {
        val last = result.previous ?: return false
        if (!result.load.sameAs(last.load)) return false
        return result.tulSeconds < last.tulSeconds
    }

    /**
     * 5–10% more, each end rounded up to the nearest half kilo and forced to differ from the
     * current weight — on a light machine 5% can round to nothing, and a suggestion of "same
     * again" is not the step the protocol asked for.
     */
    fun weightStep(currentKg: Double): Pair<Double, Double> {
        if (currentKg <= 0.0) return 0.0 to 0.0
        val low = atLeastOneIncrementAbove(currentKg, TrainingConstants.PROGRESSION_PERCENT_LOW)
        val high = atLeastOneIncrementAbove(currentKg, TrainingConstants.PROGRESSION_PERCENT_HIGH)
        return low to max(high, low)
    }

    private fun atLeastOneIncrementAbove(currentKg: Double, percent: Double): Double {
        val stepped = roundUpToIncrement(currentKg * (1.0 + percent))
        return if (stepped > currentKg + 1e-9) {
            stepped
        } else {
            roundUpToIncrement(currentKg + TrainingConstants.WEIGHT_INCREMENT_KG)
        }
    }

    fun roundUpToIncrement(value: Double): Double {
        val step = TrainingConstants.WEIGHT_INCREMENT_KG
        return ceil(value / step - 1e-9) * step
    }

    /** What changes for this exercise next session. */
    fun nextStepFor(result: ExerciseResult): NextStep = when (verdict(result)) {
        TulVerdict.BELOW_WINDOW -> NextStep.BelowWindow(result.tulSeconds)

        TulVerdict.IN_WINDOW -> NextStep.Hold

        TulVerdict.ABOVE_WINDOW -> if (result.isBodyweight) {
            positionStep(result)
        } else {
            val (low, high) = weightStep(result.load.weightKg)
            NextStep.Heavier(fromKg = result.load.weightKg, lowKg = low, highKg = high)
        }
    }

    private fun positionStep(result: ExerciseResult): NextStep {
        if (result.clearancesAtThisLoad < POSITION_CLEARANCES_REQUIRED) return NextStep.Hold
        val current = BoardPosition.from(result.load.position) ?: return NextStep.Hold
        val next = current.next() ?: return NextStep.Hold
        return NextStep.NextPosition(from = current.label, to = next.label)
    }

    /**
     * Evaluate a whole session.
     *
     * A stall ends it: the caller stops there rather than running the remaining exercises, and
     * nothing gets a suggested increase, because the session the suggestion would apply to is
     * the one that just failed.
     *
     * Otherwise every exercise is judged on its own TUL. That is a real difference from the
     * previous protocol, where one short exercise held back every other lift's increase —
     * here the exercises are independent, because TUL is.
     */
    fun evaluate(results: List<ExerciseResult>): SessionEvaluation {
        val totalTul = results.sumOf { it.tulSeconds }
        val stalledResult = results.firstOrNull(::isStall)
        if (stalledResult != null) {
            return SessionEvaluation(
                stalled = true,
                stalledOn = stalledResult.exerciseName,
                steps = emptyMap(),
                totalTulSeconds = totalTul
            )
        }
        val steps = results
            .associate { it.exerciseName to nextStepFor(it) }
            .filterValues { it !is NextStep.Hold }
        return SessionEvaluation(
            stalled = false,
            stalledOn = null,
            steps = steps,
            totalTulSeconds = totalTul
        )
    }

    /**
     * The load to put in front of the next set: stepped up if the last one cleared the window,
     * otherwise exactly what was used last time. Repeating the load is the normal case, not a
     * failure — the window is where the protocol wants you to stay.
     */
    fun openingLoadFor(
        last: PreviousSet?,
        targetMaxSec: Int = TrainingConstants.TARGET_TUL_MAX_SEC,
        isBodyweight: Boolean = false,
        clearancesAtThisLoad: Int = 1
    ): Load? {
        if (last == null) return null
        if (last.tulSeconds <= targetMaxSec) return last.load
        if (isBodyweight) {
            if (clearancesAtThisLoad < POSITION_CLEARANCES_REQUIRED) return last.load
            val next = BoardPosition.from(last.load.position)?.next() ?: return last.load
            return last.load.copy(position = next.label)
        }
        return last.load.copy(weightKg = weightStep(last.load.weightKg).first)
    }
}
