package com.example.ui.lobby

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoudizhuRoom
import com.example.model.PlayerProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ImperialDarkRed
import com.example.ui.theme.ImperialRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMedium
import kotlinx.coroutines.launch

@Composable
fun DoudizhuLobby(
    profile: PlayerProfile,
    onProfileUpdate: (PlayerProfile) -> Unit,
    onSelectRoom: (DoudizhuRoom) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showStatsDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showCheckInDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceDark,
                        TableFeltDark,
                        Color(0xFF0D1E16)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: User Profile Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile & Avatar Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x77000000))
                        .border(1.dp, Color(0x33FFD700), RoundedCornerShape(24.dp))
                        .clickable { showStatsDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("profile_card_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(AmberGold, Color(0xFFB8860B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = profile.avatarEmoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.nickname,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(ImperialRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "VIP ${profile.vipLevel}",
                                    color = AmberGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "胜率: ${profile.winRate}% (${profile.wins}/${profile.totalMatches})",
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp
                        )
                    }
                }

                // Currency Bar (Coins & Diamonds)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x88000000), RoundedCornerShape(16.dp))
                            .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "💰 ${profile.coins}",
                            color = AmberGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                onProfileUpdate(profile.copy(coins = profile.coins + 5000))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("领取破产救济金 +5,000 金币！")
                                }
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AmberGold)
                                .testTag("claim_coins_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "增加金币",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Audio switch
                    IconButton(
                        onClick = {
                            onProfileUpdate(profile.copy(soundEnabled = !profile.soundEnabled))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = if (profile.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "声音开关",
                            tint = if (profile.soundEnabled) AmberGold else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Banner Quick Benefit Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily Check-in Card
                Card(
                    onClick = {
                        if (!profile.isCheckedInToday) {
                            val reward = 8888L
                            onProfileUpdate(
                                profile.copy(
                                    coins = profile.coins + reward,
                                    isCheckedInToday = true
                                )
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("每日签到成功！获得 $reward 金币！")
                            }
                        } else {
                            showCheckInDialog = true
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (profile.isCheckedInToday) SurfaceCardElevated else ImperialDarkRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("daily_checkin_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "每日签到",
                            tint = AmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (profile.isCheckedInToday) "今日已签到" else "每日签到领金币",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (profile.isCheckedInToday) "明日再来" else "+8,888 金币",
                                color = AmberGold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Rule Codex Card
                Card(
                    onClick = { showRulesDialog = true },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lobby_rules_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "规则指南",
                            tint = BrightGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "斗地主百科",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "牌型·炸弹·春天",
                                color = Color(0xFFB0BEC5),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Room Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 选择对战赛场",
                    color = AmberGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "万人在线 实时人机切磋",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            // Room Selection List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(DoudizhuRoom.ROOMS, key = { it.id }) { room ->
                    RoomItemCard(
                        room = room,
                        userCoins = profile.coins,
                        onSelect = { onSelectRoom(room) }
                    )
                }
            }

            // Bottom Quick Start Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        // Quick join highest suitable room
                        val suitable = DoudizhuRoom.ROOMS.filter { profile.coins >= it.minCoins }.lastOrNull()
                            ?: DoudizhuRoom.ROOMS.first()
                        onSelectRoom(suitable)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .testTag("quick_start_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "快速开始",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "快速开局 · 立即对决",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )

        // Player Stats Dialog
        if (showStatsDialog) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Leaderboard, contentDescription = null, tint = AmberGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("个人雀坛战绩", color = AmberGold, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("昵称：${profile.nickname}", color = Color.White)
                        Text("总局数：${profile.totalMatches} 局", color = Color.White)
                        Text("胜场数：${profile.wins} 局 (胜率 ${profile.winRate}%)", color = AmberGold)
                        Text("当前连胜：${profile.currentStreak} 局 (历史最高 ${profile.maxStreak} 连胜)", color = Color.White)
                        Text("打出炸弹：${profile.bombsPlayed} 次", color = Color.White)
                        Text("打出春天：${profile.springsAchieved} 次", color = Color.White)
                        Text("最高倍数记录：${profile.highestMultiplier} 倍", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatsDialog = false }) {
                        Text("确定", color = AmberGold)
                    }
                },
                containerColor = Color(0xFF1E252F),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Rules Guide Dialog
        if (showRulesDialog) {
            AlertDialog(
                onDismissRequest = { showRulesDialog = false },
                title = {
                    Text("欢乐斗地主 · 完整玩法说明", color = AmberGold, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎴 游戏流程：三人对局，发17张手牌并留3张底牌。叫抢地主胜出者拿底牌并成为地主，其余两人为农民同盟。", color = Color.White, fontSize = 12.sp)
                        Text("🃏 基础牌型：单张、对子、三张、三带一、三带二、单顺（5张起）、双顺/连对（3对起）、飞机带翅膀、四带二。", color = Color.White, fontSize = 12.sp)
                        Text("💣 炸弹与王炸：炸弹（四张相同点数）可压制除王炸外的任意牌型，倍数×2；大王+小王为【王炸】，最大无解，倍数×2！", color = AmberGold, fontSize = 12.sp)
                        Text("🌸 春天与反春：地主一把出完手牌且农民未出过一张牌为【春天】，倍数×2；农民打完且地主仅出了一手牌为【反春】，倍数×2！", color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRulesDialog = false }) {
                        Text("知道了", color = AmberGold)
                    }
                },
                containerColor = Color(0xFF1E252F),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showCheckInDialog) {
            AlertDialog(
                onDismissRequest = { showCheckInDialog = false },
                title = { Text("今日已签到", color = AmberGold) },
                text = { Text("您今天已经领取过每日金币奖励，明天记得再来领取丰厚奖励哦！", color = Color.White) },
                confirmButton = {
                    TextButton(onClick = { showCheckInDialog = false }) {
                        Text("好", color = AmberGold)
                    }
                },
                containerColor = Color(0xFF1E252F)
            )
        }
    }
}

@Composable
fun RoomItemCard(
    room: DoudizhuRoom,
    userCoins: Long,
    onSelect: () -> Unit
) {
    val canEnter = userCoins >= room.minCoins

    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (canEnter) SurfaceCard else Color(0xFF1C1F26)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (canEnter) Color(0x44FFD700) else Color(0x22FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .testTag("room_item_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF37474F), Color(0xFF1E272C))
                            )
                        )
                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = room.badge, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0x55000000), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color(0x66FFD700), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "封顶 ${room.maxMultiplier}倍",
                                color = AmberGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "底分: ${room.baseScore}  |  准入: ${room.minCoins}金币",
                        color = AmberGold.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = room.description,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canEnter) AmberGold else Color(0xFF37474F)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (canEnter) "入场" else "金币不足",
                    color = if (canEnter) Color.Black else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
