<template>
  <div class="reserved-game-container shisanshui-theme">
    <header class="reserved-header">
      <button class="back-btn" @click="$emit('back-to-lobby')">← 返回大厅</button>
      <span class="game-title">🌊 十三水 (罗宋/比牌)</span>
      <span class="author-tag">AI 4 预留模块</span>
    </header>

    <main class="reserved-body">
      <div class="reserved-card">
        <div class="icon-pulse">🌊</div>
        <h2>十三水桌面 UI 开发中</h2>
        <p class="desc">此板块接口与桌面组件已预留就绪，等待 <strong>AI 4</strong> 编写十三水（头道3张、中道5张、尾道5张、一键理牌、打枪全垒打）核心交互逻辑！</p>
        <div class="preview-lanes">
          <div class="lane-mock">头道: 3张</div>
          <div class="lane-mock">中道: 5张</div>
          <div class="lane-mock">尾道: 5张</div>
        </div>
        <button class="back-lobby-btn" @click="$emit('back-to-lobby')">返回大厅体验斗地主</button>
      </div>
    </main>
  </div>
</template>

<script setup>
defineEmits(['back-to-lobby']);
</script>

<style scoped>
.reserved-game-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at 50% 30%, #3e2723 0%, #261613 65%, #130a08 100%);
  color: #fff;
}

.reserved-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.5);
  border-bottom: 1px solid rgba(255, 215, 0, 0.25);
}

.back-btn {
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
}

.game-title {
  color: #ffb74d;
  font-size: 15px;
  font-weight: 800;
}

.author-tag {
  background: rgba(0, 0, 0, 0.4);
  border: 0.5px solid #ffb74d;
  color: #ffb74d;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.reserved-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.reserved-card {
  background: rgba(36, 24, 20, 0.9);
  border: 1.5px solid rgba(255, 183, 77, 0.4);
  border-radius: 20px;
  padding: 30px 24px;
  max-width: 440px;
  width: 100%;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
}

.icon-pulse {
  font-size: 48px;
  margin-bottom: 12px;
}

.reserved-card h2 {
  color: #ffb74d;
  font-size: 20px;
  margin-bottom: 10px;
}

.desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: 20px;
}

.preview-lanes {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}

.lane-mock {
  background: rgba(0, 0, 0, 0.5);
  border: 1px dashed #ffb74d;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 12px;
  color: #ffb74d;
  font-weight: bold;
}

.back-lobby-btn {
  background: #ffb74d;
  color: #000;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
</style>
