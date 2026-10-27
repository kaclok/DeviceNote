// NSSM 服务配置器 —— WinForms 单文件工具（复用 dev-launcher 的 Shared.cs）
//
// 用途：把任意 exe 通过 nssm 注册为 Windows 服务并设为「自动启动」（开机自启）。
//
// 使用前提：本机自备 nssm.exe（https://nssm.cc，建议 nssm-64.exe 改名 nssm.exe），
//   放到本程序同目录或在下拉框里手动指定路径。
//
// 注册链路（点「配置服务」后依序执行，任一步失败即中止）：
//   nssm install <服务名> <exe路径>
//   nssm set    <服务名> AppDirectory <exe所在目录>
//   nssm set    <服务名> AppParameters <启动参数>          （参数非空时）
//   nssm set    <服务名> Start SERVICE_AUTO_START
//   nssm start  <服务名>                                   （勾选「立即启动」时）
// install 已存在时不中止：转「更新模式」继续 set 覆盖配置。
//
// 权限：app.manifest 为 requireAdministrator（装服务必须管理员），启动即弹 UAC。
//
// 自检：NssmSvc.exe --selftest <outFile>  → 校验命令行拼装与引号，写 JSON。
//
// 编译：.NET Framework 4.0 csc（C# 5）⇒ 禁 $""、?.、nameof、表达式体成员、out var 等

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using DevLaunch;

namespace NsSvc
{
    static class Program
    {
        [STAThread]
        static int Main(string[] args)
        {
            if (args.Length >= 2 && args[0] == "--selftest")
            {
                return Selftest.Run(args[1]);
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
            return 0;
        }
    }

    // ============================ 命令拼装（自检覆盖） ============================
    static class Cmds
    {
        public static string Q(string s) { return "\"" + s + "\""; }

        /// 注册/更新服务的 nssm 命令序列（依序执行）
        public static List<string> BuildCommands(string nssm, string name, string exe, string args, bool startNow)
        {
            List<string> cmds = new List<string>();
            cmds.Add(Q(nssm) + " install " + Q(name) + " " + Q(exe));
            cmds.Add(Q(nssm) + " set " + Q(name) + " AppDirectory " + Q(Path.GetDirectoryName(exe)));
            string a = args == null ? "" : args.Trim();
            if (a.Length > 0) cmds.Add(Q(nssm) + " set " + Q(name) + " AppParameters " + a);
            cmds.Add(Q(nssm) + " set " + Q(name) + " Start SERVICE_AUTO_START");
            if (startNow) cmds.Add(Q(nssm) + " start " + Q(name));
            return cmds;
        }
    }

    // ============================ 自检 ============================
    static class Selftest
    {
        public static int Run(string outFile)
        {
            List<string> cmds = Cmds.BuildCommands("C:\\n s\\nssm.exe", "MySvc", "C:\\app dir\\my.exe", "-c 2 --flag", true);
            bool ok = cmds.Count == 5
                && cmds[0] == "\"C:\\n s\\nssm.exe\" install \"MySvc\" \"C:\\app dir\\my.exe\""
                && cmds[1].EndsWith("AppDirectory \"C:\\app dir\"")
                && cmds[2].EndsWith("AppParameters -c 2 --flag")
                && cmds[3].EndsWith("Start SERVICE_AUTO_START")
                && cmds[4].EndsWith("start \"MySvc\"");
            // 无参数时不应有 AppParameters 行
            List<string> noArgs = Cmds.BuildCommands("C:\\nssm.exe", "S", "C:\\a.exe", "  ", false);
            ok = ok && noArgs.Count == 3;

            StringBuilder sb = new StringBuilder();
            sb.Append("{\"ok\":").Append(ok ? "true" : "false")
              .Append(",\"count\":").Append(cmds.Count).Append(",\"cmds\":[");
            for (int i = 0; i < cmds.Count; i++)
            {
                if (i > 0) sb.Append(',');
                sb.Append('"').Append(cmds[i].Replace("\\", "\\\\").Replace("\"", "\\\"")).Append('"');
            }
            sb.Append("]}");
            File.WriteAllText(outFile, sb.ToString(), new UTF8Encoding(false));
            return ok ? 0 : 1;
        }
    }

