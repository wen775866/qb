package doudizhu

import (
	"encoding/json"
	"errors"
	"math/rand"
	"sort"
	"sync"
	"time"

	"game-platform/game"
)

// Card represents a playing card in Go
type Card struct {
	ID      int    `json:"id"`
	Suit    string `json:"suit"` // "♠", "♥", "♣", "♦", "★"
	Rank    string `json:"rank"` // "3".."10", "J", "Q", "K", "A", "2", "小王", "大王"
	Weight  int    `json:"weight"`
	IsRed   bool   `json:"isRed"`
	IsJoker bool   `json:"isJoker"`
}

// HandType represents combination classification
type HandType int

const (
	TypeInvalid HandType = iota
	TypeSingle
	TypePair
	TypeTriplet
	TypeTripletWithOne
	TypeTripletWithTwo
	TypeStraight
	TypeConsecutivePairs
	TypeAirplane
	TypeAirplaneWithWings
	TypeFourWithTwo
	TypeBomb
	TypeRocket
)

type Combo struct {
	Type       HandType `json:"type"`
	TypeName   string   `json:"typeName"`
	MainWeight int      `json:"mainWeight"`
	Length     int      `json:"length"`
	Cards      []Card   `json:"cards"`
}

type PlayerState struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	IsBot     bool   `json:"isBot"`
	Role      string `json:"role"` // "landlord", "farmer", ""
	CardCount int    `json:"cardCount"`
	Cards     []Card `json:"cards,omitempty"` // only visible for player
}

type DoudizhuState struct {
	RoomID        string        `json:"roomId"`
	GameType      game.GameType `json:"gameType"`
	Phase         string        `json:"phase"` // "BIDDING", "DOUBLING", "PLAYING", "GAME_OVER"
	CurrentTurn   int           `json:"currentTurn"`
	LandlordSeat  int           `json:"landlordSeat"`
	BaseScore     int           `json:"baseScore"`
	Multiplier    int           `json:"multiplier"`
	MaxMultiplier int           `json:"maxMultiplier"`
	BottomCards   []Card        `json:"bottomCards"`
	Players       []PlayerState `json:"players"`
	LastCombo     *Combo        `json:"lastCombo"`
	LastPlayer    int           `json:"lastPlayer"`
	PassCount     int           `json:"passCount"`
	WinnerSeat    int           `json:"winnerSeat"`
	IsSpring      bool          `json:"isSpring"`
	Message       string        `json:"message"`
}

func (s *DoudizhuState) ToJSON() ([]byte, error) {
	return json.Marshal(s)
}

type DoudizhuEngine struct {
	mu    sync.RWMutex
	rooms map[string]*DoudizhuState
}

var globalEngine = &DoudizhuEngine{
	rooms: make(map[string]*DoudizhuState),
}

func init() {
	game.RegisterEngine(globalEngine)
}

func (e *DoudizhuEngine) GetGameType() game.GameType {
	return game.GameTypeDoudizhu
}

func (e *DoudizhuEngine) GetGameName() string {
	return "经典斗地主"
}

func (e *DoudizhuEngine) CreateRoom(roomID string, baseScore int) (game.GameState, error) {
	e.mu.Lock()
	defer e.mu.Unlock()

	deck := Generate54Deck()
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	r.Shuffle(len(deck), func(i, j int) { deck[i], deck[j] = deck[j], deck[i] })

	h0 := SortCards(deck[0:17])
	h1 := SortCards(deck[17:34])
	h2 := SortCards(deck[34:51])
	bottom := deck[51:54]

	state := &DoudizhuState{
		RoomID:        roomID,
		GameType:      game.GameTypeDoudizhu,
		Phase:         "BIDDING",
		CurrentTurn:   0,
		LandlordSeat:  -1,
		BaseScore:     baseScore,
		Multiplier:    1,
		MaxMultiplier: 128,
		BottomCards:   bottom,
		WinnerSeat:    -1,
		Players: []PlayerState{
			{ID: "player_0", Name: "玩家", IsBot: false, CardCount: len(h0), Cards: h0},
			{ID: "bot_1", Name: "赌侠小华", IsBot: true, CardCount: len(h1), Cards: h1},
			{ID: "bot_2", Name: "牌神阿强", IsBot: true, CardCount: len(h2), Cards: h2},
		},
	}

	e.rooms[roomID] = state
	return state, nil
}

func (e *DoudizhuEngine) GetRoomState(roomID string) (game.GameState, error) {
	e.mu.RLock()
	defer e.mu.RUnlock()
	state, exists := e.rooms[roomID]
	if !exists {
		return nil, errors.New("room not found")
	}
	return state, nil
}

