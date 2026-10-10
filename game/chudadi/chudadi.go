package chudadi

import (
	"encoding/json"
	"errors"
	"sync"

	"game-platform/game"
)

// ChudadiEngine 由 AI 5 预留开发的锄大地逻辑
type ChudadiEngine struct {
	mu sync.RWMutex
}

var globalEngine = &ChudadiEngine{}

func init() {
	game.RegisterEngine(globalEngine)
}

func (e *ChudadiEngine) GetGameType() game.GameType {
	return game.GameTypeChudadi
}

func (e *ChudadiEngine) GetGameName() string {
	return "锄大地"
}

func (e *ChudadiEngine) CreateRoom(roomID string, baseScore int) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:     roomID,
		GameType:   game.GameTypeChudadi,
		GameName:   "锄大地",
		Status:     "reserved",
		BaseScore:  baseScore,
		Multiplier: 1,
		Message:    "【预留接口】锄大地后端逻辑由 AI 5 负责开发接入",
	}, nil
}

func (e *ChudadiEngine) HandleAction(roomID string, playerID string, action string, payload []byte) (game.GameState, error) {
	return nil, errors.New("锄大地游戏逻辑正在由 AI 5 编写中...")
}

func (e *ChudadiEngine) GetRoomState(roomID string) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:   roomID,
		GameType: game.GameTypeChudadi,
		GameName: "锄大地",
		Status:   "reserved",
		Message:  "【预留接口】锄大地后端逻辑由 AI 5 负责开发接入",
	}, nil
}

func (e *ChudadiEngine) ToJSON() ([]byte, error) {
	return json.Marshal(map[string]string{
		"gameType": string(game.GameTypeChudadi),
		"status":   "reserved",
		"message":  "锄大地预留接口",
	})
}
