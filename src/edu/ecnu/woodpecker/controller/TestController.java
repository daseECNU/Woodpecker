package edu.ecnu.woodpecker.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import edu.ecnu.woodpecker.constant.CLIParameterConstant;
import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.DataValueConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.environment.CedarEnvirOperation;
import edu.ecnu.woodpecker.executor.Parser;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.sql.DbmsBrand;
import edu.ecnu.woodpecker.tools.CedarKiller;
import edu.ecnu.woodpecker.tools.CedarMerge;
import edu.ecnu.woodpecker.tools.CedarReelect;
import edu.ecnu.woodpecker.tools.SyntaxChecker;
import edu.ecnu.woodpecker.util.ProxyInfo;
import edu.ecnu.woodpecker.util.Util;

/**
 * 测试控制器 控制所有脚本文件（测试案例）组别间的执行顺序和组别内的执行顺序
 * 
 */
public class TestController
{
    /**
     * 流程配置文件的相对路径，不能更改
     */
    private final static String WORKFLOW_CONFIG_PATH = "./config/Woodpecker.conf";

    /**
     * 解析器生成的中间结果集文件存放路径，不可更改
     */
    private final static String MIDDLE_RESULT_PATH = "./middle_result/";

    /**
     * 数据库名称，全小写，没有空格
     */
    private static DbmsBrand database = null;

    /**
     * 测试环境模块的配置文件路径，路径最后一定有'/'
     */
    protected static String testEnvironmentConfigPath = null;

    /**
     * 所有测试案例存放的路径，路径最后一定有'/'
     */
    protected static String testCasePath = null;

    /**
     * 理想结果集文件存放的路径，路径最后一定有'/'
     */
    private static String idealResultSetPath = null;

    /**
     * 数据库实例文件的路径，实例文件的后缀名为 .dbi
     */
    private static String databaseInstancePath = null;

    /**
     * 服务器账户和密码
     */
    private static String serverUserName = null;
    private static String serverPassword = null;

    /**
     * 压测模块负载配置文件和jar包所在目录，最后一定有'/'
     */
    private static String stressPath = null;

    /**
     * Woodpecker所在机器的IP（当Woodpecker在服务器时此配置项才有用）
     */
    private static String woodpeckerIP = null;

    /**
     * 测试报告存放路径
     */
    private static String reportPath = null;

    /**
     * case execute fail
     */
    protected static int retryCount;

    /**
     * 存放将执行的测试组别的名称；当前正在执行的案例的组别名称
     */
    protected static Set<String> groupSet = new HashSet<String>(10);
    protected static String currentGroup = null;
    
    private static DatabaseOperation dbOperation = null;

    /**
     * entry of test controller
     */
    public static void start(CommandLine line)
    {
        // 初始化参数
        initSysParameter();
        initFlowParameter();
        // 生成与测试案例文件夹一样的目录结构，用于存放中间结果集
        try
        {
            Parser.geneMidResultDirectory(testCasePath, MIDDLE_RESULT_PATH);
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            System.err.println("There are errors when generating directory structure");
            exit(2);
        }

        if (database.getBrand().equals(ConfigConstant.CEDAR))
        {
            // Cedar
            WpLog.recordLog(LogLevelConstant.INFO, "DBMS is Cedar");
            dbOperation = CedarOperation.getInstance();
            boolean confCorrect = (line.hasOption(CLIParameterConstant.EDIT_MODE) ? true : false) ? true
                    : CedarEnvirOperation.readCompileConf(testEnvironmentConfigPath + FileConstant.CEDAR_COMPILE_FILE_NAME, FileConstant.UTF_8);
            if (confCorrect)
            {
                dbOperation.enter(line);
            }
            else
            {
                WpLog.recordLog(LogLevelConstant.ERROR, "There are errors in config file, stop Woodpecker");
                exit(1);
            }
        }
        else if (database.getBrand().equals(ConfigConstant.MYSQL))
        {
            // MySQL
            WpLog.recordLog(LogLevelConstant.INFO, "DBMS is MySQL");
            dbOperation = MySQLOperation.getInstance();
            dbOperation.initialize(testEnvironmentConfigPath + FileConstant.MYSQL_CONFIG_FILE_NAME);
            dbOperation.enter(line);
        }
        else if (database.getBrand().equals(ConfigConstant.POSTGRESQL))
        {
            // PostgreSQL
            WpLog.recordLog(LogLevelConstant.INFO, "DBMS is PostgreSQL");
            dbOperation = PostgreSQLOperation.getInstance();
            dbOperation.initialize(testEnvironmentConfigPath + FileConstant.POSTGRESQL_CONFIG_FILE_NAME);
            dbOperation.enter(line);
        }
        else
        {
            // wrong dbms type
            WpLog.recordLog(LogLevelConstant.ERROR, "DBMS type in config file is wrong");
            exit(1);
        }
    }

