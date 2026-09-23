# -*- coding: utf-8 -*-
"""验证「已在运行的进程也能停止」：
   1) 真实场景只读探测（用户当前的 vite 4177）
   2) 起一个真实的 node 服务当「别处启动的服务」→ UI 必须识别并让停止按钮可用
   3) 停止（与「停止」按钮同一条 Sys.KillPid 路径）→ 端口释放、进程消失
   4) 停止后 UI 回到「未运行」
   5) 后端窗口同样验证
"""
import io
import os
import socket
import subprocess
import sys
import time

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, "bin")
OUT = os.path.join(HERE, "_selftest")
BOOT = os.path.join(BIN, "启动后端.exe")
VITE = os.path.join(BIN, "启动前端.exe")
NODE = r"E:\NVM\nodejs\node.exe"
FE_DIR = os.path.abspath(os.path.join(HERE, "..", "..", "ft-checnote"))
BE_DIR = os.path.abspath(os.path.join(HERE, "..", "..", "backend", "device-note"))

if not os.path.isdir(OUT):
    os.makedirs(OUT)

fails = []


def check(name, cond, extra=""):
    print("%-46s %s %s" % (name, "PASS" if cond else "FAIL", extra))
    if not cond:
        fails.append(name)


def rd(name):
    return io.open(os.path.join(OUT, name), encoding="utf-8", errors="replace").read()


def rd_kv(name):
    d = {}
    for line in rd(name).splitlines():
        i = line.find("=")
        if i > 0:
            d[line[:i]] = line[i + 1:]
    return d


def probe(exe, port):
    fn = "p_%d.txt" % port
    subprocess.run([exe, "--probe", str(port), os.path.join(OUT, fn)], capture_output=True, timeout=60)
    return rd_kv(fn)


def stop_port(exe, port):
    fn = "s_%d.txt" % port
    subprocess.run([exe, "--stop-port", str(port), os.path.join(OUT, fn)], capture_output=True, timeout=60)
    return rd_kv(fn), rd(fn)


def uidump(exe, d, arg2, port, out):
    subprocess.run([exe, "--uidump", d, arg2, str(port), os.path.join(OUT, out)],
                   capture_output=True, timeout=120)
    return rd_kv(out)


def free_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    p = s.getsockname()[1]
    s.close()
    return p


