// Spring Boot 一键启动器
// 编译：csc /target:winexe /out:SpringBootLauncher.exe Shared.cs SpringBootLauncher.cs
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
    class BootInfo
    {
        public string PickedDir = "";
        public string RootDir = "";
        public string RootPom = "";
        public bool Multi;
        public string LaunchModule = "";
        public string LaunchDir = "";
        public List<string> ModuleCandidates = new List<string>();
        public List<string> Profiles = new List<string>();
        public string Mvn = "";
        public string Wrapper = "";
        public string JavaHome = "";
        public string JavaVer = "";
        public string WantJava = "";
        public int Port;
        public bool NeedInstall;
        public bool Ok;
        public string Error = "";
        public string Summary = "";
        public string Tool = "maven";
        public string Repo = "";          // 用户显式配置的本地仓库（空 = 跟随 settings.xml）
        public string SettingsFile = "";  // 用户显式配置的 settings.xml（空 = Maven 默认）
    }

    // ==================== 工具链配置（JDK / Maven / 仓库 / settings）====================
    // 四项都可为空：为空的项回落到自动探测（环境变量 / PATH / settings.xml）
    class ToolChain
    {
        public string Jdk = "";
        public string MavenDir = "";
        public string Repo = "";
        public string SettingsFile = "";

        public static ToolChain Current;   // BootForm 装填后，Boot.Detect / 命令拼装统一采用

        public static string AutoJdk()
        {
            string v = Environment.GetEnvironmentVariable("JAVA_HOME");
            return string.IsNullOrEmpty(v) ? "" : v;
        }

        public static string AutoMavenDir()
        {
            string mh = Environment.GetEnvironmentVariable("M2_HOME");
            if (string.IsNullOrEmpty(mh)) mh = Environment.GetEnvironmentVariable("MAVEN_HOME");
            if (!string.IsNullOrEmpty(mh) && File.Exists(Path.Combine(mh, "bin", "mvn.cmd"))) return mh;
            string mvn = Sys.Which("mvn");
            if (mvn != null)
            {
                try
                {
                    DirectoryInfo bin = Directory.GetParent(mvn);        // ...\bin\mvn.cmd -> ...\bin
                    if (bin != null)
                    {
                        DirectoryInfo home = bin.Parent;                 // ...\bin -> Maven 安装目录
                        if (home != null && File.Exists(Path.Combine(home.FullName, "bin", "mvn.cmd")))
                            return home.FullName;
                    }
                }
                catch (Exception) { }
            }
            return "";
        }

        public static string AutoSettings()
        {
            string home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            List<string> c = new List<string>();
            c.Add(Path.Combine(home, ".m2", "settings.xml"));
            string mh = Environment.GetEnvironmentVariable("M2_HOME");
            if (string.IsNullOrEmpty(mh)) mh = Environment.GetEnvironmentVariable("MAVEN_HOME");
            if (!string.IsNullOrEmpty(mh)) c.Add(Path.Combine(mh, "conf", "settings.xml"));
            foreach (string f in c) if (File.Exists(f)) return f;
            return "";
        }

        // mvn 可执行文件：用户配置的 Maven 目录优先（工程 wrapper 与 PATH 兜底）
        public string MvnCmd()
        {
            if (MavenDir.Length > 0)
            {
                string p = Path.Combine(MavenDir, "bin", "mvn.cmd");
                if (File.Exists(p)) return p;
            }
            return "";
        }
    }

    static class Boot
    {
        static readonly HashSet<string> Skip = new HashSet<string>(
            new string[] { "target", "node_modules", ".git", ".idea", "dist", "build", ".mvn", ".workbuddy", "out", "bin", "logs", ".logs" });

        // ---------- 工具 ----------
        public static string FindUp(string dir, string file, int max)
        {
            return Sys.FindUp(dir, file, max);
        }

        static void Walk(string dir, int depth, List<string> outFiles)
        {
            if (depth > 8) return;
            try
            {
                foreach (string f in Directory.GetFiles(dir))
                {
                    string n = Path.GetFileName(f).ToLowerInvariant();
                    if (n.StartsWith("application") && (n.EndsWith(".yml") || n.EndsWith(".yaml") || n.EndsWith(".properties")))
                        outFiles.Add(f);
                }
                foreach (string d in Directory.GetDirectories(dir))
                {
                    string n = Path.GetFileName(d).ToLowerInvariant();
                    if (Skip.Contains(n)) continue;
                    Walk(d, depth + 1, outFiles);
                }
            }
            catch (Exception) { }
        }

        static int Order(string name)
        {
            string[] preset = new string[] { "dev", "test", "deploy", "prod", "local", "uat", "pre", "stage", "release" };
            for (int i = 0; i < preset.Length; i++)
                if (preset[i].Equals(name, StringComparison.OrdinalIgnoreCase)) return i;
            return 100;
        }

        static string ReadTextSafe(string path)
        {
            try { return File.ReadAllText(path); }
            catch (Exception) { return ""; }
        }

        // ---------- 主探测 ----------
        public static BootInfo Detect(string dir)
        {
            BootInfo bi = new BootInfo();
            bi.PickedDir = dir;
            if (string.IsNullOrEmpty(dir) || !Directory.Exists(dir))
            {
                bi.Error = "目录不存在";
                return bi;
            }

            string pom = FindUp(dir, "pom.xml", 6);
            if (pom == null)
            {
                string bg = FindUp(dir, "build.gradle", 6);
                string bgk = FindUp(dir, "build.gradle.kts", 6);
                if (bg != null || bgk != null)
                {
                    bi.Tool = "gradle";
                    bi.Error = "检测到 Gradle 工程，请使用「mvnw」或改用 IDEA 运行（本工具当前仅支持 Maven 命令拼装）";
                    return bi;
                }
                bi.Error = "未找到 pom.xml（请选择包含 pom.xml 的工程目录）";
                return bi;
            }

            Pom p;
            try { p = Pom.Read(pom); }
            catch (Exception ex) { bi.Error = "pom.xml 解析失败：" + ex.Message; return bi; }

            string rootDir = p.Dir;
            bool multi = p.Modules.Count > 0;

            // 选中的是子模块 → 自动提升到父工程
            if (!multi)
            {
                try
                {
                    System.Xml.XmlDocument doc = Pom.LoadXml(pom);
                    System.Xml.XmlElement par = Pom.FirstKid(doc.DocumentElement, "parent");
                    if (par != null)
                    {
                        string rel = Pom.Text(par, "relativePath");
                        if (rel == null) rel = "../pom.xml";
                        rel = rel.Trim();
                        if (rel.Length > 0)
                        {
                            string pp = Path.GetFullPath(Path.Combine(p.Dir, rel));
                            if (File.Exists(pp))
                            {
                                Pom ppom = Pom.Read(pp);
                                if (ppom.Modules.Count > 0)
                                {
                                    string self = new DirectoryInfo(p.Dir).Name;
                                    foreach (string m in ppom.Modules)
                                    {
                                        if (m.Trim().Replace('/', '\\').Equals(self, StringComparison.OrdinalIgnoreCase))
                                        {
                                            rootDir = ppom.Dir;
                                            p = ppom;
                                            multi = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception) { }
            }

            bi.RootDir = rootDir;
            bi.RootPom = Path.Combine(rootDir, "pom.xml");
            bi.Multi = multi;
            bi.WantJava = ReadJavaVersion(p);

            // 候选启动模块
            if (multi)
            {
                List<string> withPlugin = new List<string>();
                List<string> others = new List<string>();
                foreach (string m in p.Modules)
                {
                    string md = Path.Combine(rootDir, m.Replace('/', '\\'));
                    string mp = Path.Combine(md, "pom.xml");
                    if (!File.Exists(mp)) { others.Add(m); continue; }
                    if (ReadTextSafe(mp).Contains("spring-boot-maven-plugin")) withPlugin.Add(m);
                    else others.Add(m);
                }
                bi.ModuleCandidates = withPlugin.Count > 0 ? withPlugin : others;
                if (bi.ModuleCandidates.Count == 0) { bi.Error = "未在子模块中找到可启动模块"; return bi; }

                // 默认选中：用户选的目录本身优先 → 名字像启动器 → 第一个
                string pickedName = new DirectoryInfo(dir).Name;
                string pick = null;
                foreach (string m in bi.ModuleCandidates)
                {
                    string leaf = m.Trim().Replace('/', '\\');
                    int j = leaf.LastIndexOf('\\');
                    if (j >= 0) leaf = leaf.Substring(j + 1);
                    if (leaf.Equals(pickedName, StringComparison.OrdinalIgnoreCase)) { pick = m; break; }
                }
                if (pick == null)
                {
                    foreach (string m in bi.ModuleCandidates)
                    {
                        string leaf = m.ToLowerInvariant();
                        if (leaf.Contains("launcher") || leaf.Contains("app") || leaf.Contains("boot") || leaf.Contains("server") || leaf.Contains("web"))
                        { pick = m; break; }
                    }
                }
                if (pick == null) pick = bi.ModuleCandidates[0];
                bi.LaunchModule = pick;
                bi.LaunchDir = Path.Combine(rootDir, pick.Replace('/', '\\'));
            }
            else
            {
                bi.LaunchModule = "";
                bi.LaunchDir = rootDir;
                bi.ModuleCandidates.Add("");
            }

            // mvn / wrapper（用户配置的 Maven 目录优先于工程 wrapper 与 PATH）
            ToolChain t0 = ToolChain.Current;
            string tcMvn = t0 == null ? "" : t0.MvnCmd();
            if (tcMvn.Length > 0)
            {
                bi.Mvn = tcMvn;
                bi.Wrapper = "";   // 显式配置压过 mvnw
            }
            else
            {
                string w = Path.Combine(rootDir, "mvnw.cmd");
                if (File.Exists(w)) bi.Wrapper = w;
                if (bi.Wrapper.Length == 0)
                {
                    string w2 = Path.Combine(bi.LaunchDir, "mvnw.cmd");
                    if (File.Exists(w2)) bi.Wrapper = w2;
                }
                bi.Mvn = Sys.Which("mvn");
                if (bi.Mvn == null && bi.Wrapper.Length == 0)
                {
                    bi.Error = "PATH 中找不到 mvn，且工程没有 mvnw.cmd（可在「Maven 目录」里指定 Maven 安装位置）";
                    return bi;
                }
            }

            // java
            bi.JavaHome = (t0 != null && t0.Jdk.Length > 0) ? t0.Jdk : Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(bi.JavaHome)) bi.JavaVer = ReadJavaVer(bi.JavaHome);

            // profiles
            bi.Profiles = ScanProfiles(rootDir, p.ProfileIds);

            // 端口 / 依赖（仓库与 settings 只在用户显式配置时下发）
            if (t0 != null)
            {
                bi.Repo = t0.Repo;
                bi.SettingsFile = t0.SettingsFile;
            }
            bi.Port = GuessPort(bi.LaunchDir, bi.Profiles.Count > 0 ? bi.Profiles[0] : "");
            bi.NeedInstall = multi && NeedInstall(rootDir, p, bi.LaunchModule, bi.Repo);

            List<string> desc = new List<string>();
            desc.Add(multi ? "Maven 多模块（" + (p.Modules.Count) + " 个子模块）" : "Maven 单模块");
            if (multi) desc.Add("启动模块 " + bi.LaunchModule);
            desc.Add("profile " + bi.Profiles.Count + " 个");
            bi.Summary = string.Join(" · ", desc.ToArray());
            bi.Ok = true;
            return bi;
        }

        static string ReadJavaVersion(Pom p)
        {
            try
            {
                System.Xml.XmlDocument doc = Pom.LoadXml(p.Path);
                System.Xml.XmlElement props = Pom.FirstKid(doc.DocumentElement, "properties");
                if (props != null)
                {
                    string v = Pom.Text(props, "java.version");
                    if (!string.IsNullOrEmpty(v)) return v;
                    v = Pom.Text(props, "maven.compiler.source");
                    if (!string.IsNullOrEmpty(v)) return v;
                }
            }
            catch (Exception) { }
            return "";
        }

        static string ReadJavaVer(string javaHome)
        {
            try
            {
                string rel = Path.Combine(javaHome, "release");
                if (!File.Exists(rel)) return "";
                foreach (string line in File.ReadAllLines(rel))
                    if (line.StartsWith("JAVA_VERSION="))
                        return line.Substring(13).Trim().Trim('"');
            }
            catch (Exception) { }
            return "";
        }

        static List<string> ScanProfiles(string rootDir, List<string> pomProfiles)
        {
            HashSet<string> set = new HashSet<string>();
            List<string> files = new List<string>();
            Walk(rootDir, 0, files);
            Regex re = new Regex(@"^application-(.+)\.(yml|yaml|properties)$", RegexOptions.IgnoreCase);
            foreach (string f in files)
            {
                Match m = re.Match(Path.GetFileName(f));
                if (m.Success) set.Add(m.Groups[1].Value);
            }
            foreach (string s in pomProfiles) if (!string.IsNullOrEmpty(s)) set.Add(s);

            List<string> list = new List<string>(set);
            list.Sort(delegate(string a, string b)
            {
                int oa = Order(a), ob = Order(b);
                if (oa != ob) return oa.CompareTo(ob);
                return string.Compare(a, b, StringComparison.OrdinalIgnoreCase);
            });
            return list;
        }

        public static int GuessPort(string launchDir, string profile)
        {
            string baseDir = Path.Combine(launchDir, "src", "main", "resources");
            List<string> tries = new List<string>();
            if (!string.IsNullOrEmpty(profile))
            {
                tries.Add(Path.Combine(baseDir, "application-" + profile + ".yml"));
                tries.Add(Path.Combine(baseDir, "application-" + profile + ".yaml"));
                tries.Add(Path.Combine(baseDir, "application-" + profile + ".properties"));
            }
            tries.Add(Path.Combine(baseDir, "application.yml"));
            tries.Add(Path.Combine(baseDir, "application.yaml"));
            tries.Add(Path.Combine(baseDir, "application.properties"));

            foreach (string f in tries)
            {
                if (!File.Exists(f)) continue;
                int v = PortFromYml(ReadTextSafe(f));
                if (v > 0) return v;
            }
            return 0;
        }

        // 只在顶格 server: 段内取 port，避免匹配到 mongodb/redis 等同名键
        // 注意兼容 CRLF：行尾锚点用 [ \t]*\r?$
        static int PortFromYml(string text)
        {
            Match sm = Regex.Match(text, @"(?m)^server[ \t]*:[ \t]*\r?$");
            if (sm.Success)
            {
                int start = sm.Index + sm.Length;
                string tail = text.Substring(start);
                Match nx = Regex.Match(tail, @"(?m)^\S");
                int end = nx.Success ? start + nx.Index : text.Length;
                string scope = text.Substring(start, end - start);
                Match pm = Regex.Match(scope, @"(?m)^[ \t]+port[ \t]*:[ \t]*(\d{2,5})[ \t]*\r?$");
                if (pm.Success)
                {
                    int v;
                    if (int.TryParse(pm.Groups[1].Value, out v)) return v;
                }
            }
            Match pp = Regex.Match(text, @"(?m)^[ \t]*server\.port[ \t]*=[ \t]*(\d{2,5})");
            if (pp.Success)
            {
                int v;
                if (int.TryParse(pp.Groups[1].Value, out v)) return v;
            }
            return 0;
        }

        // 依赖模块是否需要在启动前 install（repoDir 为空则用自动探测的本地仓库）
        public static bool NeedInstall(string rootDir, Pom rootPom, string launchModule, string repoDir)
        {
            try
            {
                string m2 = string.IsNullOrEmpty(repoDir) ? Sys.LocalRepo() : repoDir;
                if (!Directory.Exists(m2)) return true;

                string rootGid = GroupId(rootPom);
                string rootVer = Version(rootPom);

                foreach (string m in rootPom.Modules)
                {
                    if (m.Trim().Equals(launchModule, StringComparison.OrdinalIgnoreCase)) continue;
                    string md = Path.Combine(rootDir, m.Replace('/', '\\'));
                    string mp = Path.Combine(md, "pom.xml");
                    if (!File.Exists(mp)) continue;
                    Pom sp = Pom.Read(mp);
                    string gid = string.IsNullOrEmpty(GroupId(sp)) ? rootGid : GroupId(sp);
                    string aid = sp.ArtifactId;
                    string ver = string.IsNullOrEmpty(sp.Version) || sp.Version.IndexOf("$") >= 0 ? rootVer : sp.Version;
                    if (string.IsNullOrEmpty(gid) || string.IsNullOrEmpty(aid) || string.IsNullOrEmpty(ver)) return true;

                    string jarDir = Path.Combine(m2, gid.Replace('.', '\\'), aid, ver);
                    string jar = Path.Combine(jarDir, aid + "-" + ver + ".jar");
                    if (!File.Exists(jar)) return true;

                    DateTime jarTime = File.GetLastWriteTimeUtc(jar);
                    DateTime srcTime = LatestSourceTime(md);
                    if (srcTime > jarTime) return true;
                }
            }
            catch (Exception) { return true; }
            return false;
        }

        static string GroupId(Pom p)
        {
            try
            {
                System.Xml.XmlDocument doc = Pom.LoadXml(p.Path);
                string g = Pom.Text(doc.DocumentElement, "groupId");
                if (!string.IsNullOrEmpty(g)) return g.Trim();
                System.Xml.XmlElement par = Pom.FirstKid(doc.DocumentElement, "parent");
                if (par != null)
                {
                    string pg = Pom.Text(par, "groupId");
                    if (!string.IsNullOrEmpty(pg)) return pg.Trim();
                }
            }
            catch (Exception) { }
            return "";
        }

        static string Version(Pom p)
        {
            try
            {
                System.Xml.XmlDocument doc = Pom.LoadXml(p.Path);
                string v = Pom.Text(doc.DocumentElement, "version");
                if (!string.IsNullOrEmpty(v)) return v.Trim();
                System.Xml.XmlElement par = Pom.FirstKid(doc.DocumentElement, "parent");
                if (par != null)
                {
                    string pv = Pom.Text(par, "version");
                    if (!string.IsNullOrEmpty(pv)) return pv.Trim();
                }
            }
            catch (Exception) { }
            return "";
        }

        static DateTime LatestSourceTime(string moduleDir)
        {
            DateTime max = DateTime.MinValue;
            try
            {
                string src = Path.Combine(moduleDir, "src");
                if (!Directory.Exists(src)) return File.GetLastWriteTimeUtc(Path.Combine(moduleDir, "pom.xml"));
                string[] pats = new string[] { "*.java", "*.kt", "*.xml", "*.yml", "*.yaml", "*.properties" };
                foreach (string pat in pats)
                    foreach (string f in Directory.GetFiles(src, pat, SearchOption.AllDirectories))
                    {
                        DateTime t = File.GetLastWriteTimeUtc(f);
                        if (t > max) max = t;
                    }
                DateTime pt = File.GetLastWriteTimeUtc(Path.Combine(moduleDir, "pom.xml"));
                if (pt > max) max = pt;
            }
            catch (Exception) { }
            return max;
        }

        // ---------- 命令拼装 ----------
        public static string CmdExe(BootInfo bi)
        {
            string e = bi.Wrapper.Length > 0 ? bi.Wrapper : bi.Mvn;
            return "\"" + e + "\"";
        }

        // 每条 mvn 命令都要带上的覆盖参数（用户显式配置了才有）
        public static string ExtraArgs(BootInfo bi)
        {
            StringBuilder sb = new StringBuilder();
            if (!string.IsNullOrEmpty(bi.SettingsFile)) sb.Append(" -s \"" + bi.SettingsFile + "\"");
            if (!string.IsNullOrEmpty(bi.Repo)) sb.Append(" -Dmaven.repo.local=\"" + bi.Repo + "\"");
            return sb.ToString();
        }

        // spring-boot:run fork 出来的应用 JVM 不继承 MAVEN_OPTS，必须显式传编码参数，
        // 否则应用日志在管道下按系统 GBK 写出，日志面板里中文全是乱码
        public const string RunJvmEnc = " -Dspring-boot.run.jvmArguments=\"-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8\"";

        public static string BuildCommand(BootInfo bi, string profile, bool doInstall, bool clean)
        {
            List<string> parts = new List<string>();
            string mvn = CmdExe(bi);
            string pf = string.IsNullOrEmpty(profile) ? "" : (" -P" + profile);
            string runProfiles = string.IsNullOrEmpty(profile) ? "" : (" -Dspring-boot.run.profiles=" + profile);
            string common = mvn + " -B" + pf + ExtraArgs(bi);

            if (bi.Multi)
            {
                if (clean)
                {
                    string cleanCmd = common + " -pl \"" + bi.LaunchModule + "\" -am clean install -DskipTests -Dspring-boot.repackage.skip=true";
                    parts.Add(cleanCmd);
                }
                else if (doInstall)
                {
                    string ins = common + " -pl \"" + bi.LaunchModule + "\" -am install -DskipTests -Dspring-boot.repackage.skip=true";
                    parts.Add(ins);
                }
                parts.Add(mvn + " -B" + pf + ExtraArgs(bi) + " -pl \"" + bi.LaunchModule + "\"" + runProfiles + RunJvmEnc + " spring-boot:run");
                return string.Join(" && ", parts.ToArray());
            }

            if (clean) parts.Add(common + " clean");
            parts.Add(mvn + " -B" + pf + ExtraArgs(bi) + runProfiles + RunJvmEnc + " spring-boot:run");
            return string.Join(" && ", parts.ToArray());
        }

        // 仅编译依赖模块
        public static string BuildInstallCommand(BootInfo bi, string profile)
        {
            string mvn = CmdExe(bi);
            string pf = string.IsNullOrEmpty(profile) ? "" : (" -P" + profile);
            string extra = ExtraArgs(bi);
            if (bi.Multi)
                return mvn + " -B" + pf + extra + " -pl \"" + bi.LaunchModule + "\" -am install -DskipTests -Dspring-boot.repackage.skip=true";
            return mvn + " -B" + pf + extra + " -DskipTests compile";
        }

        // 打包命令：产出可执行 jar（不带 repackage.skip），跳过测试
        public static string BuildPackageCommand(BootInfo bi, string profile)
        {
            string mvn = CmdExe(bi);
            string pf = string.IsNullOrEmpty(profile) ? "" : (" -P" + profile);
            string common = mvn + " -B" + pf + ExtraArgs(bi);
            if (bi.Multi)
                return common + " -pl \"" + bi.LaunchModule + "\" -am package -DskipTests";
            return common + " package -DskipTests";
        }

        // 打包产物目录：jar 落在启动模块的 target 下
        public static string PackageDir(BootInfo bi)
        {
            if (bi.Multi && bi.LaunchModule.Length > 0)
                return Path.Combine(bi.RootDir, bi.LaunchModule.Replace('/', '\\'), "target");
            return Path.Combine(bi.LaunchDir, "target");
        }

        // ---------- 自检输出 ----------
        public static void DumpDetect(string dir, string outFile)
        {
            BootInfo bi = Detect(dir);
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("ok=" + bi.Ok);
            sb.AppendLine("error=" + bi.Error);
            sb.AppendLine("tool=" + bi.Tool);
            sb.AppendLine("pickedDir=" + bi.PickedDir);
            sb.AppendLine("rootDir=" + bi.RootDir);
            sb.AppendLine("multi=" + bi.Multi);
            sb.AppendLine("launchModule=" + bi.LaunchModule);
            sb.AppendLine("launchDir=" + bi.LaunchDir);
            sb.AppendLine("candidates=" + string.Join(",", bi.ModuleCandidates.ToArray()));
            sb.AppendLine("profiles=" + string.Join(",", bi.Profiles.ToArray()));
            sb.AppendLine("wrapper=" + bi.Wrapper);
            sb.AppendLine("mvn=" + bi.Mvn);
            sb.AppendLine("javaHome=" + bi.JavaHome);
            sb.AppendLine("javaVer=" + bi.JavaVer);
            sb.AppendLine("wantJava=" + bi.WantJava);
            sb.AppendLine("port=" + bi.Port);
            sb.AppendLine("needInstall=" + bi.NeedInstall);
            sb.AppendLine("repo=" + bi.Repo);
            sb.AppendLine("settings=" + bi.SettingsFile);
            sb.AppendLine("summary=" + bi.Summary);
            sb.AppendLine("command=" + BuildCommand(bi, bi.Profiles.Count > 0 ? bi.Profiles[0] : "", bi.NeedInstall, false));
            sb.AppendLine("installCmd=" + BuildInstallCommand(bi, bi.Profiles.Count > 0 ? bi.Profiles[0] : ""));
            sb.AppendLine("packageCmd=" + BuildPackageCommand(bi, bi.Profiles.Count > 0 ? bi.Profiles[0] : ""));
            sb.AppendLine("packageDir=" + PackageDir(bi));
            File.WriteAllText(outFile, sb.ToString(), new UTF8Encoding(false));
        }
    }

    // ==================== 主窗体 ====================
    class BootForm : Form
    {
        TextBox txtDir;
        TextBox txtJdk, txtMaven, txtRepo, txtSettings;   // 工具链（空 = 自动探测）
        FlatCombo cbModule;
        FlatCombo cbProfile;
        Label lblStatus;
        Label lblPort;
        Label lblRunState;
        CheckBox chkInstall;
        CheckBox chkKill;
        CheckBox chkOpen;
        FlatBtn btnRun, btnStop, btnClean, btnBuild, btnCmd;
        FlatBtn btnPack, btnPackDir;
        LogView log;
        ProcRunner runner = new ProcRunner();
        ProcRunner packRunner = new ProcRunner();
        BootInfo info = new BootInfo();
        ToolChain tool = new ToolChain();
        System.Windows.Forms.Timer pollTimer;

        // ---- 「已在运行」探测（含 IDEA / 上次遗留的进程）----
        PortWatcher portWatch = new PortWatcher();
        ToolTip Tip;
        Sys.PortOwner extRun;      // 端口上已在跑、但非本窗口启动的服务
        int extPort;
        string extKey = "";        // 日志提示去重
        int actualPort;            // 从启动日志校正出的实际端口
        int tick;
        // ---- 自检模式（--uidump）----
        public string PresetDir;
        public string PresetProfile;
        public int PresetPort;
        public string DumpAfter;

        public BootForm()
        {
            Text = "Spring Boot 一键启动器";
            ClientSize = new Size(920, 828);
            MinimumSize = new Size(760, 664);
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
            // ---- 第一行：项目文件夹 ----
            Label l1 = Ux.Lbl("项目文件夹", 20, 22, 76);
            Controls.Add(l1);

            txtDir = new TextBox();
            txtDir.Font = Th.Ui;
            Panel pf = Ux.Field(txtDir, 100, 18, 540, 28);
            Controls.Add(pf);

            FlatBtn btnBrowse = Ux.Btn("浏览...", 648, 18, 74, false);
            btnBrowse.Click += delegate { Browse(); };
            Controls.Add(btnBrowse);

            FlatBtn btnDetect = Ux.Btn("重新检测", 728, 18, 90, false);
            btnDetect.Click += delegate { DetectNow(true); };
            Controls.Add(btnDetect);

            // ---- 第二行：模块 / profile / 端口 ----
            Label l2 = Ux.Lbl("启动模块", 20, 62, 76);
            Controls.Add(l2);
            cbModule = new FlatCombo();
            Panel pm = Ux.Field(cbModule, 100, 58, 190, 28);
            Controls.Add(pm);
            cbModule.SelectedIndexChanged += delegate { OnModuleChanged(); };

            Label l3 = Ux.Lbl("配置 profile", 306, 62, 86);
            Controls.Add(l3);
            cbProfile = new FlatCombo();
            Panel pp = Ux.Field(cbProfile, 396, 58, 140, 28);
            Controls.Add(pp);
            cbProfile.SelectedIndexChanged += delegate { OnProfileChanged(); };

            Label l4 = Ux.Lbl("端口", 552, 62, 34);
            Controls.Add(l4);
            lblPort = Ux.Val("端口 -", 590, 62, 110, Th.Cyan);
            lblPort.Font = Th.UiB;
            Controls.Add(lblPort);
            Tip = new ToolTip();
            Tip.SetToolTip(lblPort, "来自所选 profile 的 src/main/resources/application-<profile>.yml 的 server.port，随 profile 切换自动更新；启动后以日志里的实际端口为准");

            Button dummy = new Button();
            dummy.Visible = false;

            lblStatus = Ux.Val("请选择 Spring Boot 工程目录", 20, 96, 880, Th.Dim);
            Controls.Add(lblStatus);

            // ---- 工具链配置（留空 = 自动探测；预填自动探测值，可改）----
            Label lj = Ux.Lbl("JDK 目录", 20, 126, 66);
            Controls.Add(lj);
            txtJdk = new TextBox();
            Controls.Add(Ux.Field(txtJdk, 90, 122, 250, 28));
            FlatBtn bj = Ux.Btn("浏览", 346, 122, 56, false);
            bj.Height = 28;
            bj.Click += delegate { PickDir(txtJdk, "选择 JDK 安装目录（作为 JAVA_HOME）"); };
            Controls.Add(bj);

            Label lm = Ux.Lbl("Maven 目录", 412, 126, 80);
            Controls.Add(lm);
            txtMaven = new TextBox();
            Controls.Add(Ux.Field(txtMaven, 496, 122, 250, 28));
            FlatBtn bm = Ux.Btn("浏览", 752, 122, 56, false);
            bm.Height = 28;
            bm.Click += delegate { PickDir(txtMaven, "选择 Maven 安装目录（其下应有 bin\\mvn.cmd）"); };
            Controls.Add(bm);

            Label lr = Ux.Lbl("Maven 仓库", 20, 160, 80);
            Controls.Add(lr);
            txtRepo = new TextBox();
            Controls.Add(Ux.Field(txtRepo, 100, 156, 250, 28));
            FlatBtn br = Ux.Btn("浏览", 356, 156, 56, false);
            br.Height = 28;
            br.Click += delegate { PickDir(txtRepo, "选择 Maven 本地仓库目录（localRepository）"); };
            Controls.Add(br);

            Label ls = Ux.Lbl("settings.xml", 426, 160, 80);
            Controls.Add(ls);
            txtSettings = new TextBox();
            Controls.Add(Ux.Field(txtSettings, 510, 156, 236, 28));
            FlatBtn bs = Ux.Btn("浏览", 752, 156, 56, false);
            bs.Height = 28;
            bs.Click += delegate { PickFile(txtSettings, "选择 Maven settings.xml"); };
            Controls.Add(bs);

            // ---- 选项 ----
            chkInstall = MkCheck("启动前自动安装依赖模块", 20, 194, Th.Fg);
            chkKill = MkCheck("启动前结束占用端口的进程", 250, 194, Th.Fg);
            chkOpen = MkCheck("启动后打开浏览器", 480, 194, Th.Fg);
            chkInstall.Checked = true;
            chkKill.Checked = true;
            chkOpen.Checked = true;
            Controls.Add(chkInstall);
            Controls.Add(chkKill);
            Controls.Add(chkOpen);

            // ---- 按钮行 ----
            btnRun = Ux.Btn("▶  启动", 20, 228, 120, true);
            btnRun.Height = 36;
            btnRun.Click += delegate { Run(false); };
            Controls.Add(btnRun);

            btnStop = Ux.Btn("■  停止", 148, 228, 100, false);
            btnStop.Height = 36;
            btnStop.Enabled = false;
            btnStop.Click += delegate { Stop(); };
            Controls.Add(btnStop);

            btnClean = Ux.Btn("清理并重启", 256, 228, 110, false);
            btnClean.Height = 36;
            btnClean.Click += delegate { Run(true); };
            Controls.Add(btnClean);

            btnBuild = Ux.Btn("仅装依赖", 374, 228, 100, false);
            btnBuild.Height = 36;
            btnBuild.Click += delegate { InstallOnly(); };
            Controls.Add(btnBuild);

            btnCmd = Ux.Btn("查看命令", 482, 228, 90, false);
            btnCmd.Height = 36;
            btnCmd.Click += delegate { ShowCommand(); };
            Controls.Add(btnCmd);

            FlatBtn btnFolder = Ux.Btn("打开目录", 580, 228, 90, false);
            btnFolder.Height = 36;
            btnFolder.Click += delegate { if (info.Ok) Sys.OpenFolder(info.LaunchDir); };
            Controls.Add(btnFolder);

            lblRunState = Ux.Val("● 未运行", 690, 228, 220, Th.Dim);
            lblRunState.Font = Th.UiB;
            Controls.Add(lblRunState);

            // ---- 打包行 ----
            btnPack = Ux.Btn("打包", 20, 272, 100, false);
            btnPack.Height = 36;
            btnPack.Click += delegate { PackOnly(); };
            Controls.Add(btnPack);

            btnPackDir = Ux.Btn("产物目录", 128, 272, 100, false);
            btnPackDir.Height = 36;
            btnPackDir.Click += delegate { OpenPackDir(); };
            Controls.Add(btnPackDir);

            Label lPack = Ux.Val("mvn package（跳过测试），产物在启动模块 target 下", 236, 280, 430, Th.Dim);
            Controls.Add(lPack);

            // ---- 日志 ----
            Label l5 = Ux.Val("运行日志", 20, 322, 100, Th.Fg);
            l5.Font = Th.UiB;
            Controls.Add(l5);

            FlatBtn btnCopy = Ux.Btn("复制", 830, 316, 70, false);
            btnCopy.Height = 26;
            btnCopy.Click += delegate
            {
                try { if (log.Text().Length > 0) Clipboard.SetText(log.Text()); }
                catch (Exception) { }
            };
            Controls.Add(btnCopy);

            FlatBtn btnCls = Ux.Btn("清空", 752, 316, 70, false);
            btnCls.Height = 26;
            btnCls.Click += delegate { log.Clear(); };
            Controls.Add(btnCls);

            log = new LogView();
            Panel pl = new Panel();
            pl.SetBounds(20, 348, 880, 460);
            pl.Padding = new Padding(1);
            pl.BackColor = Th.Border;
            pl.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            log.Box.Dock = DockStyle.Fill;
            pl.Controls.Add(log.Box);
            Controls.Add(pl);
            log.Box.BackColor = Th.LogBg;
        }

        CheckBox MkCheck(string text, int x, int y, Color c)
        {
            CheckBox c2 = new CheckBox();
            c2.Text = text;
            c2.ForeColor = c;
            c2.Font = Th.Ui;
            c2.SetBounds(x, y, 226, 24);
            c2.BackColor = Color.Transparent;
            c2.FlatStyle = FlatStyle.Standard;
            c2.Cursor = Cursors.Hand;
            return c2;
        }

        void PickDir(TextBox tb, string desc)
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = desc;
            if (Directory.Exists(tb.Text.Trim())) d.SelectedPath = tb.Text.Trim();
            if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.SelectedPath;
        }

        void PickFile(TextBox tb, string desc)
        {
            OpenFileDialog d = new OpenFileDialog();
            d.Title = desc;
            d.Filter = "settings.xml|settings.xml|XML 文件|*.xml|全部文件|*.*";
            string cur = tb.Text.Trim();
            if (File.Exists(cur)) { d.InitialDirectory = Path.GetDirectoryName(cur); d.FileName = Path.GetFileName(cur); }
            else if (Directory.Exists(cur)) d.InitialDirectory = cur;
            if (d.ShowDialog(this) == DialogResult.OK) tb.Text = d.FileName;
        }

        // ---------- 生命周期 ----------
        void OnLoadInit()
        {
            Dictionary<string, string> c = Cfg.Load("springboot");
            string last = null;
            c.TryGetValue("dir", out last);
            LoadChain(c);
            log.Info("Spring Boot 一键启动器就绪。");
            log.Dim2("选择工程目录 → 选择 profile → 点击「启动」。工具链四项留空即用自动探测值。");
            if (!string.IsNullOrEmpty(last) && Directory.Exists(last))
            {
                txtDir.Text = last;
                DetectNow(false);
            }
        }

        // 从配置装载工具链；为空的项用自动探测值预填（可见、可改）
        void LoadChain(Dictionary<string, string> c)
        {
            string v;
            c.TryGetValue("jdk", out v);
            txtJdk.Text = string.IsNullOrEmpty(v) ? ToolChain.AutoJdk() : v;
            c.TryGetValue("maven", out v);
            txtMaven.Text = string.IsNullOrEmpty(v) ? ToolChain.AutoMavenDir() : v;
            c.TryGetValue("repo", out v);
            txtRepo.Text = string.IsNullOrEmpty(v) ? Sys.LocalRepo() : v;
            c.TryGetValue("settings", out v);
            txtSettings.Text = string.IsNullOrEmpty(v) ? ToolChain.AutoSettings() : v;
            ApplyChain();
        }

        // 把界面上的四项同步到全局 ToolChain（Detect / 命令拼装都从这取）
        void ApplyChain()
        {
            tool.Jdk = txtJdk.Text.Trim();
            tool.MavenDir = txtMaven.Text.Trim();
            tool.Repo = txtRepo.Text.Trim();
            tool.SettingsFile = txtSettings.Text.Trim();
            ToolChain.Current = tool;
        }

        void OnClosing(object sender, FormClosingEventArgs e)
        {
            if (runner.Running)
            {
                DialogResult r = MessageBox.Show(this, "项目仍在运行，是否停止并退出？", "确认",
                    MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (r != DialogResult.Yes) { e.Cancel = true; return; }
            }
            runner.Stop();
            if (string.IsNullOrEmpty(DumpAfter)) Save();   // 自检模式不覆盖用户配置
        }

        void Save()
        {
            ApplyChain();
            Dictionary<string, string> c = new Dictionary<string, string>();
            c["dir"] = txtDir.Text.Trim();
            c["jdk"] = tool.Jdk;
            c["maven"] = tool.MavenDir;
            c["repo"] = tool.Repo;
            c["settings"] = tool.SettingsFile;
            if (cbModule.SelectedItem != null) c["module"] = cbModule.SelectedItem.ToString();
            if (cbProfile.SelectedItem != null) c["profile"] = cbProfile.SelectedItem.ToString();
            Cfg.Save("springboot", c);
        }

        void Browse()
        {
            FolderBrowserDialog d = new FolderBrowserDialog();
            d.Description = "选择 Spring Boot 工程目录（多模块选根目录或任意子模块均可）";
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
                info = new BootInfo();
                info.Error = "目录不存在";
                lblStatus.ForeColor = Th.Err;
                lblStatus.Text = "✗ 目录不存在";
                cbModule.Items.Clear(); cbProfile.Items.Clear();
                return;
            }

            ApplyChain();
            info = Boot.Detect(dir);
            if (!info.Ok)
            {
                lblStatus.ForeColor = Th.Err;
                lblStatus.Text = "✗ " + info.Error;
                cbModule.Items.Clear(); cbProfile.Items.Clear();
                if (verbose) log.Err("检测失败：" + info.Error);
                return;
            }

            // 模块
            cbModule.Items.Clear();
            Dictionary<string, string> c = Cfg.Load("springboot");
            string lastMod = null; c.TryGetValue("module", out lastMod);
            int sel = 0;
            for (int i = 0; i < info.ModuleCandidates.Count; i++)
            {
                string m = info.ModuleCandidates[i];
                cbModule.Items.Add(m.Length == 0 ? "（单模块）" : m);
                if (lastMod != null && m.Equals(lastMod, StringComparison.OrdinalIgnoreCase)) sel = i;
            }
            if (info.Multi)
            {
                for (int i = 0; i < info.ModuleCandidates.Count; i++)
                    if (info.ModuleCandidates[i].Equals(info.LaunchModule, StringComparison.OrdinalIgnoreCase)) sel = i;
            }
            cbModule.SelectedIndex = sel;

            // profile
            cbProfile.Items.Clear();
            string lastPf = null; c.TryGetValue("profile", out lastPf);
            int ps = 0;
            for (int i = 0; i < info.Profiles.Count; i++)
            {
                cbProfile.Items.Add(info.Profiles[i]);
                if (lastPf != null && info.Profiles[i].Equals(lastPf, StringComparison.OrdinalIgnoreCase)) ps = i;
                else if (lastPf == null && info.Profiles[i].Equals("dev", StringComparison.OrdinalIgnoreCase)) ps = i;
            }
            if (cbProfile.Items.Count > 0) cbProfile.SelectedIndex = ps;

            lblStatus.ForeColor = Th.Ok;
            lblStatus.Text = "✓ " + info.Summary + "   |   工程根目录 " + info.RootDir;

            if (verbose)
            {
                log.Ok("检测通过：" + info.Summary);
                log.Info("工程根目录 : " + info.RootDir);
                log.Info("启动模块   : " + (info.Multi ? info.LaunchModule : "（单模块，根目录即启动模块）"));
                log.Info("启动目录   : " + info.LaunchDir);
                log.Info("可用 profile: " + string.Join(", ", info.Profiles.ToArray()));
                if (info.Profiles.Count > 0)
                {
                    List<string> pms = new List<string>();
                    foreach (string p2 in info.Profiles)
                    {
                        int pp = Boot.GuessPort(info.LaunchDir, p2);
                        pms.Add(p2 + (pp > 0 ? "→" + pp : "→默认"));
                    }
                    log.Info("端口映射   : " + string.Join("   ", pms.ToArray()) + "    （读自 application-<profile>.yml 的 server.port，随 profile 切换）");
                }
                log.Info("构建命令   : " + Boot.CmdExe(info).Trim('"'));
                log.Info("JAVA_HOME  : " + (string.IsNullOrEmpty(info.JavaHome) ? "（未设置）" : info.JavaHome + "  " + info.JavaVer));
                if (tool.Repo.Length > 0 || tool.SettingsFile.Length > 0)
                    log.Info("仓库/配置  : " + (tool.Repo.Length > 0 ? tool.Repo : "（跟随 settings.xml）")
                        + (tool.SettingsFile.Length > 0 ? "  |  -s " + tool.SettingsFile : ""));
                if (tool.Jdk.Length > 0 && !Directory.Exists(tool.Jdk))
                    log.Warn("⚠ 配置的 JDK 目录不存在：" + tool.Jdk);
                if (tool.MavenDir.Length > 0 && !File.Exists(Path.Combine(tool.MavenDir, "bin", "mvn.cmd")))
                    log.Warn("⚠ 配置的 Maven 目录下没有 bin\\mvn.cmd：" + tool.MavenDir);
                if (tool.Repo.Length > 0 && !Directory.Exists(tool.Repo))
                    log.Warn("⚠ 配置的 Maven 仓库目录不存在：" + tool.Repo);
                if (tool.SettingsFile.Length > 0 && !File.Exists(tool.SettingsFile))
                    log.Warn("⚠ 配置的 settings.xml 不存在：" + tool.SettingsFile);
                if (!string.IsNullOrEmpty(info.WantJava))
                {
                    string jv = MainVersion(info.JavaVer);
                    string wv = MainVersion(info.WantJava);
                    if (jv.Length > 0 && wv.Length > 0 && jv != wv)
                    {
                        log.Warn("⚠ 项目要求 Java " + info.WantJava + "，但 JAVA_HOME 是 " + info.JavaVer + "。若编译报错请先切换 JDK。");
                    }
                }
                if (info.NeedInstall) log.Warn("依赖模块尚未安装或已过期，启动时会自动执行 install。");
                else if (info.Multi) log.Dim2("依赖模块已是较新版本，启动时将跳过 install。");
            }
            RefreshPort();
        }

        static string MainVersion(string v)
        {
            if (string.IsNullOrEmpty(v)) return "";
            Match m = Regex.Match(v, @"(\d+)");
            if (!m.Success) return "";
            string s = m.Groups[1].Value;
            if (s == "1") { Match m2 = Regex.Match(v, @"1\.(\d+)"); if (m2.Success) return m2.Groups[1].Value; }
            return s;
        }

        void OnModuleChanged()
        {
            if (!info.Ok || !info.Multi) return;
            string m = cbModule.SelectedItem == null ? "" : cbModule.SelectedItem.ToString();
            if (m.Length == 0) return;
            info.LaunchModule = m;
            info.LaunchDir = Path.Combine(info.RootDir, m.Replace('/', '\\'));
            info.NeedInstall = Boot.NeedInstall(info.RootDir, Pom.Read(info.RootPom), m, info.Repo);
            if (chkInstall != null)
                chkInstall.Text = info.NeedInstall ? "启动前自动安装依赖模块（需要）" : "启动前自动安装依赖模块";
            RefreshPort();
        }

        void OnProfileChanged()
        {
            RefreshPort();
        }

        void RefreshPort()
        {
            if (!info.Ok) { lblPort.Text = "端口 -"; return; }
            string pf = cbProfile.SelectedItem == null ? "" : cbProfile.SelectedItem.ToString();
            info.Port = Boot.GuessPort(info.LaunchDir, pf);
            actualPort = 0;
            extKey = "";
            lblPort.Text = info.Port > 0 ? ("端口 " + info.Port + " (" + pf + ")") : "端口 默认";
            PortWatchNow();
        }

        static string JoinOwners(List<Sys.PortOwner> os)
        {
            List<string> s = new List<string>();
            foreach (Sys.PortOwner o in os) s.Add(Sys.Describe(o));
            return string.Join(", ", s.ToArray());
        }

        string CurProfile()
        {
            return cbProfile.SelectedItem == null ? "" : cbProfile.SelectedItem.ToString();
        }

        // ---------- 动作 ----------
        void Run(bool clean)
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择一个有效的 Spring Boot 工程目录。", "提示"); return; }
            if (runner.Running) { MessageBox.Show(this, "项目已在运行，请先停止。", "提示"); return; }
            if (packRunner.Running) { MessageBox.Show(this, "正在打包，请等待打包完成。", "提示"); return; }

            string pf = CurProfile();
            if (pf.Length == 0) { MessageBox.Show(this, "请选择配置 profile。", "提示"); return; }

            bool doInstall = chkInstall.Checked;
            if (!doInstall && info.Multi && info.NeedInstall)
            {
                DialogResult r = MessageBox.Show(this,
                    "检测到依赖模块（common / logic 等）尚未安装或已过期。\n跳过安装很可能启动失败，仍要继续吗？",
                    "确认", MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (r != DialogResult.Yes) return;
            }

            Save();

            // 端口占用处理
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

            string cmd = Boot.BuildCommand(info, pf, doInstall, clean);
            log.Banner(clean ? "清理并启动" : "启动 Spring Boot");
            log.Info("profile   : " + pf);
            log.Info("工作目录  : " + (info.Multi ? info.RootDir : info.LaunchDir));
            log.Cmd("$ " + cmd);
            log.Line("", Th.Fg);

            try
            {
                Dictionary<string, string> env = new Dictionary<string, string>();
                env["MAVEN_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8";
                ChainEnv(env);
                runner.Start(info.RootDir, cmd, env);
            }
            catch (Exception ex)
            {
                log.Err("启动失败：" + ex.Message);
                return;
            }

            btnRun.Enabled = false;
            btnClean.Enabled = false;
            btnStop.Enabled = true;
            RefreshRunState();

            if (chkOpen.Checked)
            {
                System.Threading.Timer t = null;
                t = new System.Threading.Timer(delegate (object st)
                {
                    try
                    {
                        System.Threading.Thread.Sleep(12000);
                        int p = CurPort();
                        if (p > 0) Sys.OpenUrl("http://localhost:" + p);
                    }
                    catch (Exception) { }
                    try { t.Dispose(); } catch (Exception) { }
                }, null, 12000, System.Threading.Timeout.Infinite);
            }
        }

        void InstallOnly()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            if (runner.Running) { MessageBox.Show(this, "项目正在运行，请先停止。", "提示"); return; }
            if (packRunner.Running) { MessageBox.Show(this, "正在打包，请等待打包完成。", "提示"); return; }
            string cmd = Boot.BuildInstallCommand(info, CurProfile());
            log.Banner("仅安装依赖模块");
            log.Cmd("$ " + cmd);
            Dictionary<string, string> env = new Dictionary<string, string>();
            env["MAVEN_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8";
            ChainEnv(env);
            runner.Start(info.RootDir, cmd, env);
            btnRun.Enabled = false; btnClean.Enabled = false; btnStop.Enabled = true;
        }

        // ---------- 打包 ----------
        void PackOnly()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            if (runner.Running || packRunner.Running) { MessageBox.Show(this, "已有任务在运行，请等待完成或先停止。", "提示"); return; }
            string cmd = Boot.BuildPackageCommand(info, CurProfile());
            log.Banner("打包工程");
            log.Info("产物目录  : " + Boot.PackageDir(info));
            log.Cmd("$ " + cmd);
            log.Line("", Th.Fg);
            Dictionary<string, string> env = new Dictionary<string, string>();
            env["MAVEN_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8";
            ChainEnv(env);
            try { packRunner.Start(info.RootDir, cmd, env); }
            catch (Exception ex) { log.Err("打包启动失败：" + ex.Message); return; }
            btnRun.Enabled = false; btnClean.Enabled = false; btnBuild.Enabled = false; btnPack.Enabled = false;
        }

        void OpenPackDir()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            string d = Boot.PackageDir(info);
            if (!Directory.Exists(d)) { MessageBox.Show(this, "产物目录还不存在，请先打包：\n" + d, "提示"); return; }
            Sys.OpenFolder(d);
        }

        void OnPackOut(string line)
        {
            Color c = Th.LogFg;
            string t = line.TrimStart();
            if (t.StartsWith("ERROR") || t.Contains("BUILD FAILURE")) c = Th.Err;
            else if (t.Contains("BUILD SUCCESS")) c = Th.Ok;
            else if (t.StartsWith("[INFO] ---") || t.StartsWith("Downloading") || t.StartsWith("Downloaded")) c = Th.Dim;
            log.Line(line, c);
        }

        void OnPackExit(int code)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    btnRun.Enabled = true; btnClean.Enabled = true; btnBuild.Enabled = true; btnPack.Enabled = true;
                    if (code == 0) log.Ok(">>> 打包完成，产物目录：" + Boot.PackageDir(info));
                    else log.Err(">>> 打包失败（exit " + code + "）");
                }));
            }
            catch (Exception) { }
        }

        // 把工具链选择注入子进程环境：JAVA_HOME 决定 mvn 用哪个 JDK，PATH 前置保证优先命中
        void ChainEnv(Dictionary<string, string> env)
        {
            string jdkBin = "";
            if (tool.Jdk.Length > 0 && Directory.Exists(tool.Jdk))
            {
                env["JAVA_HOME"] = tool.Jdk;
                jdkBin = tool.Jdk + "\\bin;";
            }
            string pre = jdkBin;
            if (tool.MavenDir.Length > 0 && Directory.Exists(tool.MavenDir))
                pre += tool.MavenDir + "\\bin;";
            if (pre.Length > 0)
            {
                string old = Environment.GetEnvironmentVariable("PATH");
                env["PATH"] = string.IsNullOrEmpty(old) ? pre : pre + old;
            }
        }

        void ShowCommand()
        {
            if (!info.Ok) { MessageBox.Show(this, "请先选择工程目录。", "提示"); return; }
            string pf = CurProfile();
            log.Banner("命令预览");
            log.Cmd("$ " + Boot.BuildCommand(info, pf, chkInstall.Checked, false));
            log.Dim2("（不会真正执行）");
        }

        void Stop()
        {
            // 1) 本窗口启动的：杀整棵 cmd -> mvn.cmd -> java 进程树
            if (runner.Running)
            {
                log.Warn("正在停止本窗口启动的进程（含子进程 java）...");
                runner.Stop();
                btnRun.Enabled = true; btnClean.Enabled = true;
                RefreshRunState();
                return;
            }

            // 2) 端口上已经在跑的：IDEA 启动的，或上次关闭本窗口时没停掉的
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

            bool mine = Sys.InDir(o.Cmd, info.RootDir);
            string who = Sys.Describe(o) + (port > 0 ? "，端口 " + port : "");
            string msg = "本窗口没有启动过服务，但检测到" + (mine ? "本工程的" : "其它") + "进程正在监听：\n\n"
                + who + (o.Cmd.Length > 0 ? "\n" + Sys.Shorten(o.Cmd, 220) : "")
                + "\n\n它可能是在 IDEA 里启动的，也可能是上次关闭本窗口时没有停止。\n确定要结束它吗？";
            if (MessageBox.Show(this, msg, "结束进程", MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes)
                return;

            log.Warn("正在结束 " + who + " ...");
            Sys.KillPid(o.Pid);
            System.Threading.Thread.Sleep(500);
            extRun = null; extKey = "";
            RefreshRunState();
            PortWatchNow();
        }

        // ---------- 事件 ----------
        void OnOut(string line)
        {
            Color c = Th.LogFg;
            string t = line.TrimStart();
            if (t.StartsWith("ERROR") || t.Contains("BUILD FAILURE") || t.Contains("APPLICATION FAILED TO START"))
                c = Th.Err;
            else if (t.StartsWith("WARN") || t.Contains("BUILD SUCCESS"))
                c = Th.Warn;
            else if (t.StartsWith("[INFO] ---") || t.StartsWith("Downloading") || t.StartsWith("Downloaded"))
                c = Th.Dim;
            else if (t.Contains("Started ") && t.Contains(" in ") && t.Contains("seconds"))
                c = Th.Ok;
            else if (t.Contains("Tomcat started on port") || t.Contains("Started "))
                c = Th.Ok;
            CatchPort(line);
            log.Line(line, c);
        }

        // 从启动日志校正实际端口（Spring Boot 2 是 port(s):，3 是 port）
        void CatchPort(string line)
        {
            if (actualPort > 0) return;
            Match m = Regex.Match(line, @"port\(s\):\s*(\d+)");
            if (!m.Success) m = Regex.Match(line, @"[Tt]omcat started on port\s+(\d+)");
            if (!m.Success) return;
            int p;
            if (!int.TryParse(m.Groups[1].Value, out p) || p <= 0 || p > 65535) return;
            actualPort = p;
            try
            {
                if (!log.Box.IsDisposed)
                    log.Box.BeginInvoke(new Action(delegate
                    {
                        lblPort.Text = "端口 " + p + " (实际)";
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
                    btnRun.Enabled = true; btnClean.Enabled = true;
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
            if (tick % 2 == 1) PortWatchNow();   // 每 2 秒探测一次，避免 netstat 频繁开销
            RefreshRunState();
        }

        void PortWatchNow()
        {
            int p = CurPort();
            if (p <= 0) { extRun = null; extPort = 0; return; }
            portWatch.Refresh(p);
        }

        void OnPortProbe(List<Sys.PortOwner> owners, int port)
        {
            if (log.Box.IsDisposed) return;
            try
            {
                log.Box.BeginInvoke(new Action(delegate
                {
                    if (port != CurPort()) return;      // 端口已切换，丢弃过期结果
                    extPort = port;
                    extRun = owners.Count > 0 ? owners[0] : null;

                    string key = extRun == null ? "" : ("pid:" + extRun.Pid);
                    if (key != extKey)
                    {
                        if (extRun != null && !runner.Running)
                        {
                            bool mine = Sys.InDir(extRun.Cmd, info.RootDir);
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
                bool mine = Sys.InDir(o.Cmd, info.RootDir);
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
            if (!string.IsNullOrEmpty(PresetProfile))
            {
                for (int i = 0; i < cbProfile.Items.Count; i++)
                    if (cbProfile.Items[i].ToString() == PresetProfile) { cbProfile.SelectedIndex = i; break; }
            }
            if (PresetPort > 0)
            {
                actualPort = PresetPort;
                lblPort.Text = "端口 " + PresetPort + " (实际)";
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
                sb.AppendLine("portLabel=" + lblPort.Text);
                sb.AppendLine("profile=" + CurProfile());
                sb.AppendLine("status=" + lblStatus.Text);
                sb.AppendLine("runState=" + lblRunState.Text);
                sb.AppendLine("stopText=" + btnStop.Text);
                sb.AppendLine("stopEnabled=" + btnStop.Enabled);
                sb.AppendLine("runEnabled=" + btnRun.Enabled);
                sb.AppendLine("extPid=" + (extRun == null ? 0 : extRun.Pid));
                sb.AppendLine("extName=" + (extRun == null ? "" : extRun.Name));
                sb.AppendLine("extInDir=" + (extRun == null ? "" : Sys.InDir(extRun.Cmd, info.RootDir).ToString()));
                sb.AppendLine("extCmd=" + (extRun == null ? "" : Sys.Shorten(extRun.Cmd, 300)));
                File.WriteAllText(DumpAfter, sb.ToString(), new UTF8Encoding(false));
            }
            catch (Exception) { }
        }
    }

    static class Program2
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
                BootForm f = new BootForm();
                f.PresetDir = args[1];
                f.PresetProfile = args[2];
                f.PresetPort = ov;
                f.DumpAfter = args[4];
                Application.Run(f);
                return;
            }
            if (args.Length >= 7 && args[0] == "--chain")
            {
                // 自检：--chain <工程目录> <jdk> <maven目录> <仓库> <settings> <输出文件>
                ToolChain t = new ToolChain();
                t.Jdk = args[2]; t.MavenDir = args[3]; t.Repo = args[4]; t.SettingsFile = args[5];
                ToolChain.Current = t;
                try { Boot.DumpDetect(args[1], args[6]); }
                catch (Exception ex)
                {
                    File.WriteAllText(args[6], "ok=False\r\nerror=" + ex.ToString(), new UTF8Encoding(false));
                }
                return;
            }
            if (args.Length >= 3 && args[0] == "--detect")
            {
                try { Boot.DumpDetect(args[1], args[2]); }
                catch (Exception ex)
                {
                    File.WriteAllText(args[2], "ok=False\r\nerror=" + ex.ToString(), new UTF8Encoding(false));
                }
                return;
            }
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new BootForm());
        }
    }
}
