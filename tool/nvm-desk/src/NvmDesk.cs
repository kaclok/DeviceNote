// ============================================================================
//  NVMSwitcher  ——  nvm-desk 的修复重写版（产物固定叫 NVMSwitcher.exe）
//
//  原项目 https://gitee.com/code-ape-hc/nvm-desk 是一份 PowerShell + WinForms
//  脚本（nvm_manager.ps1，25072 B）。本文件**沿用原项目的浅色 UI 风格**
//  （白色背景 / 大标题 / Current Environment 卡片 / 蓝色选中标签），
//  并在其上补上 NVM / NVM_SYMLINK / CDN 三项配置的浏览与编辑，
//  最后再加一个 `.nvmrc` 页：选定项目根目录，写入 / 读取项目级版本锁定文件。
//
//  逐条修掉的缺陷（行号对应原 nvm_manager.ps1）：
//
//  ① 第 7 行 `$NvmRoot = "D:\Program Files\Roaming\nvm"` —— 硬编码。
//     别人机器上该目录不存在 ⇒ 已安装列表永远空白。
//     → 修：NvmEnv.Candidates() 按「用户配置 → NVM_HOME 环境变量 → 注册表
//       HKCU/HKLM 的 Environment → 从 nvm.exe 反推 → 常见位置兜底」逐级探测；
//       并在 Settings 页给用户可编辑 + 可浏览的路径框。
//
//  ② 第 21 行 `Sort-Object { [Version]($_.TrimStart("v")) } -Descending`。
//     目录里混进 "22.14.0-rc.1" 这类名字时 [Version] 强转抛异常，
//     一条抛错导致整份排序失败（列表再次空白）。
//     → 修：Ver.Cmp() 逐段比较，非版本号沉底、尾缀降级字符串比较，永不抛。
//
//  ③ 第 43-51 行 从 `<NvmRoot>\settings.txt` 正则抠 node_mirror，
//     抠不到就静默退回 nodejs.org ⇒ 国内网络下慢到像卡死。
//     → 修：CDN 镜像源升为一等配置项，默认 cdn.npmmirror.com，Settings 页可改。
//
//  ④ 第 52 行 `Invoke-RestMethod -TimeoutSec 15` 在 Build-UI 里**同步**执行。
//     拉网络期间整个窗口不响应，标签切不动。
//     → 修：后台线程 + HttpWebRequest，主线程 100ms Timer 轮询取回结果。
//
//  ⑤ 第 175/190/312/348/473 五处 `$Global:MainForm.Controls.Clear()` 之后
//     全量重建所有控件 —— 每次操作都闪，重建期间事件还会丢。
//     → 修：控件只建一次，靠 Visible / 数据刷新驱动，绝不 Clear。
//
//  ⑥ 第 518 行 `$form.Add_Resize({ $Global:MainForm.Refresh() })` —— 每帧全窗重绘。
//     → 修：双缓冲 + Anchor，Resize 里不做任何重绘。
//
//  ⑦ 第 305/341/457 行 `Start-Process -FilePath "nvm" ... -Verb RunAs`
//     依赖 PATH 里恰好有 nvm.exe，且每次操作都弹 UAC。
//     → 修：manifest 里 requireAdministrator 一次提权；nvm 路径从配置取。
//
//  ⑧ 原项目只有「已安装 / 在线」两页，没有项目维度的版本锁定。
//     → 新增 `.nvmrc` 页：Browse 选定项目根目录；Write .nvmrc 把选中版本
//       写进 <根目录>\.nvmrc（等同 `node -v > .nvmrc`，带 v 前缀）；
//       Read .nvmrc & Switch 读该文件并把 node 切到其中的版本。
//       读取侧支持 v20.2.0 / 20.2.0 / 20.2 / 20 这些写法，以及 lts/*、
//       lts/<代号>、node 这些别名（别名会联网从镜像源解析成具体版本）。
//
//  UI 层刻意**不用** Shared.cs 的深色主题（那是 dev-launcher 的观感），
//  只复用它的 Sys（Which/OpenFolder）与 Cfg（ini 持久化）两项工具。
// ============================================================================

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;

// Shared.cs 里的 Sys / Cfg 全在 DevLaunch 命名空间下，所以本文件也挂同一命名空间。
namespace DevLaunch
{
    // ========================================================================
    // 浅色主题（对齐原 nvm-desk 的观感：白底 + 灰色描边 + 蓝色选中标签）
    // ========================================================================
    static class Lt
    {
        public static readonly Color Bg = Color.White;
        public static readonly Color Fg = Color.FromArgb(33, 33, 33);
        public static readonly Color Dim = Color.FromArgb(150, 150, 150);
        public static readonly Color Border = Color.FromArgb(219, 219, 219);
        public static readonly Color Hover = Color.FromArgb(243, 246, 250);
        public static readonly Color Accent = Color.FromArgb(43, 127, 227);
        public static readonly Color AccHov = Color.FromArgb(28, 110, 208);
        public static readonly Color Green = Color.FromArgb(30, 158, 78);
        public static readonly Color Teal = Color.FromArgb(20, 160, 160);
        public static readonly Color Warn = Color.FromArgb(198, 128, 20);
        public static readonly Color Err = Color.FromArgb(200, 60, 60);
        public static readonly Color SelBg = Color.FromArgb(230, 240, 253);
        public static readonly Color BtnBg = Color.FromArgb(246, 246, 246);
        public static readonly Color BtnBd = Color.FromArgb(208, 208, 208);

        public static readonly Font Title = F("Microsoft YaHei UI", 18F, FontStyle.Bold);
        public static readonly Font Sub = F("Microsoft YaHei UI", 9.5F, FontStyle.Regular);
        public static readonly Font H = F("Microsoft YaHei UI", 9.5F, FontStyle.Bold);
        public static readonly Font Ui = F("Microsoft YaHei UI", 9.5F, FontStyle.Regular);
        public static readonly Font Big = F("Microsoft YaHei UI", 11F, FontStyle.Bold);
        public static readonly Font Small = F("Microsoft YaHei UI", 8.5F, FontStyle.Regular);
        public static readonly Font Mono = F("Consolas", 9.5F, FontStyle.Regular);

        // 字体族缺失时不能让整个类型加载失败
        static Font F(string family, float size, FontStyle style)
        {
            try { return new Font(family, size, style); }
            catch (Exception) { return new Font(FontFamily.GenericSansSerif, size, style); }
        }
    }

    // 白底 + 1px 浅灰描边的卡片（截图里 Current Environment 与内容区都是这个样式）
    class LCard : Panel
    {
        public LCard()
        {
            SetStyle(ControlStyles.UserPaint | ControlStyles.AllPaintingInWmPaint
                   | ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw, true);
            BackColor = Lt.Bg;
            ForeColor = Lt.Fg;
            Font = Lt.Ui;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            // AllPaintingInWmPaint 下 WM_ERASEBKGND 被忽略，必须自己填底
            using (SolidBrush b = new SolidBrush(BackColor))
                e.Graphics.FillRectangle(b, ClientRectangle);
            using (Pen p = new Pen(Lt.Border))
                e.Graphics.DrawRectangle(p, 0, 0, Width - 1, Height - 1);
        }
    }

    // 普通按钮（浅色）
    class LBtn : Button
    {
        bool _primary;

        public bool Primary
        {
            get { return _primary; }
            set { _primary = value; Apply(); }
        }

        void Apply()
        {
            BackColor = _primary ? Lt.Accent : Lt.BtnBg;
            ForeColor = _primary ? Color.White : Lt.Fg;
            FlatAppearance.BorderColor = _primary ? Lt.Accent : Lt.BtnBd;
            FlatAppearance.MouseOverBackColor = _primary ? Lt.AccHov : Lt.Hover;
            FlatAppearance.MouseDownBackColor = _primary ? Lt.AccHov : Lt.Hover;
            Invalidate();
        }

        public LBtn()
        {
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 1;
            Font = Lt.Ui;
            Height = 30;
            Cursor = Cursors.Hand;
            UseVisualStyleBackColor = false;
            TabStop = false;
            Apply();
        }

        protected override void OnEnabledChanged(EventArgs e)
        {
            base.OnEnabledChanged(e);
            // 禁用态统一走灰底灰字，别沿用主色
            if (!Enabled)
            {
                BackColor = Color.FromArgb(240, 240, 240);
                ForeColor = Lt.Dim;
                FlatAppearance.BorderColor = Lt.Border;
            }
            else Apply();
        }
    }

    // 标签页按钮：选中 = 蓝底白字，未选中 = 白底灰字
    class LTab : Button
    {
        bool _on;

        public bool On
        {
            get { return _on; }
            set { _on = value; Apply(); }
        }

        void Apply()
        {
            BackColor = _on ? Lt.Accent : Lt.Bg;
            ForeColor = _on ? Color.White : Lt.Dim;
            FlatAppearance.BorderColor = _on ? Lt.Accent : Lt.Border;
            FlatAppearance.MouseOverBackColor = _on ? Lt.AccHov : Lt.Hover;
            FlatAppearance.MouseDownBackColor = _on ? Lt.AccHov : Lt.Hover;
            Invalidate();
        }

        public LTab()
        {
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 1;
            Font = Lt.Ui;
            Height = 32;
            Cursor = Cursors.Hand;
            UseVisualStyleBackColor = false;
            TabStop = false;
            Apply();
        }
    }

    // ========================================================================
    // 版本号：解析 / 比较 / 排序（对应缺陷 ②）
    // ========================================================================
    static class Ver
    {
        public static string Norm(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            s = s.Trim();
            if (s.Length > 0 && (s[0] == 'v' || s[0] == 'V')) s = s.Substring(1);
            return s;
        }

        // 拆成可比较段；返回 false 表示这不是规范的 x.y.z
        public static bool Parse(string s, out int[] nums, out string tail)
        {
            nums = new int[0];
            tail = "";
            s = Norm(s);
            if (s.Length == 0) return false;

            int i = 0;
            List<int> got = new List<int>();
            while (i < s.Length)
            {
                int j = i;
                while (j < s.Length && char.IsDigit(s[j])) j++;
                if (j == i) break;
                int n;
                if (!int.TryParse(s.Substring(i, j - i), out n)) n = 0;
                got.Add(n);
                i = j;
                if (i < s.Length && s[i] == '.') { i++; continue; }
                break;
            }
            if (got.Count == 0) return false;
            nums = got.ToArray();
            tail = i < s.Length ? s.Substring(i) : "";     // 例如 "-rc.1"
            return true;
        }

        // 永不抛异常的比较：数字段按值比，非版本号沉底，正式版 > 预览版
        public static int Cmp(string a, string b)
        {
            int[] na, nb;
            string ta, tb;
            bool oka = Parse(a, out na, out ta);
            bool okb = Parse(b, out nb, out tb);

            if (!oka && !okb) return string.Compare(a, b, StringComparison.OrdinalIgnoreCase);
            if (!oka) return -1;
            if (!okb) return 1;

            int len = na.Length > nb.Length ? na.Length : nb.Length;
            for (int i = 0; i < len; i++)
            {
                int x = i < na.Length ? na[i] : 0;
                int y = i < nb.Length ? nb[i] : 0;
                if (x != y) return x > y ? 1 : -1;
            }
            if (ta.Length == 0 && tb.Length == 0) return 0;
            if (ta.Length == 0) return 1;
            if (tb.Length == 0) return -1;
            return string.Compare(ta, tb, StringComparison.OrdinalIgnoreCase);
        }
    }

    // ========================================================================
    // 环境探测：NVM / NVM_SYMLINK / CDN 镜像源（对应缺陷 ① ③）
    // ========================================================================
    static class NvmEnv
    {
        public const string DEFAULT_MIRROR = "https://cdn.npmmirror.com/binaries/node";
        public const string OFFICIAL_MIRROR = "https://nodejs.org/dist";