def start_fake(dir_, port, tag):
    """在工程目录里起一个真的 node HTTP 服务，命令行会带上工程路径"""
    js = os.path.join(dir_, "_probe_srv_%s.cjs" % tag)
    io.open(js, "w", encoding="utf-8").write(
        "require('http').createServer(function(q,s){s.end('ok')}).listen(%d,'127.0.0.1');"
        "setTimeout(function(){},1);\n" % port)
    p = subprocess.Popen([NODE, js], cwd=dir_, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for _ in range(30):
        time.sleep(0.5)
        if probe(VITE, port).get("count") == "1":
            break
    return p, js


# ============ 1) 真实场景（只读）：用户当前的 vite 4177 ============
kv = probe(VITE, 4177)
print("--- 真实端口 4177 ---")
print(rd("p_4177.txt"))
check("真实 4177：探测到监听进程", kv.get("count") == "1", kv.get("count", ""))
check("真实 4177：进程名 node", "node" in kv.get("owner", ""), kv.get("owner", ""))
kv8092 = probe(BOOT, 8092)
print("--- 真实端口 8092（IDEA 里跑着就有）---")
print(rd("p_8092.txt"))

# ============ 2) 前端窗口：识别「外部运行中」并启用停止按钮 ============
port = free_port()
srv, js = start_fake(FE_DIR, port, "fe")
print("### 外部服务 pid=%d port=%d（工程目录内，命令行带工程路径）" % (srv.pid, port))
kv = probe(VITE, port)
check("probe 找到监听进程", kv.get("count") == "1", kv.get("count", ""))
check("probe 进程名是 node", "node" in kv.get("owner", ""), kv.get("owner", ""))

ui = uidump(VITE, FE_DIR, "", port, "ui_front.txt")
print("--- ui_front ---")
print(rd("ui_front.txt"))
check("前端 UI：状态=运行中·非本窗口", "运行中" in ui.get("runState", "") and "非本窗口" in ui.get("runState", ""),
      ui.get("runState", ""))
check("前端 UI：按钮=停止(外部)", "停止(外部)" in ui.get("stopText", ""), ui.get("stopText", ""))
check("前端 UI：停止按钮可用", ui.get("stopEnabled") == "True", ui.get("stopEnabled", ""))
check("前端 UI：外部 PID 正确", ui.get("extPid") == str(srv.pid), ui.get("extPid", "") + " vs " + str(srv.pid))
check("前端 UI：命令行判为本工程", ui.get("extInDir") == "True", ui.get("extInDir", ""))

# ============ 3) 停止外部进程（与「停止」按钮同一条 Sys.KillPid 路径） ============
kv, raw = stop_port(VITE, port)
print("--- stop ---")
print(raw)
check("停止后端口释放", kv.get("left") == "0", kv.get("left", ""))
time.sleep(0.8)
check("node 进程确实已退出", srv.poll() is not None, str(srv.poll()))

# ============ 4) 停止后窗口回到「未运行」 ============
ui2 = uidump(VITE, FE_DIR, "", port, "ui_front2.txt")
print("--- ui_front2 ---")
print(rd("ui_front2.txt"))
check("前端 UI：停止后=未运行", ui2.get("runState", "") == "● 未运行", ui2.get("runState", ""))
check("前端 UI：停止后按钮禁用", ui2.get("stopEnabled") == "False", ui2.get("stopEnabled", ""))

# ============ 5) 真实 vite（4177）：窗口应显示运行中，且不误杀 ============
ui3 = uidump(VITE, FE_DIR, "", 4177, "ui_real.txt")
print("--- ui_real (4177 用户的 vite) ---")
print(rd("ui_real.txt"))
check("真实 4177：窗口显示运行中", "运行中" in ui3.get("runState", ""), ui3.get("runState", ""))
check("真实 4177：停止按钮可用", ui3.get("stopEnabled") == "True", ui3.get("stopEnabled", ""))
check("真实 4177：未被误杀", probe(VITE, 4177).get("count") == "1", "探测仍在监听")

# ============ 6) 后端窗口同样识别 ============
port2 = free_port()
srv2, js2 = start_fake(BE_DIR, port2, "be")
ub = uidump(BOOT, BE_DIR, "dev", port2, "ui_boot.txt")
print("--- ui_boot ---")
print(rd("ui_boot.txt"))
check("后端 UI：状态=运行中", "运行中" in ub.get("runState", ""), ub.get("runState", ""))
check("后端 UI：停止(外部) 可用", "停止(外部)" in ub.get("stopText", "") and ub.get("stopEnabled") == "True",
      ub.get("stopText", "") + "/" + ub.get("stopEnabled", ""))
check("后端 UI：profile 生效", ub.get("profile") == "dev", ub.get("profile", ""))
check("后端 UI：端口=覆盖值", ub.get("port") == str(port2), ub.get("port", ""))
kvb, rawb = stop_port(BOOT, port2)
print("--- stop(boot) ---")
print(rawb)
check("后端：外部进程已停止", srv2.poll() is not None, str(srv2.poll()))

# ============ 7) 非本工程的占用者：应显示「非本工程」，但仍能停止 ============
port3 = free_port()
code = "require('http').createServer(function(q,s){s.end('ok')}).listen(%d,'127.0.0.1')" % port3
srv3 = subprocess.Popen([NODE, "-e", code], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
for _ in range(20):
    time.sleep(0.5)
    if probe(VITE, port3).get("count") == "1":
        break
ui4 = uidump(VITE, FE_DIR, "", port3, "ui_other.txt")
print("--- ui_other（非本工程占用者）---")
print(rd("ui_other.txt"))
check("非本工程：状态标注正确", "运行中" in ui4.get("runState", "") and "非本工程" in ui4.get("runState", ""),
      ui4.get("runState", ""))
check("非本工程：extInDir=False", ui4.get("extInDir") == "False", ui4.get("extInDir", ""))
check("非本工程：停止按钮仍可用", ui4.get("stopEnabled") == "True", ui4.get("stopEnabled", ""))
stop_port(VITE, port3)
time.sleep(0.8)
check("非本工程：仍能被停止", srv3.poll() is not None, str(srv3.poll()))

# 清理
for p, f in ((srv, js), (srv2, js2)):
    try:
        if p.poll() is None:
            p.kill()
    except Exception:
        pass
    try:
        os.remove(f)
    except Exception:
        pass
for p in (port, port2, port3):
    subprocess.run([VITE, "--stop-port", str(p), os.path.join(OUT, "cleanup.txt")], capture_output=True)

print("\n==== 结果：%d 项失败 ====" % len(fails))
for f in fails:
    print("  FAIL", f)
sys.exit(1 if fails else 0)
