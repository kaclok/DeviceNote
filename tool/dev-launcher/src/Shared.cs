// DevLauncher 公共库 —— 主题 UI / 进程管理 / 项目探测
// 目标编译器：.NET Framework 4.0 csc (C# 5)，勿使用字符串插值、?.、nameof、$"" 等 C#6+ 语法
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Linq;
using System.Management;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;
using System.Xml;

namespace DevLaunch
{
    // ============================ 主题 ============================
    // 深色玻璃风：大圆角卡片 + 细描边 + 单色青蓝强调色，状态用「色点 + 文字」表达
    static class Th
    {
        // ---- 底色层次（越靠前越亮）----
        public static readonly Color Bg = Color.FromArgb(18, 20, 25);          // 窗体底
        public static readonly Color Card = Color.FromArgb(27, 30, 37);        // 卡片
        public static readonly Color CardHi = Color.FromArgb(34, 38, 46);      // 卡片内嵌区
        public static readonly Color Input = Color.FromArgb(36, 40, 49);       // 输入框 / 次按钮
        public static readonly Color Panel = Color.FromArgb(27, 30, 37);
        public static readonly Color Hover = Color.FromArgb(50, 55, 66);
        public static readonly Color Press = Color.FromArgb(44, 48, 58);

        // ---- 文字 ----
        public static readonly Color Fg = Color.FromArgb(232, 236, 242);       // 主文字
        public static readonly Color Dim = Color.FromArgb(140, 147, 160);      // 次要文字
        public static readonly Color Faint = Color.FromArgb(104, 111, 124);    // 更弱（提示 / 占位）

        // ---- 强调色（青蓝）----
        public static readonly Color Accent = Color.FromArgb(47, 143, 235);
        public static readonly Color AccHov = Color.FromArgb(70, 163, 252);
        public static readonly Color AccPress = Color.FromArgb(36, 122, 208);
        public static readonly Color AccentText = Color.FromArgb(105, 190, 255);

        // ---- 状态色 ----
        public static readonly Color Ok = Color.FromArgb(64, 208, 154);
        public static readonly Color Warn = Color.FromArgb(242, 188, 88);
        public static readonly Color Err = Color.FromArgb(245, 112, 112);
        public static readonly Color Cyan = Color.FromArgb(105, 190, 255);

        // ---- 描边 / 分隔 ----
        public static readonly Color Border = Color.FromArgb(48, 53, 63);
        public static readonly Color BorderHi = Color.FromArgb(72, 80, 94);
        public static readonly Color Divider = Color.FromArgb(38, 42, 50);

        // ---- 日志 ----
        public static readonly Color LogBg = Color.FromArgb(14, 16, 20);
        public static readonly Color LogFg = Color.FromArgb(206, 212, 222);

        // ---- 字体（按存在性挑，缺失则回退雅黑）----
        public static readonly Font Ui = Pick(new string[] { "Segoe UI", "Microsoft YaHei UI" }, 9F, FontStyle.Regular);
        public static readonly Font UiB = Pick(new string[] { "Segoe UI Semibold", "Microsoft YaHei UI" }, 9F, FontStyle.Bold);
        public static readonly Font UiS = Pick(new string[] { "Segoe UI", "Microsoft YaHei UI" }, 8.25F, FontStyle.Regular);
        public static readonly Font Mono = Pick(new string[] { "Cascadia Mono", "Consolas" }, 9F, FontStyle.Regular);
        public static readonly Font Glyph = Pick(new string[] { "Segoe Fluent Icons", "Segoe MDL2 Assets" }, 10F, FontStyle.Regular);
        public static readonly Font GlyphS = Pick(new string[] { "Segoe Fluent Icons", "Segoe MDL2 Assets" }, 7F, FontStyle.Regular);

        static Font Pick(string[] names, float size, FontStyle style)
        {
            try
            {
                using (InstalledFontCollection col = new InstalledFontCollection())
                {
                    foreach (string n in names)
                    {
                        foreach (FontFamily ff in col.Families)
                        {
                            if (ff.Name.Equals(n, StringComparison.OrdinalIgnoreCase))
                                return new Font(n, size, style, GraphicsUnit.Point);
                        }
                    }
                }
            }
            catch (Exception) { }
            return new Font("Microsoft YaHei UI", size, style, GraphicsUnit.Point);
        }
    }

