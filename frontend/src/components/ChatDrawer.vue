<template>
  <div class="chat-drawer-backdrop" @click="$emit('close')">
    <div class="chat-drawer" @click.stop>
      <!-- Chat Header -->
      <div class="drawer-header">
        <div class="title-wrap">
          <span class="icon">💬</span>
          <h3>公共游戏聊天室</h3>
          <span class="online-tag">实时同步</span>
        </div>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <!-- Messages Stream -->
      <div class="messages-container" ref="msgListRef">
        <div v-if="messages.length === 0" class="empty-hint">
          暂无聊天消息，快来发送第一条吧！
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id || msg.timestamp"
          class="msg-item"
          :class="{
            'is-announcement': msg.type === 'system_announcement',
            'is-self': msg.senderId === currentUserId
          }"
        >
          <!-- System Announcement Display -->
          <template v-if="msg.type === 'system_announcement'">
            <div class="announcement-card">
              <div class="announcement-title">📢 全服公告 · {{ msg.sender }}</div>
              <div class="announcement-body">{{ msg.text }}</div>
              <div class="announcement-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </template>

          <!-- Normal Player Message Display -->
          <template v-else>
            <div class="msg-avatar">{{ msg.avatar || '👤' }}</div>
            <div class="msg-content">
              <div class="msg-header">
                <span class="sender-name">{{ msg.sender }}</span>
                <span v-if="msg.vipLevel" class="vip-tag">VIP {{ msg.vipLevel }}</span>
                <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
              <div class="msg-bubble">{{ msg.text }}</div>
            </div>
          </template>
        </div>
      </div>

      <!-- Quick Phrases Bar -->
      <div class="quick-phrases-bar">
        <span
          v-for="(phrase, idx) in quickPhrases"
          :key="idx"
          class="phrase-chip"
          @click="selectQuickPhrase(phrase)"
        >
          {{ phrase }}
        </span>
      </div>

      <!-- Chat Input Area -->
      <div class="drawer-input-row">
        <input
          v-model="inputMsg"
          type="text"
          class="chat-input"
          placeholder="说点什么吧..."
          maxlength="60"
          @keyup.enter="handleSend"
        />
        <button class="send-btn" :disabled="!inputMsg.trim()" @click="handleSend">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  currentUserId: {
    type: String,
    default: ''
  },
  currentUserName: {
    type: String,
    default: '我'
  },
  currentUserAvatar: {
    type: String,
    default: '🤠'
  },
  roomId: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['close', 'send-message']);

const inputMsg = ref('');
const msgListRef = ref(null);

const quickPhrases = [
  '快点啊，等到花儿都谢了！',
  '和你合作真是太愉快了！',
  '一手好牌，怎么打都是赢！',
  '炸弹在手，天下我有！',
  '给个赞👍，老铁牌技高超！'
];

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toTimeString().substring(0, 5);
}

function selectQuickPhrase(phrase) {
  inputMsg.value = phrase;
  handleSend();
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight;
    }
  });
}

function handleSend() {
  const text = inputMsg.value.trim();
  if (!text) return;

  emit('send-message', {
    text,
    senderId: props.currentUserId,
    sender: props.currentUserName,
    avatar: props.currentUserAvatar,
    roomId: props.roomId
  });

  inputMsg.value = '';
  scrollToBottom();
}

onMounted(() => {
  scrollToBottom();
});
</script>

<style scoped>
.chat-drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  justify-content: flex-end;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.chat-drawer {
  width: 100%;
  max-width: 360px;
  height: 100%;
  background: #151d18;
  border-left: 1px solid rgba(255, 215, 0, 0.3);
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.7);
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: rgba(0, 0, 0, 0.4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-wrap .icon {
  font-size: 18px;
}

.title-wrap h3 {
  font-size: 15px;
  color: #ffd54f;
  margin: 0;
}

.online-tag {
  background: #2e7d32;
  color: #fff;
  font-size: 9px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: bold;
}

.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.messages-container {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-hint {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
  margin-top: 40px;
}

.msg-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg-item.is-self {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #263238;
  border: 1px solid rgba(255, 215, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.msg-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.msg-item.is-self .msg-content {
  align-items: flex-end;
}

.msg-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.sender-name {
  font-size: 11px;
  color: #b0bec5;
  font-weight: bold;
}

.vip-tag {
  background: #c02c38;
  color: #ffd700;
  font-size: 8px;
  padding: 0 4px;
  border-radius: 3px;
  font-weight: 800;
}

.msg-time {
  font-size: 9px;
  color: rgba(255, 255, 255, 0.35);
}

.msg-bubble {
  background: #202b23;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 7px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.4;
  word-break: break-word;
}

.msg-item.is-self .msg-bubble {
  background: linear-gradient(135deg, #1b5e20, #2e7d32);
  border-color: #81c784;
}

/* Announcement Card */
.announcement-card {
  width: 100%;
  background: linear-gradient(135deg, #b71c1c, #880e4f);
  border: 1.5px solid #ffd700;
  border-radius: 12px;
  padding: 10px 14px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.announcement-title {
  color: #ffd54f;
  font-weight: 800;
  font-size: 12px;
  margin-bottom: 4px;
}

.announcement-body {
  font-size: 13px;
  line-height: 1.4;
  font-weight: 500;
}

.announcement-time {
  font-size: 9px;
  color: rgba(255, 255, 255, 0.6);
  text-align: right;
  margin-top: 4px;
}

.quick-phrases-bar {
  display: flex;
  overflow-x: auto;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.25);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.quick-phrases-bar::-webkit-scrollbar {
  display: none;
}

.phrase-chip {
  background: rgba(255, 255, 255, 0.08);
  border: 0.5px solid rgba(255, 215, 0, 0.3);
  color: #ffe082;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 12px;
  white-space: nowrap;
  cursor: pointer;
}

.phrase-chip:hover {
  background: rgba(255, 215, 0, 0.2);
}

.drawer-input-row {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.chat-input {
  flex: 1;
  background: #1b241e;
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #fff;
  padding: 8px 12px;
  border-radius: 18px;
  font-size: 13px;
  outline: none;
}

.chat-input:focus {
  border-color: #ffd54f;
}

.send-btn {
  background: #ffd54f;
  color: #000;
  border: none;
  font-weight: 800;
  font-size: 12px;
  padding: 0 16px;
  border-radius: 18px;
  cursor: pointer;
}

.send-btn:disabled {
  background: #37474f;
  color: rgba(255, 255, 255, 0.4);
  cursor: not-allowed;
}
</style>
