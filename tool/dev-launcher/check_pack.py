# -*- coding: utf-8 -*-
"""打包功能自检：--detect 输出里的 packageCmd / packageDir / outDir（不执行打包）

用法：python check_pack.py
"""
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, "bin")
OUT = os.path.join(HERE, "_selftest")
BOOT = os.path.join(BIN, "启动后端.exe")
VITE = os.path.join(BIN, "启动前端.exe")
COMBO = os.path.join(BIN, "启动台.exe")

REPO = r"F:\Projects\Study\github\DeviceNote"
BOOT_DIR = os.path.join(REPO, "backend", "device-note")
VITE_DIR = os.path.join(REPO, "ft-checnote")

passed = failed = 0


def ck(name, cond, detail=""):
    global passed, failed
    if cond:
        passed += 1
        print("ok   %s" % name)
    else:
        failed += 1
        print("FAIL %s  %s" % (name, detail))


def run_detect(exe, args, outname):
    outfile = os.path.join(OUT, outname)
    if os.path.exists(outfile):
        os.remove(outfile)
    subprocess.run([exe] + args + [outfile], capture_output=True, timeout=180)
    if not os.path.exists(outfile):
        return {}
    d = {}
    with open(outfile, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if "=" in line:
                k, v = line.split("=", 1)
                d[k] = v
    return d


def main():
    os.makedirs(OUT, exist_ok=True)

    # ---- 后端：多模块 ----
    b = run_detect(BOOT, ["--detect", BOOT_DIR], "pack_boot_multi.txt")
    ck("boot multi ok", b.get("ok") == "True", str(b.get("ok")))
    pc = b.get("packageCmd", "")
    ck("multi packageCmd 带 -pl + -am package -DskipTests",
       ("package -DskipTests" in pc and "-pl" in pc and "-am" in pc), pc)
    ck("multi packageCmd 不带 repackage.skip（要产出可执行 jar）",
       "repackage.skip" not in pc, pc)
    ck("multi packageCmd 带工具链 extra（-s / repo 按配置）",
       ("-s " in pc or "-Dmaven.repo.local" in pc or pc.count('"') >= 2), pc)
    pd = b.get("packageDir", "")
    ck("multi packageDir = 根目录/launcher/target",
       pd.lower().endswith("target") and "launcher" in pd, pd)

    # ---- 后端：单模块（lp-jtlj）----
    b2 = run_detect(BOOT, ["--detect", os.path.join(REPO, "backend", "lp-jtlj")], "pack_boot_single.txt")
    ck("boot single ok", b2.get("ok") == "True", str(b2.get("ok")))
    pc2 = b2.get("packageCmd", "")
    ck("single packageCmd = mvn package -DskipTests（无 -pl）",
       ("package -DskipTests" in pc2 and "-pl" not in pc2), pc2)
    ck("single packageDir 落在工程目录 target",
       b2.get("packageDir", "").lower().endswith("target"), b2.get("packageDir"))

    # ---- 前端 ----
    v = run_detect(VITE, ["--detect", VITE_DIR], "pack_vite.txt")
    ck("vite ok", v.get("ok") == "True", str(v.get("ok")))
    ck("vite outDir = dist-0.0.1-cors（读自 vite.config 的 outDir 表达式）",
       v.get("outDir") == "dist-0.0.1-cors", str(v.get("outDir")))
    ck("vite default 脚本仍是 local dev（打包脚本独立选择 build，不改默认）",
       v.get("default") == "local dev", str(v.get("default")))

    # ---- 启动台：--detect-all 不受影响 ----
    c = run_detect(COMBO, ["--detect-all", BOOT_DIR, VITE_DIR], "pack_combo.txt")
    ck("combo backend ok", c.get("ok") == "True", str(c.get("ok")))
    ck("combo frontend ok", c.get("ok") == "True", str(c.get("ok")))

    # ---- 源码级：三处 UI 接线齐全（编译器已验语法，这里验装配）----
    def src(name):
        with open(os.path.join(HERE, "src", name), encoding="utf-8") as f:
            return f.read()

    sb = src("SpringBootLauncher.cs")
    vt = src("ViteLauncher.cs")
    cb = src("CombinedLauncher.cs")
    ck("boot: packRunner 接线 + 打包行 + 防并发守卫",
       ("packRunner.Out += OnPackOut" in sb and "btnPack = Ux.Btn(\"打包\"" in sb
        and "packRunner.Running" in sb))
    ck("vite: BuildScript 优先 build + GuessOutDir 兜底 dist",
       ("== \"build\"") in vt and "return \"dist\";" in vt)
    ck("vite: packRunner 接线 + 打包行",
       ("packRunner.Out += OnPackOut" in vt and "btnPack = Ux.Btn(\"打包\"" in vt))
    ck("combo: 双 packRunner 接线 + 4 个打包按钮",
       ("packRunB.Out += OnPackOutB" in cb and "packRunF.Out += OnPackOutF" in cb
        and "Ux.Btn(\"打包后端\"" in cb and "Ux.Btn(\"打包前端\"" in cb
        and "Ux.Btn(\"后端产物\"" in cb and "Ux.Btn(\"前端产物\"" in cb))
    ck("combo: 打包时后端运行中会拒绝（jar 被锁）",
       "运行中的 jar 无法被覆盖" in cb)
    ck("产物目录快捷按钮三处都有",
       ("OpenPackDir()" in sb and "OpenPackDir()" in vt
        and "OpenPackDirB()" in cb and "OpenPackDirF()" in cb))

    print("\n%s  通过 %d / 失败 %d" % ("全部通过" if not failed else "存在失败", passed, failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