    // ============================ 绘制工具 ============================
    static class Draw2
    {
        public static GraphicsPath Round(RectangleF r, float radius)
        {
            GraphicsPath p = new GraphicsPath();
            float d = Math.Max(0F, Math.Min(radius, Math.Min(r.Width, r.Height) / 2F)) * 2F;
            if (d <= 0.5F)
            {
                p.AddRectangle(r);
                return p;
            }
            p.AddArc(r.X, r.Y, d, d, 180, 90);
            p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
            p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
            p.CloseFigure();
            return p;
        }

        public static void Fill(Graphics g, RectangleF r, float radius, Color c)
        {
            if (r.Width <= 0 || r.Height <= 0) return;
            using (GraphicsPath p = Round(r, radius))
            using (SolidBrush b = new SolidBrush(c))
                g.FillPath(b, p);
        }

        public static void Stroke(Graphics g, RectangleF r, float radius, Color c, float w)
        {
            if (r.Width <= 1 || r.Height <= 1) return;
            RectangleF rr = new RectangleF(r.X + w / 2F, r.Y + w / 2F, r.Width - w, r.Height - w);
            using (GraphicsPath p = Round(rr, radius))
            using (Pen pen = new Pen(c, w))
                g.DrawPath(pen, p);
        }

        // 细字体的抗锯齿文字（文本框 / 日志这类要按像素对齐的地方不用）
        public static void Text(Graphics g, string s, Font f, Color c, RectangleF r, ContentAlignment al)
        {
            if (string.IsNullOrEmpty(s) || r.Width <= 2) return;
            TextFormatFlags flags = TextFormatFlags.NoPadding | TextFormatFlags.NoPrefix | TextFormatFlags.SingleLine;
            if (al == ContentAlignment.MiddleLeft || al == ContentAlignment.TopLeft || al == ContentAlignment.BottomLeft) flags |= TextFormatFlags.Left;
            else if (al == ContentAlignment.MiddleCenter || al == ContentAlignment.TopCenter || al == ContentAlignment.BottomCenter) flags |= TextFormatFlags.HorizontalCenter;
            else flags |= TextFormatFlags.Right;
            if (al == ContentAlignment.MiddleLeft || al == ContentAlignment.MiddleCenter || al == ContentAlignment.MiddleRight) flags |= TextFormatFlags.VerticalCenter;
            else if (al == ContentAlignment.TopLeft || al == ContentAlignment.TopCenter || al == ContentAlignment.TopRight) flags |= TextFormatFlags.Top;
            else flags |= TextFormatFlags.Bottom;
            bool ellipsis = al == ContentAlignment.MiddleLeft || al == ContentAlignment.MiddleCenter || al == ContentAlignment.MiddleRight;
            if (ellipsis) flags |= TextFormatFlags.EndEllipsis;
            Rectangle rr = new Rectangle((int)r.X, (int)r.Y, (int)Math.Ceiling(r.Width), (int)Math.Ceiling(r.Height));
            TextRenderer.DrawText(g, s, f, rr, c, flags);
        }

        public static Size2 Measure(string s, Font f)
        {
            return new Size2(TextRenderer.MeasureText(s, f).Width, TextRenderer.MeasureText(s, f).Height);
        }

        // 简单尺寸容器（避免到处写 SizeF）
        public struct Size2
        {
            public int W, H;
            public Size2(int w, int h) { W = w; H = h; }
        }
    }

    // 分段控件里的一个图标 / 一小段文字
    // （用于日志分栏的「切换」页签，以及状态点前的小图标）
    static class Glyph
    {
        // Segoe MDL2 Assets / Segoe Fluent Icons 字符
        public const string Play = "\uE768";
        public const string PlayWin = "\uE102";
        public const string Stop = "\uE71A";
        public const string Power = "\uE7E8";
        public const string Folder = "\uE8B7";
        public const string File = "\uE8A5";
        public const string Refresh = "\uE72C";
        public const string Settings = "\uE713";
        public const string Pack = "\uE7B8";
        public const string OpenIn = "\uE8A7";
        public const string Copy = "\uE8C8";
        public const string Erase = "\uE894";
        public const string Globe = "\uE774";
        public const string Clear = "\uE894";
        public const string Check = "\uE73E";
        public const string Warning = "\uE7BA";
        public const string Error = "\uE783";
        public const string Sync = "\uE895";
        public const string ChevronDown = "\uE70D";
        public const string Close = "\uE711";

