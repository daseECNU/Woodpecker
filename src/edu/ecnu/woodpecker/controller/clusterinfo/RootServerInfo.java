package edu.ecnu.woodpecker.controller.clusterinfo;

/**
 * RootServer信息类
 * 
 */
public class RootServerInfo extends ServerInfo
{
    /**
     * RootServer端口
     */
    private int port;

    /**
     * 集群ID
     */
    private int clusterID;

    public RootServerInfo()
    {
        port = -1;
        clusterID = -1;
    }

    public RootServerInfo(int port, int clusterID)
    {
        this.port = port;
        this.clusterID = clusterID;
    }

    public int getClusterID()
    {
        return clusterID;
    }

    public void setClusterID(int clusterID)
    {
        this.clusterID = clusterID;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getPort()
    {
        return this.port;
    }
}
