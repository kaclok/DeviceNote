# -*- coding: utf-8 -*-
"""端到端验证：真正启动一次 vite，验证 cmd 引号 / UTF-8 输出 / ANSI 剥离 / 进程树清理

用法：python e2e.py [--with-install]
    --with-install  额外跑一遍后端 maven install（会真正编译，约 20s~数分钟）
"""
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
BIN = os.path.join(HERE, "bin")
OUT = os.path.join(HERE, "_selftest")
BOOT = os.path.join(BIN, "启动后端.exe")
VITE = os.path.join(BIN, "启动前端.exe")

REPO = r"F:\Projects\Study\github\DeviceNote"
VITE_DIR = os.path.join(REPO, "ft-checnote")
BOOT_DIR = os.path.join(REPO, "backend", "device-note")
NPM = r"C:\Users\DELL\.workbuddy\binaries\node\versions\22.22.2-3\npm.cmd"
MVN = r"E:\Javas\maven\3.9.6\bin\mvn.cmd"

results = []


def check(name, cond, extra=""):
    results.append((name, bool(cond)))
    print("%-4s %-34s %s" % ("ok" if cond else "FAIL", name, extra))


def probe(exe, workdir, command, outname, seconds):
    outfile = os.path.join(OUT, outname)
    if os.path.exists(outfile):
        os.remove(outfile)
    subprocess.run([exe, "--run", workdir, command, outfile, str(seconds)],
                   capture_output=True, timeout=seconds + 300)
    return open(outfile, "rb").read() if os.path.exists(outfile) else b""


def listening(port):
    r = subprocess.run(["netstat", "-ano", "-p", "tcp"], capture_output=True)
    t = r.stdout.decode("gbk", "replace")
    for line in t.splitlines():
        p = line.split()
        if len(p) >= 5 and p[0] == "TCP" and p[1].endswith(":%d" % port) and p[3] == "LISTENING":
            return int(p[4])
    return 0


def main():
    os.makedirs(OUT, exist_ok=True)

    print("### 1) 真启动 vite，然后停止（验证引号 / UTF-8 / ANSI / 进程树）")
    port_before = listening(4177)
    print("    启动前 4177 占用者 PID:", port_before or "无")
    data = probe(VITE, VITE_DIR, '"%s" run "local dev"' % NPM, "e2e_vite.txt", 40)
    txt = data.decode("utf-8", "replace")
    check("vite 输出被识别", "VITE v" in txt)
    check("无 ANSI 转义残留", b"\x1b" not in data)
    check("无 U+FFFD 乱码", "\ufffd" not in txt)
    check("超时前未退出（说明在常驻）", "finishedBeforeTimeout=False" in txt)
    time.sleep(3)
    # 找出本次真实使用的端口（4177 被占时会退到 4178）
    used = 0
    for cand in (4178, 4179, 4180, 4177):
        if ":%d/" % cand in txt:
            used = cand
            break
    alive = listening(used) if used else 0
    check("停止后端口 %s 已释放" % (used or "?"), alive == 0, "残留 PID %s" % alive)

    print("\n### 2) cmd 中文路径（cmd 自身回显中文会变 U+FFFD，但路径必须能进去）")
    cn = os.path.join(os.environ.get("TEMP", r"C:\Windows\Temp"), "中文目录测试")
    os.makedirs(cn, exist_ok=True)
    data = probe(VITE, os.environ.get("TEMP", "C:\\Windows\\Temp"),
                 'cd /d "%s" && echo REACHED-CN-DIR' % cn, "e2e_cnpath.txt", 8)
    txt = data.decode("utf-8", "replace")
    check("cd 进入含中文路径成功", "REACHED-CN-DIR" in txt)

    if "--with-install" in sys.argv:
        print("\n### 3) 后端 maven install（真编译）")
        cmd = '"%s" -B -Pdev -pl "launcher" -am install -DskipTests -Dspring-boot.repackage.skip=true' % MVN
        t0 = time.time()
        data = probe(BOOT, BOOT_DIR, cmd, "e2e_install.txt", 900)
        txt = data.decode("utf-8", "replace")
        check("BUILD SUCCESS", "BUILD SUCCESS" in txt, "%.0fs" % (time.time() - t0))
        check("无 U+FFFD 乱码", "\ufffd" not in txt)
    else:
        print("\n### 3) 后端 install 已跳过（加 --with-install 可开启）")

    bad = [n for n, ok in results if not ok]
    print("\n%s  共 %d 项，失败 %d 项" % ("全部通过" if not bad else "存在失败", len(results), len(bad)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
