package com.example.engine

import com.example.model.PlayingCard
import com.example.model.Rank
import com.example.model.Suit

enum class DoudizhuHandType(val power: Int, val displayName: String) {
    INVALID(0, "无效牌型"),
    SINGLE(1, "单张"),
    PAIR(2, "对子"),
    TRIPLET(3, "三张"),
    TRIPLET_WITH_ONE(4, "三带一"),
    TRIPLET_WITH_PAIR(5, "三带二"),
    STRAIGHT(6, "顺子"),
    CONSECUTIVE_PAIRS(7, "连对"),
    AIRPLANE(8, "飞机"),
    AIRPLANE_WITH_WINGS(9, "飞机带翅膀"),
    FOUR_WITH_TWO(10, "四带二"),
    BOMB(100, "炸弹"),
    ROCKET(1000, "王炸")
}

data class DoudizhuCombo(
    val type: DoudizhuHandType,
    val mainRankWeight: Int, // The primary rank used for comparison
    val length: Int, // Length for straights, airplane, etc.
    val cards: List<PlayingCard>
)

object DoudizhuEngine {

    fun sortCards(cards: List<PlayingCard>): List<PlayingCard> {
        return cards.sortedWith(
            compareByDescending<PlayingCard> { it.rank.ddzWeight }
                .thenByDescending { it.suit.orderWeight }
        )
    }

    // Creates an exciting "不洗牌" deal where cards stay somewhat clustered together, creating more bombs and straights!
    fun createUnshuffledDecks(): Triple<List<PlayingCard>, List<PlayingCard>, List<PlayingCard>> {
        val baseDeck = PlayingCard.createFull54Deck().toMutableList()
        // Cut deck into segments and re-interleave lightly instead of full random shuffle
        val chunks = baseDeck.chunked(4).shuffled()
        val mixed = chunks.flatten()
        val hand0 = sortCards(mixed.subList(0, 17))
        val hand1 = sortCards(mixed.subList(17, 34))
        val hand2 = sortCards(mixed.subList(34, 51))
        val bottom = mixed.subList(51, 54)
        return Triple(hand0, hand1, hand2)
    }

