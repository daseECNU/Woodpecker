package edu.ecnu.woodpecker.controller;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import edu.ecnu.woodpecker.constant.CLIParameterConstant;
import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.environment.CedarCompileInfo;
import edu.ecnu.woodpecker.environment.CedarEnvirOperation;
import edu.ecnu.woodpecker.executor.Executor;
import edu.ecnu.woodpecker.executor.Parser;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.systemfunction.CedarSystemOperator;

/**
 * Cedar
 */
public class CedarOperation extends TestController implements DatabaseOperation
{
    /**
     * Cedar database's user name and password
     */
    private static String databaseUser = null;
    private static String databasePassword = null;
    
    private CedarOperation()
    {}

    private static class SingletonHolder
    {
        private static CedarOperation instance = new CedarOperation();
    }

    public static CedarOperation getInstance()
    {
        return SingletonHolder.instance;
    }

    @Override
    public void enter(CommandLine line)
    {
        // 编辑模式和编译选项只对Cedar有效
        boolean editMode = line.hasOption(CLIParameterConstant.EDIT_MODE) ? true : false;
        boolean needCompile = line.hasOption(CLIParameterConstant.COMPILE_CEDAR) ? true : false;
        // 对MySQL和Cedar都有效
        boolean needDeploy = line.hasOption(CLIParameterConstant.DEPLOY) ? true : false;
        try
        {
            initialize(null);
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, "initialize fail, exception: %s", WpLog.getExceptionInfo(e));
        }
        handleOnlyCommandLine(line);
        if (needCompile && !editMode)
        {
            if (!compileCedar())
            {
                // 编译失败，记录日志，结束此次框架的运行
                WpLog.recordLog(LogLevelConstant.ERROR, "Compile Cedar unsuccessfully and stop Woodpecker");
                exit(1);
            }
        }

        boolean disableRestart = line.hasOption(CLIParameterConstant.DISABLE_RESTART) ? true : false;
        // 针对每个组别里的测试案例解析执行
        for (String group : groupSet)
        {
            TestController.currentGroup = group;
            if (editMode || !needDeploy)
            {
                if (editMode)
                {
                    // 编辑模式下初始化deploy的配置文件
                    CedarEnvirOperation.initializeCluster(testEnvironmentConfigPath + FileConstant.CEDAR_COMPILE_FILE_NAME,
                            testEnvironmentConfigPath + group + FileConstant.CONFIG_FILE_SUFFIX, FileConstant.UTF_8);
                }
                else if (!needDeploy)
                {
                    // 第一次运行不部署启动集群，默认此时已经在运行了，仅适合用在编写Cedar测试案例时
                    CedarEnvirOperation.readCompileConf(testEnvironmentConfigPath + group + FileConstant.CONFIG_FILE_SUFFIX, FileConstant.UTF_8);
                }
            }
            // 根据组别部署，启动集群
            if (needDeploy && !editMode && !deployCedar(group))
            {
                // 部署失败，记录日志，结束此次框架的运行，因为已经清空数据和日志了
                WpLog.recordLog(LogLevelConstant.ERROR, "Deploy Cedar unsuccessfully and stop Woodpecker");
                exit(1);
            }
            WpLog.recordTestflow("%s/%s", testEnvironmentConfigPath, group);
            if (needDeploy && !editMode && !startCedar())
            {
                // 启动失败，记录日志，结束此次框架的运行，因为已经清空数据和日志了
                WpLog.recordLog(LogLevelConstant.ERROR, "Start Cedar unsuccessfully and stop Woodpecker");
                exit(1);
            }
            needDeploy = true;

            // 对每个组别内的测试案例解析执行，记录日志
            File[] caseFiles = new File(testCasePath + group).listFiles();
            for (File caseFile : caseFiles)
            {
                WpLog.recordTestflow("caseid:%s/%s", group, caseFile.getName());
                boolean retrySucceed = false;
                File midResult = Parser.parse(caseFile);
                if (midResult == null)
                {
                    // 解析失败，记录日志，此案例跳过
                    WpLog.recordLog(LogLevelConstant.ERROR,
                            "%s parse unsuccessfully and skip", caseFile.getName());
                    continue;
                }

                boolean isPass = Executor.execute(midResult, Parser.getVarValueMap(), Parser.getVarTypeMap());
                if (!isPass)
                {
                    // 首次运行没通过，记录日志
                    WpLog.recordLog(LogLevelConstant.ERROR,
                            "%s execute unsuccessfully first time", caseFile.getName());
                    // 重试retryCount次
                    for (int i = 0; i < retryCount; i++)
                    {
                        if (!disableRestart)
                        {
                            // 杀集群，清理上次运行失败产生的数据，转储Cedar日志，重新启动集群
//                            closeCedar(TestStatusConstant.FAIL);
//                            startCedar();
                        }
                        // 重试执行
                        WpLog.recordTestflow("caseid:%s/%s", group, caseFile.getName());
                        isPass = Executor.execute(midResult, Parser.getVarValueMap(), Parser.getVarTypeMap());
                        if (isPass)
                        {
                            // 记录日志，第i+1次重试成功
                            WpLog.recordLog(LogLevelConstant.INFO, "%s execute successfully after retry %d times", caseFile.getName(),
                                    i + 1);
                            retrySucceed = true;
                            break;
                        }
                    }

                    if (!retrySucceed)
                    {
                        // 记录日志“重试retryCount次依旧没有通过，跳过此case”
                        WpLog.recordLog(LogLevelConstant.ERROR,
                                "%s execute unsuccessfully after retry %d times", caseFile.getName(), retryCount);
                        if (!disableRestart)
                        {
                            // 杀集群，清数据，转储Cedar日志，重启集群
//                            closeCedar(TestStatusConstant.FAIL);
//                            startCedar();
                        }
                        continue;
                    }
                }
                else
                {
                    // 记录日志，此caseFile运行通过
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "%s execute successfully first time", caseFile.getName());
                }// end of if(!isPass)

                if (!disableRestart)
                {
                    // 一个案例执行结束，判断集群是否正常，不正常则杀集群，清数据、转储Cedar日志，重启集群，正常则不变
//                    int clusterState = CedarSystemOperator.isClusterAvailable();
                    int clusterState =0;
//                    if (clusterState != 0)
//                    {
//                        // 集群不正常
//                        closeCedar(TestStatusConstant.FAIL);
//                        startCedar();
//                    }
                }
            }// end of for (File caseFile : caseFiles)

