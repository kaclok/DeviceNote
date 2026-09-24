// DevLauncher 启动台 —— 后端 + 前端 合并窗体
// 复用 Shared.cs（主题/进程/探测）+ SpringBootLauncher.cs（Boot/ToolChain）+ ViteLauncher.cs（Node）
// 编译：csc /target:winexe /main:DevLaunch.Program0 /out:DevLauncher.exe Shared.cs SpringBootLauncher.cs ViteLauncher.cs CombinedLauncher.cs
// C# 5 语法（.NET 4.0 csc），勿使用字符串插值、?.、nameof 等
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;

namespace DevLaunch
{
    // ==================== 设置对话框（工具链 + 行为开关） ====================
    class ComboSettingsDialog : Form
    {
        public string Jdk = "", MavenDir = "", Repo = "", SettingsFile = "", NodeDir = "";
        public bool AutoInstall = true, KillPort = true;

        TextBox tbJdk, tbMaven, tbRepo, tbSettings, tbNode;
        CheckBox chkInstall, chkKill;

        public ComboSettingsDialog(string jdk, string maven, string repo, string settings, string node,
                                   bool autoInstall, bool killPort)
        {
            Text = "工具链与行为设置";
            ClientSize = new Size(470, 300);
            BackColor = Th.Bg;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterParent;

            MkRow("JDK 目录", 14, tbJdk = new TextBox(), false, "留空 = 使用系统 JAVA_HOME");
            MkRow("Maven 目录", 48, tbMaven = new TextBox(), false, "留空 = 工程 mvnw 或 PATH 里的 mvn");
            MkRow("Maven 仓库", 82, tbRepo = new TextBox(), false, "留空 = 跟随 settings.xml 的 localRepository");
            MkRow("settings.xml", 116, tbSettings = new TextBox(), true, "留空 = Maven 默认");
            MkRow("Node.js 目录", 150, tbNode = new TextBox(), false, "留空 = 系统 PATH（前端用）");

            chkInstall = MkCheck("后端启动前自动安装依赖模块（多模块，源码有变更时）", 16, 188, autoInstall);
            chkKill = MkCheck("启动前结束占用端口的进程", 16, 216, killPort);
            Controls.Add(chkInstall);
            Controls.Add(chkKill);

            FlatBtn ok = Ux.Btn("确定", 250, 252, 90, true);
            ok.Click += delegate { ApplyAndClose(); };
            Controls.Add(ok);

            FlatBtn cancel = Ux.Btn("取消", 350, 252, 90, false);
            cancel.Click += delegate { DialogResult = DialogResult.Cancel; Close(); };
            Controls.Add(cancel);

            tbJdk.Text = jdk; tbMaven.Text = maven; tbRepo.Text = repo;
            tbSettings.Text = settings; tbNode.Text = node;
        }

        void MkRow(string label, int y, TextBox tb, bool isFile, string tip)
        {
            Label l = Ux.Lbl(label, 16, y + 4, 88);
            Controls.Add(l);
            Controls.Add(Ux.Field(tb, 108, y, 300, 26));
            FlatBtn b = Ux.Btn("浏览", 414, y, 44, false);
            b.Height = 26;
            b.Click += delegate
            {
                if (isFile)
                {
                    OpenFileDialog d = new OpenFileDialog();
                    d.Title = "选择 settings.xml";
                    d.Filter = "settings.xml|settings.xml|XML 文件|*.xml|全部文件|*.*";
                    if (File.Exists(tb.Text.Trim())) { d.InitialDirectory = Path.GetDirectoryName(tb.Text.Trim()); d.FileName = Path.GetFileName(tb.Text.Trim()); }
                    if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.FileName;
                }
                else
                {
                    FolderBrowserDialog d = new FolderBrowserDialog();
                    d.Description = tip;
                    if (Directory.Exists(tb.Text.Trim())) d.SelectedPath = tb.Text.Trim();
                    if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.SelectedPath;
                }
            };
            Controls.Add(b);
            ToolTip t = new ToolTip();
            t.SetToolTip(l, tip);
        }

        CheckBox MkCheck(string text, int x, int y, bool val)
        {
            CheckBox c = new CheckBox();
            c.Text = text;
            c.ForeColor = Th.Fg;
            c.Font = Th.Ui;
            c.SetBounds(x, y, 440, 24);
            c.BackColor = Color.Transparent;
            c.FlatStyle = FlatStyle.Standard;
            c.Checked = val;
            c.Cursor = Cursors.Hand;
            return c;
        }

        void ApplyAndClose()
        {
            Jdk = tbJdk.Text.Trim();
            MavenDir = tbMaven.Text.Trim();
            Repo = tbRepo.Text.Trim();
            SettingsFile = tbSettings.Text.Trim();
            NodeDir = tbNode.Text.Trim();
            AutoInstall = chkInstall.Checked;
            KillPort = chkKill.Checked;
            DialogResult = DialogResult.OK;
            Close();
        }
    }

    // ==================== 合并主窗体 ====================
    class ComboForm : Form
    {
        // ---- 后端侧 ----
        TextBox txtBDir;
        FlatCombo cbProfile;
        FlatBtn bBRun, bBStop;
        Label lblBState, lblBLogHead;
        LogView logB;
        ProcRunner runB = new ProcRunner();
        BootInfo bi = new BootInfo();
        PortWatcher watchB = new PortWatcher();
        Sys.PortOwner extB;
        int extPortB;
        string extKeyB = "";
        int actualB;
        ToolTip tipB;

        // ---- 前端侧 ----
        TextBox txtFDir;
        FlatCombo cbScript;
        FlatBtn bFRun, bFStop;
        Label lblFState, lblFLogHead;
        LogView logF;
        ProcRunner runF = new ProcRunner();
        ProcRunner packRunB = new ProcRunner();
        ProcRunner packRunF = new ProcRunner();
        NodeInfo ni = new NodeInfo();
        PortWatcher watchF = new PortWatcher();
        Sys.PortOwner extF;
        int extPortF;
        string extKeyF = "";
        int actualF;
        ToolTip tipF;

        // ---- 公共 ----
        FlatBtn bAllRun, bAllStop;
        FlatBtn bPackB, bPackDirB, bPackF, bPackDirF;
        CheckBox chkOpen;
        System.Windows.Forms.Timer timer;
        int tick;

        // ---- 工具链（设置对话框编辑） ----
        string jdk = "", mavenDir = "", repo = "", settingsFile = "", nodeDir = "";
        bool autoInstall = true, killPort = true;

        // ---- 自检模式（--uidump） ----
        public string PresetBDir;
        public string PresetFDir;
        public string DumpAfter;