        public static string Pick(string glyph)
        {
            // 字体缺失时字符会变成方框；这里不处理（Windows 10+ 都有 Segoe MDL2 Assets）
            return glyph;
        }
    }

    // ============================ 控件 ============================
    // 通用按钮：圆角、按下变暗、禁用灰、主按钮带强调色
    class FlatBtn : Button
    {
        // 保持旧字段名（三个窗体的代码里直接赋 Base / Hover 的地方仍然有效）
        public Color Base = Th.Input;
        public Color Hover = Th.Hover;
        public int Radius = 6;
        public bool Primary;
        public string GlyphText = "";        // 左侧图标（Segoe MDL2）

        bool _down, _over;

        public FlatBtn()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            FlatStyle = FlatStyle.Flat;
            FlatAppearance.BorderSize = 0;
            BackColor = Th.Card;
            ForeColor = Th.Fg;
            Font = Th.Ui;
            Height = 32;
            Cursor = Cursors.Hand;
            UseVisualStyleBackColor = false;
            TabStop = false;
        }

        public void PrimaryStyle()
        {
            Primary = true;
            Base = Th.Accent;
            Hover = Th.AccHov;
        }

        protected override void OnMouseEnter(EventArgs e)
        {
            base.OnMouseEnter(e);
            if (!Enabled) return;
            _over = true;
            if (Cursor != Cursors.Hand) Cursor = Cursors.Hand;
            Invalidate();
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            base.OnMouseLeave(e);
            _over = false;
            _down = false;
            Invalidate();
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            base.OnMouseDown(e);
            if (!Enabled) return;
            _down = true;
            Invalidate();
        }

        protected override void OnMouseUp(MouseEventArgs e)
        {
            base.OnMouseUp(e);
            _down = false;
            Invalidate();
        }

        protected override void OnEnabledChanged(EventArgs e)
        {
            base.OnEnabledChanged(e);
            Cursor = Enabled ? Cursors.Hand : Cursors.Default;
            Invalidate();
        }

