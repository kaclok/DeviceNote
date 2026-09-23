// DevLauncher 公共库 —— 深色主题 UI / 进程管理 / 项目探测
// 目标编译器：.NET Framework 4.0 csc (C# 5)，勿使用字符串插值、?.、nameof 等 C#6+ 语法
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Management;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;
using System.Xml;

namespace DevLaunch
{
    // ============================ 主题 ============================
    static class Th
    {
        public static readonly Color Bg = Color.FromArgb(30, 30, 30);
        public static readonly Color Panel = Color.FromArgb(37, 37, 38);
        public static readonly Color Input = Color.FromArgb(45, 45, 48);
        public static readonly Color Hover = Color.FromArgb(62, 62, 67);
        public static readonly Color Fg = Color.FromArgb(226, 226, 226);
        public static readonly Color Dim = Color.FromArgb(148, 148, 152);
        public static readonly Color Accent = Color.FromArgb(14, 99, 156);
        public static readonly Color AccHov = Color.FromArgb(24, 122, 194);
        public static readonly Color Border = Color.FromArgb(62, 62, 67);
        public static readonly Color LogBg = Color.FromArgb(22, 22, 23);
        public static readonly Color Ok = Color.FromArgb(122, 202, 122);
        public static readonly Color Warn = Color.FromArgb(228, 178, 74);
        public static readonly Color Err = Color.FromArgb(238, 110, 110);
        public static readonly Color Cyan = Color.FromArgb(106, 202, 222);
        public static readonly Color LogFg = Color.FromArgb(214, 214, 214);

        public static readonly Font Ui = new Font("Microsoft YaHei UI", 9F);
        public static readonly Font UiB = new Font("Microsoft YaHei UI", 9F, FontStyle.Bold);
        public static readonly Font UiS = new Font("Microsoft YaHei UI", 8.25F);
        public static readonly Font Mono = new Font("Consolas", 9F);
    }

    // ============================ 控件 ============================
    class FlatBtn : Button
    {
        public Color Base = Th.Input;
        public Color Hover = Th.Hover;

        public FlatBtn()
        {
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 0;
            FlatAppearance.MouseOverBackColor = Hover;
            BackColor = Base;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            Height = 32;
            Cursor = Cursors.Hand;
            UseVisualStyleBackColor = false;
            TabStop = false;
        }

        protected override void OnMouseEnter(EventArgs e)
        {
            base.OnMouseEnter(e);
            if (Enabled) BackColor = Hover;
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            base.OnMouseLeave(e);
            BackColor = Enabled ? Base : Th.Panel;
        }

        protected override void OnEnabledChanged(EventArgs e)
        {
            base.OnEnabledChanged(e);
            BackColor = Enabled ? Base : Th.Panel;
            ForeColor = Enabled ? Th.Fg : Th.Dim;
            Cursor = Enabled ? Cursors.Hand : Cursors.Default;
        }
    }

    // 下拉框：owner-draw，保证深色下也好看
    class FlatCombo : ComboBox
    {
        public FlatCombo()
        {
            DrawMode = DrawMode.OwnerDrawFixed;
            DropDownStyle = ComboBoxStyle.DropDownList;
            FlatStyle = FlatStyle.Flat;
            BackColor = Th.Input;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            ItemHeight = 22;
        }

        protected override void OnDrawItem(DrawItemEventArgs e)
        {
            if (e.Index < 0) return;
            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            using (SolidBrush b = new SolidBrush(sel ? Th.Accent : Th.Input))
                e.Graphics.FillRectangle(b, e.Bounds);
            string t = Convert.ToString(Items[e.Index]);
            TextRenderer.DrawText(e.Graphics, t, Th.Ui,
                new Point(e.Bounds.X + 7, e.Bounds.Y + (e.Bounds.Height - Th.Ui.Height) / 2),
                sel ? Color.White : Th.Fg);
        }
    }

    // ============================ 小工具 ============================
    static class Ux
    {
        public static Label Lbl(string text, int x, int y, int w)
        {
            Label l = new Label();
            l.Text = text;
            l.ForeColor = Th.Dim;
            l.Font = Th.Ui;
            l.AutoSize = false;
            l.SetBounds(x, y, w, 20);
            l.TextAlign = ContentAlignment.MiddleLeft;
            l.BackColor = Color.Transparent;
            return l;
        }

