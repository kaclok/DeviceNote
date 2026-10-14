// ============================================================================
//  NVM 版本管理器  ——  nvm-desk 的修复重写版
//
//  原项目 https://gitee.com/code-ape-hc/nvm-desk 是一份 PowerShell + WinForms
//  脚本（nvm_manager.ps1，25072 B），存在下列缺陷，本文件逐条修掉：
//
//  ① 第 7 行 `$NvmRoot = "D:\Program Files\Roaming\nvm"` —— 硬编码，别人机器上
//     该目录根本不存在，于是「已安装列表」永远空白。
//     → 修：NvmEnv.DetectRoot() 按「用户配置 → NVM_HOME 环境变量 → 注册表
//       HKCU/HKLM 的 Environment → 常见位置兜底 → 从 nvm.exe 反推」逐级探测，
//       并且窗体上给用户一个可编辑的路径框。
//
//  ② 第 21 行 `Sort-Object { [Version]($_.TrimStart("v")) } -Descending` ——
//     只要目录里混进 "22.14.0-rc.1" 这种非规范版本名，[Version] 强转就抛异常，
//     一条抛错导致整份排序失败（列表又是空的）。
//     → 修：自己写 Ver.Cmp() 逐段比较，非数字段降级为字符串比较，永不抛。
//
//  ③ 第 43-51 行 从 `<NvmRoot>\settings.txt` 里正则抠 node_mirror，抠不到就
//     静默退回 nodejs.org —— 国内网络下 index.json 拉几十秒，看起来就是「卡」。
//     → 修：镜像源作为一等配置项，默认 cdn.npmmirror.com，可在窗体上改。
//
//  ④ 第 52 行 `Invoke-RestMethod -TimeoutSec 15` 在 Build-UI 里同步执行 ——
//     拉网络期间整个窗口不响应，标签页切不动。
//     → 修：后台线程 + HttpWebRequest + 100ms 轮询回主线程，全程 UI 不冻。
//
//  ⑤ 第 175/190/312/348/473 五处 `$Global:MainForm.Controls.Clear()` 之后
//     全量重建所有控件 —— 每次切换/安装都闪一下，且控件重建期间事件丢失。
//     → 修：一次性建好所有控件，用 Visible / 数据刷新驱动，绝不 Clear。
//
//  ⑥ 第 518 行 `$form.Add_Resize({ $Global:MainForm.Refresh() })` —— 拖拽窗口
//     时每帧全窗重绘，拖起来一顿一顿。
//     → 修：改用双缓冲 + 标准布局锚定，Resize 里不做任何重绘。
//
//  ⑦ 第 305/341/457 行 `Start-Process -FilePath "nvm" ... -Verb RunAs`
//     依赖 PATH 里恰好有 nvm.exe，且每次操作都弹 UAC。
//     → 修：manifest 里 requireAdministrator 一次提权；nvm.exe 路径从 NvmHome
//       里直接定位，不走 PATH。
//
//  保留原项目的信息架构：左「已安装」右「在线」两个标签，同一套深色主题。
// ============================================================================

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net;
using System.Reflection;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;

// Shared.cs 里的 Th / FlatBtn / FlatCombo / LogView / Ux / Sys / Sh / Cfg 全在
// DevLaunch 命名空间下，所以本文件也挂同一命名空间，直接复用。
namespace DevLaunch
{
    // ========================================================================
    // 版本号：解析 / 比较 / 排序
    // 原项目用 [Version] 强转，遇到 "22.14.0-rc.1"、"v18.19.0-nightly..." 直接抛。
    // 这里全部自己算，永远不抛异常。
    // ========================================================================
    static class Ver
    {
        // "v20.2.0" -> "20.2.0"；顺手去掉 LTS 后缀之类的杂字符
        public static string Norm(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            s = s.Trim();
            if (s.Length > 0 && (s[0] == 'v' || s[0] == 'V')) s = s.Substring(1);
            return s;
        }

        // 拆成可比较段：数字段按数值比，非数字段按字符串比
        // 返回 false 表示这不是一个规范的 x.y.z 版本号
        public static bool Parse(string s, out int[] nums, out string tail)
        {
            nums = new int[0];
            tail = "";
            s = Norm(s);
            if (s.Length == 0) return false;

            // 主版本段：连续的数字.数字
            int i = 0;
            List<int> got = new List<int>();
            while (i < s.Length)
            {
                int j = i;
                while (j < s.Length && char.IsDigit(s[j])) j++;
                if (j == i) break;                       // 起手就不是数字
                int n;
                if (!int.TryParse(s.Substring(i, j - i), out n)) n = 0;
                got.Add(n);
                i = j;
                if (i < s.Length && s[i] == '.') { i++; continue; }
                break;
            }
            if (got.Count == 0) return false;
            nums = got.ToArray();
            tail = i < s.Length ? s.Substring(i) : "";    // 例如 "-rc.1"
            return true;
        }

        // 版本比较：大的排前面（Descending 用）
        // 规范版本 > 预览版；同为预览版时比较整个尾巴字符串
        public static int Cmp(string a, string b)
        {
            int[] na, nb;
            string ta, tb;
            bool oka = Parse(a, out na, out ta);
            bool okb = Parse(b, out nb, out tb);

            if (!oka && !okb) return string.Compare(a, b, StringComparison.OrdinalIgnoreCase);
            if (!oka) return -1;                          // 非版本号沉底
            if (!okb) return 1;

            int len = na.Length > nb.Length ? na.Length : nb.Length;
            for (int i = 0; i < len; i++)
            {
                int x = i < na.Length ? na[i] : 0;
                int y = i < nb.Length ? nb[i] : 0;
                if (x != y) return x > y ? 1 : -1;
            }
            // 主体相同看尾巴：空尾巴（正式版）> 有尾巴（预览版）
            if (ta.Length == 0 && tb.Length == 0) return 0;
            if (ta.Length == 0) return 1;
            if (tb.Length == 0) return -1;
            return string.Compare(ta, tb, StringComparison.OrdinalIgnoreCase);
        }

        public static string Trim(string s, int max)
        {
            if (s == null) return "";
            s = Norm(s);
            return s.Length <= max ? s : s.Substring(0, max);
        }
    }

    // ========================================================================
    // 环境探测：NVM_HOME / NVM_SYMLINK / 镜像源
    // ========================================================================
    static class NvmEnv
    {
        public const string DEFAULT_MIRROR = "https://cdn.npmmirror.com/binaries/node";
        public const string OFFICIAL_MIRROR = "https://nodejs.org/dist";

        // 起手就把 TLS1.2 打开：.NET 4.0 默认只认 TLS1.0，
        // 走 https 的镜像站会直接抛「基础连接已关闭」。
        static NvmEnv()
        {
            try
            {
                ServicePointManager.SecurityProtocol =
                    (SecurityProtocolType)3072 | (SecurityProtocolType)768 | SecurityProtocolType.Tls;
            }
            catch (Exception) { }
            try { ServicePointManager.DefaultConnectionLimit = 8; } catch (Exception) { }
        }

        // ---------- 注册表读环境变量（用户级 + 机器级）----------
        public static string RegEnv(string name)
        {
            string[] keys = new string[]
            {
                @"HKEY_CURRENT_USER\Environment",
                @"HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment",
            };
            foreach (string k in keys)
            {
                try
                {
                    object v = Microsoft.Win32.Registry.GetValue(k, name, null);
                    string s = v as string;
                    if (!string.IsNullOrEmpty(s)) return Environment.ExpandEnvironmentVariables(s).TrimEnd('\\');
                }
                catch (Exception) { }
            }
            return null;
        }

        static string Env(string name)
        {
            try
            {
                string v = Environment.GetEnvironmentVariable(name);
                if (!string.IsNullOrEmpty(v)) return Environment.ExpandEnvironmentVariables(v).TrimEnd('\\');
            }
            catch (Exception) { }
            return null;
        }

        // ---------- 从 settings.txt 读 root/path/mirror ----------
        public static Dictionary<string, string> ReadSettings(string nvmDir)
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            if (string.IsNullOrEmpty(nvmDir)) return d;
            try
            {
                string f = Path.Combine(nvmDir, "settings.txt");
                if (!File.Exists(f)) return d;
                foreach (string raw in File.ReadAllLines(f, Encoding.UTF8))
                {
                    string line = raw.Trim();
                    if (line.Length == 0) continue;
                    Match m = Regex.Match(line, @"^(root|path|node_mirror|npm_mirror)\s+(.+)$",
                                          RegexOptions.IgnoreCase);
                    if (m.Success) d[m.Groups[1].Value.ToLowerInvariant()] = m.Groups[2].Value.Trim();
                }
            }
            catch (Exception) { }
            return d;
        }