        protected override void OnTextChanged(EventArgs e)
        {
            base.OnTextChanged(e);
            Invalidate();
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.Clear(BackColor);

            Color fill;
            if (!Enabled) fill = Th.CardHi;
            else if (Primary) fill = _down ? Th.AccPress : (_over ? Th.AccHov : Th.Accent);
            else if (_down) fill = Th.Press;
            else if (_over) fill = Hover;
            else fill = Base;

            RectangleF r = new RectangleF(0, 0, Width, Height);
            Draw2.Fill(g, r, Radius, fill);
            // 次按钮加一道细描边，主按钮不加（避免边缘发脏）
            if (!Primary && Enabled) Draw2.Stroke(g, r, Radius, Th.Border, 1F);

            Color fg;
            if (!Enabled) fg = Th.Faint;
            else if (Primary) fg = Color.White;
            else if (_over) fg = Th.Fg;
            else fg = Th.Fg;

            // 图标（可选，位于左侧，以居中偏左的方式对齐）
            float tx = 0, tw = Width;
            if (GlyphText.Length > 0)
            {
                Size gw = TextRenderer.MeasureText(GlyphText, Th.Glyph, new Size(Height * 3, Height * 3),
                    TextFormatFlags.NoPadding | TextFormatFlags.SingleLine);
                Size textW = string.IsNullOrEmpty(Text) ? new Size(0, 0)
                    : TextRenderer.MeasureText(Text, Font, new Size(Width * 2, Height * 3),
                        TextFormatFlags.NoPadding | TextFormatFlags.SingleLine);
                float total = gw.Width + (textW.Width > 0 ? 5 + textW.Width : 0);
                float x = (Width - total) / 2F;
                RectangleF gr = new RectangleF(x, 0, gw.Width + 2, Height);
                TextRenderer.DrawText(g, GlyphText, Th.Glyph,
                    new Rectangle((int)gr.X, (int)gr.Y, (int)gr.Width, (int)gr.Height), fg,
                    TextFormatFlags.NoPadding | TextFormatFlags.SingleLine | TextFormatFlags.VerticalCenter | TextFormatFlags.Left);
                tx = x + gw.Width + 5;
                tw = Width - tx;
            }
            if (!string.IsNullOrEmpty(Text))
            {
                Draw2.Text(g, Text, Font, fg, new RectangleF(tx, 0, tw, Height), ContentAlignment.MiddleCenter);
            }
        }
    }

    // 下拉框：owner-draw，深色下保持统一观感
    class FlatCombo : ComboBox
    {
        public FlatCombo()
        {
            SetStyle(ControlStyles.OptimizedDoubleBuffer, true);
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
            if (e.Index < 0 || e.Index >= Items.Count) return;
            bool sel = (e.State & DrawItemState.Selected) == DrawItemState.Selected;
            Color bg = sel ? Th.Accent : Th.CardHi;
            using (SolidBrush b = new SolidBrush(bg))
                e.Graphics.FillRectangle(b, e.Bounds);
            string t = Convert.ToString(Items[e.Index]);
            TextRenderer.DrawText(e.Graphics, t, Th.Ui,
                new Point(e.Bounds.X + 8, e.Bounds.Y + (e.Bounds.Height - Th.Ui.Height) / 2),
                sel ? Color.White : Th.Fg,
                TextFormatFlags.NoPrefix | TextFormatFlags.SingleLine | TextFormatFlags.EndEllipsis);
        }
    }

    // 可点的小开关（「启动后自动打开页面」这类）
    class PillToggle : CheckBox
    {
        public PillToggle()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            Font = Th.Ui;
            ForeColor = Th.Fg;
            BackColor = Color.Transparent;
            Cursor = Cursors.Hand;
            AutoSize = false;
            Height = 26;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            // Transparent 在 UserPaint 下会 Clear 成黑；回退父级底色
            Color bg = BackColor;
            if (bg.A == 0) bg = Parent != null ? Parent.BackColor : Th.Bg;
            g.Clear(bg);
            bool on = Checked;
            int sw = 34, sh = 18;
            RectangleF track = new RectangleF(1, (Height - sh) / 2F, sw, sh);
            Draw2.Fill(g, track, sh / 2F, on ? Th.Accent : Th.BorderHi);
            float kr = sh - 6;
            float kx = on ? track.Right - kr - 3 : track.X + 3;
            using (SolidBrush kb = new SolidBrush(on ? Color.White : Color.FromArgb(170, 176, 188)))
                g.FillEllipse(kb, kx, track.Y + 3, kr, kr);
            Draw2.Text(g, Text, Font, on ? Th.Fg : Th.Dim, new RectangleF(sw + 8, 0, Width - sw - 8, Height), ContentAlignment.MiddleLeft);
        }

        protected override void OnCheckedChanged(EventArgs e)
        {
            base.OnCheckedChanged(e);
            Invalidate();
        }

        protected override void OnMouseEnter(EventArgs e)
        {
            base.OnMouseEnter(e);
            Invalidate();
        }
    }

    // 带小标题的卡片容器（圆角 + 描边），子控件由调用方用绝对坐标放进去
    // 子控件坐标相对卡片；标题带高 HeadH，内容从 y >= HeadH 开始摆
    class Card : Panel
    {
        public string Title = "";
        public string Subtitle = "";
        public const int HeadH = 26;

        public Card()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            BackColor = Th.Bg;
            ForeColor = Th.Fg;
        }

        protected override void OnPaintBackground(PaintEventArgs e)
        {
            // 铺窗体底色：圆角以外的四角呈现“透明”观感
            using (SolidBrush b = new SolidBrush(Th.Bg)) e.Graphics.FillRectangle(b, ClientRectangle);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            RectangleF r = new RectangleF(0, 0, Width, Height);
            Draw2.Fill(g, r, 10F, Th.Card);
            Draw2.Stroke(g, r, 10F, Th.Border, 1F);
            if (Title.Length > 0)
            {
                Draw2.Fill(g, new RectangleF(14, 8, 3, 12), 1.5F, Th.Accent);
                Draw2.Text(g, Title, Th.UiB, Th.Fg,
                    new RectangleF(23, 3, Math.Max(60, Width - 160), 20), ContentAlignment.MiddleLeft);
                if (Subtitle.Length > 0)
                    Draw2.Text(g, Subtitle, Th.UiS, Th.Dim,
                        new RectangleF(Width - 140, 4, 128, 18), ContentAlignment.MiddleRight);
            }
        }

        public void SetBoundsJ(int x, int y, int w, int h)
        {
            SetBounds(x, y, w, h);
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
            l.UseMnemonic = false;
            return l;
        }

        public static Label Val(string text, int x, int y, int w, Color c)
        {
            Label l = Lbl(text, x, y, w);
            l.ForeColor = c;
            return l;
        }

        // 给控件套一层圆角描边的输入框
        public static Panel Field(Control inner, int x, int y, int w, int h)
        {
            Panel p = new RoundPanel();
            p.SetBounds(x, y, w, h);
            p.Padding = new Padding(10, 1, 8, 1);
            p.BackColor = Th.Card;
            TextBox tb = inner as TextBox;
            if (tb != null)
            {
                tb.BorderStyle = BorderStyle.None;
                tb.BackColor = Th.Input;
                tb.ForeColor = Th.Fg;
            }
            ComboBox cb = inner as ComboBox;
            if (cb != null) { cb.BackColor = Th.Input; cb.ForeColor = Th.Fg; }
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
            if (primary) b.PrimaryStyle();
            return b;
        }

        // 带图标的按钮：宽度按「图标 + 文字」实测自适应，传入的 w 只是下限
        public static FlatBtn BtnI(string glyph, string text, int x, int y, int w, bool primary)
        {
            FlatBtn b = Btn(text, x, y, FitW(glyph, text, w), primary);
            b.GlyphText = glyph;
            return b;
        }

        // 实测图标与文字宽度，保证按钮装得下内容（NoPadding 口径，与 FlatBtn.OnPaint 一致）
        public static int FitW(string glyph, string text, int w)
        {
            int need = 16;
            if (!string.IsNullOrEmpty(glyph))
            {
                Size gw = TextRenderer.MeasureText(glyph, Th.Glyph, new Size(120, 40),
                    TextFormatFlags.NoPadding | TextFormatFlags.SingleLine);
                need += gw.Width + 5;
            }
            if (!string.IsNullOrEmpty(text))
            {
                Size tw = TextRenderer.MeasureText(text, Th.Ui, new Size(400, 40),
                    TextFormatFlags.NoPadding | TextFormatFlags.SingleLine);
                need += tw.Width;
            }
            return Math.Max(w, need);
        }

        // ---- 卡片内摆放：坐标相对卡片；标签底色对齐卡片填充色，避免透明标签露出底色 ----
        public static Card AddCard(Form f, string title, int x, int y, int w, int h)
        {
            Card c = new Card();
            c.Title = title;
            c.SetBounds(x, y, w, h);
            f.Controls.Add(c);
            return c;
        }

        public static Label LblC(Control host, string text, int x, int y, int w, Color c)
        {
            Label l = Val(text, x, y, w, c);
            l.BackColor = Th.Card;
            host.Controls.Add(l);
            return l;
        }

        public static Panel FieldC(Control host, Control inner, int x, int y, int w, int h)
        {
            Panel p = Field(inner, x, y, w, h);
            host.Controls.Add(p);
            return p;
        }

        public static FlatBtn BtnC(Control host, string text, int x, int y, int w, bool primary)
        {
            FlatBtn b = Btn(text, x, y, w, primary);
            host.Controls.Add(b);
            return b;
        }

        public static FlatBtn BtnIC(Control host, string glyph, string text, int x, int y, int w, bool primary)
        {
            FlatBtn b = BtnI(glyph, text, x, y, w, primary);
            host.Controls.Add(b);
            return b;
        }

        public static CheckBox ToggleC(Control host, string text, int x, int y, int w, bool val)
        {
            PillToggle t = new PillToggle();
            t.Text = text;
            t.Checked = val;
            t.SetBounds(x, y, w, 24);
            t.BackColor = Th.Card;
            host.Controls.Add(t);
            return t;
        }

        public static void Dark(Form f)
        {
            f.BackColor = Th.Bg;
            f.ForeColor = Th.Fg;
            f.Font = Th.Ui;
            f.FormBorderStyle = FormBorderStyle.Sizable;
            f.MaximizeBox = true;
            f.StartPosition = FormStartPosition.CenterScreen;
            try { f.Icon = Ux.MakeIcon(); } catch (Exception) { }
        }

        // 程序图标：圆角方块 + 播放三角（不依赖外部文件，单 exe 拷贝即用）
        static Icon _icon;

        public static Icon MakeIcon()
        {
            if (_icon != null) return _icon;
            try
            {
                using (Bitmap bmp = new Bitmap(32, 32))
                {
                    using (Graphics g = Graphics.FromImage(bmp))
                    {
                        g.SmoothingMode = SmoothingMode.AntiAlias;
                        g.Clear(Color.Transparent);
                        using (GraphicsPath p = Draw2.Round(new RectangleF(1, 1, 30, 30), 7))
                        using (LinearGradientBrush lg = new LinearGradientBrush(new RectangleF(1, 1, 30, 30),
                            Color.FromArgb(72, 168, 255), Color.FromArgb(38, 112, 210), 55F))
                            g.FillPath(lg, p);
                        PointF[] tri = new PointF[] {
                            new PointF(13, 10), new PointF(23, 16), new PointF(13, 22)
                        };
                        using (SolidBrush wb = new SolidBrush(Color.White)) g.FillPolygon(wb, tri);
                    }
                    _icon = Icon.FromHandle(bmp.GetHicon());
                    return _icon;
                }
            }
            catch (Exception) { return null; }
        }
    }

    // 圆角描边面板（输入框外壳 / 日志外壳 / 卡片）
    class RoundPanel : Panel
    {
        public int Radius = 7;
        public bool DrawBorder = true;
        public float BorderWidth = 1F;
        public Color BorderColor = Th.Border;
        public bool DrawFill = true;

        public RoundPanel()
        {
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer
                | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            BackColor = Th.Bg;
        }

        protected override void OnPaintBackground(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            using (SolidBrush b = new SolidBrush(BackColor)) g.FillRectangle(b, ClientRectangle);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.SmoothingMode = SmoothingMode.AntiAlias;
            RectangleF r = new RectangleF(0, 0, Width, Height);
            if (DrawFill) Draw2.Fill(g, r, Radius, FillColor());
            if (DrawBorder) Draw2.Stroke(g, r, Radius, BorderColor, BorderWidth);
        }

        protected virtual Color FillColor()
        {
            return Th.Input;
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
        public void Cmd(string s) { Line(s, Th.AccentText); }
        public void Dim2(string s) { Line(s, Th.Dim); }

        public void Banner(string title)
        {
            Line("", Th.LogFg);
            Line("┃ " + title + "  " + DateTime.Now.ToString("HH:mm:ss"), Th.AccentText);
        }
    }

    // ============================ 进程运行器 ============================
    class ProcRunner
    {
        Process _p;
        public event Action<string> Out;
        public event Action<int> Exited;
        public event Action<string> ErrOut;

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
            Start(workDir, command, env, null);
        }

        public void Start(string workDir, string command, Dictionary<string, string> env, Action<string> stderrSink)
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
            if (!Directory.Exists(dir)) return;
            // 首选：Shell.Application COM（Out-of-Process 服务就是桌面 explorer）——
            // 由已运行的 shell 进程自己打开目录窗口，本进程完全不新建 explorer.exe。
            // 受限环境下手动拉起的 explorer 子进程会 0xc0000142 初始化失败，此通道可绕开。
            try
            {
                Type t = Type.GetTypeFromProgID("Shell.Application");
                if (t != null)
                {
                    object shell = Activator.CreateInstance(t);
                    t.InvokeMember("Open", System.Reflection.BindingFlags.InvokeMethod,
                        null, shell, new object[] { dir });
                    return;
                }
            }
            catch (Exception) { }
            // 次选：ShellExecuteEx 打开目录路径
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = dir;
                psi.UseShellExecute = true;
                Process.Start(psi);
                return;
            }
            catch (Exception) { }
            // 末选：显式拉起 explorer.exe，并把错误暴露出来（不再静默吞掉）
            try
            {
                Process.Start("explorer.exe", "\"" + dir + "\"");
            }
            catch (Exception ex)
            {
                try { MessageBox.Show("打开目录失败：\n" + dir + "\n\n" + ex.Message, "提示"); } catch (Exception) { }
            }
        }
    }

    // ============================ 端口监听探测（异步） ============================
    // netstat 有开销，放线程池跑；结果回调用后台线程触发，调用方自行 BeginInvoke 到 UI
    class PortWatcher
    {
        public event Action<List<Sys.PortOwner>, int> Result;
        volatile bool _busy;

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
