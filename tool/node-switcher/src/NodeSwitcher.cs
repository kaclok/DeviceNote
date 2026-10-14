// Node.js 版本切换器 —— WinForms 单文件工具（复用 dev-launcher 的 Shared.cs）
//
// 编译：.NET Framework 4.0 csc（C# 5）
//   ⇒ 禁用字符串插值 $""、?.、nameof、表达式体成员、out var 等 C#6+ 语法
//
// ⚠️ 为什么本工具不调用 nvm.exe
//   本机 <NVM_HOME>\nvm.exe 是正牌 nvm-windows（Go/Console，PE 头正常），但它在需要写
//   symlink 时会经 elevate.vbs 走 ShellExecute("runas", 隐藏窗口 UAC)。在非交互调用下
//   UAC 无法确认，进程以 rc=0 静默退出 —— 既拿不到任何输出（管道与文件重定向均为空），
//   也判断不了成功失败（实测 settings.txt mtime 不变、junction 不变）。因此这里自实现
//   nvm 的两个核心动作，产物完全兼容 nvm 的目录布局，nvm 日后可用时依然兼容：
//       切换 = 重建 <NVM_SYMLINK> 目录联接   ≡ nvm use
//       安装 = 下 zip 解压到 <NVM_HOME>\vX   ≡ nvm install
//
// 目录布局约定（与 nvm-windows 一致）：
//   <NVM_HOME>            版本仓库，内含 v14.21.3 / v20.2.0 / ... 各版本目录
//   <NVM_HOME>\vX.Y.Z    某版本本体（含 node.exe / npm.cmd / node_modules\npm）
//   <NVM_SYMLINK>         目录联接，指向当前激活的版本目录（PATH 里挂的就是它）
//   <NVM_SYMLINK>\node_global  npm 全局包目录（每个版本一份，互不干扰）
//
// 自检入口（GUI 无法自动化验证，故留 CLI）：
//   Node版本切换器.exe --scan     <out.txt>             环境 + 已装版本清单
//   Node版本切换器.exe --use      <ver> <out.txt>       切换版本
//   Node版本切换器.exe --releases <indexUrl> <out.txt>  拉取可下载清单
//   Node版本切换器.exe --url      <mirror> <ver> <out.txt>  打印下载地址

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;
using Microsoft.Win32;
using DevLaunch;

namespace NodeSwitch
{
    // ============================ 数据模型 ============================
    class VerEntry
    {
        public string Ver;        // v22.14.0
        public string Dir;        // E:\NVM\nvm\v22.14.0
        public long Bytes;        // 目录顶层文件字节数
        public bool Current;      // 是否为当前激活版本

        public override string ToString() { return Ver; }
    }

    class ReleaseEntry
    {
        public string Ver;        // v24.9.0
        public string Lts;        // Krypton；非 LTS 为空
        public bool Security;

        public override string ToString()
        {
            if (Lts != null && Lts.Length > 0) return Ver + "  (LTS " + Lts + ")";
            return Ver;
        }
    }

    // ============================ 环境探测 ============================
    static class NodeEnv
    {
        public static string Home = "";        // NVM_HOME
        public static string Link = "";        // NVM_SYMLINK
        public static bool Admin;
        public static string PickedHome = "";  // 用户手工指定（Cfg 持久化）

        public static void Detect()
        {
            Admin = IsAdmin();
            Home = Normalize(FirstNonEmpty(
                PickedHome,
                Environment.GetEnvironmentVariable("NVM_HOME"),
                Reg("NVM_HOME")));
            Link = Normalize(FirstNonEmpty(
                Environment.GetEnvironmentVariable("NVM_SYMLINK"),
                Reg("NVM_SYMLINK")));

            if (Home.Length == 0)
            {
                // 常见位置兜底：用户目录 AppData\Roaming\nvm、C:\nvm
                string[] cands = new string[] {
                    Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "nvm"),
                    @"C:\nvm",
                };
                foreach (string c in cands)
                    if (DirHasVersions(c)) { Home = c; break; }
                if (Home.Length == 0 && cands.Length > 0) Home = cands[0];
            }
            if (Link.Length == 0) Link = @"C:\Program Files\nodejs";
        }

        static string FirstNonEmpty(params string[] xs)
        {
            foreach (string x in xs)
                if (!string.IsNullOrEmpty(x) && x.Trim().Length > 0) return x.Trim();
            return "";
        }

        // 用户/机器环境变量之外，nvm-setup 还会写注册表
        static string Reg(string name)
        {
            try
            {
                object v = Registry.GetValue(@"HKEY_CURRENT_USER\Environment", name, null);
                if (v != null && Convert.ToString(v).Trim().Length > 0) return Convert.ToString(v);
                v = Registry.GetValue(
                    @"HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment",
                    name, null);
                if (v != null && Convert.ToString(v).Trim().Length > 0) return Convert.ToString(v);
            }
            catch (Exception) { }
            return "";
        }

        static bool DirHasVersions(string dir)
        {
            try
            {
                if (!Directory.Exists(dir)) return false;
                return Directory.GetDirectories(dir, "v*").Length > 0;
            }
            catch (Exception) { return false; }
        }

        public static string Normalize(string p)
        {
            if (string.IsNullOrEmpty(p)) return "";
            try { return Path.GetFullPath(p.Trim().Trim('"').TrimEnd('\\')); }
            catch (Exception) { return p.Trim().TrimEnd('\\'); }
        }

        public static bool IsAdmin()
        {
            try
            {
                var id = System.Security.Principal.WindowsIdentity.GetCurrent();
                var pr = new System.Security.Principal.WindowsPrincipal(id);
                return pr.IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
            }
            catch (Exception) { return false; }
        }

        // 扫描版本仓库：<NVM_HOME>\v*，且目录内含 node.exe
        public static List<VerEntry> Scan()
        {
            List<VerEntry> res = new List<VerEntry>();
            if (Home.Length == 0) return res;
            string cur = CurrentVer();
            string[] dirs;
            try { dirs = Directory.GetDirectories(Home, "v*"); }
            catch (Exception) { return res; }

            foreach (string d in dirs)
            {
                string nm = Path.GetFileName(d);
                if (!Regex.IsMatch(nm, @"^v\d+\.\d+\.\d+$")) continue;
                if (!File.Exists(Path.Combine(d, "node.exe"))) continue;
                VerEntry v = new VerEntry();
                v.Ver = nm;
                v.Dir = d;
                v.Bytes = DirBytes(d);
                v.Current = (cur != null && cur.Length > 0 &&
                             string.Equals(nm, cur, StringComparison.OrdinalIgnoreCase));
                res.Add(v);
            }
            // 版本号降序：新的在上
            res.Sort(delegate(VerEntry a, VerEntry b) { return CmpVer(b.Ver, a.Ver); });
            return res;
        }

        public static int CmpVer(string a, string b)
        {
            try { return new Version(a.TrimStart('v')).CompareTo(new Version(b.TrimStart('v'))); }
            catch (Exception) { return string.CompareOrdinal(a, b); }
        }

