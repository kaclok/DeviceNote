// Vite 前端一键启动器
// 编译：csc /target:winexe /out:ViteLauncher.exe Shared.cs ViteLauncher.cs
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;

namespace DevLaunch
{
    // ==================== 探测结果 ====================
    class NodeInfo
    {
        public string Dir = "";
        public string PkgPath = "";
        public string Name = "";
        public string Pm = "npm";
        public string PmExe = "";
        public string NodeDir = "";
        public List<string> ScriptNames = new List<string>();
        public List<string> ScriptCmds = new List<string>();
        public int DefaultIndex;
        public int Port;
        public bool HasModules;
        public bool ModulesStale;
        public bool Ok;
        public string Error = "";
        public string Summary = "";
    }

    static class Node
    {
        // ---------- 极简 JSON 取值（够用即可，不引外部程序集） ----------
        public static string JsonStr(string json, string key)
        {
            Match m = Regex.Match(json, "\"" + Regex.Escape(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            if (!m.Success) return null;
            return Unescape(m.Groups[1].Value);
        }

        public static string Unescape(string s)
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.Length; i++)
            {
                char c = s[i];
                if (c != '\\' || i + 1 >= s.Length) { sb.Append(c); continue; }
                i++;
                char n = s[i];
                if (n == 'n') sb.Append('\n');
                else if (n == 't') sb.Append('\t');
                else if (n == 'r') sb.Append('\r');
                else if (n == 'b') sb.Append('\b');
                else if (n == 'f') sb.Append('\f');
                else if (n == 'u' && i + 4 < s.Length)
                {
                    try { sb.Append((char)Convert.ToInt32(s.Substring(i + 1, 4), 16)); i += 4; }
                    catch (Exception) { sb.Append(n); }
                }
                else sb.Append(n);
            }
            return sb.ToString();
        }

        // 提取 "scripts": { ... } 块里的所有键值
        public static void ParseScripts(string json, List<string> names, List<string> cmds)
        {
            Match m = Regex.Match(json, "\"scripts\"\\s*:\\s*\\{");
            if (!m.Success) return;
            int i = m.Index + m.Length;
            int depth = 1;
            int start = i;
            bool inStr = false;
            while (i < json.Length && depth > 0)
            {
                char c = json[i];
                if (inStr)
                {
                    if (c == '\\') i++;
                    else if (c == '"') inStr = false;
                }
                else
                {
                    if (c == '"') inStr = true;
                    else if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
                if (depth == 0) break;
                i++;
            }
            string block = json.Substring(start, Math.Min(i - start, json.Length - start));

            Regex kv = new Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            foreach (Match k in kv.Matches(block))
            {
                names.Add(Unescape(k.Groups[1].Value));
                cmds.Add(Unescape(k.Groups[2].Value));
            }
        }

        // ---------- 主探测 ----------
        public static NodeInfo Detect(string dir, string preferScript, string nodeDir)
        {
            NodeInfo ni = new NodeInfo();
            ni.Dir = dir;
            ni.NodeDir = nodeDir == null ? "" : nodeDir.Trim();
            if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir))
            {
                ni.Error = "目录不存在";
                return ni;
            }

            string pkg = Path.Combine(dir, "package.json");
            if (!File.Exists(pkg))
            {
                string up = Sys.FindUp(dir, "package.json", 4);
                if (up != null) { pkg = up; dir = Path.GetDirectoryName(up); ni.Dir = dir; }
            }
            if (!File.Exists(pkg))
            {
                ni.Error = "未找到 package.json（请选择前端工程根目录）";
                return ni;
            }
            ni.PkgPath = pkg;

            string json;
            try { json = File.ReadAllText(pkg, Encoding.UTF8); }
            catch (Exception ex) { ni.Error = "package.json 读取失败：" + ex.Message; return ni; }

            ni.Name = JsonStr(json, "name");
            if (ni.Name == null) ni.Name = new DirectoryInfo(dir).Name;

            ParseScripts(json, ni.ScriptNames, ni.ScriptCmds);

            // 默认脚本：上次用的 → dev/serve/start/本地
            int def = -1;
            if (!string.IsNullOrEmpty(preferScript))
                for (int i = 0; i < ni.ScriptNames.Count; i++)
                    if (ni.ScriptNames[i] == preferScript) { def = i; break; }
            if (def < 0)
            {
                for (int i = 0; i < ni.ScriptNames.Count; i++)
                {
                    string n = ni.ScriptNames[i].ToLowerInvariant();
                    string c = ni.ScriptCmds[i].ToLowerInvariant();
                    if (n == "dev" || n == "serve" || n == "start") { def = i; break; }
                }
            }
            if (def < 0)
            {
                for (int i = 0; i < ni.ScriptNames.Count; i++)
                {
                    string n = ni.ScriptNames[i].ToLowerInvariant();
                    string c = ni.ScriptCmds[i].ToLowerInvariant();
                    if (n.Contains("dev") || c.StartsWith("vite")) { def = i; break; }
                }
            }
            if (def < 0) def = 0;
            ni.DefaultIndex = def;

            // 包管理器：配置了 Node.js 目录时优先用目录里的 pm.cmd，否则走 PATH
            if (File.Exists(Path.Combine(dir, "pnpm-lock.yaml"))) ni.Pm = "pnpm";
            else if (File.Exists(Path.Combine(dir, "yarn.lock"))) ni.Pm = "yarn";
            else if (File.Exists(Path.Combine(dir, "package-lock.json"))) ni.Pm = "npm";
            else ni.Pm = "npm";
            ResolvePm(ni);

