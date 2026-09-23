# -*- coding: utf-8 -*-
"""启动台（合并窗体）功能验证：探测链路 + 界面状态 dump + 外部运行识别 + 进程链路"""
import io
import os
import subprocess
import sys
import time

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
EXE = os.path.join(HERE, "bin", "启动台.exe")
OUT = os.path.join(HERE, "_selftest")
if not os.path.isdir(OUT):
    os.makedirs(OUT)

BACKEND = os.path.join(ROOT, "backend", "device-note")
FRONTEND = os.path.join(ROOT, "ft-checnote")

NL = chr(10)

fails = []


def check(name, cond, detail=""):
    print(("  ok  " if cond else "  FAIL") + "  " + name + ("   " + detail if detail else ""))
    if not cond:
        fails.append(name)


def dump(name, args, wait=25):
    p = os.path.join(OUT, name)
    subprocess.run([EXE] + args + [p], capture_output=True, timeout=wait + 40)
    time.sleep(0.3)
    if not os.path.exists(p):
        return {}
    d = {}
    for line in io.open(p, encoding="utf-8", errors="replace").read().splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d


print("=== 1) --detect-all 双端探测 ===")
p = os.path.join(OUT, "combo_detect.txt")
subprocess.run([EXE, "--detect-all", BACKEND, FRONTEND, p], capture_output=True, timeout=120)
t = io.open(p, encoding="utf-8", errors="replace").read()
for line in t.splitlines():
    if line.startswith(("[", "ok=", "port=", "profiles=", "launchModule=", "default=", "scripts=", "pm=", "command=")):
        print("   " + line)
check("后端探测 ok", ("[backend]" + NL + "ok=True") in t)
check("后端 profile 含 dev/test/deploy", "profiles=dev,test,deploy" in t)
check("后端命令含 spring-boot:run 且指定 run.profiles", "spring-boot:run" in t and "spring-boot.run.profiles=dev" in t)
check("后端命令含 fork JVM 编码参数", "spring-boot.run.jvmArguments" in t)
check("前端探测 ok", ("[frontend]" + NL + "ok=True") in t)
check("前端默认脚本 local dev", "default=local dev" in t)
check("前端命令为 <npm.cmd> run <脚本>", 'run "local dev"' in t or 'run local dev' in t)

print()
print("=== 2) --uidump 界面状态（两个工程都配置） ===")
d = dump("combo_ui.txt", ["--uidump", BACKEND, FRONTEND])
for k in ("bOk", "bPort", "bProfile", "bState", "bStopEnabled", "fOk", "fPort", "fScript", "fState", "fStopEnabled", "bExtPid", "fExtPid"):
    print("   %-14s %s" % (k, d.get(k, "<missing>")))
check("后端状态灯含端口与 profile", "8092" in d.get("bState", "") and "(dev)" in d.get("bState", ""))
check("后端已选 dev profile", d.get("bProfile") == "dev")
check("前端状态灯含端口", "4177" in d.get("fState", ""))
check("前端脚本 local dev", d.get("fScript") == "local dev")
check("后端未运行 → 停止禁用、启动可用", d.get("bStopEnabled") == "False" and d.get("bRunEnabled") == "True")

print()
print("=== 3) 端口上的外部服务应被识别，且不被误杀 ===")
r = subprocess.run(["netstat", "-ano", "-p", "tcp"], capture_output=True)
ns = r.stdout.decode("gbk", "replace")
listen = {}
for line in ns.splitlines():
    parts = line.split()
    if len(parts) >= 5 and parts[0].upper() == "TCP" and parts[3].upper() == "LISTENING":
        try:
            listen.setdefault(int(parts[1].rsplit(":", 1)[1]), []).append(parts[4])
        except Exception:
            pass
for port in (8092, 4177):
    print("   端口 %d 监听者: %s" % (port, listen.get(port, "无")))
if listen.get(4177):
    check("4177 的外部服务被识别", str(d.get("fExtPid", "0")) != "0", "fExtPid=" + d.get("fExtPid", "?"))
    check("外部服务使前端「停止」可用（可一键接管）", d.get("fStopEnabled") == "True")
    check("外部服务仍存活（未被误杀）", str(listen.get(4177)[0]) == d.get("fExtPid"))
else:
    print("   4177 无监听，跳过外部识别断言")
if listen.get(8092):
    check("8092 的外部服务被识别", str(d.get("bExtPid", "0")) != "0", "bExtPid=" + d.get("bExtPid", "?"))
else:
    check("8092 无监听 → 后端显示未运行", "未运行" in d.get("bState", ""))

print()
print("=== 4) 进程链路（共用 RunnerProbe：真跑一条命令并清理进程树） ===")
p4 = os.path.join(OUT, "combo_run.txt")
subprocess.run([EXE, "--run", FRONTEND, "node -v", p4, "10"], capture_output=True, timeout=90)
t4 = io.open(p4, encoding="utf-8", errors="replace").read()
print("   " + " | ".join([l for l in t4.splitlines() if l.startswith(("exitCode", "finishedBeforeTimeout"))]))
print("   output: " + " ".join(t4.split("---- output ----")[-1].split()))
check("命令执行完成且退出码 0", "finishedBeforeTimeout=True" in t4 and "exitCode=0" in t4)
check("node 输出可读（v 开头版本号）", "v2" in t4)

print()
print("=== 5) 端口探测子命令（--probe） ===")
p5 = os.path.join(OUT, "combo_probe.txt")
subprocess.run([EXE, "--probe", "4177", p5], capture_output=True, timeout=60)
t5 = io.open(p5, encoding="utf-8", errors="replace").read()
print("   " + t5.replace("\r\n", " / ").strip()[:200])
check("--probe 能列出端口占用者", ("count=1" in t5) or ("count=0" in t5))

print()
print("=== 6) 配置持久化 ===")
cfg = os.path.join(os.environ["APPDATA"], "DevLauncher", "combined.ini")
if os.path.exists(cfg):
    txt = io.open(cfg, encoding="utf-8", errors="replace").read()
    print("   " + txt.replace("\r\n", " | ")[:300])
    check("配置里有后端目录", "bdir=" in txt and "device-note" in txt)
    check("配置里有前端目录", "fdir=" in txt and "ft-checnote" in txt)
    check("配置记住 profile=dev", "bprofile=dev" in txt)
else:
    check("生成 combined.ini", False, "未找到 " + cfg)

print()
if fails:
    print("FAILED %d:" % len(fails))
    for f in fails:
        print("  - " + f)
else:
    print("ALL PASS")
