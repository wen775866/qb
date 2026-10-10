// 统一 WebSocket 通信服务
class GameWebSocketService {
  constructor() {
    this.ws = null;
    this.listeners = new Map();
    this.isConnected = false;
    this.reconnectTimer = null;
    this.url = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host + '/ws';
  }

  connect(customUrl) {
    if (customUrl) this.url = customUrl;
    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        this.isConnected = true;
        this.emit('connection_change', { connected: true });
      };

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          this.emit(data.event || 'message', data);
        } catch (e) {
          this.emit('raw_message', event.data);
        }
      };

      this.ws.onclose = () => {
        this.isConnected = false;
        this.emit('connection_change', { connected: false });
        this.scheduleReconnect();
      };

      this.ws.onerror = (err) => {
        this.emit('error', err);
      };
    } catch (e) {
      // Offline / standalone fallback
      this.isConnected = false;
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, 3000);
  }

  sendAction(gameType, roomId, action, payload = {}) {
    const message = {
      gameType,
      roomId,
      action,
      payload,
      timestamp: Date.now()
    };

    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
      return true;
    }

    // Local dispatch event fallback
    this.emit('local_action', message);
    return false;
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  off(event, callback) {
    if (!this.listeners.has(event)) return;
    const list = this.listeners.get(event).filter(cb => cb !== callback);
    this.listeners.set(event, list);
  }

  emit(event, data) {
    const list = this.listeners.get(event) || [];
    list.forEach(cb => {
      try {
        cb(data);
      } catch (err) {
        console.error('WS Listener error:', err);
      }
    });
  }
}

export const wsService = new GameWebSocketService();
