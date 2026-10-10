package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DoudizhuCombo
import com.example.engine.DoudizhuEngine
import com.example.engine.DoudizhuHandType
import com.example.model.DoudizhuRoom
import com.example.model.PlayingCard
import com.example.ui.components.CardBackView
import com.example.ui.components.GameOverDialog
import com.example.ui.components.GameTableScaffold
import com.example.ui.components.PlayerAvatarView
import com.example.ui.components.PlayingCardView
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ImperialDarkRed
import com.example.ui.theme.ImperialRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DoudizhuPhase {
    BIDDING,
    DOUBLING,
    PLAYING,
    GAME_OVER
}

@Composable
fun DoudizhuScreen(
    room: DoudizhuRoom,
    onBackToLobby: () -> Unit,
    onCoinsUpdate: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(DoudizhuPhase.BIDDING) }
    var currentTurn by remember { mutableIntStateOf(0) } // 0: User, 1: Bot Right, 2: Bot Left
    var landlordSeat by remember { mutableIntStateOf(-1) }
    var multiplier by remember { mutableIntStateOf(1) }

    var bottomCards by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var userHand by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var botRightHand by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }
    var botLeftHand by remember { mutableStateOf<List<PlayingCard>>(emptyList()) }

    var lastCombo by remember { mutableStateOf<DoudizhuCombo?>(null) }
    var lastPlayerSeat by remember { mutableIntStateOf(-1) }
    var passCount by remember { mutableIntStateOf(0) }

    // Spring detection counters
    var landlordPlayCount by remember { mutableIntStateOf(0) }
    var farmerPlayCount by remember { mutableIntStateOf(0) }

    // Speech bubbles
    var userBubble by remember { mutableStateOf<String?>(null) }
    var botRightBubble by remember { mutableStateOf<String?>(null) }
    var botLeftBubble by remember { mutableStateOf<String?>(null) }

    // Hint cycle tracker
    var hintIndex by remember { mutableIntStateOf(0) }

    // Quick chat dialog
    var showChatMenu by remember { mutableStateOf(false) }

    // Special effects VFX banner
    var vfxBannerText by remember { mutableStateOf<String?>(null) }

    // Settlement state
    var showSettlement by remember { mutableStateOf(false) }
    var isUserWin by remember { mutableStateOf(false) }
    var settlementTitle by remember { mutableStateOf("") }
    var coinsWon by remember { mutableLongStateOf(0L) }
    var settlementDetails by remember { mutableStateOf("") }

    fun triggerVfx(text: String) {
        vfxBannerText = text
        coroutineScope.launch {
            delay(1200)
            vfxBannerText = null
        }
    }

    fun startNewGame() {
        if (room.id == "bomb") {
            // Unshuffled deck: high clustering for bombs and straights!
            val (h0, h1, h2) = DoudizhuEngine.createUnshuffledDecks()
            userHand = h0
            botRightHand = h1
            botLeftHand = h2
            val deck = PlayingCard.createFull54Deck()
            bottomCards = deck.shuffled().take(3)
        } else {
            val deck = PlayingCard.createFull54Deck().shuffled()
            userHand = DoudizhuEngine.sortCards(deck.subList(0, 17))
            botRightHand = DoudizhuEngine.sortCards(deck.subList(17, 34))
            botLeftHand = DoudizhuEngine.sortCards(deck.subList(34, 51))
            bottomCards = deck.subList(51, 54)
        }

        landlordSeat = -1
        multiplier = 1
        currentTurn = 0
        phase = DoudizhuPhase.BIDDING
        lastCombo = null
        lastPlayerSeat = -1
        passCount = 0
        landlordPlayCount = 0
        farmerPlayCount = 0
        userBubble = null
        botRightBubble = null
        botLeftBubble = null
        hintIndex = 0
        showSettlement = false
        vfxBannerText = null
    }

    LaunchedEffect(Unit) {
        startNewGame()
    }

    // Bot AI turn handler
    fun processBotTurn() {
        coroutineScope.launch {
            delay(850)

            if (phase == DoudizhuPhase.BIDDING) {
                val hand = if (currentTurn == 1) botRightHand else botLeftHand
                val highCount = hand.count { it.rank.ddzWeight >= 14 }
                val willBid = highCount >= 3

                if (willBid) {
                    if (currentTurn == 1) botRightBubble = "抢地主!" else botLeftBubble = "抢地主!"
                    multiplier = minOf(room.maxMultiplier, multiplier * 2)
                    landlordSeat = currentTurn
                } else {
                    if (currentTurn == 1) botRightBubble = "不抢" else botLeftBubble = "不抢"
                }

                delay(600)
                if (currentTurn == 1) botRightBubble = null else botLeftBubble = null

                if (currentTurn == 2) {
                    val finalLandlord = if (landlordSeat == -1) 0 else landlordSeat
                    landlordSeat = finalLandlord
                    when (finalLandlord) {
                        0 -> userHand = DoudizhuEngine.sortCards(userHand + bottomCards)
                        1 -> botRightHand = DoudizhuEngine.sortCards(botRightHand + bottomCards)
                        2 -> botLeftHand = DoudizhuEngine.sortCards(botLeftHand + bottomCards)
                    }
                    // Doubling phase
                    phase = DoudizhuPhase.DOUBLING
                    currentTurn = 0
                } else {
                    currentTurn++
                }
            } else if (phase == DoudizhuPhase.DOUBLING) {
                // Bots decide doubling
                val hand = if (currentTurn == 1) botRightHand else botLeftHand
                if (hand.count { it.rank.ddzWeight >= 15 } >= 2) {
                    if (currentTurn == 1) botRightBubble = "加倍!" else botLeftBubble = "加倍!"
                    multiplier = minOf(room.maxMultiplier, multiplier * 2)
                } else {
                    if (currentTurn == 1) botRightBubble = "不加" else botLeftBubble = "不加"
                }
                delay(500)
                if (currentTurn == 1) botRightBubble = null else botLeftBubble = null

                if (currentTurn == 2) {
                    phase = DoudizhuPhase.PLAYING
                    currentTurn = landlordSeat
                } else {
                    currentTurn++
                }
            } else if (phase == DoudizhuPhase.PLAYING) {
                val currentHand = if (currentTurn == 1) botRightHand else botLeftHand
                val targetCombo = if (lastPlayerSeat == currentTurn) null else lastCombo

                val isBotLandlord = (currentTurn == landlordSeat)
                val isTeammate = !isBotLandlord && (lastPlayerSeat != landlordSeat && lastPlayerSeat != -1)
                val teammateCount = if (currentTurn == 1) botLeftHand.size else botRightHand.size

                val moveCards = DoudizhuEngine.findAiMove(
                    currentHand,
                    targetCombo,
                    isTeammate = isTeammate,
                    teammateCardCount = teammateCount
                )

                if (moveCards != null) {
                    val combo = DoudizhuEngine.parseCombo(moveCards)!!
                    lastCombo = combo
                    lastPlayerSeat = currentTurn
                    passCount = 0

                    if (isBotLandlord) landlordPlayCount++ else farmerPlayCount++

                    if (combo.type == DoudizhuHandType.BOMB) {
                        multiplier = minOf(room.maxMultiplier, multiplier * 2)
                        triggerVfx("💣 炸弹！倍数翻倍至 ${multiplier}倍！")
                    } else if (combo.type == DoudizhuHandType.ROCKET) {
                        multiplier = minOf(room.maxMultiplier, multiplier * 2)
                        triggerVfx("🚀 王炸！倍数翻倍至 ${multiplier}倍！")
                    } else if (combo.type == DoudizhuHandType.AIRPLANE || combo.type == DoudizhuHandType.AIRPLANE_WITH_WINGS) {
                        triggerVfx("✈️ 飞机带翅膀！")
                    }

                    if (currentTurn == 1) {
                        botRightHand = currentHand.filterNot { moveCards.contains(it) }
                        botRightBubble = combo.type.displayName
                        if (botRightHand.size in 1..2) botRightBubble = "🚨 只剩${botRightHand.size}张！"
                    } else {
                        botLeftHand = currentHand.filterNot { moveCards.contains(it) }
                        botLeftBubble = combo.type.displayName
                        if (botLeftHand.size in 1..2) botLeftBubble = "🚨 只剩${botLeftHand.size}张！"
                    }

                    // Check win
                    val remaining = if (currentTurn == 1) botRightHand.size else botLeftHand.size
                    if (remaining == 0) {
                        delay(600)
                        val botWonLandlord = (currentTurn == landlordSeat)
                        val userIsLandlord = (landlordSeat == 0)
                        val userWin = if (botWonLandlord) !userIsLandlord else !userIsLandlord

                        // Check Spring
                        var springApplied = false
                        if (botWonLandlord && farmerPlayCount == 0) {
                            multiplier = minOf(room.maxMultiplier, multiplier * 2)
                            springApplied = true
                            triggerVfx("🌸 春天来啦！农民一张未出！")
                        } else if (!botWonLandlord && landlordPlayCount == 1) {
                            multiplier = minOf(room.maxMultiplier, multiplier * 2)
                            springApplied = true
                            triggerVfx("🌸 反春！地主仅出一手牌！")
                        }

                        val winFactor = if (userIsLandlord) 2 else 1
                        val totalReward = room.baseScore.toLong() * multiplier * winFactor * (if (userWin) 1 else -1)

                        isUserWin = userWin
                        settlementTitle = if (botWonLandlord) "地主胜出！" else "农民同盟胜出！"
                        coinsWon = totalReward
                        settlementDetails = "底分 ${room.baseScore} × ${multiplier}倍 ${if (springApplied) "(含春天)" else ""}"
                        showSettlement = true
                        phase = DoudizhuPhase.GAME_OVER
                        onCoinsUpdate(totalReward)
                        return@launch
                    }
                } else {
                    passCount++
                    if (currentTurn == 1) botRightBubble = "要不起" else botLeftBubble = "要不起"
                    if (passCount >= 2) {
                        lastCombo = null
                        lastPlayerSeat = -1
                    }
                }

                delay(500)
                if (currentTurn == 1) botRightBubble = null else botLeftBubble = null

                currentTurn = (currentTurn + 1) % 3
            }
        }
    }

    LaunchedEffect(currentTurn, phase) {
        if ((currentTurn == 1 || currentTurn == 2) && phase != DoudizhuPhase.GAME_OVER) {
            processBotTurn()
        }
    }

    val ruleText = """
        【${room.name} · 规则】
        1. 发牌：每人17张手牌，留3张底牌。
        2. 叫抢地主：叫/抢地主成功者获3张底牌，倍数翻倍！
        3. 加倍阶段：地主确定后，每位玩家可选择加倍！
        4. 炸弹与王炸：炸弹倍数×2，王炸倍数×2！当前场次最高封顶 ${room.maxMultiplier}倍！
        5. 春天：地主一把出完手牌且农民未出过一张，或反春（地主仅出首手牌），倍数×2！
    """.trimIndent()

    GameTableScaffold(
        title = room.name,
        baseScore = room.baseScore,
        multiplier = multiplier,
        ruleExplanation = ruleText,
        onBackToLobby = onBackToLobby
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hole cards (底牌) at Top Center
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (phase == DoudizhuPhase.BIDDING) {
                    repeat(3) { CardBackView(width = 38.dp, height = 54.dp) }
                } else {
                    bottomCards.forEach { card ->
                        PlayingCardView(card = card, width = 38.dp, height = 54.dp)
                    }
                }
            }

            // Left Opponent (Bot Left - 阿强)
            PlayerAvatarView(
                name = "牌神·阿强",
                avatarEmoji = "🤖",
                roleTag = if (landlordSeat == 2) "地主" else if (landlordSeat != -1) "农民" else null,
                cardCount = botLeftHand.size,
                isActiveTurn = currentTurn == 2,
                speechBubbleText = botLeftBubble,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )

            // Right Opponent (Bot Right - 小华)
            PlayerAvatarView(
                name = "赌侠·小华",
                avatarEmoji = "🤠",
                roleTag = if (landlordSeat == 1) "地主" else if (landlordSeat != -1) "农民" else null,
                cardCount = botRightHand.size,
                isActiveTurn = currentTurn == 1,
                speechBubbleText = botRightBubble,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )

            // Special Effects Banner Overlay
            AnimatedVisibility(
                visible = vfxBannerText != null,
                enter = scaleIn(tween(250)) + fadeIn(),
                exit = scaleOut(tween(250)) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ImperialRed, AmberGold, ImperialRed)
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = vfxBannerText.orEmpty(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Center Table Discard / Active Combo Area
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lastCombo != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val sender = when (lastPlayerSeat) {
                            0 -> "您"
                            1 -> "小华"
                            else -> "阿强"
                        }
                        Text(
                            text = "[$sender]  ${lastCombo!!.type.displayName}",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                            lastCombo!!.cards.forEach { card ->
                                PlayingCardView(card = card, width = 46.dp, height = 66.dp)
                            }
                        }
                    }
                } else if (phase == DoudizhuPhase.PLAYING) {
                    Text(
                        text = if (currentTurn == 0) "轮到您出牌，任意牌型均可出" else "等待对手出牌...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            // Bottom Area: Player Controls & Cards
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Buttons
                if (currentTurn == 0 && phase == DoudizhuPhase.BIDDING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userBubble = "不抢"
                                currentTurn = 1
                                coroutineScope.launch {
                                    delay(500)
                                    userBubble = null
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.testTag("pass_bid_button")
                        ) {
                            Text("不抢")
                        }
                        Button(
                            onClick = {
                                userBubble = "抢地主!"
                                multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                landlordSeat = 0
                                currentTurn = 1
                                coroutineScope.launch {
                                    delay(500)
                                    userBubble = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            modifier = Modifier.testTag("accept_bid_button")
                        ) {
                            Text("抢地主 (×2)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (currentTurn == 0 && phase == DoudizhuPhase.DOUBLING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userBubble = "不加倍"
                                currentTurn = 1
                                coroutineScope.launch {
                                    delay(500)
                                    userBubble = null
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.testTag("no_double_button")
                        ) {
                            Text("不加倍")
                        }
                        Button(
                            onClick = {
                                userBubble = "超级加倍!"
                                multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                currentTurn = 1
                                triggerVfx("⚡ 加倍成功！倍数提升至 ${multiplier}倍！")
                                coroutineScope.launch {
                                    delay(500)
                                    userBubble = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            modifier = Modifier.testTag("double_button")
                        ) {
                            Text("超级加倍 (×2)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (currentTurn == 0 && phase == DoudizhuPhase.PLAYING) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pass button
                        if (lastCombo != null && lastPlayerSeat != 0) {
                            OutlinedButton(
                                onClick = {
                                    passCount++
                                    userBubble = "要不起"
                                    if (passCount >= 2) {
                                        lastCombo = null
                                        lastPlayerSeat = -1
                                    }
                                    currentTurn = 1
                                    coroutineScope.launch {
                                        delay(500)
                                        userBubble = null
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.testTag("ddz_pass_button")
                            ) {
                                Text("不出")
                            }
                        }

                        // Cyclic Hint button
                        Button(
                            onClick = {
                                val targetCombo = if (lastPlayerSeat == 0) null else lastCombo
                                val allMoves = DoudizhuEngine.findAllValidMoves(userHand, targetCombo)
                                if (allMoves.isNotEmpty()) {
                                    val chosen = allMoves[hintIndex % allMoves.size]
                                    hintIndex++
                                    userHand = userHand.map { c ->
                                        c.copy(isSelected = chosen.any { it.id == c.id })
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                            modifier = Modifier.testTag("ddz_hint_button")
                        ) {
                            Text("提示", color = AmberGold)
                        }

                        // Clear selection
                        OutlinedButton(
                            onClick = {
                                userHand = userHand.map { it.copy(isSelected = false) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("重选")
                        }

                        // Play Cards Button
                        Button(
                            onClick = {
                                val selected = userHand.filter { it.isSelected }
                                val combo = DoudizhuEngine.parseCombo(selected)
                                val targetCombo = if (lastPlayerSeat == 0) null else lastCombo

                                if (combo != null && DoudizhuEngine.canBeat(targetCombo, combo)) {
                                    lastCombo = combo
                                    lastPlayerSeat = 0
                                    passCount = 0

                                    if (landlordSeat == 0) landlordPlayCount++ else farmerPlayCount++

                                    if (combo.type == DoudizhuHandType.BOMB) {
                                        multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                        triggerVfx("💣 炸弹！倍数翻倍至 ${multiplier}倍！")
                                    } else if (combo.type == DoudizhuHandType.ROCKET) {
                                        multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                        triggerVfx("🚀 王炸！双倍爆炸至 ${multiplier}倍！")
                                    } else if (combo.type == DoudizhuHandType.AIRPLANE || combo.type == DoudizhuHandType.AIRPLANE_WITH_WINGS) {
                                        triggerVfx("✈️ 飞机带翅膀！")
                                    }

                                    userHand = userHand.filterNot { selected.contains(it) }
                                    userBubble = combo.type.displayName

                                    if (userHand.isEmpty()) {
                                        // User wins
                                        val winLandlord = (landlordSeat == 0)
                                        var springApplied = false
                                        if (winLandlord && farmerPlayCount == 0) {
                                            multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                            springApplied = true
                                            triggerVfx("🌸 春天来啦！倍数 x2！")
                                        } else if (!winLandlord && landlordPlayCount == 1) {
                                            multiplier = minOf(room.maxMultiplier, multiplier * 2)
                                            springApplied = true
                                            triggerVfx("🌸 反春！倍数 x2！")
                                        }

                                        val winFactor = if (winLandlord) 2 else 1
                                        val totalReward = room.baseScore.toLong() * multiplier * winFactor

                                        isUserWin = true
                                        settlementTitle = if (winLandlord) "地主独霸天下！" else "农民携手大捷！"
                                        coinsWon = totalReward
                                        settlementDetails = "底分 ${room.baseScore} × ${multiplier}倍 ${if (springApplied) "(含春天)" else ""}"
                                        showSettlement = true
                                        phase = DoudizhuPhase.GAME_OVER
                                        onCoinsUpdate(totalReward)
                                    } else {
                                        currentTurn = 1
                                    }
                                } else {
                                    userBubble = "不符合规则"
                                    coroutineScope.launch {
                                        delay(700)
                                        userBubble = null
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                            modifier = Modifier.testTag("ddz_play_button")
                        ) {
                            Text("出牌", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Quick chat icon
                        IconButton(
                            onClick = { showChatMenu = !showChatMenu },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .testTag("quick_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "快捷聊天",
                                tint = AmberGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // User Identity and Warning
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    if (landlordSeat == 0) {
                        Text(
                            text = "[地主] ",
                            color = ImperialRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (landlordSeat != -1) {
                        Text(
                            text = "[农民同盟] ",
                            color = Color(0xFF64B5F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "您 (剩余${userHand.size}张)",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    if (userHand.size in 1..2) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🚨 报警！",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // User Hand Cards Row (Scrollable / Overlapping)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy((-24).dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                ) {
                    items(userHand, key = { it.id }) { card ->
                        PlayingCardView(
                            card = card,
                            width = 54.dp,
                            height = 80.dp,
                            isLifted = card.isSelected,
                            onClick = {
                                userHand = userHand.map {
                                    if (it.id == card.id) it.copy(isSelected = !it.isSelected) else it
                                }
                            }
                        )
                    }
                }
            }

            // Quick Chat Popup Menu
            if (showChatMenu) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 120.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1E242C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        listOf(
                            "快点啊，等到花儿都谢了！",
                            "和你合作真是太愉快了！",
                            "你是MM还是GG？",
                            "一手好牌，怎么打都是赢！",
                            "炸弹在手，天下我有！"
                        ).forEach { phrase ->
                            Text(
                                text = phrase,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable {
                                        userBubble = phrase
                                        showChatMenu = false
                                        coroutineScope.launch {
                                            delay(1500)
                                            userBubble = null
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Settlement Dialog
            if (showSettlement) {
                GameOverDialog(
                    isWin = isUserWin,
                    title = settlementTitle,
                    coinsChanged = coinsWon,
                    details = settlementDetails,
                    onRestart = { startNewGame() },
                    onBackToLobby = onBackToLobby
                )
            }
        }
    }
}
