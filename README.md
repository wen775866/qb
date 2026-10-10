# 🎴 棋牌游戏大厅全栈单体融合项目 (Go + Vue 3)

一个采用 **Go 语言 + Vue 3** 打造的高性能、单体融合架构的在线棋牌游戏平台。
前端静态资源通过 Go `embed.FS` 编译打包进单个二进制执行文件，内置 **WebSocket 异步非阻塞广播中心 (Goroutine + Channel)**、**Telegram 管理员机器人 (长轮询)** 以及 **Cloudflare Tunnel 公网发布隧道**，可完美运行于云服务器及 **手机 Termux** 环境中。

---

## 📁 目录结构

```text
├── main.go                    # 单体融合核心：HTTP/WS + 嵌入式静态资源托管 + 环境变量加载
├── go.mod                     # Go 依赖配置
├── .env.example               # 环境变量配置模板
├── .gitignore                 # Git 忽略配置 (自动排除二进制与敏感配置)
│
├── termux_setup.sh            # Termux 一键环境部署脚本 (DNS修复 + Go + cloudflared)
├── termux_start.sh            # Termux 一键启动脚本 (服务 + 隧道后台守护)
├── termux_stop.sh             # Termux 一键停止脚本
├── termux_status.sh           # Termux 运行状态与日志监控
│
├── game/                      # 核心后端游戏逻辑包 (各 AI 独立模块)
│   ├── engine.go              # 统一接口定义 (GameEngine / Room / Player / Action)
│   ├── doudizhu/              # 【AI 1 已完成】斗地主全套规则、叫抢地主、加倍、炸弹春天、智能AI
│   ├── majiang/               # 【AI 2 预留接口】国粹麻将逻辑
│   ├── paodekuai/             # 【AI 3 预留接口】跑得快逻辑
│   ├── shisanshui/            # 【AI 4 预留接口】十三水逻辑
│   └── chudadi/               # 【AI 5 预留接口】锄大地逻辑
│
├── hub/                       # WebSocket 异步非阻塞广播中心 (Goroutine + Channel)
├── chat/                      # 全服与房间聊天管理中心
├── telegram/                  # Telegram 管理员机器人服务 (长轮询 / 指令响应 / 战报推送)
│
└── frontend/                  # 前端 Vue 3 SPA 项目 (已打包并嵌入至 dist/)
    ├── src/
    │   ├── App.vue            # 前端总入口
    │   ├── components/
    │   │   ├── Lobby.vue      # 游戏大厅界面 (游戏选择、全服聊天、公告、快捷加金币)
    │   │   └── TelegramAdminModal.vue # Telegram 悬浮管理员面板
    │   ├── services/
    │   │   └── websocket.js   # 统一 WebSocket 通信服务
    │   └── games/
    │       ├── Doudizhu.vue   # 【AI 1 已完成】斗地主精致桌面 UI、出牌动画、音效提示
    │       ├── Majiang.vue    # 【AI 2 预留 UI】
    │       ├── Paodekuai.vue  # 【AI 3 预留 UI】
    │       ├── Shisanshui.vue # 【AI 4 预留 UI】
    │       └── Chudadi.vue    # 【AI 5 预留 UI】
    └── dist/                  # 已构建的前端生产包 (Go main.go 自动内嵌)
```

---

## 🚀 一、保存到 GitHub 仓库

在 AI Studio 或本地终端中执行以下命令，将所有源码推送到您的 GitHub 仓库：

```bash
# 1. 初始化 Git 仓库 (如尚未初始化)
git init
git add .
git commit -m "feat: 单体融合棋牌大厅 (Go+Vue3) 斗地主完整对局+Telegram长轮询+Termux一键部署"

# 2. 关联您的 GitHub 远程仓库 (将 YOUR_USER 和 YOUR_REPO 替换为您自己的仓库信息)
git remote add origin https://github.com/YOUR_USER/YOUR_REPO.git
git branch -M main

# 3. 推送源码至 GitHub
git push -u origin main
```

---

## 📱 二、Termux 手机端拉取与部署步骤

