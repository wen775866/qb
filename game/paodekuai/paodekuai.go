package paodekuai

import (
	"encoding/json"
	"errors"
	"sync"

	"game-platform/game"
)

// PaodekuaiEngine 由 AI 3 预留开发的跑得快逻辑
type PaodekuaiEngine struct {
	mu sync.RWMutex
}

var globalEngine = &PaodekuaiEngine{}

func init() {
	game.RegisterEngine(globalEngine)
}

func (e *PaodekuaiEngine) GetGameType() game.GameType {
	return game.GameTypePaodekuai
}

func (e *PaodekuaiEngine) GetGameName() string {
	return "跑得快"
}

func (e *PaodekuaiEngine) CreateRoom(roomID string, baseScore int) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:     roomID,
		GameType:   game.GameTypePaodekuai,
		GameName:   "跑得快",
		Status:     "reserved",
		BaseScore:  baseScore,
		Multiplier: 1,
		Message:    "【预留接口】跑得快后端逻辑由 AI 3 负责开发接入",
	}, nil
}

func (e *PaodekuaiEngine) HandleAction(roomID string, playerID string, action string, payload []byte) (game.GameState, error) {
	return nil, errors.New("跑得快游戏逻辑正在由 AI 3 编写中...")
}

func (e *PaodekuaiEngine) GetRoomState(roomID string) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:   roomID,
		GameType: game.GameTypePaodekuai,
		GameName: "跑得快",
		Status:   "reserved",
		Message:  "【预留接口】跑得快后端逻辑由 AI 3 负责开发接入",
	}, nil
}

func (e *PaodekuaiEngine) ToJSON() ([]byte, error) {
	return json.Marshal(map[string]string{
		"gameType": string(game.GameTypePaodekuai),
		"status":   "reserved",
		"message":  "跑得快预留接口",
	})
}
