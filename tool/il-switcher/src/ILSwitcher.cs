// 完整性级别切换器 (Integrity Level Switcher) —— WinForms 单文件工具（复用 dev-launcher 的 Shared.cs）
//
// 用途：查看 / 切换文件或文件夹的 NTFS 强制完整性标签（Mandatory Integrity Label）。
//   背景：本机 DeviceNote 工程被沙箱打上 Low 标签，其下编译出的 exe 继承 Low ⇒
//   双击启动后进程运行在低完整性级别 ⇒ 拉起的 explorer 等子进程 0xc0000142、
//   资源管理器安全检查拦「无法访问指定设备」。本工具一键把目标恢复为 Medium。
//
// 编译：.NET Framework 4.0 csc（C# 5）
//   ⇒ 禁用 $""、?.、nameof、表达式体成员、out var、StandardOutputEncoding 等
//
// 实现口径：
//   读标签 = icacls <path>，解析输出里的 "Mandatory Label\Xxx Mandatory Level" 行
//           （标签名 icacls 固定输出英文，不受系统语言影响；ASCII 不受 GBK 解码影响）
//   写标签 = icacls <path> /setintegritylevel [(OI)(CI)] <L|M|H>
//           写 SACL 通常需要管理员权限：失败时弹窗询问后以 runas 拉起自身提权实例完成
//           ⚠️ OS 安全策略：不允许把标签设到高于当前进程自身的完整性级别，
//           故普通权限下设 High 必失败、必须走提权
//
// 自检入口（GUI 无法自动化验证，故留 CLI）：
//   ILSwitcher.exe --get <path> <out.txt>                 查询当前标签，写文件 "label=<...>"
//   ILSwitcher.exe --set <path> <L|M|H> <out.txt> [sub] [show]
//                                                         设置标签，写文件 "ok:<lvl>" / "error:<msg>"

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Text;
using System.Windows.Forms;
using DevLaunch;

namespace ILSwitch
{
    static class Program
    {
        static readonly UTF8Encoding Utf8NoBom = new UTF8Encoding(false);

        [STAThread]
        static int Main(string[] args)
        {
            if (args.Length >= 3 && args[0] == "--get")
            {
                return CliGet(args[1], args[2]);
            }
            if (args.Length >= 4 && args[0] == "--set")
            {
                string path = args[1];
                string lvl = args[2];
                string outFile = args[3];
                bool sub = HasFlag(args, "sub");
                bool show = HasFlag(args, "show");
                return CliSet(path, lvl, outFile, sub, show);
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            // 支持带路径参数启动（如资源管理器「打开方式」）：启动即选中该目标
            string init = (args.Length == 1 && (File.Exists(args[0]) || Directory.Exists(args[0]))) ? args[0] : null;
            Application.Run(new MainForm(init));
            return 0;
        }

        static bool HasFlag(string[] args, string flag)
        {
            for (int i = 4; i < args.Length; i++)
            {
                if (args[i] == flag) return true;
            }
            return false;
        }

        static int CliGet(string path, string outFile)
        {
            string label;
            string err;
            int rc = Ic.QueryLabel(path, out label, out err);
            string text = rc == 0 ? ("label=" + (label.Length == 0 ? "(none)" : label)) : ("error:" + err);
            File.WriteAllText(outFile, text, Utf8NoBom);
            return rc;
        }

        static int CliSet(string path, string lvl, string outFile, bool sub, bool show)
        {
            string outText;
            int rc = Ic.SetLabel(path, lvl, sub, null, out outText);
            string result = rc == 0 ? ("ok:" + lvl) : ("error:" + FirstLine(outText));
            try { File.WriteAllText(outFile, result, Utf8NoBom); }
            catch (Exception) { }
            if (show)
            {
                MessageBox.Show(rc == 0
                    ? ("已完成：「" + path + "」\n\n完整性级别已设置为 " + Ic.LvlName(lvl))
                    : ("设置失败：\n" + outText), "完整性级别切换器");
            }
            return rc == 0 ? 0 : 1;
        }

        static string FirstLine(string s)
        {
            if (s == null) return "";
            s = s.Trim();
            int i = s.IndexOf('\n');
            if (i >= 0) s = s.Substring(0, i);
            return s.Trim();
        }
    }

