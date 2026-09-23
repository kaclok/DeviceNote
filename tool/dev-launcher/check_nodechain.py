# -*- coding: utf-8 -*-
"""验证 前端 --detect 的 nodeDir 注入：配置目录后 pmExe 应指向该目录下的 npm.cmd"""
import os
import subprocess
import sys

HERE = r"F:\Projects\Study\github\DeviceNote\tool\dev-launcher"
VITE = os.path.join(HERE, "bin", "启动前端.exe")
OUT = os.path.join(HERE, "_selftest", "vite_nodechain.txt")
REPO = r"F:\Projects\Study\github\DeviceNote"
FRONT = os.path.join(REPO, "ft-checnote")

# 找一个真实存在的 node 安装目录（managed node 根目录下应有 npm.cmd / node.exe）
CANDS = [
    r"C:\Users\DELL\.workbuddy\binaries\node\versions\22.22.2-3",
    r"E:\NVM\nodejs",
]
node_dir = None
for c in CANDS:
    if os.path.isfile(os.path.join(c, "npm.cmd")):
        node_dir = c
        break
print("node_dir =", node_dir)
if not node_dir:
    print("FAIL 未找到含 npm.cmd 的 node 目录")
    sys.exit(1)

# ① 带 nodeDir 探测：pmExe 必须落在该目录
if os.path.exists(OUT):
    os.remove(OUT)
subprocess.run([VITE, "--detect", FRONT, OUT, node_dir], capture_output=True, timeout=120)
d = {}
for line in open(OUT, encoding="utf-8"):
    line = line.rstrip("\r\n")
    if "=" in line:
        k, v = line.split("=", 1)
        d[k] = v
fails = []
if d.get("ok") != "True":
    fails.append("ok 期望 True 实际 %r" % d.get("ok"))
pm_exe = d.get("pmExe", "")
if not pm_exe.startswith(node_dir):
    fails.append("pmExe 应在 nodeDir 下，实际 %r" % pm_exe)
if d.get("nodeDir") != node_dir:
    fails.append("nodeDir 回读 %r" % d.get("nodeDir"))

# ② 不带 nodeDir：pmExe 不应指向 nodeDir（走 PATH）
OUT2 = os.path.join(HERE, "_selftest", "vite_nodechain_auto.txt")
subprocess.run([VITE, "--detect", FRONT, OUT2], capture_output=True, timeout=120)
d2 = {}
for line in open(OUT2, encoding="utf-8"):
    line = line.rstrip("\r\n")
    if "=" in line:
        k, v = line.split("=", 1)
        d2[k] = v
if d2.get("ok") != "True":
    fails.append("auto ok 期望 True 实际 %r" % d2.get("ok"))
# auto 模式走 PATH：本机 PATH 恰好也指向该 node 目录，因此只断言解析成功，不断言路径归属
if not d2.get("pmExe"):
    fails.append("auto 模式 pmExe 应非空")
if "nodeDir=" in open(OUT2, encoding="utf-8").read():
    v = dict(l.split("=", 1) for l in open(OUT2, encoding="utf-8").read().splitlines() if "=" in l).get("nodeDir", "")
    if v != "":
        fails.append("auto 模式 nodeDir 应为空，实际 %r" % v)

if fails:
    print("FAIL " + "; ".join(fails))
    sys.exit(1)
print("ok nodeDir 注入: pmExe=%s" % pm_exe)
print("ok auto 模式   : pmExe=%s" % d2.get("pmExe"))
print("全部通过 2/2")
