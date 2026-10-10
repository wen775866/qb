#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux 一键停止脚本 (termux_stop.sh)
# ==============================================================================

echo "🛑 正在停止游戏服务与 Cloudflare 隧道..."

# 停止 Go 游戏服务
if pgrep -f "./server|/server" >/dev/null; then
    pkill -f "./server|/server" || true
    echo "✅ 已停止 Go 游戏服务"
else
    echo "⚪ Go 游戏服务未在运行"
fi

# 停止 Cloudflare 隧道
if pgrep -f cloudflared >/dev/null; then
    pkill -f cloudflared || true
    echo "✅ 已停止 Cloudflare 隧道"
else
    echo "⚪ Cloudflare 隧道未在运行"
fi

echo "============================================================"
echo "所有进程已停止。"
echo "============================================================"
