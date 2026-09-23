# -*- coding: utf-8 -*-
"""工程探测自检（不启动任何服务）

用法：python selftest.py
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, "bin")
OUT = os.path.join(HERE, "_selftest")
BOOT = os.path.join(BIN, "启动后端.exe")
VITE = os.path.join(BIN, "启动前端.exe")

REPO = r"F:\Projects\Study\github\DeviceNote"

# (exe, 目标目录, 输出文件, 期望字段)
CASES = [
    (BOOT, os.path.join(REPO, "backend", "device-note"), "boot_root.txt",
     {"ok": "True", "multi": "True", "launchModule": "launcher",
      "profiles": "dev,test,deploy", "port": "8092"}),
    (BOOT, os.path.join(REPO, "backend", "device-note", "launcher"), "boot_module.txt",
     {"ok": "True", "multi": "True", "launchModule": "launcher", "port": "8092"}),
    (BOOT, os.path.join(REPO, "backend", "lp-jtlj"), "boot_single.txt",
     {"ok": "True", "multi": "False", "port": "7052"}),
    (BOOT, os.path.join(REPO, "ft-checnote"), "boot_mismatch.txt",
     {"ok": "False"}),
    (VITE, os.path.join(REPO, "ft-checnote"), "vite_ok.txt",
     {"ok": "True", "pm": "npm", "default": "local dev", "port": "4177"}),
    (VITE, os.path.join(REPO, "backend", "device-note"), "vite_mismatch.txt",
     {"ok": "False"}),
]


def parse(path):
    d = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if "=" in line and not line.startswith("----"):
                k, v = line.split("=", 1)
                d[k] = v
    return d


def main():
    os.makedirs(OUT, exist_ok=True)
    passed = failed = 0
    for exe, target, outname, expect in CASES:
        outfile = os.path.join(OUT, outname)
        if os.path.exists(outfile):
            os.remove(outfile)
        subprocess.run([exe, "--detect", target, outfile], capture_output=True, timeout=180)
        if not os.path.exists(outfile):
            print("FAIL %-18s 无输出文件" % outname)
            failed += 1
            continue
        got = parse(outfile)
        bad = []
        for k, v in expect.items():
            if got.get(k) != v:
                bad.append("%s 期望 %r 实际 %r" % (k, v, got.get(k)))
        if bad:
            failed += 1
            print("FAIL %-18s %s" % (outname, "; ".join(bad)))
        else:
            passed += 1
            print("ok   %-18s %s" % (outname, got.get("summary") or got.get("error") or ""))
    print("\n%s  通过 %d / 失败 %d" % ("全部通过" if not failed else "存在失败", passed, failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
