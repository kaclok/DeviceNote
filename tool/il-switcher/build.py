# -*- coding: utf-8 -*-
"""编译 完整性级别切换器（单文件 WinForms exe，复用 dev-launcher 的 Shared.cs）

用法：
    python build.py              # 全量编译
    python build.py ILSwitcher   # 关键字过滤

注意：首次编译后建议给 bin/ 目录设置 Medium 完整性标签
（DeviceNote 工程根有 Low 标签继承，否则产物 exe 双击会被安全检查拦）：
    icacls bin /setintegritylevel (OI)(CI)M
本脚本每次编译后自动执行该设置。
"""
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "src")
BIN = os.path.join(HERE, "bin")
SHARED = os.path.normpath(os.path.join(HERE, "..", "dev-launcher", "src", "Shared.cs"))
MANIFEST = os.path.join(SRC, "app.manifest")

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
]

# (临时英文名, 最终名, 主文件, 附加源文件)
# ⚠️ 临时名必须与最终名不同：NTFS 大小写不敏感，同名会 SameFileError
TARGETS = [
    ("_tmp_ILSwitcher.exe", "ILSwitcher.exe", "ILSwitcher.cs", []),
]


def find_csc():
    for c in CSC_CANDIDATES:
        if os.path.exists(c):
            return c
    return None


def build(csc, out_name, main_file, extra_sources):
    out_path = os.path.join(BIN, out_name)
    if os.path.exists(out_path):
        try:
            os.remove(out_path)
        except OSError:
            pass
    args = [
        csc, "/nologo", "/codepage:65001", "/target:winexe", "/platform:anycpu",
        "/optimize+", "/warn:4", "/out:" + out_path,
        "/win32manifest:" + MANIFEST,
    ]
    args += REFS
    args.append(SHARED)
    args.append(os.path.join(SRC, main_file))
    for s in extra_sources:
        args.append(os.path.join(SRC, s))
    r = subprocess.run(args, capture_output=True)
    so = r.stdout.decode("gbk", errors="replace") if r.stdout else ""
    se = r.stderr.decode("gbk", errors="replace") if r.stderr else ""
    return r.returncode, (so + se).strip(), out_path


def fix_bin_label():
    """bin 目录显式设为 Medium：覆盖 DeviceNote 工程根继承下来的 Low 标签，
    保证产物 exe 双击可用（详见 2026-10-19 dev-launcher 排障结论）。"""
    try:
        subprocess.run(["icacls", BIN, "/setintegritylevel", "(OI)(CI)M"],
                       capture_output=True, timeout=30)
    except Exception:
        pass


def main():
    if not os.path.isfile(SHARED):
        print("ERROR: 找不到共享库 " + SHARED)
        return 1
    if not os.path.isdir(BIN):
        os.makedirs(BIN)
    csc = find_csc()
    if not csc:
        print("ERROR: 未找到 csc.exe")
        return 1
    print("csc    =", csc)
    print("shared =", SHARED)
    filt = sys.argv[1] if len(sys.argv) > 1 else None
    fail = 0
    for tmp_name, final_name, main_file, extra_sources in TARGETS:
        if filt and filt not in tmp_name and filt not in final_name and filt not in main_file:
            continue
        rc, log, out_path = build(csc, tmp_name, main_file, extra_sources)
        ok = rc == 0 and os.path.exists(out_path)
        print("---- %-16s %s" % (tmp_name, "OK" if ok else "FAIL"))
        if log:
            print(log)
        if not ok:
            fail += 1
            continue
        final_path = os.path.join(BIN, final_name)
        shutil.copyfile(out_path, final_path)
        os.remove(out_path)
        print("     -> %s  (%.1f KB)" % (final_name, os.path.getsize(final_path) / 1024.0))
    fix_bin_label()
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