        public ComboForm()
        {
            Text = "启动台 · 后端 + 前端";
            ClientSize = new Size(880, 560);
            MinimumSize = new Size(760, 460);
            Ux.Dark(this);
            BuildUi();

            runB.Out += OnOutB;
            runB.Exited += OnExitB;
            watchB.Result += OnProbeB;
            runF.Out += OnOutF;
            runF.Exited += OnExitF;
            packRunB.Out += OnPackOutB;
            packRunB.Exited += OnPackExitB;
            packRunF.Out += OnPackOutF;
            packRunF.Exited += OnPackExitF;
            watchF.Result += OnProbeF;

            timer = new System.Windows.Forms.Timer();
            timer.Interval = 1000;
            timer.Tick += delegate
            {
                tick++;
                if (tick % 2 == 1) { WatchNowB(); WatchNowF(); }
                SetStateB();
                SetStateF();
            };
            timer.Start();

            Load += delegate { OnLoadInit(); };
            Shown += delegate { OnShownInit(); };
            FormClosing += OnClosingAll;
        }

        // ---------- 界面 ----------
        void BuildUi()
        {
            // ---- 第 1 行：后端 ----
            Label lb = Ux.Val("后端", 12, 17, 36, Th.Cyan);
            lb.Font = Th.UiB;
            Controls.Add(lb);

            txtBDir = new TextBox();
            Controls.Add(Ux.Field(txtBDir, 50, 13, 330, 26));
            FlatBtn bb = Ux.Btn("浏览", 386, 13, 50, false);
            bb.Height = 26;
            bb.Click += delegate { BrowseDir(txtBDir, "选择 Spring Boot 工程目录（多模块选根目录或任意子模块均可）", delegate { DetectB(true); }); };
            Controls.Add(bb);

            cbProfile = new FlatCombo();
            Controls.Add(Ux.Field(cbProfile, 442, 13, 120, 26));
            tipB = new ToolTip();
            tipB.SetToolTip(cbProfile, "配置 profile（来自 application-*.yml），切换后端口跟着变");
            cbProfile.SelectedIndexChanged += delegate { RefreshPortB(); };

            bBRun = Ux.Btn("▶", 568, 13, 46, true);
            bBRun.Height = 26;
            tipB.SetToolTip(bBRun, "启动后端");
            bBRun.Click += delegate { RunB(); };
            Controls.Add(bBRun);

            bBStop = Ux.Btn("■", 618, 13, 46, false);
            bBStop.Height = 26;
            bBStop.Enabled = false;
            tipB.SetToolTip(bBStop, "停止后端");
            bBStop.Click += delegate { StopB(); };
            Controls.Add(bBStop);

            lblBState = Ux.Val("未配置", 672, 17, 196, Th.Dim);
            Controls.Add(lblBState);
            tipB.SetToolTip(lblBState, "后端状态：端口（来自 application-<profile>.yml）+ 运行状态。启动后以日志里的实际端口为准。");

            // ---- 第 2 行：前端 ----
            Label lf = Ux.Val("前端", 12, 51, 36, Th.Cyan);
            lf.Font = Th.UiB;
            Controls.Add(lf);

            txtFDir = new TextBox();
            Controls.Add(Ux.Field(txtFDir, 50, 47, 290, 26));
            FlatBtn fb = Ux.Btn("浏览", 346, 47, 50, false);
            fb.Height = 26;
            fb.Click += delegate { BrowseDir(txtFDir, "选择前端工程根目录（含 package.json）", delegate { DetectF(true); }); };
            Controls.Add(fb);

            cbScript = new FlatCombo();
            Controls.Add(Ux.Field(cbScript, 402, 47, 160, 26));
            tipF = new ToolTip();
            tipF.SetToolTip(cbScript, "启动脚本（来自 package.json scripts）");
            cbScript.SelectedIndexChanged += delegate { RefreshPortF(); };

            bFRun = Ux.Btn("▶", 568, 47, 46, true);
            bFRun.Height = 26;
            tipF.SetToolTip(bFRun, "启动前端");
            bFRun.Click += delegate { RunF(); };
            Controls.Add(bFRun);

            bFStop = Ux.Btn("■", 618, 47, 46, false);
            bFStop.Height = 26;
            bFStop.Enabled = false;
            tipF.SetToolTip(bFStop, "停止前端");
            bFStop.Click += delegate { StopF(); };
            Controls.Add(bFStop);

            lblFState = Ux.Val("未配置", 672, 51, 196, Th.Dim);
            Controls.Add(lblFState);
            tipF.SetToolTip(lblFState, "前端状态：端口（来自 vite.config.*）+ 运行状态。启动后以日志里的实际端口为准。");

            // ---- 第 3 行：全局动作 ----
            bAllRun = Ux.Btn("▶ 全部启动", 50, 82, 110, true);
            bAllRun.Height = 30;
            bAllRun.Click += delegate { RunB(); RunF(); };
            Controls.Add(bAllRun);

            bAllStop = Ux.Btn("■ 全部停止", 166, 82, 100, false);
            bAllStop.Height = 30;
            bAllStop.Click += delegate { StopB(); StopF(); };
            Controls.Add(bAllStop);

            FlatBtn btnSettings = Ux.Btn("设置…", 272, 82, 62, false);
            btnSettings.Height = 30;
            btnSettings.Click += delegate { OpenSettings(); };
            Controls.Add(btnSettings);

            chkOpen = new CheckBox();
            chkOpen.Text = "启动后打开页面";
            chkOpen.ForeColor = Th.Fg;
            chkOpen.Font = Th.Ui;
            chkOpen.SetBounds(344, 86, 140, 24);
            chkOpen.BackColor = Color.Transparent;
            chkOpen.FlatStyle = FlatStyle.Standard;
            chkOpen.Checked = true;
            chkOpen.Cursor = Cursors.Hand;
            Controls.Add(chkOpen);

            FlatBtn btnOpenB = Ux.Btn("后端页面", 492, 82, 76, false);
            btnOpenB.Height = 30;
            btnOpenB.Click += delegate { int p = CurPortB(); if (p > 0) Sys.OpenUrl("http://localhost:" + p); };
            Controls.Add(btnOpenB);

            FlatBtn btnOpenF = Ux.Btn("前端页面", 574, 82, 76, false);
            btnOpenF.Height = 30;
            btnOpenF.Click += delegate { int p = CurPortF(); if (p > 0) Sys.OpenUrl("http://localhost:" + p); };
            Controls.Add(btnOpenF);

            FlatBtn btnClear = Ux.Btn("清空", 760, 84, 52, false);
            btnClear.Height = 26;
            btnClear.Click += delegate { logB.Clear(); logF.Clear(); };
            Controls.Add(btnClear);

            FlatBtn btnCopy = Ux.Btn("复制", 816, 84, 52, false);
            btnCopy.Height = 26;
            btnCopy.Click += delegate
            {
                try
                {
                    string t = "===== 后端 =====\r\n" + logB.Text() + "\r\n===== 前端 =====\r\n" + logF.Text();
                    if (t.Trim().Length > 0) Clipboard.SetText(t);
                }
                catch (Exception) { }
            };
            Controls.Add(btnCopy);

            // ---- 第 4 行：打包 ----
            bPackB = Ux.Btn("打包后端", 50, 116, 100, false);
            bPackB.Height = 28;
            tipB.SetToolTip(bPackB, "mvn package（跳过测试），产物在启动模块 target 下");
            bPackB.Click += delegate { PackB(); };
            Controls.Add(bPackB);

            bPackDirB = Ux.Btn("后端产物", 156, 116, 84, false);
            bPackDirB.Height = 28;
            bPackDirB.Click += delegate { OpenPackDirB(); };
            Controls.Add(bPackDirB);

            bPackF = Ux.Btn("打包前端", 248, 116, 100, false);
            bPackF.Height = 28;
            tipF.SetToolTip(bPackF, "npm run build，产物目录读自 vite.config 的 outDir");
            bPackF.Click += delegate { PackF(); };
            Controls.Add(bPackF);

            bPackDirF = Ux.Btn("前端产物", 354, 116, 84, false);
            bPackDirF.Height = 28;
            bPackDirF.Click += delegate { OpenPackDirF(); };
            Controls.Add(bPackDirF);

            Label lPackHint = Ux.Val("打包不影响已启动的服务；产物目录不存在时请先打包", 446, 121, 396, Th.Dim);
            Controls.Add(lPackHint);

            // ---- 日志区：左右分栏，同屏可看 ----
            Panel plLog = new Panel();
            plLog.SetBounds(12, 150, 856, 398);
            plLog.Padding = new Padding(0);
            plLog.BackColor = Th.Bg;
            plLog.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;

            SplitContainer sc = new SplitContainer();
            sc.Dock = DockStyle.Fill;
            sc.BackColor = Th.Bg;
            sc.SplitterWidth = 5;

            sc.Panel1.Controls.Add(MkLogPane("后端日志", out lblBLogHead, out logB));
            sc.Panel2.Controls.Add(MkLogPane("前端日志", out lblFLogHead, out logF));
            plLog.Controls.Add(sc);
            Controls.Add(plLog);

            Shown += delegate
            {
                try { sc.SplitterDistance = Math.Max(200, plLog.Width / 2 - 2); }
                catch (Exception) { }
            };
        }

