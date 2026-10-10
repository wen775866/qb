package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.DoudizhuRoom
import com.example.model.PlayerProfile
import com.example.ui.games.DoudizhuScreen
import com.example.ui.lobby.DoudizhuLobby
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceDark
                ) {
                    DoudizhuApp()
                }
            }
        }
    }
}

@Composable
fun DoudizhuApp() {
    var profile by remember { mutableStateOf(PlayerProfile()) }
    var selectedRoom by remember { mutableStateOf<DoudizhuRoom?>(null) }

    if (selectedRoom != null) {
        BackHandler {
            selectedRoom = null
        }

        DoudizhuScreen(
            room = selectedRoom!!,
            onBackToLobby = { selectedRoom = null },
            onCoinsUpdate = { delta ->
                val newCoins = maxOf(0L, profile.coins + delta)
                val newWins = if (delta > 0) profile.wins + 1 else profile.wins
                val newMatches = profile.totalMatches + 1
                val newStreak = if (delta > 0) profile.currentStreak + 1 else 0
                val newMaxStreak = maxOf(profile.maxStreak, newStreak)

                profile = profile.copy(
                    coins = newCoins,
                    wins = newWins,
                    totalMatches = newMatches,
                    currentStreak = newStreak,
                    maxStreak = newMaxStreak
                )
            }
        )
    } else {
        DoudizhuLobby(
            profile = profile,
            onProfileUpdate = { updated -> profile = updated },
            onSelectRoom = { room -> selectedRoom = room }
        )
    }
}