        public static Label Val(string text, int x, int y, int w, Color c)
        {
            Label l = Lbl(text, x, y, w);
            l.ForeColor = c;
            return l;
        }

        // 给控件套一层 1px 描边
        public static Panel Field(Control inner, int x, int y, int w, int h)
        {
            Panel p = new Panel();
            p.SetBounds(x, y, w, h);
            p.Padding = new Padding(1);
            p.BackColor = Th.Border;
            TextBox tb = inner as TextBox;
            if (tb != null) tb.BorderStyle = BorderStyle.None;
            inner.Dock = DockStyle.Fill;
            inner.BackColor = Th.Input;
            inner.ForeColor = Th.Fg;
            inner.Font = Th.Ui;
            p.Controls.Add(inner);
            return p;
        }

        public static FlatBtn Btn(string text, int x, int y, int w, bool primary)
        {
            FlatBtn b = new FlatBtn();
            b.Text = text;
            b.SetBounds(x, y, w, 32);
            if (primary) { b.Base = Th.Accent; b.Hover = Th.AccHov; }
            return b;
        }

        public static void Dark(Form f)
        {
            f.BackColor = Th.Bg;
            f.ForeColor = Th.Fg;
            f.Font = Th.Ui;
            f.FormBorderStyle = FormBorderStyle.Sizable;
            f.MaximizeBox = true;
            f.StartPosition = FormStartPosition.CenterScreen;
        }
    }

    // ============================ 日志面板 ============================
    class LogView
    {
        public RichTextBox Box;

        public LogView()
        {
            Box = new RichTextBox();
            Box.BackColor = Th.LogBg;
            Box.ForeColor = Th.LogFg;
            Box.Font = Th.Mono;
            Box.BorderStyle = BorderStyle.None;
            Box.ReadOnly = true;
            Box.WordWrap = false;
            Box.HideSelection = false;
            Box.DetectUrls = false;
            Box.ScrollBars = RichTextBoxScrollBars.Both;
            Box.Dock = DockStyle.Fill;
        }

        public void Clear()
        {
            if (Box.IsDisposed) return;
            Box.Clear();
        }

        public string Text()
        {
            return Box.IsDisposed ? "" : Box.Text;
        }

        void AppendInner(string s, Color c)
        {
            if (Box.IsDisposed) return;
            if (Box.TextLength > 400000)
            {
                Box.Select(0, 150000);
                Box.SelectedText = "";
            }
            Box.SelectionStart = Box.TextLength;
            Box.SelectionLength = 0;
            Box.SelectionColor = c;
            Box.AppendText(s);
            Box.SelectionColor = Th.LogFg;
            Box.SelectionStart = Box.TextLength;
            Box.ScrollToCaret();
        }

        public void Append(string s, Color c)
        {
            if (Box.IsDisposed) return;
            if (Box.InvokeRequired)
            {
                try { Box.BeginInvoke(new Action<string, Color>(Append), new object[] { s, c }); }
                catch (Exception) { }
                return;
            }
            AppendInner(s, c);
        }

        public void Line(string s, Color c)
        {
            Append(s + "\r\n", c);
        }

        public void Info(string s) { Line(s, Th.LogFg); }
        public void Ok(string s) { Line(s, Th.Ok); }
        public void Warn(string s) { Line(s, Th.Warn); }
        public void Err(string s) { Line(s, Th.Err); }
        public void Cmd(string s) { Line(s, Th.Cyan); }
        public void Dim2(string s) { Line(s, Th.Dim); }

        public void Banner(string title)
        {
            Line("", Th.LogFg);
            Line("========== " + title + "  " + DateTime.Now.ToString("HH:mm:ss") + " ==========", Th.Cyan);
        }
    }

    // ============================ 进程运行器 ============================
    class ProcRunner
    {
        Process _p;
        public event Action<string> Out;
        public event Action<int> Exited;

        public bool Running
        {
            get
            {
                try { return _p != null && !_p.HasExited; }
                catch (Exception) { return false; }
            }
        }

        public int Pid
        {
            get { try { return _p == null ? -1 : _p.Id; } catch (Exception) { return -1; } }
        }