        // 只算顶层与 node_modules 一层：node.exe 占绝大部分体积。
        // 不做全量递归 —— 递归要 stat 几千个文件，5 个版本会让列表刷新慢到 10 秒。
        public static long DirBytes(string dir)
        {
            long n = 0;
            try
            {
                foreach (string f in Directory.GetFiles(dir))
                    try { n += new FileInfo(f).Length; } catch (Exception) { }
                string nm = Path.Combine(dir, "node_modules");
                if (Directory.Exists(nm))
                    foreach (string f in Directory.GetFiles(nm))
                        try { n += new FileInfo(f).Length; } catch (Exception) { }
            }
            catch (Exception) { }
            return n;
        }

        // 当前激活版本：问 <NVM_SYMLINK>\node.exe 自己，最可靠（junction 目标名可能被改写）
        public static string CurrentVer()
        {
            if (Link.Length == 0) return "";
            string exe = Path.Combine(Link, "node.exe");
            if (!File.Exists(exe)) return "";
            return Sh.Capture(exe, "-v").Trim();
        }

        public static string NpmVer()
        {
            if (Link.Length == 0) return "";
            string cmd = Path.Combine(Link, "npm.cmd");
            if (!File.Exists(cmd)) return "";
            return Sh.Capture("cmd.exe", "/c \"" + cmd + "\" -v").Trim();
        }

