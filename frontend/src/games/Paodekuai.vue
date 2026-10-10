<template>
  <div class="reserved-game-container paodekuai-theme">
    <header class="reserved-header">
      <button class="back-btn" @click="$emit('back-to-lobby')">← 返回大厅</button>
      <span class="game-title">⚡ 跑得快 (争先16张)</span>
      <span class="author-tag">AI 3 预留模块</span>
    </header>

    <main class="reserved-body">
      <div class="reserved-card">
        <div class="icon-pulse">⚡</div>
        <h2>跑得快桌面 UI 开发中</h2>
        <p class="desc">此板块接口与桌面组件已预留就绪，等待 <strong>AI 3</strong> 编写跑得快（16张经典规则、黑桃3首出、必压规则、报单顶大）核心交互逻辑！</p>
        <div class="preview-cards">
          <div class="card-mock black">♠3</div>
          <div class="card-mock red">♥A</div>
          <div class="card-mock black">♣K</div>
          <div class="card-mock red">♦2</div>
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
  background: radial-gradient(circle at 50% 30%, #1e3c72 0%, #112344 65%, #081122 100%);
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
  color: #ffd54f;
  font-size: 15px;
  font-weight: 800;
}

.author-tag {
  background: rgba(0, 0, 0, 0.4);
  border: 0.5px solid #64b5f6;
  color: #64b5f6;
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
  background: rgba(18, 30, 48, 0.9);
  border: 1.5px solid rgba(100, 181, 246, 0.4);
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
  color: #64b5f6;
  font-size: 20px;
  margin-bottom: 10px;
}

.desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: 20px;
}

.preview-cards {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.card-mock {
  width: 44px;
  height: 64px;
  background: #fdfbf7;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 14px;
  box-shadow: 0 3px 8px rgba(0,0,0,0.4);
}

.card-mock.black {
  color: #1a1a1a;
}

.card-mock.red {
  color: #d32f2f;
}

.back-lobby-btn {
  background: #64b5f6;
  color: #000;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
</style>
