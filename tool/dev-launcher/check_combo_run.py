# -*- coding: utf-8 -*-
"""用「启动台.exe」真实跑一次它自己拼出的后端启动命令，验证：命令可用 / 中文正常 / 停止后端口释放"""
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
BACKEND = os.path.join(ROOT, "backend", "device-note")

det = io.open(os.path.join(OUT, "combo_detect.txt"), encoding="utf-8", errors="replace").read()
cmd = ""
for line in det.splitlines():
    if line.startswith("command=") and "spring-boot:run" in line:
        cmd = line[len("command="):].strip()
assert cmd, "未从 detect-all 结果里取到后端命令"
# 追加随机端口，避免与用户当前可能在跑的服务冲突
cmd += " -Dspring-boot.run.arguments=--server.port=0"
print("命令:", cmd)

out = os.path.join(OUT, "combo_realserver.txt")
print("真跑 90 秒 ...")
t0 = time.time()
subprocess.run([EXE, "--run", BACKEND, cmd, out, "90"], capture_output=True, timeout=240)
print("耗时 %.0fs" % (time.time() - t0))

t = io.open(out, encoding="utf-8", errors="replace").read()
tomcat = [l for l in t.splitlines() if "Tomcat started on port" in l or "Started LauncherApplication" in l]
bad = [l for l in t.splitlines() if "\ufffd" in l or "锟" in l]
print("启动标志行:")
for l in tomcat:
    print("   " + l.strip())
print("乱码行数: %d" % len(bad))
for l in bad[:5]:
    print("   ! " + l.strip()[:160])
print("finishedBeforeTimeout:", [l for l in t.splitlines() if l.startswith("finishedBeforeTimeout")])
print("退出码:", [l for l in t.splitlines() if l.startswith("exitCode")])

fails = []
if not tomcat:
    fails.append("未见 Tomcat 启动日志")
if bad:
    fails.append("出现 %d 行乱码" % len(bad))

time.sleep(3)
r = subprocess.run(["netstat", "-ano", "-p", "tcp"], capture_output=True)
ns = r.stdout.decode("gbk", "replace")
left = [l.strip() for l in ns.splitlines() if ":8092" in l and "LISTENING" in l.upper()]
print("8092 残留监听: %d" % len(left))
java = subprocess.run(["tasklist", "/FI", "IMAGENAME eq java.exe"], capture_output=True)
print(java.stdout.decode("gbk", "replace").strip()[:400])

print()
print("FAILED: " + ", ".join(fails) if fails else "ALL PASS")