    fun parseCombo(cards: List<PlayingCard>): DoudizhuCombo? {
        val sorted = sortCards(cards)
        val size = sorted.size
        if (size == 0) return null

        // 1. Check Rocket (王炸)
        if (size == 2 && sorted.any { it.rank == Rank.BLACK_JOKER } && sorted.any { it.rank == Rank.RED_JOKER }) {
            return DoudizhuCombo(DoudizhuHandType.ROCKET, 100, 2, sorted)
        }

        // Group by rank
        val counts = sorted.groupBy { it.rank.ddzWeight }.mapValues { it.value.size }

        // 2. Single
        if (size == 1) {
            return DoudizhuCombo(DoudizhuHandType.SINGLE, sorted[0].rank.ddzWeight, 1, sorted)
        }

        // 3. Pair
        if (size == 2 && counts.size == 1) {
            return DoudizhuCombo(DoudizhuHandType.PAIR, sorted[0].rank.ddzWeight, 2, sorted)
        }

        // 4. Triplet
        if (size == 3 && counts.size == 1) {
            return DoudizhuCombo(DoudizhuHandType.TRIPLET, sorted[0].rank.ddzWeight, 3, sorted)
        }

        // 5. Bomb (4 cards of same rank)
        if (size == 4 && counts.size == 1) {
            return DoudizhuCombo(DoudizhuHandType.BOMB, sorted[0].rank.ddzWeight, 4, sorted)
        }

        // 6. Triplet with One
        if (size == 4 && counts.containsValue(3)) {
            val mainWeight = counts.entries.first { it.value == 3 }.key
            return DoudizhuCombo(DoudizhuHandType.TRIPLET_WITH_ONE, mainWeight, 4, sorted)
        }

        // 7. Triplet with Pair
        if (size == 5 && counts.containsValue(3) && counts.containsValue(2)) {
            val mainWeight = counts.entries.first { it.value == 3 }.key
            return DoudizhuCombo(DoudizhuHandType.TRIPLET_WITH_PAIR, mainWeight, 5, sorted)
        }

        // 8. Straight (顺子, 5+ consecutive, max rank cannot be 2 or Joker)
        if (size >= 5 && counts.values.all { it == 1 } && sorted.none { it.rank.ddzWeight >= 15 }) {
            val weights = sorted.map { it.rank.ddzWeight }
            var isConsecutive = true
            for (i in 0 until weights.size - 1) {
                if (weights[i] - weights[i + 1] != 1) {
                    isConsecutive = false
                    break
                }
            }
            if (isConsecutive) {
                return DoudizhuCombo(DoudizhuHandType.STRAIGHT, weights[0], size, sorted)
            }
        }

        // 9. Consecutive Pairs (双顺/连对, 3+ consecutive pairs, max cannot include 2 or Joker)
        if (size >= 6 && size % 2 == 0 && counts.values.all { it == 2 } && sorted.none { it.rank.ddzWeight >= 15 }) {
            val weights = counts.keys.sortedDescending()
            var isConsecutive = true
            for (i in 0 until weights.size - 1) {
                if (weights[i] - weights[i + 1] != 1) {
                    isConsecutive = false
                    break
                }
            }
            if (isConsecutive && weights.size >= 3) {
                return DoudizhuCombo(DoudizhuHandType.CONSECUTIVE_PAIRS, weights[0], size, sorted)
            }
        }

        // 10. Four with two (4+2 singles or 4+2 pairs)
        if (size == 6 && counts.containsValue(4)) {
            val mainWeight = counts.entries.first { it.value == 4 }.key
            return DoudizhuCombo(DoudizhuHandType.FOUR_WITH_TWO, mainWeight, 6, sorted)
        }
        if (size == 8 && counts.containsValue(4) && counts.values.count { it == 2 } == 2) {
            val mainWeight = counts.entries.first { it.value == 4 }.key
            return DoudizhuCombo(DoudizhuHandType.FOUR_WITH_TWO, mainWeight, 8, sorted)
        }

        // 11. Airplane (飞机 / 飞机带翅膀)
        val tripletWeights = counts.filter { it.value >= 3 && it.key < 15 }.keys.sortedDescending()
        if (tripletWeights.size >= 2) {
            var maxConsecutive = mutableListOf(tripletWeights[0])
            for (i in 0 until tripletWeights.size - 1) {
                if (tripletWeights[i] - tripletWeights[i + 1] == 1) {
                    maxConsecutive.add(tripletWeights[i + 1])
                } else {
                    if (maxConsecutive.size >= 2) break
                    maxConsecutive = mutableListOf(tripletWeights[i + 1])
                }
            }
            val numTriplets = maxConsecutive.size
            if (numTriplets >= 2) {
                // Pure airplane: size == numTriplets * 3
                if (size == numTriplets * 3) {
                    return DoudizhuCombo(DoudizhuHandType.AIRPLANE, maxConsecutive[0], size, sorted)
                }
                // Airplane with singles: size == numTriplets * 4
                if (size == numTriplets * 4) {
                    return DoudizhuCombo(DoudizhuHandType.AIRPLANE_WITH_WINGS, maxConsecutive[0], size, sorted)
                }
                // Airplane with pairs: size == numTriplets * 5
                if (size == numTriplets * 5 && counts.values.count { it == 2 } >= numTriplets) {
                    return DoudizhuCombo(DoudizhuHandType.AIRPLANE_WITH_WINGS, maxConsecutive[0], size, sorted)
                }
            }
        }

        return null
    }

    fun canBeat(lastCombo: DoudizhuCombo?, candidate: DoudizhuCombo): Boolean {
        if (lastCombo == null) return true // leading any valid combo
        // Rocket beats everything
        if (candidate.type == DoudizhuHandType.ROCKET) return true
        if (lastCombo.type == DoudizhuHandType.ROCKET) return false

        // Bomb beats any non-bomb, or smaller bomb
        if (candidate.type == DoudizhuHandType.BOMB) {
            if (lastCombo.type != DoudizhuHandType.BOMB) return true
            return candidate.mainRankWeight > lastCombo.mainRankWeight
        }

        // Same type and length required
        if (candidate.type != lastCombo.type || candidate.length != lastCombo.length) {
            return false
        }

        return candidate.mainRankWeight > lastCombo.mainRankWeight
    }

