package edu.ecnu.woodpecker.controller.clusterinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.log.WpLog;

/**
 * Cedar主备集群信息的定义 此对象对应一个主集群或备集群里所有server的信息
 * 
 */
public class Cluster
{
    /**
     * 一个集群中RootServer、UpdateServer、ChunkServer、
     * MergeServer的信息 为日后CEDAR扩展考虑，暂将RS、UPS以列表形式存储
     */
    private List<RootServerInfo> RSList = null;
    private List<UpdateServerInfo> UPSList = null;
    private List<ChunkServerInfo> CSList = null;
    private List<MergeServerInfo> MSList = null;

    /**
     * 集群角色，1为主集群，-1为备集群，0为初始化状态
     */
    private int clusterRole;

    /**
     * 集群ID，标识一个集群
     */
    private int clusterID;

    public Cluster()
    {
        RSList = new ArrayList<RootServerInfo>(1);
        UPSList = new ArrayList<UpdateServerInfo>(1);
        CSList = new ArrayList<ChunkServerInfo>();
        MSList = new ArrayList<MergeServerInfo>();
    }

    /**
     * 增加RS到此集群中
     * 
     * @param rs
     */
    public void addRootServer(RootServerInfo rs)
    {
        RSList.add(rs);
    }

    /**
     * 增加UPS到此集群中
     * 
     * @param ups
     */
    public void addUpdateServer(UpdateServerInfo ups)
    {
        UPSList.add(ups);
    }

    /**
     * 增加MS到此集群中
     * 
     * @param ms    
     */
    public void addMergeServer(MergeServerInfo ms)
    {
        MSList.add(ms);
    }

    /**
     * 增加CS到此集群中
     * 
     * @param cs
     */
    public void addChunkServer(ChunkServerInfo cs)
    {
        CSList.add(cs);
    }

    /**
     * 返回此集群中所有server的列表
     * @return  所有server的列表
     */
    public List<ServerInfo> getAllServer()
    {
        List<ServerInfo> serverList = new ArrayList<ServerInfo>();
        
        serverList.addAll(RSList);
        serverList.addAll(UPSList);
        serverList.addAll(CSList);
        serverList.addAll(MSList);
        
        return serverList;
    }

    /**
     * 从此集群中随机返回一个RS，目前只有一个RS
     * 
     * @return a random RS info
     */
    public RootServerInfo getRandomRS()
    {
        try
        {
            return RSList.get(0);
        }
        catch (Exception e)
        {
            // 初始化没有添加RS对象
            WpLog.recordLog(LogLevelConstant.ERROR, "there is no RootServer info");
        }
        return null;
    }
    
    /**
     * 从此集群中随机返回一个UPS，目前只有一个UPS
     * 
     * @return 一个未下线的UPS，如果UPS下线则返回null
     */
    public UpdateServerInfo getRandomUPS()
    {
        try
        {
            return UPSList.get(0);
        }catch(Exception e)
        {
            //记录日志，初始化没有添加UPS对象
            WpLog.recordLog(LogLevelConstant.ERROR, "there is no UpdateServer info");
        }
        return null;
    }

    /**
     * 从此集群中随机返回一个MS 随机10次仍然找不到未下线MS则使用遍历寻找，仍旧找不到则返回null
     * 
     * @return MS，找不到则返回null
     */
    public MergeServerInfo getRandomMS()
    {
        MergeServerInfo ms = null;
        
        int index = new Random().nextInt(MSList.size());
        ms = MSList.get(index);
        if (null == ms)
            WpLog.recordLog(LogLevelConstant.ERROR, "merge server info is null, index=%d", index);
        return ms;
    }

    /**
     * 从此集群中随机返回CS 随机10次仍然找不到未下线CS则使用遍历寻找，仍旧找不到则返回null
     * 
     * @return CS，找不到则返回null
     */
    public ChunkServerInfo getRandomCS()
    {
        ChunkServerInfo cs = null;
        int index = new Random().nextInt(CSList.size());
        cs = CSList.get(index);
        if (null == cs)
            WpLog.recordLog(LogLevelConstant.ERROR, "chunk server info is null, index=%d", index);
        return cs;
    }

