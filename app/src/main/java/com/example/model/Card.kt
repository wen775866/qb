package com.example.model

enum class Suit(val symbol: String, val isRed: Boolean, val orderWeight: Int) {
    DIAMONDS("♦", true, 1),
    CLUBS("♣", false, 2),
    HEARTS("♥", true, 3),
    SPADES("♠", false, 4),
    JOKER("★", false, 5)
}

enum class Rank(val text: String, val ddzWeight: Int, val chudadiWeight: Int, val pokerWeight: Int) {
    THREE("3", 3, 3, 3),
    FOUR("4", 4, 4, 4),
    FIVE("5", 5, 5, 5),
    SIX("6", 6, 6, 6),
    SEVEN("7", 7, 7, 7),
    EIGHT("8", 8, 8, 8),
    NINE("9", 9, 9, 9),
    TEN("10", 10, 10, 10),
    JACK("J", 11, 11, 11),
    QUEEN("Q", 12, 12, 12),
    KING("K", 13, 13, 13),
    ACE("A", 14, 14, 14),
    TWO("2", 15, 15, 2),
    BLACK_JOKER("小王", 16, 0, 0),
    RED_JOKER("大王", 17, 0, 0);
}

data class PlayingCard(
    val id: Int,
    val suit: Suit,
    val rank: Rank,
    val isSelected: Boolean = false
) {
    val isJoker: Boolean get() = suit == Suit.JOKER
    val isRed: Boolean get() = if (isJoker) rank == Rank.RED_JOKER else suit.isRed

    val displayText: String
        get() = when (rank) {
            Rank.BLACK_JOKER -> "小王"
            Rank.RED_JOKER -> "大王"
            else -> rank.text
        }

    val displaySymbol: String
        get() = when (rank) {
            Rank.BLACK_JOKER -> "🃏"
            Rank.RED_JOKER -> "👑"
            else -> suit.symbol
        }

    companion object {
        fun createFull54Deck(): List<PlayingCard> {
            val list = mutableListOf<PlayingCard>()
            var id = 1
            for (rank in listOf(
                Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT,
                Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE, Rank.TWO
            )) {
                for (suit in listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)) {
                    list.add(PlayingCard(id++, suit, rank))
                }
            }
            list.add(PlayingCard(id++, Suit.JOKER, Rank.BLACK_JOKER))
            list.add(PlayingCard(id++, Suit.JOKER, Rank.RED_JOKER))
            return list
        }

        fun create52Deck(): List<PlayingCard> {
            val list = mutableListOf<PlayingCard>()
            var id = 1
            for (rank in listOf(
                Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX, Rank.SEVEN, Rank.EIGHT,
                Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE, Rank.TWO
            )) {
                for (suit in listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)) {
                    list.add(PlayingCard(id++, suit, rank))
                }
            }
            return list
        }

        // 跑得快经典16张 (48张牌: 去掉大小王，去掉3个2，去掉1个A，去掉1个K)
        fun createPaodekuaiDeck(): List<PlayingCard> {
            val full52 = create52Deck().toMutableList()
            // Keep Spades 2 only, remove Heart 2, Club 2, Diamond 2
            val toRemove = full52.filter {
                (it.rank == Rank.TWO && it.suit != Suit.SPADES) ||
                (it.rank == Rank.ACE && it.suit == Suit.DIAMONDS) ||
                (it.rank == Rank.KING && it.suit == Suit.DIAMONDS)
            }
            full52.removeAll(toRemove)
            return full52.take(48)
        }
    }
}
