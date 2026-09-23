# DevLauncher · 一键启动器（三个 Windows 绿色小工具）

双击即用：**一个合并窗口（启动台）** + **两个独立启动器**，替代「切到 IDEA → 找运行按钮 → 点一下」的流程。

- `bin/启动台.exe` —— 一个窗口同时管**后端 + 前端**，前后端日志左右分栏同屏（**日常推荐**，见第一节）
- `bin/启动后端.exe` —— 选工程目录 + 选 profile → 点「启动」→ 后端起来
- `bin/启动前端.exe` —— 选工程目录 + 选 npm 脚本 → 点「启动」→ 前端起来

三个都是**原生 WinForms 单文件 exe**（98KB / 55KB / 44KB，只依赖系统自带的 .NET Framework 4.x），**不需要装 Python / Node / 任何运行时**，可以直接拷到桌面或 U 盘。

> 已经在运行也能停：工具不只看自己启动的进程，**端口上有服务在跑就会显示「运行中」并让「停止」按钮可用** —— IDEA 里启动的、上次没停干净的，都可以在这里一键结束。详见第三节。

---

## 一、启动台（启动台.exe）· 前后端合一（日常推荐）

一个窗口同时管后端 + 前端，**窗口只有 880×560**（比两个单独工具都小），前后端日志**左右分栏同屏**。

```
┌ 启动台 · 后端 + 前端 ─────────────────────────────────────────────────────────────┐
│ 后端  [F:\...\backend\device-note              ] 浏览  [dev ▾]  ▶  ■  ● 8092(dev) · 未运行 │
│ 前端  [F:\...\ft-checnote                      ] 浏览  [local dev ▾] ▶ ■  ● 4177 · 运行中 … │
│ ▶ 全部启动  ■ 全部停止  设置…  ☑启动后打开页面  后端页面 前端页面      清空  复制      │
├───────────────────────────────┬───────────────────────────────────────────────────┤
│ 后端日志 · Maven 多模块(4)…    │ 前端日志 · ft-checnote                              │
│ [INFO] BUILD SUCCESS           │ VITE v5.x  ready in 380 ms                         │
│ Tomcat started on port 8092    │ ➜  Local: http://localhost:4177/                   │
└───────────────────────────────┴───────────────────────────────────────────────────┘
```

行为要点：

- **一行一个端**：目录（浏览即自动检测）→ 下拉（后端 profile / 前端 npm 脚本）→ `▶` 启动 / `■` 停止。状态灯实时显示 `端口(profile) · 运行中/未运行`，端口跟随 profile、启动后按日志校正。
- **`▶ 全部启动` / `■ 全部停止`**：一次起两边（各自独立进程，互不影响）；「全部停止」只对**真正在运行**的一侧生效。
- **日志左右分栏**：拖中间的分隔条可改变宽度；`复制` 把两边日志合并复制（各带标题），`清空` 清两边。
- **`设置…`**：JDK 目录 / Maven 目录 / Maven 仓库 / settings.xml / **Node.js 目录**（5 项，留空 = 自动探测或 PATH）+ 两个行为开关（后端自动 install、启动前结束占用端口的进程）。
- **配置继承**：首次打开会继承 `启动后端.exe`（`springboot.ini`）和 `启动前端.exe`（`vite.ini`）已保存的目录 / profile / 脚本 / 工具链，之后存在 `%APPDATA%\DevLauncher\combined.ini`。
- **「已在运行」照样能停**：与两个单独工具同一套端口探测逻辑（见第四节）——IDEA / WebStorm 启动的、上次遗留的，状态灯会标 `· 外部`，`■` 可一键接管（非本工程会弹确认框）。

> 两个单独的 exe 保留不变：想只做一件事（只起后端、只看后端日志）时照旧用它们；日常一边改前端一边重启后端就用启动台。

---

## 二、后端启动器（启动后端.exe）

### 用法

1. 双击 `启动后端.exe`
2. 「项目文件夹」点 **浏览** —— 选**多模块根目录**或**任意子模块目录**都行，会自动向上找到真正的工程根：
   - 选 `backend/device-note` → 识别为多模块，启动模块自动选 `launcher`
   - 选 `backend/device-note/launcher` → 同样自动提升到父工程，启动模块仍是 `launcher`