            if (!editMode)
            {
                // 一组case结束，杀集群，清数据，清日志
                // closeCedar(TestStatusConstant.PASS);
            }

        }// end of for (String group : groupSet)

        WpLog.recordLog(LogLevelConstant.INFO, "Framework has executed all test cases and prepares to stop");
        // 初始化并生成报表
        WpLog.generateReport();
        WpLog.generateStressReport();
        if (!editMode)
        {
            // Operator.initializeCEDAR("deploy");
        }
    }

    @Override
    public void initialize(String configFilePath)
    {
        databaseUser = null == CedarCompileInfo.getDatabaseUser() ? ConfigConstant.ADMIN_NAME : CedarCompileInfo.getDatabaseUser();
        databasePassword = null == CedarCompileInfo.getDatabasePassword() ? ConfigConstant.ADMIN_PASSWORD : CedarCompileInfo.getDatabasePassword();
    }

    /**
     * 编译，如果失败则重试retryCount次，都失败则返回false
     * 
     * @return 编译失败返回false
     */
    private static boolean compileCedar()
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Start to compile Cedar");
        // 首次编译
        boolean isSuccessful = CedarEnvirOperation.compileCEDAR();
        if (!isSuccessful)
        {
            // 重试，记录日志
            WpLog.recordLog(LogLevelConstant.ERROR, "Compile Cedar unsuccessfully first time");
            for (int i = 0; i < retryCount; i++)
            {
                isSuccessful = CedarEnvirOperation.compileCEDAR();
                if (isSuccessful)
                {
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "Compile Cedar successfully after retry %d times", i + 1);
                    return true;
                }
            }

            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Compile Cedar unsuccessfully after retry %d times", retryCount);
            return false;
        }
        WpLog.recordLog(LogLevelConstant.INFO, "Compile Cedar successfully");
        return true;
    }

    /**
     * 部署Cedar集群，如果失败则重试retryCount次，都失败则返回false
     * 
     * @param groupName 每个测试组别的名字
     * @return 部署成功为true
     */
    private static boolean deployCedar(String groupName)
    {
        WpLog.recordLog(LogLevelConstant.INFO, "Start to deploy Cedar");
        String deployConfigFilePath = testEnvironmentConfigPath + groupName + FileConstant.CONFIG_FILE_SUFFIX;
        boolean isSuccessful = CedarEnvirOperation.deployCEDAR(deployConfigFilePath, FileConstant.UTF_8);
        if (!isSuccessful)
        {
            // 重试
            WpLog.recordLog(LogLevelConstant.ERROR, "Deploy Cedar unsuccessfully first time");
            for (int i = 0; i < retryCount; i++)
            {
                isSuccessful = CedarEnvirOperation.deployCEDAR(deployConfigFilePath, FileConstant.UTF_8);
                if (isSuccessful)
                {
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "Deploy Cedar successfully after retry %d times", i + 1);
                    return true;
                }
            }
            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Deploy Cedar unsuccessfully after retry %d times", retryCount);
            return false;
        }
        WpLog.recordLog(LogLevelConstant.INFO, "Deploy Cedar successfully");
        return true;
    }

    /**
     * 启动Cedar集群，如果失败则重试retryCount次，都失败则返回false
     * 
     * @return 启动成功为true
     */
    private static boolean startCedar()
    {
        boolean isSuccessful = CedarEnvirOperation.startCEDAR();
        if (!isSuccessful)
        {
            // 重试
            WpLog.recordLog(LogLevelConstant.ERROR, "Start Cedar unsuccessfully first time");
            for (int i = 0; i < retryCount; i++)
            {
                isSuccessful = CedarEnvirOperation.startCEDAR();
                if (isSuccessful)
                {
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "Start Cedar successfully after retry %d times", i + 1);
                    return true;
                }
            }

            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Start Cedar unsuccessfully after retry %d times", retryCount);
            return false;
        }

        WpLog.recordLog(LogLevelConstant.INFO, "Start Cedar successfully");
        return true;
    }

    /**
     * 关闭Cedar集群，如果失败则重试retryCount次，都失败则返回false
     * 
     * @param type 集群关闭类型：normal/unnormal
     * @return 关闭成功为true
     */
    private static boolean closeCedar(String type)
    {
        boolean isSuccessful = CedarEnvirOperation.closeCEDAR(type);
        if (!isSuccessful)
        {
            // 重试
            WpLog.recordLog(LogLevelConstant.ERROR, "Close Cedar unsuccessfully first time");
            for (int i = 0; i < retryCount; i++)
            {
                isSuccessful = CedarEnvirOperation.closeCEDAR(type);
                if (isSuccessful)
                {
                    WpLog.recordLog(LogLevelConstant.INFO,
                            "Close Cedar successfully after retry %d times", i + 1);
                    return true;
                }
            }

            WpLog.recordLog(LogLevelConstant.ERROR,
                    "Close Cedar unsuccessfully after retry %d times", retryCount);
            return false;
        }

        WpLog.recordLog(LogLevelConstant.INFO, "Close Cedar successfully");
        return true;
    }

    /**
     * handle -compile_cedar_only and -deploy_only command
     * 
     * @param line
     */
    private static void handleOnlyCommandLine(CommandLine line)
    {
        if (line.hasOption(CLIParameterConstant.COMPILE_CEDAR_ONLY))
        {
            if (!compileCedar())
            {
                // compile unsuccessfully and exit framework
                WpLog.recordLog(LogLevelConstant.ERROR, "Compile Cedar unsuccessfully and stop Woodpecker");
                exit(1);
            }
            WpLog.recordLog(LogLevelConstant.INFO, "Compile Cedar successfully");
            if (!line.hasOption(CLIParameterConstant.DEPLOY_ONLY))
                exit(0);
        }
        if (line.hasOption(CLIParameterConstant.DEPLOY_ONLY))
        {
            String group = groupSet.iterator().next(); // only deploy first group
            if (!deployCedar(group) || !startCedar())
            {
                // deploy unsuccessfully and exit framework
                WpLog.recordLog(LogLevelConstant.ERROR, "Deploy Cedar unsuccessfully and stop Woodpecker");
                exit(1);
            }
            WpLog.recordLog(LogLevelConstant.INFO, "Deploy Cedar successfully");
            exit(0);
        }
    }

    public static void setDatabaseUser(String userName)
    {
        CedarOperation.databaseUser = userName;
    }

    public static void setDatabasePassword(String password)
    {
        CedarOperation.databasePassword = password;
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
