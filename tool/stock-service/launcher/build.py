# -*- coding: utf-8 -*-
"""编译股票服务启动器（单文件 exe，.NET Framework 4.0 csc，无运行时依赖）

产物：tool/stock-service/股票服务.exe（与 start.bat / .venv 同级 —— exe 以自己所在目录为服务根）
源码：launcher/StockService.cs + ../dev-launcher/src/Shared.cs（基础设施单一实现，不复制）
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRV = os.path.dirname(HERE)
SHARED = os.path.normpath(os.path.join(HERE, "..", "..", "dev-launcher", "src", "Shared.cs"))
MAIN = os.path.join(HERE, "StockService.cs")
OUT = os.path.join(SRV, "股票服务.exe")

CSC_CANDIDATES = [
    r"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    r"C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe",
]

REFS = [
    "/r:System.dll",
    "/r:System.Core.dll",
    "/r:System.Xml.dll",        # Shared.cs 的 Pom（Maven settings 解析）
    "/r:System.Drawing.dll",
    "/r:System.Windows.Forms.dll",
    "/r:System.Management.dll",  # Shared.cs 的 WMI 命令行查询
]


def find_csc():
    for c in CSC_CANDIDATES:
        if os.path.exists(c):
            return c
    return None


def main():
    csc = find_csc()
    if not csc:
        print("ERROR: 未找到 csc.exe")
        return 1
    if not os.path.exists(SHARED):
        print("ERROR: 未找到共享源码：" + SHARED)
        return 1
    print("csc =", csc)
    args = [csc, "/nologo", "/codepage:65001", "/target:winexe", "/platform:anycpu",
            "/optimize+", "/warn:4", "/out:" + OUT] + REFS + [SHARED, MAIN]
    r = subprocess.run(args, capture_output=True)
    out = (r.stdout or b"").decode("gbk", errors="replace") \
        + (r.stderr or b"").decode("gbk", errors="replace")
    if out.strip():
        print(out.strip())
    if r.returncode == 0 and os.path.exists(OUT):
        print("OK -> %s  (%.1f KB)" % (OUT, os.path.getsize(OUT) / 1024.0))
        return 0
    print("FAIL")
    return 1


if __name__ == "__main__":
    sys.exit(main())
