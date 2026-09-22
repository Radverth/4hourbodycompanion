package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.domain.deck.CardTier
import com.tom.fourhourbody.domain.deck.DeckBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeckBuilderTest {

    private fun config(id: Long, name: String, active: Boolean = true) = ExerciseConfigEntity(
        id = id,
        slotName = "Slot $id",
        exerciseName = name,
        equipment = "Machine",
        isActive = active,
        orderIndex = id.toInt()
    )

    private fun log(sessionId: Long, name: String, kg: Double) = ExerciseLogEntity(
        sessionId = sessionId,
        exerciseName = name,
        equipment = "Machine",
        weightKg = kg,
        tulSec = 60
    )

    @Test
    fun `tiers come from plays and nothing else`() {
        assertEquals(CardTier.UNPLAYED, CardTier.forPlays(0))
        assertEquals(CardTier.OPENING, CardTier.forPlays(1))
        assertEquals(CardTier.OPENING, CardTier.forPlays(3))
        assertEquals(CardTier.ESTABLISHED, CardTier.forPlays(4))
        assertEquals(CardTier.ESTABLISHED, CardTier.forPlays(9))
        assertEquals(CardTier.CORNERSTONE, CardTier.forPlays(10))
        assertEquals(CardTier.CORNERSTONE, CardTier.forPlays(250))
    }

    @Test
    fun `an exercise is played once per session, not once per set`() {
        val deck = DeckBuilder.build(
            configs = listOf(config(1, "Leg press")),
            logs = listOf(
                log(1, "Leg press", 100.0),
                log(1, "Leg press", 100.0),
                log(2, "Leg press", 105.0)
            )
        )
        assertEquals(2, deck.cards.single().plays)
    }

    @Test
    fun `exercise gain runs from the first logged weight to the last`() {
        val deck = DeckBuilder.build(
            configs = listOf(config(1, "Leg press")),
            logs = listOf(
                log(1, "Leg press", 100.0),
                log(2, "Leg press", 110.0),
                log(3, "Leg press", 122.5)
            )
        )
        assertEquals(22.5, deck.cards.single().gainKg!!, 0.001)
    }

    @Test
    fun `a single logged session reports no gain rather than zero`() {
        val deck = DeckBuilder.build(
            configs = listOf(config(1, "Leg press")),
            logs = listOf(log(1, "Leg press", 100.0))
        )
        assertNull(deck.cards.single().gainKg)
    }

    @Test
    fun `a benched exercise is still a card, just not in the deck`() {
        val deck = DeckBuilder.build(
            configs = listOf(config(1, "Leg press"), config(2, "Calf raise", active = false)),
            logs = emptyList()
        )
        assertEquals(2, deck.cards.size)
        assertEquals(listOf(true, false), deck.cards.map { it.inDeck })
    }

    @Test
    fun `the deck splits into what is in play and what is benched`() {
        val deck = DeckBuilder.build(
            configs = listOf(config(1, "Leg press"), config(2, "Calf raise", active = false)),
            logs = listOf(log(1, "Leg press", 100.0))
        )
        assertEquals(1, deck.inDeck.size)
        assertEquals(1, deck.bench.size)
        assertEquals(1, deck.played)
    }
}
