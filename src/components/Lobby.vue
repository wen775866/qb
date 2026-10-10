<template>
  <div class="lobby-container">
    <!-- Top Bar -->
    <header class="lobby-header">
      <div class="user-pill" @click="showStats = true">
        <div class="avatar-circle">{{ profile.avatarEmoji }}</div>
        <div class="user-meta">
          <div class="name-row">
            <span class="user-name">{{ profile.nickname }}</span>
            <span class="vip-badge">VIP {{ profile.vipLevel }}</span>
          </div>
          <span class="win-rate">胜率: {{ winRate }}% ({{ profile.wins }}/{{ profile.totalMatches }})</span>
        </div>
      </div>

      <div class="header-right">
        <div class="coins-pill">
          <span class="coin-icon">💰</span>
          <span class="coin-val">{{ formattedCoins }}</span>
          <button class="add-coin-btn" @click="claimRelief" title="领取破产补助">+</button>
        </div>

        <button class="icon-btn" @click="toggleSound" :title="soundEnabled ? '静音' : '开启声音'">
          <span v-if="soundEnabled">🔊</span>
          <span v-else>🔇</span>
        </button>

        <button class="icon-btn" @click="showRules = true" title="规则说明">
          <span>📖</span>
        </button>
      </div>
    </header>

    <!-- Daily Bonus Card -->
    <section class="banner-section">
      <div class="checkin-banner" :class="{ 'claimed': profile.isCheckedInToday }" @click="handleCheckIn">
        <div class="banner-icon">🎁</div>
        <div class="banner-text">
          <h4>{{ profile.isCheckedInToday ? '今日已签到' : '每日签到领金币' }}</h4>
          <p>{{ profile.isCheckedInToday ? '明天再来领取新奖励' : '点击即领 +8,888 金币' }}</p>
        </div>
        <button class="banner-btn">{{ profile.isCheckedInToday ? '已领取' : '立即签到' }}</button>
      </div>
    </section>

    <!-- Rooms Title -->
    <div class="section-title">
      <span class="title-icon">🏆</span>
      <h3>选择对战赛场</h3>
      <span class="sub-text">真人牌感 · 智能AI对战</span>
    </div>

    <!-- Rooms Grid -->
    <div class="rooms-list">
      <div
        v-for="room in rooms"
        :key="room.id"
        class="room-card"
        :class="{ 'locked': profile.coins < room.minCoins }"
        @click="selectRoom(room)"
      >
        <div class="room-badge">{{ room.badge }}</div>
        <div class="room-info">
          <div class="room-name-row">
            <span class="room-name">{{ room.name }}</span>
            <span class="multiplier-tag">封顶 {{ room.maxMultiplier }}倍</span>
          </div>
          <div class="room-details">
            <span>底分: {{ room.baseScore }}</span>
            <span class="dot">·</span>
            <span>准入: {{ room.minCoins }}金币</span>
          </div>
          <p class="room-desc">{{ room.desc }}</p>
        </div>
        <button class="room-action-btn" :disabled="profile.coins < room.minCoins">
          {{ profile.coins >= room.minCoins ? '进入' : '金币不足' }}
        </button>
      </div>
    </div>

    <!-- Quick Play Button -->
    <footer class="lobby-footer">
      <button class="quick-play-btn" @click="quickStart">
        <span>⚡ 快速开局 · 立即对决</span>
      </button>
    </footer>

    <!-- Stats Modal -->
    <div v-if="showStats" class="modal-backdrop" @click="showStats = false">
      <div class="modal-card" @click.stop>
        <div class="modal-header">
          <h3>📊 个人雀坛战绩</h3>
          <button class="close-btn" @click="showStats = false">✕</button>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="label">总对局</span>
            <span class="val">{{ profile.totalMatches }} 局</span>
          </div>
          <div class="stat-item">
            <span class="label">胜场</span>
            <span class="val highlight">{{ profile.wins }} 局</span>
          </div>
          <div class="stat-item">
            <span class="label">胜率</span>
            <span class="val gold">{{ winRate }}%</span>
          </div>
          <div class="stat-item">
            <span class="label">当前连胜</span>
            <span class="val">{{ profile.currentStreak }} 局</span>
          </div>
          <div class="stat-item">
            <span class="label">最高连胜</span>
            <span class="val">{{ profile.maxStreak }} 局</span>
          </div>
          <div class="stat-item">
            <span class="label">打出炸弹</span>
            <span class="val">{{ profile.bombsPlayed }} 次</span>
          </div>
          <div class="stat-item">
            <span class="label">春天次数</span>
            <span class="val">{{ profile.springsAchieved }} 次</span>
          </div>
          <div class="stat-item">
            <span class="label">历史最高倍数</span>
            <span class="val red">{{ profile.highestMultiplier }} 倍</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Rules Modal -->
    <div v-if="showRules" class="modal-backdrop" @click="showRules = false">
      <div class="modal-card" @click.stop>
        <div class="modal-header">
          <h3>📖 欢乐斗地主·玩法百科</h3>
          <button class="close-btn" @click="showRules = false">✕</button>
        </div>
        <div class="rules-content">
          <div class="rule-block">
            <h4>1. 游戏规则与身份</h4>
            <p>三人使用一副牌（54张）。开局每人发17张牌，留3张底牌。通过叫抢地主确定地主，地主拿底牌独战两位农民。</p>
          </div>
          <div class="rule-block">
            <h4>2. 常见牌型</h4>
            <p>单张、对子、三张、三带一、三带二、顺子（5张起）、双顺/连对（3对起）、飞机带翅膀、四带二。</p>
          </div>
          <div class="rule-block">
            <h4>3. 炸弹与王炸</h4>
            <p>炸弹（四张同点数）可压制所有常规牌型，倍数翻倍！大王+小王为【王炸】，全场最大，倍数翻倍！</p>
          </div>
          <div class="rule-block">
            <h4>4. 春天与反春</h4>
            <p>地主一把牌打完且农民未出过一张为【春天】；农民打完且地主只出过一手牌为【反春】。均享倍数翻倍奖励！</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div v-if="toastMsg" class="toast-popup">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { soundService } from '../services/sound.js';