        // 一个日志分栏：头部标签 + 日志视图
        Control MkLogPane(string title, out Label head, out LogView log)
        {
            Panel p = new Panel();
            p.Dock = DockStyle.Fill;
            p.Padding = new Padding(1);
            p.BackColor = Th.Border;

            head = Ux.Val(title, 2, 1, 400, Th.Fg);
            head.Font = Th.UiB;
            head.Dock = DockStyle.Top;
            head.Height = 20;
            head.BackColor = Th.Panel;
            p.Controls.Add(head);

            log = new LogView();
            log.Box.Dock = DockStyle.Fill;
            p.Controls.Add(log.Box);
            return p;
        }

        void BrowseDir(TextBox tb, string desc, Action after)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = desc;
            if (Directory.Exists(tb.Text.Trim())) d.SelectedPath = tb.Text.Trim();
            if (d.ShowDialog(this) == DialogResult.OK)
            {
                tb.Text = d.SelectedPath;
                after();
            }
        }

        // ---------- 配置 ----------
        static string G(Dictionary<string, string> d, string k)
        {
            string v;
            d.TryGetValue(k, out v);
            return v == null ? "" : v;
        }

        void OnLoadInit()
        {
            Dictionary<string, string> c = Cfg.Load("combined");
            if (!c.ContainsKey("bdir"))
            {
                // 首次使用：继承两个独立工具已保存的配置
                Dictionary<string, string> sb = Cfg.Load("springboot");
                if (sb.ContainsKey("dir")) c["bdir"] = G(sb, "dir");
                if (sb.ContainsKey("profile")) c["bprofile"] = G(sb, "profile");
                if (sb.ContainsKey("jdk")) c["jdk"] = G(sb, "jdk");
                if (sb.ContainsKey("maven")) c["maven"] = G(sb, "maven");
                if (sb.ContainsKey("repo")) c["repo"] = G(sb, "repo");
                if (sb.ContainsKey("settings")) c["settings"] = G(sb, "settings");
            }
            if (!c.ContainsKey("fdir"))
            {
                Dictionary<string, string> vt = Cfg.Load("vite");
                if (vt.ContainsKey("dir")) c["fdir"] = G(vt, "dir");
                if (vt.ContainsKey("script")) c["fscript"] = G(vt, "script");
                if (vt.ContainsKey("node")) c["node"] = G(vt, "node");
            }

            txtBDir.Text = G(c, "bdir");
            txtFDir.Text = G(c, "fdir");
            jdk = G(c, "jdk");
            mavenDir = G(c, "maven");
            repo = G(c, "repo");
            settingsFile = G(c, "settings");
            nodeDir = G(c, "node");
            autoInstall = G(c, "autoInstall") != "0";
            killPort = G(c, "killPort") != "0";
            string op;
            if (c.TryGetValue("openPage", out op)) chkOpen.Checked = op != "0";

            logB.Info("启动台就绪。后端（上）/ 前端（下）各自配置目录后，可单独启动也可「全部启动」。");
            logB.Dim2("日志左右分栏同屏显示；「设置…」里可配置 JDK / Maven / 仓库 / settings / Node 目录。");
            logF.Dim2("（前端日志显示在这里）");

            if (txtBDir.Text.Trim().Length > 0 && Directory.Exists(txtBDir.Text.Trim())) DetectB(false);
            if (txtFDir.Text.Trim().Length > 0 && Directory.Exists(txtFDir.Text.Trim())) DetectF(false);
        }

        void OnShownInit()
        {
            if (!string.IsNullOrEmpty(PresetBDir)) { txtBDir.Text = PresetBDir; DetectB(false); }
            if (!string.IsNullOrEmpty(PresetFDir)) { txtFDir.Text = PresetFDir; DetectF(false); }
            SetStateB();
            SetStateF();
            if (!string.IsNullOrEmpty(DumpAfter))
            {
                System.Windows.Forms.Timer t = new System.Windows.Forms.Timer();
                t.Interval = 5000;
                t.Tick += delegate { t.Stop(); WriteDump(); Close(); };
                t.Start();
            }
        }

