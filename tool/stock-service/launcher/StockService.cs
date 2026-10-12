// 股票行情本地服务 启动器：一个 exe 集合「启动 / 结束」
//
// 交互范式与 DevLauncher 的「启动前端.exe」一致：工具链可浏览、可配置、下次自动带出。
// 配置存在 %APPDATA%\DevLauncher\stock.ini（复用 Shared.cs 的 Cfg，与其它启动器同一份实现）。
//
// 可配置项（每一项留空 = 括号里的默认值）：
//   Python 解释器  (服务目录\.venv\Scripts\python.exe)   ← 主窗口内联一行，可浏览
//   服务目录       (exe 所在目录)                        ← 工作目录
//   端口           (7300)
//   监听地址       (0.0.0.0)
//   启动模块       (main:app)                            ← uvicorn 的 ASGI 目标
//   依赖清单       (服务目录\requirements.txt)
//   Ashare lib 目录(服务目录\lib)                        ← 注入 STOCK_LIB_DIR
//   缓存 TTL 倍率  (1；0 = 关闭缓存)                      ← 注入 STOCK_CACHE_TTL_SCALE
// 行为开关：启动前自动安装依赖（pip install -r）/ 启动前结束占用端口的进程
//
// exe 放在 tool/stock-service/ 根目录（与 start.bat、.venv 同级），双击即切换：
//   ● 未运行          → 「▶ 启动服务」：以配置的解释器起 uvicorn
//   ● 本窗口已启动    → 「■ 结束服务」：taskkill 杀整棵进程树（cmd -> python）
//   ● start.bat / 上个会话遗留 → 自动检测端口占用，确认后结束外部进程
//
// 自检：
//   StockService.exe --probe  <输出文件>  → 打开窗体 2.5 秒后写出状态并退出
//   StockService.exe --detect <输出文件>  → 不开窗，只写出「解析后的配置 + 将执行的命令」
//
// 编译：launcher\build.py（复用 ..\dev-launcher\src\Shared.cs 的 Th/Ux/Sys/ProcRunner/PortWatcher/Cfg）
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace DevLaunch
{
    // ==================== 可配置项（唯一口径，可持久化） ====================
    class StockCfg
    {
        public const string DefPort = "7300";
        public const string DefHost = "0.0.0.0";
        public const string DefModule = "main:app";
        public const string DefTtl = "1";

        public string Python = "";       // 解释器；留空 = 服务目录\.venv\Scripts\python.exe
        public string Dir = "";          // 服务目录（工作目录）；留空 = exe 所在目录
        public string Port = "";         // 留空 = 7300
        public string Host = "";         // 留空 = 0.0.0.0
        public string Module = "";       // 留空 = main:app
        public string Req = "";          // 依赖清单；留空 = 服务目录\requirements.txt
        public string Lib = "";          // Ashare lib 目录；留空 = 服务目录\lib
        public string Ttl = "";          // 缓存 TTL 倍率；留空 = 1
        public bool AutoInstall;         // 启动前 pip install -r
        public bool KillPort;            // 启动前结束占用端口的进程

        public static StockCfg Load()
        {
            Dictionary<string, string> c = Cfg.Load("stock");
            StockCfg s = new StockCfg();
            s.Python = Get(c, "python");
            s.Dir = Get(c, "dir");
            s.Port = Get(c, "port");
            s.Host = Get(c, "host");
            s.Module = Get(c, "module");
            s.Req = Get(c, "req");
            s.Lib = Get(c, "lib");
            s.Ttl = Get(c, "ttl");
            s.AutoInstall = Get(c, "autoInstall") == "1";
            s.KillPort = Get(c, "killPort") == "1";
            return s;
        }

        public void Save()
        {
            Dictionary<string, string> c = new Dictionary<string, string>();
            c["python"] = Python.Trim();
            c["dir"] = Dir.Trim();
            c["port"] = Port.Trim();
            c["host"] = Host.Trim();
            c["module"] = Module.Trim();
            c["req"] = Req.Trim();
            c["lib"] = Lib.Trim();
            c["ttl"] = Ttl.Trim();
            c["autoInstall"] = AutoInstall ? "1" : "0";
            c["killPort"] = KillPort ? "1" : "0";
            Cfg.Save("stock", c);
        }

        public StockCfg Clone()
        {
            StockCfg s = new StockCfg();
            s.Python = Python; s.Dir = Dir; s.Port = Port; s.Host = Host;
            s.Module = Module; s.Req = Req; s.Lib = Lib; s.Ttl = Ttl;
            s.AutoInstall = AutoInstall; s.KillPort = KillPort;
            return s;
        }

        static string Get(Dictionary<string, string> c, string k)
        {
            string v;
            if (c.TryGetValue(k, out v)) return v == null ? "" : v.Trim();
            return "";
        }
    }

    // ==================== 解析结果（配置 → 实际路径 / 命令 / 环境变量） ====================
    class StockResolved
    {
        public string Dir = "", Python = "", Host = "", Module = "", Req = "", Lib = "";
        public int Port;
        public double Ttl = 1.0;
        public bool AutoInstall, KillPort;
        public string Cmd = "";
        public List<string> Notes = new List<string>();
        public List<string> Errors = new List<string>();
        public bool Ok { get { return Errors.Count == 0; } }
    }

    static class StockResolve
    {
        public static string ExeDir()
        {
            // winexe 下 AppBase 即 exe 所在目录；去掉尾分隔符，界面与 dump 里更干净
            string d = AppDomain.CurrentDomain.BaseDirectory;
            if (d.Length > 3) d = d.TrimEnd('\\', '/');
            return d;
        }

        public static string Num(double v)
        {
            return v.ToString(CultureInfo.InvariantCulture);
        }

        public static StockResolved Of(StockCfg c)
        {
            StockResolved r = new StockResolved();
            r.AutoInstall = c.AutoInstall;
            r.KillPort = c.KillPort;

            // 1) 服务目录（工作目录）
            r.Dir = c.Dir.Length > 0 ? c.Dir : ExeDir();
            if (!Directory.Exists(r.Dir)) r.Errors.Add("服务目录不存在：" + r.Dir);

            // 2) Python 解释器：填目录时自动补 python.exe
            string py = c.Python.Length > 0 ? c.Python : Path.Combine(r.Dir, ".venv", "Scripts", "python.exe");
            if (Directory.Exists(py)) py = Path.Combine(py, "python.exe");
            r.Python = py;
            if (!File.Exists(py)) r.Errors.Add("未找到 Python 解释器：" + py);

            // 3) 端口
            string ptext = c.Port.Length > 0 ? c.Port : StockCfg.DefPort;
            int pv;
            if (!int.TryParse(ptext, NumberStyles.Integer, CultureInfo.InvariantCulture, out pv)
                || pv <= 0 || pv > 65535)
                r.Errors.Add("端口非法：" + ptext + "（应为 1-65535 的整数）");
            else r.Port = pv;

            // 4) 监听地址 / 启动模块
            r.Host = c.Host.Length > 0 ? c.Host : StockCfg.DefHost;
            r.Module = c.Module.Length > 0 ? c.Module : StockCfg.DefModule;

            // 5) 依赖清单
            r.Req = c.Req.Length > 0 ? c.Req : Path.Combine(r.Dir, "requirements.txt");
            if (!File.Exists(r.Req))
            {
                if (r.AutoInstall) r.Errors.Add("依赖清单不存在：" + r.Req);
                else r.Notes.Add("依赖清单不存在（未开自动安装，不影响启动）：" + r.Req);
            }

            // 6) Ashare lib 目录
            r.Lib = c.Lib.Length > 0 ? c.Lib : Path.Combine(r.Dir, "lib");
            if (!Directory.Exists(r.Lib)) r.Notes.Add("lib 目录不存在（仅指数分时线会用到）：" + r.Lib);

            // 7) 缓存 TTL 倍率
            string ttext = c.Ttl.Length > 0 ? c.Ttl : StockCfg.DefTtl;
            double tv;
            if (!double.TryParse(ttext, NumberStyles.Float, CultureInfo.InvariantCulture, out tv) || tv < 0)
            {
                r.Notes.Add("缓存 TTL 倍率非法：" + ttext + "（应为 >= 0 的数），已按 1 处理");
                tv = 1.0;
            }
            r.Ttl = tv;

            // 8) 最终命令
            string run = "\"" + r.Python + "\" -m uvicorn " + r.Module
                       + " --host " + r.Host + " --port " + r.Port;
            if (r.AutoInstall)
                run = "\"" + r.Python + "\" -m pip install -r \"" + r.Req + "\" && " + run;
            r.Cmd = run;
            return r;
        }

        // 注入给服务端的环境变量（service.py 读；缺省值与这里一致，单独起服务行为不变）
        public static Dictionary<string, string> Env(StockResolved r)
        {
            Dictionary<string, string> e = new Dictionary<string, string>();
            e["STOCK_LIB_DIR"] = r.Lib;
            e["STOCK_CACHE_TTL_SCALE"] = Num(r.Ttl);
            return e;
        }

        public static string Dump(StockCfg c)
        {
            StockResolved r = Of(c);
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("ok=" + r.Ok);
            sb.AppendLine("dir=" + r.Dir);
            sb.AppendLine("dirCfg=" + c.Dir);
            sb.AppendLine("python=" + r.Python);
            sb.AppendLine("pythonCfg=" + c.Python);
            sb.AppendLine("pythonExists=" + File.Exists(r.Python));
            sb.AppendLine("port=" + r.Port);
            sb.AppendLine("host=" + r.Host);
            sb.AppendLine("module=" + r.Module);
            sb.AppendLine("req=" + r.Req);
            sb.AppendLine("reqExists=" + File.Exists(r.Req));
            sb.AppendLine("lib=" + r.Lib);
            sb.AppendLine("ttl=" + Num(r.Ttl));
            sb.AppendLine("autoInstall=" + r.AutoInstall);
            sb.AppendLine("killPort=" + r.KillPort);
            sb.AppendLine("command=" + r.Cmd);
            sb.AppendLine("env=STOCK_LIB_DIR=" + r.Lib + " ; STOCK_CACHE_TTL_SCALE=" + Num(r.Ttl));
            sb.AppendLine("errors=" + string.Join(" | ", r.Errors.ToArray()));
            sb.AppendLine("notes=" + string.Join(" | ", r.Notes.ToArray()));
            return sb.ToString();
        }
    }

    // ==================== 设置对话框（工具链 + 行为开关） ====================
    class StockSettingsDialog : Form
    {
        public StockCfg Cfg;

        TextBox tbDir, tbPort, tbHost, tbModule, tbReq, tbLib, tbTtl;
        CheckBox chkInstall, chkKill;

        public StockSettingsDialog(StockCfg src)
        {
            Cfg = src.Clone();

            Text = "股票服务设置 · 工具链与行为";
            ClientSize = new Size(480, 358);
            BackColor = Th.Bg;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterParent;

            MkRow("服务目录", 14, tbDir = new TextBox(), true, false,
                  "股票服务的根目录（含 main.py）。留空 = exe 所在目录");
            MkRow("端口", 48, tbPort = new TextBox(), false, false,
                  "留空 = 7300。改端口需同步 ft-checnote/backends.mjs 里 trend.url 的端口");
            MkRow("监听地址", 82, tbHost = new TextBox(), false, false,
                  "留空 = 0.0.0.0（允许局域网访问）；本机自用可填 127.0.0.1");
            MkRow("启动模块", 116, tbModule = new TextBox(), false, false,
                  "uvicorn 的 ASGI 目标。留空 = main:app");
            MkRow("依赖清单", 150, tbReq = new TextBox(), false, true,
                  "pip 安装用。留空 = 服务目录\\requirements.txt");
            MkRow("Ashare lib 目录", 184, tbLib = new TextBox(), true, false,
                  "指数分时线用的单文件库所在目录（注入 STOCK_LIB_DIR）。留空 = 服务目录\\lib");
            MkRow("缓存 TTL 倍率", 218, tbTtl = new TextBox(), false, false,
                  "留空 = 1（保持各接口原 TTL）；0 = 关闭缓存；2 = 全部加倍");

            chkInstall = MkCheck("启动前自动安装依赖（pip install -r 依赖清单）", 16, 252, Cfg.AutoInstall);
            chkKill = MkCheck("启动前结束占用端口的进程", 16, 280, Cfg.KillPort);
            Controls.Add(chkInstall);
            Controls.Add(chkKill);

            FlatBtn ok = Ux.Btn("确定", 250, 316, 90, true);
            ok.Click += delegate { ApplyAndClose(); };
            Controls.Add(ok);

            FlatBtn cancel = Ux.Btn("取消", 350, 316, 90, false);
            cancel.Click += delegate { DialogResult = DialogResult.Cancel; Close(); };
            Controls.Add(cancel);

            tbDir.Text = Cfg.Dir;
            tbPort.Text = Cfg.Port;
            tbHost.Text = Cfg.Host;
            tbModule.Text = Cfg.Module;
            tbReq.Text = Cfg.Req;
            tbLib.Text = Cfg.Lib;
            tbTtl.Text = Cfg.Ttl;
        }

        void MkRow(string label, int y, TextBox tb, bool browseDir, bool browseFile, string tip)
        {
            Label l = Ux.Lbl(label, 16, y + 4, 88);
            Controls.Add(l);
            Controls.Add(Ux.Field(tb, 108, y, 300, 26));
            if (browseDir || browseFile)
            {
                FlatBtn b = Ux.Btn("浏览", 414, y, 44, false);
                b.Height = 26;
                b.Click += delegate
                {
                    if (browseFile)
                    {
                        OpenFileDialog d = new OpenFileDialog();
                        d.Title = "选择 " + label;
                        d.Filter = "全部文件|*.*";
                        string cur = tb.Text.Trim();
                        if (File.Exists(cur))
                        {
                            d.InitialDirectory = Path.GetDirectoryName(cur);
                            d.FileName = Path.GetFileName(cur);
                        }
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
            }
            ToolTip t = new ToolTip();
            t.SetToolTip(l, tip);
            t.SetToolTip(tb, tip);
        }

        CheckBox MkCheck(string text, int x, int y, bool val)
        {
            CheckBox c = new CheckBox();
            c.Text = text;
            c.ForeColor = Th.Fg;
            c.Font = Th.Ui;
            c.SetBounds(x, y, 448, 24);
            c.BackColor = Color.Transparent;
            c.FlatStyle = FlatStyle.Standard;
            c.Checked = val;
            c.Cursor = Cursors.Hand;
            return c;
        }

        void ApplyAndClose()
        {
            Cfg.Dir = tbDir.Text.Trim();
            Cfg.Port = tbPort.Text.Trim();
            Cfg.Host = tbHost.Text.Trim();
            Cfg.Module = tbModule.Text.Trim();
            Cfg.Req = tbReq.Text.Trim();
            Cfg.Lib = tbLib.Text.Trim();
            Cfg.Ttl = tbTtl.Text.Trim();
            Cfg.AutoInstall = chkInstall.Checked;
            Cfg.KillPort = chkKill.Checked;
            DialogResult = DialogResult.OK;
            Close();
        }
    }

    // ==================== 主窗体 ====================
    class StockForm : Form
    {
        StockCfg cfg = StockCfg.Load();
        StockResolved res;

        FlatBtn btnToggle, btnDocs, btnFolder, btnSettings, btnCopy, btnClear;
        TextBox txtPy;
        Label lblDir, lblState, lblPyHint, lblCfg;
        LogView log;
        ProcRunner runner = new ProcRunner();
        PortWatcher portWatch = new PortWatcher();
        Sys.PortOwner extRun;      // 端口上正在监听、但不是本窗口启动的进程
        string extKey = "";
        int tick;

        // 自检模式（--probe <file>）
        public string ProbeOut;

        public StockForm()
        {
            Text = "股票行情服务  本地 Python 数据服务";
            ClientSize = new Size(720, 470);
            MinimumSize = new Size(680, 430);
            Ux.Dark(this);
            BuildUi();

            runner.Out += OnOut;
            runner.Exited += OnProcExit;
            portWatch.Result += OnPortProbe;

            System.Windows.Forms.Timer poll = new System.Windows.Forms.Timer();
            poll.Interval = 1000;
            poll.Tick += delegate
            {
                tick++;
                if (tick % 2 == 1) portWatch.Refresh(CurPort());
                RefreshState();
            };
            poll.Start();

            Load += delegate { OnLoadInit(); };
            Shown += delegate { ApplyProbe(); };
            FormClosing += OnClosing;
        }

        void BuildUi()
        {
            lblDir = Ux.Val("", 20, 20, 680, Th.Dim);
            Controls.Add(lblDir);

            // ---- 工具链：Python 解释器（与「启动前端.exe」的 Node.js 目录同款交互）----
            Controls.Add(Ux.Lbl("Python 解释器", 20, 46, 88));
            txtPy = new TextBox();
            Controls.Add(Ux.Field(txtPy, 108, 42, 500, 28));
            FlatBtn btnPy = Ux.Btn("浏览", 616, 42, 84, false);
            btnPy.Height = 28;
            btnPy.Click += delegate { PickPython(); };
            Controls.Add(btnPy);
            ToolTip tipPy = new ToolTip();
            tipPy.SetToolTip(txtPy, "选 python.exe；也可以直接填 Python 安装目录（自动补 python.exe）。留空 = 服务目录\\.venv\\Scripts\\python.exe");
            txtPy.TextChanged += delegate { OnPythonChanged(); };

            lblPyHint = Ux.Val("", 20, 76, 680, Th.Dim);
            lblPyHint.Font = Th.UiS;
            Controls.Add(lblPyHint);

            btnToggle = Ux.Btn("▶  启动服务", 20, 104, 132, true);
            btnToggle.Height = 38;
            btnToggle.Click += delegate { Toggle(); };
            Controls.Add(btnToggle);

            btnDocs = Ux.Btn("接口文档", 160, 104, 90, false);
            btnDocs.Height = 38;
            btnDocs.Enabled = false;
            btnDocs.Click += delegate { Sys.OpenUrl("http://localhost:" + CurPort() + "/docs"); };
            Controls.Add(btnDocs);

            btnFolder = Ux.Btn("打开目录", 256, 104, 90, false);
            btnFolder.Height = 38;
            btnFolder.Click += delegate { Sys.OpenFolder(res.Dir); };
            Controls.Add(btnFolder);

            btnSettings = Ux.Btn("设置…", 352, 104, 90, false);
            btnSettings.Height = 38;
            btnSettings.Click += delegate { OpenSettings(); };
            Controls.Add(btnSettings);

            lblState = Ux.Val("● 检测中...", 448, 112, 252, Th.Dim);
            lblState.Font = Th.UiB;
            Controls.Add(lblState);

            lblCfg = Ux.Val("", 20, 150, 680, Th.Dim);
            lblCfg.Font = Th.UiS;
            Controls.Add(lblCfg);

            btnCopy = Ux.Btn("复制", 564, 172, 66, false);
            btnCopy.Height = 24;
            btnCopy.Click += delegate
            {
                try { if (log.Text().Length > 0) Clipboard.SetText(log.Text()); }
                catch (Exception) { }
            };
            Controls.Add(btnCopy);

            btnClear = Ux.Btn("清空", 636, 172, 64, false);
            btnClear.Height = 24;
            btnClear.Click += delegate { log.Clear(); };
            Controls.Add(btnClear);

            log = new LogView();
            Panel pl = new Panel();
            pl.SetBounds(20, 200, 680, 250);
            pl.Padding = new Padding(1);
            pl.BackColor = Th.Border;
            pl.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            log.Box.Dock = DockStyle.Fill;
            pl.Controls.Add(log.Box);
            Controls.Add(pl);
            log.Box.BackColor = Th.LogBg;
        }

        static string Shorten(string s, int max)
        {
            if (s.Length <= max) return s;
            return "..." + s.Substring(s.Length - max);
        }

        // ---------- 配置 ----------
        int CurPort()
        {
            return res == null ? 0 : res.Port;
        }

        void Recompute()
        {
            res = StockResolve.Of(cfg);
            RefreshCfgLabels();
        }

        void RefreshCfgLabels()
        {
            lblDir.Text = "目录  " + Shorten(res.Dir, 74);
            lblCfg.Text = "端口 " + res.Port + " · " + res.Host + " · uvicorn " + res.Module
                        + " · 缓存 TTL ×" + StockResolve.Num(res.Ttl)
                        + (res.AutoInstall ? " · 自动装依赖" : "")
                        + (res.KillPort ? " · 启动前清端口" : "");
            Text = "股票行情服务  localhost:" + res.Port;

            bool exists = File.Exists(res.Python);
            if (res.Errors.Count > 0)
            {
                lblPyHint.ForeColor = Th.Err;
                lblPyHint.Text = "✗ " + ResolveHintErrors();
            }
            else if (cfg.Python.Length == 0)
            {
                lblPyHint.ForeColor = Th.Dim;
                lblPyHint.Text = "留空 = 服务目录\\.venv\\Scripts\\python.exe（已找到）";
            }
            else if (Directory.Exists(cfg.Python))
            {
                lblPyHint.ForeColor = exists ? Th.Ok : Th.Err;
                lblPyHint.Text = exists ? "目录 → " + Shorten(res.Python, 92) : "⚠ 该目录下没有 python.exe";
            }
            else
            {
                lblPyHint.ForeColor = exists ? Th.Ok : Th.Err;
                lblPyHint.Text = exists ? "✓ " + Shorten(res.Python, 96) : "⚠ 文件不存在：" + Shorten(res.Python, 90);
            }
        }

        string ResolveHintErrors()
        {
            for (int i = 0; i < res.Errors.Count; i++)
            {
                string e = res.Errors[i];
                if (e.IndexOf("Python 解释器") >= 0) return "未找到 Python 解释器：" + Shorten(res.Python, 88);
            }
            return res.Errors[0];
        }

        void OnPythonChanged()
        {
            cfg.Python = txtPy.Text.Trim();
            Recompute();
        }

        void PickPython()
        {
            string cur = txtPy.Text.Trim();
            if (Directory.Exists(cur))
            {
                PickDir(txtPy, "选择 Python 安装目录（其下应有 python.exe）");
                return;
            }
            OpenFileDialog d = new OpenFileDialog();
            d.Title = "选择 Python 解释器（python.exe）";
            d.Filter = "python.exe|python.exe|可执行文件|*.exe|全部文件|*.*";
            if (File.Exists(cur))
            {
                d.InitialDirectory = Path.GetDirectoryName(cur);
                d.FileName = Path.GetFileName(cur);
            }
            else
            {
                string cand = Path.Combine(res != null ? res.Dir : StockResolve.ExeDir(), ".venv", "Scripts");
                if (Directory.Exists(cand)) d.InitialDirectory = cand;
            }
            if (d.ShowDialog(this) == DialogResult.OK) txtPy.Text = d.FileName;
        }

        void PickDir(TextBox tb, string desc)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = desc;
            if (Directory.Exists(tb.Text.Trim())) d.SelectedPath = tb.Text.Trim();
            if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.SelectedPath;
        }

        void OpenSettings()
        {
            StockSettingsDialog d = new StockSettingsDialog(cfg);
            if (d.ShowDialog(this) != DialogResult.OK) return;
            cfg = d.Cfg;
            cfg.Save();
            Recompute();
            PrintConfig("设置已保存");
        }

        void OnLoadInit()
        {
            txtPy.Text = cfg.Python;
            Recompute();
            log.Info("股票行情本地服务启动器就绪。");
            log.Dim2("可配置：Python 解释器 / 服务目录 / 端口 / 监听地址 / 启动模块 / 依赖清单 / lib 目录 / 缓存 TTL。"
                   + "「设置…」里改，下次自动带出。");
            PrintConfig(null);
            RefreshState();
            portWatch.Refresh(CurPort());
        }

        void PrintConfig(string banner)
        {
            if (banner != null)
            {
                log.Banner(banner);
                log.Info("配置     : %APPDATA%\\DevLauncher\\stock.ini");
            }
            log.Info("服务目录   : " + res.Dir);
            log.Info("解释器     : " + res.Python);
            log.Info("监听       : " + res.Host + ":" + res.Port + "   （uvicorn " + res.Module + "）");
            log.Info("依赖清单   : " + res.Req + (res.AutoInstall ? "   （启动前自动安装）" : ""));
            log.Info("Ashare lib : " + res.Lib);
            log.Info("缓存 TTL   : 倍率 " + StockResolve.Num(res.Ttl)
                     + (res.Ttl <= 0 ? "   （已关闭缓存）" : ""));
            if (res.Port != 7300)
                log.Warn("⚠ 端口已改为 " + res.Port + "：请同步修改 ft-checnote/backends.mjs 里 trend.url 的端口，"
                       + "否则前端代理仍指向 7300。");
            for (int i = 0; i < res.Notes.Count; i++) log.Warn("⚠ " + res.Notes[i]);
            for (int i = 0; i < res.Errors.Count; i++) log.Err("✗ " + res.Errors[i]);
        }

        // ---------- 状态与主按钮 ----------
        bool ServiceUp
        {
            get { return runner.Running || extRun != null; }
        }

        void RefreshState()
        {
            if (runner.Running)
            {
                lblState.ForeColor = Th.Ok;
                lblState.Text = "● 运行中  PID " + runner.Pid;
            }
            else if (extRun != null)
            {
                lblState.ForeColor = Th.Warn;
                lblState.Text = "● 运行中（外部） " + Sys.Describe(extRun);
            }
            else
            {
                lblState.ForeColor = Th.Dim;
                lblState.Text = "● 未运行";
            }
            btnToggle.Text = ServiceUp ? "■  结束服务" : "▶  启动服务";
            btnDocs.Enabled = ServiceUp;
        }

        void Toggle()
        {
            if (runner.Running)
            {
                log.Warn("正在结束本窗口启动的服务（taskkill 进程树）...");
                runner.Stop();
                return;
            }
            if (extRun != null)
            {
                string who = Sys.Describe(extRun);
                string msg = "本窗口没有启动过服务，但端口 " + CurPort() + " 正被以下进程监听\n"
                    + "（可能是 start.bat 启动的，也可能是上次窗口退出时保持后台运行的服务）：\n\n"
                    + who + (extRun.Cmd.Length > 0 ? "\n" + Sys.Shorten(extRun.Cmd, 220) : "")
                    + "\n\n确定要结束它吗？";
                if (MessageBox.Show(this, msg, "结束进程", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes)
                    return;
                log.Warn("正在结束 " + who + " ...");
                Sys.KillPid(extRun.Pid);
                System.Threading.Thread.Sleep(400);
                extRun = null;
                extKey = "";
                RefreshState();
                portWatch.Refresh(CurPort());
                return;
            }
            Start();
        }

        void Start()
        {
            Recompute();
            if (!res.Ok)
            {
                for (int i = 0; i < res.Errors.Count; i++) log.Err("✗ " + res.Errors[i]);
                MessageBox.Show(this,
                    "配置不完整，无法启动：\n\n" + string.Join("\n", res.Errors.ToArray())
                    + "\n\n请在「设置…」里补齐（或先建好虚拟环境）后重试。",
                    "提示", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            if (res.KillPort)
            {
                List<Sys.PortOwner> busy = Sys.OwnersOnPort(res.Port);
                if (busy.Count > 0)
                {
                    log.Warn("端口 " + res.Port + " 被占用：" + JoinOwners(busy) + "，正在结束...");
                    for (int i = 0; i < busy.Count; i++) Sys.KillPid(busy[i].Pid);
                    System.Threading.Thread.Sleep(600);
                    extRun = null; extKey = "";
                }
            }

            List<Sys.PortOwner> os = Sys.OwnersOnPort(res.Port);
            if (os.Count > 0)
            {
                extRun = os[0];
                extKey = "";
                log.Warn("端口 " + res.Port + " 已被占用，未启动。可点「结束服务」后重试，"
                       + "或到「设置…」勾选「启动前结束占用端口的进程」。");
                RefreshState();
                return;
            }

            cfg.Save();
            log.Banner("启动股票行情服务");
            log.Info("目录   : " + res.Dir);
            log.Info("解释器 : " + res.Python);
            log.Info("环境变量: STOCK_LIB_DIR=" + res.Lib + "  STOCK_CACHE_TTL_SCALE=" + StockResolve.Num(res.Ttl));
            log.Cmd("$ " + res.Cmd);
            try { runner.Start(res.Dir, res.Cmd, StockResolve.Env(res)); }
            catch (Exception ex) { log.Err("启动失败：" + ex.Message); return; }
            RefreshState();
        }

        static string JoinOwners(List<Sys.PortOwner> os)
        {
            List<string> s = new List<string>();
            for (int i = 0; i < os.Count; i++) s.Add(Sys.Describe(os[i]));
            return string.Join(", ", s.ToArray());
        }

        // ---------- 日志着色 ----------
        void OnOut(string line)
        {
            Color c = Th.LogFg;
            string t = line.TrimStart();
            if (t.Contains("Uvicorn running on") || t.Contains("Application startup complete")
                || t.Contains("Started server process") || t.StartsWith("INFO:"))
                c = Th.Ok;
            else if (t.Contains("Traceback") || t.Contains("ERROR") || t.StartsWith("CRITICAL:"))
                c = Th.Err;
            else if (t.Contains("WARNING") || t.StartsWith("WARNING:"))
                c = Th.Warn;
            log.Line(line, c);
        }

        void OnProcExit(int code)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    if (code == 0) log.Ok(">>> 服务已停止（exit 0）");
                    else log.Err(">>> 服务进程已退出（exit " + code + "）");
                    extRun = null;
                    extKey = "";
                    RefreshState();
                    portWatch.Refresh(CurPort());
                }));
            }
            catch (Exception) { }
        }

        // ---------- 端口探测（覆盖 start.bat / 后台遗留进程） ----------
        void OnPortProbe(List<Sys.PortOwner> owners, int port)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate { ApplyProbe(owners, port); }));
            }
            catch (Exception) { }
        }

        void ApplyProbe(List<Sys.PortOwner> owners, int port)
        {
            if (port != CurPort()) return;
            extRun = owners.Count > 0 ? owners[0] : null;
            string key = extRun == null ? "" : "pid:" + extRun.Pid;
            if (key != extKey)
            {
                if (extRun != null && !runner.Running)
                {
                    log.Warn("检测到端口 " + port + " 已有服务在监听：" + Sys.Describe(extRun)
                        + "，可直接点「结束服务」。");
                    if (extRun.Cmd.Length > 0) log.Dim2("    " + Sys.Shorten(extRun.Cmd, 150));
                }
                else if (extRun == null && extKey.Length > 0 && !runner.Running)
                {
                    log.Dim2("端口 " + port + " 已释放。");
                }
                extKey = key;
            }
            RefreshState();
        }

        // ---------- 关窗：三选一 ----------
        void OnClosing(object sender, FormClosingEventArgs e)
        {
            if (!runner.Running) return;
            DialogResult r = MessageBox.Show(this,
                "服务仍在运行。选择：\n\n"
                + "「是」= 停止服务并退出\n"
                + "「否」= 保持后台运行并退出（下次打开本工具可直接结束它）\n"
                + "「取消」= 返回",
                "退出确认", MessageBoxButtons.YesNoCancel, MessageBoxIcon.Question);
            if (r == DialogResult.Cancel) { e.Cancel = true; return; }
            if (r == DialogResult.Yes) runner.Stop();
            // 「否」：进程不随窗口退出（未挂作业对象，子进程独立存活），下次打开靠端口探测接管。
        }

        // ---------- 自检（--probe <file>）----------
        void ApplyProbe()
        {
            if (string.IsNullOrEmpty(ProbeOut)) return;
            System.Windows.Forms.Timer t = new System.Windows.Forms.Timer();
            t.Interval = 2500;
            t.Tick += delegate { t.Stop(); WriteProbe(); Close(); };
            t.Start();
        }

        void WriteProbe()
        {
            try
            {
                Recompute();
                StringBuilder sb = new StringBuilder();
                sb.Append(StockResolve.Dump(cfg));
                sb.AppendLine("state=" + lblState.Text);
                sb.AppendLine("toggle=" + btnToggle.Text);
                sb.AppendLine("docsEnabled=" + btnDocs.Enabled);
                sb.AppendLine("title=" + Text);
                sb.AppendLine("cfgLine=" + lblCfg.Text);
                sb.AppendLine("pyHint=" + lblPyHint.Text);
                sb.AppendLine("ini=" + Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "DevLauncher", "stock.ini"));
                File.WriteAllText(ProbeOut, sb.ToString(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    static class StockServiceProgram
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (args.Length >= 2 && args[0] == "--detect")
            {
                // 不开窗：只解析配置并把「将执行的命令」写文件（供自检与排查）
                try
                {
                    File.WriteAllText(args[1], StockResolve.Dump(StockCfg.Load()), new UTF8Encoding(false));
                }
                catch (Exception ex)
                {
                    try
                    {
                        File.WriteAllText(args[1], "ok=False\r\nerrors=" + ex.Message, new UTF8Encoding(false));
                    }
                    catch (Exception) { }
                }
                return;
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            StockForm f = new StockForm();
            if (args.Length >= 2 && args[0] == "--probe") f.ProbeOut = args[1];
            Application.Run(f);
        }
    }
}
