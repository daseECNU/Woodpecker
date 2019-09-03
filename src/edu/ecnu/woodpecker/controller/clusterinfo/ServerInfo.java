package edu.ecnu.woodpecker.controller.clusterinfo;

/**
 * Server信息抽象类
 * 
 */
public abstract class ServerInfo
{
//    /**
//     * 部署地址
//     */
//    private String deployPath = null;
    /**
     * 此server的IP地址
     */
    private String IP = null;

    /**
     * server绑定的网卡名称，Network Interface Card
     */
    private String NIC = null;

    /**
     * 此server是否下线
     */
    private boolean isDown;

    /**
     * 此server是否在主集群，是则为true
     */
    // private boolean isMaster;

    /**
     * 此server当前所在集群RootServer的IP地址
     */
    private String curClusterRSIP = null;

    /**
     * 此server当前所在集群RootServer的端口
     */
    private int curClusterRSPort;

    /**
     * 此server的进程号
     */
    private int PID;

    public int getPID()
    {
        return PID;
    }

    public void setPID(int pID)
    {
        PID = pID;
    }

    public int getCurClusterRSPort()
    {
        return curClusterRSPort;
    }

    public void setCurClusterRSPort(int curClusterRSPort)
    {
        this.curClusterRSPort = curClusterRSPort;
    }

    public String getIP()
    {
        return IP;
    }

    public void setIP(String iP)
    {
        IP = iP;
    }

    public String getCurClusterRSIP()
    {
        return curClusterRSIP;
    }

    public void setCurClusterRSIP(String curClusterRSIP)
    {
        this.curClusterRSIP = curClusterRSIP;
    }

    public boolean isDown()
    {
        return this.isDown;
    }

    public void setIsDown(boolean isDown)
    {
        this.isDown = isDown;
    }

    // public boolean isMaster()
    // {
    // return this.isMaster;
    // }
    //
    // public void setIsMaster(boolean isMaster)
    // {
    // this.isMaster = isMaster;
    // }

    public String getNIC()
    {
        return NIC;
    }

    public void setNIC(String nIC)
    {
        NIC = nIC;
    }

//    public String getDeployPath()
//    {
//        return deployPath;
//    }
//
//    public void setDeployPath(String deployPath)
//    {
//        this.deployPath = deployPath;
//    }
}