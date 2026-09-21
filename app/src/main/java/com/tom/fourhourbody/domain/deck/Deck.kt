package com.tom.fourhourbody.domain.deck

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.domain.adherence.PillarAdherence

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

enum class CardKind { PILLAR, EXERCISE }

/**
 * One card. [plays] is the only thing that sets a tier, and what counts as a play differs by
 * kind — see [DeckBuilder].
 */
data class DeckCard(
    val id: String,
    val kind: CardKind,
    val name: String,
    val pillar: Pillar,
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

    fun of(kind: CardKind): List<DeckCard> = cards.filter { it.kind == kind }
}

/**
 * Builds the deck out of what is already stored.
 *
 * The two kinds of card count plays differently, and each is the honest measure for its kind:
 *
 *  - An **exercise** counts sessions it has ever been logged in. A movement you have run
 *    twenty times really is established, and the weight carries across runs, so this only
 *    ever climbs.
 *  - A **pillar** counts days it was actually run inside the adherence window. This one can
 *    fall, and that is deliberate: a pillar you have stopped running has stopped being a
 *    cornerstone, and a tier that could only ever climb would say otherwise.
 */
object DeckBuilder {

    fun pillarCards(
        adherence: List<PillarAdherence>,
        isEnabled: (Pillar) -> Boolean
    ): List<DeckCard> {
        val byPillar = adherence.associateBy { it.pillar }
        return Pillar.entries.map { pillar ->
            val stats = byPillar[pillar]
            DeckCard(
                id = "pillar:${pillar.name}",
                kind = CardKind.PILLAR,
                name = pillar.label,
                pillar = pillar,
                inDeck = isEnabled(pillar),
                plays = stats?.completed ?: 0,
                detail = stats?.detail ?: "Not being tracked"
            )
        }
    }

    /**
     * @param logs every exercise log from a completed session, oldest first — the order is
     *   what makes the first and last weight meaningful.
     */
    fun exerciseCards(
        configs: List<ExerciseConfigEntity>,
        logs: List<ExerciseLogEntity>
    ): List<DeckCard> {
        val byName = logs.groupBy { it.exerciseName }
        return configs.map { config ->
            val entries = byName[config.exerciseName].orEmpty()
            val plays = entries.map { it.sessionId }.distinct().size
            val gain = if (entries.size >= 2) {
                entries.last().weightKg - entries.first().weightKg
            } else {
                null
            }
            DeckCard(
                id = "exercise:${config.id}",
                kind = CardKind.EXERCISE,
                name = config.exerciseName,
                pillar = Pillar.TRAINING,
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
    }

    fun build(
        adherence: List<PillarAdherence>,
        isEnabled: (Pillar) -> Boolean,
        configs: List<ExerciseConfigEntity>,
        logs: List<ExerciseLogEntity>
    ): Deck = Deck(pillarCards(adherence, isEnabled) + exerciseCards(configs, logs))
}