    // ============================ icacls 封装 ============================
    static class Ic
    {
        // 兼容两种入参：单字母（L/M/H，来自按钮）与完整单词（Low/Medium/High，来自 QueryLabel）
        public static string LvlName(string c)
        {
            if (c == "L" || c == "Low") return "低 Low";
            if (c == "H" || c == "High") return "高 High";
            return "中 Medium";
        }

        // 查询当前标签。返回 0 成功（label 可为 ""=无显式标签），非 0 时 err 含错误信息
        public static int QueryLabel(string path, out string label, out string err)
        {
            label = "";
            err = "";
            string output;
            int rc = RunIcacls("\"" + path + "\"", out output);
            if (rc != 0)
            {
                err = output.Trim();
                return rc;
            }
            string[] lines = output.Split('\n');
            for (int i = 0; i < lines.Length; i++)
            {
                string t = lines[i].ToLowerInvariant();
                if (t.Contains("mandatory label") || t.Contains("强制标签"))
                {
                    if (t.Contains("low") || t.Contains("低")) { label = "Low"; return 0; }
                    if (t.Contains("medium") || t.Contains("中")) { label = "Medium"; return 0; }
                    if (t.Contains("high") || t.Contains("高")) { label = "High"; return 0; }
                    if (t.Contains("system")) { label = "System"; return 0; }
                }
            }
            return 0; // 无显式标签
        }

        // 设置标签。lvl = L/M/H；sub = 同时应用到子项（目录）
        // ⚠️ 三个 OS 行为（均实测）：
        //   1) (OI)(CI) 继承标志只对「新建」子项生效，不下推既有子项
        //   2) icacls /setintegritylevel 不接受多个路径（多路径报「无效参数」）
        //   3) /T 会递归打目录，但**不会改写带显式标签的既有文件**（无标签文件自动继承父目录，无需写）
        //   ⇒ sub = 先 /T 打目录树，再逐个补写文件（带显式标签的那批），进度经 progress 上报
        public static int SetLabel(string path, string lvl, bool sub, Action<int> progress, out string output)
        {
            if (!sub || !Directory.Exists(path))
            {
                return RunIcacls("\"" + path + "\" /setintegritylevel " + lvl, out output);
            }
            int rc = RunIcacls("\"" + path + "\" /setintegritylevel (OI)(CI)" + lvl + " /T", out output);
            List<string> files = new List<string>();
            WalkFiles(path, files);
            int fixedCnt = 0;
            for (int i = 0; i < files.Count; i++)
            {
                if (progress != null && i % 20 == 0) progress(i);
                string fo;
                int r = RunIcacls("\"" + files[i] + "\" /setintegritylevel " + lvl, out fo);
                if (r != 0)
                {
                    if (rc == 0) rc = r;
                    output += "\n" + fo.Trim();
                }
                else
                {
                    fixedCnt++;
                }
            }
            if (progress != null) progress(files.Count);
            if (fixedCnt > 0) output += "\n已补写文件数: " + fixedCnt;
            return rc;
        }

        static void WalkFiles(string dir, List<string> files)
        {
            try
            {
                foreach (string f in Directory.GetFiles(dir)) files.Add(f);
                foreach (string d in Directory.GetDirectories(dir)) WalkFiles(d, files);
            }
            catch (Exception) { } // 无权限的子树跳过
        }

        static int RunIcacls(string arguments, out string output)
        {
            output = "";
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = "icacls.exe";
                psi.Arguments = arguments;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;
                using (Process p = Process.Start(psi))
                {
                    string so = p.StandardOutput.ReadToEnd();
                    string se = p.StandardError.ReadToEnd();
                    p.WaitForExit();
                    output = so + se;
                    return p.ExitCode;
                }
            }
            catch (Exception ex)
            {
                output = ex.Message;
                return -1;
            }
        }
    }

    // ============================ 拖拽区（虚线框） ============================
    class DropZone : Panel
    {
        public DropZone()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            BackColor = Th.Bg;
            AllowDrop = true;
            Cursor = Cursors.Hand;
        }