        public void Start(string workDir, string command, Dictionary<string, string> env)
        {
            Stop();

            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = Environment.GetEnvironmentVariable("COMSPEC");
            if (string.IsNullOrEmpty(psi.FileName)) psi.FileName = "cmd.exe";

            // cmd /c "整条命令"：cmd 在含特殊字符时会剥掉首尾引号，内部引号原样保留
            string inner = "chcp 65001>nul & " + command;
            psi.Arguments = "/c \"" + inner + "\"";
            psi.WorkingDirectory = workDir;
            psi.UseShellExecute = false;
            psi.RedirectStandardOutput = true;
            psi.RedirectStandardError = true;    // stderr 并入日志：子进程的告警/异常栈不再丢失
            psi.CreateNoWindow = true;

            if (env != null)
            {
                foreach (KeyValuePair<string, string> kv in env)
                    psi.EnvironmentVariables[kv.Key] = kv.Value;
            }

            _p = new Process();
            _p.StartInfo = psi;
            _p.EnableRaisingEvents = true;
            _p.Exited += OnExit;
            _p.Start();
            Pump(_p.StandardOutput.BaseStream);
            Pump(_p.StandardError.BaseStream);
        }

        // ---------- 输出解码（字节层，按行）----------
        // 子进程编码不统一：mvn（有 MAVEN_OPTS）输出 UTF-8，而 spring-boot:run fork 出来的
        // 应用 JVM 不继承 MAVEN_OPTS，在管道下按系统 GBK 写中文。固定按一种编码读必然有一边乱码。
        // 所以按行取原始字节：能严格按 UTF-8 解就用 UTF-8，失败回退 GBK —— 两种来源都正常显示。
        static readonly UTF8Encoding Utf8Strict = new UTF8Encoding(false, true);
        static readonly Encoding Gbk = Encoding.GetEncoding(936);

        void Pump(Stream s)
        {
            byte[] buf = new byte[8192];
            List<byte> acc = new List<byte>(8192);
            System.Threading.ThreadPool.QueueUserWorkItem(delegate(object st)
            {
                try
                {
                    while (true)
                    {
                        int n = s.Read(buf, 0, buf.Length);
                        if (n <= 0) break;
                        for (int i = 0; i < n; i++)
                        {
                            if (buf[i] == 0x0A) { EmitLine(acc); acc.Clear(); }
                            else acc.Add(buf[i]);
                        }
                    }
                    if (acc.Count > 0) { EmitLine(acc); acc.Clear(); }
                }
                catch (Exception) { }
            });
        }

        void EmitLine(List<byte> acc)
        {
            int end = acc.Count;
            if (end > 0 && acc[end - 1] == 0x0D) end--;
            if (end == 0) { Deliver(""); return; }
            int start = (end >= 3 && acc[0] == 0xEF && acc[1] == 0xBB && acc[2] == 0xBF) ? 3 : 0;   // 去 BOM
            byte[] line = new byte[end - start];
            acc.CopyTo(start, line, 0, end - start);
            string s;
            try { s = Utf8Strict.GetString(line); }
            catch (Exception) { s = Gbk.GetString(line); }
            Deliver(s);
        }

        void Deliver(string s)
        {
            s = Sys.StripAnsi(s);
            if (Out != null) { try { Out(s); } catch (Exception) { } }
        }

        void OnExit(object sender, EventArgs e)
        {
            int code = -1;
            try { code = _p.ExitCode; } catch (Exception) { }
            if (Exited != null) { try { Exited(code); } catch (Exception) { } }
        }

        // 杀掉整棵进程树（cmd -> mvn.cmd -> java）
        public void Stop()
        {
            if (_p == null) return;
            int pid = Pid;
            if (pid > 0)
            {
                try
                {
                    ProcessStartInfo k = new ProcessStartInfo("taskkill.exe", "/F /T /PID " + pid);
                    k.UseShellExecute = false;
                    k.CreateNoWindow = true;
                    k.RedirectStandardOutput = true;
                    k.RedirectStandardError = true;
                    using (Process kp = Process.Start(k))
                        kp.WaitForExit(6000);
                }
                catch (Exception) { }
            }
            try { if (!_p.HasExited) _p.Kill(); } catch (Exception) { }
            try { _p.Dispose(); } catch (Exception) { }
            _p = null;
        }
    }

