<template>
  <div class="app-root">
    <!-- Live Global Announcement Marquee -->
    <transition name="slide-down">
      <div v-if="activeAnnouncement" class="global-announcement-banner">
        <span class="bell-icon">📢</span>
        <span class="banner-text">{{ activeAnnouncement }}</span>
        <button class="dismiss-btn" @click="activeAnnouncement = ''">✕</button>
      </div>
    </transition>

    <!-- Floating Quick Bar (Chat & Telegram Admin) -->
    <aside class="floating-quick-bar">
      <button class="float-btn chat-btn" @click="showChatDrawer = true" title="公共聊天室">
        <span>💬</span>
        <span v-if="unreadChatCount > 0" class="badge">{{ unreadChatCount }}</span>
      </button>

      <button class="float-btn tg-btn" @click="showTelegramModal = true" title="Telegram 管理员">
        <span>✈️</span>
      </button>
    </aside>

    <!-- 1. Doudizhu Game View (AI 1 完善) -->
    <Doudizhu
      v-if="currentScreen === 'doudizhu' && currentRoom"
      :room="currentRoom"
      @back-to-lobby="backToLobby"
      @update-coins="handleCoinsUpdate"
    />

    <!-- 2. Majiang Game View (AI 2 预留) -->
    <Majiang
      v-else-if="currentScreen === 'majiang'"
      @back-to-lobby="backToLobby"
    />

    <!-- 3. Paodekuai Game View (AI 3 预留) -->
    <Paodekuai
      v-else-if="currentScreen === 'paodekuai'"
      @back-to-lobby="backToLobby"
    />

    <!-- 4. Shisanshui Game View (AI 4 预留) -->
    <Shisanshui
      v-else-if="currentScreen === 'shisanshui'"
      @back-to-lobby="backToLobby"
    />

    <!-- 5. Chudadi Game View (AI 5 预留) -->
    <Chudadi
      v-else-if="currentScreen === 'chudadi'"
      @back-to-lobby="backToLobby"
    />

    <!-- 6. Lobby Main View -->
    <Lobby
      v-else
      :profile="profile"
      @update-profile="handleProfileUpdate"
      @select-room="handleSelectDoudizhuRoom"
      @select-game="handleSelectGame"
    />

    <!-- Real-time Chat Drawer -->
    <ChatDrawer
      v-if="showChatDrawer"
      :messages="chatMessages"
      :current-user-id="profile.nickname"
      :current-user-name="profile.nickname"
      :current-user-avatar="profile.avatarEmoji"
      :room-id="currentRoom?.id || ''"
      @close="closeChatDrawer"
      @send-message="handleSendChatMessage"
    />

    <!-- Telegram Admin Modal -->
    <TelegramAdminModal
      v-if="showTelegramModal"
      @close="showTelegramModal = false"
      @grant-coins="handleAdminGrantCoins"
      @send-announcement="handleAdminBroadcast"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import Lobby from './components/Lobby.vue';
import Doudizhu from './games/Doudizhu.vue';
import Majiang from './games/Majiang.vue';
import Paodekuai from './games/Paodekuai.vue';
import Shisanshui from './games/Shisanshui.vue';
import Chudadi from './games/Chudadi.vue';
import ChatDrawer from './components/ChatDrawer.vue';
import TelegramAdminModal from './components/TelegramAdminModal.vue';
import { wsService } from './services/websocket.js';
import { soundService } from './services/sound.js';

const STORAGE_KEY = 'doudizhu_platform_profile';

const defaultProfile = {
  nickname: '雀圣赌神',
  avatarEmoji: '🤠',
  coins: 88888,
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
  } catch (e) {}
  return { ...defaultProfile };
}

const profile = ref(loadProfile());
const currentScreen = ref('lobby');
const currentRoom = ref(null);

const showChatDrawer = ref(false);
const showTelegramModal = ref(false);
const unreadChatCount = ref(0);
const activeAnnouncement = ref('');
const chatMessages = ref([
  {
    id: 1,
    type: 'system_announcement',
    sender: '系统通知',
    text: '欢迎来到欢乐棋牌合集！斗地主板块已开启，支持三人对决、连炸翻倍与实时聊天。',
    timestamp: Date.now() - 3600000
  },
  {
    id: 2,
    type: 'player_chat',
    sender: '牌神阿强',
    senderId: 'bot_2',
    avatar: '🤖',
    vipLevel: 4,
    text: '有人来连炸场切磋一下吗？我准备好了！',
    timestamp: Date.now() - 1200000
  }
]);

