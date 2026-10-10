package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ImperialRed
import com.example.ui.theme.TableFeltDark
import com.example.ui.theme.TableFeltMedium

@Composable
fun GameTableScaffold(
    title: String,
    baseScore: Int = 100,
    multiplier: Int = 1,
    ruleExplanation: String,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showRuleDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TableFeltMedium,
                        TableFeltDark,
                        Color(0xFF041E12)
                    )
                )
            )
    ) {
        // Table subtle felt texture oval line
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(1.5.dp, Color(0x33FFD700), RoundedCornerShape(28.dp))
        )

        // Top Navigation & Stats Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(
                onClick = onBackToLobby,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .testTag("back_to_lobby_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回大厅",
                    tint = Color.White
                )
            }

            // Game title & table info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0x88000000), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x44FFD700), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    color = AmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "底分:$baseScore",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${multiplier}倍",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick actions: sound & rules
            Row {
                IconButton(
                    onClick = { soundEnabled = !soundEnabled },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "声音开关",
                        tint = if (soundEnabled) AmberGold else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showRuleDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .testTag("game_rules_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "规则说明",
                        tint = AmberGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Main game content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
        ) {
            content()
        }

        // Rules explanation dialog
        if (showRuleDialog) {
            AlertDialog(
                onDismissRequest = { showRuleDialog = false },
                title = {
                    Text(
                        text = "$title · 规则玩法说明",
                        color = AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = ruleExplanation,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showRuleDialog = false }) {
                        Text("明白", color = AmberGold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF1E242B),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun GameOverDialog(
    isWin: Boolean,
    title: String,
    coinsChanged: Long,
    details: String,
    onRestart: () -> Unit,
    onBackToLobby: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = if (isWin) AmberGold else ImperialRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("play_again_button")
            ) {
                Text(
                    text = "再来一局",
                    color = if (isWin) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBackToLobby,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("game_over_back_button")
            ) {
                Text("返回大厅", color = Color.White)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWin) "🎉 获胜结算 🎉" else "💔 遗憾落败 💔",
                    color = if (isWin) AmberGold else Color(0xFFFF5252),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (coinsChanged >= 0) "+$coinsChanged 金币" else "$coinsChanged 金币",
                    color = if (coinsChanged >= 0) AmberGold else Color(0xFFFF5252),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = details,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        },
        containerColor = Color(0xFF1A1F29),
        shape = RoundedCornerShape(16.dp)
    )
}