    // ============================ 系统工具 ============================
    static class Sys
    {
        // Maven 本地仓库位置：优先读 settings.xml 的 localRepository（常被改到别的盘）
        public static string LocalRepo()
        {
            string home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            List<string> cands = new List<string>();
            cands.Add(Path.Combine(home, ".m2", "settings.xml"));
            string mh = Environment.GetEnvironmentVariable("M2_HOME");
            if (string.IsNullOrEmpty(mh)) mh = Environment.GetEnvironmentVariable("MAVEN_HOME");
            if (!string.IsNullOrEmpty(mh)) cands.Add(Path.Combine(mh, "conf", "settings.xml"));

            foreach (string f in cands)
            {
                if (!File.Exists(f)) continue;
                try
                {
                    XmlDocument doc = Pom.LoadXml(f);
                    foreach (XmlElement x in Pom.Descendants(doc.DocumentElement, "localRepository"))
                    {
                        string v = x.InnerText.Trim();
                        if (v.Length == 0) continue;
                        v = v.Replace("${user.home}", home);
                        return v;
                    }
                }
                catch (Exception) { }
            }
            return Path.Combine(home, ".m2", "repository");
        }

        // 从 dir 起向上查找文件，最多 max 层
        public static string FindUp(string dir, string file, int max)
        {
            string cur = dir;
            for (int i = 0; i < max && cur != null; i++)
            {
                string p = Path.Combine(cur, file);
                if (File.Exists(p)) return p;
                DirectoryInfo di = Directory.GetParent(cur);
                if (di == null) return null;
                cur = di.FullName;
            }
            return null;
        }

        // ANSI 转义序列（CSI / OSC）：vite 等 CLI 即便输出到管道也会带颜色码
        static readonly Regex AnsiRe = new Regex(@"\x1B(?:\[[0-9;?]*[ -/]*[@-~]|\][^\x07\x1B]*(?:\x07|\x1B\\))");

        public static string StripAnsi(string s)
        {
            if (string.IsNullOrEmpty(s)) return s;
            if (s.IndexOf('\x1B') < 0) return s;
            return AnsiRe.Replace(s, string.Empty);
        }

        // 端口占用时用来告诉用户即将结束的是哪个进程
        public static string ProcName(int pid)
        {
            try { using (Process p = Process.GetProcessById(pid)) return p.ProcessName; }
            catch (Exception) { return "?"; }
        }

        static Dictionary<string, string> _whichCache = new Dictionary<string, string>();

        // 在 PATH 里找可执行文件（支持 .cmd/.bat/.exe）
        public static string Which(string name)
        {
            string key = name.ToLowerInvariant();
            if (_whichCache.ContainsKey(key)) return _whichCache[key];

            string path = Environment.GetEnvironmentVariable("PATH");
            string[] exts = new string[] { ".cmd", ".bat", ".exe", "" };
            string found = null;
            if (!string.IsNullOrEmpty(path))
            {
                string[] dirs = path.Split(';');
                foreach (string d0 in dirs)
                {
                    string d = d0.Trim().Trim('"');
                    if (d.Length == 0) continue;
                    foreach (string ext in exts)
                    {
                        try
                        {
                            string full = Path.Combine(d, name + ext);
                            if (File.Exists(full)) { found = full; break; }
                        }
                        catch (Exception) { }
                    }
                    if (found != null) break;
                }
            }
            _whichCache[key] = found;
            return found;
        }

        // ---- 监听端口（判断「已在运行」的依据：不管是谁启动的）----

        public class PortOwner
        {
            public int Port;
            public int Pid;
            public string Name = "";
            public string Cmd = "";
        }

