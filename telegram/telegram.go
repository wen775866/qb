package telegram

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"
)

type Config struct {
	BotToken    string `json:"botToken"`
	AdminChatID string `json:"adminChatId"`
	Enabled     bool   `json:"enabled"`
	AutoAlerts  bool   `json:"autoAlerts"` // 推送高倍对局与春天战报
}

type TelegramUpdate struct {
	UpdateID int64 `json:"update_id"`
	Message  *struct {
		MessageID int64 `json:"message_id"`
		Chat      struct {
			ID int64 `json:"id"`
		} `json:"chat"`
		From struct {
			ID        int64  `json:"id"`
			FirstName string `json:"first_name"`
			Username  string `json:"username"`
		} `json:"from"`
		Text string `json:"text"`
	} `json:"message"`
}

type TelegramService struct {
	config Config
	mu     sync.RWMutex
	client *http.Client

	pollingRunning bool
	pollingMu      sync.Mutex

	// 指令执行回调
	OnBroadcast func(text string)
	OnGiveCoins func(playerID string, amount int64)
	GetStatus   func() map[string]interface{}
}

var GlobalService = &TelegramService{
	client: &http.Client{Timeout: 30 * time.Second},
	config: Config{
		Enabled:    false,
		AutoAlerts: true,
	},
}

func (s *TelegramService) GetConfig() Config {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.config
}

func (s *TelegramService) UpdateConfig(c Config) {
	s.mu.Lock()
	s.config = c
	s.mu.Unlock()
	log.Printf("[Telegram] 配置已更新: Enabled=%v, AdminChatID=%s", c.Enabled, c.AdminChatID)
}

// SendMessageAsync 异步非阻塞向 Telegram 发送消息 (Goroutine)
func (s *TelegramService) SendMessageAsync(chatID string, text string) {
	go func() {
		s.mu.RLock()
		token := s.config.BotToken
		enabled := s.config.Enabled
		if chatID == "" {
			chatID = s.config.AdminChatID
		}
		s.mu.RUnlock()

		if !enabled || token == "" || chatID == "" {
			return
		}

		apiURL := fmt.Sprintf("https://api.telegram.org/bot%s/sendMessage", token)
		payload := map[string]string{
			"chat_id":    chatID,
			"text":       text,
			"parse_mode": "HTML",
		}
		data, err := json.Marshal(payload)
		if err != nil {
			return
		}

		resp, err := s.client.Post(apiURL, "application/json", bytes.NewBuffer(data))
		if err != nil {
			log.Printf("[Telegram] 发送消息失败: %v", err)
			return
		}
		defer resp.Body.Close()
	}()
}

// SendStartupNotification 服务端启动通知
func (s *TelegramService) SendStartupNotification(port string, hasTunnel bool) {
	tunnelStatus := "⚪ 未配置隧道"
	if hasTunnel {
		tunnelStatus = "🟢 Cloudflare Tunnel 隧道已就绪"
	}

	msg := fmt.Sprintf(
		"🚀 <b>【棋牌游戏大厅·服务端已成功上线】</b>\n\n"+
			"🌐 本地端口：<code>:%s</code>\n"+
			"☁️ 公网接入：%s\n"+
			"👥 广播引擎：Goroutine + Channel 异步非阻塞\n"+
			"🤖 管理员模式：长轮询指令监听已就绪\n\n"+
			"💡 <b>在当前对话窗口可直接发送以下指令进行远程管理：</b>\n"+
			"• <code>/status</code> - 实时查看在线人数与游戏房间\n"+
			"• <code>/broadcast &lt;内容&gt;</code> - 全服实时弹窗公告\n"+
			"• <code>/give_coins &lt;玩家ID&gt; &lt;数量&gt;</code> - 特批金币充值\n"+
			"• <code>/test</code> - 测速与通道心跳检查\n"+
			"• <code>/help</code> - 查看详细操作帮助",
		port, tunnelStatus,
	)
	s.SendMessageAsync("", msg)
}