    /**
     * 根据系统配置文件里的内容，初始化系统参数
     */
    private static void initSysParameter()
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(WORKFLOW_CONFIG_PATH), FileConstant.UTF_8)))
        {
            WpLog.recordLog(LogLevelConstant.INFO, "Initialize woodpecker parameters");
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // 空行或者注释行
                if (line.matches("^(#{1}.*)|(\\s*+)$"))
                    continue;
                // 匹配到"---------"分隔行，说明系统参数读取结束
                if (line.matches("\\s*[-]+\\s*"))
                    break;
                WpLog.recordLog(LogLevelConstant.INFO, "init parameter: %s", line);
                // 获取配置项对应的函数名字和配置项的值
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
                String confValue = line.substring(line.indexOf(SignConstant.ASSIGNMENT_CHAR) + 1).trim();
                int index = confValue.indexOf(SignConstant.SHARP_CHAR);
                confValue = index == -1 ? confValue : confValue.substring(0, index).trim();
                stringBuilder.delete(0, stringBuilder.length());
                // 使用反射，所有反射调用的set方法要求参数的类型是String
                Method method = TestController.class.getMethod(methodName, String.class);
                method.invoke(null, confValue);
            }
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            System.err.println("There are errors when initializing parameters");
            exit(3);
        }
        // Initialize some parameters which aren't setted in Woodpecker.conf
        if (woodpeckerIP == null)
            woodpeckerIP = "127.0.0.1";
    }

    /**
     * 初始化流程参数 求此次框架运行所需要执行的所有组别的名称，结果存放于类成员变量groupSet中 初始化重试次数
     */
    private static void initFlowParameter()
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Initialize work flow parameters");
        // 是否按照默认流程执行，true为默认流程
        boolean defaultOrNot = true;
        // 读取流程配置文件并决定是按照默认流程还是指定流程执行
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(WORKFLOW_CONFIG_PATH), FileConstant.UTF_8));)
        {
            String line = null;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // 匹配到"-----------"分隔行
                if (line.matches("^\\s*+-+\\s*+"))
                    break;
            }
            // 读取到流程参数部分
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                // 当前读取行是注释或空行
                if (line.matches("^(#{1}.*)|(\\s*+)$"))
                    continue;
                WpLog.recordLog(LogLevelConstant.INFO, "init parameter: %s", line);
                // 初始化重试次数
                if (line.startsWith(ConfigConstant.RETRY_COUNT))
                {
                    String[] parts = line.split("=|#");
                    retryCount = Integer.parseInt(parts[1].trim());
                    continue;
                }
                // 求测试组别
                if (defaultOrNot && line.startsWith(ConfigConstant.DEFAULT_OR_NOT))
                {
                    int index = line.indexOf(SignConstant.SHARP_CHAR);
                    line = index == -1 ? line : line.substring(0, index).trim();
                    if (line.endsWith(DataValueConstant.TRUE))
                        break;
                    else
                    {
                        defaultOrNot = false;
                        continue;
                    }
                }
                // 不按照默认流程，求指定组别的名字
                if (line.startsWith(ConfigConstant.CASE_GROUP_NAME))
                {
                    line = line.substring(line.indexOf(SignConstant.ASSIGNMENT_CHAR) + 1);
                    int index = line.indexOf(SignConstant.DOUBLE_SHARP);
                    line = index == -1 ? line : line.substring(0, index).trim();
                    String[] names = line.split(SignConstant.COMMA_STR);
                    for (String name : names)
                        groupSet.add(name.trim());
                }
            }
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            System.err.println("There are errors when initializing parameters");
            exit(3);
        }

        if (defaultOrNot)
        {
            // 求默认流程，即所有组别的名称
            File[] files = new File(testCasePath).listFiles();
            for (File file : files)
            {
                if (file.isDirectory())
                    groupSet.add(file.getName().trim());
            }
        }
        WpLog.recordLog(LogLevelConstant.INFO, "execute case group: %s", groupSet.toString());
    }
    
    /**
     * use tools provided by Woodpecker
     * 
     * @param line
     */
    private static void useTools(CommandLine line) throws Exception
    {
        // Check conflict between options
        if (OptionsHasConflict(line))
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "There is conflict between input options");
            exit(1);
        }
        if(line.hasOption(CLIParameterConstant.HELP))
        {
            WoodpeckerCommandLine.printUsage();
            exit(0);
        }
        if(line.hasOption(CLIParameterConstant.MERGE))
        {
            new CedarMerge().start();
            exit(0);
        }
        if (line.hasOption(CLIParameterConstant.KILL_CEDAR))
        {
            new CedarKiller().start();
            exit(0);
        }
        if (line.hasOption(CLIParameterConstant.REELECT))
        {
            new CedarReelect().start(false);
            exit(0);
        }
        if (line.hasOption(CLIParameterConstant.GET_OBI_ROLE))
        {
            new CedarReelect().start(true);
            exit(0);
        }
        if (line.hasOption(CLIParameterConstant.SYNTAX_CHECK))
        {
            initializeParameter();
            new SyntaxChecker().check(line.getOptionValue(CLIParameterConstant.SYNTAX_CHECK).trim());
            exit(0);
        }
    }
    
    /**
     * Check conflict between options, can't ensure that find all conflicts
     * 
     * @return true if exist conflicts
     */
    private static boolean OptionsHasConflict(CommandLine line)
    {
        // For multiple usage
        Supplier<Stream<Option>> streamSupplier = () -> Stream.of(line.getOptions());
        // Exclusive option
        if (streamSupplier.get()
                .anyMatch(ele -> ele.getOpt().equals(CLIParameterConstant.HELP) || ele.getOpt().equals(CLIParameterConstant.MERGE)
                        || ele.getOpt().equals(CLIParameterConstant.REELECT) || ele.getOpt().equals(CLIParameterConstant.KILL_CEDAR)
                        || ele.getOpt().equals(CLIParameterConstant.GET_OBI_ROLE)
                        || ele.getOpt().equals(CLIParameterConstant.SYNTAX_CHECK)))
        {
            return streamSupplier.get().count() > 1 ? true : false;
        }
        // Combination option
        if (streamSupplier.get().anyMatch(ele -> ele.getOpt().equals(CLIParameterConstant.COMPILE_CEDAR_ONLY)
                || ele.getOpt().equals(CLIParameterConstant.DEPLOY_ONLY)))
        {
            return streamSupplier.get().count() > 2 ? true : false;
        }
        return false;
    }

    /**
     * Exit Woodpecker framework
     * 
     * @param status
     */
    protected static void exit(int status)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "-------------stop Woodpecker-------------");
        System.exit(status);
    }
    
    public static void initializeParameter() throws Exception
    {
        initSysParameter();
        initFlowParameter();
    }
    
    public static DbmsBrand getDatabase()
    {
        return database;
    }

    public static String getTestEnvironmentConfigPath()
    {
        return testEnvironmentConfigPath;
    }

    public static String getServerUserName()
    {
        return serverUserName;
    }

    public static String getServerPassword()
    {
        return serverPassword;
    }

    public static String getDatabaseInstancePath()
    {
        return databaseInstancePath;
    }

    public static String getIdealResultSetPath()
    {
        return idealResultSetPath;
    }

    public static String getStressPath()
    {
        return stressPath;
    }

    public static String getWoodpeckerIP()
    {
        return woodpeckerIP;
    }

    public static String getReportPath()
    {
        return reportPath;
    }

    public static String getCurrentGroup()
    {
        return currentGroup;
    }

    public static Set<String> getGroupSet()
    {
        return groupSet;
    }
    public static void setIdealResultSetPath(String idealResultSetPath)
    {
        if (idealResultSetPath.matches("\".*\""))
            idealResultSetPath = idealResultSetPath.substring(1, idealResultSetPath.length() - 1);
        TestController.idealResultSetPath = idealResultSetPath + FileConstant.FILE_SEPARATOR_CHAR;
    }

    public static void setDatabaseInstancePath(String databaseInstancePath)
    {
        if (databaseInstancePath.matches("\".*\""))
            databaseInstancePath = databaseInstancePath.substring(1, databaseInstancePath.length() - 1);
        TestController.databaseInstancePath = databaseInstancePath + FileConstant.FILE_SEPARATOR_CHAR;
    }

    public static void setCurrentGroup(String currentGroup)
    {
        TestController.currentGroup = currentGroup;
    }

    public static void setDatabase(String database)
    {
        TestController.database = DbmsBrand.of(database);
    }

    public static void setTestEnvironmentConfigPath(String testEnvironmentConfigPath)
    {
        if (testEnvironmentConfigPath.matches("\".*\""))
            testEnvironmentConfigPath = testEnvironmentConfigPath.substring(1, testEnvironmentConfigPath.length() - 1);
        TestController.testEnvironmentConfigPath = testEnvironmentConfigPath + FileConstant.FILE_SEPARATOR_CHAR;
    }

    public static void setTestCasePath(String testCasePath)
    {
        if (testCasePath.matches("\".*\""))
            testCasePath = testCasePath.substring(1, testCasePath.length() - 1);
        TestController.testCasePath = testCasePath + FileConstant.FILE_SEPARATOR_CHAR;
    }

    public static void setServerUserName(String serverUserName)
    {
        TestController.serverUserName = serverUserName;
    }

    public static void setServerPassword(String serverPassword)
    {
        TestController.serverPassword = serverPassword;
    }

    public static void setStressPath(String stressPath)
    {
        if (stressPath.matches("\".*\""))
            stressPath = stressPath.substring(1, stressPath.length() - 1);
        TestController.stressPath = stressPath + FileConstant.FILE_SEPARATOR_CHAR;
    }

    public static void setWoodpeckerIP(String woodpeckerIP)
    {
        TestController.woodpeckerIP = woodpeckerIP;
    }

    public static void setReportPath(String reportPath)
    {
        if (reportPath.matches("\".*\""))
            reportPath = reportPath.substring(1, reportPath.length() - 1);
        TestController.reportPath = reportPath + FileConstant.FILE_SEPARATOR_CHAR;
    }
    
    /**
     * Set the value of ProxyInfo class
     * 
     * @param input Include all values
     */
    public static void setProxyInfo(String input) throws Exception
    {
        // Index 0 is proxy type, 1 is host, 2 is port, 3 is user, 4 is password
        String[] values = Util.removeBlankElement(input.split(SignConstant.COMMA_STR));
        ProxyInfo.setProxyType(values[0]);
        ProxyInfo.setProxyHost(values[1].substring(1, values[1].length() - 1));
        ProxyInfo.setProxyPort(values[2]);
        ProxyInfo.setUser(values[3]);
        ProxyInfo.setPassword(values[4]);
        ProxyInfo.setGlobalProxyServer();
    }

    /**
     * Entry of woodpecker
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception
    {
        WpLog.recordLog(LogLevelConstant.INFO, "-------------start Woodpecker-------------");
        // use Apache Commons CLI parse parameters
        WoodpeckerCommandLine.setOption(args);
        CommandLine line = WoodpeckerCommandLine.getCommandLine();
        useTools(line);
        TestController.start(line);
        exit(0);
    }
}