        static GraphicsPath RoundRect(Rectangle r, int rad)
        {
            GraphicsPath p = new GraphicsPath();
            p.AddArc(r.X, r.Y, rad * 2, rad * 2, 180, 90);
            p.AddArc(r.Right - rad * 2, r.Y, rad * 2, rad * 2, 270, 90);
            p.AddArc(r.Right - rad * 2, r.Bottom - rad * 2, rad * 2, rad * 2, 0, 90);
            p.AddArc(r.X, r.Bottom - rad * 2, rad * 2, rad * 2, 90, 90);
            p.CloseFigure();
            return p;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(BackColor);
            Rectangle r = ClientRectangle;
            r.Inflate(-1, -1);
            using (Pen pen = new Pen(Th.Dim))
            {
                pen.DashStyle = DashStyle.Dash;
                using (GraphicsPath path = RoundRect(r, 10))
                {
                    g.DrawPath(pen, path);
                }
            }
        }
    }

    // ============================ 主窗体 ============================
    class MainForm : Form
    {
        string _path = "";
        Label lblPath;
        Label lblNow;
        Label lblMsg;
        CheckBox chkSub;
        FlatBtn btnLow;
        FlatBtn btnMed;
        FlatBtn btnHigh;
        ToolTip tip;

        public MainForm(string initPath)
        {
            Text = "完整性级别切换器";
            ClientSize = new Size(660, 414);
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Th.Bg;
            Font = Th.Ui;
            AllowDrop = true;

            tip = new ToolTip();

            // ---- 卡片：目标（大虚线拖拽区） ----
            Card cT = Ux.AddCard(this, "目标", 16, 12, 628, 212);

            DropZone zone = new DropZone();
            zone.SetBounds(16, 36, 596, 112);
            cT.Controls.Add(zone);

            Label icon = ZoneLabel(Glyph.Folder, 0, 8, 596, 44, Color.FromArgb(226, 168, 68));
            icon.Font = new Font(Th.Glyph.FontFamily, 26);
            icon.Cursor = Cursors.Hand;
            zone.Controls.Add(icon);

            Label l1 = ZoneLabel("将 文 件 / 文 件 夹 拖 拽 到 此 处", 0, 52, 596, 20, Th.Fg);
            l1.Cursor = Cursors.Hand;
            zone.Controls.Add(l1);

            LinkLabel lnkFile = new LinkLabel();
            lnkFile.Text = "点击选择文件";
            lnkFile.LinkColor = Th.Cyan;
            lnkFile.ActiveLinkColor = Th.Cyan;
            lnkFile.VisitedLinkColor = Th.Cyan;
            lnkFile.LinkBehavior = LinkBehavior.HoverUnderline;
            lnkFile.BackColor = Th.Bg;
            lnkFile.AutoSize = false;
            lnkFile.SetBounds(148, 78, 110, 20);
            lnkFile.TextAlign = ContentAlignment.MiddleCenter;
            lnkFile.LinkClicked += delegate { BrowseFile(); };
            zone.Controls.Add(lnkFile);

            Label sep = ZoneLabel("·", 288, 78, 20, 20, Th.Dim);
            zone.Controls.Add(sep);

            LinkLabel lnkDir = new LinkLabel();
            lnkDir.Text = "选择文件夹";
            lnkDir.LinkColor = Th.Cyan;
            lnkDir.ActiveLinkColor = Th.Cyan;
            lnkDir.VisitedLinkColor = Th.Cyan;
            lnkDir.LinkBehavior = LinkBehavior.HoverUnderline;
            lnkDir.BackColor = Th.Bg;
            lnkDir.AutoSize = false;
            lnkDir.SetBounds(336, 78, 100, 20);
            lnkDir.TextAlign = ContentAlignment.MiddleCenter;
            lnkDir.LinkClicked += delegate { BrowseDir(); };
            zone.Controls.Add(lnkDir);

            zone.Click += delegate { BrowseFile(); };
            icon.Click += delegate { BrowseFile(); };
            l1.Click += delegate { BrowseFile(); };
            sep.Click += delegate { BrowseFile(); };

            DragEnter += DoDragEnter;
            DragDrop += DoDragDrop;
            zone.DragEnter += DoDragEnter;
            zone.DragDrop += DoDragDrop;

            lblPath = Ux.LblC(cT, "", 16, 156, 596, Th.Dim);

            lblNow = Ux.LblC(cT, "当前：未选择", 16, 180, 500, Th.Fg);
            lblNow.Font = Th.UiB;

            FlatBtn bRef = Ux.BtnIC(cT, Glyph.Sync, "刷新", 532, 176, 80, false);
            bRef.Height = 26;
            bRef.Click += delegate { RefreshLabel(); };

            // ---- 卡片：切换 ----
            Card cS = Ux.AddCard(this, "切换完整性级别（点对应按钮即可）", 16, 236, 628, 118);

            btnLow = Ux.BtnC(cS, "低  Low", 16, 36, 116, false);
            btnLow.Height = 36;
            btnLow.Click += delegate { Apply("L"); };

            btnMed = Ux.BtnC(cS, "中  Medium", 148, 36, 116, false);
            btnMed.Height = 36;
            tip.SetToolTip(btnMed, "普通程序的标准级别 —— 工程内编译出的 exe 通常需要用它");
            btnMed.Click += delegate { Apply("M"); };

            btnHigh = Ux.BtnC(cS, "高  High", 280, 36, 116, false);
            btnHigh.Height = 36;
            btnHigh.Click += delegate { Apply("H"); };

            chkSub = Ux.ToggleC(cS, "同时应用到子项（仅目录）", 408, 44, 204, false);

            Ux.LblC(cS, "Low=沙箱/受限   Medium=普通程序(系统默认)   High=管理员级（慎用）", 16, 84, 596, Th.Faint);

            // ---- 结果行 ----
            lblMsg = Ux.Val("", 16, 366, 628, Th.Dim);
            lblMsg.Font = Th.UiB;

            if (initPath != null)
            {
                Shown += delegate { SetTarget(initPath); };
            }
        }

        Label ZoneLabel(string t, int x, int y, int w, int h, Color c)
        {
            Label l = new Label();
            l.Text = t;
            l.ForeColor = c;
            l.BackColor = Th.Bg;
            l.AutoSize = false;
            l.SetBounds(x, y, w, h);
            l.TextAlign = ContentAlignment.MiddleCenter;
            return l;
        }

        static void DoDragEnter(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop)) e.Effect = DragDropEffects.Copy;
        }

        void DoDragDrop(object sender, DragEventArgs e)
        {
            string[] files = e.Data.GetData(DataFormats.FileDrop) as string[];
            if (files == null || files.Length == 0) return;
            SetTarget(files[0]);
            if (files.Length > 1) Msg(true, "已取第一项（共拖入 " + files.Length + " 项）");
        }

        void BrowseDir()
        {
            using (FolderBrowserDialog d = new FolderBrowserDialog())
            {
                d.ShowNewFolderButton = false;
                if (d.ShowDialog(this) == DialogResult.OK) SetTarget(d.SelectedPath);
            }
        }

        void BrowseFile()
        {
            using (OpenFileDialog d = new OpenFileDialog())
            {
                if (d.ShowDialog(this) == DialogResult.OK) SetTarget(d.FileName);
            }
        }

        void SetTarget(string path)
        {
            _path = path;
            lblPath.Text = path.Length > 76 ? "…" + path.Substring(path.Length - 75) : path;
            RefreshLabel();
        }

        // 把「当前级别」点亮成主按钮样式（青色），其余恢复普通样式——
        // 拖入目标后自动定位，切换后跟随新级别移动
        void MarkLevel(string label)
        {
            bool low = label == "Low" || label == "L";
            bool high = label == "High" || label == "H";
            bool med = !low && !high && label.Length > 0;   // Medium（无显式标签不点亮）
            if (btnLow.Primary != low) { btnLow.Primary = low; btnLow.Invalidate(); }
            if (btnMed.Primary != med) { btnMed.Primary = med; btnMed.Invalidate(); }
            if (btnHigh.Primary != high) { btnHigh.Primary = high; btnHigh.Invalidate(); }
        }

        void RefreshLabel()
        {
            if (_path.Length == 0) { lblNow.Text = "当前：未选择"; MarkLevel(""); return; }
            if (!File.Exists(_path) && !Directory.Exists(_path))
            {
                lblNow.Text = "当前：路径不存在";
                MarkLevel("");
                return;
            }
            string label, err;
            int rc = Ic.QueryLabel(_path, out label, out err);
            if (rc != 0)
            {
                lblNow.Text = "当前：查询失败";
                Msg(false, "查询失败：" + err);
                MarkLevel("");
                return;
            }
            string kind = Directory.Exists(_path) ? "文件夹" : "文件";
            if (label.Length == 0)
            {
                lblNow.Text = "当前：" + kind + "，无显式标签（等效 Medium，随访问进程令牌）";
                MarkLevel("");
            }
            else
            {
                lblNow.Text = "当前：" + kind + "，" + Ic.LvlName(label);
                MarkLevel(label);
            }
        }

        void Apply(string lvl)
        {
            if (_path.Length == 0)
            {
                MessageBox.Show(this, "请先选择目标：把文件 / 文件夹拖进上方虚线框，或点击选择。",
                    "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            if (!File.Exists(_path) && !Directory.Exists(_path))
            {
                Msg(false, "路径不存在");
                return;
            }
            bool sub = chkSub.Checked && Directory.Exists(_path);

            // 已是该级别且不含子项：明确告知，不做无变化操作。
            // ⚠️ 勾了「应用到子项」时不能短路：目录本身可能已是该级别，但子项未必。
            string cur, qerr;
            if (!sub && Ic.QueryLabel(_path, out cur, out qerr) == 0 && cur == MapName(lvl))
            {
                Msg(true, "目标已经是 " + Ic.LvlName(lvl) + "，无需更改");
                MessageBox.Show(this,
                    "目标已经是 " + Ic.LvlName(lvl) + "，无需更改。\n\n" +
                    "若要让其内部的文件 / 子文件夹也写入该级别，请勾选「同时应用到子项」后再点一次。",
                    "无需更改", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            Msg(true, sub ? "正在设置（递归子项，文件多时需稍候）…" : "正在设置…");
            Cursor = Cursors.WaitCursor;
            string output;
            int rc = Ic.SetLabel(_path, lvl, sub, delegate(int done)
            {
                lblMsg.Text = "正在设置… 已处理 " + done + " 个文件";
                Application.DoEvents();
            }, out output);
            Cursor = Cursors.Default;
            if (rc == 0)
            {
                Msg(true, "✔ 已设置为 " + Ic.LvlName(lvl) + (sub ? "（含子项）" : ""));
                RefreshLabel();
                MessageBox.Show(this,
                    "已完成：「" + _path + "」\n\n完整性级别 = " + Ic.LvlName(lvl) + (sub ? "（含子项）" : ""),
                    "完成", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }
            // 失败即询问提权重试（典型原因：新标签高于当前进程的完整性级别，需管理员）
            DialogResult dr = MessageBox.Show(this,
                "设置失败：" + output.Trim() + "\n\n常见原因是新级别高于当前进程权限。是否以管理员身份重试？",
                "需要提权", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
            if (dr != DialogResult.Yes) { Msg(false, "已取消（权限不足）"); return; }
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = Application.ExecutablePath;
                psi.Arguments = "--set \"" + _path + "\" " + lvl + " \"" +
                    Path.Combine(Path.GetTempPath(), "ilswitch_result.txt") + "\"" +
                    (sub ? " sub" : "") + " show";
                psi.Verb = "runas";
                psi.UseShellExecute = true;
                Process.Start(psi);
                Msg(true, "已请求管理员权限处理，结果见提权实例弹窗");
                RefreshLabel();
            }
            catch (Exception)
            {
                Msg(false, "已取消（UAC）");
            }
        }

        static string MapName(string lvl)
        {
            if (lvl == "L") return "Low";
            if (lvl == "H") return "High";
            return "Medium";
        }

        void Msg(bool ok, string s)
        {
            lblMsg.Text = s;
            lblMsg.ForeColor = s.Length == 0 ? Th.Dim : (ok ? Th.Ok : Th.Err);
        }
    }
}
