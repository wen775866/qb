package hub

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 65536
	sendBufferSize = 256
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true // Allow cross-origin WebSocket connections
	},
}

type WSMessage struct {
	Event     string          `json:"event"`
	Sender    string          `json:"sender,omitempty"`
	RoomID    string          `json:"roomId,omitempty"`
	Data      json.RawMessage `json:"data,omitempty"`
	Timestamp int64           `json:"timestamp"`
}

type Client struct {
	hub        *Hub
	conn       *websocket.Conn
	send       chan []byte // Asynchronous non-blocking outbound channel
	PlayerID   string
	PlayerName string
	Avatar     string
	VIPLevel   int
	RoomID     string
	mu         sync.Mutex
}

func (c *Client) readPump() {
	defer func() {
		c.hub.unregister <- c
		c.conn.Close()
	}()

	c.conn.SetReadLimit(maxMessageSize)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("WebSocket read error: %v", err)
			}
			break
		}

		var wsMsg WSMessage
		if err := json.Unmarshal(message, &wsMsg); err == nil {
			wsMsg.Timestamp = time.Now().UnixMilli()
			if wsMsg.Sender == "" {
				wsMsg.Sender = c.PlayerName
			}
			c.hub.incoming <- ClientMessage{client: c, msg: wsMsg}
		}
	}
}

func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				// The hub closed the channel.
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := c.conn.NextWriter(websocket.TextMessage)
			if err != nil {
				return
			}
			w.Write(message)

			// Drain queued messages into the same write buffer for high throughput
			n := len(c.send)
			for i := 0; i < n; i++ {
				w.Write([]byte{'\n'})
				w.Write(<-c.send)
			}

			if err := w.Close(); err != nil {
				return
			}

		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

type ClientMessage struct {
	client *Client
	msg    WSMessage
}

type RoomBroadcastMessage struct {
	RoomID string
	Data   []byte
}

type Hub struct {
	clients       map[*Client]bool
	roomClients   map[string]map[*Client]bool
	broadcast     chan []byte
	roomBroadcast chan RoomBroadcastMessage
	incoming      chan ClientMessage
	register      chan *Client
	unregister    chan *Client
	mu            sync.RWMutex
	onMessage     func(c *Client, msg WSMessage)
}

func NewHub(onMsg func(c *Client, msg WSMessage)) *Hub {
	return &Hub{
		clients:       make(map[*Client]bool),
		roomClients:   make(map[string]map[*Client]bool),
		broadcast:     make(chan []byte, 1024),
		roomBroadcast: make(chan RoomBroadcastMessage, 1024),
		incoming:      make(chan ClientMessage, 1024),
		register:      make(chan *Client, 256),
		unregister:    make(chan *Client, 256),
		onMessage:     onMsg,
	}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			h.clients[client] = true
			if client.RoomID != "" {
				if _, ok := h.roomClients[client.RoomID]; !ok {
					h.roomClients[client.RoomID] = make(map[*Client]bool)
				}
				h.roomClients[client.RoomID][client] = true
			}
			h.mu.Unlock()

			// Broadcast online user count update
			h.BroadcastOnlineCountAsync()

		case client := <-h.unregister:
			h.mu.Lock()
			if _, ok := h.clients[client]; ok {
				delete(h.clients, client)
				close(client.send)
				if client.RoomID != "" && h.roomClients[client.RoomID] != nil {
					delete(h.roomClients[client.RoomID], client)
					if len(h.roomClients[client.RoomID]) == 0 {
						delete(h.roomClients, client.RoomID)
					}
				}
			}
			h.mu.Unlock()

			h.BroadcastOnlineCountAsync()

		// Global Asynchronous Non-blocking Broadcast (Goroutine + Channel)
		case message := <-h.broadcast:
			h.mu.RLock()
			for client := range h.clients {
				select {
				case client.send <- message:
				default:
					// Non-blocking: client channel is full/stalled, remove it safely
					go func(c *Client) {
						h.unregister <- c
					}(client)
				}
			}
			h.mu.RUnlock()

		// Room-level Asynchronous Non-blocking Broadcast
		case rbm := <-h.roomBroadcast:
			h.mu.RLock()
			if clients, ok := h.roomClients[rbm.RoomID]; ok {
				for client := range clients {
					select {
					case client.send <- rbm.Data:
					default:
						go func(c *Client) {
							h.unregister <- c
						}(client)
					}
				}
			}
			h.mu.RUnlock()

		case cm := <-h.incoming:
			if h.onMessage != nil {
				go h.onMessage(cm.client, cm.msg)
			}
		}
	}
}

// BroadcastAsync sends a global message asynchronously without blocking the caller
func (h *Hub) BroadcastAsync(event string, payload interface{}) {
	go func() {
		raw, err := json.Marshal(payload)
		if err != nil {
			return
		}
		msg := WSMessage{
			Event:     event,
			Data:      raw,
			Timestamp: time.Now().UnixMilli(),
		}
		bytes, err := json.Marshal(msg)
		if err != nil {
			return
		}

		select {
		case h.broadcast <- bytes:
		default:
			log.Println("Hub broadcast channel full, skipping message")
		}
	}()
}

// BroadcastToRoomAsync sends a room message asynchronously without blocking the caller
func (h *Hub) BroadcastToRoomAsync(roomID string, event string, payload interface{}) {
	go func() {
		raw, err := json.Marshal(payload)
		if err != nil {
			return
		}
		msg := WSMessage{
			Event:     event,
			RoomID:    roomID,
			Data:      raw,
			Timestamp: time.Now().UnixMilli(),
		}
		bytes, err := json.Marshal(msg)
		if err != nil {
			return
		}

		select {
		case h.roomBroadcast <- RoomBroadcastMessage{RoomID: roomID, Data: bytes}:
		default:
			log.Printf("Hub room %s broadcast channel full, skipping", roomID)
		}
	}()
}

func (h *Hub) BroadcastOnlineCountAsync() {
	h.mu.RLock()
	count := len(h.clients)
	h.mu.RUnlock()

	h.BroadcastAsync("online_count", map[string]interface{}{
		"count": count,
	})
}

func (h *Hub) GetOnlineCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients)
}

func (h *Hub) ServeWS(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("WebSocket upgrade error: %v", err)
		return
	}

	playerID := r.URL.Query().Get("playerId")
	if playerID == "" {
		playerID = "guest_" + time.Now().Format("150405")
	}
	playerName := r.URL.Query().Get("name")
	if playerName == "" {
		playerName = "玩家"
	}
	roomID := r.URL.Query().Get("roomId")

	client := &Client{
		hub:        h,
		conn:       conn,
		send:       make(chan []byte, sendBufferSize),
		PlayerID:   playerID,
		PlayerName: playerName,
		Avatar:     r.URL.Query().Get("avatar"),
		RoomID:     roomID,
	}

	h.register <- client

	// Start asynchronous reader and writer goroutines
	go client.writePump()
	go client.readPump()
}
