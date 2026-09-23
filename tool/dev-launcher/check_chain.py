# -*- coding: utf-8 -*-
# 验证工具链配置：--chain 注入真实 JDK/Maven/仓库/settings，对比默认 --detect 的输出
import io, os, re, shutil, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")

HERE = os.path.dirname(os.path.abspath(__file__))
EXE = os.path.join(HERE, "bin", "启动后端.exe")
OUT = os.path.join(HERE, "_selftest")
os.makedirs(OUT, exist_ok=True)
PROJ = os.path.join(HERE, "..", "..", "backend", "device-note")
PROJ = os.path.abspath(PROJ)

fails = []
def check(name, cond, detail=""):
    print(("PASS  " if cond else "FAIL  ") + name + (("  | " + detail) if detail else ""))
    if not cond: fails.append(name)

# ---- 探测本机真实工具链 ----
jdk = os.environ.get("JAVA_HOME", "")
if not (jdk and os.path.isdir(jdk)):
    for cand in [r"C:\Program Files\Java", r"C:\Program Files\Eclipse Adoptium", r"D:\Java", r"E:\Java"]:
        if os.path.isdir(cand):
            subs = [os.path.join(cand, d) for d in sorted(os.listdir(cand))]
            subs = [s for s in subs if os.path.isdir(s)]
            if subs: jdk = subs[-1]; break
mvn = shutil.which("mvn")
mvn_home = ""
if mvn:
    p = os.path.abspath(mvn)
    bin_dir = os.path.dirname(p)
    parent = os.path.dirname(bin_dir)
    if os.path.isfile(os.path.join(parent, "bin", "mvn.cmd")):
        mvn_home = parent
settings = os.path.join(os.environ.get("USERPROFILE", ""), ".m2", "settings.xml")
m2h = os.environ.get("M2_HOME") or os.environ.get("MAVEN_HOME") or ""
if not os.path.isfile(settings) and m2h:
    settings = os.path.join(m2h, "conf", "settings.xml")
repo = "F:\\MavenRepo" if os.path.isdir("F:\\MavenRepo") else (os.path.join(os.environ.get("USERPROFILE",""), ".m2", "repository") if os.path.isdir(os.path.join(os.environ.get("USERPROFILE",""), ".m2", "repository")) else "")
print("jdk      =", jdk)
print("mvn_home =", mvn_home, "(which:", mvn, ")")
print("settings =", settings, os.path.isfile(settings))
print("repo     =", repo, os.path.isdir(repo))

# 若 settings.xml 里写着别的仓库，以它为准（更真实）
if os.path.isfile(settings):
    t = io.open(settings, encoding="utf-8", errors="replace").read()
    m = re.search(r"<localRepository>([^<]+)</localRepository>", t)
    if m and os.path.isdir(m.group(1).strip()):
        repo = m.group(1).strip()
    print("repo(from settings) =", repo)

# ---- 1) 默认 --detect（不注入工具链）----
r1 = os.path.join(OUT, "chain_default.txt")
subprocess.run([EXE, "--detect", PROJ, r1], capture_output=True, timeout=60)
d1 = io.open(r1, encoding="utf-8").read()
print("\n--- default detect ---")
print(d1.strip())

# ---- 2) --chain 注入工具链 ----
r2 = os.path.join(OUT, "chain_custom.txt")
subprocess.run([EXE, "--chain", PROJ, jdk, mvn_home, repo, settings, r2], capture_output=True, timeout=60)
d2 = io.open(r2, encoding="utf-8").read()
print("\n--- chain detect ---")
print(d2.strip())

def kv(text, key):
    m = re.search(r"^%s=(.*)$" % key, text, re.M)
    return m.group(1).strip() if m else ""

# ---- 断言 ----
check("default: 探测成功", kv(d1, "ok") == "True")
check("chain: 探测成功", kv(d2, "ok") == "True")
if jdk:
    check("chain: javaHome 覆盖为配置的 JDK", kv(d2, "javaHome").lower() == jdk.lower(), kv(d2, "javaHome"))
cmd2 = kv(d2, "command")
if mvn_home:
    want = os.path.join(mvn_home, "bin", "mvn.cmd").lower()
    check("chain: mvn 用配置目录下的 mvn.cmd", want in cmd2.lower(), cmd2[:80])
check("chain: 命令带 -Dmaven.repo.local", "-Dmaven.repo.local=\"%s\"" % repo in cmd2 if repo else True)
check("chain: 命令带 -s settings", "-s \"%s\"" % settings in cmd2 if settings and os.path.isfile(settings) else True)
cmd1 = kv(d1, "command")
check("default: 命令不带 -Dmaven.repo.local（未配置不下发）", "-Dmaven.repo.local" not in cmd1)
check("chain: needInstall 仍可判定", kv(d2, "needInstall") in ("True", "False"))
# 仓库对比：needInstall 在指定 repo 下应有明确结果，且与 default 一致（同一台机器上应同值）
check("chain/default: needInstall 判定一致", kv(d1, "needInstall") == kv(d2, "needInstall"),
      "default=%s chain=%s" % (kv(d1, "needInstall"), kv(d2, "needInstall")))
# 两条 mvn 命令（install 与 run）都带上了覆盖参数
check("chain: install 段也带 -s", cmd2.count("-s \"") >= (2 if cmd2.count("&&") else 1) if os.path.isfile(settings) else True,
      "&&=%d" % cmd2.count("&&"))

print("\n==== %s ====" % ("ALL PASS" if not fails else "%d FAIL" % len(fails)))
sys.exit(0)