3. 「启动模块」：多模块工程只列出**含 `spring-boot-maven-plugin` 的子模块**（通常唯一，已自动选好）
4. 「配置 profile」：从 `src/main/resources/application-*.yml` + 根 pom 的 `<profiles>` 自动扫描（本项目 `dev / test / deploy`）
5. **工具链四项**（和项目文件夹一样的交互方式，都预填了自动探测值，可改；下次启动记住）：
   - **JDK 目录**：作为 `JAVA_HOME` 传给 mvn 子进程（同时把 `%JAVA_HOME%\bin` 前置到 PATH）。想用 JDK 21 跑本项目而系统默认是 22？在这里指定即可
   - **Maven 目录**：配置后**优先于工程 `mvnw.cmd` 与 PATH 里的 mvn**，实际执行 `<目录>\bin\mvn.cmd`
   - **Maven 仓库**：配置后所有 mvn 命令追加 `-Dmaven.repo.local=<目录>`，且「install 是否需要」的判定也按这个仓库算
   - **settings.xml**：配置后所有 mvn 命令追加 `-s <文件>`
6. 点 **▶ 启动**

### 启动时实际执行的命令

```
# ① 依赖模块没装、或源码比仓库里的 jar 新 → 先装一次（可关掉）
mvn -B -Pdev -pl "launcher" -am install -DskipTests -Dspring-boot.repackage.skip=true

# ② 起服务（在工程根目录执行，不用切目录）
mvn -B -Pdev -pl "launcher" -Dspring-boot.run.profiles=dev spring-boot:run
```

> 如果配置了 Maven 仓库 / settings.xml，上面每条 mvn 命令都会带上 `-s "..." -Dmaven.repo.local="..."`；
> 配置了 JDK 时子进程的 `JAVA_HOME` 会被替换、`PATH` 前置 `%JAVA_HOME%\bin`。没配置的项不下发任何参数（走 Maven 默认）。

几处刻意的设计：

- **`-Dspring-boot.run.profiles=<p>` 在运行时指定 profile**，绕开「pom 里 `@activated@` 是编译期占位符、切 profile 不 clean 不生效」的老毛病 —— 所以切 dev/test/deploy **不需要 clean**，秒切。
- **install 是否执行是算出来的**：比对本地仓库里兄弟模块的 jar 与新 jar 的 `src` 目录最新修改时间，没变就跳过，日常重启不会白等一次 install。本地仓库位置从 `settings.xml` 的 `localRepository` 读（这台机器上是 `F:\MavenRepo`，不是默认的 `~/.m2/repository`）。
- **多模块用根目录 + `-pl <module>`**，不 `pushd` 进子模块目录，避免含中文的路径经过 cmd 参数。
- **中文编码三层保障**（否则日志面板里应用日志全是乱码）：
  1. `spring-boot:run` 追加 `-Dspring-boot.run.jvmArguments="-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8"` —— fork 出来的应用 JVM **不继承 `MAVEN_OPTS`**，必须显式传；
  2. 日志按行在字节层解码：先严格 UTF-8，失败回退 GBK —— mvn（UTF-8）与各类 GBK 输出的老工具都能正常显示；
  3. stderr 并入日志（以前只接 stdout，写到 stderr 的告警/异常栈会丢）。
- 单模块工程直接一条 `mvn -Pdev … spring-boot:run`，不做 install。

### 界面开关

| 开关 | 作用 |
| --- | --- |
| 启动前自动安装依赖模块 | 关掉就直接 `spring-boot:run`；依赖不新鲜时会弹窗二次确认 |
| 启动前结束占用端口的进程 | 端口从 profile 的 yml 读（dev=**8092**），启动前用 `netstat` 找出监听该端口的进程并 `taskkill /F /T`，日志里会写明 `PID 12888 (node)` 与命令行 |
| 启动后打开浏览器 | 启动 12 秒后打开 `http://localhost:<端口>`（用日志校正过的真实端口） |