    // ============================ 卡片容器 ============================
    class NsCard : Panel
    {
        public string Title = "";
        const int HeadH = 26;

        public NsCard()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            BackColor = Th.Panel;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.Clear(Th.Panel);
            using (SolidBrush b = new SolidBrush(Th.Cyan))
                g.FillRectangle(b, 0, 0, 3, HeadH);
            TextRenderer.DrawText(g, Title, Th.UiB, new Point(12, 5), Th.Cyan);
            using (Pen p = new Pen(Th.Border))
                g.DrawLine(p, 0, HeadH, Width, HeadH);
        }
    }

    // ============================ 主窗体 ============================
    class MainForm : Form
    {
        TextBox txtNssm;
        TextBox txtExe;
        TextBox txtName;
        TextBox txtArgs;
        CheckBox chkStart;
        LogView log;
        Label lblState;
        FlatBtn btnGo;
        ToolTip tip;

        const int EM_SETCUEBANNER = 0x1501;
        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, string lParam);

        void Cue(TextBox tb, string text)
        {
            SendMessage(tb.Handle, EM_SETCUEBANNER, (IntPtr)1, text);
        }

        public MainForm()
        {
            Text = "NSSM 服务配置器";
            ClientSize = new Size(660, 484);
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Th.Bg;
            Font = Th.Ui;

            tip = new ToolTip();

            // ---- 卡片：配置 ----
            NsCard cC = new NsCard();
            cC.Title = "配置";
            cC.SetBounds(16, 12, 628, 190);
            Controls.Add(cC);

            cC.Controls.Add(Ux.Lbl("nssm 程序", 16, 40, 66));
            txtNssm = new TextBox();
            cC.Controls.Add(Ux.Field(txtNssm, 86, 36, 320, 28));
            FlatBtn bNssm = Ux.Btn("浏览", 414, 36, 64, false);
            bNssm.Click += delegate { PickFile(txtNssm, "选择 nssm.exe"); };
            cC.Controls.Add(bNssm);
            tip.SetToolTip(txtNssm, "nssm.exe 完整路径（https://nssm.cc 下载，64 位系统建议 nssm-64.exe）");

            cC.Controls.Add(Ux.Lbl("服务 exe", 16, 80, 60));
            txtExe = new TextBox();
            cC.Controls.Add(Ux.Field(txtExe, 86, 76, 320, 28));
            FlatBtn bExe = Ux.Btn("浏览", 414, 76, 64, false);
            bExe.Click += delegate
            {
                string old = txtExe.Text;
                PickFile(txtExe, "选择要注册为服务的 exe");
                if (txtExe.Text != old && txtName.Text.Trim().Length == 0)
                    txtName.Text = Path.GetFileNameWithoutExtension(txtExe.Text);
            };
            cC.Controls.Add(bExe);
            tip.SetToolTip(txtExe, "要开机自启的目标程序；服务名留空时自动取 exe 文件名");

            cC.Controls.Add(Ux.Lbl("服务名", 16, 120, 50));
            txtName = new TextBox();
            cC.Controls.Add(Ux.Field(txtName, 86, 116, 120, 28));
            tip.SetToolTip(txtName, "Windows 服务名（仅字母/数字/-/_，已存在则按更新模式覆盖配置）");

            cC.Controls.Add(Ux.Lbl("启动参数", 216, 120, 56));
            txtArgs = new TextBox();
            cC.Controls.Add(Ux.Field(txtArgs, 276, 116, 254, 28));
            tip.SetToolTip(txtArgs, "传给 exe 的命令行参数，可留空；含空格的参数请自带引号");

            chkStart = new CheckBox();
            chkStart.Text = "立即启动";
            chkStart.SetBounds(16, 154, 92, 28);
            chkStart.ForeColor = Th.Fg;
            chkStart.BackColor = Th.Panel;
            chkStart.Font = Th.Ui;
            cC.Controls.Add(chkStart);
            tip.SetToolTip(chkStart, "配置完成后立刻启动服务（不勾选则等到下次开机自启）");

            btnGo = Ux.Btn("配置服务", 112, 154, 110, true);
            btnGo.Click += delegate { DoConfigure(); };
            cC.Controls.Add(btnGo);

            lblState = Ux.Val("", 234, 162, 370, Th.Dim);
            lblState.Font = Th.UiB;
            cC.Controls.Add(lblState);

            // ---- 卡片：执行输出 ----
            NsCard cL = new NsCard();
            cL.Title = "执行输出";
            cL.SetBounds(16, 214, 628, 226);
            Controls.Add(cL);

            FlatBtn bClear = Ux.Btn("清空", 548, 2, 64, false);
            bClear.Height = 22;
            bClear.Click += delegate { log.Clear(); };
            cL.Controls.Add(bClear);

            Panel pl = new Panel();
            pl.SetBounds(16, 36, 596, 174);
            pl.Padding = new Padding(1);
            pl.BackColor = Th.Border;
            log = new LogView();
            log.Box.Dock = DockStyle.Fill;
            pl.Controls.Add(log.Box);
            cL.Controls.Add(pl);

            // ---- 恢复配置 ----
            Dictionary<string, string> cfg = Cfg.Load("nssm-svc");
            if (cfg.ContainsKey("nssm")) txtNssm.Text = cfg["nssm"];
            else txtNssm.Text = FindNssm();   // 默认自动探测
            if (cfg.ContainsKey("exe")) txtExe.Text = cfg["exe"];
            if (cfg.ContainsKey("name")) txtName.Text = cfg["name"];
            if (cfg.ContainsKey("args")) txtArgs.Text = cfg["args"];
            chkStart.Checked = cfg.ContainsKey("start") && cfg["start"] == "1";

            // ---- 水印 ----
            Cue(txtNssm, @"如 D:\nssm\nssm.exe");
            Cue(txtExe, @"如 D:\apps\worker.exe");
            Cue(txtName, "留空 = 取 exe 文件名");
            Cue(txtArgs, "可留空");

            // 权限自检提示（manifest 应保证管理员，防御性检查）
            if (!IsAdmin())
                log.Warn("当前不是管理员权限，配置服务可能失败（请右键以管理员身份运行）");
        }

