# -*- coding: utf-8 -*-
"""gb-watcher 自检回归：行捕获 + 缓存落盘/恢复。"""
import subprocess, sys, os, shutil, json

EXE = r"F:\Projects\Study\github\DeviceNote\tool\gb-watcher\bin\GbWatcher.exe"
WORK = r"F:\Projects\Study\github\DeviceNote\tool\gb-watcher\_st_root"
OUT = os.path.join(WORK, "_result.json")
COUT = OUT + ".index"

shutil.rmtree(WORK, ignore_errors=True)
# 唯一 root：断点状态文件是全局的，复用固定路径会让旧 offset 触发「截断重读」误捕历史
import time as _t
WORK = os.path.join(WORK, "run-" + str(int(_t.time())))
os.makedirs(WORK, exist_ok=True)

r = subprocess.run([EXE, "--selftest", WORK, OUT, "6"],
                   capture_output=True, timeout=60)
lines_out = open(OUT, encoding="utf-8").read() if os.path.exists(OUT) else "(missing)"
cache_out = open(COUT, encoding="utf-8").read() if os.path.exists(COUT) else "(missing)"
print("exit =", r.returncode)
print("lines =", lines_out[:600])
print("cache =", cache_out)

data = json.loads(lines_out)
cdata = json.loads(cache_out)
ok = (r.returncode == 0 and len(data) == 4
      and cdata["captured"] >= 1 and cdata["recaptured"] >= 1 and cdata["same_fp"])
print("index_assert", "PASS" if (cdata["captured"] >= 1 and cdata["recaptured"] >= 1 and cdata["same_fp"]) else "FAIL")
print("ASSERT", "PASS" if ok else "FAIL")