        // 一次 netstat，得到 监听端口 -> PID 列表
        public static Dictionary<int, List<int>> ListenPortMap()
        {
            Dictionary<int, List<int>> res = new Dictionary<int, List<int>>();
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo("netstat.exe", "-ano -p tcp");
                psi.UseShellExecute = false;
                psi.RedirectStandardOutput = true;
                psi.CreateNoWindow = true;
                psi.StandardOutputEncoding = Encoding.Default;
                using (Process p = Process.Start(psi))
                {
                    string outp = p.StandardOutput.ReadToEnd();
                    p.WaitForExit(8000);
                    foreach (string raw in outp.Split('\n'))
                    {
                        string l = raw.Trim();
                        if (!l.StartsWith("TCP", StringComparison.OrdinalIgnoreCase)) continue;
                        string[] parts = Regex.Split(l, @"\s+");
                        if (parts.Length < 5) continue;
                        if (!parts[3].Equals("LISTENING", StringComparison.OrdinalIgnoreCase)) continue;
                        int colon = parts[1].LastIndexOf(':');
                        if (colon <= 0) continue;
                        int port, pid;
                        if (!int.TryParse(parts[1].Substring(colon + 1), out port)) continue;
                        if (!int.TryParse(parts[4], out pid) || pid <= 0) continue;
                        if (!res.ContainsKey(port)) res[port] = new List<int>();
                        if (!res[port].Contains(pid)) res[port].Add(pid);
                    }
                }
            }
            catch (Exception) { }
            return res;
        }

        public static List<int> PidsOnPort(int port)
        {
            List<int> res = new List<int>();
            Dictionary<int, List<int>> m = ListenPortMap();
            List<int> v;
            if (m.TryGetValue(port, out v)) res.AddRange(v);
            return res;
        }

        // 端口上的监听进程（带进程名与命令行；命令行用来判断是否属于当前工程）
        public static List<PortOwner> OwnersOnPort(int port)
        {
            List<PortOwner> res = new List<PortOwner>();
            foreach (int pid in PidsOnPort(port))
            {
                PortOwner o = new PortOwner();
                o.Port = port;
                o.Pid = pid;
                o.Name = ProcName(pid);
                o.Cmd = CmdLineOf(pid);
                res.Add(o);
            }
            return res;
        }

        // 进程完整命令行（WMI；取不到就返回空串，绝不抛）
        public static string CmdLineOf(int pid)
        {
            try
            {
                using (ManagementObjectSearcher s = new ManagementObjectSearcher(
                    "SELECT CommandLine FROM Win32_Process WHERE ProcessId=" + pid))
                {
                    foreach (ManagementObject mo in s.Get())
                    {
                        object v = mo["CommandLine"];
                        if (v != null) return v.ToString();
                    }
                }
            }
            catch (Exception) { }
            return "";
        }

        // 命令行是否指向某目录：用于识别本项目进程（IDEA 启动的 / 本工具启动的 / 上次遗留的）
        public static bool InDir(string cmd, string dir)
        {
            if (string.IsNullOrEmpty(cmd) || string.IsNullOrEmpty(dir)) return false;
            string c = cmd.Replace('/', '\\').ToLowerInvariant();
            string d = dir.Replace('/', '\\').TrimEnd('\\').ToLowerInvariant();
            if (d.Length < 3) return false;
            if (c.Contains(d)) return true;
            int i = d.LastIndexOf('\\');
            if (i > 0)
            {
                int j = d.LastIndexOf('\\', i - 1);
                if (j > 0)
                {
                    string tail = d.Substring(j + 1);
                    if (tail.Length >= 5 && c.Contains(tail)) return true;
                }
            }
            return false;
        }

        public static string Describe(PortOwner o)
        {
            if (o == null) return "";
            return "PID " + o.Pid + (o.Name.Length > 0 ? " (" + o.Name + ")" : "");
        }

        public static string Shorten(string s, int max)
        {
            if (string.IsNullOrEmpty(s)) return "";
            s = s.Replace("\r", " ").Replace("\n", " ").Trim();
            if (s.Length <= max) return s;
            return s.Substring(0, max) + " ...";
        }

        public static void KillPid(int pid)
        {
            try
            {
                ProcessStartInfo k = new ProcessStartInfo("taskkill.exe", "/F /T /PID " + pid);
                k.UseShellExecute = false;
                k.CreateNoWindow = true;
                k.RedirectStandardOutput = true;
                k.RedirectStandardError = true;
                using (Process p = Process.Start(k)) p.WaitForExit(6000);
            }
            catch (Exception) { }
        }

