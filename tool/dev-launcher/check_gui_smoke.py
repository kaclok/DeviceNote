# -*- coding: utf-8 -*-
"""GUI 冒烟：三个 exe 各正常起窗 4 秒不崩，然后关闭"""
import subprocess, time, os, sys

BIN = r"F:\Projects\Study\github\DeviceNote\tool\dev-launcher\bin"
EXES = ["启动后端.exe", "启动前端.exe", "启动台.exe"]

failed = False
for name in EXES:
    p = subprocess.Popen([os.path.join(BIN, name)])
    time.sleep(4)
    alive = p.poll() is None
    if alive:
        p.kill()
        p.wait()
    print(("ok   " if alive else "FAIL ") + name + (" 窗口启动" if alive else " 启动后 4 秒内退出"))
    failed = failed or not alive

sys.exit(1 if failed else 0)
