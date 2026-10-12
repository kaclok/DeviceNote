# -*- coding: utf-8 -*-
"""股票行情服务 · 启动器可配置性自检

覆盖三块：
  A. 静态：StockService.cs 的配置读写键集是否对称、默认常量与命令拼装模板是否完好；
     service.py 是否只认那两个环境变量，且旧写法（now + ttl）已消失。
  B. 动态（启动器）：真跑「股票服务.exe --detect」，逐组合核对解析结果与最终命令行 ——
     默认回落、全量自定义、解释器填目录、端口/TTL 非法、依赖清单缺失、服务目录不存在。
  C. 动态（服务端）：真跑 service.py，核对 STOCK_LIB_DIR 与 STOCK_CACHE_TTL_SCALE 生效，
     且「不设环境变量 == 原行为」。
  D. 冒烟：真开一次启动器窗口（--probe），确认界面能正常构建。

用法：
    python check_config.py                 # 用 tool/stock-service/股票服务.exe
    python check_config.py --exe <路径>     # 指定待测 exe（正式 exe 被占用时用临时构建）
"""
import argparse
import io
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile

SRV = os.path.dirname(os.path.abspath(__file__))
CS = os.path.join(SRV, "launcher", "StockService.cs")
PY = os.path.join(SRV, "service.py")
VENV_PY = os.path.join(SRV, ".venv", "Scripts", "python.exe")
INI = os.path.join(os.environ.get("APPDATA", ""), "DevLauncher", "stock.ini")

CFG_KEYS = ["python", "dir", "port", "host", "module", "req", "lib", "ttl", "autoInstall", "killPort"]

PASS = 0
FAIL = 0