        void Save()
        {
            Dictionary<string, string> c = new Dictionary<string, string>();
            c["bdir"] = txtBDir.Text.Trim();
            c["bprofile"] = CurProfileB();
            c["fdir"] = txtFDir.Text.Trim();
            c["fscript"] = CurScriptF() == null ? "" : CurScriptF();
            c["jdk"] = jdk;
            c["maven"] = mavenDir;
            c["repo"] = repo;
            c["settings"] = settingsFile;
            c["node"] = nodeDir;
            c["autoInstall"] = autoInstall ? "1" : "0";
            c["killPort"] = killPort ? "1" : "0";
            c["openPage"] = chkOpen.Checked ? "1" : "0";
            Cfg.Save("combined", c);
        }

        void OpenSettings()
        {
            ComboSettingsDialog d = new ComboSettingsDialog(jdk, mavenDir, repo, settingsFile, nodeDir, autoInstall, killPort);
            if (d.ShowDialog(this) == DialogResult.OK)
            {
                jdk = d.Jdk; mavenDir = d.MavenDir; repo = d.Repo; settingsFile = d.SettingsFile; nodeDir = d.NodeDir;
                autoInstall = d.AutoInstall; killPort = d.KillPort;
                if (bi.Ok) DetectB(false);
                if (ni.Ok) DetectF(false);
                Save();
            }
        }

        void ApplyChain()
        {
            ToolChain t = new ToolChain();
            t.Jdk = jdk;
            t.MavenDir = mavenDir;
            t.Repo = repo;
            t.SettingsFile = settingsFile;
            ToolChain.Current = t;
        }

        // ---------- 检测：后端 ----------
        void DetectB(bool verbose)
        {
            string dir = txtBDir.Text.Trim();
            if (dir.Length == 0 || !Directory.Exists(dir))
            {
                bi = new BootInfo();
                bi.Error = dir.Length == 0 ? "未配置" : "目录不存在";
                if (verbose) logB.Err("检测失败：" + bi.Error);
                SetStateB();
                return;
            }
            ApplyChain();
            bi = Boot.Detect(dir);
            if (!bi.Ok)
            {
                if (verbose) logB.Err("检测失败：" + bi.Error);
                SetStateB();
                return;
            }

            Dictionary<string, string> c = Cfg.Load("combined");
            string lastPf = G(c, "bprofile");
            cbProfile.Items.Clear();
            int sel = 0;
            for (int i = 0; i < bi.Profiles.Count; i++)
            {
                cbProfile.Items.Add(bi.Profiles[i]);
                if (lastPf.Length > 0 && bi.Profiles[i].Equals(lastPf, StringComparison.OrdinalIgnoreCase)) sel = i;
                else if (lastPf.Length == 0 && bi.Profiles[i].Equals("dev", StringComparison.OrdinalIgnoreCase)) sel = i;
            }
            if (cbProfile.Items.Count > 0) cbProfile.SelectedIndex = sel;

            if (verbose)
            {
                logB.Banner("检测后端工程");
                logB.Ok("检测通过：" + bi.Summary);
                logB.Info("工程根目录 : " + bi.RootDir);
                logB.Info("可用 profile: " + string.Join(", ", bi.Profiles.ToArray()));
                List<string> pms = new List<string>();
                foreach (string p2 in bi.Profiles)
                {
                    int pp = Boot.GuessPort(bi.LaunchDir, p2);
                    pms.Add(p2 + (pp > 0 ? "→" + pp : "→默认"));
                }
                if (pms.Count > 0) logB.Info("端口映射   : " + string.Join("   ", pms.ToArray()));
                logB.Info("JAVA_HOME  : " + (string.IsNullOrEmpty(bi.JavaHome) ? "（未设置）" : bi.JavaHome + "  " + bi.JavaVer));
                if (jdk.Length > 0 && !Directory.Exists(jdk)) logB.Warn("⚠ 设置里的 JDK 目录不存在：" + jdk);
                if (mavenDir.Length > 0 && !File.Exists(Path.Combine(mavenDir, "bin", "mvn.cmd"))) logB.Warn("⚠ 设置里的 Maven 目录下没有 bin\\mvn.cmd：" + mavenDir);
                if (bi.NeedInstall) logB.Warn("依赖模块尚未安装或已过期，启动时会自动执行 install。");
            }
            RefreshPortB();
        }

        void RefreshPortB()
        {
            if (!bi.Ok) { SetStateB(); return; }
            bi.Port = Boot.GuessPort(bi.LaunchDir, CurProfileB());
            actualB = 0;
            extKeyB = "";
            WatchNowB();
            SetStateB();
        }

        string CurProfileB()
        {
            return cbProfile.SelectedItem == null ? "" : cbProfile.SelectedItem.ToString();
        }

        int CurPortB()
        {
            return actualB > 0 ? actualB : (bi.Ok ? bi.Port : 0);
        }

        // ---------- 检测：前端 ----------
        void DetectF(bool verbose)
        {
            string dir = txtFDir.Text.Trim();
            if (dir.Length == 0 || !Directory.Exists(dir))
            {
                ni = new NodeInfo();
                ni.Error = dir.Length == 0 ? "未配置" : "目录不存在";
                if (verbose) logF.Err("检测失败：" + ni.Error);
                SetStateF();
                return;
            }
            Dictionary<string, string> c = Cfg.Load("combined");
            ni = Node.Detect(dir, G(c, "fscript"), nodeDir);
            if (!ni.Ok)
            {
                if (verbose) logF.Err("检测失败：" + ni.Error);
                SetStateF();
                return;
            }

            cbScript.Items.Clear();
            for (int i = 0; i < ni.ScriptNames.Count; i++)
                cbScript.Items.Add(ni.ScriptNames[i] + "   →   " + ni.ScriptCmds[i]);
            if (cbScript.Items.Count > 0) cbScript.SelectedIndex = ni.DefaultIndex;

            if (verbose)
            {
                logF.Banner("检测前端工程");
                logF.Ok("检测通过：" + ni.Name);
                logF.Info("工程目录   : " + ni.Dir);
                logF.Info("包管理器   : " + ni.Pm + "  (" + ni.PmExe + ")");
                logF.Info("可用脚本   : " + string.Join(", ", ni.ScriptNames.ToArray()));
                logF.Info("依赖状态   : " + (ni.HasModules ? (ni.ModulesStale ? "已安装（lock 更新过，建议重装）" : "已安装") : "未安装"));
                if (ni.Port > 0) logF.Info("推测端口   : " + ni.Port);
                if (nodeDir.Length > 0 && !Directory.Exists(nodeDir)) logF.Warn("⚠ 设置里的 Node.js 目录不存在：" + nodeDir + "，将退回系统 PATH。");
                else if (nodeDir.Length > 0 && ni.PmExe != null && !ni.PmExe.StartsWith(nodeDir, StringComparison.OrdinalIgnoreCase))
                    logF.Warn("⚠ 设置里的 Node.js 目录下没有 " + ni.Pm + ".cmd，实际仍用：" + ni.PmExe);
            }
            RefreshPortF();
        }

