package game

import "encoding/json"

// GameType 定义游戏类型标识
type GameType string

const (
	GameTypeDoudizhu   GameType = "doudizhu"   // 斗地主
	GameTypeMajiang    GameType = "majiang"    // 麻将
	GameTypePaodekuai  GameType = "paodekuai"  // 跑得快
	GameTypeShisanshui GameType = "shisanshui" // 十三水
	GameTypeChudadi    GameType = "chudadi"    // 锄大地
)

// GameEngine 是统一的游戏引擎接口，所有游戏模块均实现此接口
type GameEngine interface {
	GetGameType() GameType
	GetGameName() string
	CreateRoom(roomID string, baseScore int) (GameState, error)
	HandleAction(roomID string, playerID string, action string, payload []byte) (GameState, error)
	GetRoomState(roomID string) (GameState, error)
}

// GameState 统一状态接口
type GameState interface {
	ToJSON() ([]byte, error)
}

// BaseGameState 基础通用状态结构体
type BaseGameState struct {
	RoomID     string   `json:"roomId"`
	GameType   GameType `json:"gameType"`
	GameName   string   `json:"gameName"`
	Status     string   `json:"status"` // "waiting", "playing", "finished", "reserved"
	BaseScore  int      `json:"baseScore"`
	Multiplier int      `json:"multiplier"`
	Message    string   `json:"message"`
}

func (s *BaseGameState) ToJSON() ([]byte, error) {
	return json.Marshal(s)
}

// EngineRegistry 引擎注册管理器
var EngineRegistry = make(map[GameType]GameEngine)

// RegisterEngine 注册游戏引擎
func RegisterEngine(engine GameEngine) {
	EngineRegistry[engine.GetGameType()] = engine
}

// GetEngine 获取指定类型的游戏引擎
func GetEngine(gType GameType) (GameEngine, bool) {
	e, exists := EngineRegistry[gType]
	return e, exists
}