        static bool IsAdmin()
        {
            using (System.Security.Principal.WindowsIdentity id =
                System.Security.Principal.WindowsIdentity.GetCurrent())
            {
                return new System.Security.Principal.WindowsPrincipal(id)
                    .IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);
            }
        }

        // nssm 默认探测：程序同目录 → tool\nssm-svc\nssm*.exe → tool\nssm\*.exe
        static string FindNssm()
        {
            string[] candidates = new string[]
            {
                Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "nssm.exe"),
                Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "nssm-64.exe"),
                Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "nssm", "nssm.exe"),
                Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "nssm", "nssm-64.exe"),
            };
            foreach (string c in candidates)
                if (File.Exists(c)) return c;
            return "";
        }

        void PickFile(TextBox target, string title)
        {
            using (OpenFileDialog d = new OpenFileDialog())
            {
                d.Title = title;
                d.Filter = "exe 程序 (*.exe)|*.exe|全部文件 (*.*)|*.*";
                if (File.Exists(target.Text)) d.FileName = target.Text;
                if (d.ShowDialog(this) == DialogResult.OK) target.Text = d.FileName;
            }
        }

        // 同步执行一条命令，返回退出码；输出（stdout+stderr）进 outp
        static int RunCmd(string file, string args, out string outp)
        {
            outp = "";
            try
            {
                ProcessStartInfo pi = new ProcessStartInfo();
                pi.FileName = file;
                pi.Arguments = args;
                pi.UseShellExecute = false;
                pi.CreateNoWindow = true;
                pi.RedirectStandardOutput = true;
                pi.RedirectStandardError = true;
                using (Process p = Process.Start(pi))
                {
                    string so = p.StandardOutput.ReadToEnd();
                    string se = p.StandardError.ReadToEnd();
                    p.WaitForExit();
                    outp = (so + " " + se).Trim();
                    return p.ExitCode;
                }
            }
            catch (Exception ex)
            {
                outp = ex.Message;
                return -1;
            }
        }

        void DoConfigure()
        {
            string nssm = txtNssm.Text.Trim();
            string exe = txtExe.Text.Trim();
            string name = txtName.Text.Trim();
            string args = txtArgs.Text.Trim();
            bool startNow = chkStart.Checked;

            if (nssm.Length == 0 || !File.Exists(nssm))
            {
                MessageBox.Show(this, "nssm 程序不存在，请先指定 nssm.exe 的完整路径。", "提示");
                return;
            }
            if (exe.Length == 0 || !File.Exists(exe))
            {
                MessageBox.Show(this, "目标 exe 不存在，请先选择要注册为服务的程序。", "提示");
                return;
            }
            if (name.Length == 0) name = Path.GetFileNameWithoutExtension(exe);
            if (!Regex.IsMatch(name, @"^[A-Za-z0-9_\-\.]+$"))
            {
                MessageBox.Show(this, "服务名只能包含字母、数字、-、_、. 。", "提示");
                return;
            }

            Dictionary<string, string> cfg = new Dictionary<string, string>();
            cfg["nssm"] = nssm;
            cfg["exe"] = exe;
            cfg["name"] = name;
            cfg["args"] = args;
            cfg["start"] = startNow ? "1" : "0";
            Cfg.Save("nssm-svc", cfg);

            btnGo.Enabled = false;
            lblState.Text = "配置中…";
            lblState.ForeColor = Th.Warn;
            log.Append(DateTime.Now.ToString("HH:mm:ss") + " 开始配置服务 " + name + "\n", Th.Cyan);

            ThreadPool.QueueUserWorkItem(delegate
            {
                bool ok = true;
                try
                {
                    string outp;
                    int rc = RunCmd(nssm, "install " + Cmds.Q(name) + " " + Cmds.Q(exe), out outp);
                    LogCmd("install");
                    if (rc == 0)
                    {
                        LogOk("install 成功");
                    }
                    else
                    {
                        LogWarn("install 返回 " + rc + "（服务可能已存在，转更新模式）"
                            + (outp.Length > 0 ? "：" + outp : ""));
                    }

                    if (Set(nssm, name, "AppDirectory " + Cmds.Q(Path.GetDirectoryName(exe))) == false) ok = false;
                    if (ok && args.Length > 0 && Set(nssm, name, "AppParameters " + args) == false) ok = false;
                    if (ok && Set(nssm, name, "Start SERVICE_AUTO_START") == false) ok = false;

                    if (ok && startNow)
                    {
                        LogCmd("start");
                        rc = RunCmd(nssm, "start " + Cmds.Q(name), out outp);
                        if (rc == 0) LogOk("服务已启动");
                        else { LogErr("start 返回 " + rc + "：" + outp); ok = false; }
                    }
                }
                catch (Exception ex)
                {
                    LogErr("异常：" + ex.Message);
                    ok = false;
                }

                BeginInvoke((MethodInvoker)delegate
                {
                    btnGo.Enabled = true;
                    lblState.Text = ok ? "配置完成 ✓ 服务开机自启" + (startNow ? "，且已启动" : "")
                                       : "配置失败，详见执行输出";
                    lblState.ForeColor = ok ? Th.Ok : Th.Err;
                    log.Append(DateTime.Now.ToString("HH:mm:ss") + (ok ? " ---- 完成 ----" : " ---- 失败 ----") + "\n",
                        ok ? Th.Ok : Th.Err);
                });
            });
        }

        // nssm set 一条属性；失败时记日志返回 false
        bool Set(string nssm, string name, string pair)
        {
            string outp;
            string head = pair.Split(' ')[0];
            LogCmd("set " + head);
            int rc = RunCmd(nssm, "set " + Cmds.Q(name) + " " + pair, out outp);
            if (rc == 0)
            {
                LogOk("set " + head + " 成功");
                return true;
            }
            LogErr("set " + head + " 返回 " + rc + "：" + outp);
            return false;
        }

        void LogCmd(string s) { AppendLog("> " + s, Th.Cyan); }
        void LogOk(string s) { AppendLog(s, Th.Ok); }
        void LogWarn(string s) { AppendLog(s, Th.Warn); }
        void LogErr(string s) { AppendLog(s, Th.Err); }

        void AppendLog(string s, Color c)
        {
            string t = DateTime.Now.ToString("HH:mm:ss") + " " + s;
            BeginInvoke((MethodInvoker)delegate { log.Append(t + "\n", c); });
        }
    }
}