        // 目录联接的指向（dir /AL 解析，取 [target]）
        public static string LinkTarget()
        {
            if (Link.Length == 0) return "";
            // .NET 4.0 没有 DirectoryInfo.LinkTarget，用 dir /AL 解析联接指向
            string outp = Sh.RunOut("cmd.exe", "/c dir \"" + Path.GetDirectoryName(Link) + "\" /AL");
            foreach (string raw in outp.Split('\n'))
            {
                string l = raw.Trim();
                if (l.IndexOf("SYMLINKD", StringComparison.OrdinalIgnoreCase) < 0 &&
                    l.IndexOf("JUNCTION", StringComparison.OrdinalIgnoreCase) < 0) continue;
                int i = l.LastIndexOf('[');
                int j = l.LastIndexOf(']');
                if (i >= 0 && j > i) return l.Substring(i + 1, j - i - 1).TrimEnd('\\');
            }
            return "";
        }
    }

    // ============================ 进程执行 ============================
    static class Sh
    {
        public static string RunOut(string exe, string args)
        {
            string o;
            int rc = Run(exe, args, out o);
            return o;
        }

        public static string Capture(string exe, string args)
        {
            string o;
            Run(exe, args, out o);
            return o;
        }

        public static int Run(string exe, string args, out string output)
        {
            output = "";
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(exe, args);
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;
                // 不设 StandardOutputEncoding（.NET 4.5 才有的属性）：这里读的都是
                // ASCII 输出（node -v / tar 结果 / mklink 提示），默认 ANSI 解码足够
                using (Process p = Process.Start(psi))
                {
                    string so = p.StandardOutput.ReadToEnd();
                    string se = p.StandardError.ReadToEnd();
                    p.WaitForExit(120000);
                    output = (so + (se.Length > 0 ? "\r\n" + se : "")).Trim();
                    try { return p.ExitCode; } catch (Exception) { return -1; }
                }
            }
            catch (Exception ex)
            {
                output = ex.Message;
                return -1;
            }
        }

        public static int Cmd(string cmdline, out string output)
        {
            return Run("cmd.exe", "/c " + cmdline, out output);
        }
    }

    // ============================ 核心操作 ============================
    static class Ops
    {
        public const string DEFAULT_MIRROR = "https://cdn.npmmirror.com/binaries/node";
        public const string DEFAULT_INDEX = "https://cdn.npmmirror.com/binaries/node/index.json";

        static Ops()
        {
            // .NET 4.0 默认只协商 TLS1.0，访问 https 镜像必须显式打开 TLS1.2
            try { ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072; }
            catch (Exception) { }
        }

        public static string NormVer(string v)
        {
            if (v == null) return "";
            v = v.Trim();
            if (Regex.IsMatch(v, @"^\d+\.\d+\.\d+$")) v = "v" + v;
            return v;
        }

        public static bool ValidVer(string v)
        {
            return Regex.IsMatch(v == null ? "" : v.Trim(), @"^v?\d+\.\d+\.\d+$");
        }

        public static string ZipUrl(string mirror, string ver)
        {
            string m = (mirror == null || mirror.Trim().Length == 0) ? DEFAULT_MIRROR : mirror.Trim().TrimEnd('/');
            return m + "/" + ver + "/node-" + ver + "-win-x64.zip";
        }

        // ---- 切换：重建 junction ----
        // rmdir 只删联接本身（不碰目标内容）；mklink /J 重建。等价 nvm use。
        public static bool Use(string ver, Action<string> log)
        {
            ver = NormVer(ver);
            if (!ValidVer(ver)) { log("[×] 版本号非法：" + ver); return false; }
            string target = Path.Combine(NodeEnv.Home, ver);
            if (!File.Exists(Path.Combine(target, "node.exe")))
            {
                log("[×] 版本 " + ver + " 未安装：" + target);
                return false;
            }
            if (!NodeEnv.Admin)
            {
                log("[×] 需要管理员权限才能重建目录联接 " + NodeEnv.Link);
                return false;
            }

            log("[·] 目标版本目录 " + target);
            if (Directory.Exists(NodeEnv.Link) || File.Exists(NodeEnv.Link))
            {
                // ⚠️ rmdir 对 junction 是安全的：只摘掉联接，不会删目标内容
                string o;
                int rc = Sh.Cmd("rmdir \"" + NodeEnv.Link + "\"", out o);
                log("    rmdir 联接 → rc=" + rc + (o.Length > 0 ? " " + o : ""));
                if (rc != 0 && Directory.Exists(NodeEnv.Link))
                {
                    log("[×] 无法移除原有联接（可能被占用）：" + o);
                    return false;
                }
            }
            else
            {
                try
                {
                    string parent = Path.GetDirectoryName(NodeEnv.Link);
                    if (!string.IsNullOrEmpty(parent) && !Directory.Exists(parent))
                        Directory.CreateDirectory(parent);
                }
                catch (Exception ex) { log("[!] 创建父目录失败：" + ex.Message); }
            }

            string mkOut;
            int rc2 = Sh.Cmd("mklink /J \"" + NodeEnv.Link + "\" \"" + target + "\"", out mkOut);
            log("    mklink → rc=" + rc2 + " " + mkOut);
            if (rc2 != 0) { log("[×] 创建联接失败"); return false; }

            string now = NodeEnv.CurrentVer();
            log("[√] 已切换到 " + ver + "（" + NodeEnv.Link + "\\node.exe -v = " + now + "）");
            if (!string.Equals(now, ver, StringComparison.OrdinalIgnoreCase))
            {
                log("[!] 注意：联接读取到的版本是 " + now + "，与预期不符，请检查目标目录");
                return false;
            }
            log("    新开的终端立即生效（PATH 里挂的就是这个联接路径，无需重启）");
            return true;
        }

        // ---- 解压 ----
        // node 官方 zip 内层有 node-vX-win-x64\ 一层，需拍平到 <NVM_HOME>\vX
        static string TarExe()
        {
            string p = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "tar.exe");
            return File.Exists(p) ? p : "tar.exe";
        }

        public static bool ExtractTo(string zip, string destDir, Action<string> log)
        {
            // 临时目录放在目标同盘同层：Directory.Move 才是一次改名（跨盘会变成真拷贝）
            string parentDir = Path.GetDirectoryName(destDir);
            if (string.IsNullOrEmpty(parentDir)) parentDir = NodeEnv.Home;
            string tmp = Path.Combine(parentDir, ".ns-tmp-" + Guid.NewGuid().ToString("N").Substring(0, 8));
            try
            {
                Directory.CreateDirectory(tmp);
            }
            catch (Exception ex) { log("[×] 创建临时目录失败：" + ex.Message); return false; }

            try
            {
                string o;
                int rc = Sh.Run(TarExe(), "-xf \"" + zip + "\" -C \"" + tmp + "\"", out o);
                if (rc != 0)
                {
                    log("[×] 解压失败（rc=" + rc + "）：" + o);
                    // 回退：PowerShell Expand-Archive
                    log("[·] 回退 PowerShell Expand-Archive ...");
                    rc = Sh.Run("powershell.exe",
                        "-NoProfile -NonInteractive -Command \"Expand-Archive -LiteralPath '" + zip +
                        "' -DestinationPath '" + tmp + "' -Force\"", out o);
                    if (rc != 0) { log("[×] 回退解压同样失败：" + o); return false; }
                }

                // 找含 node.exe 的那一层
                string inner = null;
                if (File.Exists(Path.Combine(tmp, "node.exe"))) inner = tmp;
                else
                    foreach (string d in Directory.GetDirectories(tmp))
                        if (File.Exists(Path.Combine(d, "node.exe"))) { inner = d; break; }
                if (inner == null) { log("[×] 解压结果里找不到 node.exe"); return false; }

                if (string.Equals(inner, tmp, StringComparison.OrdinalIgnoreCase))
                {
                    if (Directory.Exists(destDir)) Directory.Delete(destDir, true);
                    Directory.Move(tmp, destDir);
                }
                else
                {
                    if (Directory.Exists(destDir)) Directory.Delete(destDir, true);
                    Directory.Move(inner, destDir);
                    try { Directory.Delete(tmp, true); } catch (Exception) { }
                }
                log("[√] 已解压到 " + destDir);
                return true;
            }
            catch (Exception ex)
            {
                log("[×] 解压异常：" + ex.Message);
                return false;
            }
            finally
            {
                try { if (Directory.Exists(tmp)) Directory.Delete(tmp, true); } catch (Exception) { }
            }
        }

        // ---- 下载 ----
        public static bool Download(string url, string dest, Action<long, long> onProgress,
                                    Func<bool> cancelled, Action<string> log)
        {
            try
            {
                HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
                req.UserAgent = "Mozilla/5.0 NodeSwitcher";
                req.Timeout = 30000;
                req.ReadWriteTimeout = 60000;
                req.AllowAutoRedirect = true;
                using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
                {
                    if (resp.StatusCode != HttpStatusCode.OK)
                    {
                        log("[×] 服务器返回 HTTP " + (int)resp.StatusCode);
                        return false;
                    }
                    long total = resp.ContentLength;
                    using (Stream s = resp.GetResponseStream())
                    using (FileStream fs = new FileStream(dest, FileMode.Create, FileAccess.Write,
                                                          FileShare.None, 65536))
                    {
                        byte[] buf = new byte[65536];
                        long got = 0;
                        long nextTick = 0;
                        while (true)
                        {
                            if (cancelled != null && cancelled()) { log("[!] 已取消下载"); return false; }
                            int n = s.Read(buf, 0, buf.Length);
                            if (n <= 0) break;
                            fs.Write(buf, 0, n);
                            got += n;
                            if (got >= nextTick)
                            {
                                nextTick = got + 262144;   // 每 256KB 回调一次，避免 UI 刷爆
                                if (onProgress != null) onProgress(got, total);
                            }
                        }
                        if (onProgress != null) onProgress(got, total);
                    }
                }
                return true;
            }
            catch (Exception ex)
            {
                log("[×] 下载失败：" + ex.Message);
                return false;
            }
        }

        // ---- 可下载版本清单 ----
        public static List<ReleaseEntry> FetchReleases(string indexUrl, Action<string> log)
        {
            List<ReleaseEntry> res = new List<ReleaseEntry>();
            string url = (indexUrl == null || indexUrl.Trim().Length == 0) ? DEFAULT_INDEX : indexUrl.Trim();
            try
            {
                HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
                req.UserAgent = "Mozilla/5.0 NodeSwitcher";
                req.Timeout = 25000;
                string txt;
                using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
                using (Stream s = resp.GetResponseStream())
                using (StreamReader sr = new StreamReader(s, Encoding.UTF8))
                    txt = sr.ReadToEnd();

                // 逐条抠：{"version":"v26.11.1", ... ,"lts":"Krypton"|false,"security":false}
                MatchCollection ms = Regex.Matches(txt,
                    "\\{[^{}]*\"version\"\\s*:\\s*\"(v[^\"]+)\"[^{}]*\\}");
                foreach (Match m in ms)
                {
                    string body = m.Value;
                    ReleaseEntry r = new ReleaseEntry();
                    r.Ver = m.Groups[1].Value;
                    Match lm = Regex.Match(body, "\"lts\"\\s*:\\s*(false|\"([^\"]*)\")");
                    if (lm.Success && lm.Groups[2].Success) r.Lts = lm.Groups[2].Value;
                    Match sm = Regex.Match(body, "\"security\"\\s*:\\s*(true|false)");
                    r.Security = sm.Success && sm.Groups[1].Value == "true";
                    res.Add(r);
                }
                log("[√] 拉取到 " + res.Count + " 个可下载版本");
            }
            catch (Exception ex)
            {
                log("[×] 拉取版本清单失败：" + ex.Message);
            }
            return res;
        }

        // ---- 卸载（严格前置校验，防误删）----
        public static bool Uninstall(VerEntry v, Action<string> log)
        {
            if (v == null) return false;
            if (v.Current) { log("[×] " + v.Ver + " 是当前激活版本，请先切换到别的版本"); return false; }
            if (Regex.IsMatch(Path.GetFileName(v.Dir), @"^\.ns-tmp-")) { log("[×] 拒绝操作临时目录"); return false; }
            if (!Regex.IsMatch(Path.GetFileName(v.Dir), @"^v\d+\.\d+\.\d+$")) { log("[×] 目录名不符合版本规范，拒绝删除"); return false; }
            string parent;
            try { parent = Directory.GetParent(v.Dir).FullName; }
            catch (Exception) { log("[×] 无法定位父目录"); return false; }
            if (!string.Equals(NodeEnv.Normalize(parent), NodeEnv.Normalize(NodeEnv.Home), StringComparison.OrdinalIgnoreCase))
            {
                log("[×] 目标不在 NVM_HOME 下，拒绝删除：" + v.Dir);
                return false;
            }
            string o;
            int rc = Sh.Cmd("rmdir /S /Q \"" + v.Dir + "\"", out o);
            if (rc != 0 || Directory.Exists(v.Dir))
            {
                log("[×] 删除失败（rc=" + rc + "）：" + o);
                return false;
            }
            log("[√] 已卸载 " + v.Ver);
            return true;
        }
    }

    // ============================ 版本列表控件 ============================
    class VerListBox : ListBox
    {
        public VerListBox()
        {
            DrawMode = DrawMode.OwnerDrawFixed;
            BorderStyle = BorderStyle.None;
            BackColor = Th.Input;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            ItemHeight = 34;
            IntegralHeight = false;
            SelectionMode = SelectionMode.One;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= Items.Count) return;
            VerEntry v = Items[e.Index] as VerEntry;
            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;

            using (SolidBrush bg = new SolidBrush(sel ? Th.Accent : Th.Input))
                e.Graphics.FillRectangle(bg, e.Bounds);
            if (v == null) return;

            int pad = 12;
            Font f = v.Current ? Th.UiB : Th.Ui;
            TextRenderer.DrawText(e.Graphics, v.Ver, f,
                new Point(e.Bounds.X + pad, e.Bounds.Y + (e.Bounds.Height - f.Height) / 2),
                sel ? Color.White : (v.Current ? Th.Ok : Th.Fg));

            string tag;
            Color tc;
            if (v.Current) { tag = "● 当前"; tc = Th.Ok; }
            else { tag = Fmt.Size(v.Bytes); tc = Th.Dim; }
            Size ts = TextRenderer.MeasureText(tag, Th.UiS);
            TextRenderer.DrawText(e.Graphics, tag, Th.UiS,
                new Point(e.Bounds.Right - ts.Width - pad,
                          e.Bounds.Y + (e.Bounds.Height - Th.UiS.Height) / 2),
                sel ? Color.White : tc);
        }
    }

    static class Fmt
    {
        public static string Size(long b)
        {
            if (b <= 0) return "";
            if (b >= 1073741824L) return (b / 1073741824.0).ToString("0.0") + " GB";
            if (b >= 1048576L) return (b / 1048576.0).ToString("0.0") + " MB";
            if (b >= 1024L) return (b / 1024.0).ToString("0") + " KB";
            return b + " B";
        }

        public static string MB(double bytes)
        {
            return (bytes / 1048576.0).ToString("0.0");
        }
    }

    // ============================ 主窗体 ============================
    class MainForm : Form
    {
        const int WIN_W = 980, WIN_H = 720, HEAD_H = 58, GAP = 12, LEFT_W = 318;

        LogView _log = new LogView();
        VerListBox _list = new VerListBox();
        Label _lblVerCount, _lblBadge, _lblSub, _lblHint;
        FlatCombo _cbUse = new FlatCombo();
        FlatCombo _cbDl = new FlatCombo();
        TextBox _txtMirror = new TextBox();
        FlatBtn _btnSwitch, _btnFetch, _btnInstall, _btnOpenPage, _btnRefresh;
        FlatBtn _btnOpenDir, _btnUninstall;
        Panel _barTrack, _barFill;
        Label _lblProgress;
        Label[] _pathLabels = new Label[4];

        Panel _leftPanel, _rightPanel, _logPanel, _header;

        List<VerEntry> _vers = new List<VerEntry>();
        List<ReleaseEntry> _releases = new List<ReleaseEntry>();
        Dictionary<string, string> _cfg;
        volatile bool _busy;
        volatile bool _cancel;

        public MainForm()
        {
            _cfg = Cfg.Load("nodeswitcher");
            NodeEnv.PickedHome = _cfg.ContainsKey("home") ? _cfg["home"] : "";

            Text = "Node.js 版本切换器";
            Ux.Dark(this);
            ClientSize = new Size(WIN_W, WIN_H);
            MinimumSize = new Size(920, 686);
            StartPosition = FormStartPosition.CenterScreen;

            BuildHeader();
            BuildLeft();
            BuildRight();
            BuildLog();
            Resize += delegate { Layout2(); };

            Load += delegate
            {
                Layout2();
                NodeEnv.Detect();
                LogBanner();
                RefreshVersions(true);
                string mirror = _cfg.ContainsKey("mirror") ? _cfg["mirror"] : Ops.DEFAULT_MIRROR;
                _txtMirror.Text = mirror;
            };
        }

        // ---------- 日志 ----------
        void UI(Action a)
        {
            if (IsDisposed) return;
            try
            {
                if (InvokeRequired) BeginInvoke(a);
                else a();
            }
            catch (Exception) { }
        }

        void LogBanner()
        {
            _log.Banner("Node.js 版本切换器");
            _log.Info("版本仓库 NVM_HOME   : " + (NodeEnv.Home.Length > 0 ? NodeEnv.Home : "(未检测到)"));
            _log.Info("当前联接 NVM_SYMLINK: " + NodeEnv.Link);
            _log.Info("权限                : " + (NodeEnv.Admin ? "管理员 ✓" : "普通用户（切换/安装将失败）"));
            if (!NodeEnv.Admin)
                _log.Err("⚠ 非管理员运行：无法重建目录联接，也无法写入 NVM_HOME。请右键「以管理员身份运行」。");
            _log.Dim2("说明：本工具不调用 nvm.exe —— 它写 symlink 时会经 UAC 提权，非交互下静默空转。");
            _log.Dim2("      切换 = 重建目录联接（≡ nvm use）；安装 = 下 zip 解压到 <NVM_HOME>\\vX（≡ nvm install）。");
        }

        // ---------- 顶部 ----------
        void BuildHeader()
        {
            _header = new Panel();
            _header.BackColor = Th.Panel;
            Controls.Add(_header);

            Label t = new Label();
            t.Text = "Node.js 版本切换器";
            t.ForeColor = Th.Fg;
            t.Font = new Font("Microsoft YaHei UI", 12.5F, FontStyle.Bold);
            t.AutoSize = false;
            t.TextAlign = ContentAlignment.MiddleLeft;
            t.BackColor = Color.Transparent;
            t.SetBounds(18, 8, 340, 26);
            _header.Controls.Add(t);

            _lblSub = new Label();
            _lblSub.ForeColor = Th.Dim;
            _lblSub.Font = Th.UiS;
            _lblSub.AutoSize = false;
            _lblSub.TextAlign = ContentAlignment.MiddleLeft;
            _lblSub.BackColor = Color.Transparent;
            _lblSub.SetBounds(20, 34, 620, 18);
            _header.Controls.Add(_lblSub);

            Label badge = new Label();
            badge.Name = "badge";
            badge.ForeColor = Th.Ok;
            badge.Font = Th.UiB;
            badge.AutoSize = false;
            badge.TextAlign = ContentAlignment.MiddleCenter;
            badge.BackColor = Th.Input;
            badge.SetBounds(700, 14, 160, 30);
            _header.Controls.Add(badge);
            _lblBadge = badge;

            _btnRefresh = Ux.Btn("刷新", 872, 13, 90, false);
            _btnRefresh.Click += delegate { RefreshVersions(false); };
            _header.Controls.Add(_btnRefresh);
        }

        // ---------- 左：版本列表 ----------
        void BuildLeft()
        {
            _leftPanel = Card(0, 0, LEFT_W, 100, "已安装版本");
            _lblVerCount = (Label)_leftPanel.Controls[0];
            Controls.Add(_leftPanel);

            _list.SetBounds(10, 38, LEFT_W - 20, 100);
            _list.DoubleClick += delegate { DoSwitch(); };
            _list.SelectedIndexChanged += delegate { UpdatePathLabels(); };
            _leftPanel.Controls.Add(_list);

            _btnOpenDir = Ux.Btn("打开目录", 10, 0, 145, false);
            _btnOpenDir.Click += delegate { OpenSelectedDir(); };
            _leftPanel.Controls.Add(_btnOpenDir);

            _btnUninstall = Ux.Btn("卸载", 163, 0, 145, false);
            _btnUninstall.Click += delegate { DoUninstall(); };
            _leftPanel.Controls.Add(_btnUninstall);

            _lblHint = Ux.Val("双击版本 = 切换；右键可复制版本号", 12, 0, LEFT_W - 24, Th.Dim);
            _lblHint.Font = Th.UiS;
            _leftPanel.Controls.Add(_lblHint);
        }

        // ---------- 右：三张卡 ----------
        void BuildRight()
        {
            _rightPanel = new Panel();
            _rightPanel.BackColor = Th.Bg;
            Controls.Add(_rightPanel);

            // ① 切换
            Panel c1 = Card(0, 0, 100, 118, "① 切换版本");
            _rightPanel.Controls.Add(c1);

            Label l1 = Ux.Lbl("目标版本", 14, 46, 62);
            c1.Controls.Add(l1);
            _cbUse.SetBounds(80, 42, 210, 26);
            c1.Controls.Add(_cbUse);

            _btnSwitch = Ux.Btn("切换到此版本", 304, 40, 130, true);
            _btnSwitch.Click += delegate { DoSwitch(); };
            c1.Controls.Add(_btnSwitch);

            Label l1b = Ux.Val("切换 = 重建目录联接，立即生效且不改动任何已装版本的内容", 14, 82, 560, Th.Dim);
            l1b.Font = Th.UiS;
            c1.Controls.Add(l1b);

            // ② 下载安装
            Panel c2 = Card(0, 0, 100, 176, "② 下载安装");
            _rightPanel.Controls.Add(c2);

            Label l2 = Ux.Lbl("镜像源", 14, 46, 62);
            c2.Controls.Add(l2);
            Panel fm = Ux.Field(_txtMirror, 78, 43, 388, 26);
            c2.Controls.Add(fm);

            _btnFetch = Ux.Btn("获取可用版本", 476, 41, 132, false);
            _btnFetch.Click += delegate { DoFetch(); };
            c2.Controls.Add(_btnFetch);

            Label l3 = Ux.Lbl("可下载", 14, 88, 62);
            c2.Controls.Add(l3);
            _cbDl.SetBounds(78, 84, 240, 26);
            c2.Controls.Add(_cbDl);

            _btnInstall = Ux.Btn("下载并安装", 330, 82, 130, true);
            _btnInstall.Click += delegate { DoInstall(); };
            c2.Controls.Add(_btnInstall);

            _btnOpenPage = Ux.Btn("打开下载页", 472, 82, 136, false);
            _btnOpenPage.Click += delegate { Sys.OpenUrl("https://nodejs.org/en/download/releases"); };
            c2.Controls.Add(_btnOpenPage);

            _barTrack = new Panel();
            _barTrack.BackColor = Th.Input;
            c2.Controls.Add(_barTrack);
            _barFill = new Panel();
            _barFill.BackColor = Th.Accent;
            _barTrack.Controls.Add(_barFill);

            _lblProgress = Ux.Val("", 14, 0, 580, Th.Dim);
            _lblProgress.Font = Th.UiS;
            c2.Controls.Add(_lblProgress);

            // ③ 定位
            Panel c3 = Card(0, 0, 100, 148, "③ 定位 nodejs");
            _rightPanel.Controls.Add(c3);

            string[] names = new string[] { "版本仓库", "当前联接", "版本目录", "可执行文件" };
            for (int i = 0; i < 4; i++)
            {
                int y = 40 + i * 26;
                Label ln = Ux.Lbl(names[i], 14, y, 68);
                ln.Font = Th.UiS;
                c3.Controls.Add(ln);

                Label lv = Ux.Val("", 86, y, 400, Th.Fg);
                lv.Font = Th.UiS;
                c3.Controls.Add(lv);
                _pathLabels[i] = lv;

                int idx = i;
                FlatBtn bo = Ux.Btn("打开", 498, y - 2, 54, false);
                bo.Height = 24;
                bo.Font = Th.UiS;
                bo.Click += delegate { OpenPath(idx); };
                c3.Controls.Add(bo);

                FlatBtn bc = Ux.Btn("复制", 558, y - 2, 54, false);
                bc.Height = 24;
                bc.Font = Th.UiS;
                bc.Click += delegate { CopyPath(idx); };
                c3.Controls.Add(bc);
            }
        }

        // ---------- 日志面板 ----------
        void BuildLog()
        {
            _logPanel = new Panel();
            _logPanel.BackColor = Th.Panel;
            Controls.Add(_logPanel);

            Panel inner = new Panel();
            inner.Padding = new Padding(1);
            inner.BackColor = Th.Border;
            inner.Dock = DockStyle.Fill;
            inner.Controls.Add(_log.Box);
            _logPanel.Controls.Add(inner);

            Label cap = new Label();
            cap.Text = "运行日志";
            cap.ForeColor = Th.Dim;
            cap.Font = Th.UiS;
            cap.AutoSize = true;
            cap.BackColor = Color.Transparent;
            cap.SetBounds(10, 3, 120, 16);
            cap.BringToFront();
            _logPanel.Controls.Add(cap);
            inner.SetBounds(0, 20, 100, 100);
            _logTop = 20;
        }

        int _logTop = 20;

        static Panel Card(int x, int y, int w, int h, string title)
        {
            Panel p = new Panel();
            p.SetBounds(x, y, w, h);
            p.BackColor = Th.Panel;
            Label t = new Label();
            t.Text = title;
            t.ForeColor = Th.Fg;
            t.Font = Th.UiB;
            t.AutoSize = false;
            t.SetBounds(14, 10, 400, 20);
            t.TextAlign = ContentAlignment.MiddleLeft;
            t.BackColor = Color.Transparent;
            p.Controls.Add(t);
            return p;
        }

        // ---------- 布局 ----------
        void Layout2()
        {
            int w = ClientSize.Width, h = ClientSize.Height;
            // 三张卡的最小高度预算：不够时优先压缩日志区，保证卡片内控件不被裁切
            const int CARD1 = 108, CARD2 = 166, CARD3 = 150;
            int needBody = CARD1 + CARD2 + CARD3 + GAP * 2;      // 448
            int logH = h - HEAD_H - GAP * 3 - needBody;
            if (logH < 120) logH = 120;

            int bodyTop = HEAD_H + GAP;
            int bodyH = h - HEAD_H - GAP - logH - GAP * 2;
            if (bodyH < needBody) bodyH = needBody;

            int leftW = LEFT_W;
            int rightX = GAP * 2 + leftW;
            int rightW = w - leftW - GAP * 3;
            if (rightW < 520) rightW = 520;

            _header.SetBounds(0, 0, w, HEAD_H);
            _lblSub.Text = "版本仓库 " + (NodeEnv.Home.Length > 0 ? NodeEnv.Home : "(未检测到)") +
                           "    ·    联接 " + NodeEnv.Link;
            _btnRefresh.Left = w - 102;
            _lblBadge.Left = w - 102 - 8 - 160;

            _leftPanel.SetBounds(GAP, bodyTop, leftW, bodyH);
            _rightPanel.SetBounds(rightX, bodyTop, rightW, bodyH);
            _logPanel.SetBounds(GAP, h - logH - GAP, w - GAP * 2, logH);

            // 左内
            _list.SetBounds(10, 38, leftW - 20, bodyH - 38 - 78);
            _btnOpenDir.Top = bodyH - 44;
            _btnUninstall.Top = bodyH - 44;
            _lblHint.Top = bodyH - 20;

            // 右内：三卡纵向排列，间距各一个 GAP
            Panel c1 = _rightPanel.Controls[0] as Panel;
            Panel c2 = _rightPanel.Controls[1] as Panel;
            Panel c3 = _rightPanel.Controls[2] as Panel;
            c1.SetBounds(0, 0, rightW, CARD1);
            c2.SetBounds(0, CARD1 + GAP, rightW, CARD2);
            c3.SetBounds(0, CARD1 + GAP + CARD2 + GAP, rightW, CARD3);

            // 定位卡随宽度重排按钮
            foreach (Control cc in c3.Controls)
            {
                if (!(cc is FlatBtn)) continue;
                FlatBtn b = (FlatBtn)cc;
                if (b.Text == "打开") b.Left = rightW - 122;
                else if (b.Text == "复制") b.Left = rightW - 62;
            }
            for (int i = 0; i < _pathLabels.Length; i++)
                _pathLabels[i].Width = rightW - 200;

            // 进度条
            _barTrack.SetBounds(14, 126, rightW - 32, 8);
            _barFill.Height = 8;
            _lblProgress.SetBounds(14, 140, rightW - 32, 18);
            LayoutProgress();

            // 下载卡的控件随宽度拉伸
            _btnFetch.Left = rightW - 148;
            _btnFetch.Width = 134;
            fm_Width(rightW);
            _btnOpenPage.Left = rightW - 148;
            _btnOpenPage.Width = 136;
            _btnInstall.Left = rightW - 292;
            _lblProgress.Width = rightW - 32;

            if (_logPanel.Controls.Count > 0)
            {
                Control inner = _logPanel.Controls[0];
                inner.SetBounds(0, _logTop, _logPanel.Width, _logPanel.Height - _logTop);
            }
        }

        Panel _mirrorField;
        void fm_Width(int rightW)
        {
            if (_mirrorField == null)
            {
                Panel c2 = _rightPanel.Controls[1] as Panel;
                foreach (Control c in c2.Controls)
                    if (c is Panel && c != _barTrack && c.Left == 78) _mirrorField = (Panel)c;
            }
            if (_mirrorField != null) _mirrorField.Width = rightW - 148 - 78 - 8;
        }

        void LayoutProgress()
        {
            if (_barTrack == null || _barFill == null) return;
            _barFill.Height = _barTrack.Height;
            if (_barFill.Tag == null) _barFill.Width = 0;
        }

        void SetProgress(long got, long total)
        {
            UI(delegate
            {
                if (total > 0)
                {
                    int w = (int)(_barTrack.Width * (double)got / total);
                    _barFill.Width = Math.Max(0, Math.Min(_barTrack.Width, w));
                    _lblProgress.Text = string.Format("下载中 {0} / {1} MB  ({2}%)",
                        Fmt.MB(got), Fmt.MB(total), (int)(100.0 * got / total));
                }
                else
                {
                    _barFill.Width = 0;
                    _lblProgress.Text = "下载中 " + Fmt.MB(got) + " MB ...";
                }
            });
        }

        // ---------- 刷新版本 ----------
        void RefreshVersions(bool first)
        {
            _vers = NodeEnv.Scan();
            _list.BeginUpdate();
            _list.Items.Clear();
            foreach (VerEntry v in _vers) _list.Items.Add(v);
            _list.EndUpdate();

            string cur = NodeEnv.CurrentVer();
            _lblVerCount.Text = "已安装版本 (" + _vers.Count + ")";
            _lblBadge.Text = cur.Length > 0 ? "当前 " + cur : "未检测到";

            _cbUse.Items.Clear();
            foreach (VerEntry v in _vers) _cbUse.Items.Add(v);
            // 默认选中当前版本
            for (int i = 0; i < _cbUse.Items.Count; i++)
            {
                VerEntry v = _cbUse.Items[i] as VerEntry;
                if (v != null && v.Current) { _cbUse.SelectedIndex = i; break; }
            }
            if (_cbUse.SelectedIndex < 0 && _cbUse.Items.Count > 0) _cbUse.SelectedIndex = 0;

            for (int i = 0; i < _list.Items.Count; i++)
            {
                VerEntry v = _list.Items[i] as VerEntry;
                if (v != null && v.Current) { _list.SelectedIndex = i; break; }
            }
            if (_list.SelectedIndex < 0 && _list.Items.Count > 0) _list.SelectedIndex = 0;

            UpdatePathLabels();
            if (!first)
                _log.Ok("已刷新：" + _vers.Count + " 个版本，当前 " + (cur.Length > 0 ? cur : "?"));
            _log.Dim2("    " + string.Join("  ", _vers.Select(delegate(VerEntry v) { return v.Ver; }).ToArray()));
        }

        void UpdatePathLabels()
        {
            string selDir = "";
            if (_list.SelectedItem is VerEntry) selDir = ((VerEntry)_list.SelectedItem).Dir;
            _pathLabels[0].Text = NodeEnv.Home;
            _pathLabels[1].Text = NodeEnv.Link;
            _pathLabels[2].Text = selDir;
            _pathLabels[3].Text = Path.Combine(NodeEnv.Link, "node.exe");
        }

        void OpenPath(int i)
        {
            string p = _pathLabels[i].Text;
            if (string.IsNullOrEmpty(p)) return;
            if (i == 3) Sys.OpenFolder(Path.GetDirectoryName(p));
            else Sys.OpenFolder(p);
        }

        void CopyPath(int i)
        {
            string p = _pathLabels[i].Text;
            if (string.IsNullOrEmpty(p)) return;
            try { Clipboard.SetText(p); _log.Ok("已复制：" + p); }
            catch (Exception ex) { _log.Err("复制失败：" + ex.Message); }
        }

        void OpenSelectedDir()
        {
            VerEntry v = _list.SelectedItem as VerEntry;
            if (v == null) { _list.SelectedIndex = 0; v = _list.SelectedItem as VerEntry; }
            if (v == null) { _log.Warn("没有可打开的版本目录"); return; }
            Sys.OpenFolder(v.Dir);
        }

        // ---------- 切换 ----------
        void DoSwitch()
        {
            if (_busy) { _log.Warn("有任务正在进行，请稍候"); return; }
            VerEntry v = null;
            // 来源：左列表双击优先，其次下拉
            if (_list.Focused && _list.SelectedItem is VerEntry) v = (VerEntry)_list.SelectedItem;
            if (v == null && _cbUse.SelectedItem is VerEntry) v = (VerEntry)_cbUse.SelectedItem;
            if (v == null) { _log.Warn("请先选择目标版本"); return; }
            if (v.Current) { _log.Warn(v.Ver + " 已经是当前版本"); return; }

            string cur = NodeEnv.CurrentVer();
            DialogResult r = MessageBox.Show(
                "将当前 Node.js 从 " + (cur.Length > 0 ? cur : "?") + " 切换到 " + v.Ver + "？\n\n" +
                "做法：重建目录联接 " + NodeEnv.Link + " → " + v.Dir + "\n" +
                "新开的终端立即生效，已装版本的内容不会被改动。",
                "切换 Node.js 版本", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
            if (r != DialogResult.OK) return;

            _busy = true;
            _log.Banner("切换 → " + v.Ver);
            bool ok = Ops.Use(v.Ver, delegate(string s) { _log.Info(s); });
            _busy = false;
            if (ok) { RefreshVersions(false); }
            else
            {
                _log.Err("切换失败。若提示权限不足，请右键「以管理员身份运行」本工具。");
                MessageBox.Show("切换失败，详见日志。", "失败", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        // ---------- 卸载 ----------
        void DoUninstall()
        {
            if (_busy) return;
            VerEntry v = _list.SelectedItem as VerEntry;
            if (v == null) { _log.Warn("请先选择要卸载的版本"); return; }
            if (v.Current) { _log.Warn(v.Ver + " 是当前激活版本，先切换到别的版本再卸载"); return; }
            DialogResult r = MessageBox.Show(
                "确定卸载 " + v.Ver + " 吗？\n\n将删除目录：\n" + v.Dir +
                "\n\n此操作不可恢复。",
                "卸载版本", MessageBoxButtons.OKCancel, MessageBoxIcon.Warning);
            if (r != DialogResult.OK) return;
            _busy = true;
            _log.Banner("卸载 " + v.Ver);
            bool ok = Ops.Uninstall(v, delegate(string s) { _log.Info(s); });
            _busy = false;
            if (ok) RefreshVersions(false);
            else MessageBox.Show("卸载失败，详见日志。", "失败", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }

        // ---------- 拉取清单 ----------
        void DoFetch()
        {
            if (_busy) { _log.Warn("有任务正在进行"); return; }
            _busy = true;
            _btnFetch.Enabled = false;
            _log.Banner("拉取可下载版本");
            string mirror = _txtMirror.Text.Trim();
            string indexUrl = mirror.TrimEnd('/') + "/index.json";
            _log.Info("清单地址 " + indexUrl);
            _releases = Ops.FetchReleases(indexUrl, delegate(string s) { _log.Info(s); });
            _busy = false;
            _btnFetch.Enabled = true;
            if (_releases.Count == 0) { _log.Err("没拉到版本，检查镜像源或网络"); return; }

            _cbDl.Items.Clear();
            // 先 LTS 再其它，各取前 60 条，避免下拉过长
            List<ReleaseEntry> shown = new List<ReleaseEntry>();
            int n = 0;
            foreach (ReleaseEntry r in _releases)
            {
                if (r.Lts != null && r.Lts.Length > 0) { shown.Add(r); n++; if (n >= 40) break; }
            }
            n = 0;
            foreach (ReleaseEntry r in _releases)
            {
                if (r.Lts == null || r.Lts.Length == 0) { shown.Add(r); n++; if (n >= 25) break; }
            }
            foreach (ReleaseEntry r in shown) _cbDl.Items.Add(r);
            if (_cbDl.Items.Count > 0) _cbDl.SelectedIndex = 0;
            _log.Ok("可选 " + _cbDl.Items.Count + " 项（LTS 优先，其次最新若干）");
        }

        // ---------- 安装 ----------
        void DoInstall()
        {
            if (_busy) { _log.Warn("有任务正在进行"); return; }
            ReleaseEntry r = _cbDl.SelectedItem as ReleaseEntry;
            string ver = r != null ? r.Ver : _cbDl.Text.Trim();
            ver = Ops.NormVer(ver);
            if (!Ops.ValidVer(ver)) { _log.Warn("请先选择要安装的版本（点「获取可用版本」）"); return; }

            string dest = Path.Combine(NodeEnv.Home, ver);
            if (File.Exists(Path.Combine(dest, "node.exe")))
            {
                _log.Warn(ver + " 已经安装过了，可直接在左侧双击切换");
                return;
            }
            if (NodeEnv.Home.Length == 0) { _log.Err("未检测到 NVM_HOME，无法确定安装位置"); return; }
            if (!NodeEnv.Admin) { _log.Err("需要管理员权限写入 " + NodeEnv.Home); return; }

            string url = Ops.ZipUrl(_txtMirror.Text, ver);
            DialogResult q = MessageBox.Show(
                "将下载并安装 Node.js " + ver + "：\n\n" + url + "\n\n安装位置：" + dest +
                "\n\n（约 35 MB，安装不会自动切换，装完可在左侧双击切换）",
                "下载并安装", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
            if (q != DialogResult.OK) return;

            _cfg["mirror"] = _txtMirror.Text.Trim();
            Cfg.Save("nodeswitcher", _cfg);

            _busy = true;
            _cancel = false;
            _btnInstall.Enabled = false;
            _barFill.Width = 0;
            _log.Banner("安装 " + ver);

            string tmpZip = Path.Combine(Path.GetTempPath(), "node-" + ver + "-win-x64.zip");
            Action work = delegate
            {
                bool ok = Ops.Download(url, tmpZip,
                    delegate(long got, long total) { SetProgress(got, total); },
                    delegate { return _cancel; },
                    delegate(string s) { _log.Info(s); });

                if (ok)
                {
                    UI(delegate { _lblProgress.Text = "下载完成，正在解压 ..."; });
                    _log.Ok("下载完成 " + Fmt.MB(new FileInfo(tmpZip).Length) + " MB → " + tmpZip);
                    ok = Ops.ExtractTo(tmpZip, dest, delegate(string s) { _log.Info(s); });
                }

                UI(delegate
                {
                    _btnInstall.Enabled = true;
                    _busy = false;
                    _barFill.Width = 0;
                    if (ok)
                    {
                        _lblProgress.Text = "安装完成";
                        _log.Ok("已安装 " + ver + "，可在左侧双击切换");
                        RefreshVersions(false);
                        MessageBox.Show("Node.js " + ver + " 安装完成。\n\n" +
                            "位置：" + dest + "\n现在可以在左侧双击它切换过去。",
                            "安装完成", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    }
                    else
                    {
                        _lblProgress.Text = "安装失败，详见日志";
                        MessageBox.Show("安装失败，详见日志。", "失败", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    }
                });
                try { if (File.Exists(tmpZip)) File.Delete(tmpZip); } catch (Exception) { }
            };
            new Thread(new ThreadStart(work)) { IsBackground = true }.Start();
        }

        // 供 --uicheck 自检：报告真实构建后的控件与布局状态
        public string Diag()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("client=" + ClientSize.Width + "x" + ClientSize.Height);
            sb.AppendLine("home=" + NodeEnv.Home);
            sb.AppendLine("link=" + NodeEnv.Link);
            sb.AppendLine("admin=" + NodeEnv.Admin);
            sb.AppendLine("list_items=" + _list.Items.Count);
            sb.AppendLine("list_selected=" + _list.SelectedIndex);
            sb.AppendLine("combo_use_items=" + _cbUse.Items.Count);
            sb.AppendLine("combo_use_selected=" + _cbUse.SelectedIndex);
            sb.AppendLine("badge=" + _lblBadge.Text);
            sb.AppendLine("ver_count_label=" + _lblVerCount.Text);
            sb.AppendLine("header=" + _header.Bounds);
            sb.AppendLine("leftPanel=" + _leftPanel.Bounds);
            sb.AppendLine("rightPanel=" + _rightPanel.Bounds);
            sb.AppendLine("logPanel=" + _logPanel.Bounds);
            sb.AppendLine("card1=" + _rightPanel.Controls[0].Bounds);
            sb.AppendLine("card2=" + _rightPanel.Controls[1].Bounds);
            sb.AppendLine("card3=" + _rightPanel.Controls[2].Bounds);
            sb.AppendLine("list=" + _list.Bounds);
            sb.AppendLine("barTrack=" + _barTrack.Bounds);
            sb.AppendLine("path0=" + _pathLabels[0].Text);
            sb.AppendLine("path1=" + _pathLabels[1].Text);
            sb.AppendLine("path2=" + _pathLabels[2].Text);
            sb.AppendLine("path3=" + _pathLabels[3].Text);
            sb.AppendLine("log_lines=" + _log.Box.Lines.Length);
            if (_log.Box.Lines.Length > 0) sb.AppendLine("log_head=" + _log.Box.Lines[0]);
            if (_log.Box.Lines.Length > 1) sb.AppendLine("log_2=" + _log.Box.Lines[1]);
            sb.AppendLine("min_size=" + MinimumSize.Width + "x" + MinimumSize.Height);
            return sb.ToString();
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            _cancel = true;
            base.OnFormClosing(e);
        }
    }

    // ============================ 命令行自检 ============================
    static class Cli
    {
        static void Write(string path, string text)
        {
            try { File.WriteAllText(path, text, new UTF8Encoding(false)); }
            catch (Exception) { }
        }

        public static bool TryHandle(string[] args)
        {
            if (args.Length < 2) return false;
            string cmd = args[0];

            if (cmd == "--scan")
            {
                NodeEnv.Detect();
                List<VerEntry> vs = NodeEnv.Scan();
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("home=" + NodeEnv.Home);
                sb.AppendLine("link=" + NodeEnv.Link);
                sb.AppendLine("admin=" + NodeEnv.Admin);
                sb.AppendLine("link_target=" + NodeEnv.LinkTarget());
                sb.AppendLine("current=" + NodeEnv.CurrentVer());
                sb.AppendLine("npm=" + NodeEnv.NpmVer());
                sb.AppendLine("count=" + vs.Count);
                foreach (VerEntry v in vs)
                    sb.AppendLine(v.Ver + "|" + v.Dir + "|" + (v.Current ? "1" : "0") + "|" + v.Bytes);
                Write(args[1], sb.ToString());
                return true;
            }

            if (cmd == "--use" && args.Length >= 3)
            {
                NodeEnv.Detect();
                StringBuilder sb = new StringBuilder();
                bool ok = Ops.Use(args[1], delegate(string s) { sb.AppendLine(s); });
                sb.AppendLine("RESULT=" + (ok ? "OK" : "FAIL"));
                sb.AppendLine("CURRENT=" + NodeEnv.CurrentVer());
                Write(args[2], sb.ToString());
                return true;
            }

            if (cmd == "--releases" && args.Length >= 3)
            {
                List<ReleaseEntry> rs = Ops.FetchReleases(args[1], delegate(string s) { });
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("count=" + rs.Count);
                int lts = 0;
                foreach (ReleaseEntry r in rs) if (r.Lts != null && r.Lts.Length > 0) lts++;
                sb.AppendLine("lts_count=" + lts);
                // 与 UI 下拉同一套排序：LTS 优先，其次最新若干
                List<ReleaseEntry> shown = new List<ReleaseEntry>();
                int n = 0;
                foreach (ReleaseEntry r in rs)
                    if (r.Lts != null && r.Lts.Length > 0) { shown.Add(r); if (++n >= 20) break; }
                n = 0;
                foreach (ReleaseEntry r in rs)
                    if (r.Lts == null || r.Lts.Length == 0) { shown.Add(r); if (++n >= 10) break; }
                foreach (ReleaseEntry r in shown)
                    sb.AppendLine(r.Ver + "|" + (r.Lts == null ? "" : r.Lts) + "|" + r.Security);
                Write(args[2], sb.ToString());
                return true;
            }

            if (cmd == "--install" && args.Length >= 4)
            {
                // 用于自动化验证：把安装到 <homeDir>（可为临时目录），不动真实 NVM_HOME
                NodeEnv.Detect();
                if (!string.IsNullOrEmpty(args[2])) NodeEnv.Home = NodeEnv.Normalize(args[2]);
                string ver = Ops.NormVer(args[1]);
                string dest = Path.Combine(NodeEnv.Home, ver);
                StringBuilder sb = new StringBuilder();
                string url = Ops.ZipUrl(null, ver);
                sb.AppendLine("url=" + url);
                sb.AppendLine("dest=" + dest);
                string zip = Path.Combine(Path.GetTempPath(), "ns-inst-" + ver + ".zip");
                bool ok = Ops.Download(url, zip, null, null, delegate(string s) { sb.AppendLine(s); });
                sb.AppendLine("download=" + ok);
                if (ok)
                {
                    try { sb.AppendLine("zip_bytes=" + new FileInfo(zip).Length); } catch (Exception) { }
                    ok = Ops.ExtractTo(zip, dest, delegate(string s) { sb.AppendLine(s); });
                    sb.AppendLine("extract=" + ok);
                    string exe = Path.Combine(dest, "node.exe");
                    sb.AppendLine("node_exe=" + File.Exists(exe));
                    if (File.Exists(exe)) sb.AppendLine("node_v=" + Sh.Capture(exe, "-v"));
                    string npm = Path.Combine(dest, "npm.cmd");
                    sb.AppendLine("npm_cmd=" + File.Exists(npm));
                }
                try { if (File.Exists(zip)) File.Delete(zip); } catch (Exception) { }
                Write(args[3], sb.ToString());
                return true;
            }

            if (cmd == "--uicheck" && args.Length >= 2)
            {
                // 无头验证 UI：真建窗体、真跑 Load 与布局，2.2 秒后把诊断写盘再退出
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                MainForm f = new MainForm();
                f.Opacity = 0.0;              // 不打扰用户（控件照常创建）
                f.ShowInTaskbar = false;
                System.Windows.Forms.Timer t = new System.Windows.Forms.Timer();
                t.Interval = 2200;
                t.Tick += delegate(object s, EventArgs e)
                {
                    try { Write(args[1], f.Diag()); }
                    catch (Exception ex) { Write(args[1], "DIAG_ERROR " + ex); }
                    t.Stop();
                    Application.Exit();
                };
                t.Start();
                Application.Run(f);
                return true;
            }

            if (cmd == "--url" && args.Length >= 4)
            {
                Write(args[3], Ops.ZipUrl(args[1], Ops.NormVer(args[2])) + "\r\n");
                return true;
            }

            return false;
        }
    }

    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            try { ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072; }
            catch (Exception) { }
            if (args.Length > 0 && Cli.TryHandle(args)) return;
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
