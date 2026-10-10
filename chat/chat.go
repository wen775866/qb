package chat

import (
	"sync"
	"time"
)

type MessageType string

const (
	TypePlayerChat          MessageType = "player_chat"
	TypeSystemAnnouncement  MessageType = "system_announcement"
	TypeAdminNotice         MessageType = "admin_notice"
	TypeGameNotice          MessageType = "game_notice"
)

type ChatMessage struct {
	ID        int64       `json:"id"`
	Type      MessageType `json:"type"`
	SenderID  string      `json:"senderId"`
	Sender    string      `json:"sender"`
	Avatar    string      `json:"avatar"`
	VIPLevel  int         `json:"vipLevel"`
	RoomID    string      `json:"roomId,omitempty"`
	Text      string      `json:"text"`
	Timestamp int64       `json:"timestamp"`
}

type Manager struct {
	messages []ChatMessage
	mu       sync.RWMutex
	maxCount int
}

var GlobalManager = &Manager{
	messages: make([]ChatMessage, 0, 100),
	maxCount: 100,
}

func (m *Manager) AddMessage(msg ChatMessage) ChatMessage {
	m.mu.Lock()
	defer m.mu.Unlock()

	msg.ID = time.Now().UnixNano()
	msg.Timestamp = time.Now().UnixMilli()

	m.messages = append(m.messages, msg)
	if len(m.messages) > m.maxCount {
		m.messages = m.messages[len(m.messages)-m.maxCount:]
	}
	return msg
}

func (m *Manager) GetHistory(roomID string) []ChatMessage {
	m.mu.RLock()
	defer m.mu.RUnlock()

	var result []ChatMessage
	for _, msg := range m.messages {
		if roomID == "" || msg.RoomID == "" || msg.RoomID == roomID {
			result = append(result, msg)
		}
	}
	return result
}
