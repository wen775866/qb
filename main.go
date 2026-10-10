package main

import (
	"embed"
	"encoding/json"
	"fmt"
	"io"
	"io/fs"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"game-platform/chat"
	"game-platform/game"
	_ "game-platform/game/chudadi"
	_ "game-platform/game/doudizhu"
	_ "game-platform/game/majiang"
	_ "game-platform/game/paodekuai"
	_ "game-platform/game/shisanshui"
	"game-platform/hub"
	"game-platform/telegram"
)

//go:embed frontend/dist/*
var embeddedFrontend embed.FS

type ActionRequest struct {
	RoomID   string          `json:"roomId"`
	PlayerID string          `json:"playerId"`
	GameType game.GameType   `json:"gameType"`
	Action   string          `json:"action"`
	Payload  json.RawMessage `json:"payload"`
}

var (
	wsHub     *hub.Hub
	startTime = time.Now()
)

// loadEnv 自动按优先级探测加载上级目录 ../.env (Termux 根目录) 及当前目录 ./.env
func loadEnv() {
	envFiles := []string{"../.env", ".env"}
	for _, envFile := range envFiles {
		data, err := os.ReadFile(envFile)
		if err == nil {
			log.Printf("[Env] 发现并加载环境变量文件: %s", envFile)
			lines := strings.Split(string(data), "\n")
			for _, line := range lines {
				line = strings.TrimSpace(line)
				if line == "" || strings.HasPrefix(line, "#") {
					continue
				}
				parts := strings.SplitN(line, "=", 2)
				if len(parts) == 2 {
					key := strings.TrimSpace(parts[0])
					val := strings.TrimSpace(parts[1])
					val = strings.Trim(val, `"'`)
					if os.Getenv(key) == "" {
						os.Setenv(key, val)
					}
				}
			}
		}
	}
}