onMounted(() => {
  wsService.connect();

  // Listen to asynchronous broadcast messages from WebSocket Hub
  wsService.on('chat_message', (msg) => {
    chatMessages.value.push(msg);
    if (!showChatDrawer.value) {
      unreadChatCount.value++;
    }
  });

  wsService.on('system_announcement', (announcement) => {
    chatMessages.value.push(announcement);
    activeAnnouncement.value = announcement.text || announcement;
    soundService.playWin();
    setTimeout(() => {
      activeAnnouncement.value = '';
    }, 6000);
  });

  wsService.on('coin_gift', (gift) => {
    const amount = gift.amount || 10000;
    profile.value.coins += amount;
    activeAnnouncement.value = `🎉 恭喜获得 ${gift.reason || '管理员赠送'}：+${amount.toLocaleString()} 金币！`;
    soundService.playWin();
  });
});

watch(
  profile,
  (val) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(val));
    } catch (e) {}
  },
  { deep: true }
);

function handleProfileUpdate(newProf) {
  profile.value = newProf;
}

function handleSelectDoudizhuRoom(room) {
  currentRoom.value = room;
  currentScreen.value = 'doudizhu';
}

function handleSelectGame(gameId) {
  if (gameId === 'doudizhu') {
    currentScreen.value = 'doudizhu';
  } else {
    currentScreen.value = gameId;
  }
}

function backToLobby() {
  currentScreen.value = 'lobby';
  currentRoom.value = null;
}

function closeChatDrawer() {
  showChatDrawer.value = false;
  unreadChatCount.value = 0;
}

function handleSendChatMessage(payload) {
  const newMsg = {
    id: Date.now(),
    type: 'player_chat',
    senderId: payload.senderId,
    sender: payload.sender,
    avatar: payload.avatar,
    vipLevel: profile.value.vipLevel,
    text: payload.text,
    roomId: payload.roomId,
    timestamp: Date.now()
  };

  chatMessages.value.push(newMsg);

  // Send via WebSocket async broadcast (with HTTP fallback)
  const sent = wsService.sendAction('doudizhu', payload.roomId || '', 'chat_message', payload);
  if (!sent) {
    fetch('/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newMsg)
    }).catch(() => {});
  }
}

function handleAdminGrantCoins(gift) {
  const amt = gift.amount || 50000;
  profile.value.coins += amt;
  soundService.playWin();
}

function handleAdminBroadcast(text) {
  const announcement = {
    id: Date.now(),
    type: 'system_announcement',
    sender: '系统管理员',
    text,
    timestamp: Date.now()
  };
  chatMessages.value.push(announcement);
  activeAnnouncement.value = text;
  soundService.playWin();
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

  // Report high win to Telegram Bot
  if (isWin && Math.abs(delta) >= 3000) {
    fetch('/api/game/report_win', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        playerName: profile.value.nickname,
        roomName: currentRoom.value?.name || '斗地主',
        multiplier: Math.min(128, Math.round(Math.abs(delta) / 100)),
        coins: delta,
        isSpring: false
      })
    }).catch(() => {});
  }
}
</script>

<style>
.app-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* Global Announcement Banner */
.global-announcement-banner {
  position: fixed;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(90deg, #b71c1c, #ff6f00, #b71c1c);
  border: 1.5px solid #ffd54f;
  color: #fff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.7);
  z-index: 999;
  max-width: 90%;
}

.bell-icon {
  font-size: 16px;
}

.banner-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dismiss-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}

/* Floating Action Bar */
.floating-quick-bar {
  position: fixed;
  right: 14px;
  bottom: 80px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 100;
}

.float-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(20, 25, 22, 0.9);
  border: 1.5px solid rgba(255, 215, 0, 0.5);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  position: relative;
  transition: transform 0.15s;
}

.float-btn:hover {
  transform: scale(1.08);
}

.float-btn .badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #ff5252;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
  padding: 1px 5px;
  border-radius: 10px;
  border: 1px solid #fff;
}

.tg-btn {
  border-color: #29b6f6;
}
</style>