按钮：**▶ 启动**、**■ 停止**（`taskkill /F /T` 杀整棵进程树 `cmd → mvn.cmd → java`）、**清理并重启**（先 `clean install` 再起，改 pom / 换 profile 后用）、**仅装依赖**、**查看命令**（只打印不执行）、**打开目录**。

其它：
- **端口随 profile 走**：切换 profile 下拉时，端口框实时按 `application-<profile>.yml` 重新解析（如 device-note：dev→8092 / test→8091 / deploy→8090；single-device-note：dev→7092 / test→7091 / deploy→7090）。检测日志里会打印每个 profile 的端口映射。
- 端口框显示 `端口 8092 (dev)`；启动后以日志校正的实际端口为准（显示 `端口 xxxx (实际)`）。注意工具读的是**磁盘上的** yml —— IDEA 里改了没保存（Ctrl+S）是看不到的。
- JDK 版本对不上会告警（比较**生效的 JDK**（配置的目录优先，否则 `JAVA_HOME`）的 `release` 与 pom 的 `<java.version>`：本工程要 21、本机默认 22 → 提示，但不阻塞）。
- 工具链四项如有路径填错（目录不存在、没有 `bin\mvn.cmd`、settings 不是文件），检测时日志会逐项告警。
- 日志面板按行着色：`BUILD FAILURE`/`ERROR`/`APPLICATION FAILED TO START` 红，`BUILD SUCCESS`/`Started … in … seconds` 绿，下载类灰。
- 上次的目录 / profile / 工具链四项存在 `%APPDATA%\DevLauncher\springboot.ini`，下次自动带出并自动检测。

---

## 三、前端启动器（启动前端.exe）

1. 双击 `启动前端.exe`
2. 选前端工程根目录（含 `package.json`）
3. 「启动脚本」下拉直接读 `package.json` 的 `scripts`，每项显示成 `名字 → 真实命令`；自动优先选中 `dev`/`serve`/名字含 dev 的（本项目：`local dev → vite`）
4. **Node.js 目录**（和后端的 JDK 目录同款交互，下次启动记住）：留空 = 用系统 PATH；配置后**优先用该目录下的 `npm.cmd` / `pnpm.cmd` / `yarn.cmd`**，并把该目录前置到子进程 `PATH`（npm.cmd 内部找 `node.exe` 也走 PATH，保证整套 Node 都是同一个版本）。右侧提示会实时显示解析到的 pm 路径，目录不存在 / 目录下没有对应 pm.cmd 会标黄告警。想用 22 跑而系统默认 20？在这里指定即可
5. 点 **▶ 启动**

- 包管理器按 lock 文件判定：`pnpm-lock.yaml`→pnpm、`yarn.lock`→yarn、`package-lock.json`→npm（本项目 npm）
- 端口从 `vite.config.*` 的 `server.port` 读（本项目 4177）；如果 vite 因为端口被占自动顺延，工具会从 `Local: http://localhost:4178/` 这条日志抓出**真实端口**，「打开页面」和停止都用真实端口
- 开关：**依赖未安装时自动安装**（缺 `node_modules` 先跑 install）、**启动前结束占用端口的进程**、**启动后打开浏览器**
- 按钮：**▶ 启动**、**■ 停止**、**安装依赖**、**查看命令**、**打开目录**、**打开页面**

---

## 四、已经在运行？直接停

「运行中」的判断不是「本窗口有没有启过进程」，而是**你配的那个端口上有没有人在监听**（netstat，每 2 秒查一次，后台线程，不卡界面）。所以下面三种情况都能识别，并且 **■ 停止** 都可点：

| 场景 | 状态灯 | 停止按钮 | 点击后 |
| --- | --- | --- | --- |
| 本窗口启动的 | 绿 `● 运行中  PID 8092 (java)` | `■ 停止` | 直接杀整棵进程树 `cmd → mvn.cmd → java` |
| IDEA / WebStorm 启动的，或上次工具关了但服务还在 | 黄 `● 运行中  PID 12888 (node) · 非本窗口` | `■ 停止(外部)` | 先弹确认框（PID / 进程名 / 完整命令行），确认后 `taskkill /F /T` |
| 端口被别的程序占了（不是这个工程） | 黄 `● 运行中  PID 7234 (node) · 非本工程` | `■ 停止(外部)` | 同上，确认框里会标出「其它进程」 |