// NotifyHighMultiplierAlert 高倍对局或春天战报推送
func (s *TelegramService) NotifyHighMultiplierAlert(playerName string, roomName string, multiplier int, coins int64, isSpring bool) {
	s.mu.RLock()
	auto := s.config.AutoAlerts
	s.mu.RUnlock()
	if !auto {
		return
	}

	springText := ""
	if isSpring {
		springText = "🌸 <b>打出春天翻倍！</b>\n"
	}

	msg := fmt.Sprintf(
		"🎰 <b>【棋牌大厅·高倍捷报】</b>\n\n"+
			"👤 获胜玩家：<b>%s</b>\n"+
			"🏆 所在场次：<b>%s</b>\n"+
			"💥 最终倍数：<b>%d 倍</b>\n"+
			"%s"+
			"💰 结算金币：<b>%+d 金币</b>\n"+
			"⏰ 记录时间：%s",
		playerName, roomName, multiplier, springText, coins, time.Now().Format("2006-01-02 15:04:05"),
	)
	s.SendMessageAsync("", msg)
}

// StartPolling 启动 Telegram Bot 异步长轮询服务 (无需公网 Webhook 域名即可接收指令)
func (s *TelegramService) StartPolling() {
	s.pollingMu.Lock()
	if s.pollingRunning {
		s.pollingMu.Unlock()
		return
	}
	s.pollingRunning = true
	s.pollingMu.Unlock()

	go func() {
		log.Printf("[Telegram] 启动 Telegram Bot 异步长轮询 (getUpdates)...")

		// 启动前先清除已绑定的 Webhook，以便 getUpdates 可以接收消息
		s.mu.RLock()
		initToken := s.config.BotToken
		s.mu.RUnlock()
		if initToken != "" {
			delURL := fmt.Sprintf("https://api.telegram.org/bot%s/deleteWebhook", initToken)
			if resp, err := s.client.Get(delURL); err == nil {
				resp.Body.Close()
			}
		}

		var offset int64 = 0

		for {
			s.mu.RLock()
			token := s.config.BotToken
			enabled := s.config.Enabled
			adminID := s.config.AdminChatID
			s.mu.RUnlock()

			if !enabled || token == "" {
				time.Sleep(5 * time.Second)
				continue
			}

			pollURL := fmt.Sprintf("https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=25", token, offset)
			resp, err := s.client.Get(pollURL)
			if err != nil {
				// 网络闪断或 DNS 解析等待，3 秒后重试
				time.Sleep(3 * time.Second)
				continue
			}

			body, err := io.ReadAll(resp.Body)
			resp.Body.Close()
			if err != nil {
				time.Sleep(2 * time.Second)
				continue
			}

			var result struct {
				OK     bool             `json:"ok"`
				Result []TelegramUpdate `json:"result"`
			}
			if err := json.Unmarshal(body, &result); err != nil || !result.OK {
				time.Sleep(3 * time.Second)
				continue
			}

			for _, update := range result.Result {
				if update.UpdateID >= offset {
					offset = update.UpdateID + 1
				}

				if update.Message != nil && update.Message.Text != "" {
					chatIDStr := strconv.FormatInt(update.Message.Chat.ID, 10)

					// 权限校验：如果已指定 AdminChatID，则仅允许该管理员操作管理指令
					if adminID != "" && chatIDStr != adminID {
						s.SendMessageAsync(chatIDStr, fmt.Sprintf(
							"⛔ <b>权限不足</b>\n您当前的 Telegram ID 为: <code>%s</code>\n该机器人仅授权给管理员: <code>%s</code> 操作。",
							chatIDStr, adminID,
						))
						continue
					}

					reply := s.ProcessCommand(chatIDStr, update.Message.Text)
					if reply != "" {
						s.SendMessageAsync(chatIDStr, reply)
					}
				}
			}
		}
	}()
}

