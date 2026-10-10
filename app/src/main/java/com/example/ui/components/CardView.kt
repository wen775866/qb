package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayingCard
import com.example.ui.theme.CardBlackSuit
import com.example.ui.theme.CardRedSuit
import com.example.ui.theme.CardWhite

@Composable
fun PlayingCardView(
    card: PlayingCard,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 80.dp,
    isLifted: Boolean = card.isSelected,
    onClick: (() -> Unit)? = null
) {
    val offsetY by animateDpAsState(
        targetValue = if (isLifted) (-14).dp else 0.dp,
        animationSpec = spring(),
        label = "liftAnimation"
    )

    val textColor = if (card.isRed) CardRedSuit else CardBlackSuit
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .width(width)
            .height(height)
            .shadow(if (isLifted) 8.dp else 3.dp, shape)
            .clip(shape)
            .background(CardWhite)
            .border(
                width = if (isLifted) 2.dp else 1.dp,
                color = if (isLifted) Color(0xFFFFB300) else Color(0xFFD6D9E0),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .testTag("card_${card.id}")
                        .clickable { onClick() }
                } else Modifier
            )
            .padding(3.dp)
    ) {
        if (card.isJoker) {
            // Joker display
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.displayText,
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.displaySymbol,
                    fontSize = 18.sp
                )
                Text(
                    text = card.displayText,
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(180f)
                )
            }
        } else {
            // Top-left rank & suit
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.displayText,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 13.sp
                )
                Text(
                    text = card.displaySymbol,
                    color = textColor,
                    fontSize = 11.sp,
                    lineHeight = 11.sp
                )
            }

            // Center large suit watermark
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.displaySymbol,
                    color = textColor.copy(alpha = 0.22f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom-right inverted rank & suit
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .rotate(180f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.displayText,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 11.sp
                )
                Text(
                    text = card.displaySymbol,
                    color = textColor,
                    fontSize = 9.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

@Composable
fun CardBackView(
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 68.dp
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF8B1A24),
                        Color(0xFF5A0D15)
                    )
                )
            )
            .border(1.5.dp, Color(0xFFFFD700), shape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.8.dp, Color(0x66FFD700), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎴",
                fontSize = 18.sp
            )
        }
    }
}
