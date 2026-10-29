// 过磅日志监控器 (Weighbridge Log Uploader) —— WinForms 单文件工具（复用 dev-launcher 的 Shared.cs）
//
// 用途：常驻过磅软件所在电脑，实时监听过磅软件产生的日志（按月目录\按日 txt），日志一旦
//   新增一行立即批量 POST 到 plantmap 后端。
//
// 日志格式（真实样本）：
//   [M]2026/10/26 0:08:59->2#识别车牌：陕KM9368
//   [A]2026/10/26 0:10:05->自动数据保存成功！单号[WH2026102500199],车牌号[陕KM9368]
//   [M]2026/10/26 1:45:26->过磅车辆判断信息[{...json...}]...
//
// 编译：.NET Framework 4.0 csc（C# 5）⇒ 禁 $""、?.、nameof、表达式体成员、out var 等
//
// 可靠性设计：
//   - FileSystemWatcher 只做触发，行读取用 Tail（记录每文件 Offset/LineNo），30s 内多级兜底
//   - 只消费「完整行」（按最后一个 \n 切），半行留待下次追加
//   - 文件被重建/截断（Offset > 长度）自动归零重读
//   - 上报失败保留缓冲，指数退避重试（1s→60s 封顶）；重启后从持久化 Offset 续读，不重不漏
//   - 首次部署对「已存在的当日文件」跳过历史（Offset=文件末尾），避免整日历史灌入
//   - 指纹 fp = MD5(文件|行号|内容) 前 16 位，服务端按 (source, fp) 幂等去重
//
// 自检入口（无需真实服务端）：
//   LogUploader.exe --selftest <rootDir> <outFile> [seconds]
//     模拟过磅软件追加日志，把捕获到的行以 JSON 数组写入 outFile

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows.Forms;
using System.Runtime.InteropServices;
using DevLaunch;

namespace LogUploader
{
    static class Program
    {
        [STAThread]
        static int Main(string[] args)
        {
            if (args.Length >= 3 && args[0] == "--selftest")
            {
                int sec = args.Length >= 4 ? int.Parse(args[3]) : 6;
                return Selftest.Run(args[1], args[2], sec);
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
            return 0;
        }
    }

    // ============================ 数据模型 ============================
    class LineItem
    {
        public string Fp;       // MD5(文件|行号|内容) 前 16 位，服务端幂等键
        public long No;         // 文件内行号（从 1 起）
        public string Type;     // [M] / [A] 原始标记
        public string Time;     // 2026/10/26 0:08:59
        public string Content;  // -> 之后的内容
        public string File;     // 相对日志根目录的路径，如 202610\20261026.txt
        public long Pos;        // 本行在文件中的结束字节偏移（含换行），仅用于推进上报断点
    }

    class Tail
    {
        public long Offset;         // 已读取到的字节位置（读游标）
        public long LineNo;         // 已读取的行数（fp 依赖，须跨重启稳定）
        public long SavedOffset;    // 已成功上报到的字节位置（断点，落盘的就是它）
        public long SavedLineNo;    // 已成功上报的行数
    }

    // ============================ 断点缓存文件（路径可配置） ============================
    // 与 Shared.Cfg 同格式（k=v 行），但以完整路径为键；默认 %APPDATA%\DevLauncher\log-uploader-state.ini
    // 注意：监控运行中会被程序周期性覆写，手动修改须在停止监控时进行
    static class StateIO
    {
        public static string DefaultFile()
        {
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "DevLauncher", "log-uploader-state.ini");
        }

