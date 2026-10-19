using System;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;

// 临时工具：启动一个 exe，截图其主窗口后关闭。用法：
//   caps.exe <exe路径> <工作目录> <输出png> <等待秒> [宽] [高]
static class Cap
{
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc cb, IntPtr p);
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] static extern bool GetWindowRect(IntPtr h, out RECT r);
    [DllImport("user32.dll")] static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
    [DllImport("user32.dll")] static extern bool ShowWindow(IntPtr h, int cmd);
    [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] static extern bool MoveWindow(IntPtr h, int x, int y, int w, int ht, bool repaint);
    [DllImport("user32.dll")] static extern IntPtr PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
    delegate bool EnumProc(IntPtr h, IntPtr l);

    struct RECT { public int Left, Top, Right, Bottom; }

    static void Main(string[] a)
    {
        string exe = a[0], wd = a[1], outp = a[2];
        double wait = double.Parse(a[3], System.Globalization.CultureInfo.InvariantCulture);
        int wantW = a.Length > 4 ? int.Parse(a[4]) : 0;
        int wantH = a.Length > 5 ? int.Parse(a[5]) : 0;

        ProcessStartInfo psi = new ProcessStartInfo(exe);
        psi.WorkingDirectory = wd;
        psi.UseShellExecute = true;
        Process p = Process.Start(psi);
        Thread.Sleep((int)(wait * 1000));

        IntPtr found = IntPtr.Zero;
        EnumWindows(delegate(IntPtr h, IntPtr l)
        {
            uint pid; GetWindowThreadProcessId(h, out pid);
            if (pid == p.Id && IsWindowVisible(h)) { found = h; return false; }
            return true;
        }, IntPtr.Zero);
        if (found == IntPtr.Zero) { Console.WriteLine("no window"); try { p.Kill(); } catch {} return; }

        if (wantW > 0) MoveWindow(found, 40, 40, wantW, wantH, true);
        ShowWindow(found, 9);
        SetForegroundWindow(found);
        Thread.Sleep(700);

        RECT r; GetWindowRect(found, out r);
        int w = r.Right - r.Left, h2 = r.Bottom - r.Top;
        using (Bitmap bmp = new Bitmap(w, h2))
        {
            using (Graphics g = Graphics.FromImage(bmp))
            {
                IntPtr hdc = g.GetHdc();
                PrintWindow(found, hdc, 0);
                g.ReleaseHdc(hdc);
            }
            bmp.Save(outp, ImageFormat.Png);
        }
        Console.WriteLine("saved " + outp + " " + w + "x" + h2);

        PostMessage(found, 0x0010, IntPtr.Zero, IntPtr.Zero);
        Thread.Sleep(1500);
        try { if (!p.HasExited) { Process.Start(new ProcessStartInfo("taskkill.exe", "/F /T /PID " + p.Id) { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true }).WaitForExit(5000); } }
        catch (Exception) { }
    }
}
