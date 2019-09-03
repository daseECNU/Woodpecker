package edu.ecnu.woodpecker.controller;

import org.apache.commons.cli.CommandLine;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.log.WpLog;
import edu.ecnu.woodpecker.tools.Initializable;

/**
 * PostgreSQL
 */
public class PostgreSQLOperation extends TestController implements Initializable, DatabaseOperation
{
    private String databaseUser = null;
    private String databasePassword = null;
    
    private String IP = null;
    private int port;
    
    private PostgreSQLOperation(){}
    
    private static class SingletonHolder
    {
        private static PostgreSQLOperation instance = new PostgreSQLOperation();
    }
    
    public static PostgreSQLOperation getInstance()
    {
        return SingletonHolder.instance;
    }
    
    /**
     * PostgreSQL入口 TODO
     */
    @Override
    public void enter(CommandLine line)
    {
        
    }

    /**
     * 读取PostgreSQL配置文件并初始化
     * 
     * @param configFilePath PG配置文件路径
     */
    @Override
    public void initialize(String configFilePath)
    {
        try
        {
            initialize(this, configFilePath);
        }
        catch (Exception e)
        {
            WpLog.recordLog(LogLevelConstant.ERROR, WpLog.getExceptionInfo(e));
            System.err.println("There are errors when initializing parameters");
            exit(1);
        }
    }

    public String getDatabaseUser()
    {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser)
    {
        this.databaseUser = databaseUser;
    }

    public String getDatabasePassword()
    {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword)
    {
        this.databasePassword = databasePassword;
    }

    public String getIP()
    {
        return IP;
    }

    public void setIP(String IP)
    {
        this.IP = IP;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = Integer.parseInt(port);
    }
    
}