    /**
     * 从此集群中随机返回一个未下线的RS，目前只有一个RS
     * 
     * @return 一个未下线的RS，如果RS下线则返回null
     */
    public RootServerInfo getAliveRandomRS()
    {
        try
        {
            return RSList.get(0).isDown() ? null : RSList.get(0);
        }
        catch (Exception e)
        {
            // 记录日志，初始化没有添加RS对象
            WpLog.recordLog(LogLevelConstant.ERROR, "root server info is null");
        }
        return null;
    }

    /**
     * 从此集群中随机返回一个未下线的UPS，目前只有一个UPS
     * 
     * @return 一个未下线的UPS，如果UPS下线则返回null
     */
    public UpdateServerInfo getAliveRandomUPS()
    {
        try
        {
            return UPSList.get(0).isDown()? null:UPSList.get(0);
        }catch(Exception e)
        {
            //记录日志，初始化没有添加UPS对象
            WpLog.recordLog(LogLevelConstant.ERROR, "update server info is null");
        }
        return null;
    }

    /**
     * 从此集群中随机返回一个未下线的MS 随机10次仍然找不到未下线MS则使用遍历寻找，仍旧找不到则返回null
     * 
     * @return 未下线MS，找不到则返回null
     */
    public MergeServerInfo getAliveRandomMS()
    {
        MergeServerInfo ms = null;
        
        //随机10次查找未下线MS
        for (int i = 0; i < 10; i++)
        {
            int index = new Random().nextInt(MSList.size());
            ms = MSList.get(index);
            if (!ms.isDown())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "get alive merge server: %s, index=%d", ms, index);
                return ms;
            }
        }
    
        // 使用遍历查找未下线MS
        for (MergeServerInfo msi : MSList)
        {
            if (!msi.isDown())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "get alive merge server: %s", msi);
                return msi;
            }
        }
        WpLog.recordLog(LogLevelConstant.ERROR, "no alive merge server");
        return null;
    }

    /**
     * 从此集群中随机返回一个未下线的CS 随机10次仍然找不到未下线CS则使用遍历寻找，仍旧找不到则返回null
     * 
     * @return 未下线CS，找不到则返回null
     */
    public ChunkServerInfo getAliveRandomCS()
    {
        ChunkServerInfo cs = null;
        for (int i = 0; i < 10; i++)
        {
            int index = new Random().nextInt(CSList.size());
            cs = CSList.get(index);
            if (!cs.isDown())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "get alive chunk server: %s, index=%d", cs, index);
                return cs;
            }
        }
    
        // 使用遍历查找
        for (ChunkServerInfo csi : CSList)
        {
            if (!csi.isDown())
            {
                WpLog.recordLog(LogLevelConstant.INFO, "get alive chunk server: %s", csi);
                return csi;
            }
        }
    
        return null;
    }

    /**
     * 从此集群中随机返回一个RS，目前只有一个RS
     * @return
     */
    public RootServerInfo getALLAliveRS()
    {
        try
        {
            return RSList.get(0);
        }
        catch (Exception e)
        {
            // 记录日志，初始化没有添加RS对象
            WpLog.recordLog(LogLevelConstant.ERROR, "no alive root server");
        }
        return null;
    }
    
    /**
     * 返回此集群中所有未下线MS的列表
     * @return  所有未下线MS的列表
     */
    public List<MergeServerInfo> getAllAliveMS()
    {
        List<MergeServerInfo> msList = new ArrayList<MergeServerInfo>();
        for(MergeServerInfo ms:MSList)
        {
            if(!ms.isDown())
            {
                msList.add(ms);
            }
        }
        
        return msList;
    }

    /**
     * 返回此集群中所有未下线CS的列表
     * @return  所有未下线CS的列表
     */
    public List<ChunkServerInfo> getAllAliveCS()
    {
        List<ChunkServerInfo> csList = new ArrayList<ChunkServerInfo>();
        for(ChunkServerInfo cs:CSList)
        {
            if(!cs.isDown())
            {
                csList.add(cs);
            }
        }
        
        return csList;
    }
    
    /**
     * 返回此集群中所有未下线server的列表
     * @return  所有未下线server的列表
     */
    public List<ServerInfo> getAllAliveServer()
    {
        List<ServerInfo> serverList = new ArrayList<ServerInfo>();
        
        // RS and UPS
        ServerInfo server = getAliveRandomRS();
        if(server!=null)
        {
            serverList.add(server);
        }
        server = getAliveRandomUPS();
        if(server!=null)
        {
            serverList.add(server);
        }
        
        //CS and MS
        serverList.addAll(getAllAliveCS());
        serverList.addAll(getAllAliveMS());
        
        return serverList;
    }
    /**
     * 将此集群中所有server里的isDown都置为true
     */
    public void setAllDown()
    {
        for(RootServerInfo rs:RSList)
        {
            rs.setIsDown(true);
        }
        for(UpdateServerInfo ups: UPSList)
        {
            ups.setIsDown(true);
        }
        for(ChunkServerInfo cs:CSList)
        {
            cs.setIsDown(true);
        }
        for(MergeServerInfo ms: MSList)
        {
            ms.setIsDown(true);
        }
    }
    
    /**
     * 此集群中是否包含此IP和PID对应的server
     * 包含则返回true
     */
    public boolean contain(String IP,int PID)
    {
        //RS and UPS
        for(RootServerInfo rs:RSList)
        {
            if(rs.getIP().equals(IP)&&rs.getPID()==PID)
            {
                return true;
            }
        }
        for(UpdateServerInfo ups: UPSList)
        {
            if(ups.getIP().equals(IP) && ups.getPID()==PID)
            {
                return true;
            }
        }
        
        //CS and MS
        for(ChunkServerInfo cs: CSList)
        {
            if(cs.getIP().equals(IP) && cs.getPID()==PID)
            {
                return true;
            }
        }
        for(MergeServerInfo ms: MSList)
        {
            if(ms.getIP().equals(IP)&& ms.getPID()==PID)
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 将此集群中指定server的isDown置为true
     * 输入的server肯定在此集群中
     * @param serverList    指定server的列表
     */
    public void setServerDown(List<ServerInfo> serverList)
    {
        String IP = null;
        int PID=-1;
        for(ServerInfo server:serverList)
        {
            IP=server.getIP();
            PID=server.getPID();
            getServer(IP,PID).setIsDown(true);
        }
    }
    /**
     * 将此集群中指定server的isDown置为false
     * 输入的server肯定在此集群中
     * @param serverList    指定server的列表
     */
    public void setServerUp(List<ServerInfo> serverList)
    {
        String IP = null;
        int PID=-1;
        for(ServerInfo server:serverList)
        {
            IP=server.getIP();
            PID=server.getPID();
            getServer(IP,PID).setIsDown(false);
        }
    }
    public int getClusterRole()
    {
        return clusterRole;
    }

    public void setClusterRole(int clusterRole)
    {
        this.clusterRole = clusterRole;
    }

    public int getClusterID()
    {
        return clusterID;
    }

    public void setClusterID(int clusterID)
    {
        this.clusterID = clusterID;
    }
    
    /**
     * 根据IP和PID返回对应的server对象，查找失败时返回null
     * @param IP    IP地址
     * @param PID   进程号
     * @return  IP和PID对应的server对象或null
     */
    private ServerInfo getServer(String IP,int PID)
    {
        for(RootServerInfo rs:RSList)
        {
            if(rs.getIP().equals(IP)&& rs.getPID()==PID)
            {
                return rs;
            }
        }
        
        for(UpdateServerInfo ups:UPSList)
        {
            if(ups.getIP().equals(IP) && ups.getPID()==PID)
            {
                return ups;
            }
        }
        
        for(ChunkServerInfo cs:CSList)
        {
            if(cs.getIP().equals(IP)&& cs.getPID()==PID)
            {
                return cs;
            }
        }
        
        for(MergeServerInfo ms:MSList)
        {
            if(ms.getIP().equals(IP)&& ms.getPID()==PID)
            {
                return ms;
            }
        }
        
        return null;
    }
}