### 1. 准备 Termux 环境并拉取代码
打开手机 Termux，执行以下命令：
```bash
# 更新环境并安装基础工具
pkg update -y
pkg install -y git golang proot

# 拉取您的 GitHub 仓库 (以进入 Termux 根目录创建项目目录为例)
cd ~
git clone https://github.com/YOUR_USER/YOUR_REPO.git game
cd game
```

### 2. 配置环境变量 (.env)
服务端支持从 **Termux 根目录 `~/.env`**（即项目的上级目录 `../.env`）或 **项目根目录 `./.env`** 自动读取配置。

您可以直接在 Termux 根目录创建 `~/.env`：
```bash
cat << 'EOF' > ~/.env
# 游戏服务运行端口
APP_PORT=3000

# Telegram 管理员机器人配置
TELEGRAM_BOT_TOKEN=8981031993:AAFamkfF2IQlYVZCxMgYWOdJUtCj0CkNpsg
TELEGRAM_ADMIN_CHAT_ID=7318296570

# Cloudflare Tunnel 隧道 Token (将游戏直接发布到公网)
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiOWFiNmE4YjQ0NGQ3MDA2OWNlMGIyM2RlMzVmNzE2ZDEiLCJ0IjoiMDgyNDIyZGUtYjBiZC00NTE5LWFhOTYtNjBiZDVhYjc2NWNmIiwicyI6Ik5XWmtNalU0WVRNdFlqa3lZeTAwTVRBM0xXRmtZbU10WVdZeE9XRXlNREU0TkRRMSJ9
EOF
```

### 3. 一键配置环境与依赖
在 `game` 目录下运行安装脚本：
```bash
bash termux_setup.sh
```
该脚本将全自动完成：
- ✅ 配置 DNS 解析 (`$PREFIX/etc/resolv.conf`)，彻底解决 Go 与 Cloudflared 在 Android 上的域名解析报错；
- ✅ 安装 Go 编译器与 `cloudflared` 隧道工具；
- ✅ 编译单体全栈可执行文件 `server`。

### 4. 一键启动服务与公网隧道
```bash
bash termux_start.sh
```
服务将在后台运行：
- **局域网/本机访问**：`http://127.0.0.1:3000`
- **公网访问**：Cloudflare Tunnel 将自动接管并将流量透传至本地 3000 端口，可通过您在 Cloudflare 绑定的公网域名直接秒开游戏！

### 5. 常用维护指令
- **查看状态与日志**：`bash termux_status.sh`
- **查看 Go 服务日志**：`tail -f server.log`
- **查看隧道日志**：`tail -f tunnel.log`
- **停止所有服务**：`bash termux_stop.sh`

---

## 🤖 三、Telegram 远程管理员控制台

服务端集成了基于 **长轮询 (Long Polling)** 的 Telegram Bot 管理员服务，无需公网 Webhook 域名即可实时接收与响应管理员指令。

当前绑定的 Telegram 机器人：`@yansan66_bot`（管理员 ID：`7318296570`）。

### 支持的管理员指令：
- `/status`：查看服务器健康状态、当前在线玩家数、活跃房间及连续运行时间；
- `/broadcast <内容>`：向所有在线玩家发送全服实时弹窗公告；
- `/give_coins <玩家ID> <数量>`：为指定玩家特批充值/破产救济金币；
- `/test`：测试通讯响应延迟；
- `/help`：查看指令帮助。

> 此外，当玩家在对局中打出高倍数胜局（如炸弹连续翻倍）或**打出春天**时，系统会自动向管理员推送即时高倍战报！

---

## 🧩 四、多 AI 协作扩展规范

本项目为后续多个 AI 协同开发预留了标准化接口：
1. **新增游戏**：在 `game/` 下新建子目录（如 `game/majiang/`），实现 `game.GameEngine` 接口中的 `CreateRoom`、`GetRoom`、`ProcessAction`、`DestroyRoom` 方法，并在 `init()` 中调用 `game.RegisterEngine(gameType, engine)`。
2. **前端接入**：在 `frontend/src/games/` 下创建对应组件（如 `Majiang.vue`），统一通过 `websocket.js` 进行通信。
