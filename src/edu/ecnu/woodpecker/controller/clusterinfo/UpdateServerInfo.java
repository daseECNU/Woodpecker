package edu.ecnu.woodpecker.controller.clusterinfo;

/**
 * UpdateServer信息类
 * 
 */
public class UpdateServerInfo extends ServerInfo
{
    /**
     * UpdateServer服务端口
     */
    private int servicePort;

    /**
     * UpdateServer合并端口
     */
    private int mergePort;

    public UpdateServerInfo()
    {
        servicePort = -1;
        mergePort = -1;
    }

    public UpdateServerInfo(int servicePort, int mergePort)
    {
        this.servicePort = servicePort;
        this.mergePort = mergePort;
    }

    public int getMergePort()
    {
        return mergePort;
    }

    public void setMergePort(int mergePort)
    {
        this.mergePort = mergePort;
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