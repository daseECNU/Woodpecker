package edu.ecnu.woodpecker.controller.clusterinfo;

/**
 * ChunkServer信息类
 * 
 */
public class ChunkServerInfo extends ServerInfo
{
    /**
     * ChunkServer服务端口
     */
    private int servicePort;

    /**
     * ChunkServer APP名称
     */
    private String appName = null;

    public ChunkServerInfo()
    {
        servicePort = -1;
        appName = "";
    }

    /**
     * @param servicePort 服务端口
     * @param appName APP名称
     */
    public ChunkServerInfo(int servicePort, String appName)
    {
        this.servicePort = servicePort;
        this.appName = appName;
    }

    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    public void setServicePort(int servicePort)
    {
        this.servicePort = servicePort;
    }

    public int getServicePort()
    {
        return this.servicePort;
    }
}
