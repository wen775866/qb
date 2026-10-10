<template>
  <div class="doudizhu-table">
    <!-- Top Table Header -->
    <header class="table-top-bar">
      <button class="nav-back-btn" @click="$emit('back-to-lobby')" title="返回大厅">
        ← 返回大厅
      </button>

      <div class="table-info-pill">
        <span class="room-title">{{ room.name }}</span>
        <span class="divider">|</span>
        <span class="base-score">底分: {{ room.baseScore }}</span>
        <span class="divider">|</span>
        <span class="multiplier-badge">{{ multiplier }} 倍</span>
      </div>

      <div class="top-controls">
        <button class="icon-btn-small" @click="toggleSound" :title="soundEnabled ? '静音' : '开启声音'">
          <span v-if="soundEnabled">🔊</span>
          <span v-else>🔇</span>
        </button>
        <button class="icon-btn-small" @click="showTableRules = true" title="本场规则">
          <span>ℹ️</span>
        </button>
      </div>
    </header>

    <!-- Main Game Felt Area -->
    <main class="table-felt">
      <!-- Top Bottom Cards (底牌) -->
      <div class="bottom-cards-rack">
        <div class="hole-card-slot" v-for="(card, idx) in bottomCards" :key="idx">
          <div v-if="phase === 'BIDDING'" class="card-back-small">🎴</div>
          <div v-else class="mini-card" :class="{ 'red': card.isRed, 'has-svg': card.key && !failedSvgKeys.has(card.key) }">
            <img
              v-if="card.key && !failedSvgKeys.has(card.key)"
              :src="card.svgUrl || `/cards/${card.key}.svg`"
              :alt="card.rank"
              class="card-svg-img"
              @error="onCardImgError(card.key)"
            />
            <template v-else>
              <span class="mini-rank">{{ card.rank }}</span>
              <span class="mini-suit">{{ card.isJoker ? (card.weight === 17 ? '👑' : '🃏') : card.suit.symbol }}</span>
            </template>
          </div>
        </div>
      </div>

      <!-- Left Bot (阿强) -->
      <div class="opponent-seat left-seat" :class="{ 'active-turn': currentTurn === 2 }">
        <div class="avatar-wrap">
          <div class="avatar-face">🤖</div>
          <div v-if="landlordSeat === 2" class="role-badge landlord">地主</div>
          <div v-else-if="landlordSeat !== -1" class="role-badge farmer">农民</div>
        </div>
        <div class="seat-name">牌神·阿强</div>
        <div class="card-counter" :class="{ 'alarm': botLeftHand.length <= 2 }">
          {{ botLeftHand.length }} 张
        </div>
        <div v-if="botLeftBubble" class="speech-bubble left-bubble">{{ botLeftBubble }}</div>
      </div>

      <!-- Right Bot (小华) -->
      <div class="opponent-seat right-seat" :class="{ 'active-turn': currentTurn === 1 }">
        <div class="avatar-wrap">
          <div class="avatar-face">🤠</div>
          <div v-if="landlordSeat === 1" class="role-badge landlord">地主</div>
          <div v-else-if="landlordSeat !== -1" class="role-badge farmer">农民</div>
        </div>
        <div class="seat-name">赌侠·小华</div>
        <div class="card-counter" :class="{ 'alarm': botRightHand.length <= 2 }">
          {{ botRightHand.length }} 张
        </div>
        <div v-if="botRightBubble" class="speech-bubble right-bubble">{{ botRightBubble }}</div>
      </div>

      <!-- Special Effects Banner Overlay -->
      <transition name="pop">
        <div v-if="vfxBanner" class="vfx-banner">
          {{ vfxBanner }}
        </div>
      </transition>

      <!-- Center Discard Area -->
      <div class="center-play-area">
        <div v-if="lastCombo" class="played-combo-box">
          <div class="combo-label">
            [{{ getPlayerName(lastPlayerSeat) }}] {{ lastCombo.type.name }}
          </div>
          <div class="played-cards-row">
            <div
              v-for="card in lastCombo.cards"
              :key="card.id"
              class="table-poker-card"
              :class="{ 'red': card.isRed, 'has-svg': card.key && !failedSvgKeys.has(card.key) }"
            >
              <img
                v-if="card.key && !failedSvgKeys.has(card.key)"
                :src="card.svgUrl || `/cards/${card.key}.svg`"
                :alt="card.rank"
                class="card-svg-img"
                @error="onCardImgError(card.key)"
              />
              <template v-else>
                <div class="corner-top">
                  <span class="c-rank">{{ card.rank }}</span>
                  <span class="c-suit">{{ card.isJoker ? (card.weight === 17 ? '👑' : '🃏') : card.suit.symbol }}</span>
                </div>
                <div class="center-watermark">
                  {{ card.isJoker ? (card.weight === 17 ? '大王' : '小王') : card.suit.symbol }}
                </div>
              </template>
            </div>
          </div>
        </div>
        <div v-else-if="phase === 'PLAYING'" class="empty-table-tip">
          {{ currentTurn === 0 ? '轮到您出牌，任意有效牌型均可' : '等待对手出牌...' }}
        </div>
      </div>

      <!-- User Hand & Controls (Bottom) -->
      <div class="user-bottom-dock">
        <!-- User Speech Bubble -->
        <div v-if="userBubble" class="speech-bubble user-bubble">{{ userBubble }}</div>

        <!-- Action Control Buttons -->
        <div class="action-btn-row">
          <!-- Bidding Actions -->
          <template v-if="currentTurn === 0 && phase === 'BIDDING'">
            <button class="btn btn-outline" @click="handleUserBid(false)">不抢</button>
            <button class="btn btn-gold" @click="handleUserBid(true)">抢地主 (×2)</button>
          </template>

          <!-- Doubling Actions -->
          <template v-if="currentTurn === 0 && phase === 'DOUBLING'">
            <button class="btn btn-outline" @click="handleUserDouble(false)">不加倍</button>
            <button class="btn btn-gold" @click="handleUserDouble(true)">超级加倍 (×2)</button>
          </template>

          <!-- Playing Actions -->
          <template v-if="currentTurn === 0 && phase === 'PLAYING'">
            <button
              v-if="lastCombo && lastPlayerSeat !== 0"
              class="btn btn-outline"
              @click="handleUserPass"
            >
              不出
            </button>
            <button class="btn btn-dark" @click="handleUserHint">提示</button>
            <button class="btn btn-outline" @click="clearSelection">重选</button>
            <button class="btn btn-gold" @click="handleUserPlay">出牌</button>
            <button class="btn-chat" @click="showChatMenu = !showChatMenu" title="快捷聊天">💬</button>
          </template>
        </div>

        <!-- User Role & Warning Header -->
        <div class="user-meta-bar">
          <span v-if="landlordSeat === 0" class="role-tag landlord-tag">👑 [地主]</span>
          <span v-else-if="landlordSeat !== -1" class="role-tag farmer-tag">🤝 [农民同盟]</span>
          <span class="user-tag">您 (剩余 {{ userHand.length }} 张)</span>
          <span v-if="userHand.length <= 2" class="alarm-tag">🚨 只剩{{ userHand.length }}张！</span>
        </div>

        <!-- User Cards Row -->
        <div class="cards-fan-container">
          <div
            v-for="card in userHand"
            :key="card.id"
            class="user-poker-card"
            :class="{ 'selected': card.selected, 'red': card.isRed, 'has-svg': card.key && !failedSvgKeys.has(card.key) }"
            @click="toggleCardSelection(card)"
          >
            <img
              v-if="card.key && !failedSvgKeys.has(card.key)"
              :src="card.svgUrl || `/cards/${card.key}.svg`"
              :alt="card.rank"
              class="card-svg-img"
              @error="onCardImgError(card.key)"
            />
            <template v-else>
              <div class="corner-top">
                <span class="c-rank">{{ card.rank }}</span>
                <span class="c-suit">{{ card.isJoker ? (card.weight === 17 ? '👑' : '🃏') : card.suit.symbol }}</span>
              </div>
              <div class="center-watermark">
                {{ card.isJoker ? (card.weight === 17 ? '大王' : '小王') : card.suit.symbol }}
              </div>
              <div class="corner-bottom">
                <span class="c-rank">{{ card.rank }}</span>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- Quick Chat Menu Dropdown -->
      <div v-if="showChatMenu" class="chat-menu">
        <div
          v-for="(phrase, i) in quickPhrases"
          :key="i"
          class="chat-item"
          @click="sendQuickChat(phrase)"
        >
          {{ phrase }}
        </div>
      </div>
    </main>

    <!-- Settlement Modal -->
    <div v-if="showSettlement" class="modal-backdrop">
      <div class="settlement-card">
        <div class="settlement-header" :class="{ 'win': isUserWin }">
          <h2>{{ isUserWin ? '🎉 获胜大捷 🎉' : '💔 遗憾落败 💔' }}</h2>
          <p class="settlement-sub">{{ settlementTitle }}</p>
        </div>

        <div class="settlement-body">
          <div class="coins-delta" :class="{ 'win': isUserWin }">
            {{ coinsDelta >= 0 ? '+' : '' }}{{ coinsDelta.toLocaleString() }} 金币
          </div>
          <p class="settlement-calc">{{ settlementCalc }}</p>
        </div>

        <div class="settlement-footer">
          <button class="btn btn-outline" @click="$emit('back-to-lobby')">返回大厅</button>
          <button class="btn btn-gold" @click="startNewRound">再来一局</button>
        </div>
      </div>
    </div>

    <!-- Table Rules Modal -->
    <div v-if="showTableRules" class="modal-backdrop" @click="showTableRules = false">
      <div class="modal-card" @click.stop>
        <div class="modal-header">
          <h3>ℹ️ {{ room.name }} · 规则详情</h3>
          <button class="close-btn" @click="showTableRules = false">✕</button>
        </div>
        <p style="font-size: 13px; line-height: 1.6; color: #e0e0e0;">
          底分：{{ room.baseScore }} 金币<br/>
          封顶倍数：{{ room.maxMultiplier }} 倍<br/>
          叫/抢地主翻倍，超级加倍翻倍，炸弹/王炸翻倍！<br/>
          地主通关未让农民出牌即为【春天】（倍数翻倍）；农民获胜且地主只出一手牌即为【反春】（倍数翻倍）！
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import confetti from 'canvas-confetti';
import {
  createStandardDeals,
  createUnshuffledDeals,
  parseCombo,
  canBeat,
  findAllValidMoves,
  findAiMove,
  HAND_TYPES
} from '../services/doudizhuEngine.js';
import { soundService } from '../services/sound.js';

