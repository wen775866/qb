package com.example.model

data class PlayerProfile(
    val nickname: String = "斗地主赌神",
    val avatarEmoji: String = "🤠",
    val coins: Long = 68888,
    val diamonds: Int = 88,
    val vipLevel: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val totalMatches: Int = 36,
    val wins: Int = 24,
    val highestMultiplier: Int = 64,
    val bombsPlayed: Int = 42,
    val springsAchieved: Int = 5,
    val currentStreak: Int = 3,
    val maxStreak: Int = 7,
    val isCheckedInToday: Boolean = false
) {
    val winRate: Int
        get() = if (totalMatches > 0) (wins * 100 / totalMatches) else 0
}

data class DoudizhuRoom(
    val id: String,
    val name: String,
    val baseScore: Int,
    val minCoins: Long,
    val maxMultiplier: Int,
    val badge: String,
    val description: String
) {
    companion object {
        val ROOMS = listOf(
            DoudizhuRoom("novice", "新手初级场", 100, 1000, 32, "🌱", "适合新手入门，轻松对局"),
            DoudizhuRoom("elite", "精英中级场", 500, 5000, 64, "🔥", "牌技切磋，翻倍激斗"),
            DoudizhuRoom("master", "雀圣大师场", 2000, 20000, 128, "👑", "高手过招，一掷千金"),
            DoudizhuRoom("bomb", "激情连炸场", 1000, 10000, 256, "💣", "炸弹连击，倍数飙升")
        )
    }
}
