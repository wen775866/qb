package majiang

import (
	"encoding/json"
	"errors"
	"sync"

	"game-platform/game"
)

// MajiangEngine 由 AI 2 预留开发的麻将逻辑
type MajiangEngine struct {
	mu sync.RWMutex
}

var globalEngine = &MajiangEngine{}

func init() {
	game.RegisterEngine(globalEngine)
}

func (e *MajiangEngine) GetGameType() game.GameType {
	return game.GameTypeMajiang
}

func (e *MajiangEngine) GetGameName() string {
	return "经典麻将"
}

func (e *MajiangEngine) CreateRoom(roomID string, baseScore int) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:     roomID,
		GameType:   game.GameTypeMajiang,
		GameName:   "经典麻将",
		Status:     "reserved",
		BaseScore:  baseScore,
		Multiplier: 1,
		Message:    "【预留接口】麻将后端逻辑由 AI 2 负责开发接入",
	}, nil
}

func (e *MajiangEngine) HandleAction(roomID string, playerID string, action string, payload []byte) (game.GameState, error) {
	return nil, errors.New("麻将游戏逻辑正在由 AI 2 编写中...")
}

func (e *MajiangEngine) GetRoomState(roomID string) (game.GameState, error) {
	return &game.BaseGameState{
		RoomID:   roomID,
		GameType: game.GameTypeMajiang,
		GameName: "经典麻将",
		Status:   "reserved",
		Message:  "【预留接口】麻将后端逻辑由 AI 2 负责开发接入",
	}, nil
}

func (e *MajiangEngine) ToJSON() ([]byte, error) {
	return json.Marshal(map[string]string{
		"gameType": string(game.GameTypeMajiang),
		"status":   "reserved",
		"message":  "麻将预留接口",
	})
}