        public static void OpenUrl(string url)
        {
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(url);
                psi.UseShellExecute = true;
                Process.Start(psi);
            }
            catch (Exception) { }
        }

        public static void OpenFolder(string dir)
        {
            try
            {
                if (!Directory.Exists(dir)) return;
                ProcessStartInfo psi = new ProcessStartInfo("explorer.exe", "\"" + dir + "\"");
                psi.UseShellExecute = true;
                Process.Start(psi);
            }
            catch (Exception) { }
        }
    }

    // ============================ 端口监听探测（异步） ============================
    // netstat 有开销，放线程池跑；结果回调用后台线程触发，调用方自行 BeginInvoke 到 UI
    class PortWatcher
    {
        public event Action<List<Sys.PortOwner>, int> Result;
        bool _busy;

        public bool Refresh(int port)
        {
            if (port <= 0 || _busy) return false;
            _busy = true;
            int p = port;
            System.Threading.ThreadPool.QueueUserWorkItem(delegate(object st)
            {
                List<Sys.PortOwner> r;
                try { r = Sys.OwnersOnPort(p); }
                catch (Exception) { r = new List<Sys.PortOwner>(); }
                _busy = false;
                Action<List<Sys.PortOwner>, int> h = Result;
                if (h != null) { try { h(r, p); } catch (Exception) { } }
            });
            return true;
        }
    }

    // ============================ 配置持久化（极简 ini） ============================
    static class Cfg
    {
        static string File(string app)
        {
            string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "DevLauncher");
            if (!Directory.Exists(dir)) Directory.CreateDirectory(dir);
            return Path.Combine(dir, app + ".ini");
        }

        public static Dictionary<string, string> Load(string app)
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            try
            {
                string f = File(app);
                if (!System.IO.File.Exists(f)) return d;
                foreach (string line in System.IO.File.ReadAllLines(f, Encoding.UTF8))
                {
                    int i = line.IndexOf('=');
                    if (i <= 0) continue;
                    string k = line.Substring(0, i).Trim();
                    string v = line.Substring(i + 1);
                    if (k.Length > 0) d[k] = v;
                }
            }
            catch (Exception) { }
            return d;
        }

        public static void Save(string app, Dictionary<string, string> d)
        {
            try
            {
                List<string> lines = new List<string>();
                foreach (KeyValuePair<string, string> kv in d)
                    lines.Add(kv.Key + "=" + kv.Value.Replace("\r", " ").Replace("\n", " "));
                System.IO.File.WriteAllLines(File(app), lines.ToArray(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    // ============================ 命令行自检入口 ============================
    // 用法：xxx.exe --run <工作目录> <命令> <输出文件> <秒数>
    // 跑指定秒数后停止，把捕获到的输出与退出码写入文件。用于验证 cmd 引号/编码/进程树清理。
    static class RunnerProbe
    {
        public static void Run(string workDir, string command, string outFile, int seconds)
        {
            List<string> lines = new List<string>();
            ProcRunner pr = new ProcRunner();
            pr.Out += delegate(string s) { lock (lines) { lines.Add(s); } };

            bool finished = false;
            int code = -999;
            System.Threading.ManualResetEvent done = new System.Threading.ManualResetEvent(false);
            pr.Exited += delegate(int c) { code = c; done.Set(); };

            pr.Start(workDir, command, null);
            finished = done.WaitOne(seconds * 1000, false);
            if (!finished) pr.Stop();
            System.Threading.Thread.Sleep(2000);

            StringBuilder sb = new StringBuilder();
            sb.AppendLine("finishedBeforeTimeout=" + finished);
            sb.AppendLine("exitCode=" + code);
            sb.AppendLine("lineCount=" + lines.Count);
            sb.AppendLine("---- output ----");
            lock (lines) { foreach (string l in lines) sb.AppendLine(l); }
            File.WriteAllText(outFile, sb.ToString(), new UTF8Encoding(false));
        }

        // xxx.exe --probe <port> <outFile>      探测端口上的监听者（自检用）
        // xxx.exe --stop-port <port> <outFile>  结束端口监听者（验证「非本窗口启动的也能停」）
        static bool HandlePortTools(string[] args)
        {
            if (args.Length < 3) return false;
            if (args[0] != "--probe" && args[0] != "--stop-port") return false;

            int port;
            int.TryParse(args[1], out port);
            StringBuilder sb = new StringBuilder();
            List<Sys.PortOwner> os = Sys.OwnersOnPort(port);
            sb.AppendLine("port=" + port);
            sb.AppendLine("count=" + os.Count);
            foreach (Sys.PortOwner o in os)
            {
                sb.AppendLine("owner=" + Sys.Describe(o));
                sb.AppendLine("cmd=" + Sys.Shorten(o.Cmd, 400));
            }
            if (args[0] == "--stop-port")
            {
                foreach (Sys.PortOwner o in os) Sys.KillPid(o.Pid);
                System.Threading.Thread.Sleep(900);
                sb.AppendLine("left=" + Sys.PidsOnPort(port).Count);
            }
            File.WriteAllText(args[2], sb.ToString(), new UTF8Encoding(false));
            return true;
        }

        public static bool TryHandle(string[] args)
        {
            if (args.Length >= 3 && HandlePortTools(args)) return true;
            if (args.Length < 5 || args[0] != "--run") return false;
            int sec = 20;
            int.TryParse(args[4], out sec);
            if (sec <= 0) sec = 20;
            try { Run(args[1], args[2], args[3], sec); }
            catch (Exception ex)
            {
                try { File.WriteAllText(args[3], "ERROR " + ex.ToString(), new UTF8Encoding(false)); }
                catch (Exception) { }
            }
            return true;
        }
    }

    // ============================ POM 解析 ============================
    class Pom
    {
        public string Path;
        public string Dir;
        public string ArtifactId;
        public string Version;
        public string Packaging;
        public List<string> Modules = new List<string>();
        public List<string> ProfileIds = new List<string>();
        public bool HasSpringBootPlugin;

        public static XmlDocument LoadXml(string path)
        {
            XmlDocument doc = new XmlDocument();
            doc.XmlResolver = null;
            doc.Load(path);
            return doc;
        }

        public static List<XmlElement> Kids(XmlNode n, string local)
        {
            List<XmlElement> res = new List<XmlElement>();
            foreach (XmlNode c in n.ChildNodes)
                if (c.NodeType == XmlNodeType.Element && c.LocalName == local) res.Add((XmlElement)c);
            return res;
        }

        public static XmlElement FirstKid(XmlNode n, string local)
        {
            foreach (XmlNode c in n.ChildNodes)
                if (c.NodeType == XmlNodeType.Element && c.LocalName == local) return (XmlElement)c;
            return null;
        }

        public static string Text(XmlNode n, string local)
        {
            XmlElement e = FirstKid(n, local);
            return e == null ? null : e.InnerText.Trim();
        }

        public static List<XmlElement> Descendants(XmlNode n, string local)
        {
            List<XmlElement> res = new List<XmlElement>();
            foreach (XmlNode c in n.ChildNodes)
            {
                if (c.NodeType != XmlNodeType.Element) continue;
                if (c.LocalName == local) res.Add((XmlElement)c);
                res.AddRange(Descendants(c, local));
            }
            return res;
        }

        public static Pom Read(string pomPath)
        {
            Pom p = new Pom();
            p.Path = pomPath;
            p.Dir = System.IO.Path.GetDirectoryName(pomPath);
            XmlDocument doc = LoadXml(pomPath);
            XmlElement root = doc.DocumentElement;
            p.ArtifactId = Text(root, "artifactId");
            p.Version = Text(root, "version");
            p.Packaging = Text(root, "packaging");
            if (p.Packaging == null) p.Packaging = "jar";

            XmlElement mods = FirstKid(root, "modules");
            if (mods != null)
                foreach (XmlElement m in Kids(mods, "module"))
                    if (m.InnerText.Trim().Length > 0) p.Modules.Add(m.InnerText.Trim());

            foreach (XmlElement pf in Descendants(root, "profile"))
            {
                string id = Text(pf, "id");
                if (!string.IsNullOrEmpty(id) && !p.ProfileIds.Contains(id)) p.ProfileIds.Add(id);
            }

            p.HasSpringBootPlugin = System.IO.File.ReadAllText(pomPath).Contains("spring-boot-maven-plugin");
            return p;
        }
    }
}
