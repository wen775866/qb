package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkGold
import com.example.ui.theme.ImperialRed

@Composable
fun PlayerAvatarView(
    name: String,
    avatarEmoji: String,
    roleTag: String? = null,
    coins: Long? = null,
    cardCount: Int? = null,
    isActiveTurn: Boolean = false,
    speechBubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(if (isActiveTurn) 8.dp else 3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF37474F), Color(0xFF1E272C))
                            )
                        )
                        .border(
                            width = if (isActiveTurn) 2.5.dp else 1.5.dp,
                            color = if (isActiveTurn) AmberGold else Color(0xFF78909C),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarEmoji,
                        fontSize = 26.sp
                    )
                }

                // Role tag (e.g. 地主, 庄家)
                if (roleTag != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 6.dp)
                            .background(ImperialRed, RoundedCornerShape(4.dp))
                            .border(0.5.dp, AmberGold, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = roleTag,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Player name
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Card count badge or coins
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cardCount != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x99000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${cardCount}张",
                            color = if (cardCount <= 2) Color(0xFFFF5252) else AmberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (coins != null) {
                    Text(
                        text = "💰${coins / 1000}k",
                        color = AmberGold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Speech bubble popup
        AnimatedVisibility(
            visible = !speechBubbleText.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-32).dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp))
                    .border(1.dp, DarkGold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = speechBubbleText.orEmpty(),
                    color = Color(0xFF3E2723),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
