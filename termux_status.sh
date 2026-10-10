#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux 服务运行状态检查脚本 (termux_status.sh)
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "============================================================"
echo "📊 棋牌游戏大厅全栈服务状态："
echo "============================================================"

# 检查 Go 服务
if pgrep -f "$PROJECT_DIR/server" >/dev/null; then
    PID=$(pgrep -f "$PROJECT_DIR/server" | head -n 1)
    echo "🟢 Go 游戏服务: 正常运行中 (PID: $PID)"
else
    echo "🔴 Go 游戏服务: 未运行"
fi

# 检查 Cloudflared
if pgrep -f "cloudflared tunnel" >/dev/null; then
    CF_PID=$(pgrep -f "cloudflared tunnel" | head -n 1)
    echo "🟢 Cloudflare 隧道: 正常运行中 (PID: $CF_PID)"
else
    echo "🔴 Cloudflare 隧道: 未运行"
fi

echo "------------------------------------------------------------"
echo "📜 最近 15 行 Go 服务日志 (server.log):"
if [ -f "$PROJECT_DIR/server.log" ]; then
    tail -n 15 "$PROJECT_DIR/server.log"
else
    echo "（暂无日志文件）"
fi

echo "------------------------------------------------------------"
echo "📜 最近 15 行隧道日志 (tunnel.log):"
if [ -f "$PROJECT_DIR/tunnel.log" ]; then
    tail -n 15 "$PROJECT_DIR/tunnel.log"
else
    echo "（暂无日志文件）"
fi
echo "============================================================"
