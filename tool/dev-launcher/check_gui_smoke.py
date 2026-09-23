# -*- coding: utf-8 -*-
"""GUI 冒烟：启动前端.exe 正常起窗 4 秒不崩，然后关闭"""
import subprocess, time, os, sys

EXE = r"F:\Projects\Study\github\DeviceNote\tool\dev-launcher\bin\启动前端.exe"
p = subprocess.Popen([EXE])
time.sleep(4)
alive = p.poll() is None
if alive:
    p.kill()
    p.wait()
print("ok 窗口启动" if alive else "FAIL 启动后 4 秒内退出")
sys.exit(0 if alive else 1)