func main() {
	// 0. 加载本地或 Termux 上级目录的环境变量配置
	loadEnv()

	// 1. 初始化 WebSocket 异步非阻塞广播中心 (Goroutine + Channel)
	wsHub = hub.NewHub(func(client *hub.Client, msg hub.WSMessage) {
		switch msg.Event {
		case "chat_message":
			var payload struct {
				Text   string `json:"text"`
				RoomID string `json:"roomId"`
			}
			if err := json.Unmarshal(msg.Data, &payload); err == nil && payload.Text != "" {
				chatMsg := chat.ChatMessage{
					Type:     chat.TypePlayerChat,
					SenderID: client.PlayerID,
					Sender:   client.PlayerName,
					Avatar:   client.Avatar,
					VIPLevel: client.VIPLevel,
					RoomID:   payload.RoomID,
					Text:     payload.Text,
				}
				saved := chat.GlobalManager.AddMessage(chatMsg)

				// 采用 Goroutine + Channel 异步非阻塞广播
				if payload.RoomID != "" {
					wsHub.BroadcastToRoomAsync(payload.RoomID, "chat_message", saved)
				} else {
					wsHub.BroadcastAsync("chat_message", saved)
				}
			}

		case "join_room":
			var payload struct {
				RoomID string `json:"roomId"`
			}
			if err := json.Unmarshal(msg.Data, &payload); err == nil {
				client.RoomID = payload.RoomID
			}
		}
	})
	go wsHub.Run()

	// 2. 初始化 Telegram 管理员服务回调
	telegram.GlobalService.OnBroadcast = func(text string) {
		announcement := chat.ChatMessage{
			Type:   chat.TypeSystemAnnouncement,
			Sender: "系统管理员 (Telegram)",
			Text:   text,
		}
		saved := chat.GlobalManager.AddMessage(announcement)
		wsHub.BroadcastAsync("system_announcement", saved)
	}

	telegram.GlobalService.OnGiveCoins = func(playerID string, amount int64) {
		wsHub.BroadcastAsync("coin_gift", map[string]interface{}{
			"playerId": playerID,
			"amount":   amount,
			"reason":   "Telegram 管理员特批发赠",
		})
	}

	telegram.GlobalService.GetStatus = func() map[string]interface{} {
		return map[string]interface{}{
			"onlineCount": wsHub.GetOnlineCount(),
			"roomCount":   4,
			"uptime":      time.Since(startTime).Truncate(time.Second).String(),
		}
	}

	// 3. 自动检测并启动 Telegram Bot 管理员长轮询服务
	tgBotToken := os.Getenv("TELEGRAM_BOT_TOKEN")
	if tgBotToken == "" {
		tgBotToken = os.Getenv("TG_BOT_TOKEN")
	}
	tgAdminChatID := os.Getenv("TELEGRAM_ADMIN_CHAT_ID")
	if tgAdminChatID == "" {
		tgAdminChatID = os.Getenv("TG_CHAT_ID")
	}
	if tgBotToken != "" {
		telegram.GlobalService.UpdateConfig(telegram.Config{
			BotToken:    tgBotToken,
			AdminChatID: tgAdminChatID,
			Enabled:     true,
			AutoAlerts:  true,
		})
		telegram.GlobalService.StartPolling()
	}

	mux := http.NewServeMux()

	// WebSocket 端点
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		wsHub.ServeWS(w, r)
	})

	// 获取所有支持的游戏板块列表
	mux.HandleFunc("/api/games", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")

		gamesList := []map[string]interface{}{
			{
				"id":          game.GameTypeDoudizhu,
				"name":        "经典斗地主",
				"badge":       "🎴",
				"status":      "ready",
				"author":      "AI 1 (主控完善)",
				"description": "经典三人斗地主，含叫抢地主、加倍、炸弹王炸、智能AI对战",
			},
			{
				"id":          game.GameTypeMajiang,
				"name":        "国粹麻将",
				"badge":       "🀄",
				"status":      "reserved",
				"author":      "AI 2 (预留接入)",
				"description": "四人国标/血战麻将，预留接口等待 AI 2 编写逻辑",
			},
			{
				"id":          game.GameTypePaodekuai,
				"name":        "跑得快",
				"badge":       "⚡",
				"status":      "reserved",
				"author":      "AI 3 (预留接入)",
				"description": "争先跑得快，首出黑桃3，预留接口等待 AI 3 编写逻辑",
			},
			{
				"id":          game.GameTypeShisanshui,
				"name":        "十三水",
				"badge":       "🌊",
				"status":      "reserved",
				"author":      "AI 4 (预留接入)",
				"description": "十三张策略比牌摆道，预留接口等待 AI 4 编写逻辑",
			},
			{
				"id":          game.GameTypeChudadi,
				"name":        "锄大地",
				"badge":       "👑",
				"status":      "reserved",
				"author":      "AI 5 (预留接入)",
				"description": "粤港步步高大老二，预留接口等待 AI 5 编写逻辑",
			},
		}
		json.NewEncoder(w).Encode(gamesList)
	})

	// 聊天历史记录 API
	mux.HandleFunc("/api/chat/history", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		roomID := r.URL.Query().Get("roomId")
		history := chat.GlobalManager.GetHistory(roomID)
		json.NewEncoder(w).Encode(history)
	})

	// 发送聊天 API
	mux.HandleFunc("/api/chat/send", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		if r.Method == "OPTIONS" {
			w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			return
		}

		var req chat.ChatMessage
		body, _ := io.ReadAll(r.Body)
		if err := json.Unmarshal(body, &req); err == nil && req.Text != "" {
			req.Type = chat.TypePlayerChat
			saved := chat.GlobalManager.AddMessage(req)
			wsHub.BroadcastAsync("chat_message", saved)
			json.NewEncoder(w).Encode(map[string]interface{}{"success": true, "message": saved})
			return
		}
		http.Error(w, "invalid message payload", http.StatusBadRequest)
	})

	// Telegram 管理员配置与测试 API
	mux.HandleFunc("/api/telegram/config", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		if r.Method == "OPTIONS" {
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			return
		}

		if r.Method == "POST" {
			var cfg telegram.Config
			body, _ := io.ReadAll(r.Body)
			if err := json.Unmarshal(body, &cfg); err == nil {
				telegram.GlobalService.UpdateConfig(cfg)
				json.NewEncoder(w).Encode(map[string]interface{}{"success": true, "config": cfg})
				return
			}
			http.Error(w, "bad request", http.StatusBadRequest)
			return
		}

		cfg := telegram.GlobalService.GetConfig()
		json.NewEncoder(w).Encode(cfg)
	})

	mux.HandleFunc("/api/telegram/test", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		if r.Method == "OPTIONS" {
			w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			return
		}

		var req struct {
			Message string `json:"message"`
		}
		body, _ := io.ReadAll(r.Body)
		json.Unmarshal(body, &req)
		if req.Message == "" {
			req.Message = "🔔 这是一条来自棋牌游戏平台的 Telegram 测试推送消息！"
		}

		telegram.GlobalService.SendMessageAsync("", req.Message)
		json.NewEncoder(w).Encode(map[string]interface{}{"success": true, "sent": true})
	})

	mux.HandleFunc("/api/telegram/broadcast", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		if r.Method == "OPTIONS" {
			w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			return
		}

		var req struct {
			Text string `json:"text"`
		}
		body, _ := io.ReadAll(r.Body)
		json.Unmarshal(body, &req)

		if req.Text != "" {
			telegram.GlobalService.OnBroadcast(req.Text)
			json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
			return
		}
		http.Error(w, "empty broadcast message", http.StatusBadRequest)
	})

	mux.HandleFunc("/api/telegram/webhook", func(w http.ResponseWriter, r *http.Request) {
		telegram.GlobalService.HandleWebhook(w, r)
	})

	// 房间创建
	mux.HandleFunc("/api/room/create", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")

		var req struct {
			GameType  game.GameType `json:"gameType"`
			RoomID    string        `json:"roomId"`
			BaseScore int           `json:"baseScore"`
		}
		body, _ := io.ReadAll(r.Body)
		json.Unmarshal(body, &req)

		engine, exists := game.GetEngine(req.GameType)
		if !exists {
			http.Error(w, fmt.Sprintf("Game engine %s not found", req.GameType), http.StatusBadRequest)
			return
		}

		state, err := engine.CreateRoom(req.RoomID, req.BaseScore)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		raw, _ := state.ToJSON()
		w.Write(raw)
	})

	// 战报推送
	mux.HandleFunc("/api/game/report_win", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("Access-Control-Allow-Origin", "*")
		if r.Method == "OPTIONS" {
			w.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
			return
		}

		var req struct {
			PlayerName string `json:"playerName"`
			RoomName   string `json:"roomName"`
			Multiplier int    `json:"multiplier"`
			Coins      int64  `json:"coins"`
			IsSpring   bool   `json:"isSpring"`
		}
		body, _ := io.ReadAll(r.Body)
		if err := json.Unmarshal(body, &req); err == nil {
			telegram.GlobalService.NotifyHighMultiplierAlert(req.PlayerName, req.RoomName, req.Multiplier, req.Coins, req.IsSpring)
			json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
			return
		}
		http.Error(w, "bad report payload", http.StatusBadRequest)
	})

	// 单体融合核心：嵌入式静态资源托管与 SPA 回退
	distFS, err := fs.Sub(embeddedFrontend, "frontend/dist")
	if err != nil {
		log.Printf("Warning: embedded dist not found: %v, falling back to disk", err)
		distFS = os.DirFS("./frontend/dist")
	}

	fileServer := http.FileServer(http.FS(distFS))

	// 根路径分发：处理静态资源与 SPA index.html 回退
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		path := strings.TrimPrefix(r.URL.Path, "/")
		if path == "" {
			path = "index.html"
		}

		// 尝试读取文件
		f, err := distFS.Open(path)
		if err != nil {
			// SPA 客户端路由回退：提供 index.html
			indexFile, err := distFS.Open("index.html")
			if err != nil {
				http.NotFound(w, r)
				return
			}
			defer indexFile.Close()
			data, _ := io.ReadAll(indexFile)
			w.Header().Set("Content-Type", "text/html; charset=utf-8")
			w.Write(data)
			return
		}
		f.Close()

		fileServer.ServeHTTP(w, r)
	})

	port := os.Getenv("APP_PORT")
	if port == "" {
		port = os.Getenv("PORT")
		if port == "" || port == "8080" {
			port = "3000" // 默认端口 3000
		}
	}

	hasTunnel := os.Getenv("CLOUDFLARE_TUNNEL_TOKEN") != ""
	if tgBotToken != "" {
		telegram.GlobalService.SendStartupNotification(port, hasTunnel)
	}

	log.Printf("=====================================================")
	log.Printf(" 单体全栈棋牌项目 Go Server 已启动！")
	log.Printf(" 监听端口: :%s (HTTP + WS + Vue3 SPA 静态资源融合)", port)
	log.Printf(" 斗地主模块: 完整启用 (含叫抢/加倍/智能AI/春天/炸弹)")
	log.Printf(" WebSocket: Goroutine + Channel 异步非阻塞广播中心")
	log.Printf(" Telegram 管理员: 轮询就绪 (支持 /status, /broadcast, /give_coins)")
	log.Printf(" Cloudflare Tunnel: 配置状态=%v", hasTunnel)
	log.Printf("=====================================================")

	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), mux); err != nil {
		log.Fatalf("Server startup failed: %v", err)
	}
}