            // 依赖状态
            string nm = Path.Combine(dir, "node_modules");
            ni.HasModules = Directory.Exists(nm);
            if (ni.HasModules)
            {
                try
                {
                    DateTime nmTime = Directory.GetLastWriteTimeUtc(nm);
                    string[] locks = new string[] { "package-lock.json", "pnpm-lock.yaml", "yarn.lock" };
                    foreach (string lk in locks)
                    {
                        string lp = Path.Combine(dir, lk);
                        if (File.Exists(lp) && File.GetLastWriteTimeUtc(lp) > nmTime) { ni.ModulesStale = true; break; }
                    }
                }
                catch (Exception) { }
            }

            ni.Port = GuessPort(dir);
            ni.Summary = ni.Pm + " · " + ni.ScriptNames.Count + " 个脚本" + (ni.HasModules ? " · 依赖已安装" : " · 依赖未安装");
            ni.Ok = true;
            return ni;
        }

        // 解析包管理器可执行文件：Node.js 目录优先，其次 PATH（Sys.Which 有缓存，目录变更时直接重算）
        public static void ResolvePm(NodeInfo ni)
        {
            ni.PmExe = null;
            if (ni.NodeDir.Length > 0 && Directory.Exists(ni.NodeDir))
            {
                string cand = Path.Combine(ni.NodeDir, ni.Pm + ".cmd");
                if (File.Exists(cand)) ni.PmExe = cand;
            }
            if (ni.PmExe == null) ni.PmExe = Sys.Which(ni.Pm);
            if (ni.PmExe == null) ni.PmExe = Sys.Which("npm");
        }

        public static int GuessPort(string dir)
        {
            string[] cfgs = new string[] {
                "vite.config.js","vite.config.ts","vite.config.mjs","vite.config.cjs","vite.config.mts"
            };
            foreach (string c in cfgs)
            {
                string f = Path.Combine(dir, c);
                if (!File.Exists(f)) continue;
                string t = "";
                try { t = File.ReadAllText(f); } catch (Exception) { continue; }
                Match sm = Regex.Match(t, @"\bserver\s*:\s*\{");
                string scope = sm.Success ? t.Substring(sm.Index, Math.Min(3000, t.Length - sm.Index)) : t;
                Match pm = Regex.Match(scope, @"\bport\s*:\s*(\d{2,5})");
                if (pm.Success)
                {
                    int v;
                    if (int.TryParse(pm.Groups[1].Value, out v)) return v;
                }
            }
            return 0;
        }

        public static string RunCmd(NodeInfo ni, string script)
        {
            string pm = "\"" + (ni.PmExe != null ? ni.PmExe : ni.Pm) + "\"";
            string s = script;
            if (s.IndexOf(' ') >= 0) s = "\"" + s + "\"";
            return pm + " run " + s;
        }

        public static string InstallCmd(NodeInfo ni)
        {
            string pm = "\"" + (ni.PmExe != null ? ni.PmExe : ni.Pm) + "\"";
            return pm + " install";
        }

        // 打包用脚本：优先 "build"，其次名字里含 build 的第一个
        public static string BuildScript(NodeInfo ni)
        {
            for (int i = 0; i < ni.ScriptNames.Count; i++)
                if (ni.ScriptNames[i] == "build") return "build";
            for (int i = 0; i < ni.ScriptNames.Count; i++)
                if (ni.ScriptNames[i].ToLower().Contains("build")) return ni.ScriptNames[i];
            return null;
        }

        // 产物目录：从 vite.config 的 outDir 推断（支持 (env || 'dist') + '-后缀' 形态），默认 dist
        public static string GuessOutDir(string dir)
        {
            string[] cfgs = new string[] {
                "vite.config.js","vite.config.ts","vite.config.mjs","vite.config.cjs","vite.config.mts"
            };
            foreach (string c in cfgs)
            {
                string f = Path.Combine(dir, c);
                if (!File.Exists(f)) continue;
                string t = "";
                try { t = File.ReadAllText(f); } catch (Exception) { continue; }
                Match m = Regex.Match(t, @"outDir\s*:\s*([^\r\n]+)");
                if (!m.Success) break;
                string expr = m.Groups[1].Value;
                string baseDir = "dist";
                Match q = Regex.Match(expr, "'([^']*)'|\"([^\"]*)\"");
                if (q.Success) baseDir = q.Groups[1].Success ? q.Groups[1].Value : q.Groups[2].Value;
                if (baseDir.Length == 0) baseDir = "dist";
                Match suf = Regex.Match(expr, "'(-[A-Za-z0-9._-]+)'|\"(-[A-Za-z0-9._-]+)\"");
                if (suf.Success) baseDir += suf.Groups[1].Success ? suf.Groups[1].Value : suf.Groups[2].Value;
                return baseDir;
            }
            return "dist";
        }

