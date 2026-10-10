<template>
  <div class="app-root">
    <Doudizhu
      v-if="currentRoom"
      :room="currentRoom"
      @back-to-lobby="currentRoom = null"
      @update-coins="handleCoinsUpdate"
    />
    <Lobby
      v-else
      :profile="profile"
      @update-profile="handleProfileUpdate"
      @select-room="handleSelectRoom"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import Lobby from './components/Lobby.vue';
import Doudizhu from './games/Doudizhu.vue';

const STORAGE_KEY = 'doudizhu_player_profile';

const defaultProfile = {
  nickname: '斗地主赌神',
  avatarEmoji: '🤠',
  coins: 68888,
  diamonds: 88,
  vipLevel: 3,
  totalMatches: 36,
  wins: 24,
  highestMultiplier: 64,
  bombsPlayed: 42,
  springsAchieved: 5,
  currentStreak: 3,
  maxStreak: 7,
  isCheckedInToday: false
};

function loadProfile() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...defaultProfile, ...JSON.parse(raw) };
  } catch (e) {
    // fallback
  }
  return { ...defaultProfile };
}

const profile = ref(loadProfile());
const currentRoom = ref(null);

watch(
  profile,
  (val) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(val));
    } catch (e) {
      // storage error ignored
    }
  },
  { deep: true }
);

function handleProfileUpdate(newProf) {
  profile.value = newProf;
}

function handleSelectRoom(room) {
  currentRoom.value = room;
}

function handleCoinsUpdate(delta) {
  const newCoins = Math.max(0, profile.value.coins + delta);
  const isWin = delta > 0;
  const newWins = isWin ? profile.value.wins + 1 : profile.value.wins;
  const newMatches = profile.value.totalMatches + 1;
  const newStreak = isWin ? profile.value.currentStreak + 1 : 0;
  const newMaxStreak = Math.max(profile.value.maxStreak, newStreak);

  profile.value = {
    ...profile.value,
    coins: newCoins,
    wins: newWins,
    totalMatches: newMatches,
    currentStreak: newStreak,
    maxStreak: newMaxStreak
  };
}
</script>

<style>
.app-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