        // 起手打开 TLS1.2：.NET 4.0 默认只认 TLS1.0，访问 https 镜像会直接失败
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
                    if (!string.IsNullOrEmpty(s))
                        return Environment.ExpandEnvironmentVariables(s).TrimEnd('\\');
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
                if (!string.IsNullOrEmpty(v))
                    return Environment.ExpandEnvironmentVariables(v).TrimEnd('\\');
            }
            catch (Exception) { }
            return null;
        }

        // ---------- 从 settings.txt 读 root / path / mirror ----------
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

        // ---------- 探测 NVM 根目录（对应缺陷 ①）----------
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

            string nvmExe = FindNvmExe(explicitHome);
            if (nvmExe != null) add(Path.GetDirectoryName(nvmExe));

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

        // 只看顶层，不递归
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

        // 一个目录「像不像」NVM_HOME
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

        // ---------- 探测 NVM_SYMLINK ----------
        public static string DetectLink(string nvmDir)
        {
            string v = Env("NVM_SYMLINK");
            if (!string.IsNullOrEmpty(v)) return v;
            v = RegEnv("NVM_SYMLINK");
            if (!string.IsNullOrEmpty(v)) return v;

            Dictionary<string, string> st = ReadSettings(nvmDir);
            if (st.ContainsKey("path") && st["path"].Length > 0)
                return st["path"].TrimEnd('\\');

            if (!string.IsNullOrEmpty(nvmDir))
            {
                DirectoryInfo p = Directory.GetParent(nvmDir);
                if (p != null) return Path.Combine(p.FullName, "nodejs");
            }
            return null;
        }

        // 目录联接指向哪里（null = 不是联接 / 不存在）
        // .NET 4.0 的 DirectoryInfo 没有 LinkTarget（那是 .NET 6+），
        // 所以走 `dir /al <父目录>`，输出形如：
        //     2026/10/14  08:16    <JUNCTION>     nodejs [E:\NVM\nvm\v22.14.0]
        // 方括号里就是目标，格式与系统语言无关。
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
        public static string CurrentVer(string nvmDir, string link)
        {
            string t = LinkTarget(link);
            if (!string.IsNullOrEmpty(t))
            {
                string n = Path.GetFileName(t);
                if (!string.IsNullOrEmpty(n) && char.IsDigit(n.TrimStart('v', 'V')[0]))
                    return Ver.Norm(n);
            }
            if (!string.IsNullOrEmpty(link))
            {
                string exe = Path.Combine(link, "node.exe");
                if (File.Exists(exe))
                {
                    string o = (Sh.RunOut(exe, "-v", null, 10) ?? "").Trim();
                    if (o.Length > 0) return Ver.Norm(o.Split('\n')[0].Trim());
                }
            }
            string w = Sys.Which("node");
            if (!string.IsNullOrEmpty(w))
            {
                string o = (Sh.RunOut(w, "-v", null, 10) ?? "").Trim();
                if (o.Length > 0) return Ver.Norm(o.Split('\n')[0].Trim());
            }
            return null;
        }

        public static string NpmVer(string nvmDir, string link)
        {
            if (string.IsNullOrEmpty(link)) return null;
            string exe = Path.Combine(link, "npm.cmd");
            if (!File.Exists(exe)) return null;
            string o = (Sh.RunOut(exe, "-v", null, 20) ?? "").Trim();
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
        static readonly Encoding Oem = Encoding.GetEncoding(936);

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

        public static string RunOut(string cmd, string args, string workDir, int timeoutSec)
        {
            string o;
            RunIn(workDir, cmd, args, out o, timeoutSec);
            return o;
        }

        public static int CmdIn(string workDir, string line, out string out2, int timeoutSec)
        {
            return RunIn(workDir, "cmd.exe", "/d /s /c \"" + line + "\"", out out2, timeoutSec);
        }

        public static int Cmd(string line, out string out2)
        {
            return CmdIn(null, line, out out2, 300);
        }

        public static string CmdOut(string line, int timeoutSec)
        {
            string o;
            CmdIn(null, line, out o, timeoutSec);
            return o;
        }
    }

    // ========================================================================
    // 数据模型
    // ========================================================================
    class VerEntry
    {
        public string Raw = "";          // 目录名，例如 "v20.2.0"
        public string Ver = "";          // "20.2.0"
        public string Dir = "";
        public bool HasNode;
        public long Bytes;
        public bool Current;

        // 列表是 owner-draw 的，不靠 ToString；这里只给 ComboBox 用
        public override string ToString()
        {
            return "v" + Ver + (Current ? "   (current)" : "") + (HasNode ? "" : "   (missing node.exe)");
        }
    }

    class RelEntry
    {
        public string Ver = "";
        public string Lts = "";          // LTS 代号，空 = 非 LTS
        public bool Security;
        public bool Installed;
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
    // 项目级版本锁定文件 .nvmrc（对应新增缺陷 ⑧）
    //   写：<项目根目录>\.nvmrc 一行内容，等价 `node -v > .nvmrc`
    //   读：取首个非空行、剥掉 # 注释与 v 前缀，得到 token
    //       数值 token（20.2.0 / 20.2 / 20）按「段前缀」匹配本机已装版本，
    //       关键字 token（lts/*、lts/<代号>、node/stable/latest）交给在线列表解析。
    // ========================================================================
    static class NvmRc
    {
        public const string NAME = ".nvmrc";

        public static string PathOf(string dir)
        {
            if (string.IsNullOrEmpty(dir)) return NvmRc.NAME;
            return Path.Combine(dir, NvmRc.NAME);
        }

        public static bool Exists(string dir)
        {
            if (string.IsNullOrEmpty(dir)) return false;
            try { return File.Exists(PathOf(dir)); }
            catch (Exception) { return false; }
        }

        // 首个非空行 → 剥注释 → 剥 v 前缀。文件不存在/读不到返回 null。
        public static string ReadToken(string dir)
        {
            if (string.IsNullOrEmpty(dir)) return null;
            try
            {
                string p = PathOf(dir);
                if (!File.Exists(p)) return null;
                foreach (string raw in File.ReadAllLines(p, Encoding.UTF8))
                {
                    string line = raw;
                    int h = line.IndexOf('#');
                    if (h >= 0) line = line.Substring(0, h);
                    line = line.Trim();
                    if (line.Length == 0) continue;
                    return Ver.Norm(line);                  // 同时剥掉 v / V 前缀
                }
                return "";                                  // 文件存在但没有内容
            }
            catch (Exception) { return null; }
        }

        // 写一行 "v20.2.0"。返回 null 表示成功，否则是给用户看的错误文本。
        public static string Write(string dir, string ver)
        {
            if (string.IsNullOrEmpty(dir)) return "目录未指定";
            if (!Directory.Exists(dir)) return "目录不存在: " + dir;
            string v = Ver.Norm(ver);
            int[] nums; string tail;
            if (v.Length == 0 || !Ver.Parse(v, out nums, out tail) || tail.Length > 0)
                return "不是合法的版本号: " + (ver ?? "");

            string p = PathOf(dir);
            try
            {
                // 用无 BOM 的 UTF-8 + CRLF，与 nvm 生态里 `node -v > .nvmrc` 的产出一致
                File.WriteAllText(p, "v" + v + "\r\n", new UTF8Encoding(false));
                return null;
            }
            catch (Exception ex) { return ex.Message; }
        }

        // 是不是别名关键字（需要在线列表才能解析）
        public static bool IsKeyword(string token)
        {
            if (string.IsNullOrEmpty(token)) return false;
            string t = token.Trim().ToLowerInvariant();
            if (t == "node" || t == "stable" || t == "latest") return true;
            if (t.StartsWith("lts")) return true;           // lts/*、lts/hydrogen
            return false;
        }

        // 数值 token → 本机已装版本里最新的匹配项。匹配不上返回 null，why 里是原因。
        public static string MatchInstalled(string token, List<VerEntry> installed, out string why)
        {
            why = "";
            int[] want; string tail;
            if (!Ver.Parse(token, out want, out tail) || tail.Length > 0)
            {
                why = token + " 不是版本号，也不是已知别名（支持 20 / 20.2 / 20.2.0 / lts/* / node）";
                return null;
            }
            string best = null;
            if (installed != null)
            {
                foreach (VerEntry e in installed)
                {
                    int[] have; string t2;
                    if (!Ver.Parse(e.Ver, out have, out t2)) continue;
                    if (have.Length < want.Length) continue;
                    bool ok = true;
                    for (int i = 0; i < want.Length; i++)
                        if (have[i] != want[i]) { ok = false; break; }
                    if (!ok) continue;
                    if (best == null || Ver.Cmp(e.Ver, best) > 0) best = e.Ver;
                }
            }
            if (best == null) why = "本机已装版本里没有 " + token + " 或它的后继版本";
            return best;
        }

        // 关键字 token → 在线列表里的具体版本（online 已按新版本在前排好）
        public static string MatchKeyword(string token, List<RelEntry> online)
        {
            if (online == null || online.Count == 0) return null;
            string t = token.Trim().ToLowerInvariant();
            if (t.StartsWith("lts/") && t.Length > 4)
            {
                string name = t.Substring(4);
                if (name != "*")
                {
                    foreach (RelEntry e in online)
                        if (string.Equals(e.Lts, name, StringComparison.OrdinalIgnoreCase)) return e.Ver;
                    return null;
                }
            }
            if (t.StartsWith("lts"))
            {
                foreach (RelEntry e in online)
                    if (e.Lts.Length > 0) return e.Ver;
                return null;
            }
            return online[0].Ver;                           // node / stable / latest
        }
    }

    // ========================================================================
    // 版本列表 ListBox（owner-draw，浅色）
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
            BackColor = Lt.Bg;
            ForeColor = Lt.Fg;
            Font = Lt.Ui;
            ItemHeight = 26;
            IntegralHeight = false;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= Items.Count) return;
            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            Rectangle r = e.Bounds;
            using (SolidBrush b = new SolidBrush(sel ? Lt.SelBg : Lt.Bg))
                e.Graphics.FillRectangle(b, r);

            if (Kind == Mode.Installed)
            {
                VerEntry v = Items[e.Index] as VerEntry;
                if (v == null) return;

                // 当前版本用圆点 + 绿色标出
                TextRenderer.DrawText(e.Graphics, v.Current ? "●" : "○", Lt.Ui,
                    new Point(r.X + 6, r.Y + 6), v.Current ? Lt.Green : Lt.Border);
                Color vc = v.Current ? Lt.Green : (v.HasNode ? Lt.Fg : Lt.Warn);
                TextRenderer.DrawText(e.Graphics, v.Ver, Lt.Big,
                    new Point(r.X + 26, r.Y + 3), vc);
                if (v.Current)
                    TextRenderer.DrawText(e.Graphics, "current", Lt.Small,
                        new Point(r.X + 26 + TextRenderer.MeasureText(v.Ver, Lt.Big).Width + 8, r.Y + 8),
                        Lt.Green);

                string right = v.HasNode ? Fmt.Bytes(v.Bytes) : "missing node.exe";
                Color rc = v.HasNode ? Lt.Dim : Lt.Warn;
                Size sz = TextRenderer.MeasureText(right, Lt.Small);
                TextRenderer.DrawText(e.Graphics, right, Lt.Small,
                    new Point(r.Right - sz.Width - 8, r.Y + 8), rc);
            }
            else
            {
                RelEntry v = Items[e.Index] as RelEntry;
                if (v == null) return;
                bool cur = v.Ver == CurVer;
                TextRenderer.DrawText(e.Graphics, v.Ver, Lt.Big,
                    new Point(r.X + 10, r.Y + 3), cur ? Lt.Green : Lt.Fg);

                string right = "";
                Color rc = Lt.Dim;
                if (v.Lts.Length > 0) { right = "LTS " + v.Lts; rc = Lt.Teal; }
                if (v.Security) { right = right.Length > 0 ? right + "  ⚠" : "⚠"; rc = Lt.Warn; }
                if (v.Installed) { right = right.Length > 0 ? right + "  installed" : "installed"; rc = Lt.Green; }
                if (cur) { right = right.Length > 0 ? right + "  current" : "current"; rc = Lt.Green; }
                if (right.Length > 0)
                {
                    Size sz = TextRenderer.MeasureText(right, Lt.Small);
                    TextRenderer.DrawText(e.Graphics, right, Lt.Small,
                        new Point(r.Right - sz.Width - 8, r.Y + 8), rc);
                }
            }
        }
    }

    // ========================================================================
    // 版本下拉框（owner-draw，浅色，与列表同一套配色）
    // ========================================================================
    class VerCombo : ComboBox
    {
        public VerCombo()
        {
            DropDownStyle = ComboBoxStyle.DropDownList;
            DrawMode = DrawMode.OwnerDrawFixed;
            FlatStyle = FlatStyle.Flat;
            ItemHeight = 22;
            DropDownHeight = 300;
            BackColor = Lt.Bg;
            ForeColor = Lt.Fg;
            Font = Lt.Ui;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            if (e.Index < 0 || e.Index >= Items.Count) return;
            VerEntry v = Items[e.Index] as VerEntry;
            if (v == null) return;

            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            bool edit = (e.State & DrawItemState.ComboBoxEdit) == DrawItemState.ComboBoxEdit;
            Rectangle r = e.Bounds;

            using (SolidBrush b = new SolidBrush(sel && !edit ? Lt.SelBg : Lt.Bg))
                e.Graphics.FillRectangle(b, r);

            Color vc = v.Current ? Lt.Green : (v.HasNode ? Lt.Fg : Lt.Warn);
            TextRenderer.DrawText(e.Graphics, "v" + v.Ver, Lt.Ui,
                new Point(r.X + 6, r.Y + (r.Height - Lt.Ui.Height) / 2), vc);
            if (v.Current)
                TextRenderer.DrawText(e.Graphics, "current", Lt.Small,
                    new Point(r.X + 6 + TextRenderer.MeasureText("v" + v.Ver, Lt.Ui).Width + 10,
                              r.Y + (r.Height - Lt.Small.Height) / 2), Lt.Green);
        }
    }

    // ========================================================================
    // 本地已安装版本扫描
    // 铁律：只读目录名 + 探 node.exe，绝不递归（递归一个版本目录要 90s+）
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
                    if (tail.Length > 0) continue;         // 预览版目录名先不收，避免误删
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

            // 新版本在前（原项目用的就是 -Descending）
            res.Sort(delegate(VerEntry a, VerEntry b) { return Ver.Cmp(b.Ver, a.Ver); });
            return res;
        }

        // 只算顶层文件 + node_modules 一层
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
    // 在线版本：拉 index.json（对应缺陷 ④ —— 后台线程，UI 绝不冻）
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
                    if (string.IsNullOrEmpty(mirror)) throw new Exception("CDN 镜像源未配置");
                    string baseUrl = Unwarp(mirror);
                    string url = baseUrl + "/index.json";

                    if (progress != null) progress("GET " + url);
                    string body = Http(url, 30000);
                    if (progress != null) progress("received " + (body.Length / 1024) + " KB, parsing…");

                    List<RelEntry> list = ParseIndex(body, installed);
                    if (list.Count == 0) throw new Exception("index.json 里没有解析出任何版本");
                    // 新版本在前
                    list.Sort(delegate(RelEntry a, RelEntry b) { return Ver.Cmp(b.Ver, a.Ver); });
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

        // 用户可能填到 /dist/、可能填到 index.json，统一剥成 base
        public static string Unwarp(string mirror)
        {
            string b = (mirror ?? "").Trim().TrimEnd('/');
            if (b.EndsWith("/index.json", StringComparison.OrdinalIgnoreCase))
                b = b.Substring(0, b.Length - "/index.json".Length);
            return b;
        }

        public static string Http(string url, int timeoutMs)
        {
            HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
            req.Method = "GET";
            req.UserAgent = "NVMSwitcher/1.0 (+WinForms; .NET4.0)";
            req.Timeout = timeoutMs;
            req.ReadWriteTimeout = timeoutMs;
            req.AllowAutoRedirect = true;
            req.AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate;
            using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
            using (Stream s = resp.GetResponseStream())
            using (StreamReader sr = new StreamReader(s, Encoding.UTF8))
                return sr.ReadToEnd();
        }

        // index.json 是扁平数组，.NET 4.0 没有内置 JSON，手工扫对象即可
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
                if (ver.IndexOf('-') >= 0) continue;       // 排除 nightly / rc

                RelEntry e = new RelEntry();
                e.Ver = Ver.Norm(ver);
                e.Lts = Lts(o);
                e.Security = Regex.IsMatch(o, @"""security""\s*:\s*true", RegexOptions.IgnoreCase);
                e.Installed = installed != null && installed.Contains(e.Ver);
                list.Add(e);
            }
            return list;
        }

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
    // 写操作：切换 / 安装 / 卸载（对应缺陷 ⑦）
    // 不调 nvm.exe；自己做等价动作：
    //   nvm use     ≡ rmdir 联接 + mklink /J 重建
    //   nvm install ≡ 下 zip + bsdtar 解压拍平到 <NVM_HOME>\vX
    // ========================================================================
    static class Ops
    {
        public static string TarExe()
        {
            string p = Path.Combine(Environment.SystemDirectory, "tar.exe");
            return File.Exists(p) ? p : "tar.exe";
        }

        public static string Use(string nvmDir, string link, string ver)
        {
            string target = Path.Combine(nvmDir, "v" + Ver.Norm(ver));
            if (!Directory.Exists(target)) return "目标版本目录不存在: " + target;
            if (!File.Exists(Path.Combine(target, "node.exe")))
                return "该版本目录下没有 node.exe，安装不完整: " + target;
            if (string.IsNullOrEmpty(link)) return "未配置 NVM_SYMLINK 路径";

            StopNode();

            // rmdir 对 junction 只摘链接，不动目标内容
            if (Directory.Exists(link))
            {
                string o;
                int rc = Sh.Cmd("rmdir \"" + link + "\"", out o);
                if (rc != 0 && Directory.Exists(link)) return "删除旧联接失败: " + o;
            }
            else if (File.Exists(link)) return "目标路径被文件占用: " + link;

            string parent = Path.GetDirectoryName(link);
            if (!string.IsNullOrEmpty(parent) && !Directory.Exists(parent))
                Directory.CreateDirectory(parent);

            string out2;
            int c = Sh.Cmd("mklink /J \"" + link + "\" \"" + target + "\"", out out2);
            if (c != 0 || !Directory.Exists(link)) return "创建联接失败: " + out2;

            string now = NvmEnv.CurrentVer(nvmDir, link);
            if (now != Ver.Norm(ver))
                return "切换后校验失败：期望 " + Ver.Norm(ver) + "，实际 " + (now == null ? "(未知)" : now);
            return null;
        }

        // 切换前结束占用 node.exe 的进程，否则文件被锁
        static void StopNode()
        {
            try
            {
                foreach (Process p in Process.GetProcessesByName("node"))
                {
                    try { p.Kill(); p.WaitForExit(2000); }
                    catch (Exception) { }
                }
            }
            catch (Exception) { }
        }

        public static string Uninstall(string nvmDir, string link, string ver)
        {
            string target = Path.Combine(nvmDir, "v" + Ver.Norm(ver));
            if (!Directory.Exists(target)) return "版本目录不存在: " + target;

            string t = NvmEnv.LinkTarget(link);
            if (!string.IsNullOrEmpty(t) &&
                string.Equals(t.TrimEnd('\\'), target.TrimEnd('\\'), StringComparison.OrdinalIgnoreCase))
                return "该版本正在使用中，请先切换到其他版本再卸载";

            try
            {
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

        // 先下到同盘临时目录再改名，避免跨盘 Move 变成真拷贝
        public static string Install(string nvmDir, string mirror, string ver,
                                     Action<string> log, Func<bool> cancelled)
        {
            string v = Ver.Norm(ver);
            string dest = Path.Combine(nvmDir, "v" + v);
            if (Directory.Exists(dest)) return "该版本已存在: " + dest;

            string url = Online.Unwarp(mirror) + "/v" + v + "/node-v" + v + "-win-x64.zip";

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
                    if (File.Exists(Path.Combine(tmpRoot, "node.exe"))) inner = tmpRoot;
                    else return "解压结果里找不到 node.exe";
                }
                if (!File.Exists(Path.Combine(inner, "node.exe")))
                    return "解压结果里找不到 node.exe（目录结构异常）";

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

        static void Download(string url, string dest, Action<string> log, Func<bool> cancelled)
        {
            HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
            req.Method = "GET";
            req.UserAgent = "NVMSwitcher/1.0 (+WinForms; .NET4.0)";
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
                int lastBucket = -1;
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
                        if (pct / 10 != lastBucket)
                        {
                            lastBucket = pct / 10;
                            log("  " + pct + "%  " + Fmt.Bytes(got) + " / " + Fmt.Bytes(total));
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // 配置持久化
    // ========================================================================
    class Conf
    {
        public string Home = "";                       // NVM
        public string Link = "";                       // NVM_SYMLINK
        public string Mirror = NvmEnv.DEFAULT_MIRROR;  // CDN
        public string Project = "";                    // .nvmrc 页选中的项目根目录
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
            if (d.TryGetValue("project", out v) && v.Length > 0) c.Project = v;
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
            d["project"] = Project ?? "";
            d["lastTab"] = LastTab ?? "installed";
            Cfg.Save(APP, d);
        }
    }

    // ========================================================================
    // 主窗体
    //
    //  浅色布局（对齐原 nvm-desk 截图）：
    //    NVMSwitcher
    //    ┌ Current Environment ────────────────────────┐
    //    │ Node.js   v22.14.0       npm   11.11.0      │
    //    └─────────────────────────────────────────────┘
    //    [Installed] [Available Online] [Settings] [.nvmrc]
    //    ┌─────────────────────────────────────────────┐
    //    │            No Node.js versions detected     │
    //    └─────────────────────────────────────────────┘
    //
    //  关键设计（对应缺陷 ⑤⑥）：
    //    * 所有控件只建一次，之后只改内容与 Visible，绝不 Controls.Clear()
    //    * 网络请求全在后台线程，主线程靠 100ms Timer 轮询结果
    // ========================================================================
    class MainForm : Form
    {
        Conf conf;

        // 顶部环境卡
        Label lblNodeVal, lblNpmVal;

        // 标签
        LTab tabInstalled, tabOnline, tabSettings, tabProject;
        Label lblTabHint;

        // 四个内容页（互斥显示，同一块矩形）
        internal LCard paneInstalled, paneOnline, paneSettings, paneProject;

        // Installed 页
        VerListBox listInstalled;
        Label lblInsEmpty;
        LBtn btnUse, btnUninstall, btnOpenLink;

        // Available Online 页
        VerListBox listOnline;
        Label lblOnEmpty, lblOnCount;
        LBtn btnInstall, btnReload;
        CheckBox chkLtsOnly, chkHideInstalled;

        // Settings 页
        TextBox txtHome, txtLink, txtMirror;
        Label lblHomeHint, lblLinkHint, lblMirrorHint;
        LBtn btnBrowseHome, btnBrowseLink, btnDefaultMirror;
        LBtn btnSave, btnRescan, btnOpenHome, btnCopyCmds;

        // .nvmrc 页
        TextBox txtProject;
        VerCombo cboProject;
        Label lblProjectHint, lblVerHint, lblProjInfo;
        LBtn btnBrowseProject, btnProjectCurrent, btnWriteNvmrc, btnReadNvmrc, btnOpenProject;

        // 底部状态栏
        Label lblStatus;

        List<VerEntry> installed = new List<VerEntry>();
        List<RelEntry> online = new List<RelEntry>();
        string curVer = null;
        string npmVer = null;

        Thread fetchThread;
        volatile bool fetching;
        volatile Online.Result fetchResult;
        volatile string fetchTip;
        System.Windows.Forms.Timer poller;

        volatile bool busy;
        Thread workThread;
        volatile string workDone;
        volatile string workTip;

        const int PAD = 22;
        const int WIN_W = 720;
        const int WIN_H = 620;
        const int CARDW = WIN_W - PAD * 2;   // 676
        const int BTNW = 146;
        const int TABW = 124;
        const int TABGAP = 6;

        const string INFO_IDLE =
            "选定项目根目录后：\r\n" +
            "· Write .nvmrc —— 把上面选中的版本写进 <根目录>\\.nvmrc（等同 node -v > .nvmrc）\r\n" +
            "· Read .nvmrc & Switch —— 读取该文件，并把 node 切换到其中的版本\r\n" +
            "\r\n" +
            "读取侧认这些写法：v20.2.0 / 20.2.0 / 20.2 / 20（取该系列最新已装），\r\n" +
            "以及 lts/*、lts/<代号>、node 这些别名（会联网从 CDN 解析成具体版本）。";

        public MainForm()
        {
            conf = Conf.Load();

            Text = "NVMSwitcher";
            ClientSize = new Size(WIN_W, WIN_H);
            MinimumSize = new Size(WIN_W, 560);
            BackColor = Lt.Bg;
            ForeColor = Lt.Fg;
            Font = Lt.Ui;
            StartPosition = FormStartPosition.CenterScreen;
            DoubleBuffered = true;

            try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); }
            catch (Exception) { }

            BuildUi();
            SwitchTab(conf.LastTab, false);
            Rescan();

            poller = new System.Windows.Forms.Timer();
            poller.Interval = 100;
            poller.Tick += delegate { Poll(); };
            poller.Start();
        }

        // ------------------------------------------------------------ 构建 UI
        void BuildUi()
        {
            int H = ClientSize.Height;

            // ===== 标题 =====
            Label title = MkLabel("NVMSwitcher", Lt.Title, Lt.Fg);
            title.SetBounds(PAD, 16, 240, 38);
            title.TextAlign = ContentAlignment.MiddleLeft;
            Controls.Add(title);

            Label sub = MkLabel("Node version manager for Windows", Lt.Sub, Lt.Dim);
            sub.SetBounds(PAD + 246, 24, 300, 24);
            sub.TextAlign = ContentAlignment.MiddleLeft;
            Controls.Add(sub);

            // ===== Current Environment 卡 =====
            LCard env = new LCard();
            env.SetBounds(PAD, 66, CARDW, 76);
            env.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(env);

            Label envT = MkLabel("Current Environment", Lt.H, Lt.Fg);
            envT.SetBounds(14, 8, 300, 20);
            env.Controls.Add(envT);

            Label k1 = MkLabel("Node.js", Lt.Ui, Lt.Fg);
            k1.SetBounds(14, 40, 70, 22);
            env.Controls.Add(k1);
            lblNodeVal = MkLabel("-", Lt.Big, Lt.Green);
            lblNodeVal.SetBounds(86, 34, 140, 26);
            env.Controls.Add(lblNodeVal);

            Label k2 = MkLabel("npm", Lt.Ui, Lt.Fg);
            k2.SetBounds(240, 40, 40, 22);
            env.Controls.Add(k2);
            lblNpmVal = MkLabel("-", Lt.Big, Lt.Teal);
            lblNpmVal.SetBounds(284, 34, 140, 26);
            env.Controls.Add(lblNpmVal);

            Label k3 = MkLabel("NVM", Lt.Ui, Lt.Dim);
            k3.SetBounds(430, 40, 52, 22);          // "NVM" 实测要 42px，给 40 会把 M 切掉
            env.Controls.Add(k3);
            lblEnvHome = MkLabel("-", Lt.Small, Lt.Dim);
            lblEnvHome.SetBounds(486, 40, 178, 22);
            lblEnvHome.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            env.Controls.Add(lblEnvHome);

            // ===== 标签行 =====
            int tabY = 154;
            tabInstalled = MkTab("Installed", PAD, tabY);
            Controls.Add(tabInstalled);
            tabInstalled.Click += delegate { SwitchTab("installed", true); };

            tabOnline = MkTab("Available Online", PAD + (TABW + TABGAP), tabY);
            Controls.Add(tabOnline);
            tabOnline.Click += delegate { SwitchTab("online", true); };

            tabSettings = MkTab("Settings", PAD + (TABW + TABGAP) * 2, tabY);
            Controls.Add(tabSettings);
            tabSettings.Click += delegate { SwitchTab("settings", true); };

            tabProject = MkTab(".nvmrc", PAD + (TABW + TABGAP) * 3, tabY);
            Controls.Add(tabProject);
            tabProject.Click += delegate { SwitchTab("project", true); };

            lblTabHint = MkLabel("", Lt.Small, Lt.Dim);
            lblTabHint.SetBounds(PAD + (TABW + TABGAP) * 4 + 6, tabY + 6, 150, 20);
            lblTabHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            lblTabHint.TextAlign = ContentAlignment.MiddleRight;
            Controls.Add(lblTabHint);

            // ===== 内容页（四个同矩形，互斥显示）=====
            int paneTop = 200;
            int statusY = H - PAD - 20;
            int paneH = statusY - 10 - paneTop;
            if (paneH < 240) paneH = 240;

            paneInstalled = MkPane(paneTop, paneH);
            paneOnline = MkPane(paneTop, paneH);
            paneOnline.Visible = false;
            paneSettings = MkPane(paneTop, paneH);
            paneSettings.Visible = false;
            paneProject = MkPane(paneTop, paneH);
            paneProject.Visible = false;

            BuildInstalledPane(paneH);
            BuildOnlinePane(paneH);
            BuildSettingsPane(paneH);
            BuildProjectPane(paneH);

            // ===== 底部状态栏 =====
            lblStatus = MkLabel("", Lt.Small, Lt.Dim);
            lblStatus.SetBounds(PAD, statusY, CARDW, 20);
            lblStatus.Anchor = AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(lblStatus);

            UpdateHint();
        }

        Label lblEnvHome;

        Label MkLabel(string text, Font f, Color c)
        {
            Label l = new Label();
            l.Text = text;
            l.Font = f;
            l.ForeColor = c;
            l.BackColor = Lt.Bg;
            l.AutoSize = false;
            l.TextAlign = ContentAlignment.MiddleLeft;
            return l;
        }

        LTab MkTab(string text, int x, int y)
        {
            LTab t = new LTab();
            t.Text = text;
            t.SetBounds(x, y, TABW, 32);
            return t;
        }

        LCard MkPane(int top, int h)
        {
            LCard p = new LCard();
            p.SetBounds(PAD, top, CARDW, h);
            p.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(p);
            return p;
        }

        LBtn MkBtn(string text, int x, int y, int w, bool primary)
        {
            LBtn b = new LBtn();
            b.Text = text;
            b.Primary = primary;
            b.SetBounds(x, y, w, 30);
            return b;
        }

        // ---------- Installed 页 ----------
        void BuildInstalledPane(int paneH)
        {
            int listW = CARDW - 12 - 12 - BTNW - 10;          // 436
            int bx = CARDW - 12 - BTNW;                       // 458
            int listY = 12;
            int listH = paneH - listY - 12;

            listInstalled = new VerListBox();
            listInstalled.Kind = VerListBox.Mode.Installed;
            listInstalled.SetBounds(12, listY, listW, listH);
            listInstalled.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listInstalled.SelectedIndexChanged += delegate { UpdateButtons(); };
            listInstalled.DoubleClick += delegate { DoUse(); };
            paneInstalled.Controls.Add(listInstalled);

            // 空状态提示（列表为空时覆盖在列表上）
            lblInsEmpty = MkLabel("No Node.js versions detected", Lt.H, Lt.Dim);
            lblInsEmpty.SetBounds(12, listY, listW, listH);
            lblInsEmpty.TextAlign = ContentAlignment.MiddleCenter;
            lblInsEmpty.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            paneInstalled.Controls.Add(lblInsEmpty);
            lblInsEmpty.BringToFront();

            btnUse = MkBtn("Use This Version", bx, 12, BTNW, true);
            btnUse.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnUse.Click += delegate { DoUse(); };
            paneInstalled.Controls.Add(btnUse);

            btnUninstall = MkBtn("Uninstall", bx, 50, BTNW, false);
            btnUninstall.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnUninstall.Click += delegate { DoUninstall(); };
            paneInstalled.Controls.Add(btnUninstall);

            btnOpenLink = MkBtn("Open Symlink", bx, 88, BTNW, false);
            btnOpenLink.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnOpenLink.Click += delegate { OpenDir(conf.Link, "NVM_SYMLINK"); };
            paneInstalled.Controls.Add(btnOpenLink);
        }

        // ---------- Available Online 页 ----------
        void BuildOnlinePane(int paneH)
        {
            int listW = CARDW - 12 - 12 - BTNW - 10;
            int bx = CARDW - 12 - BTNW;
            int listY = 48;
            int listH = paneH - listY - 12;

            chkLtsOnly = new CheckBox();
            chkLtsOnly.Text = "LTS only";
            chkLtsOnly.ForeColor = Lt.Fg;
            chkLtsOnly.BackColor = Color.Transparent;
            chkLtsOnly.Font = Lt.Ui;
            chkLtsOnly.SetBounds(12, 14, 92, 22);
            chkLtsOnly.CheckedChanged += delegate { FilterOnline(); };
            paneOnline.Controls.Add(chkLtsOnly);

            chkHideInstalled = new CheckBox();
            chkHideInstalled.Text = "Hide installed";
            chkHideInstalled.ForeColor = Lt.Fg;
            chkHideInstalled.BackColor = Color.Transparent;
            chkHideInstalled.Font = Lt.Ui;
            chkHideInstalled.SetBounds(110, 14, 110, 22);
            chkHideInstalled.CheckedChanged += delegate { FilterOnline(); };
            paneOnline.Controls.Add(chkHideInstalled);

            lblOnCount = MkLabel("", Lt.Small, Lt.Dim);
            lblOnCount.SetBounds(226, 14, 262, 22);
            lblOnCount.TextAlign = ContentAlignment.MiddleLeft;
            paneOnline.Controls.Add(lblOnCount);

            btnReload = MkBtn("Reload", CARDW - 12 - 104, 10, 104, true);
            btnReload.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnReload.Click += delegate { StartFetch(true); };
            paneOnline.Controls.Add(btnReload);

            listOnline = new VerListBox();
            listOnline.Kind = VerListBox.Mode.Online;
            listOnline.SetBounds(12, listY, listW, listH);
            listOnline.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            listOnline.SelectedIndexChanged += delegate { UpdateButtons(); };
            listOnline.DoubleClick += delegate { DoInstall(); };
            paneOnline.Controls.Add(listOnline);

            lblOnEmpty = MkLabel("Click Reload to fetch the version list", Lt.H, Lt.Dim);
            lblOnEmpty.SetBounds(12, listY, listW, listH);
            lblOnEmpty.TextAlign = ContentAlignment.MiddleCenter;
            lblOnEmpty.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            paneOnline.Controls.Add(lblOnEmpty);
            lblOnEmpty.BringToFront();

            btnInstall = MkBtn("Install", bx, listY, BTNW, true);
            btnInstall.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnInstall.Click += delegate { DoInstall(); };
            paneOnline.Controls.Add(btnInstall);
        }

        // ---------- Settings 页（NVM / NVM_SYMLINK / CDN 三项配置浏览）----------
        void BuildSettingsPane(int paneH)
        {
            int keyX = 14, keyW = 118;
            int fx = 140;
            int browseW = 84;
            int browseX = CARDW - 12 - browseW;               // 520
            int fxW = browseX - 8 - fx;                       // 372
            int hintW = fxW;
            int hintX = fx + 2;

            // 行 1：NVM
            Label k1 = MkLabel("NVM", Lt.H, Lt.Fg);
            k1.SetBounds(keyX, 16, keyW, 22);
            paneSettings.Controls.Add(k1);

            txtHome = MkText(conf.Home);
            txtHome.SetBounds(fx, 12, fxW, 30);
            txtHome.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtHome.TextChanged += delegate { UpdateHint(); };
            paneSettings.Controls.Add(txtHome);

            btnBrowseHome = MkBtn("Browse", browseX, 12, browseW, false);
            btnBrowseHome.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnBrowseHome.Click += delegate
            {
                PickDir(txtHome, "选择 NVM 安装目录（其下应有 nvm.exe / settings.txt）");
            };
            paneSettings.Controls.Add(btnBrowseHome);

            lblHomeHint = MkLabel("", Lt.Small, Lt.Dim);
            lblHomeHint.SetBounds(hintX, 46, hintW, 18);
            lblHomeHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            paneSettings.Controls.Add(lblHomeHint);

            // 行 2：NVM_SYMLINK
            Label k2 = MkLabel("NVM_SYMLINK", Lt.H, Lt.Fg);
            k2.SetBounds(keyX, 72, keyW, 22);
            paneSettings.Controls.Add(k2);

            txtLink = MkText(conf.Link);
            txtLink.SetBounds(fx, 68, fxW, 30);
            txtLink.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtLink.TextChanged += delegate { UpdateHint(); };
            paneSettings.Controls.Add(txtLink);

            btnBrowseLink = MkBtn("Browse", browseX, 68, browseW, false);
            btnBrowseLink.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnBrowseLink.Click += delegate
            {
                PickDir(txtLink, "选择 NVM_SYMLINK 目录（nodejs 联接所在处）");
            };
            paneSettings.Controls.Add(btnBrowseLink);

            lblLinkHint = MkLabel("", Lt.Small, Lt.Dim);
            lblLinkHint.SetBounds(hintX, 102, hintW, 18);
            lblLinkHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            paneSettings.Controls.Add(lblLinkHint);

            // 行 3：CDN 镜像源
            Label k3 = MkLabel("CDN", Lt.H, Lt.Fg);
            k3.SetBounds(keyX, 128, keyW, 22);
            paneSettings.Controls.Add(k3);

            txtMirror = MkText(conf.Mirror);
            txtMirror.SetBounds(fx, 124, fxW, 30);
            txtMirror.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtMirror.TextChanged += delegate { UpdateHint(); };
            paneSettings.Controls.Add(txtMirror);

            btnDefaultMirror = MkBtn("Default", browseX, 124, browseW, false);
            btnDefaultMirror.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnDefaultMirror.Click += delegate { txtMirror.Text = NvmEnv.DEFAULT_MIRROR; };
            paneSettings.Controls.Add(btnDefaultMirror);

            lblMirrorHint = MkLabel("", Lt.Small, Lt.Dim);
            lblMirrorHint.SetBounds(hintX, 158, hintW, 18);
            lblMirrorHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            paneSettings.Controls.Add(lblMirrorHint);

            // 操作行
            btnSave = MkBtn("Save", 14, 190, 110, true);
            btnSave.Click += delegate { OnSave(); };
            paneSettings.Controls.Add(btnSave);

            btnRescan = MkBtn("Rescan", 132, 190, 110, false);
            btnRescan.Click += delegate { Rescan(); };
            paneSettings.Controls.Add(btnRescan);

            btnOpenHome = MkBtn("Open Folder", 250, 190, 110, false);
            btnOpenHome.Click += delegate { OpenDir(conf.Home, "NVM"); };
            paneSettings.Controls.Add(btnOpenHome);

            btnCopyCmds = MkBtn("Copy Commands", 368, 190, 140, false);
            btnCopyCmds.Click += delegate { CopyCommands(); };
            paneSettings.Controls.Add(btnCopyCmds);
        }

        // ---------- .nvmrc 页（选定项目根目录，写 / 读项目级版本锁定文件）----------
        void BuildProjectPane(int paneH)
        {
            int keyX = 14, keyW = 118;
            int fx = 140;
            int browseW = 84;
            int browseX = CARDW - 12 - browseW;               // 580
            int fxW = browseX - 8 - fx;                       // 432
            int hintX = fx + 2, hintW = fxW;

            // 行 1：项目根目录
            Label k1 = MkLabel("Project", Lt.H, Lt.Fg);
            k1.SetBounds(keyX, 16, keyW, 22);
            paneProject.Controls.Add(k1);

            txtProject = MkText(conf.Project);
            txtProject.SetBounds(fx, 12, fxW, 30);
            txtProject.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtProject.TextChanged += delegate { UpdateProjectHint(); };
            paneProject.Controls.Add(txtProject);

            btnBrowseProject = MkBtn("Browse", browseX, 12, browseW, false);
            btnBrowseProject.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnBrowseProject.Click += delegate
            {
                PickDir(txtProject, "选择项目根目录（.nvmrc 就读写在这个目录里）");
                SaveProjectField();
                UpdateProjectHint();
            };
            paneProject.Controls.Add(btnBrowseProject);

            lblProjectHint = MkLabel("", Lt.Small, Lt.Dim);
            lblProjectHint.SetBounds(hintX, 50, hintW, 18);
            lblProjectHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            paneProject.Controls.Add(lblProjectHint);

            // 行 2：要写进 .nvmrc 的版本
            Label k2 = MkLabel("Version", Lt.H, Lt.Fg);
            k2.SetBounds(keyX, 86, keyW, 22);
            paneProject.Controls.Add(k2);

            cboProject = new VerCombo();
            cboProject.SetBounds(fx, 82, fxW, 26);
            cboProject.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            cboProject.SelectedIndexChanged += delegate { UpdateButtons(); UpdateProjectHint(); };
            paneProject.Controls.Add(cboProject);

            btnProjectCurrent = MkBtn("Current", browseX, 82, browseW, false);
            btnProjectCurrent.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            btnProjectCurrent.Click += delegate { SelectComboVersion(curVer); };
            paneProject.Controls.Add(btnProjectCurrent);

            lblVerHint = MkLabel("", Lt.Small, Lt.Dim);
            lblVerHint.SetBounds(hintX, 120, hintW, 18);
            lblVerHint.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            paneProject.Controls.Add(lblVerHint);

            // 操作行
            btnWriteNvmrc = MkBtn("Write .nvmrc", 14, 156, 150, true);
            btnWriteNvmrc.Click += delegate { OnWriteNvmrc(); };
            paneProject.Controls.Add(btnWriteNvmrc);

            btnReadNvmrc = MkBtn("Read .nvmrc & Switch", 172, 156, 200, false);
            btnReadNvmrc.Click += delegate { OnReadNvmrc(); };
            paneProject.Controls.Add(btnReadNvmrc);

            btnOpenProject = MkBtn("Open Folder", 380, 156, 130, false);
            btnOpenProject.Click += delegate { OpenDir(txtProject.Text.Trim(), "项目目录"); };
            paneProject.Controls.Add(btnOpenProject);

            // 明细区（锚下沿，窗口变矮时自己收）
            lblProjInfo = MkLabel("", Lt.Ui, Lt.Fg);
            lblProjInfo.SetBounds(14, 198, CARDW - 28, 150);
            lblProjInfo.Anchor = AnchorStyles.Top | AnchorStyles.Bottom
                               | AnchorStyles.Left | AnchorStyles.Right;
            lblProjInfo.TextAlign = ContentAlignment.TopLeft;
            paneProject.Controls.Add(lblProjInfo);

            RefreshVerCombo();
            UpdateProjectHint();
        }

        // 已安装列表刷新后同步下拉框：保留用户原选择，否则落到当前生效版本
        void RefreshVerCombo()
        {
            if (cboProject == null) return;
            VerEntry keep = cboProject.SelectedItem as VerEntry;
            string keepVer = keep != null ? keep.Ver : null;

            cboProject.BeginUpdate();
            cboProject.Items.Clear();
            foreach (VerEntry v in installed) cboProject.Items.Add(v);
            cboProject.EndUpdate();

            int sel = -1;
            if (keepVer != null)
                for (int i = 0; i < cboProject.Items.Count; i++)
                    if (((VerEntry)cboProject.Items[i]).Ver == keepVer) { sel = i; break; }
            if (sel < 0)
                for (int i = 0; i < cboProject.Items.Count; i++)
                    if (((VerEntry)cboProject.Items[i]).Current) { sel = i; break; }
            if (sel < 0 && cboProject.Items.Count > 0) sel = 0;
            cboProject.SelectedIndex = sel;                  // 无版本时 -1
        }

        void SelectComboVersion(string ver)
        {
            if (cboProject == null) return;
            if (string.IsNullOrEmpty(ver))
            {
                Status("当前没有生效的 node 版本 —— 先在 Installed 页切一个", Lt.Warn);
                return;
            }
            for (int i = 0; i < cboProject.Items.Count; i++)
            {
                VerEntry v = cboProject.Items[i] as VerEntry;
                if (v != null && v.Ver == ver) { cboProject.SelectedIndex = i; return; }
            }
            Status("已安装列表里没有 v" + ver + "，请先 Rescan", Lt.Warn);
        }

        string InstalledVerList()
        {
            if (installed.Count == 0) return "(无)";
            StringBuilder sb = new StringBuilder();
            foreach (VerEntry v in installed)
            {
                if (sb.Length > 0) sb.Append(", ");
                sb.Append(v.Ver);
            }
            return sb.ToString();
        }

        // 项目目录换了就落盘，省得再点一次 Save（不动 Save 按钮的语义）
        void SaveProjectField()
        {
            string p = txtProject.Text.Trim();
            if (p == conf.Project) return;
            conf.Project = p;
            conf.LastTab = curTab;
            conf.Save();
        }

        // .nvmrc 页的实时提示：目录有效性 + 文件现状 + 版本能否切换
        void UpdateProjectHint()
        {
            if (txtProject == null || cboProject == null) return;

            VerEntry sel = cboProject.SelectedItem as VerEntry;
            lblVerHint.Text = sel == null
                ? "没有可选的已安装版本 —— 去 Available Online 装一个，或点 Settings 的 Rescan"
                : ("将把 v" + sel.Ver + " 写进 .nvmrc" + (sel.Current ? "（当前生效版本）" : ""));
            lblVerHint.ForeColor = sel == null ? Lt.Warn : Lt.Dim;

            string dir = txtProject.Text.Trim();

            if (dir.Length == 0)
            {
                lblProjectHint.Text = "未选择 —— 点 Browse 选择项目根目录";
                lblProjectHint.ForeColor = Lt.Dim;
                lblProjInfo.Text = INFO_IDLE;
                lblProjInfo.ForeColor = Lt.Dim;
                UpdateButtons();
                return;
            }
            if (!Directory.Exists(dir))
            {
                lblProjectHint.Text = "✗ 该目录不存在";
                lblProjectHint.ForeColor = Lt.Err;
                lblProjInfo.Text = "目录不存在：" + dir;
                lblProjInfo.ForeColor = Lt.Err;
                UpdateButtons();
                return;
            }

            string path = NvmRc.PathOf(dir);
            string token = NvmRc.ReadToken(dir);
            bool has = token != null;
            bool blank = has && token.Length == 0;

            if (!has)
            {
                lblProjectHint.Text = "○ 该目录下还没有 .nvmrc，点 Write .nvmrc 生成";
                lblProjectHint.ForeColor = Lt.Dim;
            }
            else if (blank)
            {
                lblProjectHint.Text = "⚠ .nvmrc 是空的，无法解析版本";
                lblProjectHint.ForeColor = Lt.Warn;
            }
            else
            {
                lblProjectHint.Text = "✓ .nvmrc 内容：" + token;
                lblProjectHint.ForeColor = Lt.Green;
            }

            // 用全角空格补齐，比例字体下四列才对得齐
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("文件　　　" + path);
            if (has)
            {
                sb.AppendLine("内容　　　" + (blank ? "(空)" : token));
                if (!blank)
                {
                    if (NvmRc.IsKeyword(token))
                        sb.AppendLine("解析　　　" + token + " 是别名关键字，读取时联网从镜像源解析成具体版本");
                    else
                    {
                        string why;
                        string hit = NvmRc.MatchInstalled(token, installed, out why);
                        if (hit != null)
                            sb.AppendLine("解析　　　✓ 本机已装 v" + hit
                                          + (hit == curVer ? "（就是当前生效版本）" : "，可以直接切换"));
                        else
                            sb.AppendLine("解析　　　✗ " + why);
                    }
                }
            }
            sb.AppendLine("已装版本　" + InstalledVerList());
            sb.AppendLine("当前生效　" + (curVer == null ? "(无)" : "v" + curVer));
            lblProjInfo.Text = sb.ToString();
            lblProjInfo.ForeColor = Lt.Fg;
            UpdateButtons();
        }

        // ------------------------------------------------------------ .nvmrc 写
        void OnWriteNvmrc()
        {
            string dir = txtProject.Text.Trim();
            if (dir.Length == 0 || !Directory.Exists(dir))
            {
                Status("请先选一个存在的项目根目录", Lt.Warn);
                UpdateProjectHint();
                return;
            }
            VerEntry v = cboProject.SelectedItem as VerEntry;
            if (v == null)
            {
                Status("没有可写的版本（未扫描到已安装版本）", Lt.Warn);
                return;
            }

            string path = NvmRc.PathOf(dir);
            if (NvmRc.Exists(dir))
            {
                string old = NvmRc.ReadToken(dir);
                DialogResult dr = MessageBox.Show(this,
                    path + "\r\n\r\n已存在 .nvmrc（当前内容：" +
                    (old == null || old.Length == 0 ? "(空)" : old) + "）\r\n\r\n要覆盖为 v" + v.Ver + " 吗？",
                    "确认覆盖", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
                if (dr != DialogResult.OK) return;
            }

            string err = NvmRc.Write(dir, v.Ver);
            if (err == null) Status("已写入 " + path + " → v" + v.Ver, Lt.Green);
            else Status("写入 .nvmrc 失败：" + err, Lt.Err);

            SaveProjectField();
            UpdateProjectHint();
        }

        // ------------------------------------------------------------ .nvmrc 读 + 切换
        void OnReadNvmrc()
        {
            string dir = txtProject.Text.Trim();
            if (dir.Length == 0 || !Directory.Exists(dir))
            {
                Status("请先选一个存在的项目根目录", Lt.Warn);
                UpdateProjectHint();
                return;
            }
            if (!NvmRc.Exists(dir))
            {
                Status("该目录下没有 .nvmrc：" + NvmRc.PathOf(dir), Lt.Warn);
                UpdateProjectHint();
                return;
            }
            if (!NvmEnv.IsAdmin())
            {
                Status("需要管理员权限才能重建目录联接（请以管理员身份运行）", Lt.Err);
                return;
            }

            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            string home = conf.Home, link = conf.Link;
            SaveProjectField();

            RunWork("按 .nvmrc 切换版本", delegate()
            {
                string token = NvmRc.ReadToken(dir);
                if (token == null) return "读不到 " + NvmRc.PathOf(dir);
                if (token.Length == 0) return ".nvmrc 里没有有效内容";
                workTip = ".nvmrc 内容：" + token;

                string ver;
                if (NvmRc.IsKeyword(token))
                {
                    workTip = token + " 是别名，正在取在线列表解析…";
                    Online.Result r = EnsureOnline();
                    if (r == null || !r.Ok)
                        return "解析 " + token + " 需要在线列表，获取失败："
                             + (r == null ? "超时或 CDN 未配置" : r.Error);
                    ver = NvmRc.MatchKeyword(token, r.Items);
                    if (ver == null)
                        return "在线列表里找不到 " + token + " 对应的版本";
                    workTip = token + " → v" + ver;
                }
                else
                {
                    string why;
                    ver = NvmRc.MatchInstalled(token, installed, out why);
                    if (ver == null)
                        return why + "（已装：" + InstalledVerList() + "）";
                }

                if (ver == curVer)
                {
                    workTip = "已经就是 v" + ver + "，无需切换";
                    return null;
                }

                workTip = "rmdir 旧联接 + mklink /J 新联接…";
                string e = Ops.Use(home, link, ver);
                if (e == null) workTip = "已按 .nvmrc 切换到 node v" + ver;
                return e;
            });
        }

        // 只允许在后台工作线程里调用：必要时同步拉一次在线列表。
        // 结果顺手塞进 fetchResult，Poll 在 UI 线程上会把它一起落到 Available Online 页。
        Online.Result EnsureOnline()
        {
            if (online.Count > 0)
            {
                Online.Result cached = new Online.Result();
                cached.Ok = true;
                cached.Items = online;
                cached.Mirror = conf.Mirror;
                return cached;
            }
            if (string.IsNullOrEmpty(conf.Mirror)) return null;

            ManualResetEvent done = new ManualResetEvent(false);
            Online.Result got = null;
            Online.Fetch(conf.Mirror, null,
                delegate(Online.Result r) { got = r; done.Set(); }, null);
            done.WaitOne(60000, false);
            if (got != null && got.Ok) fetchResult = got;
            return got;
        }

        TextBox MkText(string val)
        {
            TextBox t = new TextBox();
            t.Text = val;
            t.BorderStyle = BorderStyle.FixedSingle;
            t.BackColor = Lt.Bg;
            t.ForeColor = Lt.Fg;
            t.Font = Lt.Ui;
            return t;
        }

        // ---- 仅供 --uicheck / --shot 自检使用 ----
        internal bool IsFetching { get { return fetching; } }
        internal int OnlineCount { get { return online.Count; } }
        internal void StartFetchForCheck() { StartFetch(true); }
        internal void PollOnce() { Poll(); }
        internal string EmptyHintText()
        {
            return "installedCount=" + listInstalled.Items.Count
                 + " onlineCount=" + listOnline.Items.Count
                 + "; installedHintShown=" + (listInstalled.Items.Count == 0)
                 + " onlineHintShown=" + (listOnline.Items.Count == 0);
        }
        internal string ProjectHintText()
        {
            string dir = txtProject.Text.Trim();
            VerEntry sel = cboProject.SelectedItem as VerEntry;
            return "folder=" + dir
                 + " folderExists=" + Directory.Exists(dir)
                 + " nvmrcExists=" + NvmRc.Exists(dir)
                 + " token=" + (NvmRc.ReadToken(dir) ?? "(null)")
                 + " comboCount=" + cboProject.Items.Count
                 + " comboSel=" + (sel == null ? "(none)" : sel.Ver)
                 + " writeEnabled=" + btnWriteNvmrc.Enabled
                 + " readEnabled=" + btnReadNvmrc.Enabled
                 + " hint=\"" + lblProjectHint.Text + "\"";
        }
        // 供 --shot 截图用：预设项目目录并把提示刷到最新
        internal void SetProjectForShot(string dir)
        {
            txtProject.Text = dir;
            RefreshVerCombo();
            UpdateProjectHint();
        }
        // 供 --shot 截图用：切到指定标签，并把窗体内部状态刷到最新
        internal void TabForShot(string which) { SwitchTab(which, false); }
        internal void RescanForShot() { Rescan(); }
        // 模拟「一个版本都没扫到」的原始故障场景，用来截空态图
        internal void BlankForShot()
        {
            installed = new List<VerEntry>();
            listInstalled.Items.Clear();
            lblNodeVal.Text = "not activated";
            lblNpmVal.Text = "-";
            lblEnvHome.Text = conf.Home;
            SyncEmptyState();
            RefreshVerCombo();
            UpdateProjectHint();
            UpdateButtons();
            Status("未扫描到任何已安装版本 —— 请到 Settings 检查 NVM 目录", Lt.Warn);
        }
        internal void ApplyFetchedForShot(Online.Result r)
        {
            online = r.Items;
            fetching = false;
            FilterOnline();
            lblOnCount.Text = online.Count + " versions, " + r.Ms + " ms";
        }

        // ------------------------------------------------------------ 列表空态
        // 空态提示与列表**互斥显示**（不用覆盖层）：覆盖层会被 ListBox 的原生
        // 绘制盖住，而且 z 序依赖 Add 顺序，很容易在某个操作后失效。
        void SyncEmptyState()
        {
            if (lblInsEmpty != null)
            {
                bool empty = listInstalled.Items.Count == 0;
                lblInsEmpty.Visible = empty;
                listInstalled.Visible = !empty;
            }
            if (lblOnEmpty != null)
            {
                bool empty = listOnline.Items.Count == 0;
                lblOnEmpty.Text = fetching ? "Loading…" : "Click Reload to fetch the version list";
                lblOnEmpty.Visible = empty;
                listOnline.Visible = !empty;
            }
        }

        // ------------------------------------------------------------ 交互
        void PickDir(TextBox t, string title)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = title;
            if (Directory.Exists(t.Text)) d.SelectedPath = t.Text;
            if (d.ShowDialog(this) == DialogResult.OK) t.Text = d.SelectedPath;
        }

        void OpenDir(string dir, string what)
        {
            if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir))
            {
                Status(what + " 目录不存在: " + (string.IsNullOrEmpty(dir) ? "(未配置)" : dir), Lt.Warn);
                return;
            }
            try { Sys.OpenFolder(dir); Status("已打开 " + dir, Lt.Dim); }
            catch (Exception ex) { Status("打开失败: " + ex.Message, Lt.Err); }
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
            sb.AppendLine();
            sb.AppendLine("# 本机配置");
            sb.AppendLine("NVM         = " + conf.Home);
            sb.AppendLine("NVM_SYMLINK = " + conf.Link);
            sb.AppendLine("CDN         = " + conf.Mirror);
            try
            {
                Clipboard.SetText(sb.ToString());
                Status("命令与配置已复制到剪贴板", Lt.Green);
            }
            catch (Exception ex) { Status("复制失败: " + ex.Message, Lt.Err); }
        }

        void OnSave()
        {
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            conf.Mirror = txtMirror.Text.Trim();
            conf.LastTab = curTab;
            conf.Save();
            Status("配置已保存到 %APPDATA%\\DevLauncher\\nvmdesk.ini", Lt.Green);
            Rescan();
        }

        // 三项配置的实时校验提示
        void UpdateHint()
        {
            if (txtHome == null) return;
            string h = txtHome.Text.Trim();
            string l = txtLink.Text.Trim();
            string m = txtMirror.Text.Trim();
            string cur = NvmEnv.LinkTarget(l);

            bool hOk = NvmEnv.LooksLikeNvm(h);
            bool lOk = !string.IsNullOrEmpty(l);

            lblHomeHint.Text = hOk
                ? "✓ 目录有效"
                : (h.Length == 0 ? "未配置 —— 点 Browse 选择 NVM 安装目录" : "✗ 未发现 nvm.exe / 版本目录，请检查路径");
            lblHomeHint.ForeColor = hOk ? Lt.Green : (h.Length == 0 ? Lt.Dim : Lt.Warn);

            lblLinkHint.Text = !lOk
                ? "未配置 —— 点 Browse 选择 nodejs 联接所在目录"
                : (Directory.Exists(l)
                    ? ("✓ 存在" + (cur != null ? "，当前指向 " + Path.GetFileName(cur) : "（非联接）"))
                    : "✗ 该路径不存在");
            lblLinkHint.ForeColor = lOk && Directory.Exists(l) ? Lt.Green : (lOk ? Lt.Warn : Lt.Dim);

            lblMirrorHint.Text = m.Length == 0
                ? "未配置 —— 点 Default 使用 npmmirror"
                : (m.StartsWith("http", StringComparison.OrdinalIgnoreCase) ? "✓ 将从该地址读取 index.json" : "⚠ 建议以 https:// 开头");
            lblMirrorHint.ForeColor = m.Length == 0 ? Lt.Dim
                : (m.StartsWith("http", StringComparison.OrdinalIgnoreCase) ? Lt.Dim : Lt.Warn);

            if (lblEnvHome != null)
                lblEnvHome.Text = (h.Length == 0 ? "(未配置)" : h);

            if (lblTabHint != null)
            {
                if (!NvmEnv.IsAdmin()) { lblTabHint.Text = "⚠ 非管理员运行，无法切换版本"; lblTabHint.ForeColor = Lt.Warn; }
                else { lblTabHint.Text = ""; }
            }
        }

        void Status(string msg, Color c)
        {
            if (lblStatus == null) return;
            lblStatus.Text = msg;
            lblStatus.ForeColor = c;
        }

        string curTab = "installed";

        void SwitchTab(string which, bool userAction)
        {
            if (which != "installed" && which != "online" && which != "settings" && which != "project")
                which = "installed";
            curTab = which;

            paneInstalled.Visible = which == "installed";
            paneOnline.Visible = which == "online";
            paneSettings.Visible = which == "settings";
            paneProject.Visible = which == "project";

            tabInstalled.On = which == "installed";
            tabOnline.On = which == "online";
            tabSettings.On = which == "settings";
            tabProject.On = which == "project";

            if (which == "online" && userAction && online.Count == 0 && !fetching)
                StartFetch(false);

            UpdateButtons();
            SyncEmptyState();
            if (which == "project") UpdateProjectHint();
        }

        void UpdateButtons()
        {
            VerEntry ve = listInstalled.SelectedItem as VerEntry;
            bool hasIns = ve != null && !busy;
            if (btnUse != null) btnUse.Enabled = hasIns && !ve.Current;
            if (btnUninstall != null) btnUninstall.Enabled = hasIns && !ve.Current;
            if (btnOpenLink != null) btnOpenLink.Enabled = !busy;

            RelEntry re = listOnline.SelectedItem as RelEntry;
            bool hasOn = re != null && !busy;
            if (btnInstall != null)
            {
                btnInstall.Enabled = hasOn;
                btnInstall.Text = (hasOn && re.Installed) ? "Reinstall" : "Install";
            }

            if (btnReload != null) btnReload.Enabled = !busy && !fetching;
            if (btnRescan != null) btnRescan.Enabled = !busy;
            if (btnSave != null) btnSave.Enabled = !busy;
            if (btnBrowseHome != null) btnBrowseHome.Enabled = !busy;
            if (btnBrowseLink != null) btnBrowseLink.Enabled = !busy;
            if (btnDefaultMirror != null) btnDefaultMirror.Enabled = !busy;
            if (btnCopyCmds != null) btnCopyCmds.Enabled = !busy;

            // .nvmrc 页
            if (cboProject != null) cboProject.Enabled = !busy;
            if (btnBrowseProject != null) btnBrowseProject.Enabled = !busy;
            if (btnProjectCurrent != null) btnProjectCurrent.Enabled = !busy && curVer != null;
            if (btnOpenProject != null)
                btnOpenProject.Enabled = Directory.Exists(txtProject.Text.Trim());
            if (btnWriteNvmrc != null)
                btnWriteNvmrc.Enabled = !busy && cboProject.SelectedItem is VerEntry
                                     && Directory.Exists(txtProject.Text.Trim());
            if (btnReadNvmrc != null)
                btnReadNvmrc.Enabled = !busy && NvmRc.Exists(txtProject.Text.Trim());

            if (tabInstalled != null) tabInstalled.Enabled = !busy;
            if (tabOnline != null) tabOnline.Enabled = !busy;
            if (tabSettings != null) tabSettings.Enabled = !busy;
            if (tabProject != null) tabProject.Enabled = !busy;
        }

        // ------------------------------------------------------------ 扫描
        void Rescan()
        {
            conf.Home = txtHome.Text.Trim();
            conf.Link = txtLink.Text.Trim();
            curVer = NvmEnv.CurrentVer(conf.Home, conf.Link);
            npmVer = NvmEnv.NpmVer(conf.Home, conf.Link);

            installed = Scan.Installed(conf.Home, curVer);

            listInstalled.BeginUpdate();
            listInstalled.Items.Clear();
            foreach (VerEntry v in installed) listInstalled.Items.Add(v);
            listInstalled.EndUpdate();

            int sel = -1;
            for (int i = 0; i < listInstalled.Items.Count; i++)
            {
                if (((VerEntry)listInstalled.Items[i]).Current) { sel = i; break; }
            }
            if (sel >= 0) listInstalled.SelectedIndex = sel;
            else if (listInstalled.Items.Count > 0) listInstalled.SelectedIndex = 0;

            lblNodeVal.Text = curVer == null ? "not activated" : ("v" + curVer);
            lblNpmVal.Text = npmVer == null ? "-" : npmVer;

            UpdateHint();
            UpdateButtons();
            SyncEmptyState();
            RefreshInstalledFlags();
            RefreshVerCombo();
            UpdateProjectHint();

            Status(installed.Count == 0
                ? "未扫描到任何已安装版本 —— 请到 Settings 检查 NVM 目录"
                : ("已扫描到 " + installed.Count + " 个版本"),
                installed.Count == 0 ? Lt.Warn : Lt.Dim);
        }

        void RefreshInstalledFlags()
        {
            HashSet<string> set = new HashSet<string>();
            foreach (VerEntry v in installed) set.Add(v.Ver);
            foreach (RelEntry r in online) r.Installed = set.Contains(r.Ver);
            listOnline.CurVer = curVer;
            listOnline.Invalidate();
        }

        // ------------------------------------------------------------ 在线列表
        void StartFetch(bool force)
        {
            if (fetching) return;
            conf.Mirror = txtMirror.Text.Trim();
            if (conf.Mirror.Length == 0)
            {
                Status("CDN 镜像源未配置，请到 Settings 填写", Lt.Err);
                return;
            }
            if (online.Count > 0 && !force) return;

            HashSet<string> set = new HashSet<string>();
            foreach (VerEntry v in installed) set.Add(v.Ver);

            fetching = true;
            fetchResult = null;
            fetchTip = null;
            btnReload.Text = "Loading…";
            lblOnCount.Text = "正在请求 " + Online.Unwarp(conf.Mirror) + " …";
            UpdateButtons();
            SyncEmptyState();

            fetchThread = Online.Fetch(conf.Mirror, set,
                delegate(Online.Result r) { fetchResult = r; },
                delegate(string s) { fetchTip = s; });
        }

        // ------------------------------------------------------------ 轮询
        void Poll()
        {
            string tip = fetchTip;
            if (tip != null && tip.Length > 0)
            {
                fetchTip = null;
                Status(tip, Lt.Dim);
            }

            Online.Result r = fetchResult;
            if (r != null)
            {
                fetchResult = null;
                fetching = false;
                btnReload.Text = "Reload";

                if (r.Ok)
                {
                    online = r.Items;
                    int lts = 0;
                    foreach (RelEntry e in online) if (e.Lts.Length > 0) lts++;
                    FilterOnline();
                    lblOnCount.Text = online.Count + " 个版本，其中 LTS " + lts + " 个  (" + r.Ms + " ms)";
                    Status("在线列表获取成功：" + online.Count + " 个版本，耗时 " + r.Ms + " ms", Lt.Green);
                }
                else
                {
                    lblOnCount.Text = "获取失败";
                    Status("获取失败: " + r.Error, Lt.Err);
                }
                RefreshInstalledFlags();
                UpdateButtons();
                SyncEmptyState();
            }

            string wd = workDone;
            if (wd != null)
            {
                workDone = null;
                busy = false;
                bool failed = wd.StartsWith("操作失败");
                Status(wd, failed ? Lt.Err : Lt.Green);
                Rescan();
                UpdateButtons();
                if (paneOnline.Visible) StartFetch(true);
            }
            string wt = workTip;
            if (wt != null)
            {
                workTip = null;
                Status(wt, Lt.Dim);
            }
        }

        void FilterOnline()
        {
            bool ltsOnly = chkLtsOnly.Checked;
            bool hideIns = chkHideInstalled.Checked;
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
            SyncEmptyState();
            UpdateButtons();
        }

        // ------------------------------------------------------------ 写操作
        void RunWork(string title, Func<string> body)
        {
            if (busy) return;
            busy = true;
            workDone = null;
            Status(title + " …", Lt.Accent);
            UpdateButtons();

            workThread = new Thread(delegate()
            {
                string err;
                try { err = body(); }
                catch (Exception ex) { err = "异常: " + ex.Message; }

                if (err == null) workDone = title + " 完成";
                else if (err == "已取消") workDone = title + " 已取消";
                else workDone = "操作失败: " + err;
            });
            workThread.IsBackground = true;
            workThread.Start();
        }

        void DoUse()
        {
            VerEntry v = listInstalled.SelectedItem as VerEntry;
            if (v == null) return;
            if (v.Current) return;
            if (!NvmEnv.IsAdmin())
            {
                Status("需要管理员权限才能重建目录联接（请以管理员身份运行）", Lt.Err);
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
            if (v == null || v.Current) return;

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

            string home = conf.Home, mirror = conf.Mirror, ver = r.Ver, link = conf.Link;
            bool reinstall = r.Installed;

            RunWork("安装 node " + ver, delegate()
            {
                if (reinstall)
                {
                    workTip = "移除已有目录…";
                    string e0 = Ops.Uninstall(home, link, ver);
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
    //   --scan      [out]                 扫描本机 NVM 环境
    //   --resolve   [out]                 打印自动探测到的 NVM / NVM_SYMLINK / CDN
    //   --releases  [mirror] [out]        拉在线列表
    //   --url <ver> [mirror] [out]        拼出下载 URL（不发请求）
    //   --use <ver> [out]                 切换版本
    //   --install <ver> [out]             安装版本
    //   --uninstall <ver> [out]           卸载版本
    //   --nvmrc-write <dir> [ver] [out]   往 <dir> 写 .nvmrc，并回读校验
    //   --nvmrc-read <dir> [--use] [out]  读 <dir>\.nvmrc 并解析；带 --use 才真的切换
    //   --uicheck   [out]                 建窗体，校验布局重叠 + UI 是否冻结
    //   --shot <png> [tab] [--dir <dir>] [--empty] [out]
    //                                     把某个标签页渲染成 png（tab 见 SHOT_TABS）
    // ========================================================================
    static class Cli
    {
        static readonly string[] SHOT_TABS = new string[]
        { "installed", "online", "settings", "project" };
        static string Arg(string[] a, int i, string def)
        {
            return i < a.Length && a[i].Length > 0 ? a[i] : def;
        }

        // 逗号分隔的已装版本清单（自检输出用）
        static string JoinVer(List<VerEntry> list)
        {
            StringBuilder sb = new StringBuilder();
            foreach (VerEntry v in list)
            {
                if (sb.Length > 0) sb.Append(", ");
                sb.Append(v.Ver);
            }
            return sb.ToString();
        }

        // 参数像不像本地路径（把「输出文件」与「镜像源 / 版本号」分开）
        static bool LooksLikePath(string s)
        {
            if (string.IsNullOrEmpty(s)) return false;
            if (s.StartsWith("-")) return false;
            if (s.StartsWith("http", StringComparison.OrdinalIgnoreCase)) return false;
            if (Regex.IsMatch(s, @"^\d+(\.\d+)*$")) return false;
            return s.IndexOf('\\') >= 0 || s.IndexOf('/') >= 0 ||
                   s.EndsWith(".txt", StringComparison.OrdinalIgnoreCase);
        }

        // 从参数里挑镜像源；挑不到用默认。
        // 不能用固定下标：镜像源是可选参数，省略后输出文件的下标会前移。
        static string MirrorArg(string[] a)
        {
            for (int i = 1; i < a.Length; i++)
            {
                string s = a[i];
                if (LooksLikePath(s)) continue;
                if (Regex.IsMatch(s, @"^\d+(\.\d+)*$")) continue;
                if (s.StartsWith("--")) continue;
                if (s.StartsWith("http", StringComparison.OrdinalIgnoreCase)) return s;
                if (s.IndexOf('.') > 0) return s;
            }
            return NvmEnv.DEFAULT_MIRROR;
        }

        static string OutFile(string[] a)
        {
            return OutFileExcept(a, -1);
        }

        // 同上，但跳过某个下标 —— --nvmrc-* 的 <dir> 本身就是路径，
        // 不给它开洞的话，省略 [out] 时会把「目录」当成输出文件。
        static string OutFileExcept(string[] a, int skip)
        {
            for (int i = a.Length - 1; i >= 1; i--)
            {
                if (i == skip) continue;
                if (LooksLikePath(a[i])) return a[i];
            }
            return Path.Combine(Path.GetTempPath(), "nvmdesk-cli.txt");
        }

        static void Write(string path, string s)
        {
            try { File.WriteAllText(path, s ?? "", new UTF8Encoding(false)); }
            catch (Exception) { }
        }

        // 递归遍历控件树，统计「同级控件矩形重叠」——布局 bug 的唯一可证伪判据。
        // 未 Show() 的窗体里子控件 Visible 恒为 false，用它判布局没意义。
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

                for (int j = i + 1; j < kids.Count; j++)
                {
                    Control b = kids[j];
                    Rectangle ra = a.Bounds, rb = b.Bounds;
                    if (!ra.IntersectsWith(rb)) continue;
                    // 互斥显示的同位容器与「空态覆盖层」允许重叠
                    if (a is Panel && b is Panel) continue;
                    if (a is Label && b is ListBox) continue;
                    if (a is ListBox && b is Label) continue;
                    sb.AppendLine("OVERLAP\t" + tag + " ^ " +
                                  path + "/" + b.GetType().Name + "[" + j + "]" +
                                  "\t" + ra + " vs " + rb);
                    bad++;
                }
            }
            return bad;
        }

        // 文本被裁的检查：AutoSize=false 的 Label 宽度不够时字会被直接切掉，
        // 这种问题截图里不放大看不出来，所以用测量代替肉眼。
        static void CollectClip(Control parent, string path, StringBuilder sb, ref int bad)
        {
            foreach (Control c in parent.Controls)
            {
                Label l = c as Label;
                if (l != null && l.Text.Length > 0 && !l.AutoSize
                    && l.Text.IndexOf('\n') < 0
                    && (l.Anchor & AnchorStyles.Right) == 0)     // 跟着右边缘伸的标签不算
                {
                    int need = TextRenderer.MeasureText(l.Text, l.Font).Width;
                    if (need > l.Width - 2)
                    {
                        bad++;
                        if (sb.Length < 900)
                            sb.Append("[" + l.Text + " need=" + need + " w=" + l.Width
                                      + " @" + path + "] ");
                    }
                }
                if (c.Controls.Count > 0)
                    CollectClip(c, path + "/" + c.GetType().Name + "[" + c.Text + "]", sb, ref bad);
            }
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
                sb.AppendLine("npm=" + (NvmEnv.NpmVer(c.Home, c.Link) ?? ""));
                sb.AppendLine("count=" + list.Count);
                foreach (VerEntry v in list)
                    sb.AppendLine(v.Ver + "\tnode=" + v.HasNode + "\t" + Fmt.Bytes(v.Bytes) +
                                  "\t" + (v.Current ? "CURRENT" : ""));
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
                sb.AppendLine("linkTarget=" + (NvmEnv.LinkTarget(c.Link) ?? "(非联接)"));
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
                }
                Write(OutFile(args), sb.ToString());
                return true;
            }

            if (cmd == "--url")
            {
                string ver = Ver.Norm(Arg(args, 1, "20.2.0"));
                string zip = Online.Unwarp(MirrorArg(args)) +
                             "/v" + ver + "/node-v" + ver + "-win-x64.zip";
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

            if (cmd == "--nvmrc-write")
            {
                // --nvmrc-write <dir> [ver] [out]；ver 省略时用当前生效版本
                string dir = Arg(args, 1, "");
                string ver = Arg(args, 2, "");
                if (LooksLikePath(ver)) ver = "";          // 那是 [out]，不是版本
                string note = "";
                if (ver.Length == 0)
                {
                    Conf c0 = Conf.Load();
                    ver = NvmEnv.CurrentVer(c0.Home, c0.Link) ?? "";
                    note = ver.Length > 0 ? "(省略，取当前生效版本)" : "(省略，且没有生效版本)";
                }
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("dir=" + dir);
                sb.AppendLine("dirExists=" + Directory.Exists(dir));
                sb.AppendLine("in=" + ver + " " + note);
                sb.AppendLine("norm=" + Ver.Norm(ver));
                sb.AppendLine("path=" + NvmRc.PathOf(dir));
                string err = NvmRc.Write(dir, ver);
                sb.AppendLine("err=" + (err ?? ""));
                sb.AppendLine("exists=" + NvmRc.Exists(dir));
                sb.AppendLine("readBack=" + (NvmRc.ReadToken(dir) ?? "(null)"));
                try
                {
                    if (File.Exists(NvmRc.PathOf(dir)))
                    {
                        sb.AppendLine("bytes=" + new FileInfo(NvmRc.PathOf(dir)).Length);
                        byte[] raw = File.ReadAllBytes(NvmRc.PathOf(dir));
                        sb.AppendLine("rawHex=" + BitConverter.ToString(raw).Replace("-", " "));
                        sb.AppendLine("hasBom=" + (raw.Length >= 3 && raw[0] == 0xEF
                                                   && raw[1] == 0xBB && raw[2] == 0xBF));
                    }
                }
                catch (Exception) { }
                Write(OutFileExcept(args, 1), sb.ToString());
                return true;
            }

            if (cmd == "--nvmrc-read")
            {
                // --nvmrc-read <dir> [--use] [out]；不带 --use 是纯 dry-run
                string dir = Arg(args, 1, "");
                bool use = false;
                for (int i = 2; i < args.Length; i++)
                    if (args[i] == "--use") use = true;

                Conf c = Conf.Load();
                string cur = NvmEnv.CurrentVer(c.Home, c.Link);
                List<VerEntry> ins = Scan.Installed(c.Home, cur);

                StringBuilder sb = new StringBuilder();
                sb.AppendLine("dir=" + dir);
                sb.AppendLine("path=" + NvmRc.PathOf(dir));
                sb.AppendLine("exists=" + NvmRc.Exists(dir));
                string token = NvmRc.ReadToken(dir);
                sb.AppendLine("token=" + (token ?? "(null)"));
                sb.AppendLine("keyword=" + (token != null && NvmRc.IsKeyword(token)));
                sb.AppendLine("installed=" + ins.Count + " [" + JoinVer(ins) + "]");

                string target = null;
                if (token == null)
                {
                    sb.AppendLine("why=没有 .nvmrc 或读不到");
                }
                else if (token.Length == 0)
                {
                    sb.AppendLine("why=.nvmrc 内容为空");
                }
                else if (!NvmRc.IsKeyword(token))
                {
                    string why;
                    target = NvmRc.MatchInstalled(token, ins, out why);
                    sb.AppendLine("why=" + why);
                }
                else
                {
                    ManualResetEvent done = new ManualResetEvent(false);
                    Online.Result got = null;
                    Online.Fetch(c.Mirror, null,
                        delegate(Online.Result r) { got = r; done.Set(); }, null);
                    done.WaitOne(60000, false);
                    sb.AppendLine("onlineOk=" + (got != null && got.Ok));
                    if (got != null && got.Ok) target = NvmRc.MatchKeyword(token, got.Items);
                    else sb.AppendLine("onlineErr=" + (got == null ? "TIMEOUT" : got.Error));
                }
                sb.AppendLine("target=" + (target ?? "(未解析出)"));
                sb.AppendLine("current=" + (cur ?? ""));

                if (use && target != null)
                {
                    string err = Ops.Use(c.Home, c.Link, target);
                    sb.AppendLine("useErr=" + (err ?? ""));
                    sb.AppendLine("now=" + (NvmEnv.CurrentVer(c.Home, c.Link) ?? ""));
                }
                else
                {
                    sb.AppendLine("dryRun=" + (!use));
                }
                Write(OutFileExcept(args, 1), sb.ToString());
                return true;
            }

            if (cmd == "--uicheck")
            {
                StringBuilder sb = new StringBuilder();
                try
                {
                    MainForm f = new MainForm();
                    sb.AppendLine("formOk=true");
                    sb.AppendLine("clientSize=" + f.ClientSize.Width + "x" + f.ClientSize.Height);
                    sb.AppendLine("backColor=" + ColorTranslator.ToHtml(f.BackColor));

                    int total = 0, overlaps = 0;
                    overlaps += DumpChildren(f, "form", sb, ref total);
                    sb.AppendLine("totalControls=" + total);
                    sb.AppendLine("overlaps=" + overlaps);

                    // 四个内容页必须是同一块矩形（互斥显示）
                    bool same = f.paneInstalled.Bounds == f.paneOnline.Bounds
                             && f.paneOnline.Bounds == f.paneSettings.Bounds
                             && f.paneSettings.Bounds == f.paneProject.Bounds;
                    sb.AppendLine("panesSameRect=" + same);
                    sb.AppendLine("paneRect=" + f.paneInstalled.Bounds);

                    // 标签文字必须放得下（4 个标签挤一行最容易崩在这里）
                    StringBuilder fit = new StringBuilder();
                    foreach (Control c in f.Controls)
                    {
                        LTab t = c as LTab;
                        if (t == null) continue;
                        int need = TextRenderer.MeasureText(t.Text, t.Font).Width;
                        if (fit.Length > 0) fit.Append(", ");
                        fit.Append(t.Text + "=" + need + "/" + t.Width +
                                   (need + 8 <= t.Width ? ":ok" : ":TIGHT"));
                    }
                    sb.AppendLine("tabFit=" + fit);

                    // 有没有标签文字被容器切掉（截图里不放大看不出来）
                    StringBuilder clip = new StringBuilder();
                    int clipped = 0;
                    CollectClip(f, "form", clip, ref clipped);
                    sb.AppendLine("textClipped=" + clipped);
                    sb.AppendLine("textClipDetail=" + clip);

                    // 空态提示必须可见（截图里的 "No Node.js versions detected"）
                    sb.AppendLine("emptyHintText=" + f.EmptyHintText());
                    sb.AppendLine("projectHintText=" + f.ProjectHintText());

                    // ---- UI 阻塞验证 ----
                    // 起一次真实拉取，主线程连做 40 次「DoEvents + Sleep(50)」并记间隔。
                    // 若主线程被网络请求占住（原项目缺陷 ④），maxGap 会出现巨大尖峰。
                    f.StartFetchForCheck();
                    List<long> gaps = new List<long>();
                    Stopwatch sw2 = Stopwatch.StartNew();
                    long prev = 0;
                    for (int i = 0; i < 40; i++)
                    {
                        f.PollOnce();
                        Application.DoEvents();
                        Thread.Sleep(50);
                        long now = sw2.ElapsedMilliseconds;
                        gaps.Add(now - prev);
                        prev = now;
                    }
                    long maxGap = 0, sum = 0;
                    foreach (long g in gaps) { if (g > maxGap) maxGap = g; sum += g; }
                    sb.AppendLine("uiProbeCount=" + gaps.Count);
                    sb.AppendLine("uiMaxGapMs=" + maxGap);
                    sb.AppendLine("uiAvgGapMs=" + (sum / Math.Max(1, gaps.Count)));
                    sb.AppendLine("uiFrozen=" + (maxGap > 500));

                    for (int i = 0; i < 200 && f.IsFetching; i++)
                    {
                        f.PollOnce();
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

            if (cmd == "--shot")
            {
                // --shot <out.png> [tab] [--dir <项目目录>] [--empty]
                string png = null;
                string tab = "installed";
                string projDir = null;
                bool forceEmpty = false;
                for (int i = 1; i < args.Length; i++)
                {
                    string s = args[i];
                    if (s == "--empty") { forceEmpty = true; continue; }
                    if (s == "--dir" && i + 1 < args.Length) { projDir = args[++i]; continue; }
                    if (Array.IndexOf(SHOT_TABS, s) >= 0 && !LooksLikePath(s)) { tab = s; continue; }
                    if (png == null && LooksLikePath(s)) png = s;
                }
                if (png == null) png = Path.Combine(Path.GetTempPath(), "nvmdesk-shot.png");

                StringBuilder log = new StringBuilder();
                try
                {
                    // 必须在建任何控件之前调用，否则渲染不出视觉样式
                    Application.EnableVisualStyles();
                    Application.SetCompatibleTextRenderingDefault(false);

                    MainForm f = new MainForm();
                    // 子控件只有在窗体真正显示后才会拿到句柄并被绘制，
                    // 所以先挪到屏幕外 Show 一次，截完立刻 Close。
                    f.StartPosition = FormStartPosition.Manual;
                    f.Location = new Point(-6000, -6000);

                    if (forceEmpty) f.BlankForShot();       // 模拟「一个版本都没扫到」
                    else f.RescanForShot();

                    if (projDir != null) f.SetProjectForShot(projDir);

                    if (tab == "online")
                    {
                        // 截图不依赖在线状态：先同步拉一次（失败则展示空态）
                        ManualResetEvent done = new ManualResetEvent(false);
                        Online.Result got = null;
                        Online.Fetch(NvmEnv.DEFAULT_MIRROR, null,
                            delegate(Online.Result r) { got = r; done.Set(); }, null);
                        done.WaitOne(60000, false);
                        if (got != null && got.Ok) f.ApplyFetchedForShot(got);
                    }

                    f.TabForShot(tab);

                    f.Show();
                    // 让首帧渲染、布局与异步绘制都落定
                    for (int i = 0; i < 25; i++)
                    {
                        Application.DoEvents();
                        Thread.Sleep(20);
                    }

                    // 去掉输入框的焦点高亮，截图更干净
                    f.ActiveControl = null;
                    Application.DoEvents();

                    // DrawToBitmap 对 Form 会把整窗（含标题栏）一起画进目标矩形，
                    // 所以位图要按**外框**尺寸开，否则底部客户端区域会被裁掉。
                    Size win = f.Size;
                    using (Bitmap bmp = new Bitmap(win.Width, win.Height))
                    {
                        f.DrawToBitmap(bmp, new Rectangle(0, 0, win.Width, win.Height));
                        bmp.Save(png, ImageFormat.Png);
                    }

                    log.AppendLine("ok=true");
                    log.AppendLine("png=" + png);
                    log.AppendLine("tab=" + tab);
                    log.AppendLine("empty=" + forceEmpty);
                    log.AppendLine("project=" + (projDir ?? "(none)"));
                    log.AppendLine("projectHint=" + f.ProjectHintText());
                    log.AppendLine("clientSize=" + f.ClientSize.Width + "x" + f.ClientSize.Height);
                    log.AppendLine("windowSize=" + win.Width + "x" + win.Height);
                    log.AppendLine("bytes=" + new FileInfo(png).Length);
                    f.Close();
                    f.Dispose();
                }
                catch (Exception ex)
                {
                    log.AppendLine("ok=false");
                    log.AppendLine("error=" + ex);
                }
                // 截图时不要把 png 当输出文件覆盖掉
                string outTxt = null;
                for (int i = args.Length - 1; i >= 1; i--)
                {
                    if (LooksLikePath(args[i]) && !args[i].EndsWith(".png", StringComparison.OrdinalIgnoreCase))
                    { outTxt = args[i]; break; }
                }
                if (outTxt != null) Write(outTxt, log.ToString());
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