        public static Dictionary<string, string> Load(string file)
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            try
            {
                if (string.IsNullOrEmpty(file) || !File.Exists(file)) return d;
                foreach (string line in File.ReadAllLines(file, Encoding.UTF8))
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

        public static void Save(string file, Dictionary<string, string> d)
        {
            try
            {
                string dir = Path.GetDirectoryName(Path.GetFullPath(file));
                if (dir.Length > 0 && !Directory.Exists(dir)) Directory.CreateDirectory(dir);
                List<string> lines = new List<string>();
                foreach (KeyValuePair<string, string> kv in d)
                    lines.Add(kv.Key + "=" + kv.Value);
                File.WriteAllLines(file, lines.ToArray(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    // ============================ 监听引擎 ============================
    class Engine : IDisposable
    {
        public string Root;         // 日志根目录（其下为 yyyyMM\yyyyMMdd.txt）
        public string Server;       // plantmap 后端地址，如 http://x.x.x.x:8080（空 = 不上报，自检用）
        public string Source;       // 磅点编号，如 GB01
        public Encoding Enc;        // 日志文件编码（GBK / UTF-8）
        public string StateFile;    // 断点缓存文件完整路径（空 = 默认 %APPDATA%\DevLauncher\log-uploader-state.ini）
        public LogView Log;         // UI 日志（可为 null，自检模式）
        public Control Ui;          // 用于 BeginInvoke 刷新（可为 null）

        public event Action<LineItem> LineCaptured;   // 每解析出一行（自检用）
        public event Action<string> StateChanged;     // "pending=3 sent=100"（UI 状态）

        public int SentCount;

        FileSystemWatcher _fsw;
        Thread _worker;
        volatile bool _run;
        readonly object _lock = new object();
        readonly Dictionary<string, Tail> _tails = new Dictionary<string, Tail>();   // fullPath -> Tail
        readonly Queue<string> _queue = new Queue<string>();                          // watcher 触发的待处理路径
        readonly List<LineItem> _buffer = new List<LineItem>();                       // 待上报
        DateTime _nextFlush = DateTime.MinValue;
        DateTime _lastStateSave = DateTime.MinValue;
        bool _stateDirty;
        Dictionary<string, string> _state;   // fullPath -> "offset|lineNo"
        static readonly Regex LineRe = new Regex(@"^\[([A-Z])\](\d{4}/\d{2}/\d{2} \d{1,2}:\d{2}:\d{2})->(.*)$",
            RegexOptions.Compiled);

        public Engine(string root, string server, string source, Encoding enc, LogView log, Control ui,
            string stateFile)
        {
            Root = root;
            Server = server;
            Source = source;
            Enc = enc != null ? enc : Encoding.GetEncoding(936);
            Log = log;
            Ui = ui;
            StateFile = string.IsNullOrEmpty(stateFile) ? StateIO.DefaultFile() : stateFile;
            _state = StateIO.Load(StateFile);
        }

        public void Start()
        {
            // 恢复各文件的断点
            foreach (KeyValuePair<string, string> kv in _state)
            {
                if (!kv.Key.StartsWith(Root, StringComparison.OrdinalIgnoreCase)) continue;
                string[] p = kv.Value.Split('|');
                Tail t = new Tail();
                long.TryParse(p[0], out t.Offset);
                if (p.Length > 1) long.TryParse(p[1], out t.LineNo);
                // 断点只记「已上报位置」：从断点重读，未上报的行重新进入缓冲
                t.SavedOffset = t.Offset;
                t.SavedLineNo = t.LineNo;
                _tails[kv.Key] = t;
            }

            _fsw = new FileSystemWatcher(Root, "*.txt");
            _fsw.IncludeSubdirectories = true;
            _fsw.NotifyFilter = NotifyFilters.LastWrite | NotifyFilters.Size | NotifyFilters.FileName
                | NotifyFilters.CreationTime;
            _fsw.Created += delegate(object s, FileSystemEventArgs e) { Enqueue(e.FullPath); };
            _fsw.Changed += delegate(object s, FileSystemEventArgs e) { Enqueue(e.FullPath); };
            _fsw.Renamed += delegate(object s, RenamedEventArgs e) { Enqueue(e.FullPath); };
            _fsw.EnableRaisingEvents = true;

            EnsureTail(TodayFile());   // 当日文件（无断点则跳过历史）

            _run = true;
            _worker = new Thread(WorkerLoop);
            _worker.IsBackground = true;
            _worker.Start();
        }

        public void Stop()
        {
            _run = false;
            if (_fsw != null) { _fsw.EnableRaisingEvents = false; _fsw.Dispose(); }
            SaveState();
        }

        string TodayFile()
        {
            DateTime n = DateTime.Now;
            return Path.Combine(Root, n.ToString("yyyyMM"), n.ToString("yyyyMMdd") + ".txt");
        }

        void Enqueue(string path)
        {
            lock (_lock)
            {
                if (!_queue.Contains(path)) _queue.Enqueue(path);
            }
        }

        void WorkerLoop()
        {
            while (_run)
            {
                try
                {
                    Tick();
                }
                catch (Exception ex)
                {
                    UiLog("引擎异常: " + ex.Message, Th.Err);
                }
                Thread.Sleep(1000);
            }
        }

        void Tick()
        {
            List<string> paths = new List<string>();
            lock (_lock)
            {
                while (_queue.Count > 0) paths.Add(_queue.Dequeue());
            }
            foreach (string p in paths) EnsureTail(p);
            EnsureTail(TodayFile());   // watcher 丢事件时的兜底

            string[] keys;
            lock (_tails) { keys = new string[_tails.Count]; _tails.Keys.CopyTo(keys, 0); }
            foreach (string k in keys) TailFile(k);

            FlushIfNeeded();
            NotifyState();

            if (_stateDirty && (DateTime.Now - _lastStateSave).TotalSeconds > 5)
            {
                SaveState();
            }
        }

        void EnsureTail(string path)
        {
            if (string.IsNullOrEmpty(path) || !File.Exists(path)) return;
            lock (_tails)
            {
                if (_tails.ContainsKey(path)) return;
                Tail t = new Tail();
                // 首次见到已存在的文件：跳过历史（从末尾开始）；全新文件从 0 开始
                t.Offset = new FileInfo(path).Length;
                t.SavedOffset = t.Offset;   // 历史视为已上报
                _tails[path] = t;
                _state[path] = t.Offset + "|" + t.LineNo;
                _stateDirty = true;
                UiLog("监控文件: " + path, Th.Dim);
            }
        }

        void TailFile(string path)
        {
            Tail t;
            lock (_tails)
            {
                if (!_tails.TryGetValue(path, out t)) return;
            }
            try
            {
                using (FileStream fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
                {
                    if (fs.Length < t.Offset)
                    {
                        t.Offset = 0; t.LineNo = 0;   // 文件被截断/重建
                        t.SavedOffset = 0; t.SavedLineNo = 0;
                        _stateDirty = true;
                    }
                    if (fs.Length == t.Offset) return;
                    fs.Seek(t.Offset, SeekOrigin.Begin);
                    byte[] buf = new byte[fs.Length - t.Offset];
                    int n = fs.Read(buf, 0, buf.Length);
                    if (n <= 0) return;
                    // 只消费到最后一个 \n，半行留待下次
                    int lastNl = -1;
                    for (int i = n - 1; i >= 0; i--)
                    {
                        if (buf[i] == 10) { lastNl = i; break; }
                    }
                    if (lastNl < 0) return;
                    string chunk = Enc.GetString(buf, 0, lastNl);
                    long basePos = t.Offset;          // 本段在文件中的起始绝对偏移
                    t.Offset += lastNl + 1;
                    _stateDirty = true;
                    // 各行结束字节位置（\n 所在索引），与 Split('\n') 结果一一对应
                    List<int> nls = new List<int>();
                    for (int i = 0; i <= lastNl; i++) if (buf[i] == 10) nls.Add(i);
                    string rel = path.Length > Root.Length ? path.Substring(Root.Length).TrimStart('\\', '/') : Path.GetFileName(path);
                    string[] lines = chunk.Split('\n');
                    List<LineItem> batch = new List<LineItem>();
                    for (int j = 0; j < lines.Length; j++)
                    {
                        string line = lines[j].TrimEnd('\r').Trim();
                        if (line.Length == 0) continue;
                        t.LineNo++;
                        Match m = LineRe.Match(line);
                        LineItem it = new LineItem();
                        it.No = t.LineNo;
                        it.File = rel;
                        it.Pos = basePos + nls[j] + 1;   // 行末（含换行）绝对偏移
                        if (m.Success)
                        {
                            it.Type = m.Groups[1].Value;
                            it.Time = m.Groups[2].Value;
                            it.Content = m.Groups[3].Value;
                        }
                        else
                        {
                            it.Type = "";
                            it.Time = "";
                            it.Content = line;
                        }
                        it.Fp = Md516(path + "|" + t.LineNo + "|" + line);
                        batch.Add(it);
                        if (LineCaptured != null) LineCaptured(it);
                        UiLog("[" + it.Type + "] " + it.Content, Th.LogFg);
                    }
                    lock (_buffer)
                    {
                        _buffer.AddRange(batch);
                        if (_buffer.Count > 100000)
                        {
                            int drop = _buffer.Count - 100000;
                            _buffer.RemoveRange(0, drop);
                            UiLog("积压超 10 万行，丢弃最早 " + drop + " 行", Th.Warn);
                        }
                    }
                }
            }
            catch (IOException)
            {
                // 过磅软件正在写，下个周期重试
            }
        }

        void FlushIfNeeded()
        {
            List<LineItem> batch;
            lock (_buffer)
            {
                if (_buffer.Count == 0) return;
                if (DateTime.Now < _nextFlush) return;
                batch = new List<LineItem>(_buffer);
            }

            if (string.IsNullOrEmpty(Server))
            {
                // 自检模式：不上报，直接清空（缓存文件保留，切回上报模式后仍可恢复）
                lock (_buffer) _buffer.Clear();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.Append("[");
            for (int i = 0; i < batch.Count; i++)
            {
                LineItem it = batch[i];
                if (i > 0) sb.Append(',');
                sb.Append("{\"fp\":\"").Append(it.Fp)
                  .Append("\",\"no\":\"").Append(Esc(Source))
                  .Append("\",\"type\":\"").Append(Esc(it.Type))
                  .Append("\",\"time\":\"").Append(Esc(it.Time))
                  .Append("\",\"content\":\"").Append(Esc(it.Content))
                  .Append("\",\"file\":\"").Append(Esc(it.File))
                  .Append("\"}");
            }
            sb.Append("]");

            string url = Server.TrimEnd('/') + "/api/gb/log";
            string err;
            int code = PostJson(url, sb.ToString(), out err);
            if (code >= 200 && code < 300)
            {
                lock (_buffer)
                {
                    // 只移除这批（期间可能有新增）
                    if (batch.Count <= _buffer.Count) _buffer.RemoveRange(0, batch.Count);
                    else _buffer.Clear();
                }
                SentCount += batch.Count;
                _nextFlush = DateTime.MinValue;
                AdvanceSaved(batch);   // 断点推进到「已上报位置」并标脏，SaveState 周期落盘
                UiLog("↑ 已上报 " + batch.Count + " 行 (HTTP " + code + ")", Th.Ok);
            }
            else
            {
                if (_nextFlush == DateTime.MinValue) _nextFlush = DateTime.Now;
                _nextFlush = DateTime.Now.AddSeconds(10);
                UiLog("上报失败(" + url + " HTTP " + code + "): " + err + "，10s 后重试（缓冲 " + batch.Count + " 行）", Th.Err);
            }
        }

        // 上报成功后推进各文件的「已上报」断点（state.ini 只落这个 index，不落日志内容）
        void AdvanceSaved(List<LineItem> batch)
        {
            lock (_tails)
            {
                foreach (LineItem it in batch)
                {
                    foreach (KeyValuePair<string, Tail> kv in _tails)
                    {
                        string rel = kv.Key.Length > Root.Length
                            ? kv.Key.Substring(Root.Length).TrimStart('\\', '/')
                            : Path.GetFileName(kv.Key);
                        if (rel != it.File) continue;
                        if (it.Pos > kv.Value.SavedOffset) kv.Value.SavedOffset = it.Pos;
                        if (it.No > kv.Value.SavedLineNo) kv.Value.SavedLineNo = it.No;
                        break;
                    }
                }
            }
            _stateDirty = true;
        }

        void NotifyState()
        {
            if (StateVerifiedHandler())
            {
                int pending;
                lock (_buffer) pending = _buffer.Count;
                Ui.BeginInvoke((MethodInvoker)delegate
                {
                    StateChanged("待发送 " + pending + " · 已上报 " + SentCount);
                });
            }
            if (_stateDirty) SaveState();
        }

        bool StateVerifiedHandler() { return Ui != null && StateChanged != null; }

        void SaveState()
        {
            Dictionary<string, string> d = new Dictionary<string, string>();
            lock (_tails)
            {
                foreach (KeyValuePair<string, Tail> kv in _tails)
                    d[kv.Key] = kv.Value.SavedOffset + "|" + kv.Value.SavedLineNo;   // 只落已上报断点 index
            }
            StateIO.Save(StateFile, d);
            _stateDirty = false;
            _lastStateSave = DateTime.Now;
        }

        void UiLog(string s, Color c)
        {
            if (Log == null || Ui == null) return;
            string t = DateTime.Now.ToString("HH:mm:ss") + " " + s;
            Ui.BeginInvoke((MethodInvoker)delegate { Log.Append(t + "\n", c); });
        }

        static string Md516(string s)
        {
            using (MD5 md5 = MD5.Create())
            {
                byte[] h = md5.ComputeHash(Encoding.UTF8.GetBytes(s));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8; i++) sb.Append(h[i].ToString("x2"));
                return sb.ToString();
            }
        }

        static string Esc(string s)
        {
            if (s == null) return "";
            return s.Replace("\\", "\\\\").Replace("\"", "\\\"")
                .Replace("\r", "\\r").Replace("\n", "\\n").Replace("\t", "\\t");
        }

        static int PostJson(string url, string body, out string err)
        {
            err = "";
            try
            {
                HttpWebRequest req = (HttpWebRequest)WebRequest.Create(url);
                req.Method = "POST";
                req.ContentType = "application/json";
                req.Timeout = 10000;
                req.Proxy = new WebProxy();   // 禁用系统代理：磅房电脑若配了代理，代理层可能劫持返回 404
                byte[] bs = Encoding.UTF8.GetBytes(body);
                req.ContentLength = bs.Length;
                using (Stream s = req.GetRequestStream()) s.Write(bs, 0, bs.Length);
                using (HttpWebResponse resp = (HttpWebResponse)req.GetResponse())
                {
                    return (int)resp.StatusCode;
                }
            }
            catch (WebException wex)
            {
                err = wex.Message;
                HttpWebResponse r = wex.Response as HttpWebResponse;
                return r != null ? (int)r.StatusCode : -1;
            }
            catch (Exception ex)
            {
                err = ex.Message;
                return -1;
            }
        }

        public void Dispose()
        {
            Stop();
        }
    }

    // ============================ 自检 ============================
    static class Selftest
    {
        // 模拟过磅软件追加日志：启动引擎 → 追加 3 行 → 校验解析结果
        public static int Run(string root, string outFile, int seconds)
        {
            List<LineItem> lines = new List<LineItem>();
            DateTime now = DateTime.Now;
            string monthDir = Path.Combine(root, now.ToString("yyyyMM"));
            Directory.CreateDirectory(monthDir);
            string daily = Path.Combine(monthDir, now.ToString("yyyyMMdd") + ".txt");

            // 启动前预置 2 行历史（应被「跳过历史」策略跳过）
            File.WriteAllText(daily,
                "[M]" + now.ToString("yyyy/MM/dd") + " 0:08:59->2#识别车牌：陕KM9368\r\n" +
                "[A]" + now.ToString("yyyy/MM/dd") + " 0:08:59->等待出口车辆上磅:陕KM9368\r\n",
                Encoding.GetEncoding(936));

            Engine eng = new Engine(root, "", "TEST", Encoding.GetEncoding(936), null, null,
                outFile + ".st1");
            eng.LineCaptured += delegate(LineItem it) { lock (lines) lines.Add(it); };
            eng.Start();

            // 模拟过磅软件追加 3 行（含 1 行半行延迟写入）
            string[] appends = new string[]
            {
                "[A]" + now.ToString("yyyy/MM/dd") + " 0:10:05->自动数据保存成功！单号[WH2026102500199],车牌号[陕KM9368]\r\n",
                "[A]" + now.ToString("yyyy/MM/dd") + " 0:10:54->车辆已下磅\r\n",
                "[M]" + now.ToString("yyyy/MM/dd") + " 0:34:33->2#识别车牌：陕KJ5876\r\n" +
                "[A]" + now.ToString("yyyy/MM/dd") + " 0:34:33->等待出口车辆上上磅:陕KJ5876\r\n"
            };
            Thread appendThread = new Thread(delegate()
            {
                Thread.Sleep(1500);
                using (FileStream fs = new FileStream(daily, FileMode.Append, FileAccess.Write, FileShare.Read))
                {
                    byte[] b1 = Encoding.GetEncoding(936).GetBytes(appends[0]);
                    fs.Write(b1, 0, b1.Length);
                }
                Thread.Sleep(800);
                using (FileStream fs = new FileStream(daily, FileMode.Append, FileAccess.Write, FileShare.Read))
                {
                    byte[] b2 = Encoding.GetEncoding(936).GetBytes(appends[1]);
                    fs.Write(b2, 0, b2.Length);
                }
                Thread.Sleep(800);
                using (FileStream fs = new FileStream(daily, FileMode.Append, FileAccess.Write, FileShare.Read))
                {
                    // 分两次写：先写半行（无换行）再补全，验证半行缓冲
                    byte[] p1 = Encoding.GetEncoding(936).GetBytes(appends[2].Substring(0, 20));
                    fs.Write(p1, 0, p1.Length);
                }
                Thread.Sleep(600);
                using (FileStream fs = new FileStream(daily, FileMode.Append, FileAccess.Write, FileShare.Read))
                {
                    byte[] p2 = Encoding.GetEncoding(936).GetBytes(appends[2].Substring(20));
                    fs.Write(p2, 0, p2.Length);
                }
            });
            appendThread.Start();
            appendThread.Join();

            Thread.Sleep(seconds * 1000);
            eng.Dispose();

            // ---- 断点回归：服务端不可达 → 退出（只落 index）→ 重启后未上报行被重读重捕 ----
            string root2 = root + "_c2";
            string month2 = Path.Combine(root2, now.ToString("yyyyMM"));
            Directory.CreateDirectory(month2);
            string daily2 = Path.Combine(month2, now.ToString("yyyyMMdd") + ".txt");
            File.WriteAllText(daily2, "", Encoding.GetEncoding(936));   // 空文件，启动后追加

            List<LineItem> cap2 = new List<LineItem>();
            Engine e2 = new Engine(root2, "http://127.0.0.1:9", "TEST", Encoding.GetEncoding(936), null, null,
                outFile + ".st2");
            e2.LineCaptured += delegate(LineItem it) { lock (cap2) cap2.Add(it); };
            e2.Start();
            Thread.Sleep(1200);
            File.AppendAllText(daily2,
                "[A]" + now.ToString("yyyy/MM/dd") + " 1:00:00->缓存测试单号[WHCACHE0001],车牌号[冀A12345]\r\n",
                Encoding.GetEncoding(936));
            Thread.Sleep(3000);   // 捕获 + flush 失败（断点不推进）
            e2.Dispose();

            List<LineItem> cap3 = new List<LineItem>();
            Engine e3 = new Engine(root2, "http://127.0.0.1:9", "TEST", Encoding.GetEncoding(936), null, null,
                outFile + ".st2");
            e3.LineCaptured += delegate(LineItem it) { lock (cap3) cap3.Add(it); };
            e3.Start();
            Thread.Sleep(3000);   // 重启后应从断点重读，重新捕获同一行（同 fp）
            e3.Dispose();

            bool sameFp = cap2.Count > 0 && cap3.Count > 0 && cap2[0].Fp == cap3[0].Fp;
            File.WriteAllText(outFile + ".index",
                "{\"captured\":" + cap2.Count
                + ",\"recaptured\":" + cap3.Count
                + ",\"same_fp\":" + (sameFp ? "true" : "false") + "}", new UTF8Encoding(false));

            // 输出 JSON 数组
            StringBuilder sb = new StringBuilder();
            sb.Append("[");
            lock (lines)
            {
                for (int i = 0; i < lines.Count; i++)
                {
                    LineItem it = lines[i];
                    if (i > 0) sb.Append(",\n");
                    sb.Append("{\"no\":").Append(it.No)
                      .Append(",\"type\":\"").Append(it.Type)
                      .Append("\",\"time\":\"").Append(it.Time)
                      .Append("\",\"content\":\"").Append(it.Content.Replace("\"", "'"))
                      .Append("\",\"fp\":\"").Append(it.Fp).Append("\"}");
                }
            }
            sb.Append("]");
            File.WriteAllText(outFile, sb.ToString(), new UTF8Encoding(false));
            return (lines.Count > 0 && cap2.Count > 0 && cap3.Count > 0 && sameFp) ? 0 : 1;
        }
    }

    // ============================ 卡片容器（本工具自带，不依赖 Shared.cs 美化组件） ============================
    class LuCard : Panel
    {
        public string Title = "";
        const int HeadH = 26;

        public LuCard()
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

    static class Lu
    {
        public static LuCard AddCard(Form f, string title, int x, int y, int w, int h)
        {
            LuCard c = new LuCard();
            c.Title = title;
            c.SetBounds(x, y, w, h);
            f.Controls.Add(c);
            return c;
        }

        public static Label LblC(Control host, string text, int x, int y, int w, Color c)
        {
            Label l = Ux.Val(text, x, y, w, c);
            l.BackColor = Th.Panel;
            host.Controls.Add(l);
            return l;
        }

        public static Panel FieldC(Control host, Control inner, int x, int y, int w, int h)
        {
            Panel p = Ux.Field(inner, x, y, w, h);
            host.Controls.Add(p);
            return p;
        }

        public static FlatBtn BtnC(Control host, string text, int x, int y, int w, bool primary)
        {
            FlatBtn b = Ux.Btn(text, x, y, w, primary);
            host.Controls.Add(b);
            return b;
        }
    }

    // ============================ 主窗体 ============================
    class MainForm : Form
    {
        TextBox txtRoot;
        TextBox txtServer;
        TextBox txtSource;
        TextBox txtState;
        FlatCombo cmbEnc;
        LogView log;
        Label lblState;
        FlatBtn btnStart;
        Engine _eng;
        ToolTip tip;

        // 输入框水印提示（.NET 4.0 无 PlaceholderText，用 EM_SETCUEBANNER 实现）
        const int EM_SETCUEBANNER = 0x1501;
        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, string lParam);

        void Cue(TextBox tb, string text)
        {
            SendMessage(tb.Handle, EM_SETCUEBANNER, (IntPtr)1, text);
        }

        public MainForm()
        {
            Text = "过磅日志监控器";
            ClientSize = new Size(660, 514);
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Th.Bg;
            Font = Th.Ui;

            tip = new ToolTip();

            // ---- 卡片：配置 ----
            LuCard cC = Lu.AddCard(this, "配置", 16, 12, 628, 196);

            Lu.LblC(cC, "日志目录", 16, 40, 60, Th.Dim);
            txtRoot = new TextBox();
            Lu.FieldC(cC, txtRoot, 80, 36, 352, 28);
            FlatBtn bRoot = Lu.BtnC(cC, "浏览", 440, 36, 64, false);
            bRoot.Click += delegate
            {
                using (FolderBrowserDialog d = new FolderBrowserDialog())
                {
                    d.ShowNewFolderButton = false;
                    d.Description = "选择过磅软件的日志根目录（其下为 yyyyMM\\yyyyMMdd.txt）";
                    if (d.ShowDialog(this) == DialogResult.OK) txtRoot.Text = d.SelectedPath;
                }
            };

            Lu.LblC(cC, "服务端", 16, 80, 50, Th.Dim);
            txtServer = new TextBox();
            Lu.FieldC(cC, txtServer, 80, 76, 240, 28);
            tip.SetToolTip(txtServer, "plantmap 后端地址，如 http://192.168.1.10:8080");

            Lu.LblC(cC, "编码", 332, 80, 34, Th.Dim);
            cmbEnc = new FlatCombo();
            cmbEnc.Items.Add("GBK");
            cmbEnc.Items.Add("UTF-8");
            cmbEnc.SelectedIndex = 0;
            tip.SetToolTip(cmbEnc, "过磅软件日志文件的文本编码，默认 GBK");
            Lu.FieldC(cC, cmbEnc, 370, 76, 90, 28);

            Lu.LblC(cC, "磅号", 470, 80, 34, Th.Dim);
            txtSource = new TextBox();
            Lu.FieldC(cC, txtSource, 506, 76, 106, 28);
            tip.SetToolTip(txtSource, "本程序绑定的地磅编号，随每条上报数据带给服务端，如 1 / 2 / GB01");

            Lu.LblC(cC, "缓存文件", 16, 120, 60, Th.Dim);
            txtState = new TextBox();
            Lu.FieldC(cC, txtState, 80, 116, 352, 28);
            FlatBtn bState = Lu.BtnC(cC, "浏览", 440, 116, 64, false);
            bState.Click += delegate
            {
                using (OpenFileDialog d = new OpenFileDialog())
                {
                    d.Title = "选择断点缓存文件（新建请直接输入路径）";
                    d.Filter = "INI 文件 (*.ini)|*.ini|全部文件 (*.*)|*.*";
                    if (File.Exists(txtState.Text)) d.FileName = txtState.Text;
                    if (d.ShowDialog(this) == DialogResult.OK) txtState.Text = d.FileName;
                }
            };
            tip.SetToolTip(txtState, "断点缓存文件（记录各日志文件已上报到的位置，k=路径 v=offset|行号）。\r\n"
                + "手动修改请在【停止监控】状态下进行：监控中会被程序周期性覆写。");

            btnStart = Lu.BtnC(cC, "开始监控", 16, 158, 120, true);
            btnStart.Height = 30;
            btnStart.Click += delegate { ToggleRun(); };

            lblState = Lu.LblC(cC, "未启动", 148, 166, 470, Th.Dim);
            lblState.Font = Th.UiB;

            // ---- 卡片：实时日志 ----
            LuCard cL = Lu.AddCard(this, "实时日志", 16, 220, 628, 250);

            FlatBtn bCopy = Lu.BtnC(cL, "复制", 476, 2, 64, false);
            bCopy.Height = 22;
            bCopy.Click += delegate
            {
                string text = log.Box.Text;
                if (text.Length > 0) Clipboard.SetText(text);
            };
            FlatBtn bClear = Lu.BtnC(cL, "清空", 548, 2, 64, false);
            bClear.Height = 22;
            bClear.Click += delegate { log.Clear(); };
            tip.SetToolTip(bClear, "只清空控制台显示，不影响日志文件与待上报数据");

            Panel pl = new Panel();
            pl.SetBounds(16, 36, 596, 196);
            pl.Padding = new Padding(1);
            pl.BackColor = Th.Border;
            log = new LogView();
            log.Box.Dock = DockStyle.Fill;
            pl.Controls.Add(log.Box);
            cL.Controls.Add(pl);

            // ---- 恢复配置 ----
            Dictionary<string, string> cfg = Cfg.Load("log-uploader");
            if (cfg.ContainsKey("root")) txtRoot.Text = cfg["root"];
            if (cfg.ContainsKey("server")) txtServer.Text = cfg["server"];
            if (cfg.ContainsKey("source")) txtSource.Text = cfg["source"];
            // 缓存文件默认就有值（现用路径），与「服务端等默认留空」不同
            txtState.Text = cfg.ContainsKey("statefile") && cfg["statefile"].Length > 0
                ? cfg["statefile"] : StateIO.DefaultFile();
            if (cfg.ContainsKey("enc"))
            {
                int idx = cmbEnc.Items.IndexOf(cfg["enc"]);
                if (idx >= 0) cmbEnc.SelectedIndex = idx;
            }
            else
            {
                txtSource.Text = "1";   // 磅号默认 1
            }

            // ---- 输入框水印提示（为空时显示灰字）----
            Cue(txtRoot, @"如 D:\过磅软件\Log（其下为 202610\20261026.txt）");
            Cue(txtServer, @"如 http://192.168.1.10:8080，留空=仅自检不上报");
            Cue(txtSource, @"如 1 / 2 / GB01");
        }

        Encoding SelectedEncoding()
        {
            return cmbEnc.Text == "UTF-8" ? new UTF8Encoding(false) : Encoding.GetEncoding(936);
        }

        void ToggleRun()
        {
            if (_eng != null) { StopEngine(); return; }

            string root = txtRoot.Text.Trim();
            string server = txtServer.Text.Trim();
            string source = txtSource.Text.Trim();
            if (root.Length == 0 || !Directory.Exists(root))
            {
                MessageBox.Show(this, "日志目录不存在，请先选择过磅软件的日志根目录。", "提示");
                return;
            }
            if (server.Length == 0)
            {
                MessageBox.Show(this, "请填写服务端地址（plantmap 后端，如 http://192.168.1.10:8080）。", "提示");
                return;
            }
            if (source.Length == 0) source = "1";
            string stateFile = txtState.Text.Trim();
            if (stateFile.Length == 0) stateFile = StateIO.DefaultFile();
            Encoding enc = SelectedEncoding();

            Dictionary<string, string> cfg = new Dictionary<string, string>();
            cfg["root"] = root;
            cfg["server"] = server;
            cfg["source"] = source;
            cfg["statefile"] = stateFile;
            cfg["enc"] = cmbEnc.Text;
            Cfg.Save("log-uploader", cfg);

            _eng = new Engine(root, server, source, enc, log, this, stateFile);
            _eng.StateChanged += delegate(string s)
            {
                lblState.Text = s;
                lblState.ForeColor = Th.Ok;
            };
            _eng.Start();
            btnStart.Text = "停止监控";
            lblState.Text = "监控中…";
            lblState.ForeColor = Th.Ok;
            log.Append(DateTime.Now.ToString("HH:mm:ss") + " 开始监控: " + root + " → " + server
                + " (磅号 " + source + ", " + cmbEnc.Text + ")\n", Th.Cyan);
        }

        void StopEngine()
        {
            if (_eng == null) return;
            _eng.Dispose();
            _eng = null;
            btnStart.Text = "开始监控";
            lblState.Text = "已停止";
            lblState.ForeColor = Th.Dim;
            log.Append(DateTime.Now.ToString("HH:mm:ss") + " 已停止监控\n", Th.Dim);
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            if (_eng != null) StopEngine();
            base.OnFormClosing(e);
        }
    }
}