func (e *DoudizhuEngine) HandleAction(roomID string, playerID string, action string, payload []byte) (game.GameState, error) {
	e.mu.Lock()
	defer e.mu.Unlock()

	state, exists := e.rooms[roomID]
	if !exists {
		return nil, errors.New("room not found")
	}

	switch action {
	case "bid":
		var req struct {
			WillBid bool `json:"willBid"`
		}
		if err := json.Unmarshal(payload, &req); err == nil {
			if req.WillBid {
				state.Multiplier *= 2
				state.LandlordSeat = state.CurrentTurn
			}
			state.CurrentTurn++
			if state.CurrentTurn > 2 {
				// End bidding
				if state.LandlordSeat == -1 {
					state.LandlordSeat = 0
				}
				// Allocate hole cards
				landlord := &state.Players[state.LandlordSeat]
				landlord.Cards = SortCards(append(landlord.Cards, state.BottomCards...))
				landlord.CardCount = len(landlord.Cards)
				landlord.Role = "landlord"
				for i := range state.Players {
					if i != state.LandlordSeat {
						state.Players[i].Role = "farmer"
					}
				}
				state.Phase = "PLAYING"
				state.CurrentTurn = state.LandlordSeat
			}
		}

	case "play":
		var req struct {
			CardIDs []int `json:"cardIds"`
		}
		if err := json.Unmarshal(payload, &req); err == nil {
			player := &state.Players[state.CurrentTurn]
			var selected []Card
			for _, id := range req.CardIDs {
				for _, c := range player.Cards {
					if c.ID == id {
						selected = append(selected, c)
						break
					}
				}
			}

			combo := ParseCombo(selected)
			if combo != nil && CanBeat(state.LastCombo, combo) {
				state.LastCombo = combo
				state.LastPlayer = state.CurrentTurn
				state.PassCount = 0

				if combo.Type == TypeBomb || combo.Type == TypeRocket {
					state.Multiplier *= 2
				}

				// Remove played cards
				var remaining []Card
				for _, c := range player.Cards {
					played := false
					for _, sel := range selected {
						if sel.ID == c.ID {
							played = true
							break
						}
					}
					if !played {
						remaining = append(remaining, c)
					}
				}
				player.Cards = remaining
				player.CardCount = len(remaining)

				if player.CardCount == 0 {
					state.Phase = "GAME_OVER"
					state.WinnerSeat = state.CurrentTurn
				} else {
					state.CurrentTurn = (state.CurrentTurn + 1) % 3
				}
			}
		}

	case "pass":
		state.PassCount++
		if state.PassCount >= 2 {
			state.LastCombo = nil
			state.LastPlayer = -1
		}
		state.CurrentTurn = (state.CurrentTurn + 1) % 3
	}

	return state, nil
}

// Helpers for Cards and Rules
func Generate54Deck() []Card {
	var cards []Card
	id := 1
	suits := []string{"♠", "♥", "♣", "♦"}
	ranks := []struct {
		Text   string
		Weight int
	}{
		{"3", 3}, {"4", 4}, {"5", 5}, {"6", 6}, {"7", 7}, {"8", 8},
		{"9", 9}, {"10", 10}, {"J", 11}, {"Q", 12}, {"K", 13}, {"A", 14}, {"2", 15},
	}

	for _, r := range ranks {
		for _, s := range suits {
			isRed := (s == "♥" || s == "♦")
			cards = append(cards, Card{
				ID:      id,
				Suit:    s,
				Rank:    r.Text,
				Weight:  r.Weight,
				IsRed:   isRed,
				IsJoker: false,
			})
			id++
		}
	}
	cards = append(cards, Card{ID: id, Suit: "★", Rank: "小王", Weight: 16, IsRed: false, IsJoker: true})
	id++
	cards = append(cards, Card{ID: id, Suit: "★", Rank: "大王", Weight: 17, IsRed: true, IsJoker: true})
	return cards
}

func SortCards(cards []Card) []Card {
	res := make([]Card, len(cards))
	copy(res, cards)
	sort.Slice(res, func(i, j int) bool {
		return res[i].Weight > res[j].Weight
	})
	return res
}

func ParseCombo(cards []Card) *Combo {
	n := len(cards)
	if n == 0 {
		return nil
	}
	sorted := SortCards(cards)

	// Rocket
	if n == 2 && sorted[0].Weight == 17 && sorted[1].Weight == 16 {
		return &Combo{Type: TypeRocket, TypeName: "王炸", MainWeight: 100, Length: 2, Cards: sorted}
	}

	// Single
	if n == 1 {
		return &Combo{Type: TypeSingle, TypeName: "单张", MainWeight: sorted[0].Weight, Length: 1, Cards: sorted}
	}

	// Pair
	if n == 2 && sorted[0].Weight == sorted[1].Weight {
		return &Combo{Type: TypePair, TypeName: "对子", MainWeight: sorted[0].Weight, Length: 2, Cards: sorted}
	}

	// Bomb
	if n == 4 && sorted[0].Weight == sorted[1].Weight && sorted[1].Weight == sorted[2].Weight && sorted[2].Weight == sorted[3].Weight {
		return &Combo{Type: TypeBomb, TypeName: "炸弹", MainWeight: sorted[0].Weight, Length: 4, Cards: sorted}
	}

	return nil
}

func CanBeat(lastCombo *Combo, candidate *Combo) bool {
	if lastCombo == nil {
		return true
	}
	if candidate.Type == TypeRocket {
		return true
	}
	if lastCombo.Type == TypeRocket {
		return false
	}
	if candidate.Type == TypeBomb {
		if lastCombo.Type != TypeBomb {
			return true
		}
		return candidate.MainWeight > lastCombo.MainWeight
	}
	if candidate.Type != lastCombo.Type || candidate.Length != lastCombo.Length {
		return false
	}
	return candidate.MainWeight > lastCombo.MainWeight
}
