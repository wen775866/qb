<template>
  <div class="reserved-game-container majiang-theme">
    <header class="reserved-header">
      <button class="back-btn" @click="$emit('back-to-lobby')">← 返回大厅</button>
      <span class="game-title">🀄 国粹麻将 (四人经典)</span>
      <span class="author-tag">AI 2 预留模块</span>
    </header>

    <main class="reserved-body">
      <div class="reserved-card">
        <div class="icon-pulse">🀄</div>
        <h2>麻将桌面 UI 开发中</h2>
        <p class="desc">此板块接口与桌面组件已预留就绪，等待 <strong>AI 2</strong> 编写完整的麻将（摸牌、打牌、碰、杠、胡）核心交互逻辑！</p>
        <div class="preview-tiles">
          <div class="tile-mock">一万</div>
          <div class="tile-mock">九万</div>
          <div class="tile-mock">一条</div>
          <div class="tile-mock">九条</div>
          <div class="tile-mock green">发</div>
          <div class="tile-mock red">中</div>
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
  background: radial-gradient(circle at 50% 30%, #15452d 0%, #0c2b1c 65%, #051a10 100%);
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
  border: 0.5px solid #81c784;
  color: #81c784;
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
  background: rgba(20, 30, 24, 0.9);
  border: 1.5px solid rgba(255, 215, 0, 0.4);
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
  color: #ffd54f;
  font-size: 20px;
  margin-bottom: 10px;
}

.desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: 20px;
}

.preview-tiles {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 24px;
}

.tile-mock {
  width: 38px;
  height: 52px;
  background: #fdfbf7;
  border: 1px solid #c7bfa8;
  border-radius: 6px;
  color: #c62828;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

.tile-mock.green {
  color: #2e7d32;
}

.tile-mock.red {
  color: #c62828;
}

.back-lobby-btn {
  background: #ffd54f;
  color: #000;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}
</style>
