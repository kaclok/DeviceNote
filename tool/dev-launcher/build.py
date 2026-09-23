# -*- coding: utf-8 -*-
"""用 .NET Framework 4.0 的 csc 编译两个 WinForms 启动器（单文件 exe，无需运行时依赖）"""
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "src")
BIN = os.path.join(HERE, "bin")

CSC_CANDIDATES = [
    r"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    r"C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe",
]

REFS = [
    "/r:System.dll",
    "/r:System.Core.dll",
    "/r:System.Xml.dll",
    "/r:System.Drawing.dll",
    "/r:System.Windows.Forms.dll",
    "/r:System.Management.dll",
]

# (临时英文名, 最终中文名, 主文件, 附加源文件, 入口类)
TARGETS = [
    ("SpringBootLauncher.exe", "启动后端.exe", "SpringBootLauncher.cs", [], None),
    ("ViteLauncher.exe", "启动前端.exe", "ViteLauncher.cs", [], None),
    ("DevLauncher.exe", "启动台.exe", "CombinedLauncher.cs",
     ["SpringBootLauncher.cs", "ViteLauncher.cs"], "DevLaunch.Program0"),
]


def find_csc():
    for c in CSC_CANDIDATES:
        if os.path.exists(c):
            return c
    return None


def build(csc, out_name, main_file, extra_sources, main_type):
    out_path = os.path.join(BIN, out_name)
    args = [
        csc, "/nologo", "/codepage:65001", "/target:winexe", "/platform:anycpu",
        "/optimize+", "/warn:4", "/out:" + out_path,
    ]
    if main_type:
        args.append("/main:" + main_type)
    args += REFS
    args.append(os.path.join(SRC, "Shared.cs"))
    args.append(os.path.join(SRC, main_file))
    for s in extra_sources:
        args.append(os.path.join(SRC, s))
    r = subprocess.run(args, capture_output=True)
    so = r.stdout.decode("gbk", errors="replace") if r.stdout else ""
    se = r.stderr.decode("gbk", errors="replace") if r.stderr else ""
    return r.returncode, (so + se).strip(), out_path


def main():
    if not os.path.isdir(BIN):
        os.makedirs(BIN)
    csc = find_csc()
    if not csc:
        print("ERROR: 未找到 csc.exe")
        return 1
    print("csc =", csc)
    # 可选：命令行给出关键字（如「启动台」或 DevLauncher）时只编译匹配的目标
    filt = sys.argv[1] if len(sys.argv) > 1 else None
    fail = 0
    for tmp_name, final_name, main_file, extra_sources, main_type in TARGETS:
        if filt and filt not in tmp_name and filt not in final_name and filt not in main_file:
            continue
        rc, log, out_path = build(csc, tmp_name, main_file, extra_sources, main_type)
        ok = rc == 0 and os.path.exists(out_path)
        print("---- %-22s %s" % (tmp_name, "OK" if ok else "FAIL"))
        if log:
            print(log)
        if not ok:
            fail += 1
            continue
        final_path = os.path.join(BIN, final_name)
        # 直接覆盖复制：os.remove 会被安全删除钩子拦住（进回收站失败反而中断构建）
        shutil.copyfile(out_path, final_path)
        os.remove(out_path)
        print("     -> %s  (%.1f KB)" % (final_name, os.path.getsize(final_path) / 1024.0))
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
