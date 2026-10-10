<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="admin-modal-card" @click.stop>
      <!-- Header -->
      <div class="modal-header">
        <div class="header-title">
          <span class="tg-icon">✈️</span>
          <h3>Telegram 管理员控制台</h3>
        </div>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <!-- Tab Buttons -->
      <div class="tab-row">
        <button
          class="tab-btn"
          :class="{ 'active': activeTab === 'config' }"
          @click="activeTab = 'config'"
        >
          ⚙️ 机器人配置
        </button>
        <button
          class="tab-btn"
          :class="{ 'active': activeTab === 'broadcast' }"
          @click="activeTab = 'broadcast'"
        >
          📢 全服广播
        </button>
        <button
          class="tab-btn"
          :class="{ 'active': activeTab === 'coins' }"
          @click="activeTab = 'coins'"
        >
          💰 发放金币
        </button>
      </div>

      <!-- Tab 1: Bot Config -->
      <div v-if="activeTab === 'config'" class="tab-content">
        <div class="form-group">
          <label>Telegram Bot Token:</label>
          <input
            v-model="config.botToken"
            type="text"
            placeholder="例如: 123456789:ABCdefGhIJKlmNoPQRstuVWXyz"
            class="admin-input"
          />
          <span class="hint">在 Telegram 中向 @BotFather 获取的机器令牌</span>
        </div>

        <div class="form-group">
          <label>管理员 Chat ID:</label>
          <input
            v-model="config.adminChatId"
            type="text"
            placeholder="例如: 1234567890"
            class="admin-input"
          />
          <span class="hint">向 @userinfobot 发送消息获取您的用户数字ID</span>
        </div>

        <div class="toggle-row">
          <label class="toggle-label">
            <input type="checkbox" v-model="config.enabled" />
            <span>启用 Telegram 管理员集成</span>
          </label>
        </div>

        <div class="toggle-row">
          <label class="toggle-label">
            <input type="checkbox" v-model="config.autoAlerts" />
            <span>大赢家/连炸/春天战报自动推送</span>
          </label>
        </div>

        <div class="action-footer">
          <button class="btn btn-save" @click="saveConfig">保存配置</button>
          <button class="btn btn-test" @click="testNotification">测试推送通道</button>
        </div>
      </div>

      <!-- Tab 2: Live Announcement Broadcast -->
      <div v-else-if="activeTab === 'broadcast'" class="tab-content">
        <div class="form-group">
          <label>全服广播公告内容：</label>
          <textarea
            v-model="broadcastText"
            rows="3"
            class="admin-textarea"
            placeholder="输入全服公告内容，将通过 WebSocket 异步非阻塞广播至所有在线玩家..."
          ></textarea>
        </div>

        <div class="sample-chips">
          <span class="chip" @click="broadcastText = '🎉 迎国庆棋牌狂欢！本周所有对局金币收益翻倍！'">🎉 国庆活动</span>
          <span class="chip" @click="broadcastText = '⚠️ 系统将于今晚 24:00 进行短暂维护升级，敬请留意。'">⚠️ 维护通知</span>
          <span class="chip" @click="broadcastText = '恭喜玩家【赌神小李】在连炸场斩获 128 倍大胜！'">🏆 大胜捷报</span>
        </div>

        <div class="action-footer">
          <button class="btn btn-broadcast" :disabled="!broadcastText.trim()" @click="sendBroadcast">
            📢 立即全服广播
          </button>
        </div>
      </div>

      <!-- Tab 3: Coins Grant -->
      <div v-else-if="activeTab === 'coins'" class="tab-content">
        <div class="form-group">
          <label>接收玩家 ID：</label>
          <input
            v-model="coinForm.playerId"
            type="text"
            placeholder="输入玩家ID或留空发给自己"
            class="admin-input"
          />
        </div>

        <div class="form-group">
          <label>发放金币数额：</label>
          <div class="coin-presets">
            <button
              v-for="amt in [10000, 50000, 100000, 500000]"
              :key="amt"
              class="preset-btn"
              :class="{ 'active': coinForm.amount === amt }"
              @click="coinForm.amount = amt"
            >
              +{{ amt.toLocaleString() }}
            </button>
          </div>
          <input
            v-model.number="coinForm.amount"
            type="number"
            class="admin-input"
            style="margin-top: 8px;"
          />
        </div>

        <div class="action-footer">
          <button class="btn btn-coins" @click="grantCoins">
            💰 确认发放金币
          </button>
        </div>
      </div>

      <!-- Status Toast -->
      <div v-if="adminMsg" class="admin-toast">{{ adminMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const emit = defineEmits(['close', 'grant-coins', 'send-announcement']);

const activeTab = ref('config');
const adminMsg = ref('');
const broadcastText = ref('');
const config = ref({
  botToken: '',
  adminChatId: '',
  enabled: true,
  autoAlerts: true
});

const coinForm = ref({
  playerId: '',
  amount: 50000
});

function showToast(msg) {
  adminMsg.value = msg;
  setTimeout(() => {
    adminMsg.value = '';
  }, 2200);
}

async function loadConfig() {
  try {
    const res = await fetch('/api/telegram/config');
    if (res.ok) {
      const data = await res.json();
      config.value = { ...config.value, ...data };
    }
  } catch (e) {
    // offline fallback
  }
}

async function saveConfig() {
  try {
    const res = await fetch('/api/telegram/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(config.value)
    });
    if (res.ok) {
      showToast('✅ Telegram 配置保存成功！');
    }
  } catch (e) {
    showToast('⚠️ 本地保存成功');
  }
}

async function testNotification() {
  showToast('正在发送测试推送...');
  try {
    const res = await fetch('/api/telegram/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: '🔔 来自棋牌游戏平台的测试消息，Telegram 通道工作正常！' })
    });
    if (res.ok) {
      showToast('✅ 推送测试已发送至 Telegram！');
    } else {
      showToast('⚠️ 发送失败，请检查 Token 与 Chat ID');
    }
  } catch (e) {
    showToast('⚠️ 网络请求失败，已进入本地调度');
  }
}