        public static void DumpDetect(string dir, string outFile, string nodeDir)
        {
            NodeInfo ni = Detect(dir, null, nodeDir);
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("ok=" + ni.Ok);
            sb.AppendLine("error=" + ni.Error);
            sb.AppendLine("dir=" + ni.Dir);
            sb.AppendLine("name=" + ni.Name);
            sb.AppendLine("pm=" + ni.Pm);
            sb.AppendLine("pmExe=" + ni.PmExe);
            sb.AppendLine("nodeDir=" + ni.NodeDir);
            sb.AppendLine("scripts=" + string.Join(" | ", ni.ScriptNames.ToArray()));
            sb.AppendLine("cmds=" + string.Join(" | ", ni.ScriptCmds.ToArray()));
            sb.AppendLine("default=" + (ni.ScriptNames.Count > ni.DefaultIndex ? ni.ScriptNames[ni.DefaultIndex] : ""));
            sb.AppendLine("port=" + ni.Port);
            sb.AppendLine("outDir=" + GuessOutDir(dir));
            sb.AppendLine("hasModules=" + ni.HasModules);
            sb.AppendLine("modulesStale=" + ni.ModulesStale);
            sb.AppendLine("summary=" + ni.Summary);
            sb.AppendLine("command=" + (ni.Ok && ni.ScriptNames.Count > 0 ? RunCmd(ni, ni.ScriptNames[ni.DefaultIndex]) : ""));
            File.WriteAllText(outFile, sb.ToString(), new UTF8Encoding(false));
        }
    }

    // ==================== 主窗体 ====================
    class ViteForm : Form
    {
        TextBox txtDir;
        TextBox txtNode;
        Label lblNodeHint;
        FlatCombo cbScript;
        Label lblStatus, lblPort, lblRunState, lblCmd;
        CheckBox chkInstall, chkKill, chkOpen;
        FlatBtn btnRun, btnStop, btnInstall, btnCmd;
        FlatBtn btnPack, btnPackDir;
        LogView log;
        ProcRunner runner = new ProcRunner();
        ProcRunner packRunner = new ProcRunner();
        NodeInfo info = new NodeInfo();
        System.Windows.Forms.Timer pollTimer;

        // ---- 「已在运行」探测（含 IDEA / 上次遗留的进程）----
        PortWatcher portWatch = new PortWatcher();
        Sys.PortOwner extRun;
        int extPort;
        string extKey = "";
        int actualPort;            // 从 vite 日志校正出的实际端口（strictPort=false 时端口会顺延）
        int tick;
        // ---- 自检模式（--uidump）----
        public string PresetDir;
        public string PresetScript;
        public int PresetPort;
        public string DumpAfter;

        public ViteForm()
        {
            Text = "Vite 前端一键启动器";
            ClientSize = new Size(920, 744);
            MinimumSize = new Size(760, 564);
            Ux.Dark(this);
            BuildUi();
            runner.Out += OnOut;
            runner.Exited += OnProcExit;
            packRunner.Out += OnPackOut;
            packRunner.Exited += OnPackExit;
            portWatch.Result += OnPortProbe;

            pollTimer = new System.Windows.Forms.Timer();
            pollTimer.Interval = 1000;
            pollTimer.Tick += delegate { OnTick(); };
            pollTimer.Start();

            Load += delegate { OnLoadInit(); };
            Shown += delegate { ApplyPreset(); };
            FormClosing += OnClosing;
        }

        void BuildUi()
        {
            Label l1 = Ux.Lbl("项目文件夹", 20, 22, 76);
            Controls.Add(l1);

            txtDir = new TextBox();
            Panel pf = Ux.Field(txtDir, 100, 18, 540, 28);
            Controls.Add(pf);

            FlatBtn btnBrowse = Ux.Btn("浏览...", 648, 18, 74, false);
            btnBrowse.Click += delegate { Browse(); };
            Controls.Add(btnBrowse);

            FlatBtn btnDetect = Ux.Btn("重新检测", 728, 18, 90, false);
            btnDetect.Click += delegate { DetectNow(true); };
            Controls.Add(btnDetect);

            Label l2 = Ux.Lbl("启动脚本", 20, 62, 76);
            Controls.Add(l2);
            cbScript = new FlatCombo();
            Panel ps = Ux.Field(cbScript, 100, 58, 300, 28);
            Controls.Add(ps);
            cbScript.SelectedIndexChanged += delegate { OnScriptChanged(); };

            Label l4 = Ux.Lbl("端口", 424, 62, 34);
            Controls.Add(l4);
            lblPort = Ux.Val("-", 462, 62, 70, Th.Cyan);
            Controls.Add(lblPort);

            lblCmd = Ux.Val("", 540, 62, 360, Th.Dim);
            lblCmd.Font = Th.Mono;
            Controls.Add(lblCmd);

            // ---- Node.js 目录（留空 = 系统 PATH；配置后优先用该目录的 npm/pnpm/yarn）----
            Label ln = Ux.Lbl("Node.js 目录", 20, 96, 76);
            Controls.Add(ln);
            txtNode = new TextBox();
            Controls.Add(Ux.Field(txtNode, 100, 92, 440, 28));
            FlatBtn btnNode = Ux.Btn("浏览", 546, 92, 56, false);
            btnNode.Height = 28;
            btnNode.Click += delegate { PickDir(txtNode, "选择 Node.js 安装目录（其下应有 npm.cmd / node.exe）"); };
            Controls.Add(btnNode);
            lblNodeHint = Ux.Val("留空 = 使用系统 PATH", 610, 96, 290, Th.Dim);
            Controls.Add(lblNodeHint);
            txtNode.TextChanged += delegate { OnNodeChanged(); };

            lblStatus = Ux.Val("请选择前端工程目录", 20, 130, 880, Th.Dim);
            Controls.Add(lblStatus);

            chkInstall = MkCheck("依赖未安装时自动安装", 20, 160);
            chkKill = MkCheck("启动前结束占用端口的进程", 250, 160);
            chkOpen = MkCheck("启动后打开浏览器", 480, 160);
            chkInstall.Checked = true;
            chkKill.Checked = true;
            chkOpen.Checked = true;
            Controls.Add(chkInstall);
            Controls.Add(chkKill);
            Controls.Add(chkOpen);

            btnRun = Ux.Btn("▶  启动", 20, 194, 120, true);
            btnRun.Height = 36;
            btnRun.Click += delegate { Run(); };
            Controls.Add(btnRun);

            btnStop = Ux.Btn("■  停止", 148, 194, 100, false);
            btnStop.Height = 36;
            btnStop.Enabled = false;
            btnStop.Click += delegate { Stop(); };
            Controls.Add(btnStop);

            btnInstall = Ux.Btn("安装依赖", 256, 194, 100, false);
            btnInstall.Height = 36;
            btnInstall.Click += delegate { InstallOnly(); };
            Controls.Add(btnInstall);

            btnCmd = Ux.Btn("查看命令", 364, 194, 90, false);
            btnCmd.Height = 36;
            btnCmd.Click += delegate { ShowCommand(); };
            Controls.Add(btnCmd);

            FlatBtn btnFolder = Ux.Btn("打开目录", 462, 194, 90, false);
            btnFolder.Height = 36;
            btnFolder.Click += delegate { if (info.Ok) Sys.OpenFolder(info.Dir); };
            Controls.Add(btnFolder);

            FlatBtn btnBrowser = Ux.Btn("打开页面", 560, 194, 90, false);
            btnBrowser.Height = 36;
            btnBrowser.Click += delegate { OpenBrowser(); };
            Controls.Add(btnBrowser);

            lblRunState = Ux.Val("● 未运行", 680, 194, 220, Th.Dim);
            lblRunState.Font = Th.UiB;
            Controls.Add(lblRunState);

            // ---- 打包行 ----
            btnPack = Ux.Btn("打包", 20, 236, 100, false);
            btnPack.Height = 36;
            btnPack.Click += delegate { PackOnly(); };
            Controls.Add(btnPack);

            btnPackDir = Ux.Btn("产物目录", 128, 236, 100, false);
            btnPackDir.Height = 36;
            btnPackDir.Click += delegate { OpenPackDir(); };
            Controls.Add(btnPackDir);

            Label lPack = Ux.Val("npm run build，产物目录读自 vite.config 的 outDir", 236, 244, 440, Th.Dim);
            Controls.Add(lPack);

            Label l5 = Ux.Val("运行日志", 20, 288, 100, Th.Fg);
            l5.Font = Th.UiB;
            Controls.Add(l5);

            FlatBtn btnCopy = Ux.Btn("复制", 830, 282, 70, false);
            btnCopy.Height = 26;
            btnCopy.Click += delegate
            {
                try { if (log.Text().Length > 0) Clipboard.SetText(log.Text()); }
                catch (Exception) { }
            };
            Controls.Add(btnCopy);

            FlatBtn btnCls = Ux.Btn("清空", 752, 282, 70, false);
            btnCls.Height = 26;
            btnCls.Click += delegate { log.Clear(); };
            Controls.Add(btnCls);

            log = new LogView();
            Panel pl = new Panel();
            pl.SetBounds(20, 314, 880, 410);
            pl.Padding = new Padding(1);
            pl.BackColor = Th.Border;
            pl.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            log.Box.Dock = DockStyle.Fill;
            pl.Controls.Add(log.Box);
            Controls.Add(pl);
            log.Box.BackColor = Th.LogBg;
        }

        CheckBox MkCheck(string text, int x, int y)
        {
            CheckBox c2 = new CheckBox();
            c2.Text = text;
            c2.ForeColor = Th.Fg;
            c2.Font = Th.Ui;
            c2.SetBounds(x, y, 226, 24);
            c2.BackColor = Color.Transparent;
            c2.FlatStyle = FlatStyle.Standard;
            c2.Cursor = Cursors.Hand;
            return c2;
        }

        void OnLoadInit()
        {
            Dictionary<string, string> c = Cfg.Load("vite");
            string last = null;
            c.TryGetValue("dir", out last);
            string lastNode = null;
            c.TryGetValue("node", out lastNode);
            if (!string.IsNullOrEmpty(lastNode)) txtNode.Text = lastNode;
            log.Info("Vite 前端一键启动器就绪。");
            log.Dim2("选择前端工程目录 → 选择启动脚本 → 点击「启动」。Node.js 目录留空即用系统 PATH。");
            if (!string.IsNullOrEmpty(last) && Directory.Exists(last))
            {
                txtDir.Text = last;
                DetectNow(false);
            }
        }

        void OnClosing(object sender, FormClosingEventArgs e)
        {
            if (runner.Running)
            {
                DialogResult r = MessageBox.Show(this, "前端服务仍在运行，是否停止并退出？", "确认",
                    MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (r != DialogResult.Yes) { e.Cancel = true; return; }
            }
            runner.Stop();
            if (string.IsNullOrEmpty(DumpAfter)) Save();   // 自检模式不覆盖用户配置
        }

        void Save()
        {
            Dictionary<string, string> c = new Dictionary<string, string>();
            c["dir"] = txtDir.Text.Trim();
            c["node"] = txtNode.Text.Trim();
            if (cbScript.SelectedItem != null) c["script"] = cbScript.SelectedItem.ToString();
            Cfg.Save("vite", c);
        }

        void PickDir(TextBox tb, string desc)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = desc;
            if (Directory.Exists(tb.Text.Trim())) d.SelectedPath = tb.Text.Trim();
            if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.SelectedPath;
        }

        string CurNodeDir()
        {
            return txtNode.Text.Trim();
        }

        // Node.js 目录变更：轻量重解析包管理器路径 + 刷新提示（不做全量探测，避免刷日志）
        void OnNodeChanged()
        {
            string nd = CurNodeDir();
            if (nd.Length == 0)
            {
                lblNodeHint.Text = "留空 = 使用系统 PATH";
                lblNodeHint.ForeColor = Th.Dim;
            }
            else if (!Directory.Exists(nd))
            {
                lblNodeHint.Text = "⚠ 目录不存在";
                lblNodeHint.ForeColor = Th.Err;
            }
            else
            {
                string pmCmd = Path.Combine(nd, (info.Ok ? info.Pm : "npm") + ".cmd");
                lblNodeHint.Text = File.Exists(pmCmd) ? ShortenPath(pmCmd, 46) : "⚠ 目录下没有 " + info.Pm + ".cmd";
                lblNodeHint.ForeColor = File.Exists(pmCmd) ? Th.Ok : Th.Warn;
            }
            if (info.Ok)
            {
                info.NodeDir = nd;
                Node.ResolvePm(info);
            }
        }

        static string ShortenPath(string s, int max)
        {
            if (s.Length <= max) return s;
            return "..." + s.Substring(s.Length - max);
        }

        void Browse()
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = "选择前端工程根目录（含 package.json）";
            if (Directory.Exists(txtDir.Text.Trim())) d.SelectedPath = txtDir.Text.Trim();
            if (d.ShowDialog(this) == DialogResult.OK)
            {
                txtDir.Text = d.SelectedPath;
                DetectNow(true);
            }
        }

        void DetectNow(bool verbose)
        {
            string dir = txtDir.Text.Trim();
            if (verbose) log.Banner("检测工程");
            if (!Directory.Exists(dir))
            {
                info = new NodeInfo();
                info.Error = "目录不存在";
                lblStatus.ForeColor = Th.Err;
                lblStatus.Text = "✗ 目录不存在";
                cbScript.Items.Clear();
                return;
            }

            Dictionary<string, string> c = Cfg.Load("vite");
            string lastScript = null;
            c.TryGetValue("script", out lastScript);
            info = Node.Detect(dir, lastScript, CurNodeDir());

            if (!info.Ok)
            {
                lblStatus.ForeColor = Th.Err;
                lblStatus.Text = "✗ " + info.Error;
                cbScript.Items.Clear();
                if (verbose) log.Err("检测失败：" + info.Error);
                return;
            }

            cbScript.Items.Clear();
            for (int i = 0; i < info.ScriptNames.Count; i++)
                cbScript.Items.Add(info.ScriptNames[i] + "   →   " + info.ScriptCmds[i]);
            if (cbScript.Items.Count > 0) cbScript.SelectedIndex = info.DefaultIndex;

            lblStatus.ForeColor = Th.Ok;
            lblStatus.Text = "✓ " + info.Name + "   |   " + info.Summary + "   |   " + info.Dir;

            if (verbose)
            {
                log.Ok("检测通过：" + info.Name);
                log.Info("工程目录   : " + info.Dir);
                if (info.NodeDir.Length > 0) log.Info("Node.js 目录: " + info.NodeDir);
                log.Info("包管理器   : " + info.Pm + "  (" + info.PmExe + ")");
                log.Info("可用脚本   : " + string.Join(", ", info.ScriptNames.ToArray()));
                log.Info("依赖状态   : " + (info.HasModules ? (info.ModulesStale ? "已安装，但 lock/package.json 更新过，建议重新安装" : "已安装") : "未安装"));
                if (info.Port > 0) log.Info("推测端口   : " + info.Port);
            }
            if (info.NodeDir.Length > 0 && !Directory.Exists(info.NodeDir))
                log.Warn("⚠ 配置的 Node.js 目录不存在：" + info.NodeDir + "，将退回系统 PATH。");
            if (!info.HasModules) log.Warn("未发现 node_modules，启动前需要先安装依赖。");
            else if (info.ModulesStale) log.Warn("lock 文件或 package.json 比 node_modules 新，建议点「安装依赖」。");
            RefreshPort();
        }

        int CurIndex()
        {
            return cbScript.SelectedIndex;
        }

        string CurScript()
        {
            int i = CurIndex();
            if (i < 0 || i >= info.ScriptNames.Count) return null;
            return info.ScriptNames[i];
        }

        void OnScriptChanged()
        {
            RefreshPort();
        }

        void RefreshPort()
        {
            if (!info.Ok) { lblPort.Text = "-"; lblCmd.Text = ""; return; }
            int i = CurIndex();
            if (i >= 0 && i < info.ScriptCmds.Count)
            {
                string c = info.ScriptCmds[i].ToLowerInvariant();
                lblCmd.Text = c.Contains("build") ? "（构建，非开发服务）" : "";
            }
            info.Port = Node.GuessPort(info.Dir);
            actualPort = 0;
            extKey = "";
            lblPort.Text = info.Port > 0 ? info.Port.ToString() : "默认";
            PortWatchNow();
        }

        void OpenBrowser()
        {
            if (!info.Ok) return;
            int p = CurPort();
            if (p <= 0) { MessageBox.Show(this, "未能从 vite 配置中读出端口。", "提示"); return; }
            Sys.OpenUrl("http://localhost:" + p);
        }

        // 配置了 Node.js 目录时，把该目录前置到子进程 PATH（npm.cmd 内部找 node.exe 也走 PATH）
        Dictionary<string, string> NodeEnv()
        {
            string nd = CurNodeDir();
            if (nd.Length == 0 || !Directory.Exists(nd)) return null;
            Dictionary<string, string> env = new Dictionary<string, string>();
            string old = Environment.GetEnvironmentVariable("PATH");
            env["PATH"] = nd + ";" + (old == null ? "" : old);
            return env;
        }

        void Run()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择有效的前端工程目录。", "提示"); return; }
            if (runner.Running) { MessageBox.Show(this, "前端服务已在运行，请先停止。", "提示"); return; }
            if (packRunner.Running) { MessageBox.Show(this, "正在打包，请等待打包完成。", "提示"); return; }
            string script = CurScript();
            if (string.IsNullOrEmpty(script)) { MessageBox.Show(this, "请选择要执行的脚本。", "提示"); return; }

            string nd = CurNodeDir();
            if (nd.Length > 0 && !Directory.Exists(nd))
            {
                DialogResult r = MessageBox.Show(this,
                    "配置的 Node.js 目录不存在：\n" + nd + "\n\n将退回系统 PATH 继续，仍要启动吗？",
                    "确认", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (r != DialogResult.Yes) return;
            }

            Save();

            if (chkKill.Checked)
            {
                int pk = CurPort();
                if (pk > 0)
                {
                    List<Sys.PortOwner> os = Sys.OwnersOnPort(pk);
                    if (os.Count > 0)
                    {
                        log.Warn("端口 " + pk + " 被占用：" + JoinOwners(os) + "，正在结束...");
                        foreach (Sys.PortOwner o in os) Sys.KillPid(o.Pid);
                        System.Threading.Thread.Sleep(600);
                        extRun = null; extKey = "";
                    }
                }
            }

            bool needInstall = !info.HasModules;
            List<string> parts = new List<string>();
            if (needInstall && chkInstall.Checked) parts.Add(Node.InstallCmd(info));
            parts.Add(Node.RunCmd(info, script));
            string cmd = string.Join(" && ", parts.ToArray());

            if (needInstall && !chkInstall.Checked)
            {
                DialogResult r = MessageBox.Show(this,
                    "未发现 node_modules，依赖没有安装。直接启动通常会失败，仍要继续吗？",
                    "确认", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (r != DialogResult.Yes) return;
            }

            log.Banner("启动前端服务");
            log.Info("脚本      : " + script);
            log.Info("工作目录  : " + info.Dir);
            if (nd.Length > 0) log.Info("Node.js   : " + nd + "（已前置到 PATH）");
            log.Cmd("$ " + cmd);
            log.Line("", Th.Fg);

            try { runner.Start(info.Dir, cmd, NodeEnv()); }
            catch (Exception ex) { log.Err("启动失败：" + ex.Message); return; }

            btnRun.Enabled = false; btnInstall.Enabled = false; btnStop.Enabled = true;
            RefreshRunState();

            if (chkOpen.Checked)
            {
                System.Threading.Timer t = null;
                t = new System.Threading.Timer(delegate (object st)
                {
                    try
                    {
                        System.Threading.Thread.Sleep(4000);
                        int p = CurPort();
                        if (p > 0) Sys.OpenUrl("http://localhost:" + p);
                    }
                    catch (Exception) { }
                    try { t.Dispose(); } catch (Exception) { }
                }, null, 4000, System.Threading.Timeout.Infinite);
            }
        }

        void InstallOnly()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            if (runner.Running) { MessageBox.Show(this, "前端服务正在运行，请先停止。", "提示"); return; }
            if (packRunner.Running) { MessageBox.Show(this, "正在打包，请等待打包完成。", "提示"); return; }
            string cmd = Node.InstallCmd(info);
            log.Banner("安装依赖");
            log.Cmd("$ " + cmd);
            runner.Start(info.Dir, cmd, NodeEnv());
            btnRun.Enabled = false; btnInstall.Enabled = false; btnStop.Enabled = true;
        }

        // ---------- 打包 ----------
        void PackOnly()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            if (runner.Running || packRunner.Running) { MessageBox.Show(this, "已有任务在运行，请等待完成或先停止。", "提示"); return; }
            string script = Node.BuildScript(info);
            if (script == null) { MessageBox.Show(this, "package.json 里没有找到 build 类脚本。", "提示"); return; }
            string cmd = Node.RunCmd(info, script);
            log.Banner("打包前端");
            log.Info("脚本      : " + script);
            log.Info("产物目录  : " + Path.Combine(info.Dir, Node.GuessOutDir(info.Dir)));
            log.Cmd("$ " + cmd);
            log.Line("", Th.Fg);
            try { packRunner.Start(info.Dir, cmd, NodeEnv()); }
            catch (Exception ex) { log.Err("打包启动失败：" + ex.Message); return; }
            btnRun.Enabled = false; btnInstall.Enabled = false; btnPack.Enabled = false;
        }

        void OpenPackDir()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            string d = Path.Combine(info.Dir, Node.GuessOutDir(info.Dir));
            if (!Directory.Exists(d)) { MessageBox.Show(this, "产物目录还不存在，请先打包：\n" + d, "提示"); return; }
            Sys.OpenFolder(d);
        }

        void OnPackOut(string line)
        {
            Color c = Th.LogFg;
            string t = line.TrimStart();
            if (t.StartsWith("error") || t.Contains("ERR!") || t.Contains("error during build")) c = Th.Err;
            else if (t.Contains("built in ") || t.Contains("built in")) c = Th.Ok;
            log.Line(line, c);
        }

        void OnPackExit(int code)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    btnRun.Enabled = true; btnInstall.Enabled = true; btnPack.Enabled = true;
                    if (code == 0) log.Ok(">>> 打包完成，产物目录：" + Path.Combine(info.Dir, Node.GuessOutDir(info.Dir)));
                    else log.Err(">>> 打包失败（exit " + code + "）");
                }));
            }
            catch (Exception) { }
        }

        void ShowCommand()
        {
            if (!info.Ok) return;
            string script = CurScript();
            if (script == null) return;
            log.Banner("命令预览");
            List<string> parts = new List<string>();
            if (!info.HasModules && chkInstall.Checked) parts.Add(Node.InstallCmd(info));
            parts.Add(Node.RunCmd(info, script));
            log.Cmd("$ " + string.Join(" && ", parts.ToArray()));
            if (info.NodeDir.Length > 0 && Directory.Exists(info.NodeDir))
                log.Dim2("Node.js：" + info.NodeDir + "（已前置到 PATH）");
            log.Dim2("（不会真正执行）");
        }

        void Stop()
        {
            // 1) 本窗口启动的：杀整棵 cmd -> npm.cmd -> node 进程树
            if (runner.Running)
            {
                log.Warn("正在停止本窗口启动的进程（含子进程 node）...");
                runner.Stop();
                btnRun.Enabled = true; btnInstall.Enabled = true;
                RefreshRunState();
                return;
            }

            // 2) 端口上已经在跑的：IDEA / WebStorm 启动的，或上次关闭本窗口时没停掉的
            Sys.PortOwner o = extRun;
            int port = extPort;
            if (o == null)
            {
                int p = CurPort();
                if (p > 0)
                {
                    List<Sys.PortOwner> now = Sys.OwnersOnPort(p);
                    if (now.Count > 0) { o = now[0]; port = p; }
                }
            }
            if (o == null)
            {
                log.Dim2("没有检测到正在运行的服务（本窗口没启动过，端口上也没有监听者）。");
                RefreshRunState();
                return;
            }

            bool mine = Sys.InDir(o.Cmd, info.Dir);
            string who = Sys.Describe(o) + (port > 0 ? "，端口 " + port : "");
            string msg = "本窗口没有启动过服务，但检测到" + (mine ? "本工程的" : "其它") + "进程正在监听：\n\n"
                + who + (o.Cmd.Length > 0 ? "\n" + Sys.Shorten(o.Cmd, 220) : "")
                + "\n\n它可能是在 WebStorm / IDEA 里启动的，也可能是上次关闭本窗口时没有停止。\n确定要结束它吗？";
            if (MessageBox.Show(this, msg, "结束进程", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes)
                return;

            log.Warn("正在结束 " + who + " ...");
            Sys.KillPid(o.Pid);
            System.Threading.Thread.Sleep(500);
            extRun = null; extKey = "";
            RefreshRunState();
            PortWatchNow();
        }

        void OnOut(string line)
        {
            Color c = Th.LogFg;
            string t = line.TrimStart();
            if (t.StartsWith("error") || t.Contains("ERR!") || t.Contains("failed to load config"))
                c = Th.Err;
            else if (t.Contains("ready in") || t.Contains("Local:") || t.Contains("localhost:"))
                c = Th.Ok;
            else if (t.StartsWith("warn") || t.Contains("WARN"))
                c = Th.Warn;
            CatchPort(line);
            log.Line(line, c);
        }

        // 从 vite 日志校正实际端口（strictPort=false 时 4177 被占会自动顺延到 4178）
        void CatchPort(string line)
        {
            if (actualPort > 0) return;
            Match m = Regex.Match(line, @"http://localhost:(\d+)");
            if (!m.Success) return;
            int p;
            if (!int.TryParse(m.Groups[1].Value, out p) || p <= 0 || p > 65535) return;
            actualPort = p;
            try
            {
                if (!log.Box.IsDisposed)
                    log.Box.BeginInvoke(new Action(delegate
                    {
                        lblPort.Text = p.ToString();
                        PortWatchNow();
                    }));
            }
            catch (Exception) { }
        }

        void OnProcExit(int code)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    if (code == 0) log.Ok(">>> 进程已正常退出（exit 0）");
                    else log.Err(">>> 进程已退出（exit " + code + "）");
                    btnRun.Enabled = true; btnInstall.Enabled = true;
                    extKey = "";
                    RefreshRunState();
                    PortWatchNow();
                }));
            }
            catch (Exception) { }
        }

        // ---------- 「已在运行」探测 ----------
        int CurPort()
        {
            if (actualPort > 0) return actualPort;
            return info.Ok ? info.Port : 0;
        }

        void OnTick()
        {
            tick++;
            if (tick % 2 == 1) PortWatchNow();
            RefreshRunState();
        }

        void PortWatchNow()
        {
            int p = CurPort();
            if (p <= 0) { extRun = null; extPort = 0; return; }
            portWatch.Refresh(p);
        }

        static string JoinOwners(List<Sys.PortOwner> os)
        {
            List<string> s = new List<string>();
            foreach (Sys.PortOwner o in os) s.Add(Sys.Describe(o));
            return string.Join(", ", s.ToArray());
        }

        void OnPortProbe(List<Sys.PortOwner> owners, int port)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    if (port != CurPort()) return;
                    extPort = port;
                    extRun = owners.Count > 0 ? owners[0] : null;

                    string key = extRun == null ? "" : ("pid:" + extRun.Pid);
                    if (key != extKey)
                    {
                        if (extRun != null && !runner.Running)
                        {
                            bool mine = Sys.InDir(extRun.Cmd, info.Dir);
                            log.Warn("检测到端口 " + port + " 上已经有服务在运行：" + Sys.Describe(extRun)
                                + (mine ? "（本工程）" : "（非本工程）") + "，可直接点「停止」结束它。");
                            if (extRun.Cmd.Length > 0) log.Dim2("    " + Sys.Shorten(extRun.Cmd, 150));
                        }
                        else if (extRun == null && extKey.Length > 0 && !runner.Running)
                        {
                            log.Dim2("端口 " + port + " 已不再被监听。");
                        }
                        extKey = key;
                    }
                    RefreshRunState();
                }));
            }
            catch (Exception) { }
        }

        void RefreshRunState()
        {
            Sys.PortOwner o = extRun;
            bool self = runner.Running;

            if (self)
            {
                string pid = runner.Pid.ToString();
                if (o != null) pid = Sys.Describe(o);
                lblRunState.ForeColor = Th.Ok;
                lblRunState.Text = "● 运行中  PID " + pid;
                btnStop.Text = "■  停止";
            }
            else if (o != null)
            {
                bool mine = Sys.InDir(o.Cmd, info.Dir);
                lblRunState.ForeColor = Th.Warn;
                lblRunState.Text = "● 运行中  " + Sys.Describe(o) + (mine ? " · 非本窗口" : " · 非本工程");
                btnStop.Text = "■  停止(外部)";
            }
            else
            {
                lblRunState.ForeColor = Th.Dim;
                lblRunState.Text = "● 未运行";
                btnStop.Text = "■  停止";
            }
            btnStop.Enabled = self || o != null;
        }

        // ---------- 自检模式（--uidump）----------
        void ApplyPreset()
        {
            if (!string.IsNullOrEmpty(PresetDir) && Directory.Exists(PresetDir))
            {
                txtDir.Text = PresetDir;
                DetectNow(false);
            }
            if (!string.IsNullOrEmpty(PresetScript))
            {
                for (int i = 0; i < cbScript.Items.Count; i++)
                    if (cbScript.Items[i].ToString().StartsWith(PresetScript + " ")) { cbScript.SelectedIndex = i; break; }
            }
            if (PresetPort > 0)
            {
                actualPort = PresetPort;
                lblPort.Text = PresetPort.ToString();
            }
            PortWatchNow();
            RefreshRunState();

            if (!string.IsNullOrEmpty(DumpAfter))
            {
                System.Windows.Forms.Timer t = new System.Windows.Forms.Timer();
                t.Interval = 4500;
                t.Tick += delegate { t.Stop(); WriteDump(); Close(); };
                t.Start();
            }
        }

        void WriteDump()
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("dir=" + txtDir.Text.Trim());
                sb.AppendLine("port=" + CurPort());
                sb.AppendLine("script=" + (CurScript() == null ? "" : CurScript()));
                sb.AppendLine("status=" + lblStatus.Text);
                sb.AppendLine("runState=" + lblRunState.Text);
                sb.AppendLine("stopText=" + btnStop.Text);
                sb.AppendLine("stopEnabled=" + btnStop.Enabled);
                sb.AppendLine("runEnabled=" + btnRun.Enabled);
                sb.AppendLine("extPid=" + (extRun == null ? 0 : extRun.Pid));
                sb.AppendLine("extName=" + (extRun == null ? "" : extRun.Name));
                sb.AppendLine("extInDir=" + (extRun == null ? "" : Sys.InDir(extRun.Cmd, info.Dir).ToString()));
                sb.AppendLine("extCmd=" + (extRun == null ? "" : Sys.Shorten(extRun.Cmd, 300)));
                File.WriteAllText(DumpAfter, sb.ToString(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    static class Program1
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (RunnerProbe.TryHandle(args)) return;
            if (args.Length >= 5 && args[0] == "--uidump")
            {
                int ov;
                int.TryParse(args[3], out ov);
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                ViteForm f = new ViteForm();
                f.PresetDir = args[1];
                f.PresetScript = args[2];
                f.PresetPort = ov;
                f.DumpAfter = args[4];
                Application.Run(f);
                return;
            }
            if (args.Length >= 3 && args[0] == "--detect")
            {
                string nodeDir = args.Length >= 4 ? args[3] : null;
                try { Node.DumpDetect(args[1], args[2], nodeDir); }
                catch (Exception ex)
                {
                    File.WriteAllText(args[2], "ok=False\r\nerror=" + ex.ToString(), new UTF8Encoding(false));
                }
                return;
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new ViteForm());
        }
    }
}