const props = defineProps({
  room: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['back-to-lobby', 'update-coins']);

const soundEnabled = ref(soundService.enabled);
const showTableRules = ref(false);
const showChatMenu = ref(false);
const vfxBanner = ref('');

const phase = ref('BIDDING');
const currentTurn = ref(0);
const landlordSeat = ref(-1);
const multiplier = ref(1);

const bottomCards = ref([]);
const userHand = ref([]);
const botRightHand = ref([]);
const botLeftHand = ref([]);

const lastCombo = ref(null);
const lastPlayerSeat = ref(-1);
const passCount = ref(0);

const landlordPlays = ref(0);
const farmerPlays = ref(0);

const userBubble = ref('');
const botRightBubble = ref('');
const botLeftBubble = ref('');

const hintIndex = ref(0);

// 记录加载失败的 SVG 键，优雅降级为原生精美卡牌
const failedSvgKeys = ref(new Set());
function onCardImgError(cardKey) {
  if (cardKey) {
    failedSvgKeys.value.add(cardKey);
  }
}

const showSettlement = ref(false);
const isUserWin = ref(false);
const settlementTitle = ref('');
const settlementCalc = ref('');
const coinsDelta = ref(0);

const quickPhrases = [
  '快点啊，等到花儿都谢了！',
  '和你合作真是太愉快了！',
  '一手好牌，怎么打都是赢！',
  '你是MM还是GG？',
  '炸弹在手，天下我有！',
  '大清亡了，你还在想？'
];

function toggleSound() {
  soundEnabled.value = !soundEnabled.value;
  soundService.enabled = soundEnabled.value;
}

function triggerVfx(text) {
  vfxBanner.value = text;
  setTimeout(() => { vfxBanner.value = ''; }, 1400);
}

function getPlayerName(seat) {
  if (seat === 0) return '您';
  if (seat === 1) return '小华';
  if (seat === 2) return '阿强';
  return '';
}

function startNewRound() {
  soundService.playCardClick();
  const deals = props.room.id === 'bomb' ? createUnshuffledDeals() : createStandardDeals();

  userHand.value = deals.hand0.map(c => ({ ...c, selected: false }));
  botRightHand.value = deals.hand1;
  botLeftHand.value = deals.hand2;
  bottomCards.value = deals.bottom;

  landlordSeat.value = -1;
  multiplier.value = 1;
  currentTurn.value = 0;
  phase.value = 'BIDDING';
  lastCombo.value = null;
  lastPlayerSeat.value = -1;
  passCount.value = 0;
  landlordPlays.value = 0;
  farmerPlays.value = 0;
  userBubble.value = '';
  botRightBubble.value = '';
  botLeftBubble.value = '';
  hintIndex.value = 0;
  showSettlement.value = false;
  vfxBanner.value = '';
}

onMounted(() => {
  startNewRound();
});

function toggleCardSelection(card) {
  card.selected = !card.selected;
  soundService.playCardClick();
}

function clearSelection() {
  userHand.value.forEach(c => (c.selected = false));
}

function sendQuickChat(phrase) {
  userBubble.value = phrase;
  showChatMenu.value = false;
  soundService.playCardClick();
  setTimeout(() => { userBubble.value = ''; }, 2000);
}

function handleUserBid(willBid) {
  soundService.playCardClick();
  if (willBid) {
    userBubble.value = '抢地主!';
    multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
    landlordSeat.value = 0;
  } else {
    userBubble.value = '不抢';
  }
  setTimeout(() => { userBubble.value = ''; }, 800);
  currentTurn.value = 1;
  triggerBotTurn();
}

function handleUserDouble(willDouble) {
  soundService.playCardClick();
  if (willDouble) {
    userBubble.value = '超级加倍!';
    multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
    triggerVfx(`⚡ 加倍成功！倍数提升至 ${multiplier.value}倍！`);
  } else {
    userBubble.value = '不加倍';
  }
  setTimeout(() => { userBubble.value = ''; }, 800);
  currentTurn.value = 1;
  triggerBotTurn();
}

function handleUserPass() {
  soundService.playCardClick();
  passCount.value++;
  userBubble.value = '要不起';
  setTimeout(() => { userBubble.value = ''; }, 700);
  if (passCount.value >= 2) {
    lastCombo.value = null;
    lastPlayerSeat.value = -1;
  }
  currentTurn.value = 1;
  triggerBotTurn();
}

function handleUserHint() {
  soundService.playCardClick();
  const target = lastPlayerSeat.value === 0 ? null : lastCombo.value;
  const moves = findAllValidMoves(userHand.value, target);
  if (moves.length === 0) {
    userBubble.value = '没有牌能大过上家';
    setTimeout(() => { userBubble.value = ''; }, 800);
    return;
  }
  const chosen = moves[hintIndex.value % moves.length];
  hintIndex.value++;
  const chosenIds = new Set(chosen.map(c => c.id));
  userHand.value.forEach(c => {
    c.selected = chosenIds.has(c.id);
  });
}

function handleUserPlay() {
  const selectedCards = userHand.value.filter(c => c.selected);
  const combo = parseCombo(selectedCards);
  const target = lastPlayerSeat.value === 0 ? null : lastCombo.value;

  if (combo && canBeat(target, combo)) {
    soundService.playPlayCombo();
    lastCombo.value = combo;
    lastPlayerSeat.value = 0;
    passCount.value = 0;

    if (landlordSeat.value === 0) landlordPlays.value++;
    else farmerPlays.value++;

    if (combo.type.id === HAND_TYPES.BOMB.id) {
      multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
      soundService.playBomb();
      triggerVfx(`💣 炸弹！倍数翻倍至 ${multiplier.value}倍！`);
    } else if (combo.type.id === HAND_TYPES.ROCKET.id) {
      multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
      soundService.playBomb();
      triggerVfx(`🚀 王炸！双倍爆发至 ${multiplier.value}倍！`);
    } else if (combo.type.id === HAND_TYPES.AIRPLANE.id || combo.type.id === HAND_TYPES.AIRPLANE_WITH_WINGS.id) {
      triggerVfx('✈️ 飞机带翅膀！');
    }

    userHand.value = userHand.value.filter(c => !c.selected);
    userBubble.value = combo.type.name;
    setTimeout(() => { userBubble.value = ''; }, 800);

    if (userHand.value.length === 0) {
      checkEndGame(0);
    } else {
      currentTurn.value = 1;
      triggerBotTurn();
    }
  } else {
    userBubble.value = '不符合出牌规则！';
    setTimeout(() => { userBubble.value = ''; }, 800);
  }
}

function triggerBotTurn() {
  setTimeout(() => {
    if (phase.value === 'GAME_OVER') return;

    if (phase.value === 'BIDDING') {
      const hand = currentTurn.value === 1 ? botRightHand.value : botLeftHand.value;
      const highCount = hand.filter(c => c.weight >= 14).length;
      const willBid = highCount >= 3;

      if (willBid) {
        if (currentTurn.value === 1) botRightBubble.value = '抢地主!';
        else botLeftBubble.value = '抢地主!';
        multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
        landlordSeat.value = currentTurn.value;
      } else {
        if (currentTurn.value === 1) botRightBubble.value = '不抢';
        else botLeftBubble.value = '不抢';
      }

      setTimeout(() => {
        botRightBubble.value = '';
        botLeftBubble.value = '';
      }, 700);

      if (currentTurn.value === 2) {
        const finalLandlord = landlordSeat.value === -1 ? 0 : landlordSeat.value;
        landlordSeat.value = finalLandlord;
        if (finalLandlord === 0) userHand.value = [...userHand.value, ...bottomCards.value].sort((a,b)=>b.weight-a.weight);
        else if (finalLandlord === 1) botRightHand.value = [...botRightHand.value, ...bottomCards.value].sort((a,b)=>b.weight-a.weight);
        else botLeftHand.value = [...botLeftHand.value, ...bottomCards.value].sort((a,b)=>b.weight-a.weight);

        phase.value = 'DOUBLING';
        currentTurn.value = 0;
      } else {
        currentTurn.value++;
        triggerBotTurn();
      }
    } else if (phase.value === 'DOUBLING') {
      const hand = currentTurn.value === 1 ? botRightHand.value : botLeftHand.value;
      if (hand.filter(c => c.weight >= 15).length >= 2) {
        if (currentTurn.value === 1) botRightBubble.value = '加倍!';
        else botLeftBubble.value = '加倍!';
        multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
      } else {
        if (currentTurn.value === 1) botRightBubble.value = '不加';
        else botLeftBubble.value = '不加';
      }

      setTimeout(() => {
        botRightBubble.value = '';
        botLeftBubble.value = '';
      }, 600);

      if (currentTurn.value === 2) {
        phase.value = 'PLAYING';
        currentTurn.value = landlordSeat.value;
        if (currentTurn.value !== 0) triggerBotTurn();
      } else {
        currentTurn.value++;
        triggerBotTurn();
      }
    } else if (phase.value === 'PLAYING') {
      const isBot1 = currentTurn.value === 1;
      const hand = isBot1 ? botRightHand.value : botLeftHand.value;
      const target = lastPlayerSeat.value === currentTurn.value ? null : lastCombo.value;

      const isBotLandlord = currentTurn.value === landlordSeat.value;
      const isTeammate = !isBotLandlord && (lastPlayerSeat.value !== landlordSeat.value && lastPlayerSeat.value !== -1);
      const teammateCount = isBot1 ? botLeftHand.value.length : botRightHand.value.length;

      const move = findAiMove(hand, target, isTeammate, teammateCount);

      if (move) {
        const combo = parseCombo(move);
        lastCombo.value = combo;
        lastPlayerSeat.value = currentTurn.value;
        passCount.value = 0;

        if (isBotLandlord) landlordPlays.value++;
        else farmerPlays.value++;

        if (combo.type.id === HAND_TYPES.BOMB.id) {
          multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
          soundService.playBomb();
          triggerVfx(`💣 炸弹！倍数翻倍至 ${multiplier.value}倍！`);
        } else if (combo.type.id === HAND_TYPES.ROCKET.id) {
          multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
          soundService.playBomb();
          triggerVfx(`🚀 王炸！双倍爆发至 ${multiplier.value}倍！`);
        } else if (combo.type.id === HAND_TYPES.AIRPLANE.id || combo.type.id === HAND_TYPES.AIRPLANE_WITH_WINGS.id) {
          triggerVfx('✈️ 飞机带翅膀！');
        }

        const moveIds = new Set(move.map(c => c.id));
        if (isBot1) {
          botRightHand.value = botRightHand.value.filter(c => !moveIds.has(c.id));
          botRightBubble.value = combo.type.name;
          if (botRightHand.value.length <= 2) botRightBubble.value = `🚨 只剩${botRightHand.value.length}张！`;
        } else {
          botLeftHand.value = botLeftHand.value.filter(c => !moveIds.has(c.id));
          botLeftBubble.value = combo.type.name;
          if (botLeftHand.value.length <= 2) botLeftBubble.value = `🚨 只剩${botLeftHand.value.length}张！`;
        }

        const remaining = isBot1 ? botRightHand.value.length : botLeftHand.value.length;
        if (remaining === 0) {
          checkEndGame(currentTurn.value);
          return;
        }
      } else {
        passCount.value++;
        if (isBot1) botRightBubble.value = '要不起';
        else botLeftBubble.value = '要不起';

        if (passCount.value >= 2) {
          lastCombo.value = null;
          lastPlayerSeat.value = -1;
        }
      }

      setTimeout(() => {
        botRightBubble.value = '';
        botLeftBubble.value = '';
      }, 700);

      currentTurn.value = (currentTurn.value + 1) % 3;
      if (currentTurn.value !== 0) triggerBotTurn();
    }
  }, 850);
}

function checkEndGame(winnerSeat) {
  phase.value = 'GAME_OVER';
  const winnerIsLandlord = winnerSeat === landlordSeat.value;
  const userIsLandlord = landlordSeat.value === 0;
  const userWin = winnerIsLandlord ? userIsLandlord : !userIsLandlord;

  let springApplied = false;
  if (winnerIsLandlord && farmerPlays.value === 0) {
    multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
    springApplied = true;
    triggerVfx('🌸 春天！农民一张未出！');
  } else if (!winnerIsLandlord && landlordPlays.value === 1) {
    multiplier.value = Math.min(props.room.maxMultiplier, multiplier.value * 2);
    springApplied = true;
    triggerVfx('🌸 反春！地主仅出一手！');
  }

  const roleFactor = userIsLandlord ? 2 : 1;
  const delta = props.room.baseScore * multiplier.value * roleFactor * (userWin ? 1 : -1);

  isUserWin.value = userWin;
  coinsDelta.value = delta;
  settlementTitle.value = winnerIsLandlord ? '地主大获全胜！' : '农民携手获胜！';
  settlementCalc.value = `底分 ${props.room.baseScore} × 倍数 ${multiplier.value}倍 ${springApplied ? '(含春天翻倍)' : ''}`;

  if (userWin) {
    soundService.playWin();
    confetti({ particleCount: 80, spread: 60, origin: { y: 0.6 } });
  } else {
    soundService.playLose();
  }

  showSettlement.value = true;
  emit('update-coins', delta);
}
</script>

<style scoped>
.doudizhu-table {
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 50% 30%, #175438 0%, #0d3824 65%, #051a10 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.table-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.45);
  border-bottom: 1px solid rgba(255, 215, 0, 0.25);
  z-index: 10;
}

.nav-back-btn {
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 5px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.table-info-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.55);
  border: 1px solid rgba(255, 215, 0, 0.4);
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

.room-title {
  color: #ffd54f;
  font-weight: 800;
}

.divider {
  color: rgba(255, 255, 255, 0.3);
}

.base-score {
  color: #fff;
}

.multiplier-badge {
  color: #ff5252;
  font-weight: 800;
}

.top-controls {
  display: flex;
  gap: 6px;
}

.icon-btn-small {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.15);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Table Felt Area */
.table-felt {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
}

.bottom-cards-rack {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding-top: 8px;
}

.hole-card-slot {
  width: 38px;
  height: 52px;
}

.card-back-small {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #8b1a24, #5a0d15);
  border: 1.5px solid #ffd700;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.4);
}

.mini-card {
  width: 100%;
  height: 100%;
  background: #fdfbf7;
  border: 1px solid #dcdcdc;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #1a1a1a;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

.mini-card.red {
  color: #d32f2f;
}

.mini-rank {
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
}

.mini-suit {
  font-size: 11px;
}

/* Opponent Seats */
.opponent-seat {
  position: absolute;
  top: 32%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 5;
}

.left-seat {
  left: 14px;
}

.right-seat {
  right: 14px;
}

.avatar-wrap {
  position: relative;
}

.avatar-face {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: radial-gradient(circle, #37474f, #1e272c);
  border: 2px solid #78909c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.5);
  transition: all 0.2s;
}

.active-turn .avatar-face {
  border-color: #ffd54f;
  box-shadow: 0 0 16px rgba(255, 213, 79, 0.8);
  transform: scale(1.08);
}

.role-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 4px;
  color: #fff;
}

.role-badge.landlord {
  background: #c02c38;
  border: 1px solid #ffd700;
}

.role-badge.farmer {
  background: #1976d2;
}

.seat-name {
  font-size: 11px;
  color: #fff;
  margin-top: 6px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.8);
}

.card-counter {
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 215, 0, 0.4);
  color: #ffd54f;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  margin-top: 2px;
}

