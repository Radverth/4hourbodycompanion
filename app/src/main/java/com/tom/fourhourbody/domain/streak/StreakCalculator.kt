package com.tom.fourhourbody.domain.streak

/**
 * A chain of days, with a floor under it.
 *
 * [current] is what you stand to lose today; [best] is the honest record, counted without
 * passes. [passesLeft] is the forgiveness still available this week.
 */
data class Chain(
    val current: Int,
    val best: Int,
    val passesUsed: Int,
    val passesLeft: Int
) {
    val atRisk: Boolean get() = current > 0
}

/**
 * Loss aversion is the strongest lever here — a chain you do not want to break pulls harder
 * than a reward you might earn. But a chain that resets to zero on one bad day is why people
 * delete habit apps, so a miss inside the last week is absorbed by a pass instead of ending
 * the run. The pass is spent automatically: asking someone to decide whether to "use a pass"
 * puts a decision at the exact moment their motivation is lowest.
 *
 * The record in [Chain.best] never uses passes, so it stays something to actually beat.
 */
object StreakCalculator {

    const val PASSES_PER_WEEK = 1

    /** Days over which a pass is available — matches the window the user sees as "this week". */
    private const val PASS_WINDOW_DAYS = 7

    /**
     * [metNewestFirst] is one entry per day, index 0 being today, walking backwards.
     *
     * Today counts only once it is met: an unfinished day is not a broken one, so the chain
     * holds until midnight rather than accusing you at breakfast.
     */
    fun chain(metNewestFirst: List<Boolean>, passesPerWeek: Int = PASSES_PER_WEEK): Chain {
        if (metNewestFirst.isEmpty()) {
            return Chain(current = 0, best = 0, passesUsed = 0, passesLeft = passesPerWeek)
        }

        var index = if (metNewestFirst[0]) 0 else 1
        var current = 0
        var passesUsed = 0

        while (index < metNewestFirst.size) {
            when {
                metNewestFirst[index] -> current++

                index < PASS_WINDOW_DAYS && passesUsed < passesPerWeek -> passesUsed++

                else -> break
            }
            index++
        }

        return Chain(
            current = current,
            best = maxOf(longestRun(metNewestFirst), current),
            passesUsed = passesUsed,
            passesLeft = passesPerWeek - passesUsed
        )
    }

    /** The record, counted strictly — no passes, so it stays worth beating. */
    private fun longestRun(met: List<Boolean>): Int {
        var best = 0
        var run = 0
        met.forEach { hit ->
            if (hit) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }
}