const props = defineProps({
  profile: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['update-profile', 'select-room']);

const showStats = ref(false);
const showRules = ref(false);
const toastMsg = ref('');
const soundEnabled = ref(soundService.enabled);

const rooms = [
  { id: 'novice', name: '新手初级场', baseScore: 100, minCoins: 1000, maxMultiplier: 32, badge: '🌱', desc: '新手友好，低倍起航' },
  { id: 'elite', name: '精英中级场', baseScore: 500, minCoins: 5000, maxMultiplier: 64, badge: '🔥', desc: '高手过招，倍数翻腾' },
  { id: 'master', name: '雀圣大师场', baseScore: 2000, minCoins: 20000, maxMultiplier: 128, badge: '👑', desc: '一掷千金，巅峰切磋' },
  { id: 'bomb', name: '激情连炸场', baseScore: 1000, minCoins: 10000, maxMultiplier: 256, badge: '💣', desc: '不洗牌高爆率，畅爽轰炸' }
];

const winRate = computed(() => {
  if (!props.profile.totalMatches) return 0;
  return Math.round((props.profile.wins / props.profile.totalMatches) * 100);
});

const formattedCoins = computed(() => {
  return props.profile.coins.toLocaleString();
});

function showToast(msg) {
  toastMsg.value = msg;
  setTimeout(() => {
    toastMsg.value = '';
  }, 2200);
}

function toggleSound() {
  soundEnabled.value = !soundEnabled.value;
  soundService.enabled = soundEnabled.value;
  showToast(soundEnabled.value ? '音效已开启' : '音效已静音');
}

function claimRelief() {
  const newProfile = { ...props.profile, coins: props.profile.coins + 5000 };
  emit('update-profile', newProfile);
  soundService.playCardClick();
  showToast('已领取破产补助 +5,000 金币！');
}

function handleCheckIn() {
  if (props.profile.isCheckedInToday) {
    showToast('今日已签到，明天再来哦！');
    return;
  }
  const newProfile = {
    ...props.profile,
    coins: props.profile.coins + 8888,
    isCheckedInToday: true
  };
  emit('update-profile', newProfile);
  soundService.playWin();
  showToast('🎉 签到成功！获得 +8,888 金币！');
}

function selectRoom(room) {
  if (props.profile.coins < room.minCoins) {
    showToast('金币不足以进入该场次，点击右上角+补充金币！');
    return;
  }
  soundService.playCardClick();
  emit('select-room', room);
}

function quickStart() {
  const suitable = [...rooms].reverse().find(r => props.profile.coins >= r.minCoins) || rooms[0];
  selectRoom(suitable);
}
</script>

<style scoped>
.lobby-container {
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 50% 20%, #15452d 0%, #0c2b1c 60%, #06180f 100%);
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  overflow-y: auto;
}

.lobby-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(255, 215, 0, 0.3);
  padding: 6px 12px;
  border-radius: 28px;
  cursor: pointer;
  transition: transform 0.15s;
}

