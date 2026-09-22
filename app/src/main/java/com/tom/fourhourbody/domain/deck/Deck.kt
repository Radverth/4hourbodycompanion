package com.tom.fourhourbody.domain.deck

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity

/**
 * How far a card has actually been played. Nothing here is awarded: a tier is a reading of
 * the log, and the number behind it travels with it on screen so it can always be checked.
 */
enum class CardTier(val label: String, val minPlays: Int) {
    UNPLAYED("Unplayed", 0),
    OPENING("Opening", 1),
    ESTABLISHED("Established", 4),
    CORNERSTONE("Cornerstone", 10);

    companion object {
        fun forPlays(plays: Int): CardTier = entries.last { plays >= it.minPlays }
    }
}

/**
 * One card: an exercise slot. [plays] counts sessions it has ever been logged in — a movement
 * you have run twenty times really is established, and the weight carries across runs, so this
 * only ever climbs.
 */
data class DeckCard(
    val id: String,
    val name: String,
    val inDeck: Boolean,
    val plays: Int,
    val detail: String,
    val gainKg: Double? = null
) {
    val tier: CardTier get() = CardTier.forPlays(plays)
}

data class Deck(val cards: List<DeckCard>) {
    val inDeck: List<DeckCard> get() = cards.filter { it.inDeck }
    val bench: List<DeckCard> get() = cards.filterNot { it.inDeck }

    /** Cards in the deck that have actually been run — the deck's real weight, not its size. */
    val played: Int get() = inDeck.count { it.plays > 0 }
}

object DeckBuilder {

    /**
     * @param logs every exercise log from a completed session, oldest first — the order is
     *   what makes the first and last weight meaningful.
     */
    fun build(
        configs: List<ExerciseConfigEntity>,
        logs: List<ExerciseLogEntity>
    ): Deck {
        val byName = logs.groupBy { it.exerciseName }
        val cards = configs.map { config ->
            val entries = byName[config.exerciseName].orEmpty()
            val plays = entries.map { it.sessionId }.distinct().size
            val gain = if (entries.size >= 2) {
                entries.last().weightKg - entries.first().weightKg
            } else {
                null
            }
            DeckCard(
                id = "exercise:${config.id}",
                name = config.exerciseName,
                inDeck = config.isActive,
                plays = plays,
                detail = when {
                    plays == 0 -> "${config.slotName} · never logged"
                    plays == 1 -> "${config.slotName} · 1 session"
                    else -> "${config.slotName} · $plays sessions"
                },
                gainKg = gain
            )
        }
        return Deck(cards)
    }
}
