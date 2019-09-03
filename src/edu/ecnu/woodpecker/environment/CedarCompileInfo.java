package edu.ecnu.woodpecker.environment;

public class CedarCompileInfo
{
    /**
     * 数据库用户名和密码
     */
    private static String databaseUser = null;
    private static String databasePassword = null;

    /**
     * 服务器用户名和密码
     */
    private static String userName = null;
    private static String password = null;

    /**
     * 需要编译的源码所在IP地址
     */
    private static String srcIP = null;

    /**
     * 源码编译后文件所在地址IP
     */
    private static String makeIP = null;

    /**
     * 需要编译的源码所在路径（在home/userName下相对路径）
     */
    private static String srcPath = null;

    /**
     * 源码编译后文件所在路径（在home/userName下相对路径）
     */
    private static String makePath = null;

    /**
     * 编译使用到的CPU核心数
     */
    private static int core;

    /**
     * 是否编译CEDAR的tools文件夹，值有true和false两种，true表示编译
     */
    private static boolean compileTools;
    /**
     * 连接端口号
     */
    private static int connectionPort;

    // private static int port;

    public static String getDatabaseUser()
    {
        return databaseUser;
    }

    public static void setDatabaseUser(String databaseUser)
    {
        CedarCompileInfo.databaseUser = databaseUser;
    }

    public static String getDatabasePassword()
    {
        return databasePassword;
    }

    public static void setDatabasePassword(String databasePassword)
    {
        CedarCompileInfo.databasePassword = databasePassword;
    }

    public static String getSrcIP()
    {
        return srcIP;
    }

    public static void setSrcIP(String srcIP)
    {
        CedarCompileInfo.srcIP = srcIP;
    }

    public static String getMakeIP()
    {
        return makeIP;
    }

    public static void setMakeIP(String makeIP)
    {
        CedarCompileInfo.makeIP = makeIP;
    }

    public static String getSrcPath()
    {
        return srcPath;
    }

    public static void setSrcPath(String srcPath)
    {
        CedarCompileInfo.srcPath = srcPath;
    }

    public static String getMakePath()
    {
        return makePath;
    }

    public static void setMakePath(String makePath)
    {
        CedarCompileInfo.makePath = makePath;
    }

    public static int getCore()
    {
        return core;
    }

    public static void setCore(int core)
    {
        CedarCompileInfo.core = core;
    }

    public static boolean isCompileTools()
    {
        return compileTools;
    }

    public static void setCompileTools(boolean compileTools)
    {
        CedarCompileInfo.compileTools = compileTools;
    }

    public static int getConnectionPort()
    {
        return connectionPort;
    }

    public static void setConnectionPort(int connectionPort)
    {
        CedarCompileInfo.connectionPort = connectionPort;
    }

    public static String getUserName()
    {
        return userName;
    }

    public static void setUserName(String userName)
    {
        CedarCompileInfo.userName = userName;
    }

    public static String getPassword()
    {
        return password;
    }

    public static void setPassword(String password)
    {
        CedarCompileInfo.password = password;
    }

}
