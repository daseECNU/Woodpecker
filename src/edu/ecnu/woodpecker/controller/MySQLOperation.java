package edu.ecnu.woodpecker.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import org.apache.commons.cli.CommandLine;

import edu.ecnu.woodpecker.constant.CLIParameterConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.Parser;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.util.Util;

/**
 * MySQL数据库的操作类
 * 
 */
public class MySQLOperation extends TestController implements DatabaseOperation
{
    /**
     * MySQL安装方式，true则为发行版，false是RPM安装版
     */
    private static boolean release;

    /**
     * MySQL安装目录，RPM版可为空，release版不能为空
     */
    private static String MySQLRoot = null;

    /**
     * MySQL所在server的账户和密码
     */
    private static String serverUser = null;
    private static String serverPassword = null;

    /**
     * MySQL的账户和密码
     */
    private static String databaseUser = null;
    private static String databasePassword = null;

    /**
     * MySQL服务器的IP和端口
     */
    private static String IP = null;
    private static int port;
    
    private MySQLOperation(){}
    
    private static class SingletonHolder
    {
        private static MySQLOperation instance = new MySQLOperation();
    }
    
    public static MySQLOperation getInstance()
    {
        return SingletonHolder.instance;
    }

    @Override
    /**
     * 读取MySQL配置文件信息并初始化
     * 
     * @param configFilePath MySQL配置文件路径
     */
    public void initialize(String configFilePath)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Initialize MySQL parameters");
        serverUser = TestController.getServerUserName();
        serverPassword = TestController.getServerPassword();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(configFilePath), FileConstant.UTF_8)))
        {
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // 空行或者注释行
                if (line.matches("^(#{1}.*)|(\\s*+)$"))
                    continue;
                // 获取配置项对应的函数名字
                stringBuilder.append("set").append(line.substring(0, line.indexOf(SignConstant.ASSIGNMENT_CHAR)).trim());
                stringBuilder.setCharAt(3, Character.toUpperCase(stringBuilder.charAt(3)));
                for (int fromIndex = 0; fromIndex < stringBuilder.length();)
                {
                    fromIndex = stringBuilder.indexOf(SignConstant.UNDERLINE_STR, fromIndex);
                    if (fromIndex == -1)
                        break;
                    stringBuilder.deleteCharAt(fromIndex);
                    stringBuilder.setCharAt(fromIndex, Character.toUpperCase(stringBuilder.charAt(fromIndex)));
                }
                String methodName = stringBuilder.toString();
                // 获取配置项的值
                String confValue = line.substring(line.indexOf(SignConstant.ASSIGNMENT_CHAR) + 1).trim();
                int index = confValue.indexOf(SignConstant.SHARP_CHAR);
                confValue = index == -1 ? confValue : confValue.substring(0, index).trim();
                stringBuilder.delete(0, stringBuilder.length());
                // 使用反射，所有反射调用的set方法要求参数的类型是String
                Method method = MySQLOperation.class.getMethod(methodName, String.class);
                method.invoke(null, confValue);
            }
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            System.err.println("There are errors when initializing parameters");
            exit(1);
        }
        // 配置文件里没有配置MySQL Root路径则赋为空
        MySQLRoot = MySQLRoot == null ? "" : MySQLRoot;
    }

    @Override
    public void enter(CommandLine line)
    {
        // 对MySQL和Cedar都有效
        boolean needStart = line.hasOption(CLIParameterConstant.DEPLOY) ? true : false;
        // MySQL运行结束是否关闭
        boolean needClose = line.hasOption(CLIParameterConstant.CLOSE_MYSQL) ? true : false;
        // 启动MySQL
        if (needStart && !startMySQL())
        {
            // 启动MySQL失败，结束框架运行
            WpLog.recordLog(LogLevelConstant.ERROR, "Start MySQL unsuccessfully and stop Woodpecker");
            exit(1);
        }
        // 由于MySQL没有集群等概念，不需要像cedar那样针对每组测试案例进行启动和关闭
        for (String group : groupSet)
        {
            TestController.currentGroup = group;
            File[] caseFiles = new File(testCasePath + group).listFiles();
            for (File caseFile : caseFiles)
            {
                WpLog.recordTestflow("caseid: %s/%s", group, caseFile.getName());
                File midResult = Parser.parse(caseFile);
                if (midResult == null)
                {
                    // 解析失败，此案例跳过
                    WpLog.recordLog(LogLevelConstant.ERROR, "%s parse unsuccessfully and skip", caseFile.getName());
                    continue;
                }

                boolean retrySucceed = false;
                boolean isPass = Executor.execute(midResult, Parser.getVarValueMap(), Parser.getVarTypeMap());
                if (!isPass)
                {
                    // 首次运行没通过
                    WpLog.recordLog(LogLevelConstant.ERROR,
                            "%s execute unsuccessfully first time", caseFile.getName());
                    // 重试retryCount次
                    for (int i = 0; i < retryCount; i++)
                    {
                        // 关闭MySQL，清理上次运行失败产生的数据，转储MySQL日志，重新启动MySQL
                        if (needClose)
                        {
                            closeMySQL(TestStatusConstant.FAIL);
                            startMySQL();
                        }
                        // 重试执行
                        WpLog.recordTestflow("caseid: %s/%s", group, caseFile.getName());
                        isPass = Executor.execute(midResult, Parser.getVarValueMap(), Parser.getVarTypeMap());
                        if (isPass)
                        {
                            // 第i+1次重试成功
                            WpLog.recordLog(LogLevelConstant.INFO,
                                    "%s execute successfully after retry %d times", caseFile.getName(), i + 1);
                            retrySucceed = true;
                            break;
                        }
                    }

                    if (!retrySucceed)
                    {
                        // 重试retryCount次依旧没有通过，跳过此case
                        WpLog.recordLog(LogLevelConstant.ERROR,
                                "%s execute unsuccessfully after retry %d times", caseFile.getName(), retryCount);
                        // 关闭MySQL，清数据，转储MySQL日志，重启MySQL
                        if (needClose)
                        {
                            closeMySQL(TestStatusConstant.FAIL);
                            startMySQL();
                        }
                        continue;
                    }
                }
                else
                {
                    // 此案例通过
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "%s execute successfully first time", caseFile.getName());
                }
            }
        }
        // 记录日志“此次框架运行结束”
        WpLog.recordLog(LogLevelConstant.INFO, "Framework has executed all test cases and prepares to stop");
        // 初始化并生成报表
        WpLog.generateReport();
        WpLog.generateStressReport();
        if (needClose)
        {
            closeMySQL(TestStatusConstant.PASS);
        }
    }

    /**
     * 启动MySQL，重试都不成功返回false
     * 
     * @return 启动成功为true，失败为false
     */
    private static boolean startMySQL()
    {
        if (MySQLOperation.release)
        {
            // 发行版直接去安装目录启动 TODO

            return true;
        }

        // RPM版使用系统命令启动，并且用户需要有权限
        String cmd = "service mysqld start";
        for (int i = 0; i < retryCount + 1; i++)
        {
            String[] result = execShell(IP, serverUser, serverPassword, cmd);
            if (result[0].matches("Starting\\s+mysqld:\\s+\\[\\s+OK\\s+]\\s*"))
            {
                System.out.println("启动MySQL成功");
                return true;
            }
            else if (result.length != 1 || result[0].matches("Starting\\s+mysqld:\\s+\\[\\s*FAILED\\s*]\\s*"))
            {
                System.out.println("启动MySQL失败");
            }
        }
        return false;
    }

    /**
     * 关闭MySQL，重试都不成功返回false，正常关闭时清空数据和日志文件，不正常时转储日志文件
     * 
     * @param type 关闭类型：normal/unnormal
     * @return 关闭成功为true，失败为false
     */
    private static boolean closeMySQL(String type)
    {
        if (MySQLOperation.release)
        {
            // 发行版直接去安装目录关闭 TODO

            return true;
        }

        // RPM版使用系统命令关闭
        String cmd = "service mysqld stop";
        if (type.equals(TestStatusConstant.PASS))
        {
            boolean isStopSuccessful = false;
            for (int i = 0; i < retryCount + 1; i++)
            {
                String[] result = execShell(IP, serverUser, serverPassword, cmd);
                if (result[0].matches("Stopping\\s+mysqld:\\s+\\[\\s+OK\\s+]\\s*"))
                {
                    System.out.println("关闭MySQL成功");
                    isStopSuccessful = true;
                }
                else if (result.length != 1 || result[0].matches("Stopping\\s+mysqld:\\s+\\[\\s*FAILED\\s*]\\s*"))
                {
                    System.out.println("关闭MySQL失败");
                }
            }
            if (!isStopSuccessful)
            {
                return false;
            }

            // 清空数据和日志文件
            String rmDataLog = "rm -rf /var/lib/mysql/woodpecker && rm -f /var/log/mysqld.log";
            execShell(IP, serverUser, serverPassword, rmDataLog);
            return true;
        }
        else if (type.equals(TestStatusConstant.FAIL))
        {
            boolean isStopSuccessful = false;
            for (int i = 0; i < retryCount + 1; i++)
            {
                String[] result = execShell(IP, serverUser, serverPassword, cmd);
                if (result[0].matches("Stopping\\s+mysqld:\\s+\\[\\s+OK\\s+]\\s*"))
                {
                    System.out.println("关闭MySQL成功");
                    isStopSuccessful = true;
                }
                else if (result.length != 1 || result[0].matches("Stopping\\s+mysqld:\\s+\\[\\s*FAILED\\s*]\\s*"))
                {
                    System.out.println("关闭MySQL失败");
                }
            }
            if (!isStopSuccessful)
            {
                return false;
            }

            // 转储日志文件，清空数据和日志文件
            String storeLog = "scp /var/log/mysqld.log " + serverUser + "@" + IP + ":~" + serverUser + "/MySQL_log_store_by_Woodpecker";
            String rmDataLog = "rm -rf /var/lib/mysql/woodpecker && rm -f /var/log/mysqld.log";
            execShell(IP, serverUser, serverPassword, storeLog + " && " + rmDataLog);
            return true;
        }
        return false;
    }

    /**
     * Execute command in specified host
     * 
     * @param host The IP of host
     * @param serverUser
     * @param serverPassword
     * @param command 
     * @return The information of command
     */
    private static String[] execShell(String host, String serverUser, String serverPassword, String command)
    {
        String result = Util.exec(host, serverUser, serverPassword, 22, command);
        return result.split(FileConstant.WIN_LINE_FEED_STR);
    }

    public static String getIP()
    {
        return IP;
    }

    public static void setIP(String IP)
    {
        MySQLOperation.IP = IP;
    }

    public static int getPort()
    {
        return port;
    }

    public static void setPort(String port)
    {
        MySQLOperation.port = Integer.parseInt(port);
    }

    public static void setDatabaseUser(String databaseUser)
    {
        MySQLOperation.databaseUser = databaseUser;
    }

    public static void setDatabasePassword(String databasePassword)
    {
        MySQLOperation.databasePassword = databasePassword;
    }

    public static void setRelease(String release)
    {
        MySQLOperation.release = Boolean.parseBoolean(release);
    }

    public static void setMySQLRoot(String MySQLRoot)
    {
        MySQLOperation.MySQLRoot = MySQLRoot;
    }

    public static String getDatabaseUser()
    {
        return databaseUser;
    }

    public static String getDatabasePassword()
    {
        return databasePassword;
    }
    
}

/**
 * 测试案例执行状态常量类
 * 
 */
final class TestStatusConstant
{
    public final static String PASS = "normal";
    public final static String FAIL = "unnormal";
}