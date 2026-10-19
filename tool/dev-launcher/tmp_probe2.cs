using System;
using System.Collections.Generic;
using System.Drawing;
using System.Net;
using System.Net.Sockets;
using System.Threading;

// 临时：对 WinForms 控件渲染行为做「经验性验证」（无法编译 csc 时的替代手段）。
// 用法：tmp_probe.exe <输出txt>
// 说明：这里的每个结论最后都要用真实截图复核，不能替代 UI 验收。
static class Probe
{
    static List<string> lines = new List<string>();

    static void W(string s) { lines.Add(s); Console.WriteLine(s); }

    static void Main(string[] a)
    {
        Run(a);
        if (a.Length > 0) System.IO.File.WriteAllText(a[0], string.Join("\r\n", lines.ToArray()));
    }

    static void Run(string[] a)
    {
        W("== WinForms 控件渲染行为探测 ==");

        // 1) 有没有可用的「显示」环境（桌面 / 会话）
        try
        {
            W("ScreenCount=" + System.Windows.Forms.Screen.AllScreens.Length);
            W("Screen0=" + System.Windows.Forms.Screen.PrimaryScreen.Bounds);
        }
        catch (Exception ex) { W("Screen: EX " + ex.GetType().Name + " " + ex.Message); }

        // 2) 字体存在性（Theme.Pick 会按存在性挑字体，这里确认候选都在）
        string[] want = { "Segoe UI", "Segoe UI Semibold", "Microsoft YaHei UI", "Consolas", "Cascadia Mono",
                          "Segoe MDL2 Assets", "Segoe Fluent Icons" };
        using (System.Drawing.Text.InstalledFontCollection col = new System.Drawing.Text.InstalledFontCollection())
        {
            List<string> have = new List<string>();
            foreach (System.Drawing.FontFamily ff in col.Families) have.Add(ff.Name);
            foreach (string w in want) W("Font " + w + " = " + have.Contains(w));
        }

        // 3) GraphicsPath 圆角在 0/负尺寸下是否会抛（Draw2.Round 需要防御）
        try
        {
            using (System.Drawing.Drawing2D.GraphicsPath p = Draw2Round(new RectangleF(0, 0, 0, 0), 6))
                W("Round(0x0) ok points=" + p.PointCount);
        }
        catch (Exception ex) { W("Round(0x0) EX " + ex.GetType().Name + ": " + ex.Message); }

        try
        {
            using (System.Drawing.Drawing2D.GraphicsPath p = Draw2Round(new RectangleF(0, 0, 10, 40), 6))
                W("Round(radius>h/2) ok points=" + p.PointCount);
        }
        catch (Exception ex) { W("Round(radius>h/2) EX " + ex.GetType().Name + ": " + ex.Message); }

        // 4) ComboBox 禁用态 / owner-draw 下的颜色行为（无法在此确认，只记录设计假设）
        W("ASSUME: FlatCombo 禁用态由 Windows 主题绘制，ForeColor 不生效 → 用 Enabled 之外的视觉表达");
    }

    static System.Drawing.Drawing2D.GraphicsPath Draw2Round(RectangleF r, float radius)
    {
        System.Drawing.Drawing2D.GraphicsPath p = new System.Drawing.Drawing2D.GraphicsPath();
        float d = Math.Max(0F, Math.Min(radius, Math.Min(r.Width, r.Height) / 2F)) * 2F;
        if (d <= 0.5F) { p.AddRectangle(r); return p; }
        p.AddArc(r.X, r.Y, d, d, 180, 90);
        p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
        p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
        p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
        p.CloseFigure();
        return p;
    }
}
