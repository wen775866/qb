#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux 一键启动脚本 (termux_start.sh)
# 作用：自动读取上级目录/当前目录 .env、启动 Go 服务与 Cloudflare Tunnel
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PARENT_DIR="$(dirname "$PROJECT_DIR")"
cd "$PROJECT_DIR"

echo "============================================================"
echo "🎮 正在启动单体融合棋牌游戏平台 (Go + Vue3 + Cloudflare)..."
echo "============================================================"

# 1. 自动配置 DNS (解决 Go 语言与 Cloudflared 域名解析报错)
mkdir -p "$PREFIX/etc"
cat << 'EOF' > "$PREFIX/etc/resolv.conf"
nameserver 223.5.5.5
nameserver 114.114.114.114
nameserver 1.1.1.1
EOF

# 2. 导出运行环境变量
export SSL_CERT_FILE="$PREFIX/etc/tls/cert.pem"
export GODEBUG=netdns=go

# 3. 加载 .env 环境变量 (先检查上级目录 Termux 根目录，后当前目录)
if [ -f "$PARENT_DIR/.env" ]; then
    echo "📄 从 Termux 根目录读取配置: $PARENT_DIR/.env"
    set -a
    source "$PARENT_DIR/.env"
    set +a
elif [ -f "$PROJECT_DIR/.env" ]; then
    echo "📄 从当前目录读取配置: $PROJECT_DIR/.env"
    set -a
    source "$PROJECT_DIR/.env"
    set +a
else
    echo "⚠️ 未找到 .env 配置文件，将使用默认配置运行"
fi

PORT="${APP_PORT:-3000}"

# 4. 确保 Go 服务二进制存在
if [ ! -f "$PROJECT_DIR/server" ]; then
    echo "🔨 正在编译 Go 服务..."
    go build -o "$PROJECT_DIR/server" "$PROJECT_DIR/main.go"
    chmod +x "$PROJECT_DIR/server"
fi

# 5. 启动 Go 全栈单体服务
if pgrep -f "$PROJECT_DIR/server" >/dev/null; then
    echo "🟢 Go 服务已在运行中 (PID: $(pgrep -f "$PROJECT_DIR/server"))"
else
    echo "🚀 启动 Go 服务 (端口 :$PORT)..."
    nohup "$PROJECT_DIR/server" > "$PROJECT_DIR/server.log" 2>&1 &
    sleep 2
    if pgrep -f "$PROJECT_DIR/server" >/dev/null; then
        echo "✅ Go 服务启动成功！PID: $(pgrep -f "$PROJECT_DIR/server")"
    else
        echo "❌ Go 服务启动失败，请检查 server.log:"
        cat "$PROJECT_DIR/server.log"
        exit 1
    fi
fi

# 6. 自动启动 Cloudflared 隧道
if [ -n "$CLOUDFLARE_TUNNEL_TOKEN" ]; then
    if pgrep -f "cloudflared tunnel" >/dev/null; then
        echo "🟢 Cloudflare Tunnel 隧道已在运行中"
    else
        echo "☁️ 正在启动 Cloudflare 隧道发布服务到公网..."
        if command -v termux-chroot &>/dev/null; then
            nohup termux-chroot cloudflared tunnel --edge-ip-version 4 --protocol http2 run --token "$CLOUDFLARE_TUNNEL_TOKEN" > "$PROJECT_DIR/tunnel.log" 2>&1 &
        else
            nohup cloudflared tunnel --edge-ip-version 4 --protocol http2 run --token "$CLOUDFLARE_TUNNEL_TOKEN" > "$PROJECT_DIR/tunnel.log" 2>&1 &
        fi
        sleep 3
        echo "✅ Cloudflare 隧道已在后台启动！"
    fi
else
    echo "⚪ 未设置 CLOUDFLARE_TUNNEL_TOKEN，跳过启动公网隧道"
fi

echo "============================================================"
echo "🎉 平台全部服务启动就绪！"
echo "📱 本地访问地址: http://127.0.0.1:$PORT"
echo "🤖 Telegram 机器人: @yansan66_bot (管理员已接入长轮询监听)"
echo "📋 查看实时运行日志: tail -f $PROJECT_DIR/server.log"
echo "📋 查看隧道日志: tail -f $PROJECT_DIR/tunnel.log"
echo "🛑 停止所有服务: bash $PROJECT_DIR/termux_stop.sh"
echo "============================================================"