.user-pill:hover {
  transform: scale(1.02);
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: radial-gradient(circle, #ffd54f, #ffb300);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  border: 1.5px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-weight: 700;
  font-size: 14px;
  color: #fff;
}

.vip-badge {
  background: #c02c38;
  color: #ffd700;
  font-size: 9px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 4px;
}

.win-rate {
  font-size: 11px;
  color: #b0bec5;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.coins-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.55);
  border: 1px solid rgba(255, 215, 0, 0.5);
  padding: 4px 6px 4px 10px;
  border-radius: 20px;
}

.coin-icon {
  font-size: 14px;
}

.coin-val {
  color: #ffd54f;
  font-weight: 700;
  font-size: 13px;
}

.add-coin-btn {
  background: #ffb300;
  border: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-weight: 900;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #000;
}

.icon-btn {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.15);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-section {
  margin: 4px 0 12px;
}

.checkin-banner {
  background: linear-gradient(135deg, #a7232e 0%, #681119 100%);
  border: 1px solid rgba(255, 215, 0, 0.4);
  border-radius: 16px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  transition: transform 0.15s;
}

.checkin-banner.claimed {
  background: rgba(35, 45, 55, 0.7);
  border-color: rgba(255,255,255,0.15);
}

.checkin-banner:hover {
  transform: translateY(-1px);
}

.banner-icon {
  font-size: 28px;
}

.banner-text {
  flex: 1;
}

.banner-text h4 {
  font-size: 14px;
  color: #fff;
  margin-bottom: 2px;
}

.banner-text p {
  font-size: 11px;
  color: #ffe082;
}

.banner-btn {
  background: #ffd54f;
  color: #000;
  font-weight: 800;
  border: none;
  padding: 6px 14px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 10px;
}

.title-icon {
  font-size: 18px;
}

.section-title h3 {
  font-size: 15px;
  color: #ffd54f;
  font-weight: 800;
}

.sub-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 6px;
}

.rooms-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.room-card {
  background: rgba(20, 30, 24, 0.85);
  border: 1px solid rgba(255, 215, 0, 0.25);
  border-radius: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.room-card:hover {
  border-color: rgba(255, 215, 0, 0.6);
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(0,0,0,0.4);
}

.room-card.locked {
  opacity: 0.6;
}

.room-badge {
  width: 44px;
  height: 44px;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 215, 0, 0.4);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.room-info {
  flex: 1;
}

.room-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-name {
  font-weight: 700;
  font-size: 14px;
  color: #fff;
}

.multiplier-tag {
  background: rgba(0, 0, 0, 0.4);
  border: 0.5px solid #ffd54f;
  color: #ffd54f;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
}

.room-details {
  font-size: 11px;
  color: #ffca28;
  margin: 2px 0;
  display: flex;
  gap: 6px;
}

.room-desc {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.room-action-btn {
  background: #ffb300;
  color: #000;
  border: none;
  font-weight: 800;
  font-size: 12px;
  padding: 8px 16px;
  border-radius: 16px;
  cursor: pointer;
}

.room-action-btn:disabled {
  background: #455a64;
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
}

.lobby-footer {
  margin-top: 14px;
  padding-bottom: 6px;
}

.quick-play-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(90deg, #ffb300, #ffa000, #ff8f00);
  border: none;
  border-radius: 24px;
  color: #000;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 179, 0, 0.4);
  transition: transform 0.15s;
}

.quick-play-btn:active {
  transform: scale(0.98);
}

/* Modals */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.modal-card {
  background: #18221c;
  border: 1px solid rgba(255, 215, 0, 0.4);
  border-radius: 20px;
  padding: 18px;
  max-width: 420px;
  width: 100%;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.7);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 10px;
  margin-bottom: 14px;
}

.modal-header h3 {
  color: #ffd54f;
  font-size: 16px;
}

.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-item {
  background: rgba(0, 0, 0, 0.4);
  padding: 8px 12px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
}

.stat-item .label {
  font-size: 10px;
  color: #b0bec5;
}

.stat-item .val {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.stat-item .val.highlight {
  color: #81c784;
}

.stat-item .val.gold {
  color: #ffd54f;
}

.stat-item .val.red {
  color: #ff5252;
}

.rules-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 380px;
  overflow-y: auto;
}

.rule-block h4 {
  font-size: 13px;
  color: #ffd54f;
  margin-bottom: 4px;
}

.rule-block p {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.5;
}

.toast-popup {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.85);
  border: 1px solid #ffd54f;
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: bold;
  z-index: 200;
  pointer-events: none;
}
</style>