        void RefreshPortF()
        {
            if (!ni.Ok) { SetStateF(); return; }
            ni.Port = Node.GuessPort(ni.Dir);
            actualF = 0;
            extKeyF = "";
            WatchNowF();
            SetStateF();
        }

        string CurScriptF()
        {
            int i = cbScript.SelectedIndex;
            if (i < 0 || i >= ni.ScriptNames.Count) return null;
            return ni.ScriptNames[i];
        }

        int CurPortF()
        {
            return actualF > 0 ? actualF : (ni.Ok ? ni.Port : 0);
        }

        // ---------- 启动 ----------
        void RunB()
        {
            if (!bi.Ok) { MessageBox.Show(this, "请先配置有效的后端工程目录。", "提示"); return; }
            if (runB.Running) return;
            if (packRunB.Running) { MessageBox.Show(this, "正在打包后端，请等待完成。", "提示"); return; }
            ApplyChain();

            string pf = CurProfileB();
            bool doInstall = false;
            if (bi.Multi && bi.NeedInstall)
            {
                if (autoInstall) doInstall = true;
                else
                {
                    DialogResult r = MessageBox.Show(this,
                        "依赖模块尚未安装或已过期，跳过安装很可能启动失败。仍要继续吗？",
                        "确认", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                    if (r != DialogResult.Yes) return;
                }
            }

            Save();
            if (killPort) KillOwners(CurPortB(), logB, true);

            string cmd = Boot.BuildCommand(bi, pf, doInstall, false);
            logB.Banner("启动后端");
            logB.Info("profile   : " + (pf.Length > 0 ? pf : "（默认）"));
            logB.Info("工作目录  : " + bi.RootDir);
            logB.Cmd("$ " + cmd);
            logB.Line("", Th.Fg);

            Dictionary<string, string> env = new Dictionary<string, string>();
            env["MAVEN_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8";
            ChainEnvB(env);
            try { runB.Start(bi.RootDir, cmd, env); }
            catch (Exception ex) { logB.Err("启动失败：" + ex.Message); return; }

            SetStateB();
            if (chkOpen.Checked) DelayOpen(CurPortB, 12000);
        }

        void RunF()
        {
            if (!ni.Ok) { MessageBox.Show(this, "请先配置有效的前端工程目录。", "提示"); return; }
            if (runF.Running) return;
            if (packRunF.Running) { MessageBox.Show(this, "正在打包前端，请等待完成。", "提示"); return; }
            string script = CurScriptF();
            if (string.IsNullOrEmpty(script)) { MessageBox.Show(this, "请选择要执行的脚本。", "提示"); return; }

            Save();
            if (killPort) KillOwners(CurPortF(), logF, false);

            bool needInstall = !ni.HasModules;
            if (needInstall && !autoInstall)
            {
                DialogResult r = MessageBox.Show(this,
                    "未发现 node_modules，依赖没有安装，直接启动通常会失败。仍要继续吗？",
                    "确认", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (r != DialogResult.Yes) return;
            }
            List<string> parts = new List<string>();
            if (needInstall && autoInstall) parts.Add(Node.InstallCmd(ni));
            parts.Add(Node.RunCmd(ni, script));
            string cmd = string.Join(" && ", parts.ToArray());

            logF.Banner("启动前端");
            logF.Info("脚本      : " + script);
            logF.Info("工作目录  : " + ni.Dir);
            if (nodeDir.Length > 0 && Directory.Exists(nodeDir)) logF.Info("Node.js   : " + nodeDir + "（已前置到 PATH）");
            logF.Cmd("$ " + cmd);
            logF.Line("", Th.Fg);

            try { runF.Start(ni.Dir, cmd, NodeEnv()); }
            catch (Exception ex) { logF.Err("启动失败：" + ex.Message); return; }

            SetStateF();
            if (chkOpen.Checked) DelayOpen(CurPortF, 4000);
        }

        // ---------- 打包 ----------
        void PackB()
        {
            if (!bi.Ok) { MessageBox.Show(this, "请先配置有效的后端工程目录。", "提示"); return; }
            if (packRunB.Running) return;
            if (runB.Running) { MessageBox.Show(this, "后端正在运行，运行中的 jar 无法被覆盖，请先停止再打包。", "提示"); return; }
            ApplyChain();
            string cmd = Boot.BuildPackageCommand(bi, CurProfileB());
            logB.Banner("打包后端");
            logB.Info("产物目录  : " + Boot.PackageDir(bi));
            logB.Cmd("$ " + cmd);
            logB.Line("", Th.Fg);
            Dictionary<string, string> env = new Dictionary<string, string>();
            env["MAVEN_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8";
            ChainEnvB(env);
            try { packRunB.Start(bi.RootDir, cmd, env); }
            catch (Exception ex) { logB.Err("打包启动失败：" + ex.Message); return; }
            bPackB.Enabled = false;
        }

        void PackF()
        {
            if (!ni.Ok) { MessageBox.Show(this, "请先配置有效的前端工程目录。", "提示"); return; }
            if (packRunF.Running) return;
            string script = Node.BuildScript(ni);
            if (script == null) { MessageBox.Show(this, "package.json 里没有找到 build 类脚本。", "提示"); return; }
            string cmd = Node.RunCmd(ni, script);
            logF.Banner("打包前端");
            logF.Info("脚本      : " + script);
            logF.Info("产物目录  : " + Path.Combine(ni.Dir, Node.GuessOutDir(ni.Dir)));
            logF.Cmd("$ " + cmd);
            logF.Line("", Th.Fg);
            try { packRunF.Start(ni.Dir, cmd, NodeEnv()); }
            catch (Exception ex) { logF.Err("打包启动失败：" + ex.Message); return; }
            bPackF.Enabled = false;
        }

        void OpenPackDirB()
        {
            if (!bi.Ok) { MessageBox.Show(this, "请先配置有效的后端工程目录。", "提示"); return; }
            string d = Boot.PackageDir(bi);
            if (!Directory.Exists(d)) { MessageBox.Show(this, "产物目录还不存在，请先打包：\n" + d, "提示"); return; }
            Sys.OpenFolder(d);
        }

        void OpenPackDirF()
        {
            if (!ni.Ok) { MessageBox.Show(this, "请先配置有效的前端工程目录。", "提示"); return; }
            string d = Path.Combine(ni.Dir, Node.GuessOutDir(ni.Dir));
            if (!Directory.Exists(d)) { MessageBox.Show(this, "产物目录还不存在，请先打包：\n" + d, "提示"); return; }
            Sys.OpenFolder(d);
        }

        static Color PackLineColorB(string line)
        {
            string t = line.TrimStart();
            if (t.StartsWith("ERROR") || t.Contains("BUILD FAILURE")) return Th.Err;
            if (t.Contains("BUILD SUCCESS")) return Th.Ok;
            if (t.StartsWith("[INFO] ---") || t.StartsWith("Downloading") || t.StartsWith("Downloaded")) return Th.Dim;
            return Th.LogFg;
        }

        static Color PackLineColorF(string line)
        {
            string t = line.TrimStart();
            if (t.StartsWith("error") || t.Contains("ERR!") || t.Contains("error during build")) return Th.Err;
            if (t.Contains("built in ")) return Th.Ok;
            return Th.LogFg;
        }

        void OnPackOutB(string line) { logB.Line(line, PackLineColorB(line)); }

        void OnPackOutF(string line) { logF.Line(line, PackLineColorF(line)); }

        void OnPackExitB(int code)
        {
            if (logB.Box.IsDisposed) return;
            try
            {
                logB.Box.BeginInvoke(new Action(delegate
                {
                    bPackB.Enabled = true;
                    if (code == 0) logB.Ok(">>> 打包完成，产物目录：" + Boot.PackageDir(bi));
                    else logB.Err(">>> 打包失败（exit " + code + "）");
                }));
            }
            catch (Exception) { }
        }

        void OnPackExitF(int code)
        {
            if (logF.Box.IsDisposed) return;
            try
            {
                logF.Box.BeginInvoke(new Action(delegate
                {
                    bPackF.Enabled = true;
                    if (code == 0) logF.Ok(">>> 打包完成，产物目录：" + Path.Combine(ni.Dir, Node.GuessOutDir(ni.Dir)));
                    else logF.Err(">>> 打包失败（exit " + code + "）");
                }));
            }
            catch (Exception) { }
        }

        // JDK / Maven 目录注入子进程环境（JAVA_HOME 决定 mvn 用哪个 JDK；PATH 前置保证优先命中）
        void ChainEnvB(Dictionary<string, string> env)
        {
            string pre = "";
            if (jdk.Length > 0 && Directory.Exists(jdk))
            {
                env["JAVA_HOME"] = jdk;
                pre += jdk + "\\bin;";
            }
            if (mavenDir.Length > 0 && Directory.Exists(mavenDir)) pre += mavenDir + "\\bin;";
            if (pre.Length > 0)
            {
                string old = Environment.GetEnvironmentVariable("PATH");
                env["PATH"] = string.IsNullOrEmpty(old) ? pre : pre + old;
            }
        }

        Dictionary<string, string> NodeEnv()
        {
            if (nodeDir.Length == 0 || !Directory.Exists(nodeDir)) return null;
            Dictionary<string, string> env = new Dictionary<string, string>();
            string old = Environment.GetEnvironmentVariable("PATH");
            env["PATH"] = nodeDir + ";" + (old == null ? "" : old);
            return env;
        }

        void DelayOpen(Func<int> port, int ms)
        {
            System.Threading.Timer t = null;
            t = new System.Threading.Timer(delegate(object st)
            {
                try
                {
                    System.Threading.Thread.Sleep(ms);
                    if (IsDisposed) return;
                    int p = port();
                    if (p > 0) Sys.OpenUrl("http://localhost:" + p);
                }
                catch (Exception) { }
                try { t.Dispose(); } catch (Exception) { }
            }, null, ms, System.Threading.Timeout.Infinite);
        }

        void KillOwners(int port, LogView log, bool backend)
        {
            if (port <= 0) return;
            List<Sys.PortOwner> os = Sys.OwnersOnPort(port);
            if (os.Count == 0) return;
            log.Warn("端口 " + port + " 被占用：" + JoinOwners(os) + "，正在结束...");
            foreach (Sys.PortOwner o in os) Sys.KillPid(o.Pid);
            System.Threading.Thread.Sleep(600);
            if (backend) { extB = null; extKeyB = ""; }
            else { extF = null; extKeyF = ""; }
        }

        // ---------- 停止 ----------
        void StopB()
        {
            if (runB.Running)
            {
                logB.Warn("正在停止后端进程（含子进程 java）...");
                runB.Stop();
                SetStateB();
                WatchNowB();
                return;
            }
            Sys.PortOwner o = extB;
            int port = extPortB;
            if (o == null)
            {
                int p = CurPortB();
                if (p > 0)
                {
                    List<Sys.PortOwner> now = Sys.OwnersOnPort(p);
                    if (now.Count > 0) { o = now[0]; port = p; }
                }
            }
            if (o == null) { logB.Dim2("后端未在运行。"); return; }

            bool mine = bi.Ok && Sys.InDir(o.Cmd, bi.RootDir);
            string who = Sys.Describe(o) + (port > 0 ? "，端口 " + port : "");
            string msg = "本窗口没有启动过后端，但检测到" + (mine ? "本工程的" : "其它") + "进程正在监听：\n\n"
                + who + (o.Cmd.Length > 0 ? "\n" + Sys.Shorten(o.Cmd, 220) : "")
                + "\n\n它可能是在 IDEA 里启动的，也可能是上次没有停止。\n确定要结束它吗？";
            if (MessageBox.Show(this, msg, "结束进程", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes)
                return;
            logB.Warn("正在结束 " + who + " ...");
            Sys.KillPid(o.Pid);
            System.Threading.Thread.Sleep(500);
            extB = null; extKeyB = "";
            SetStateB();
            WatchNowB();
        }

        void StopF()
        {
            if (runF.Running)
            {
                logF.Warn("正在停止前端进程（含子进程 node）...");
                runF.Stop();
                SetStateF();
                WatchNowF();
                return;
            }
            Sys.PortOwner o = extF;
            int port = extPortF;
            if (o == null)
            {
                int p = CurPortF();
                if (p > 0)
                {
                    List<Sys.PortOwner> now = Sys.OwnersOnPort(p);
                    if (now.Count > 0) { o = now[0]; port = p; }
                }
            }
            if (o == null) { logF.Dim2("前端未在运行。"); return; }

            bool mine = ni.Ok && Sys.InDir(o.Cmd, ni.Dir);
            string who = Sys.Describe(o) + (port > 0 ? "，端口 " + port : "");
            string msg = "本窗口没有启动过前端，但检测到" + (mine ? "本工程的" : "其它") + "进程正在监听：\n\n"
                + who + (o.Cmd.Length > 0 ? "\n" + Sys.Shorten(o.Cmd, 220) : "")
                + "\n\n它可能是在 WebStorm / IDEA 里启动的，也可能是上次没有停止。\n确定要结束它吗？";
            if (MessageBox.Show(this, msg, "结束进程", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes)
                return;
            logF.Warn("正在结束 " + who + " ...");
            Sys.KillPid(o.Pid);
            System.Threading.Thread.Sleep(500);
            extF = null; extKeyF = "";
            SetStateF();
            WatchNowF();
        }

        // ---------- 日志 ----------
        static Color BLineColor(string line)
        {
            string t = line.TrimStart();
            if (t.StartsWith("ERROR") || t.Contains("BUILD FAILURE") || t.Contains("APPLICATION FAILED TO START")) return Th.Err;
            if (t.StartsWith("WARN") || t.Contains("BUILD SUCCESS")) return Th.Warn;
            if (t.StartsWith("[INFO] ---") || t.StartsWith("Downloading") || t.StartsWith("Downloaded")) return Th.Dim;
            if (t.Contains("Tomcat started on port") || (t.Contains("Started ") && t.Contains(" in "))) return Th.Ok;
            return Th.LogFg;
        }

        static Color FLineColor(string line)
        {
            string t = line.TrimStart();
            if (t.StartsWith("error") || t.Contains("ERR!") || t.Contains("failed to load config")) return Th.Err;
            if (t.Contains("ready in") || t.Contains("Local:") || t.Contains("localhost:")) return Th.Ok;
            if (t.StartsWith("warn") || t.Contains("WARN")) return Th.Warn;
            return Th.LogFg;
        }

        void OnOutB(string line)
        {
            CatchPortB(line);
            logB.Line(line, BLineColor(line));
        }

        void OnOutF(string line)
        {
            CatchPortF(line);
            logF.Line(line, FLineColor(line));
        }

        // 从启动日志校正实际端口（Spring Boot 2 是 port(s):；vite 是 Local: URL）
        void CatchPortB(string line)
        {
            if (actualB > 0) return;
            Match m = Regex.Match(line, @"port\(s\):\s*(\d+)");
            if (!m.Success) m = Regex.Match(line, @"[Tt]omcat started on port\s+(\d+)");
            if (!m.Success) return;
            int p;
            if (!int.TryParse(m.Groups[1].Value, out p) || p <= 0 || p > 65535) return;
            actualB = p;
            try
            {
                if (!logB.Box.IsDisposed)
                    logB.Box.BeginInvoke(new Action(delegate
                    {
                        logB.Ok("实际端口 " + p + "（从启动日志校正）。");
                        WatchNowB();
                        SetStateB();
                    }));
            }
            catch (Exception) { }
        }

        void CatchPortF(string line)
        {
            if (actualF > 0) return;
            Match m = Regex.Match(line, @"http://localhost:(\d+)");
            if (!m.Success) return;
            int p;
            if (!int.TryParse(m.Groups[1].Value, out p) || p <= 0 || p > 65535) return;
            actualF = p;
            try
            {
                if (!logF.Box.IsDisposed)
                    logF.Box.BeginInvoke(new Action(delegate
                    {
                        logF.Ok("实际端口 " + p + "（从启动日志校正）。");
                        WatchNowF();
                        SetStateF();
                    }));
            }
            catch (Exception) { }
        }

        void OnExitB(int code)
        {
            if (logB.Box.IsDisposed) return;
            try
            {
                logB.Box.BeginInvoke(new Action(delegate
                {
                    if (code == 0) logB.Ok(">>> 后端进程已正常退出（exit 0）");
                    else logB.Err(">>> 后端进程已退出（exit " + code + "）");
                    extKeyB = "";
                    SetStateB();
                    WatchNowB();
                }));
            }
            catch (Exception) { }
        }

        void OnExitF(int code)
        {
            if (logF.Box.IsDisposed) return;
            try
            {
                logF.Box.BeginInvoke(new Action(delegate
                {
                    if (code == 0) logF.Ok(">>> 前端进程已正常退出（exit 0）");
                    else logF.Err(">>> 前端进程已退出（exit " + code + "）");
                    extKeyF = "";
                    SetStateF();
                    WatchNowF();
                }));
            }
            catch (Exception) { }
        }

        // ---------- 「已在运行」探测（含 IDEA / 上次遗留的进程） ----------
        void WatchNowB()
        {
            int p = CurPortB();
            if (p <= 0) { extB = null; extPortB = 0; return; }
            watchB.Refresh(p);
        }

        void WatchNowF()
        {
            int p = CurPortF();
            if (p <= 0) { extF = null; extPortF = 0; return; }
            watchF.Refresh(p);
        }

        void OnProbeB(List<Sys.PortOwner> owners, int port)
        {
            if (logB.Box.IsDisposed) return;
            try
            {
                logB.Box.BeginInvoke(new Action(delegate
                {
                    if (port != CurPortB()) return;
                    extPortB = port;
                    extB = owners.Count > 0 ? owners[0] : null;
                    string key = extB == null ? "" : ("pid:" + extB.Pid);
                    if (key != extKeyB)
                    {
                        if (extB != null && !runB.Running)
                        {
                            bool mine = bi.Ok && Sys.InDir(extB.Cmd, bi.RootDir);
                            logB.Warn("检测到端口 " + port + " 上已有后端服务：" + Sys.Describe(extB)
                                + (mine ? "（本工程）" : "（非本工程）") + "，可点「■」结束它。");
                            if (extB.Cmd.Length > 0) logB.Dim2("    " + Sys.Shorten(extB.Cmd, 150));
                        }
                        else if (extB == null && extKeyB.Length > 0 && !runB.Running)
                        {
                            logB.Dim2("端口 " + port + " 已不再被监听。");
                        }
                        extKeyB = key;
                    }
                    SetStateB();
                }));
            }
            catch (Exception) { }
        }

        void OnProbeF(List<Sys.PortOwner> owners, int port)
        {
            if (logF.Box.IsDisposed) return;
            try
            {
                logF.Box.BeginInvoke(new Action(delegate
                {
                    if (port != CurPortF()) return;
                    extPortF = port;
                    extF = owners.Count > 0 ? owners[0] : null;
                    string key = extF == null ? "" : ("pid:" + extF.Pid);
                    if (key != extKeyF)
                    {
                        if (extF != null && !runF.Running)
                        {
                            bool mine = ni.Ok && Sys.InDir(extF.Cmd, ni.Dir);
                            logF.Warn("检测到端口 " + port + " 上已有前端服务：" + Sys.Describe(extF)
                                + (mine ? "（本工程）" : "（非本工程）") + "，可点「■」结束它。");
                            if (extF.Cmd.Length > 0) logF.Dim2("    " + Sys.Shorten(extF.Cmd, 150));
                        }
                        else if (extF == null && extKeyF.Length > 0 && !runF.Running)
                        {
                            logF.Dim2("端口 " + port + " 已不再被监听。");
                        }
                        extKeyF = key;
                    }
                    SetStateF();
                }));
            }
            catch (Exception) { }
        }

        // ---------- 状态 ----------
        static string JoinOwners(List<Sys.PortOwner> os)
        {
            List<string> s = new List<string>();
            foreach (Sys.PortOwner o in os) s.Add(Sys.Describe(o));
            return string.Join(", ", s.ToArray());
        }

        void SetStateB()
        {
            string s;
            Color c;
            if (txtBDir.Text.Trim().Length == 0) { s = "未配置"; c = Th.Dim; }
            else if (!bi.Ok) { s = "✗ " + bi.Error; c = Th.Err; }
            else
            {
                string pf = CurProfileB();
                string port = bi.Port > 0 ? (bi.Port + (pf.Length > 0 ? "(" + pf + ")" : "")) : "默认端口";
                if (runB.Running) { s = "● " + port + " · 运行中"; c = Th.Ok; }
                else if (extB != null)
                {
                    bool mine = Sys.InDir(extB.Cmd, bi.RootDir);
                    s = "● " + port + " · 运行中 " + Sys.Describe(extB) + (mine ? " · 外部" : " · 非本工程");
                    c = Th.Warn;
                }
                else { s = "○ " + port + " · 未运行"; c = Th.Dim; }
            }
            lblBState.Text = s;
            lblBState.ForeColor = c;
            tipB.SetToolTip(lblBState, s);
            lblBLogHead.Text = "后端日志" + (bi.Ok ? "  ·  " + bi.Summary : "");
            bBRun.Enabled = bi.Ok && !runB.Running;
            bBStop.Enabled = runB.Running || extB != null;
            bAllStop.Enabled = bBStop.Enabled || bFStop.Enabled;
        }

        void SetStateF()
        {
            string s;
            Color c;
            if (txtFDir.Text.Trim().Length == 0) { s = "未配置"; c = Th.Dim; }
            else if (!ni.Ok) { s = "✗ " + ni.Error; c = Th.Err; }
            else
            {
                string port = ni.Port > 0 ? ni.Port.ToString() : "默认端口";
                if (runF.Running) { s = "● " + port + " · 运行中"; c = Th.Ok; }
                else if (extF != null)
                {
                    bool mine = Sys.InDir(extF.Cmd, ni.Dir);
                    s = "● " + port + " · 运行中 " + Sys.Describe(extF) + (mine ? " · 外部" : " · 非本工程");
                    c = Th.Warn;
                }
                else { s = "○ " + port + " · 未运行"; c = Th.Dim; }
            }
            lblFState.Text = s;
            lblFState.ForeColor = c;
            tipF.SetToolTip(lblFState, s);
            lblFLogHead.Text = "前端日志" + (ni.Ok ? "  ·  " + ni.Name : "");
            bFRun.Enabled = ni.Ok && !runF.Running;
            bFStop.Enabled = runF.Running || extF != null;
            bAllStop.Enabled = bBStop.Enabled || bFStop.Enabled;
        }

        // ---------- 关闭 ----------
        void OnClosingAll(object sender, FormClosingEventArgs e)
        {
            bool any = runB.Running || runF.Running;
            if (any)
            {
                DialogResult r = MessageBox.Show(this, "仍有服务在运行，是否停止并退出？", "确认",
                    MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (r != DialogResult.Yes) { e.Cancel = true; return; }
            }
            runB.Stop();
            runF.Stop();
            Save();
        }

        // ---------- 自检模式（--uidump） ----------
        void WriteDump()
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("bDir=" + txtBDir.Text.Trim());
                sb.AppendLine("bOk=" + bi.Ok);
                sb.AppendLine("bPort=" + CurPortB());
                sb.AppendLine("bState=" + lblBState.Text);
                sb.AppendLine("bStopEnabled=" + bBStop.Enabled);
                sb.AppendLine("bRunEnabled=" + bBRun.Enabled);
                sb.AppendLine("bProfile=" + CurProfileB());
                sb.AppendLine("fDir=" + txtFDir.Text.Trim());
                sb.AppendLine("fOk=" + ni.Ok);
                sb.AppendLine("fPort=" + CurPortF());
                sb.AppendLine("fState=" + lblFState.Text);
                sb.AppendLine("fStopEnabled=" + bFStop.Enabled);
                sb.AppendLine("fRunEnabled=" + bFRun.Enabled);
                sb.AppendLine("fScript=" + (CurScriptF() == null ? "" : CurScriptF()));
                sb.AppendLine("bExtPid=" + (extB == null ? 0 : extB.Pid));
                sb.AppendLine("fExtPid=" + (extF == null ? 0 : extF.Pid));
                File.WriteAllText(DumpAfter, sb.ToString(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    // ==================== 入口 ====================
    static class Program0
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (RunnerProbe.TryHandle(args)) return;
            if (args.Length >= 4 && args[0] == "--detect-all")
            {
                // 自检：--detect-all <后端目录> <前端目录> <输出文件>
                ToolChain.Current = new ToolChain();
                StringBuilder sb = new StringBuilder();
                try
                {
                    BootInfo b = Boot.Detect(args[1]);
                    sb.AppendLine("[backend]");
                    sb.AppendLine("ok=" + b.Ok);
                    sb.AppendLine("error=" + b.Error);
                    sb.AppendLine("rootDir=" + b.RootDir);
                    sb.AppendLine("multi=" + b.Multi);
                    sb.AppendLine("launchModule=" + b.LaunchModule);
                    sb.AppendLine("profiles=" + string.Join(",", b.Profiles.ToArray()));
                    sb.AppendLine("port=" + b.Port);
                    sb.AppendLine("needInstall=" + b.NeedInstall);
                    sb.AppendLine("command=" + Boot.BuildCommand(b, b.Profiles.Count > 0 ? b.Profiles[0] : "", b.NeedInstall, false));
                }
                catch (Exception ex) { sb.AppendLine("[backend] EXCEPTION " + ex.Message); }
                try
                {
                    NodeInfo n = Node.Detect(args[2], null, null);
                    sb.AppendLine("[frontend]");
                    sb.AppendLine("ok=" + n.Ok);
                    sb.AppendLine("error=" + n.Error);
                    sb.AppendLine("dir=" + n.Dir);
                    sb.AppendLine("pm=" + n.Pm);
                    sb.AppendLine("scripts=" + string.Join(",", n.ScriptNames.ToArray()));
                    sb.AppendLine("default=" + (n.ScriptNames.Count > n.DefaultIndex ? n.ScriptNames[n.DefaultIndex] : ""));
                    sb.AppendLine("port=" + n.Port);
                    sb.AppendLine("command=" + (n.Ok && n.ScriptNames.Count > 0 ? Node.RunCmd(n, n.ScriptNames[n.DefaultIndex]) : ""));
                }
                catch (Exception ex) { sb.AppendLine("[frontend] EXCEPTION " + ex.Message); }
                File.WriteAllText(args[3], sb.ToString(), new UTF8Encoding(false));
                return;
            }
            if (args.Length >= 4 && args[0] == "--uidump")
            {
                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                ComboForm f = new ComboForm();
                f.PresetBDir = args[1];
                f.PresetFDir = args[2];
                f.DumpAfter = args[3];
                Application.Run(f);
                return;
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new ComboForm());
        }
    }
}