判断「是不是本工程」的依据是**进程命令行里有没有你的工程路径**（后端 IDEA 启动的 java 命令行里带 `\backend\device-note\launcher\target\classes`，前端 vite 带 `\ft-checnote\node_modules\...\vite.js`），拿不到命令行时退化为「非本工程」，不影响停止。

日志里会同步写明来源：

```
检测到端口 4177 上已经有服务在运行：PID 12888 (node)（本工程），可直接点「停止」结束它。
    "node"   "F:\Projects\Study\github\DeviceNote\ft-checnote\node_modules\.bin\..\vite\bin\vite.js"
```

两个配套行为：
- **点「启动」时**：如果端口上已经有服务，会先按「启动前结束占用端口的进程」开关决定是否清掉它，再起新的 —— 也就是「一键重启」。（日志会写明清掉的是 `PID 12888 (node)`。）
- **关窗口时**：只有本窗口启动的进程会被问「是否停止并退出」；别处启动的不动它。

---

## 五、改代码后重新编译

```bash
python build.py                       # 生成 bin/启动后端.exe、bin/启动前端.exe、bin/启动台.exe
python build.py 启动台                 # 只编译其中一个（按关键字过滤，避免覆盖正开着的 exe）
python selftest.py                    # 工程探测自检（6 项，不启动任何服务）
python check_nodechain.py             # 前端 Node.js 目录注入：pmExe 指向配置目录（2 项）
python check_combo.py                 # 启动台：双端探测 / 界面状态 / 外部运行识别 / 进程链路 / 配置持久化（22 项）
python check_combo_run.py             # 启动台：真跑一次它拼出的后端启动命令（Tomcat 起得来 / 0 乱码 / 停止后端口释放）
python e2e.py                         # 端到端：真起一次 vite，验证引号/UTF-8/ANSI/进程树清理
python check_runstate.py              # 「已在运行」检测 + 停止外部进程（25 项断言，含真实端口探测）
python e2e.py --with-install          # 额外跑一遍后端 maven install（真编译，约 20s）
```

> 启动台由 `CombinedLauncher.cs` + 复用 `Shared.cs` / `SpringBootLauncher.cs` / `ViteLauncher.cs` 编译而成
> （`/main:DevLaunch.Program0` 指定入口，所以三个 exe 能共用同一批探测代码）。

`build.py` 调 `C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe`，`/codepage:65001` 读 UTF-8 源码。
**源码必须保持 C# 5 语法** —— 这个旧版 csc 不支持字符串插值 `$""`、`?.`、`nameof`。
`System.Management` 引用是读进程命令行用的（`Win32_Process.CommandLine`，判断监听者属于哪个工程）。

### 当前验证状态

| 项目 | 结果 |
| --- | --- |
| 探测自检 6 项（多模块 / 子模块自动上溯 / 单模块 / 类型不匹配报错） | 全部通过 |
| 端到端：vite 真启动 → 停止 | 通过，端口已释放、无进程残留 |
| 端到端：UTF-8 输出、ANSI 剥离 | 通过（vite 输出的 `➜`、中文均正常） |
| 端到端：含中文路径 `cd` | 通过 |
| 外部运行检测 + 停止（25 项） | 全部通过 |
| ├ 真实场景：用户自己的 vite（4177，PID 12888） | 窗口显示 `运行中 · 非本窗口`、停止可用、**未误杀** |
| ├ 真实 node 服务当「别处启动的服务」（前端 / 后端各一次） | 识别到 PID + 命令行，`extInDir=True`，停止后端口释放、进程退出 |
| ├ 端口被非本工程进程占用 | 标注为 `非本工程`，仍可停止 |
| └ 停止后回到「未运行」，按钮自动禁用 | 通过 |
| 后端 `mvn -pl launcher -am install` | BUILD SUCCESS（4 模块，19.9s，中文日志正常） |
| 后端带工具链参数 `mvn -s … -Dmaven.repo.local=F:\MavenRepo … install` | BUILD SUCCESS（39.4s） |
| 工具链注入验证（check_chain.py，10 项） | 全部通过：JDK 覆盖 / mvn 用配置目录 / `-s` 与 `-Dmaven.repo.local` 只在配置时下发 / 未配置不下发 |
| 前端 Node.js 目录注入验证（check_nodechain.py，2 项） | 全部通过：配置目录后 pmExe 落在该目录下；留空走 PATH |
| 前端 GUI 冒烟（新布局含 Node.js 目录行） | 通过 |
| 后端 profile 过滤 | `target/classes/application.yml` 里 `active: dev` ✓ |
| GUI 冒烟（两个 exe 起窗不崩） | 通过 |
| 启动台 `check_combo.py`（双端探测 / 状态灯 / 外部服务识别且未误杀 / 进程链路 / 配置持久化） | 全部通过 |
| 启动台 `check_combo_run.py`（真跑后端启动命令） | Tomcat started（随机端口）/ 204 行日志 0 乱码 / 停止后 8092 无残留、无 java 残留 |

