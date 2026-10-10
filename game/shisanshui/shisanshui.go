package shisanshui

import (
	"encoding/json"
	"errors"
	"sync"

	"game-platform/game"
)

// ShisanshuiEngine 由 AI 4 预留开发的十三水逻辑
type ShisanshuiEngine struct {
	mu sync.RWMutex
}

var globalEngine = &ShisanshuiEngine{}

func init() {
	game.RegisterEngine(globalEngine)
}

func (e *ShisanshuiEngine) GetGameType() game.GameType {
	return game.GameTypeShisanshui
}

func (e *ShisanshuiEngine) GetGameName() string {
	return "十三水"
}

func (e *ShisanshuiEngine) CreateRoom(roomID string, baseScore int) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:     roomID,
		GameType:   game.GameTypeShisanshui,
		GameName:   "十三水",
		Status:     "reserved",
		BaseScore:  baseScore,
		Multiplier: 1,
		Message:    "【预留接口】十三水后端逻辑由 AI 4 负责开发接入",
	}, nil
}

func (e *ShisanshuiEngine) HandleAction(roomID string, playerID string, action string, payload []byte) (game.GameState, error) {
	return nil, errors.New("十三水游戏逻辑正在由 AI 4 编写中...")
}

func (e *ShisanshuiEngine) GetRoomState(roomID string) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:   roomID,
		GameType: game.GameTypeShisanshui,
		GameName: "十三水",
		Status:   "reserved",
		Message:  "【预留接口】十三水后端逻辑由 AI 4 负责开发接入",
	}, nil
}

func (e *ShisanshuiEngine) ToJSON() ([]byte, error) {
	return json.Marshal(map[string]string{
		"gameType": string(game.GameTypeShisanshui),
		"status":   "reserved",
		"message":  "十三水预留接口",
	})
}
