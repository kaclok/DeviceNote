package com.smlj.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Collection;

// 代码更新不停机：Spring Boot应用实现零停机更新！
// https://mp.weixin.qq.com/s/_rt1NP_LPfzatb0EYXry9Q
public class Reboot {
    @Value("smlj.rebooter.file")
    private static String rebooterFile;

    // 得到的结果是backend的全路径，包括backend
    // System.getProperty("user.dir")

    public static ConfigurableApplicationContext boot(Class applicationCls, String[] args, int curPort, int targetPort) {
        String[] newArgs = args.clone();
        boolean needChangePort = false;
        if (isPortInUse(curPort)) {
            newArgs = new String[args.length + 1];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            newArgs[newArgs.length - 1] = "--server.port=" + targetPort;
            needChangePort = true;
        }
        ConfigurableApplicationContext run = SpringApplication.run(applicationCls, newArgs);
        if (needChangePort) {
            // 在 Windows 系统上可能返回 "Windows 10"、"Windows Server 2019" 等类似包含 "Windows" 字样的字符串，在 Linux 系统上可能返回 "Linux"，在 macOS 系统上可能返回 "Mac OS X" 等
            String osName = System.getProperty("os.name").toLowerCase();
            String command = null;
            // 查看哪些进程正在使用该端口进行网络监听操作,获取这些进程pid, 然后终止这些pid的进程
            if (osName.contains("windows")) {
                command = rebooterFile;
            } else {
                command = "lsof -i :%d | grep LISTEN | awk '{print $2}' | xargs kill -9";
            }

            try {
                if (osName.contains("windows")) {
                    Runtime.getRuntime().exec(new String[]{"cmd.exe", rebooterFile, String.valueOf(curPort)}).waitFor();
                } else {
                    command = String.format(command, curPort);
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", command}).waitFor();
                }

                // 等待占用目标端口的进程被终止
                while (true) {
                    // 死循环等待
                    if (!isPortInUse(curPort)) {
                        break;
                    }
                }
                ServletWebServerFactory webServerFactory = getWebServerFactory(run);
                ((TomcatServletWebServerFactory) webServerFactory).setPort(curPort);
                WebServer webServer = webServerFactory.getWebServer(invokeSelfInitialize(((ServletWebServerApplicationContext) run)));
                webServer.start();

                ((ServletWebServerApplicationContext) run).getWebServer().stop();
            } catch (IOException | InterruptedException ignored) {
                // ignore
            }
        }

        return run;
    }

    private static ServletContextInitializer invokeSelfInitialize(ServletWebServerApplicationContext context) {
        try {
            Method method = ServletWebServerApplicationContext.class.getDeclaredMethod("getSelfInitializer");
            method.setAccessible(true);
            return (ServletContextInitializer) method.invoke(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPortInUse(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    protected static Collection<ServletContextInitializer> getServletContextInitializerBeans(ConfigurableApplicationContext context) {
        return new ServletContextInitializerBeans(context.getBeanFactory());
    }

    private static ServletWebServerFactory getWebServerFactory(ConfigurableApplicationContext context) {
        String[] beanNames = context.getBeanFactory().getBeanNamesForType(ServletWebServerFactory.class);
        return context.getBeanFactory().getBean(beanNames[0], ServletWebServerFactory.class);
    }
}