---

## 六、命令行选项（排查用）

```bash
# 只探测不启动，把结论写文件（前端 --detect 可选第 4 参指定 Node.js 目录，验证 pmExe 解析）
启动后端.exe --detect "F:\path\to\project"  out.txt
启动前端.exe --detect "F:\path\to\frontend" out.txt [node目录]

# 同上，但注入工具链（jdk / maven目录 / 仓库 / settings），验证命令拼装
启动后端.exe --chain "F:\path\to\project" "E:\Javas\jdk\22" "E:\Javas\maven\3.9.6" "F:\MavenRepo" "E:\Javas\maven\3.9.6\conf\settings.xml" out.txt

# 看某个端口上是谁在监听（PID / 进程名 / 完整命令行）
启动前端.exe --probe 4177 out.txt

# 结束某个端口上的监听者（与「停止」按钮同一条路径）
启动前端.exe --stop-port 4177 out.txt

# 在指定目录跑一条命令，捕获输出，N 秒后强杀并验证进程树清理
启动前端.exe --run "F:\path\to\frontend" "npm run dev" out.txt 30

# 自检：打开窗口 → 4.5 秒后把界面状态（状态灯 / 按钮文案 / 可用性 / 探测到的 PID）写文件后自动关闭
启动前端.exe --uidump "F:\path\to\frontend" "" 4177 out.txt
启动后端.exe --uidump "F:\path\to\project" dev 8092 out.txt

# 启动台：一次探测前后端两端（含各自将执行的命令）
启动台.exe --detect-all "F:\path\to\backend" "F:\path\to\frontend" out.txt

# 启动台：打开窗口 → 5 秒后 dump 两端状态后自动关闭
启动台.exe --uidump "F:\path\to\backend" "F:\path\to\frontend" out.txt

# 启动台同样支持 --probe / --stop-port / --run（与上面同一条路径）
```

---

## 七、已知边界

- 后端只支持 **Maven** 的命令拼装；选中 Gradle 工程会明确提示改用 IDEA。
- 只认含 `spring-boot-maven-plugin` 的子模块作为启动模块；都不含时退化成列出全部子模块让手选。
- 「运行中」依赖端口探测：如果端口识别不出来（yml 里 `server.port` 是变量、`vite.config` 里没写死 port、profile 里端口在别的文件），就只认本窗口启动的进程。启动后工具会从日志校正端口，这种情况一般能补上。
- 停止只作用于**监听该端口的那一个 PID 及其子进程树**。如果服务不是以监听端口的方式跑（纯编译、只跑单元测试），不会出现在这里。
- `cmd` 自身回显的中文（`echo 中文`、`cd` 回显路径）在 `chcp 65001` 下会变成 `U+FFFD` 方块 —— 这是 cmd 的已知问题，**不影响命令执行**（含中文的路径能正常进入），也不影响 mvn / node / java 的输出（它们是独立进程，输出 UTF-8，读取正常）。
- 日志面板最多保留约 40 万字符，超出从头部裁剪。