def ck(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print("  ok   %s" % name)
    else:
        FAIL += 1
        print("FAIL   %s%s" % (name, ("   << " + str(detail)[:220]) if detail else ""))


def read(path):
    return io.open(path, encoding="utf-8").read().replace("\r\n", "\n")


# --------------------------------------------------------------------------- A
def static_checks():
    print("\n[A] 静态：配置口径与命令模板")
    cs = read(CS)
    py = read(PY)

    load_keys = set(re.findall(r'Get\(c,\s*"([^"]+)"\)', cs))
    save_keys = set(re.findall(r'c\["([^"]+)"\]\s*=', cs))
    ck("Save 键集 == Load 键集（读写对称）", load_keys == save_keys,
       "load=%s save=%s" % (sorted(load_keys), sorted(save_keys)))
    ck("配置键集 == 预期 10 项", load_keys == set(CFG_KEYS),
       sorted(load_keys))

    ck('端口默认常量 = "7300"', 'DefPort = "7300"' in cs)
    ck('监听地址默认常量 = "0.0.0.0"', 'DefHost = "0.0.0.0"' in cs)
    ck('启动模块默认常量 = "main:app"', 'DefModule = "main:app"' in cs)
    ck('缓存 TTL 默认常量 = "1"', 'DefTtl = "1"' in cs)

    ck("命令模板含 uvicorn ASGI 目标", '-m uvicorn " + r.Module' in cs)
    ck("命令模板含 --host / --port", '" --host " + r.Host + " --port " + r.Port' in cs)
    ck("自动安装时会拼 pip install -r", '" -m pip install -r \\"" + r.Req' in cs)
    ck("配置持久化走既有 Cfg（app=stock）",
       'Cfg.Load("stock")' in cs and 'Cfg.Save("stock"' in cs)

    ck("C# 侧注入 STOCK_LIB_DIR", 'e["STOCK_LIB_DIR"] = r.Lib' in cs)
    ck("C# 侧注入 STOCK_CACHE_TTL_SCALE", 'e["STOCK_CACHE_TTL_SCALE"] = Num(r.Ttl)' in cs)
    ck("service.py 读 STOCK_LIB_DIR", 'os.environ.get("STOCK_LIB_DIR", "")' in py)
    ck("service.py 读 STOCK_CACHE_TTL_SCALE", 'os.environ.get("STOCK_CACHE_TTL_SCALE", "")' in py)
    ck("service.py 缓存写的是 effective（倍率已生效）", "now + effective, value" in py)
    ck("service.py 旧写法 now + ttl 已消失", "now + ttl, value" not in py)
    ck("service.py 倍率 <=0 时直取上游", "if effective <= 0:" in py)
    ck("service.py 缺省 lib 目录仍是本文件同级 lib/",
       'os.path.join(os.path.dirname(os.path.abspath(__file__)), "lib")' in py)

    bat = read(os.path.join(SRV, "start.bat"))
    ck("start.bat 优先委派启动器 exe", '"股票服务.exe"' in bat and 'start "" "股票服务.exe"' in bat)
    ck("start.bat 保留 .venv 直启回落", ".venv\\Scripts\\python.exe -m uvicorn main:app" in bat)


# --------------------------------------------------------------------------- B
def detect(exe, out_path, timeout=60):
    if os.path.exists(out_path):
        os.remove(out_path)
    subprocess.run([exe, "--detect", out_path], timeout=timeout, capture_output=True)
    txt = read(out_path)
    d = {}
    for line in txt.split("\n"):
        if "=" in line:
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d


def write_ini(pairs):
    os.makedirs(os.path.dirname(INI), exist_ok=True)
    lines = ["%s=%s" % (k, v) for k, v in pairs]
    io.open(INI, "wb").write(("\n".join(lines) + "\n").encode("utf-8"))


def launcher_checks(exe):
    print("\n[B] 动态：%s --detect" % os.path.basename(exe))
    tmp = tempfile.mkdtemp(prefix="_stockcfg_")
    out = os.path.join(tmp, "detect.txt")
    exe_dir = os.path.dirname(os.path.abspath(exe))
    py_default = os.path.join(exe_dir, ".venv", "Scripts", "python.exe")

    # --- 1) ini 不存在：全部回落到默认
    if os.path.exists(INI):
        os.remove(INI)
    d = detect(exe, out)
    same_dir = lambda a, b: os.path.normcase((a or "").rstrip("\\/")) == os.path.normcase(b.rstrip("\\/"))
    ck("默认 服务目录 = exe 所在目录", same_dir(d.get("dir"), exe_dir), d.get("dir"))
    ck("默认 解释器 = <服务目录>\\.venv\\Scripts\\python.exe",
       d.get("python") == py_default, d.get("python"))
    ck("默认 pythonExists 与磁盘一致",
       (d.get("pythonExists") == "True") == os.path.exists(py_default), d.get("pythonExists"))
    ck("默认 端口 7300", d.get("port") == "7300", d.get("port"))
    ck("默认 监听地址 0.0.0.0", d.get("host") == "0.0.0.0", d.get("host"))
    ck("默认 启动模块 main:app", d.get("module") == "main:app", d.get("module"))
    ck("默认 缓存 TTL 倍率 1", d.get("ttl") == "1", d.get("ttl"))
    ck("默认 依赖清单 = <服务目录>\\requirements.txt",
       d.get("req") == os.path.join(exe_dir, "requirements.txt"), d.get("req"))
    ck("默认 lib 目录 = <服务目录>\\lib",
       d.get("lib") == os.path.join(exe_dir, "lib"), d.get("lib"))
    ck("默认 命令行精确匹配",
       d.get("command") == '"%s" -m uvicorn main:app --host 0.0.0.0 --port 7300' % py_default,
       d.get("command"))
    ck("默认 两个行为开关均关闭",
       d.get("autoInstall") == "False" and d.get("killPort") == "False")
    ck("解释器缺失时 ok=False 且报错",
       (d.get("ok") == "False") == (not os.path.exists(py_default)),
       "ok=%s errors=%s" % (d.get("ok"), d.get("errors")))

    # --- 2) 全量自定义
    py = VENV_PY if os.path.exists(VENV_PY) else sys.executable
    req = os.path.join(SRV, "requirements.txt")
    lib = os.path.join(SRV, "lib")
    write_ini([("python", py), ("dir", SRV), ("port", "8123"), ("host", "127.0.0.1"),
               ("module", "main:app"), ("req", req), ("lib", lib), ("ttl", "0.5"),
               ("autoInstall", "1"), ("killPort", "1")])
    d = detect(exe, out)
    ck("自定义 目录取自 ini", d.get("dir") == SRV, d.get("dir"))
    ck("自定义 端口取自 ini", d.get("port") == "8123", d.get("port"))
    ck("自定义 监听地址取自 ini", d.get("host") == "127.0.0.1", d.get("host"))
    ck("自定义 TTL 小数（InvariantCulture）", d.get("ttl") == "0.5", d.get("ttl"))
    ck("自定义 开关均打开",
       d.get("autoInstall") == "True" and d.get("killPort") == "True")
    ck("自定义 无错误", d.get("ok") == "True" and d.get("errors") == "",
       d.get("errors"))
    ck("自定义 命令行 = pip 安装 && uvicorn",
       d.get("command") == '"%s" -m pip install -r "%s" && "%s" -m uvicorn main:app --host 127.0.0.1 --port 8123'
       % (py, req, py), d.get("command"))
    ck("自定义 env 行含两个环境变量",
       ("STOCK_LIB_DIR=" + lib) in d.get("env", "")
       and "STOCK_CACHE_TTL_SCALE=0.5" in d.get("env", ""), d.get("env"))

    # --- 3) 解释器填目录 → 自动补 python.exe
    scripts_dir = os.path.dirname(VENV_PY)          # 形如 <服务目录>\.venv\Scripts
    write_ini([("python", scripts_dir), ("dir", SRV)])
    d = detect(exe, out)
    ck("解释器填目录 → 解析为 <目录>\\python.exe",
       d.get("python") == VENV_PY, d.get("python"))
    ck("解释器填目录 → 无错误", d.get("errors") == "", d.get("errors"))

    # --- 4) 端口非法 / 越界
    write_ini([("dir", SRV), ("python", py), ("port", "abc")])
    d = detect(exe, out)
    ck("端口非数字 → 报错且 port=0",
       "端口非法" in d.get("errors", "") and d.get("port") == "0",
       "port=%s errors=%s" % (d.get("port"), d.get("errors")))
    write_ini([("dir", SRV), ("python", py), ("port", "70000")])
    d = detect(exe, out)
    ck("端口越界 70000 → 报错", "端口非法" in d.get("errors", ""), d.get("errors"))

    # --- 5) TTL 非法回落 / 0 合法
    write_ini([("dir", SRV), ("python", py), ("ttl", "abc")])
    d = detect(exe, out)
    ck("TTL 非数字 → 回落 1 并给出提示",
       d.get("ttl") == "1" and "非法" in d.get("notes", ""),
       "ttl=%s notes=%s" % (d.get("ttl"), d.get("notes")))
    write_ini([("dir", SRV), ("python", py), ("ttl", "0")])
    d = detect(exe, out)
    ck("TTL=0 合法（关缓存）且无告警",
       d.get("ttl") == "0" and "非法" not in d.get("notes", ""),
       "ttl=%s notes=%s" % (d.get("ttl"), d.get("notes")))

    # --- 6) 依赖清单缺失：开自动安装 → 报错；不开 → 仅提示
    missing = os.path.join(SRV, "_no_such_requirements.txt")
    write_ini([("dir", SRV), ("python", py), ("req", missing), ("autoInstall", "1")])
    d = detect(exe, out)
    ck("自动安装但依赖清单缺失 → 报错",
       "依赖清单不存在" in d.get("errors", ""), d.get("errors"))
    write_ini([("dir", SRV), ("python", py), ("req", missing), ("autoInstall", "0")])
    d = detect(exe, out)
    ck("不自动安装时依赖清单缺失 → 只提示不报错",
       d.get("errors") == "" and "依赖清单不存在" in d.get("notes", ""),
       "errors=%s notes=%s" % (d.get("errors"), d.get("notes")))

    # --- 7) 服务目录不存在
    write_ini([("dir", os.path.join(SRV, "_no_such_dir")), ("python", py)])
    d = detect(exe, out)
    ck("服务目录不存在 → 报错",
       "服务目录不存在" in d.get("errors", ""), d.get("errors"))

    shutil.rmtree(tmp, ignore_errors=True)


# --------------------------------------------------------------------------- C
def run_py(code, env_extra, timeout=120):
    tmp = tempfile.mkdtemp(prefix="_stockpy_")
    f = os.path.join(tmp, "probe.py")
    io.open(f, "wb").write(code.encode("utf-8"))
    env = dict(os.environ)
    env.pop("STOCK_LIB_DIR", None)
    env.pop("STOCK_CACHE_TTL_SCALE", None)
    env.update(env_extra)
    env["PYTHONIOENCODING"] = "utf-8"
    r = subprocess.run([VENV_PY, f], cwd=SRV, env=env, capture_output=True,
                       timeout=timeout)
    shutil.rmtree(tmp, ignore_errors=True)
    out = (r.stdout or b"").decode("utf-8", "replace")
    err = (r.stderr or b"").decode("utf-8", "replace")
    lines = [l for l in out.split("\n") if l.strip().startswith("{")]
    data = json.loads(lines[-1]) if lines else {}
    return data, out, err, r.returncode


TTL_PROBE = r'''
import json, os, sys, time
sys.path.insert(0, os.getcwd())
import service

calls = {"n": 0}

@service._cached(10)
def probe_fn(x):
    calls["n"] += 1
    return x * 2

a = probe_fn(3)
b = probe_fn(3)
entry = None
for k, v in service._CACHE.items():
    entry = {"name": str(k[0]), "expires_in": round(v[0] - time.time(), 2), "value": v[1]}
    break
print(json.dumps({"scale": service._ttl_scale(), "calls": calls["n"],
                  "a": a, "b": b, "entry": entry}))
'''

LIB_PROBE = r'''
import json, os, sys
sys.path.insert(0, os.getcwd())
import service
err = ""
try:
    mod = service._ashare()
    info = {"file": getattr(mod, "__file__", ""), "marker": getattr(mod, "MARKER", "")}
except Exception as exc:                       # noqa: BLE001  依赖缺失不影响目录判据
    info = {"file": "", "marker": ""}
    err = repr(exc)
print(json.dumps({"path0": sys.path[0] if sys.path else "", "info": info, "err": err}))
'''


def service_checks():
    print("\n[C] 动态：service.py 的环境变量")
    if not os.path.exists(VENV_PY):
        ck("服务端自检需要 .venv 解释器", False, VENV_PY)
        return

    # 不设环境变量 == 原行为
    d, out, err, rc = run_py(TTL_PROBE, {})
    ck("缺省 TTL 倍率 = 1.0", d.get("scale") == 1.0, d or err or out)
    ck("缺省：同参两次调用只执行一次（缓存生效）", d.get("calls") == 1, d)
    ck("缺省：缓存有效期 ≈ 原 TTL 10s",
       d.get("entry") and 8.0 <= d["entry"]["expires_in"] <= 10.0, d.get("entry"))

    d, out, err, rc = run_py(LIB_PROBE, {})
    ck("缺省 lib 目录 = 服务目录\\lib", os.path.normcase(d.get("path0", "")) == os.path.normcase(os.path.join(SRV, "lib")),
       d.get("path0") or err)

    # 倍率 2 → TTL 加倍
    d, out, err, rc = run_py(TTL_PROBE, {"STOCK_CACHE_TTL_SCALE": "2"})
    ck("倍率 2：TTL 加倍（≈20s）",
       d.get("entry") and 18.0 <= d["entry"]["expires_in"] <= 20.0, d.get("entry"))
    ck("倍率 2：仍命中缓存", d.get("calls") == 1, d)

    # 倍率 2.5
    d, out, err, rc = run_py(TTL_PROBE, {"STOCK_CACHE_TTL_SCALE": "2.5"})
    ck("倍率 2.5：TTL ≈25s",
       d.get("entry") and 23.0 <= d["entry"]["expires_in"] <= 25.0, d.get("entry"))

    # 倍率 0 → 关缓存
    d, out, err, rc = run_py(TTL_PROBE, {"STOCK_CACHE_TTL_SCALE": "0"})
    ck("倍率 0：缓存关闭（两次调用都真执行）", d.get("calls") == 2, d)
    ck("倍率 0：不写缓存表", d.get("entry") is None, d.get("entry"))
    ck("倍率 0：返回值仍然正确", d.get("a") == 6 and d.get("b") == 6, d)

    # 非法 / 空
    d, out, err, rc = run_py(TTL_PROBE, {"STOCK_CACHE_TTL_SCALE": "abc"})
    ck("倍率非法值 → 1.0", d.get("scale") == 1.0, d)
    d, out, err, rc = run_py(TTL_PROBE, {"STOCK_CACHE_TTL_SCALE": "  "})
    ck("倍率空白值 → 1.0", d.get("scale") == 1.0, d)

    # 自定义 lib 目录真的被采用
    tmp = tempfile.mkdtemp(prefix="_stocklib_")
    io.open(os.path.join(tmp, "Ashare.py"), "wb").write(
        b"MARKER = 'custom-ashare-lib'\n")
    d, out, err, rc = run_py(LIB_PROBE, {"STOCK_LIB_DIR": tmp})
    ck("STOCK_LIB_DIR 生效：sys.path[0] 指向该目录",
       os.path.normcase(d.get("path0", "")) == os.path.normcase(tmp), d.get("path0") or err)
    ck("STOCK_LIB_DIR 生效：真的 import 到该目录的模块",
       d.get("info", {}).get("marker") == "custom-ashare-lib", d)
    shutil.rmtree(tmp, ignore_errors=True)


# --------------------------------------------------------------------------- D
def free_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def gui_smoke(exe):
    print("\n[D] 冒烟：启动器窗口（--probe，2.5 秒后自动关闭）")
    tmp = tempfile.mkdtemp(prefix="_stockgui_")
    out = os.path.join(tmp, "probe.txt")
    py = VENV_PY if os.path.exists(VENV_PY) else sys.executable
    # 用一个确定空闲的端口：状态断言不该受「本机此刻是否已有服务在跑」影响
    port = free_port()
    write_ini([("python", py), ("dir", SRV), ("port", str(port))])
    r = subprocess.run([exe, "--probe", out], timeout=90, capture_output=True)
    if not os.path.exists(out):
        ck("窗口能正常构建并落盘状态", False, "exit=%s" % r.returncode)
        shutil.rmtree(tmp, ignore_errors=True)
        return
    txt = read(out)
    ck("窗口能正常构建并落盘状态", ("state=" in txt and "title=" in txt), txt[:160])
    ck("未启动服务时状态为「未运行」", "state=● 未运行" in txt, txt[:160])
    ck("未启动服务时按钮为「启动服务」", "toggle=▶  启动服务" in txt, txt[:160])
    ck("接口文档按钮初始禁用", "docsEnabled=False" in txt)
    ck("标题带端口", "title=股票行情服务  localhost:%d" % port in txt, txt[:200])
    ck("摘要行含端口/地址/模块",
       "cfgLine=端口 %d · 0.0.0.0 · uvicorn main:app" % port in txt, txt[:300])
    ck("窗口读到的 ini 路径正确", "stock.ini" in txt)
    shutil.rmtree(tmp, ignore_errors=True)


# --------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--exe", default=os.path.join(SRV, "股票服务.exe"))
    ap.add_argument("--skip-gui", action="store_true")
    args = ap.parse_args()

    exe = os.path.abspath(args.exe)
    print("exe = %s" % exe)
    if not os.path.exists(exe):
        print("ERROR: 找不到 exe")
        return 1

    backup = None
    existed = os.path.exists(INI)
    if existed:
        backup = INI + ".checkbak"
        shutil.copyfile(INI, backup)
    try:
        static_checks()
        launcher_checks(exe)
        service_checks()
        if not args.skip_gui:
            gui_smoke(exe)
    finally:
        if os.path.exists(INI):
            os.remove(INI)
        if backup:
            shutil.move(backup, INI)
            print("\n已恢复原有 stock.ini")

    print("\n==== check_config: pass=%d fail=%d ====" % (PASS, FAIL))
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