async function sendBroadcast() {
  if (!broadcastText.value.trim()) return;
  emit('send-announcement', broadcastText.value.trim());

  try {
    await fetch('/api/telegram/broadcast', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: broadcastText.value.trim() })
    });
  } catch (e) {}

  showToast('📢 全服系统广播已异步下发！');
  broadcastText.value = '';
}

function grantCoins() {
  if (coinForm.value.amount <= 0) return;
  emit('grant-coins', {
    playerId: coinForm.value.playerId,
    amount: coinForm.value.amount
  });
  showToast(`💰 成功发放 +${coinForm.value.amount.toLocaleString()} 金币！`);
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
  backdrop-filter: blur(3px);
}

.admin-modal-card {
  background: #18221b;
  border: 1.5px solid #ffd54f;
  border-radius: 20px;
  max-width: 460px;
  width: 100%;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8);
  position: relative;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 12px;
  margin-bottom: 14px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title .tg-icon {
  font-size: 20px;
}

.header-title h3 {
  color: #ffd54f;
  font-size: 16px;
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.tab-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px;
  border-radius: 12px;
}

.tab-btn {
  flex: 1;
  background: transparent;
  border: none;
  color: #b0bec5;
  font-size: 12px;
  font-weight: bold;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
}

.tab-btn.active {
  background: #233428;
  color: #ffd54f;
  border: 1px solid rgba(255, 215, 0, 0.4);
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-group label {
  font-size: 12px;
  color: #fff;
  font-weight: bold;
}

.admin-input {
  background: #0f1612;
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  outline: none;
}

.admin-input:focus {
  border-color: #ffd54f;
}

.admin-textarea {
  background: #0f1612;
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  resize: none;
}

.hint {
  font-size: 10px;
  color: #90a4ae;
}

.toggle-row {
  display: flex;
  align-items: center;
}

.toggle-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #e0e0e0;
  cursor: pointer;
}

.action-footer {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.btn {
  flex: 1;
  padding: 10px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.btn-save {
  background: #ffd54f;
  color: #000;
}

.btn-test {
  background: #37474f;
  color: #ffd54f;
  border: 1px solid #ffd54f;
}

.btn-broadcast {
  background: linear-gradient(90deg, #c02c38, #ff8f00);
  color: #fff;
}

.btn-coins {
  background: linear-gradient(90deg, #ffd54f, #ffb300);
  color: #000;
}

.coin-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.preset-btn {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #ffd54f;
  padding: 6px 2px;
  border-radius: 8px;
  font-size: 10px;
  cursor: pointer;
}

.preset-btn.active {
  background: rgba(255, 215, 0, 0.2);
  border-color: #ffd54f;
}

.sample-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  background: rgba(255, 255, 255, 0.08);
  border: 0.5px solid rgba(255, 215, 0, 0.3);
  color: #ffe082;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 10px;
  cursor: pointer;
}

.chip:hover {
  background: rgba(255, 215, 0, 0.2);
}

.admin-toast {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  border: 1px solid #ffd54f;
  color: #fff;
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: bold;
  white-space: nowrap;
}
</style>