        // ---------- 探测 nvm 安装根目录 ----------
        // 顺序：显式配置 → NVM_HOME 环境变量 → 注册表 → settings.txt 反推
        //      → 从 nvm.exe 位置反推 → 常见位置候选
        public static List<string> Candidates(string explicitHome)
        {
            List<string> list = new List<string>();
            Action<string> add = delegate(string p)
            {
                if (string.IsNullOrEmpty(p)) return;
                p = p.Trim().Trim('"').TrimEnd('\\');
                if (p.Length == 0) return;
                foreach (string x in list)
                    if (string.Equals(x, p, StringComparison.OrdinalIgnoreCase)) return;
                list.Add(p);
            };

            add(explicitHome);
            add(Env("NVM_HOME"));
            add(RegEnv("NVM_HOME"));

            // 找 nvm.exe 落在哪，它所在的目录就是 NVM_HOME
            string nvmExe = FindNvmExe(explicitHome);
            if (nvmExe != null) add(Path.GetDirectoryName(nvmExe));

            // 常见位置
            add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "nvm"));
            add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "nvm"));
            add(@"C:\nvm");
            add(@"D:\nvm");
            add(@"E:\NVM\nvm");
            add(@"E:\nvm");
            string sysDrive = Path.GetPathRoot(Environment.SystemDirectory);
            add(Path.Combine(sysDrive, "nvm"));
            add(Path.Combine(sysDrive, @"Program Files\nvm"));
            return list;
        }

        // 在候选目录里找 nvm.exe（只看顶层，不递归）
        public static string FindNvmExe(string explicitHome)
        {
            List<string> dirs = new List<string>();
            Action<string> add = delegate(string p)
            {
                if (string.IsNullOrEmpty(p)) return;
                p = p.Trim().Trim('"').TrimEnd('\\');
                if (p.Length > 0) dirs.Add(p);
            };
            add(explicitHome);
            add(Env("NVM_HOME"));
            add(RegEnv("NVM_HOME"));
            foreach (string d in dirs)
            {
                try
                {
                    foreach (string ext in new string[] { ".exe", ".cmd", ".bat" })
                    {
                        string f = Path.Combine(d, "nvm" + ext);
                        if (File.Exists(f)) return f;
                    }
                }
                catch (Exception) { }
            }
            return null;
        }

        // 判定一个目录「像不像」NVM_HOME：
        // 有 nvm.exe / nvm.cmd，或至少有一个 vX.Y.Z 版本目录
        public static bool LooksLikeNvm(string dir)
        {
            if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir)) return false;
            try
            {
                if (File.Exists(Path.Combine(dir, "nvm.exe"))) return true;
                if (File.Exists(Path.Combine(dir, "nvm.cmd"))) return true;
                if (File.Exists(Path.Combine(dir, "settings.txt"))) return true;
                foreach (string d in Directory.GetDirectories(dir))
                {
                    string n = Path.GetFileName(d);
                    int[] nums; string tail;
                    if (n.Length > 1 && (n[0] == 'v' || n[0] == 'V') && Ver.Parse(n, out nums, out tail))
                        return true;
                }
            }
            catch (Exception) { }
            return false;
        }

        // ---------- 探测 nodejs 联接（NVM_SYMLINK）----------
        public static string DetectLink(string nvmDir)
        {
            string v = Env("NVM_SYMLINK");
            if (!string.IsNullOrEmpty(v)) return v;
            v = RegEnv("NVM_SYMLINK");
            if (!string.IsNullOrEmpty(v)) return v;

            Dictionary<string, string> st = ReadSettings(nvmDir);
            if (st.ContainsKey("path") && st["path"].Length > 0)
                return st["path"].TrimEnd('\\');

            // 没配就走默认：<NVM_HOME 的同级>\nodejs
            if (!string.IsNullOrEmpty(nvmDir))
            {
                DirectoryInfo p = Directory.GetParent(nvmDir);
                if (p != null) return Path.Combine(p.FullName, "nodejs");
            }
            return null;
        }

        // 目录联接 / 符号联接指向哪里（null = 不是联接，或不存在）
        //
        // .NET 4.0 的 DirectoryInfo 没有 LinkTarget 属性（那是 .NET 6+ 的），
        // 所以走 cmd 的 `dir /al <父目录>`，它会把联接显示成：
        //     2026/10/14  08:16    <JUNCTION>     nodejs [E:\NVM\nvm\v22.14.0]
        // 方括号里就是目标，且这段格式与系统语言无关，解析最稳。
        public static string LinkTarget(string link)
        {
            if (string.IsNullOrEmpty(link) || !Directory.Exists(link)) return null;
            try
            {
                DirectoryInfo di = new DirectoryInfo(link);
                if ((di.Attributes & FileAttributes.ReparsePoint) == 0) return null;
            }
            catch (Exception) { return null; }

            try
            {
                string parent = Path.GetDirectoryName(link.TrimEnd('\\'));
                string name = Path.GetFileName(link.TrimEnd('\\'));
                if (string.IsNullOrEmpty(parent)) return null;
                string o = Sh.CmdOut("dir /al \"" + parent + "\"", 15);
                foreach (string raw in (o ?? "").Split('\n'))
                {
                    string l = raw.Trim();
                    if (l.IndexOf(name, StringComparison.OrdinalIgnoreCase) < 0) continue;
                    if (l.IndexOf("<", StringComparison.Ordinal) < 0) continue;
                    int a = l.LastIndexOf('[');
                    int b = l.LastIndexOf(']');
                    if (a >= 0 && b > a)
                        return l.Substring(a + 1, b - a - 1).Trim().TrimEnd('\\');
                }
            }
            catch (Exception) { }
            return null;
        }

        // ---------- 当前生效版本 ----------
        // 顺序：联接目标目录名 → 读 link\node.exe 的版本 → PATH 上的 node
        public static string CurrentVer(string nvmDir, string link)
        {
            string t = LinkTarget(link);
            if (!string.IsNullOrEmpty(t))
            {
                string n = Path.GetFileName(t);
                if (!string.IsNullOrEmpty(n) && char.IsDigit(n.TrimStart('v', 'V')[0]))
                    return Ver.Norm(n);
            }
            // 直接问 link 下的 node.exe
            if (!string.IsNullOrEmpty(link))
            {
                string exe = Path.Combine(link, "node.exe");
                if (File.Exists(exe))
                {
                    string o = Sh.RunOut(exe, "-v", null, 10);
                    o = (o ?? "").Trim();
                    if (o.Length > 0) return Ver.Norm(o.Split('\n')[0].Trim());
                }
            }
            // 最后问 PATH 上的 node
            string w = Sys.Which("node");
            if (!string.IsNullOrEmpty(w))
            {
                string o = Sh.RunOut(w, "-v", null, 10);
                o = (o ?? "").Trim();
                if (o.Length > 0) return Ver.Norm(o.Split('\n')[0].Trim());
            }
            return null;
        }

        public static string NpmVer(string nvmDir, string link)
        {
            if (string.IsNullOrEmpty(link)) return null;
            string exe = Path.Combine(link, "npm.cmd");
            if (!File.Exists(exe)) return null;
            string o = Sh.RunOut(exe, "-v", null, 20);
            o = (o ?? "").Trim();
            if (o.Length == 0) return null;
            string first = o.Split('\n')[0].Trim();
            return first.Length > 0 ? first : null;
        }

        public static bool IsAdmin()
        {
            try
            {
                System.Security.Principal.WindowsIdentity id =
                    System.Security.Principal.WindowsIdentity.GetCurrent();
                System.Security.Principal.WindowsPrincipal p =
                    new System.Security.Principal.WindowsPrincipal(id);
                return p.IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
            }
            catch (Exception) { return false; }
        }
    }

    // ========================================================================
    // 外部命令执行
    // ========================================================================
    static class Sh
    {
        public const string CREATE_NO_WINDOW = "CREATE_NO_WINDOW";
        static readonly Encoding Oem = Encoding.GetEncoding(936);

        // 跑一条命令，返回 exit code；out2 收 stdout+stderr
        public static int Run(string cmd, string args, out string out2)
        {
            return RunIn(null, cmd, args, out out2, 300);
        }

        public static int RunIn(string workDir, string cmd, string args, out string out2, int timeoutSec)
        {
            out2 = "";
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(cmd, args);
                psi.UseShellExecute = false;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;
                psi.CreateNoWindow = true;
                psi.StandardOutputEncoding = Oem;
                psi.StandardErrorEncoding = Oem;
                if (!string.IsNullOrEmpty(workDir)) psi.WorkingDirectory = workDir;
                using (Process p = Process.Start(psi))
                {
                    string so = p.StandardOutput.ReadToEnd();
                    string se = p.StandardError.ReadToEnd();
                    if (!p.WaitForExit(Math.Max(1, timeoutSec) * 1000))
                    {
                        try { p.Kill(); } catch (Exception) { }
                        out2 = so + se;
                        return -1;
                    }
                    out2 = (so + se).Trim();
                    return p.ExitCode;
                }
            }
            catch (Exception ex)
            {
                out2 = ex.Message;
                return -2;
            }
        }

        // 只要 stdout（用于 node -v 这类）
        public static string RunOut(string cmd, string args, string workDir, int timeoutSec)
        {
            string o;
            RunIn(workDir, cmd, args, out o, timeoutSec);
            return o;
        }

        // 走 cmd.exe /c 执行一行命令行（要重定向、要管道时用这个）
        public static int Cmd(string line, out string out2)
        {
            return CmdIn(null, line, out out2, 300);
        }

        public static int CmdIn(string workDir, string line, out string out2, int timeoutSec)
        {
            return RunIn(workDir, "cmd.exe", "/d /s /c \"" + line + "\"", out out2, timeoutSec);
        }

        // 只要输出，不要退出码
        public static string CmdOut(string line, int timeoutSec)
        {
            string o;
            CmdIn(null, line, out o, timeoutSec);
            return o;
        }
    }

    // ========================================================================
    // 已安装版本 / 在线版本 的数据模型
    // ========================================================================
    class VerEntry
    {
        public string Raw = "";          // 目录名，例如 "v20.2.0"
        public string Ver = "";          // "20.2.0"
        public string Dir = "";
        public bool HasNode;
        public long Bytes;
        public bool Current;

        public override string ToString()
        {
            return (Current ? "● " : "  ") + Ver + (HasNode ? "" : "  (不完整)");
        }
    }

    class RelEntry
    {
        public string Ver = "";          // "20.2.0"
        public string Lts = "";          // LTS 代号，空 = 非 LTS
        public bool Security;
        public bool Installed;

        public override string ToString()
        {
            string tag = Lts.Length > 0 ? ("  LTS " + Lts) : "";
            string sec = Security ? "  ⚠安全" : "";
            string ins = Installed ? "  ✓已装" : "";
            return Ver + tag + sec + ins;
        }
    }

    // ========================================================================
    // 版本列表 ListBox（owner-draw，深色）
    // ========================================================================
    class VerListBox : ListBox
    {
        public enum Mode { Installed, Online }
        public Mode Kind = Mode.Installed;
        public string CurVer = "";

        public VerListBox()
        {
            DrawMode = DrawMode.OwnerDrawFixed;
            BorderStyle = BorderStyle.None;
            BackColor = Th.LogBg;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            ItemHeight = 24;
            IntegralHeight = false;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= Items.Count) return;
            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            Rectangle r = e.Bounds;
            using (SolidBrush b = new SolidBrush(sel ? Th.Accent : Th.LogBg))
                e.Graphics.FillRectangle(b, r);

            if (Kind == Mode.Installed)
            {
                VerEntry v = Items[e.Index] as VerEntry;
                if (v == null) return;
                string mark = v.Current ? "●" : " ";
                Color mc = v.Current ? Th.Ok : Th.Dim;
                TextRenderer.DrawText(e.Graphics, mark, Th.UiB,
                    new Point(r.X + 6, r.Y + 5), mc);
                Color vc = v.Current ? Th.Ok : (v.HasNode ? Th.Fg : Th.Warn);
                TextRenderer.DrawText(e.Graphics, v.Ver, Th.Ui,
                    new Point(r.X + 24, r.Y + 5), sel ? Color.White : vc);
                string right = v.HasNode ? Fmt.Bytes(v.Bytes) : "缺少 node.exe";
                Color rc = v.HasNode ? Th.Dim : Th.Warn;
                Size sz = TextRenderer.MeasureText(right, Th.UiS);
                TextRenderer.DrawText(e.Graphics, right, Th.UiS,
                    new Point(r.Right - sz.Width - 8, r.Y + 6), sel ? Color.White : rc);
            }
            else
            {
                RelEntry v = Items[e.Index] as RelEntry;
                if (v == null) return;
                bool cur = v.Ver == CurVer;
                Color vc = cur ? Th.Ok : Th.Fg;
                TextRenderer.DrawText(e.Graphics, v.Ver, Th.Ui,
                    new Point(r.X + 8, r.Y + 5), sel ? Color.White : vc);

                string right = "";
                Color rc = Th.Dim;
                if (v.Lts.Length > 0) { right = "LTS " + v.Lts; rc = Th.Cyan; }
                if (v.Security) { right = right.Length > 0 ? right + "  ⚠" : "⚠"; rc = Th.Warn; }
                if (v.Installed) { right = right.Length > 0 ? right + "  ✓" : "✓"; rc = Th.Ok; }
                if (cur) { right = right.Length > 0 ? right + "  当前" : "当前"; rc = Th.Ok; }
                if (right.Length > 0)
                {
                    Size sz = TextRenderer.MeasureText(right, Th.UiS);
                    TextRenderer.DrawText(e.Graphics, right, Th.UiS,
                        new Point(r.Right - sz.Width - 8, r.Y + 6), sel ? Color.White : rc);
                }
            }
        }
    }

    static class Fmt
    {
        public static string Bytes(long b)
        {
            if (b <= 0) return "-";
            double v = b;
            string[] u = new string[] { "B", "KB", "MB", "GB", "TB" };
            int i = 0;
            while (v >= 1024 && i < u.Length - 1) { v /= 1024; i++; }
            return (i == 0 ? ((long)v).ToString() : v.ToString("0.#")) + " " + u[i];
        }
    }

    // ========================================================================
    // 本地已安装版本扫描
    // 性能铁律：只读目录名 + 探 node.exe，绝不递归（递归一个版本目录要 90s+）
    // ========================================================================
    static class Scan
    {
        public static List<VerEntry> Installed(string nvmDir, string curVer)
        {
            List<VerEntry> res = new List<VerEntry>();
            if (string.IsNullOrEmpty(nvmDir) || !Directory.Exists(nvmDir)) return res;
            try
            {
                foreach (string d in Directory.GetDirectories(nvmDir))
                {
                    string name = Path.GetFileName(d);
                    if (name.Length < 2) continue;
                    if (name[0] != 'v' && name[0] != 'V') continue;
                    int[] nums; string tail;
                    if (!Ver.Parse(name, out nums, out tail)) continue;
                    if (tail.Length > 0) continue;              // 预览版目录名先不收（避免误删）
                    if (nums.Length < 3) continue;

                    VerEntry e = new VerEntry();
                    e.Raw = name;
                    e.Ver = Ver.Norm(name);
                    e.Dir = d;
                    try { e.HasNode = File.Exists(Path.Combine(d, "node.exe")); }
                    catch (Exception) { }
                    e.Bytes = TopBytes(d);
                    e.Current = e.Ver == curVer;
                    res.Add(e);
                }
            }
            catch (Exception) { }

            res.Sort(delegate(VerEntry a, VerEntry b) { return Ver.Cmp(a.Ver, b.Ver); });
            return res;
        }

        // 只算顶层文件 + node_modules 一层；递归会卡到怀疑人生
        public static long TopBytes(string dir)
        {
            long sum = 0;
            try
            {
                foreach (string f in Directory.GetFiles(dir))
                {
                    try { sum += new FileInfo(f).Length; } catch (Exception) { }
                }
            }
            catch (Exception) { }
            // node_modules 里 npm 自身占大头，算一层目录名即可，不再往下走
            try
            {
                string nm = Path.Combine(dir, "node_modules");
                if (Directory.Exists(nm))
                {
                    foreach (string sub in Directory.GetDirectories(nm))
                    {
                        try
                        {
                            foreach (string f in Directory.GetFiles(sub))
                            {
                                try { sum += new FileInfo(f).Length; } catch (Exception) { }
                            }
                        }
                        catch (Exception) { }
                    }
                }
            }
            catch (Exception) { }
            return sum;
        }
    }

    // ========================================================================
    // 在线版本：拉 index.json（异步、可取消）
    // 原项目在 UI 线程同步 Invoke-RestMethod，一卡 15 秒。
    // 这里改成后台线程 + WebRequest，主线程只做 100ms 轮询，UI 全程不冻。
    // ========================================================================
    static class Online
    {
        public class Result
        {
            public bool Ok;
            public string Error = "";
            public List<RelEntry> Items = new List<RelEntry>();
            public string Mirror = "";
            public long Ms;
        }

        // 后台线程执行，result 通过回调送回；返回一个「取消」委托
        public static Thread Fetch(string mirror, HashSet<string> installed,
                                   Action<Result> done, Action<string> progress)
        {
            Thread t = new Thread(delegate()
            {
                Result r = new Result();
                r.Mirror = mirror;
                Stopwatch sw = Stopwatch.StartNew();
                try
                {
                    if (string.IsNullOrEmpty(mirror)) throw new Exception("镜像源未配置");
                    // 用户可能填了结尾没斜杠、也可能填到 index.json、也可能填到 /dist/
                    string baseUrl = mirror.Trim().TrimEnd('/');
                    if (baseUrl.EndsWith("/index.json", StringComparison.OrdinalIgnoreCase))
                        baseUrl = baseUrl.Substring(0, baseUrl.Length - "/index.json".Length);
                    string url = baseUrl + "/index.json";

                    if (progress != null) progress("正在请求 " + url);
                    string body = Http(url, 30000);
                    if (progress != null) progress("已收到 " + (body.Length / 1024) + " KB，解析中…");

                    List<RelEntry> list = ParseIndex(body, installed);
                    if (list.Count == 0) throw new Exception("index.json 里没有解析出任何版本");
                    list.Sort(delegate(RelEntry a, RelEntry b) { return Ver.Cmp(a.Ver, b.Ver); });
                    r.Items = list;
                    r.Ok = true;
                }
                catch (Exception ex)
                {
                    r.Error = ex.Message;
                    r.Ok = false;
                }
                sw.Stop();
                r.Ms = sw.ElapsedMilliseconds;
                if (done != null) done(r);
            });
            t.IsBackground = true;
            t.Name = "nvm-desk-fetch";
            t.Start();
            return t;
        }

        // 用 WebRequest 而不是 WebClient：能设 UA（某些镜像站挡默认 UA）与超时
        public static string Http(string url, int timeoutMs)
        {
            HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
            req.Method = "GET";
            req.UserAgent = "nvm-desk/1.0 (+WinForms; .NET4.0)";
            req.Timeout = timeoutMs;
            req.ReadWriteTimeout = timeoutMs;
            req.AllowAutoRedirect = true;
            req.AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate;
            using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
            using (Stream s = resp.GetResponseStream())
            using (StreamReader sr = new StreamReader(s, Encoding.UTF8))
                return sr.ReadToEnd();
        }

        // index.json 是扁平数组 [{version,date,files,lts,security,npm,v8,...}, ...]
        // 不用 JSON 库，手工扫对象即可（.NET 4.0 没有内置 JSON）
        public static List<RelEntry> ParseIndex(string body, HashSet<string> installed)
        {
            List<RelEntry> list = new List<RelEntry>();
            if (string.IsNullOrEmpty(body)) return list;

            MatchCollection objs = Regex.Matches(body, @"\{[^{}]*\}", RegexOptions.Singleline);
            foreach (Match m in objs)
            {
                string o = m.Value;
                string ver = Str(o, "version");
                if (ver.Length == 0) continue;
                // 排除 nightly / rc 之类
                if (ver.IndexOf('-') >= 0) continue;

                RelEntry e = new RelEntry();
                e.Ver = Ver.Norm(ver);
                e.Lts = Lts(o);
                e.Security = Regex.IsMatch(o, @"""security""\s*:\s*true", RegexOptions.IgnoreCase);
                e.Installed = installed != null && installed.Contains(e.Ver);
                list.Add(e);
            }
            return list;
        }

        // "version":"v20.2.0" —— 也容忍不带引号的数字形式（objects 里 files 有）
        static readonly Regex KeyStr = new Regex("\"(?<k>[A-Za-z0-9_]+)\"\\s*:\\s*\"(?<v>[^\"]*)\"");
        static readonly Regex KeyRaw = new Regex("\"(?<k>[A-Za-z0-9_]+)\"\\s*:\\s*(?<v>[^,}\\s]+)");

        static Dictionary<string, string> Fields(string o)
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            foreach (Match m in KeyStr.Matches(o))
                d[m.Groups["k"].Value.ToLowerInvariant()] = m.Groups["v"].Value;
            foreach (Match m in KeyRaw.Matches(o))
            {
                string k = m.Groups["k"].Value.ToLowerInvariant();
                if (!d.ContainsKey(k)) d[k] = m.Groups["v"].Value;
            }
            return d;
        }

        static string Str(string o, string key)
        {
            Dictionary<string, string> d = Fields(o);
            string v;
            return d.TryGetValue(key.ToLowerInvariant(), out v) ? v : "";
        }

        // lts 可能是 false（布尔）或 "Hydrogen"（字符串）
        static string Lts(string o)
        {
            Dictionary<string, string> d = Fields(o);
            string v;
            if (!d.TryGetValue("lts", out v)) return "";
            v = v.Trim();
            if (v.Length == 0) return "";
            if (v.Equals("false", StringComparison.OrdinalIgnoreCase)) return "";
            if (v.Equals("true", StringComparison.OrdinalIgnoreCase)) return "LTS";
            return v;
        }
    }

    // ========================================================================
    // 写操作：切换 / 安装 / 卸载
    // 与 node-switcher 同源实现：不调 nvm.exe（本机那份 8.1MB 非官方 build
    // 任何命令都 rc=0 但零输出零副作用），而是自己做等价动作。
    //   nvm use     ≡ rmdir 联接 + mklink /J 重建
    //   nvm install ≡ 下 zip + bsdtar 解压拍平到 <root>\vX
    // ========================================================================
    static class Ops
    {
        public static string TarExe()
        {
            string p = Path.Combine(Environment.SystemDirectory, "tar.exe");
            return File.Exists(p) ? p : "tar.exe";
        }

        // ---------- 切换 ----------
        public static string Use(string nvmDir, string link, string ver)
        {
            string target = Path.Combine(nvmDir, "v" + Ver.Norm(ver));
            if (!Directory.Exists(target)) return "目标版本目录不存在: " + target;
            if (!File.Exists(Path.Combine(target, "node.exe")))
                return "该版本目录下没有 node.exe，安装不完整: " + target;
            if (string.IsNullOrEmpty(link)) return "未配置 NVM_SYMLINK 路径";

            StopNode(link);

            // 先摘掉旧联接（rmdir 对 junction 只摘链接，不动目标内容）
            if (Directory.Exists(link))
            {
                string o;
                int rc = Sh.Cmd("rmdir \"" + link + "\"", out o);
                if (rc != 0 && Directory.Exists(link))
                    return "删除旧联接失败: " + o;
            }
            else
            {
                // link 可能是普通目录（用户手建的），不能删
                if (File.Exists(link)) return "目标路径被文件占用: " + link;
            }

            string parent = Path.GetDirectoryName(link);
            if (!string.IsNullOrEmpty(parent) && !Directory.Exists(parent))
                Directory.CreateDirectory(parent);

            string out2;
            int c = Sh.Cmd("mklink /J \"" + link + "\" \"" + target + "\"", out out2);
            if (c != 0 || !Directory.Exists(link))
                return "创建联接失败: " + out2;

            string now = NvmEnv.CurrentVer(nvmDir, link);
            if (now != Ver.Norm(ver))
                return "切换后校验失败：期望 " + Ver.Norm(ver) + "，实际 " + (now == null ? "(未知)" : now);
            return null;   // null = 成功
        }

        // 切换前先结束占用 link\node.exe 的进程，否则文件被锁
        static void StopNode(string link)
        {
            try
            {
                string exe = Path.Combine(link, "node.exe");
                if (!File.Exists(exe)) return;
                foreach (Process p in Process.GetProcessesByName("node"))
                {
                    try { p.Kill(); p.WaitForExit(2000); }
                    catch (Exception) { }
                }
            }
            catch (Exception) { }
        }

        // ---------- 卸载 ----------
        public static string Uninstall(string nvmDir, string link, string ver)
        {
            string target = Path.Combine(nvmDir, "v" + Ver.Norm(ver));
            if (!Directory.Exists(target)) return "版本目录不存在: " + target;

            // 正在使用的版本要先切走？不强制，但联接指向它时必须先摘
            string t = NvmEnv.LinkTarget(link);
            if (!string.IsNullOrEmpty(t) &&
                string.Equals(t.TrimEnd('\\'), target.TrimEnd('\\'), StringComparison.OrdinalIgnoreCase))
                return "该版本正在使用中，请先切换到其他版本再卸载";

            try
            {
                // 目录可能只读，先清属性
                foreach (string f in Directory.GetFiles(target, "*", SearchOption.AllDirectories))
                {
                    try { File.SetAttributes(f, FileAttributes.Normal); } catch (Exception) { }
                }
                Directory.Delete(target, true);
            }
            catch (Exception ex)
            {
                return "删除失败（可能有进程占用）: " + ex.Message;
            }
            return null;
        }

        // ---------- 安装 ----------
        // 先下到同盘的临时目录再改名，避免跨盘 Move 变成真拷贝
        public static string Install(string nvmDir, string mirror, string ver,
                                     Action<string> log, Func<bool> cancelled)
        {
            string v = Ver.Norm(ver);
            string dest = Path.Combine(nvmDir, "v" + v);
            if (Directory.Exists(dest)) return "该版本已存在: " + dest;

            string baseUrl = mirror.Trim().TrimEnd('/');
            if (baseUrl.EndsWith("/index.json", StringComparison.OrdinalIgnoreCase))
                baseUrl = baseUrl.Substring(0, baseUrl.Length - "/index.json".Length);
            string url = baseUrl + "/v" + v + "/node-v" + v + "-win-x64.zip";

            string tmpRoot = Path.Combine(nvmDir, ".tmp-install-" + v);
            string zip = tmpRoot + ".zip";
            try
            {
                if (Directory.Exists(tmpRoot)) Directory.Delete(tmpRoot, true);
                if (File.Exists(zip)) File.Delete(zip);
            }
            catch (Exception) { }

            try
            {
                if (log != null) log("下载 " + url);
                Download(url, zip, log, cancelled);
                if (cancelled != null && cancelled()) return "已取消";

                long size = 0;
                try { size = new FileInfo(zip).Length; } catch (Exception) { }
                if (log != null) log("下载完成 " + Fmt.Bytes(size) + "，解压中…");

                if (!Directory.Exists(tmpRoot)) Directory.CreateDirectory(tmpRoot);
                string o;
                int rc = Sh.Cmd("\"" + TarExe() + "\" -xf \"" + zip + "\" -C \"" + tmpRoot + "\"", out o);
                if (rc != 0) return "解压失败: " + o;

                // zip 内有一层 node-vX-win-x64\，拍平
                string inner = null;
                foreach (string d in Directory.GetDirectories(tmpRoot))
                {
                    if (inner == null) inner = d;
                }
                if (inner == null)
                {
                    if (File.Exists(Path.Combine(tmpRoot, "node.exe")))
                        inner = tmpRoot;
                    else
                        return "解压结果里找不到 node.exe";
                }

                if (!File.Exists(Path.Combine(inner, "node.exe")))
                    return "解压结果里找不到 node.exe（目录结构异常）";

                // inner == tmpRoot 时不能 Move 自己
                if (!string.Equals(inner, tmpRoot, StringComparison.OrdinalIgnoreCase))
                {
                    Directory.Move(inner, dest);
                    try { Directory.Delete(tmpRoot, true); } catch (Exception) { }
                }
                else
                {
                    Directory.Move(tmpRoot, dest);
                }

                try { if (File.Exists(zip)) File.Delete(zip); } catch (Exception) { }

                if (!File.Exists(Path.Combine(dest, "node.exe")))
                    return "安装后校验失败：缺少 node.exe";
                if (log != null) log("已安装到 " + dest);
                return null;
            }
            catch (Exception ex)
            {
                return "安装失败: " + ex.Message;
            }
        }

        // 带进度的下载（WebRequest + 流拷贝，可取消）
        static void Download(string url, string dest, Action<string> log, Func<bool> cancelled)
        {
            HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
            req.Method = "GET";
            req.UserAgent = "nvm-desk/1.0 (+WinForms; .NET4.0)";
            req.Timeout = 60000;
            req.ReadWriteTimeout = 60000;
            req.AllowAutoRedirect = true;
            req.AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate;

            using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
            using (Stream rs = resp.GetResponseStream())
            using (FileStream fs = new FileStream(dest, FileMode.Create, FileAccess.Write, FileShare.None))
            {
                long total = resp.ContentLength;
                byte[] buf = new byte[131072];
                long got = 0;
                int lastPct = -1;
                while (true)
                {
                    if (cancelled != null && cancelled()) throw new Exception("已取消");
                    int n = rs.Read(buf, 0, buf.Length);
                    if (n <= 0) break;
                    fs.Write(buf, 0, n);
                    got += n;
                    if (total > 0 && log != null)
                    {
                        int pct = (int)(got * 100 / total);
                        if (pct / 10 != lastPct / 10)
                        {
                            lastPct = pct;
                            log("  " + pct + "%  " + Fmt.Bytes(got) + " / " + Fmt.Bytes(total));
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // 配置
    // ========================================================================
    class Conf
    {
        public string Home = "";
        public string Link = "";
        public string Mirror = NvmEnv.DEFAULT_MIRROR;
        public string LastTab = "installed";

        const string APP = "nvmdesk";

        public static Conf Load()
        {
            Conf c = new Conf();
            Dictionary<string, string> d = Cfg.Load(APP);
            string v;
            if (d.TryGetValue("home", out v) && v.Length > 0) c.Home = v;
            if (d.TryGetValue("link", out v) && v.Length > 0) c.Link = v;
            if (d.TryGetValue("mirror", out v) && v.Length > 0) c.Mirror = v;
            if (d.TryGetValue("lastTab", out v) && v.Length > 0) c.LastTab = v;

            // 没存过的字段走自动探测
            if (c.Home.Length == 0)
            {
                foreach (string cand in NvmEnv.Candidates(null))
                {
                    if (NvmEnv.LooksLikeNvm(cand)) { c.Home = cand; break; }
                }
            }
            if (c.Link.Length == 0) c.Link = NvmEnv.DetectLink(c.Home) ?? "";
            if (c.Mirror.Length == 0) c.Mirror = NvmEnv.DEFAULT_MIRROR;
            return c;
        }

        public void Save()
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            d["home"] = Home ?? "";
            d["link"] = Link ?? "";
            d["mirror"] = Mirror ?? "";
            d["lastTab"] = LastTab ?? "installed";
            Cfg.Save(APP, d);
        }
    }

    // ========================================================================
    // 主窗体
    // 关键设计（对应原项目缺陷 ⑤⑥）：
    //   * 所有控件只建一次，之后只改内容与 Visible，绝不 Controls.Clear()
    //   * 网络请求全在后台线程，主线程靠 Timer 轮询结果
    // ========================================================================
    class MainForm : Form
    {
        Conf conf;

        TextBox txtHome, txtLink, txtMirror;
        Label lblEnv, lblCur, lblStatus;
        FlatBtn btnBrowseHome, btnBrowseLink, btnRescan, btnSave, btnReload, btnDefaultMirror;
        FlatBtn btnUse, btnInstall, btnUninstall, btnOpenHome, btnOpenLink, btnCopyCmds;
        FlatBtn tabInstalled, tabOnline;
        VerListBox listInstalled, listOnline;
        CheckBox chkLtsOnly, chkHideInstalled;
        LogView log;

        List<VerEntry> installed = new List<VerEntry>();
        List<RelEntry> online = new List<RelEntry>();
        string curVer = null;

        Thread fetchThread;
        volatile bool fetching;
        volatile Online.Result fetchResult;
        volatile string fetchTip;
        System.Windows.Forms.Timer poller;

        // 操作互斥：切换/安装/卸载期间禁用按钮
        volatile bool busy;
        Thread workThread;
        volatile string workDone;
        volatile string workTip;

        const int PAD = 16;
        const int WIN_W = 980;
        const int WIN_H = 720;

        public MainForm()
        {
            conf = Conf.Load();

            Ux.Dark(this);
            Text = "NVM 版本管理器";
            ClientSize = new Size(WIN_W, WIN_H);
            MinimumSize = new Size(880, 640);
            DoubleBuffered = true;
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer, true);
            StartPosition = FormStartPosition.CenterScreen;

            try
            {
                Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath);
            }
            catch (Exception) { }

            BuildUi();
            Rescan();

            poller = new System.Windows.Forms.Timer();
            poller.Interval = 100;
            poller.Tick += delegate { Poll(); };
            poller.Start();

            FormClosing += delegate { if (fetchThread != null && fetchThread.IsAlive) { /* 后台线程，进程退出即止 */ } };
        }

        // ---------------------------------------------------------------- UI
        void BuildUi()
        {
            // ===== 配置卡：nvm 路径 / 联接路径 / 镜像源 =====
            Panel card = new Panel();
            card.SetBounds(PAD, PAD, WIN_W - PAD * 2, 150);
            card.BackColor = Th.Panel;
            card.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(card);

            // ---- 行 1：nvm 目录 ----
            card.Controls.Add(Ux.Lbl("NVM 目录", 14, 14, 66));
            txtHome = new TextBox();
            txtHome.Text = conf.Home;
            card.Controls.Add(Ux.Field(txtHome, 86, 10, 640, 28));
            txtHome.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            btnBrowseHome = Ux.Btn("浏览", 734, 10, 56, false);
            card.Controls.Add(btnBrowseHome);
            btnBrowseHome.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnBrowseHome.Click += delegate { PickDir(txtHome, "选择 NVM 安装目录（其下应有 nvm.exe / settings.txt）"); OnEnvChanged(); };

            lblEnv = Ux.Val("", 800, 14, 160, Th.Dim);
            card.Controls.Add(lblEnv);
            lblEnv.Anchor = AnchorStyles.Top | AnchorStyles.Right;

            // ---- 行 2：nodejs 联接 ----
            card.Controls.Add(Ux.Lbl("联接路径", 14, 52, 66));
            txtLink = new TextBox();
            txtLink.Text = conf.Link;
            card.Controls.Add(Ux.Field(txtLink, 86, 48, 640, 28));
            txtLink.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            btnBrowseLink = Ux.Btn("浏览", 734, 48, 56, false);
            card.Controls.Add(btnBrowseLink);
            btnBrowseLink.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnBrowseLink.Click += delegate { PickDir(txtLink, "选择 NVM_SYMLINK 目录（nodejs 联接指向处）"); OnEnvChanged(); };

            // ---- 行 3：镜像源 ----
            card.Controls.Add(Ux.Lbl("镜像源", 14, 90, 66));
            txtMirror = new TextBox();
            txtMirror.Text = conf.Mirror;
            card.Controls.Add(Ux.Field(txtMirror, 86, 86, 640, 28));
            txtMirror.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            btnDefaultMirror = Ux.Btn("npmmirror", 734, 86, 56, false);
            card.Controls.Add(btnDefaultMirror);
            btnDefaultMirror.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnDefaultMirror.Click += delegate
            {
                txtMirror.Text = NvmEnv.DEFAULT_MIRROR;
                OnEnvChanged();
            };

            // ---- 行 4：状态 + 操作 ----
            lblStatus = Ux.Val("", 14, 122, 560, Th.Dim);
            card.Controls.Add(lblStatus);
            lblStatus.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;

            btnSave = Ux.Btn("保存配置", WIN_W - PAD * 2 - 320, 118, 74, true);
            card.Controls.Add(btnSave);
            btnSave.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnSave.Click += delegate { OnSave(); };

            btnRescan = Ux.Btn("重新扫描", WIN_W - PAD * 2 - 238, 118, 74, false);
            card.Controls.Add(btnRescan);
            btnRescan.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnRescan.Click += delegate { Rescan(); };

            btnOpenHome = Ux.Btn("打开目录", WIN_W - PAD * 2 - 156, 118, 74, false);
            card.Controls.Add(btnOpenHome);
            btnOpenHome.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnOpenHome.Click += delegate { OpenDir(conf.Home); };

            btnCopyCmds = Ux.Btn("复制命令", WIN_W - PAD * 2 - 74, 118, 74, false);
            card.Controls.Add(btnCopyCmds);
            btnCopyCmds.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnCopyCmds.Click += delegate { CopyCommands(); };

            int bodyTop = PAD + 150 + 12;

            // ===== 标签页 =====
            tabInstalled = Ux.Btn("已安装", PAD, bodyTop, 88, true);
            tabInstalled.Height = 30;
            Controls.Add(tabInstalled);
            tabInstalled.Click += delegate { SwitchTab("installed"); };

            tabOnline = Ux.Btn("在线可安装", PAD + 92, bodyTop, 96, false);
            tabOnline.Height = 30;
            Controls.Add(tabOnline);
            tabOnline.Click += delegate { SwitchTab("online"); };

            lblCur = Ux.Val("", PAD + 200, bodyTop + 5, 500, Th.Dim);
            Controls.Add(lblCur);
            lblCur.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;

            int paneTop = bodyTop + 38;
            int logH = 150;
            int paneH = WIN_H - paneTop - logH - PAD - 12;
            if (paneH < 200) { paneH = 200; logH = WIN_H - paneTop - paneH - PAD - 12; }

            // ===== 卡片：已安装 =====
            Panel pIns = new Panel();
            pIns.SetBounds(PAD, paneTop, WIN_W - PAD * 2, paneH);
            pIns.BackColor = Th.Panel;
            pIns.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(pIns);
            panelInstalled = pIns;

            Label lIns = Ux.Lbl("已安装版本（点击选择，右侧按钮操作）", 14, 10, 460);
            pIns.Controls.Add(lIns);
            lblInstalledCount = Ux.Val("", 480, 10, 300, Th.Dim);
            lblInstalledCount.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            pIns.Controls.Add(lblInstalledCount);

            listInstalled = new VerListBox();
            listInstalled.Kind = VerListBox.Mode.Installed;
            listInstalled.SetBounds(14, 36, WIN_W - PAD * 2 - 28 - 168, paneH - 50);
            listInstalled.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listInstalled.SelectedIndexChanged += delegate { UpdateButtons(); };
            listInstalled.DoubleClick += delegate { DoUse(); };
            pIns.Controls.Add(listInstalled);

            int bx = WIN_W - PAD * 2 - 14 - 154;
            btnUse = Ux.Btn("切换到此版本", bx, 36, 154, true);
            pIns.Controls.Add(btnUse);
            btnUse.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnUse.Click += delegate { DoUse(); };

            btnUninstall = Ux.Btn("卸载", bx, 76, 154, false);
            pIns.Controls.Add(btnUninstall);
            btnUninstall.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnUninstall.Click += delegate { DoUninstall(); };

            btnOpenLink = Ux.Btn("打开联接目录", bx, 116, 154, false);
            pIns.Controls.Add(btnOpenLink);
            btnOpenLink.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnOpenLink.Click += delegate { OpenDir(conf.Link); };

            // ===== 卡片：在线 =====
            Panel pOn = new Panel();
            pOn.SetBounds(PAD, paneTop, WIN_W - PAD * 2, paneH);
            pOn.BackColor = Th.Panel;
            pOn.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            pOn.Visible = false;
            Controls.Add(pOn);
            panelOnline = pOn;

            Label lOn = Ux.Lbl("在线可安装版本（镜像源 list）", 14, 10, 300);
            pOn.Controls.Add(lOn);
            // 计数标签只占中间一段，右边留给两个勾选框 + 刷新按钮，别互相压
            lblOnlineCount = Ux.Val("", 320, 10, 240, Th.Dim);
            lblOnlineCount.Anchor = AnchorStyles.Top | AnchorStyles.Left;
            pOn.Controls.Add(lblOnlineCount);

            chkLtsOnly = new CheckBox();
            chkLtsOnly.Text = "只看 LTS";
            chkLtsOnly.ForeColor = Th.Fg;
            chkLtsOnly.BackColor = Color.Transparent;
            chkLtsOnly.Font = Th.Ui;
            chkLtsOnly.SetBounds(WIN_W - PAD * 2 - 14 - 300, 8, 90, 22);
            chkLtsOnly.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            chkLtsOnly.CheckedChanged += delegate { FilterOnline(); };
            pOn.Controls.Add(chkLtsOnly);

            chkHideInstalled = new CheckBox();
            chkHideInstalled.Text = "隐藏已装";
            chkHideInstalled.ForeColor = Th.Fg;
            chkHideInstalled.BackColor = Color.Transparent;
            chkHideInstalled.Font = Th.Ui;
            chkHideInstalled.SetBounds(WIN_W - PAD * 2 - 14 - 200, 8, 90, 22);
            chkHideInstalled.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            chkHideInstalled.CheckedChanged += delegate { FilterOnline(); };
            pOn.Controls.Add(chkHideInstalled);

            btnReload = Ux.Btn("刷新列表", WIN_W - PAD * 2 - 14 - 100, 4, 100, true);
            btnReload.Height = 30;
            btnReload.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnReload.Click += delegate { StartFetch(true); };
            pOn.Controls.Add(btnReload);

            listOnline = new VerListBox();
            listOnline.Kind = VerListBox.Mode.Online;
            listOnline.SetBounds(14, 36, WIN_W - PAD * 2 - 28 - 168, paneH - 50);
            listOnline.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listOnline.SelectedIndexChanged += delegate { UpdateButtons(); };
            listOnline.DoubleClick += delegate { DoInstall(); };
            pOn.Controls.Add(listOnline);

            btnInstall = Ux.Btn("安装此版本", bx, 36, 154, true);
            pOn.Controls.Add(btnInstall);
            btnInstall.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnInstall.Click += delegate { DoInstall(); };

            // ---- 查看上一张图的 pInstalled 里 btnInstall 的停靠一致 ----

            // ===== 日志 =====
            log = new LogView();
            log.Box.SetBounds(PAD, WIN_H - logH - PAD, WIN_W - PAD * 2, logH);
            log.Box.Anchor = AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(log.Box);

            SwitchTab(conf.LastTab == "online" ? "online" : "installed", false);
            UpdateButtons();
        }

        internal Panel panelInstalled, panelOnline;
        Label lblInstalledCount, lblOnlineCount;

        // ---- 仅供 --uicheck 自检使用 ----
        internal bool IsFetching { get { return fetching; } }
        internal int OnlineCount { get { return online.Count; } }
        internal void StartFetchForCheck() { StartFetch(true); }
        // 自检时没有真正的消息循环，Timer 不会自己走，所以手动抽一次
        internal void PollOnce() { Poll(); }

        // ------------------------------------------------------------ 交互
        void PickDir(TextBox t, string title)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = title;
            if (Directory.Exists(t.Text)) d.SelectedPath = t.Text;
            if (d.ShowDialog(this) == DialogResult.OK) t.Text = d.SelectedPath;
        }

        void OpenDir(string dir)
        {
            if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir))
            {
                log.Warn("目录不存在: " + (string.IsNullOrEmpty(dir) ? "(未配置)" : dir));
                return;
            }
            try { Sys.OpenFolder(dir); log.Info("已打开 " + dir); }
            catch (Exception ex) { log.Err("打开失败: " + ex.Message); }
        }

        void CopyCommands()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("# NVM 常用命令（可在任意终端执行）");
            sb.AppendLine("nvm list");
            sb.AppendLine("nvm list available");
            sb.AppendLine("nvm use 20.2.0");
            sb.AppendLine("nvm install 20.2.0");
            sb.AppendLine("nvm uninstall 20.2.0");
            sb.AppendLine("");
            sb.AppendLine("# 本机配置");
            sb.AppendLine("NVM_HOME    = " + conf.Home);
            sb.AppendLine("NVM_SYMLINK = " + conf.Link);
            sb.AppendLine("node_mirror = " + conf.Mirror);
            try
            {
                Clipboard.SetText(sb.ToString());
                log.Ok("命令与配置已复制到剪贴板");
            }
            catch (Exception ex) { log.Err("复制失败: " + ex.Message); }
        }

        void OnSave()
        {
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            conf.Mirror = txtMirror.Text.Trim();
            conf.LastTab = (panelOnline != null && panelOnline.Visible) ? "online" : "installed";
            conf.Save();
            log.Ok("配置已保存到 %APPDATA%\\DevLauncher\\nvmdesk.ini");
            OnEnvChanged();
            Rescan();
        }

        void OnEnvChanged()
        {
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            conf.Mirror = txtMirror.Text.Trim();
            UpdateEnvLabel();
        }

        void UpdateEnvLabel()
        {
            string h = txtHome.Text.Trim();
            string l = txtLink.Text.Trim();
            bool hOk = NvmEnv.LooksLikeNvm(h);
            bool lOk = !string.IsNullOrEmpty(l);

            StringBuilder sb = new StringBuilder();
            sb.Append(hOk ? "✓ NVM 目录有效" : "✗ NVM 目录无效");
            sb.Append("   ");
            sb.Append(lOk ? "✓ 联接已配" : "? 联接未配");
            lblEnv.Text = sb.ToString();
            lblEnv.ForeColor = hOk ? Th.Ok : Th.Warn;

            string t = NvmEnv.LinkTarget(l);
            string st = "NVM_HOME=" + (h.Length == 0 ? "(空)" : h);
            st += "   NVM_SYMLINK=" + (l.Length == 0 ? "(空)" : l);
            if (!string.IsNullOrEmpty(t)) st += "  →  " + Path.GetFileName(t);
            if (!NvmEnv.IsAdmin()) st += "   ⚠ 非管理员权限";
            lblStatus.Text = st;
            lblStatus.ForeColor = NvmEnv.IsAdmin() ? Th.Dim : Th.Warn;
        }

        void SwitchTab(string which)
        {
            SwitchTab(which, true);
        }

        void SwitchTab(string which, bool userAction)
        {
            bool onlineTab = which == "online";
            if (panelOnline != null)
            {
                panelOnline.Visible = onlineTab;
                panelInstalled.Visible = !onlineTab;
                tabOnline.Base = onlineTab ? Th.Accent : Th.Input;
                tabOnline.Hover = onlineTab ? Th.AccHov : Th.Hover;
                tabInstalled.Base = onlineTab ? Th.Input : Th.Accent;
                tabInstalled.Hover = onlineTab ? Th.Hover : Th.AccHov;
                tabOnline.Invalidate();
                tabInstalled.Invalidate();
            }
            if (onlineTab && userAction)
            {
                conf.LastTab = "online";
                if (online.Count == 0 && !fetching) StartFetch(false);
            }
            if (!onlineTab && userAction) { conf.LastTab = "installed"; }
            UpdateButtons();
        }

        void UpdateButtons()
        {
            bool canUse = !busy && listInstalled != null && listInstalled.SelectedItem is VerEntry;
            if (btnUse != null) btnUse.Enabled = canUse;

            VerEntry ve = listInstalled != null ? listInstalled.SelectedItem as VerEntry : null;
            if (btnUninstall != null) btnUninstall.Enabled = canUse && ve != null && !ve.Current;

            bool canIns = !busy && listOnline != null && listOnline.SelectedItem is RelEntry;
            if (btnInstall != null) btnInstall.Enabled = canIns;

            RelEntry re = listOnline != null ? listOnline.SelectedItem as RelEntry : null;
            if (btnInstall != null && canIns && re != null)
                btnInstall.Text = re.Installed ? "重新安装" : "安装此版本";
            else if (btnInstall != null)
                btnInstall.Text = "安装此版本";

            if (btnReload != null) btnReload.Enabled = !busy && !fetching;
            if (btnRescan != null) btnRescan.Enabled = !busy;
            if (btnSave != null) btnSave.Enabled = !busy;
            if (btnDefaultMirror != null) btnDefaultMirror.Enabled = !busy;
            if (tabInstalled != null) tabInstalled.Enabled = !busy;
            if (tabOnline != null) tabOnline.Enabled = !busy;
        }

        // ------------------------------------------------------------ 扫描
        void Rescan()
        {
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            curVer = NvmEnv.CurrentVer(conf.Home, conf.Link);

            installed = Scan.Installed(conf.Home, curVer);

            listInstalled.BeginUpdate();
            listInstalled.Items.Clear();
            foreach (VerEntry v in installed) listInstalled.Items.Add(v);
            listInstalled.EndUpdate();

            // 选中当前版本
            int sel = -1;
            for (int i = 0; i < listInstalled.Items.Count; i++)
            {
                VerEntry v = (VerEntry)listInstalled.Items[i];
                if (v.Current) { sel = i; break; }
            }
            if (sel >= 0) listInstalled.SelectedIndex = sel;
            else if (listInstalled.Items.Count > 0) listInstalled.SelectedIndex = 0;

            string npm = NvmEnv.NpmVer(conf.Home, conf.Link);
            StringBuilder sb = new StringBuilder();
            sb.Append("当前版本: " + (curVer == null ? "(未激活)" : curVer));
            if (npm != null) sb.Append("   npm " + npm);
            sb.Append("   NVM_HOME: " + (Directory.Exists(conf.Home) ? conf.Home : conf.Home + " (不存在)"));
            lblCur.Text = sb.ToString();
            lblCur.ForeColor = curVer == null ? Th.Warn : Th.Dim;

            lblInstalledCount.Text = installed.Count + " 个版本";
            UpdateEnvLabel();
            UpdateButtons();

            log.Banner("扫描本机 NVM 环境");
            log.Info("NVM 目录  : " + (conf.Home.Length == 0 ? "(未配置)" : conf.Home));
            log.Info("联接路径  : " + (conf.Link.Length == 0 ? "(未配置)" : conf.Link));
            log.Info("当前版本  : " + (curVer == null ? "(未激活)" : curVer));
            if (installed.Count == 0)
                log.Warn("未扫描到任何已安装版本 —— 请检查 NVM 目录是否正确");
            else
                log.Ok("扫描到 " + installed.Count + " 个版本");

            // 已装集合变化后，让在线列表的「已装」标记跟上
            RefreshInstalledFlags();
        }

        void RefreshInstalledFlags()
        {
            HashSet<string> set = new HashSet<string>();
            foreach (VerEntry v in installed) set.Add(v.Ver);
            foreach (RelEntry r in online) r.Installed = set.Contains(r.Ver);
            // 当前版本标记
            listOnline.CurVer = curVer;
            listOnline.Invalidate();
        }

        // ------------------------------------------------------------ 在线
        void StartFetch(bool force)
        {
            if (fetching) return;
            conf.Mirror = txtMirror.Text.Trim();
            if (conf.Mirror.Length == 0)
            {
                log.Err("镜像源未配置");
                return;
            }

            HashSet<string> set = new HashSet<string>();
            foreach (VerEntry v in installed) set.Add(v.Ver);

            fetching = true;
            fetchResult = null;
            fetchTip = null;
            btnReload.Text = "获取中…";
            lblOnlineCount.Text = "正在请求…";
            UpdateButtons();
            log.Banner("拉取在线版本列表");
            log.Cmd("镜像源: " + conf.Mirror);

            fetchThread = Online.Fetch(conf.Mirror, set,
                delegate(Online.Result r) { fetchResult = r; },
                delegate(string s) { fetchTip = s; });
        }

        // ------------------------------------------------------------ 轮询
        void Poll()
        {
            // 在线列表结果
            string tip = fetchTip;
            if (tip != null && tip.Length > 0)
            {
                fetchTip = null;
                log.Info(tip);
            }

            Online.Result r = fetchResult;
            if (r != null)
            {
                fetchResult = null;
                fetching = false;
                btnReload.Text = "刷新列表";

                if (r.Ok)
                {
                    online = r.Items;
                    lblOnlineCount.Text = online.Count + " 个版本   (" + r.Ms + " ms)";
                    log.Ok("获取成功：" + online.Count + " 个版本，耗时 " + r.Ms + " ms");
                    int lts = 0;
                    foreach (RelEntry e in online) if (e.Lts.Length > 0) lts++;
                    log.Info("其中 LTS " + lts + " 个");
                    FilterOnline();
                }
                else
                {
                    lblOnlineCount.Text = "获取失败";
                    log.Err("获取失败: " + r.Error);
                    log.Dim2("请检查镜像源是否可达；国内推荐 https://cdn.npmmirror.com/binaries/node");
                }
                RefreshInstalledFlags();
                UpdateButtons();
            }

            // 写操作结果
            string wd = workDone;
            if (wd != null)
            {
                workDone = null;
                busy = false;
                log.Info(wd);
                Rescan();
                UpdateButtons();
                if (panelOnline != null && panelOnline.Visible) StartFetch(true);
            }
            string wt = workTip;
            if (wt != null)
            {
                workTip = null;
                log.Cmd(wt);
            }
        }

        void FilterOnline()
        {
            if (listOnline == null) return;
            bool ltsOnly = chkLtsOnly != null && chkLtsOnly.Checked;
            bool hideIns = chkHideInstalled != null && chkHideInstalled.Checked;

            string keep = listOnline.SelectedItem is RelEntry ? ((RelEntry)listOnline.SelectedItem).Ver : null;

            listOnline.BeginUpdate();
            listOnline.Items.Clear();
            foreach (RelEntry e in online)
            {
                if (ltsOnly && e.Lts.Length == 0) continue;
                if (hideIns && e.Installed) continue;
                listOnline.Items.Add(e);
            }
            listOnline.EndUpdate();

            if (keep != null)
            {
                for (int i = 0; i < listOnline.Items.Count; i++)
                {
                    if (((RelEntry)listOnline.Items[i]).Ver == keep) { listOnline.SelectedIndex = i; break; }
                }
            }
            else if (listOnline.Items.Count > 0)
            {
                listOnline.SelectedIndex = 0;
            }
            UpdateButtons();
        }

        // ------------------------------------------------------------ 写操作
        void RunWork(string title, Func<string> body)
        {
            if (busy) return;
            busy = true;
            workDone = null;
            log.Banner(title);
            UpdateButtons();

            workThread = new Thread(delegate()
            {
                string err;
                try { err = body(); }
                catch (Exception ex) { err = "异常: " + ex.Message; }

                if (err == null) workDone = "操作完成";
                else if (err == "已取消") workDone = "操作已取消";
                else workDone = "操作失败: " + err;
            });
            workThread.IsBackground = true;
            workThread.Start();
        }

        void DoUse()
        {
            VerEntry v = listInstalled.SelectedItem as VerEntry;
            if (v == null) return;
            if (v.Current) { log.Warn("已经在使用 " + v.Ver); return; }
            if (!NvmEnv.IsAdmin())
            {
                log.Err("需要管理员权限才能重建目录联接（请以管理员身份运行本程序）");
                return;
            }
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();

            string home = conf.Home, link = conf.Link, ver = v.Ver;
            RunWork("切换到 node " + ver, delegate()
            {
                workTip = "rmdir 旧联接 + mklink /J 新联接…";
                string e = Ops.Use(home, link, ver);
                if (e == null) workTip = "已切换到 node " + ver;
                return e;
            });
        }

        void DoUninstall()
        {
            VerEntry v = listInstalled.SelectedItem as VerEntry;
            if (v == null) return;
            if (v.Current) { log.Warn("正在使用的版本不能卸载"); return; }

            DialogResult dr = MessageBox.Show(this,
                "确定要卸载 node " + v.Ver + " 吗？\r\n\r\n目录：" + v.Dir + "\r\n此操作会永久删除该目录。",
                "确认卸载", MessageBoxButtons.OKCancel, MessageBoxIcon.Warning);
            if (dr != DialogResult.OK) return;

            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            string home = conf.Home, link = conf.Link, ver = v.Ver;

            RunWork("卸载 node " + ver, delegate()
            {
                string e = Ops.Uninstall(home, link, ver);
                if (e == null) workTip = "已卸载 node " + ver;
                return e;
            });
        }

        void DoInstall()
        {
            RelEntry r = listOnline.SelectedItem as RelEntry;
            if (r == null) return;
            conf.Home = txtHome.Text.Trim();
            conf.Mirror = txtMirror.Text.Trim();

            if (r.Installed)
            {
                DialogResult dr = MessageBox.Show(this,
                    "node " + r.Ver + " 已安装。要删除后重新安装吗？",
                    "确认重装", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
                if (dr != DialogResult.OK) return;
            }

            string home = conf.Home, mirror = conf.Mirror, ver = r.Ver;
            bool reinstall = r.Installed;

            RunWork("安装 node " + ver, delegate()
            {
                if (reinstall)
                {
                    workTip = "移除已有目录…";
                    string e0 = Ops.Uninstall(home, txtLink.Text.Trim(), ver);
                    if (e0 != null) return e0;
                }
                return Ops.Install(home, mirror, ver,
                    delegate(string s) { workTip = s; },
                    delegate() { return false; });
            });
        }
    }

    // ========================================================================
    // 命令行自检入口（GUI 没法自动化验证，所以留这些口子）
    //   --scan    [out]              扫描本机 NVM 环境
    //   --releases <mirror> [out]    拉在线列表（只跑网络那一段）
    //   --use <ver> [out]            切换版本
    //   --install <ver> [out]        安装版本
    //   --uninstall <ver> [out]      卸载版本
    //   --url <ver> <mirror> [out]   拼出下载 URL（不发请求）
    //   --resolve [out]              仅打印自动探测到的 Home/Link/Mirror
    //   --uicheck [out]              建一次窗体，验证控件布局（不进入消息循环）
    // ========================================================================
    static class Cli
    {
        static string Arg(string[] a, int i, string def)
        {
            return i < a.Length && a[i].Length > 0 ? a[i] : def;
        }

        // 参数像不像一个本地路径（用来把「输出文件」和「镜像源/版本号」分开）
        static bool LooksLikePath(string s)
        {
            if (string.IsNullOrEmpty(s)) return false;
            if (s.StartsWith("-")) return false;
            if (s.StartsWith("http", StringComparison.OrdinalIgnoreCase)) return false;
            if (Regex.IsMatch(s, @"^\d+(\.\d+)*$")) return false;
            return s.IndexOf('\\') >= 0 || s.IndexOf('/') >= 0 ||
                   s.EndsWith(".txt", StringComparison.OrdinalIgnoreCase);
        }

        // 递归遍历控件树，返回同级重叠数（把重叠的详细行写进 sb）
        static int DumpChildren(Control parent, string path, StringBuilder sb, ref int total)
        {
            int bad = 0;
            List<Control> kids = new List<Control>();
            foreach (Control c in parent.Controls) kids.Add(c);
            for (int i = 0; i < kids.Count; i++)
            {
                total++;
                Control a = kids[i];
                string tag = path + "/" + a.GetType().Name + "[" + i + "]";
                sb.AppendLine("ctrl\t" + tag + "\t" +
                              a.Left + "," + a.Top + " " + a.Width + "x" + a.Height);
                if (a.Controls.Count > 0)
                    bad += DumpChildren(a, tag, sb, ref total);

                // 只和「同级的后继兄弟」比，避免重复计数
                for (int j = i + 1; j < kids.Count; j++)
                {
                    Control b = kids[j];
                    Rectangle ra = a.Bounds, rb = b.Bounds;
                    // Panel/Dock 类容器允许重叠（互斥显示），但按钮/标签/列表之间不行
                    if (ra.IntersectsWith(rb))
                    {
                        bool bothPane = a is Panel && b is Panel;
                        if (bothPane) continue;
                        sb.AppendLine("OVERLAP\t" + tag + " ^ " +
                                      path + "/" + b.GetType().Name + "[" + j + "]" +
                                      "\t" + ra + " vs " + rb);
                        bad++;
                    }
                }
            }
            return bad;
        }

        // 从参数里挑出镜像源；挑不到就用默认。
        // 镜像源是可选参数，且必须与「输出文件」「版本号」区分开。
        static string MirrorArg(string[] a)
        {
            for (int i = 1; i < a.Length; i++)
            {
                string s = a[i];
                if (LooksLikePath(s)) continue;
                if (Regex.IsMatch(s, @"^\d+(\.\d+)*$")) continue;
                if (s.StartsWith("--")) continue;
                if (s.StartsWith("http", StringComparison.OrdinalIgnoreCase)) return s;
                // 也允许直接给域名/裸路径
                if (s.IndexOf('.') > 0) return s;
            }
            return NvmEnv.DEFAULT_MIRROR;
        }

        // 输出文件 = 最后一个「像路径」的参数。
        // 不能用固定下标：--url 的镜像源是可选参数，省略后输出文件的下标会前移。
        static string OutFile(string[] a)
        {
            for (int i = a.Length - 1; i >= 1; i--)
            {
                if (LooksLikePath(a[i])) return a[i];
            }
            return Path.Combine(Path.GetTempPath(), "nvmdesk-cli.txt");
        }

        static void Write(string path, string s)
        {
            try { File.WriteAllText(path, s ?? "", new UTF8Encoding(false)); }
            catch (Exception) { }
        }

        public static bool TryHandle(string[] args)
        {
            if (args == null || args.Length == 0) return false;
            string cmd = args[0];
            if (!cmd.StartsWith("--")) return false;

            if (cmd == "--scan")
            {
                Conf c = Conf.Load();
                string cur = NvmEnv.CurrentVer(c.Home, c.Link);
                List<VerEntry> list = Scan.Installed(c.Home, cur);
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("home=" + c.Home);
                sb.AppendLine("link=" + c.Link);
                sb.AppendLine("mirror=" + c.Mirror);
                sb.AppendLine("admin=" + NvmEnv.IsAdmin());
                sb.AppendLine("current=" + (cur == null ? "" : cur));
                sb.AppendLine("count=" + list.Count);
                foreach (VerEntry v in list)
                    sb.AppendLine(v.Ver + "\tnode=" + v.HasNode + "\t" + Fmt.Bytes(v.Bytes) + "\t" + (v.Current ? "CURRENT" : ""));
                Write(OutFile(args), sb.ToString());
                return true;
            }

            if (cmd == "--resolve")
            {
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("---- 候选目录 ----");
                foreach (string d in NvmEnv.Candidates(null))
                    sb.AppendLine((NvmEnv.LooksLikeNvm(d) ? "[OK] " : "[  ] ") + d);
                Conf c = Conf.Load();
                sb.AppendLine("---- 选定 ----");
                sb.AppendLine("home=" + c.Home);
                sb.AppendLine("link=" + c.Link);
                sb.AppendLine("mirror=" + c.Mirror);
                sb.AppendLine("nvmExe=" + (NvmEnv.FindNvmExe(c.Home) ?? "(未找到)"));
                Write(OutFile(args), sb.ToString());
                return true;
            }

            if (cmd == "--releases")
            {
                string mirror = MirrorArg(args);
                Conf c = Conf.Load();
                string cur = NvmEnv.CurrentVer(c.Home, c.Link);
                HashSet<string> set = new HashSet<string>();
                foreach (VerEntry v in Scan.Installed(c.Home, cur)) set.Add(v.Ver);

                ManualResetEvent done = new ManualResetEvent(false);
                Online.Result got = null;
                Online.Fetch(mirror, set, delegate(Online.Result r) { got = r; done.Set(); }, null);
                done.WaitOne(60000, false);

                StringBuilder sb = new StringBuilder();
                if (got == null)
                {
                    sb.AppendLine("TIMEOUT");
                }
                else
                {
                    sb.AppendLine("ok=" + got.Ok);
                    sb.AppendLine("error=" + got.Error);
                    sb.AppendLine("mirror=" + got.Mirror);
                    sb.AppendLine("ms=" + got.Ms);
                    sb.AppendLine("count=" + got.Items.Count);
                    int lts = 0, sec = 0, ins = 0;
                    foreach (RelEntry e in got.Items)
                    {
                        if (e.Lts.Length > 0) lts++;
                        if (e.Security) sec++;
                        if (e.Installed) ins++;
                    }
                    sb.AppendLine("lts=" + lts + " security=" + sec + " installed=" + ins);
                    sb.AppendLine("---- top 15 (newest) ----");
                    for (int i = 0; i < got.Items.Count && i < 15; i++)
                    {
                        RelEntry e = got.Items[i];
                        sb.AppendLine(e.Ver + "\tlts=" + e.Lts + "\tsec=" + e.Security + "\tins=" + e.Installed);
                    }
                    sb.AppendLine("---- LTS (newest 10) ----");
                    int n = 0;
                    for (int i = 0; i < got.Items.Count && n < 10; i++)
                    {
                        if (got.Items[i].Lts.Length == 0) continue;
                        sb.AppendLine(got.Items[i].Ver + "\tlts=" + got.Items[i].Lts);
                        n++;
                    }
                }
                Write(OutFile(args), sb.ToString());
                return true;
            }

            if (cmd == "--url")
            {
                string ver = Ver.Norm(Arg(args, 1, "20.2.0"));
                string mirror = MirrorArg(args);
                string baseUrl = mirror.Trim().TrimEnd('/');
                if (baseUrl.EndsWith("/index.json", StringComparison.OrdinalIgnoreCase))
                    baseUrl = baseUrl.Substring(0, baseUrl.Length - "/index.json".Length);
                string zip = baseUrl + "/v" + ver + "/node-v" + ver + "-win-x64.zip";
                Write(OutFile(args), zip + "\n");
                return true;
            }

            if (cmd == "--use")
            {
                string ver = Arg(args, 1, "");
                Conf c = Conf.Load();
                string err = Ops.Use(c.Home, c.Link, ver);
                Write(OutFile(args),
                    "home=" + c.Home + "\nlink=" + c.Link + "\nver=" + Ver.Norm(ver) +
                    "\nerr=" + (err ?? "") + "\nnow=" + (NvmEnv.CurrentVer(c.Home, c.Link) ?? "") + "\n");
                return true;
            }

            if (cmd == "--install")
            {
                string ver = Arg(args, 1, "");
                Conf c = Conf.Load();
                List<string> lines = new List<string>();
                string err = Ops.Install(c.Home, c.Mirror, ver,
                    delegate(string s) { lock (lines) { lines.Add(s); } },
                    delegate() { return false; });
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("home=" + c.Home);
                sb.AppendLine("mirror=" + c.Mirror);
                sb.AppendLine("ver=" + Ver.Norm(ver));
                sb.AppendLine("err=" + (err ?? ""));
                sb.AppendLine("---- log ----");
                lock (lines) { foreach (string l in lines) sb.AppendLine(l); }
                Write(OutFile(args), sb.ToString());
                return true;
            }

            if (cmd == "--uninstall")
            {
                string ver = Arg(args, 1, "");
                Conf c = Conf.Load();
                string err = Ops.Uninstall(c.Home, c.Link, ver);
                Write(OutFile(args),
                    "home=" + c.Home + "\nlink=" + c.Link + "\nver=" + Ver.Norm(ver) +
                    "\nerr=" + (err ?? "") + "\n");
                return true;
            }

            if (cmd == "--uicheck")
            {
                StringBuilder sb = new StringBuilder();
                try
                {
                    MainForm f = new MainForm();
                    // 未 Show() 的窗体里子控件 Visible 恒为 false，用它判布局没意义；
                    // 真正要查的是「同级控件矩形是否重叠」——那个才是真会出问题的。
                    sb.AppendLine("formOk=true");
                    sb.AppendLine("clientSize=" + f.ClientSize.Width + "x" + f.ClientSize.Height);
                    int total = 0, overlaps = 0;
                    overlaps += DumpChildren(f, "form", sb, ref total);
                    sb.AppendLine("totalControls=" + total);
                    sb.AppendLine("overlaps=" + overlaps);

                    // 两套卡片必须同位置同尺寸（它们是互斥显示的同一块区域）
                    if (f.panelInstalled != null && f.panelOnline != null)
                    {
                        bool same =
                            f.panelInstalled.Bounds == f.panelOnline.Bounds;
                        sb.AppendLine("installedOnlineCardsSameRect=" + same);
                        sb.AppendLine("installedRect=" + f.panelInstalled.Bounds);
                        sb.AppendLine("onlineRect=" + f.panelOnline.Bounds);
                    }

                    // ---- UI 阻塞验证 ----
                    // 起一次真实的在线拉取，然后在主线程上连续做 40 次「试探」，
                    // 每次记录间隔。若主线程被网络请求占住（原项目的缺陷④），
                    // 间隔会出现一次巨大的尖峰；异步化后应该全程都在个位数 ms。
                    f.StartFetchForCheck();
                    List<long> gaps = new List<long>();
                    Stopwatch sw2 = Stopwatch.StartNew();
                    long prev = 0;
                    for (int i = 0; i < 40; i++)
                    {
                        f.PollOnce();                  // 模拟 Timer 心跳
                        Application.DoEvents();        // 模拟消息泵在转
                        Thread.Sleep(50);
                        long now = sw2.ElapsedMilliseconds;
                        gaps.Add(now - prev);
                        prev = now;
                    }
                    long maxGap = 0;
                    long sumGap = 0;
                    foreach (long g in gaps) { if (g > maxGap) maxGap = g; sumGap += g; }
                    sb.AppendLine("uiProbeCount=" + gaps.Count);
                    sb.AppendLine("uiMaxGapMs=" + maxGap);
                    sb.AppendLine("uiAvgGapMs=" + (sumGap / Math.Max(1, gaps.Count)));
                    sb.AppendLine("uiFrozen=" + (maxGap > 500));
                    sb.AppendLine("fetching=" + f.IsFetching);

                    // 等它收完，确认异步真能到终点
                    for (int i = 0; i < 200 && f.IsFetching; i++)
                    {
                        f.PollOnce();                  // 模拟 Timer 心跳，把结果搬回主线程
                        Application.DoEvents();
                        Thread.Sleep(50);
                    }
                    f.PollOnce();
                    sb.AppendLine("fetchFinished=" + (!f.IsFetching));
                    sb.AppendLine("onlineCountAfter=" + f.OnlineCount);

                    f.Dispose();
                }
                catch (Exception ex)
                {
                    sb.AppendLine("formOk=false");
                    sb.AppendLine("error=" + ex.ToString());
                }
                Write(OutFile(args), sb.ToString());
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
            if (Cli.TryHandle(args)) return;

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