.card-counter.alarm {
  color: #ff5252;
  border-color: #ff5252;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.speech-bubble {
  position: absolute;
  background: #fff9c4;
  border: 1px solid #b8860b;
  color: #3e2723;
  padding: 4px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
  box-shadow: 0 3px 8px rgba(0,0,0,0.3);
  z-index: 20;
}

.left-bubble {
  top: -24px;
  left: 10px;
}

.right-bubble {
  top: -24px;
  right: 10px;
}

.user-bubble {
  bottom: 110px;
  left: 50%;
  transform: translateX(-50%);
}

.vfx-banner {
  position: absolute;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(90deg, #c02c38, #ffb300, #c02c38);
  border: 2px solid #fff;
  color: #fff;
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 900;
  box-shadow: 0 4px 20px rgba(0,0,0,0.6);
  z-index: 50;
}

.pop-enter-active, .pop-leave-active {
  transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.pop-enter-from, .pop-leave-to {
  opacity: 0;
  transform: translateX(-50%) scale(0.6);
}

.center-play-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-bottom: 70px;
}

.played-combo-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.combo-label {
  color: #ffd54f;
  font-size: 12px;
  font-weight: 800;
  text-shadow: 0 1px 3px rgba(0,0,0,0.8);
}

.played-cards-row {
  display: flex;
  justify-content: center;
}

.empty-table-tip {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.table-poker-card {
  width: 48px;
  height: 68px;
  background: #fdfbf7;
  border: 1px solid #c9cdd4;
  border-radius: 6px;
  margin-left: -20px;
  position: relative;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  color: #1a1a1a;
  padding: 3px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.table-poker-card:first-child {
  margin-left: 0;
}

.table-poker-card.red {
  color: #d32f2f;
}

.corner-top {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.c-rank {
  font-size: 13px;
  font-weight: 900;
}

.c-suit {
  font-size: 11px;
}

.center-watermark {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 22px;
  opacity: 0.25;
  font-weight: bold;
}

.user-bottom-dock {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 8px;
}

.action-btn-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  align-items: center;
}

.btn {
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.1s;
}

.btn:active {
  transform: scale(0.95);
}

.btn-gold {
  background: #ffd54f;
  color: #000;
  border: none;
  box-shadow: 0 3px 10px rgba(255, 213, 79, 0.4);
}

.btn-dark {
  background: #37474f;
  color: #ffd54f;
  border: 1px solid #ffd54f;
}

.btn-outline {
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.btn-chat {
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid #ffd54f;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-size: 15px;
  cursor: pointer;
}

.user-meta-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 11px;
}

.role-tag {
  font-weight: 800;
}

.landlord-tag {
  color: #ff5252;
}

.farmer-tag {
  color: #64b5f6;
}

.user-tag {
  color: #fff;
}

.alarm-tag {
  color: #ff5252;
  font-weight: 900;
}

.cards-fan-container {
  display: flex;
  justify-content: center;
  width: 100%;
  overflow-x: auto;
  padding: 16px 12px 4px;
}

.user-poker-card {
  width: 56px;
  height: 84px;
  background: #fdfbf7;
  border: 1px solid #c9cdd4;
  border-radius: 6px;
  margin-left: -26px;
  position: relative;
  box-shadow: 0 3px 8px rgba(0,0,0,0.35);
  color: #1a1a1a;
  padding: 3px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  cursor: pointer;
  transition: transform 0.15s, border-color 0.15s;
  flex-shrink: 0;
}

.user-poker-card:first-child {
  margin-left: 0;
}

.user-poker-card.red {
  color: #d32f2f;
}

.user-poker-card.selected {
  transform: translateY(-16px);
  border: 2px solid #ffb300;
  box-shadow: 0 8px 16px rgba(255, 179, 0, 0.5);
}

.user-poker-card.has-svg,
.table-poker-card.has-svg,
.mini-card.has-svg {
  padding: 0;
  overflow: hidden;
  background: transparent;
}

.card-svg-img {
  width: 100%;
  height: 100%;
  object-fit: fill;
  display: block;
  pointer-events: none;
  border-radius: inherit;
}

.corner-bottom {
  display: flex;
  justify-content: flex-end;
  transform: rotate(180deg);
}

.chat-menu {
  position: absolute;
  bottom: 120px;
  right: 20px;
  background: rgba(20, 25, 30, 0.95);
  border: 1px solid #ffd54f;
  border-radius: 12px;
  padding: 8px;
  z-index: 60;
  box-shadow: 0 4px 16px rgba(0,0,0,0.6);
}

.chat-item {
  padding: 6px 12px;
  font-size: 12px;
  color: #fff;
  cursor: pointer;
  border-radius: 6px;
}

.chat-item:hover {
  background: rgba(255, 215, 0, 0.2);
}

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
  max-width: 400px;
  width: 100%;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.settlement-card {
  background: #192026;
  border: 2px solid #ffd54f;
  border-radius: 20px;
  width: 100%;
  max-width: 380px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0,0,0,0.8);
  text-align: center;
}

.settlement-header {
  padding: 16px;
  background: #5a1017;
}

.settlement-header.win {
  background: linear-gradient(135deg, #a7232e, #681119);
}

.settlement-header h2 {
  font-size: 22px;
  color: #ffd54f;
  font-weight: 900;
}

.settlement-sub {
  font-size: 13px;
  color: #fff;
  margin-top: 4px;
}

.settlement-body {
  padding: 20px;
}

.coins-delta {
  font-size: 28px;
  font-weight: 900;
  color: #ff5252;
}

.coins-delta.win {
  color: #ffd54f;
}

.settlement-calc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 8px;
}

.settlement-footer {
  display: flex;
  gap: 12px;
  padding: 14px 20px 20px;
  justify-content: center;
}

.settlement-footer .btn {
  flex: 1;
}
</style>