    // Finds all candidate moves for the hint button to cycle through
    fun findAllValidMoves(hand: List<PlayingCard>, lastCombo: DoudizhuCombo?): List<List<PlayingCard>> {
        val sorted = sortCards(hand)
        val results = mutableListOf<List<PlayingCard>>()

        if (lastCombo == null) {
            // Can lead anything: suggest smallest single, smallest pair, or smallest straight
            val counts = sorted.groupBy { it.rank.ddzWeight }
            // Singles
            sorted.distinctBy { it.rank.ddzWeight }.forEach { results.add(listOf(it)) }
            // Pairs
            counts.filter { it.value.size >= 2 }.forEach { (_, cards) ->
                results.add(cards.take(2))
            }
            // Triplet
            counts.filter { it.value.size >= 3 }.forEach { (_, cards) ->
                results.add(cards.take(3))
            }
            return results.take(5)
        }

        val counts = sorted.groupBy { it.rank.ddzWeight }

        when (lastCombo.type) {
            DoudizhuHandType.SINGLE -> {
                val singles = sorted.filter { it.rank.ddzWeight > lastCombo.mainRankWeight }
                    .distinctBy { it.rank.ddzWeight }
                singles.forEach { results.add(listOf(it)) }
            }
            DoudizhuHandType.PAIR -> {
                val pairs = counts.filter { it.value.size >= 2 && it.key > lastCombo.mainRankWeight }
                pairs.toSortedMap().forEach { (_, cards) ->
                    results.add(cards.take(2))
                }
            }
            DoudizhuHandType.TRIPLET, DoudizhuHandType.TRIPLET_WITH_ONE, DoudizhuHandType.TRIPLET_WITH_PAIR -> {
                val trips = counts.filter { it.value.size >= 3 && it.key > lastCombo.mainRankWeight }
                trips.toSortedMap().forEach { (k, cards) ->
                    val trip = cards.take(3)
                    if (lastCombo.type == DoudizhuHandType.TRIPLET) {
                        results.add(trip)
                    } else if (lastCombo.type == DoudizhuHandType.TRIPLET_WITH_ONE) {
                        val single = sorted.firstOrNull { it.rank.ddzWeight != k }
                        if (single != null) results.add(trip + single)
                    } else if (lastCombo.type == DoudizhuHandType.TRIPLET_WITH_PAIR) {
                        val pairCards = counts.entries.firstOrNull { it.key != k && it.value.size >= 2 }?.value?.take(2)
                        if (pairCards != null) results.add(trip + pairCards)
                    }
                }
            }
            DoudizhuHandType.BOMB -> {
                val bombs = counts.filter { it.value.size == 4 && it.key > lastCombo.mainRankWeight }
                bombs.toSortedMap().forEach { (_, cards) ->
                    results.add(cards)
                }
            }
            else -> {}
        }

        // Add bombs if not yet a rocket
        if (lastCombo.type != DoudizhuHandType.ROCKET) {
            val bombs = counts.filter { it.value.size == 4 && (lastCombo.type != DoudizhuHandType.BOMB || it.key > lastCombo.mainRankWeight) }
            bombs.toSortedMap().forEach { (_, cards) ->
                if (!results.contains(cards)) results.add(cards)
            }
        }

        // Add Rocket
        if (sorted.any { it.rank == Rank.BLACK_JOKER } && sorted.any { it.rank == Rank.RED_JOKER }) {
            val rocket = sorted.filter { it.suit == Suit.JOKER }
            results.add(rocket)
        }

        return results
    }

    // AI Bot smart decision
    fun findAiMove(
        hand: List<PlayingCard>,
        lastCombo: DoudizhuCombo?,
        isTeammate: Boolean = false,
        teammateCardCount: Int = 10
    ): List<PlayingCard>? {
        // If teammate is leading, and teammate has very few cards left (<=2), let them pass/win!
        if (isTeammate && teammateCardCount <= 2 && lastCombo != null) {
            return null // Pass to let teammate win!
        }

        val moves = findAllValidMoves(hand, lastCombo)
        if (moves.isEmpty()) return null

        // If leading: play the lowest combination
        if (lastCombo == null) {
            // Prefer lowest single or pair
            return moves.minByOrNull { m -> m.maxOf { it.rank.ddzWeight } }
        }

        // If following teammate: don't use bomb
        if (isTeammate) {
            val nonBombs = moves.filter {
                val parsed = parseCombo(it)
                parsed?.type != DoudizhuHandType.BOMB && parsed?.type != DoudizhuHandType.ROCKET
            }
            if (nonBombs.isNotEmpty()) {
                return nonBombs.first()
            }
            // Otherwise pass rather than bombing partner!
            return null
        }

        // Opponent: play smallest winning move
        return moves.firstOrNull()
    }
}