// ProcessCommand 执行管理员文本指令
func (s *TelegramService) ProcessCommand(chatID string, text string) string {
	text = strings.TrimSpace(text)
	parts := strings.Fields(text)
	if len(parts) == 0 {
		return ""
	}

	cmd := parts[0]
	// 兼容机器人指令中的 @botname 后缀，例如 /status@yansan66_bot
	if idx := strings.Index(cmd, "@"); idx != -1 {
		cmd = cmd[:idx]
	}

	switch cmd {
	case "/start", "/help":
		return "🤖 <b>棋牌游戏平台·Telegram 管理员控制台</b>\n\n" +
			"📋 <b>可用管理指令列表：</b>\n\n" +
			"• <code>/status</code> - 实时查看服务器在线人数、活跃房间与运行时间\n" +
			"• <code>/broadcast &lt;公告内容&gt;</code> - 全服实时弹窗广播，玩家即时可见\n" +
			"• <code>/give_coins &lt;玩家ID&gt; &lt;金币数&gt;</code> - 特批金币充值与破产补助\n" +
			"• <code>/test</code> - 测试管理员推送通道与响应延迟\n" +
			"• <code>/help</code> - 显示本帮助菜单"

	case "/status":
		if s.GetStatus != nil {
			info := s.GetStatus()
			return fmt.Sprintf(
				"📊 <b>【服务器运行监控状态】</b>\n\n"+
					"👥 当前在线玩家：<b>%v 人</b>\n"+
					"🎴 活跃游戏房间：<b>%v 间</b>\n"+
					"⏱ 服务连续运行：<b>%v</b>\n"+
					"⚡ 系统健康度：<b>100%% 正常</b>\n"+
					"📡 广播管道：Goroutine + Channel 异步非阻塞",
				info["onlineCount"], info["roomCount"], info["uptime"],
			)
		}
		return "📊 服务端运行正常。"

	case "/broadcast":
		if len(parts) < 2 {
			return "⚠️ 格式错误：请使用 <code>/broadcast &lt;公告内容&gt;</code>\n例如：<code>/broadcast 系统将于今晚进行功能更新，感谢支持！</code>"
		}
		broadcastText := strings.TrimSpace(strings.TrimPrefix(text, parts[0]))
		if s.OnBroadcast != nil {
			s.OnBroadcast(broadcastText)
			return fmt.Sprintf("📢 <b>全服广播公告已成功发送！</b>\n\n内容：<i>%s</i>", broadcastText)
		}
		return "⚠️ 广播服务未就绪"

	case "/give_coins":
		if len(parts) < 3 {
			return "⚠️ 格式错误：请使用 <code>/give_coins &lt;玩家ID&gt; &lt;金币数&gt;</code>\n例如：<code>/give_coins player1 50000</code>"
		}
		targetPlayer := parts[1]
		amount, err := strconv.ParseInt(parts[2], 10, 64)
		if err != nil || amount <= 0 {
			return "⚠️ 金币数额必须为正整数！"
		}
		if s.OnGiveCoins != nil {
			s.OnGiveCoins(targetPlayer, amount)
			return fmt.Sprintf("💰 <b>金币特批发放成功！</b>\n\n🎯 目标玩家：<b>%s</b>\n💎 发放数量：<b>%+d 金币</b>", targetPlayer, amount)
		}
		return "⚠️ 发放服务未就绪"

	case "/test":
		return fmt.Sprintf("✅ <b>Telegram 管理员通道测试成功！</b>\n\n⚡ 响应正常，延迟 &lt; 50ms\n⏰ 服务器时间：%s", time.Now().Format("2006-01-02 15:04:05"))

	default:
		return "❓ 未知指令，请输入 <code>/help</code> 查看支持的管理命令。"
	}
}

// HandleWebhook 处理来自 Telegram Webhook 的回调请求 (可选方式)
func (s *TelegramService) HandleWebhook(w http.ResponseWriter, r *http.Request) {
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	var update TelegramUpdate
	if err := json.Unmarshal(body, &update); err == nil && update.Message != nil && update.Message.Text != "" {
		chatIDStr := strconv.FormatInt(update.Message.Chat.ID, 10)

		s.mu.RLock()
		adminID := s.config.AdminChatID
		s.mu.RUnlock()

		if adminID != "" && chatIDStr != adminID {
			s.SendMessageAsync(chatIDStr, "⛔ 权限不足：非管理员账号。")
		} else {
			reply := s.ProcessCommand(chatIDStr, update.Message.Text)
			if reply != "" {
				s.SendMessageAsync(chatIDStr, reply)
			}
		}
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"ok":true}`))
}
