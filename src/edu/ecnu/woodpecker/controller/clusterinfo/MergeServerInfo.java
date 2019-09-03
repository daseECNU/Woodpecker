package edu.ecnu.woodpecker.controller.clusterinfo;

/**
 * MergeServer信息类
 * 
 */
public class MergeServerInfo extends ServerInfo
{
    /**
     * MergeServer服务端口
     */
    private int servicePort;

    /**
     * MergeServer的MySQL协议端口
     */
    private int MySQLPort;

    private boolean isListener;

    public MergeServerInfo()
    {
        // 默认初始化，端口号是不能小于0的
        this.servicePort = -1;
        this.MySQLPort = -1;
        this.isListener = false;
    }

    public MergeServerInfo(int servicePort, int MySQLPort, boolean isListener)
    {
        this.servicePort = servicePort;
        this.MySQLPort = MySQLPort;
        this.isListener = isListener;
    }

    public boolean isListener()
    {
        return isListener;
    }

    public void setListener(boolean isListener)
    {
        this.isListener = isListener;
    }

    public int getServicePort()
    {
        return servicePort;
    }

    public void setServicePort(int servicePort)
    {
        this.servicePort = servicePort;
    }

    public int getMySQLPort()
    {
        return MySQLPort;
    }

    public void setMySQLPort(int MySQLPort)
    {
        this.MySQLPort = MySQLPort;
    }
}