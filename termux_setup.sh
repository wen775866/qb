#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux 一键环境部署与依赖安装脚本 (termux_setup.sh)
# 作用：配置 DNS、安装 Go 语言环境、安装 cloudflared、配置 Termux 根目录 .env
# ==============================================================================

set -e

echo "============================================================"
echo "🚀 开始配置 Termux 棋牌游戏大厅全栈运行环境..."
echo "============================================================"

# 1. 自动配置 DNS (解决 Go 语言与 Cloudflared 域名解析报错)
echo "📡 [1/5] 正在配置系统 DNS 解析..."
mkdir -p "$PREFIX/etc"
cat << 'EOF' > "$PREFIX/etc/resolv.conf"
nameserver 223.5.5.5
nameserver 114.114.114.114
nameserver 1.1.1.1
nameserver 8.8.8.8
EOF
echo "✅ DNS 解析已成功写入 $PREFIX/etc/resolv.conf"

# 2. 安装 Termux 必需软件包
echo "📦 [2/5] 正在更新并安装基础依赖包 (git, golang, proot, ca-certificates)..."
pkg update -y
pkg install -y git golang proot ca-certificates curl tar

# 3. 安装或检查 cloudflared
echo "☁️ [3/5] 正在配置 cloudflared 隧道客户端..."
if ! command -v cloudflared &>/dev/null; then
    echo "正在尝试通过 pkg 安装 cloudflared..."
    if ! pkg install -y cloudflared; then
        echo "pkg 未包含 cloudflared，正在从官方 GitHub 下载适配 Android 架构的二进制文件..."
        ARCH=$(uname -m)
        CF_ARCH="arm64"
        case "$ARCH" in
            aarch64|arm64) CF_ARCH="arm64" ;;
            armv7l|armhf)   CF_ARCH="arm" ;;
            x86_64|amd64)  CF_ARCH="amd64" ;;
            *)             CF_ARCH="arm64" ;;
        esac
        
        CF_URL="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${CF_ARCH}"
        echo "正在下载: ${CF_URL}"
        curl -L -o "$PREFIX/bin/cloudflared" "$CF_URL"
        chmod +x "$PREFIX/bin/cloudflared"
    fi
fi

if command -v cloudflared &>/dev/null; then
    echo "✅ cloudflared 安装成功: $(cloudflared --version)"
else
    echo "⚠️ 警告: 未能自动安装 cloudflared，稍后可手动放置至 \$PREFIX/bin"
fi

# 4. 自动检查或创建上级目录 (Termux 根目录) 的 .env 配置
echo "⚙️ [4/5] 正在检查环境配置文件..."
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PARENT_DIR="$(dirname "$PROJECT_DIR")"

PARENT_ENV="$PARENT_DIR/.env"
LOCAL_ENV="$PROJECT_DIR/.env"

if [ ! -f "$PARENT_ENV" ] && [ ! -f "$LOCAL_ENV" ]; then
    echo "正在在 Termux 根目录 ($PARENT_DIR) 创建默认 .env 测试配置..."
    cat << 'EOF' > "$PARENT_ENV"
# 游戏服务运行端口
APP_PORT=3000

# Telegram 管理员机器人配置
TELEGRAM_BOT_TOKEN=8981031993:AAFamkfF2IQlYVZCxMgYWOdJUtCj0CkNpsg
TELEGRAM_ADMIN_CHAT_ID=7318296570

# Cloudflare Tunnel 隧道 Token (将本地 3000 端口发布到公网)
CLOUDFLARE_TUNNEL_TOKEN=eyJhIjoiOWFiNmE4YjQ0NGQ3MDA2OWNlMGIyM2RlMzVmNzE2ZDEiLCJ0IjoiMDgyNDIyZGUtYjBiZC00NTE5LWFhOTYtNjBiZDVhYjc2NWNmIiwicyI6Ik5XWmtNalU0WVRNdFlqa3lZeTAwTVRBM0xXRmtZbU10WVdZeE9XRXlNREU0TkRRMSJ9
EOF
    echo "✅ 已在 Termux 根目录创建: $PARENT_ENV"
else
    echo "✅ 已检测到现有的 .env 配置文件"
fi

# 5. 编译 Go 服务端 (内置 Vue3 SPA 静态资源)
echo "🔨 [5/5] 正在编译 Go 单体全栈可执行文件..."
cd "$PROJECT_DIR"
export GODEBUG=netdns=go
go build -o server main.go
chmod +x server
echo "✅ Go 服务端编译成功: $PROJECT_DIR/server"

echo ""
echo "============================================================"
echo "🎉 Termux 环境部署完成！"
echo "👉 启动服务请运行: bash termux_start.sh"
echo "👉 停止服务请运行: bash termux_stop.sh"
echo "👉 查看状态请运行: bash termux_status.sh"
echo "============================================================"
