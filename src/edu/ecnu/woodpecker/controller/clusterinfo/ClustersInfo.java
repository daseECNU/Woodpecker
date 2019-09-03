package edu.ecnu.woodpecker.controller.clusterinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 主备集群间信息的定义，是控制器模块的组成部分 此对象表示所有集群的角色，全部为静态成员变量和静态成员函数
 * 
 */
public class ClustersInfo
{
    /**
     * 存放所有集群信息对象的列表
     */
    private static List<Cluster> clusters = null;

    /**
     * 部署地址
     */
    private static String deployPath = null;

    /**
     * 用户名
     */
    private static String userName = null;

    /**
     * 密码
     */
    private static String password = null;

    /**
     * port
     */
    private static int connectionPort;

    /**
     * TODO 根据测试环境模块中的配置文件初始化集群信息类
     * 
     * @return 初始化成功则为true
     */
    public static void initialize(List<Map<String, String>> serverList)
    {

        clusters = new ArrayList<Cluster>();
        int j = 0;
        RootServerInfo rs = null;
        Cluster cluster = null;

        if (serverList.size() != 0)
        {
            setDeployPath(serverList.get(0).get("deployPath"));
            setUserName(serverList.get(0).get("userName"));
            setPassword(serverList.get(0).get("password"));
            setConnectionPort(Integer.valueOf(serverList.get(0).get("connectionPort")));
            for (Map<String, String> server : serverList)
            {
                String type = server.get("serverType");

                if (type.equals("RootServer"))
                {
                    rs = new RootServerInfo();
                    cluster = new Cluster();
                    clusters.add(cluster);
                }
                if (type.equals("RootServer"))
                {
                    cluster.setClusterID(j);
                    rs.setClusterID(j);
                    // System.out.println("------ClusterID"+rs.getClusterID());
                    if (j == 0)
                    {
                        cluster.setClusterRole(1);
                    }
                    else
                    {
                        cluster.setClusterRole(-1);
                    }
                    rs.setCurClusterRSIP(server.get("RSIP"));
                    rs.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                    rs.setIP(server.get("IP"));
                    rs.setIsDown(false);
                    rs.setNIC(server.get("NIC"));
                    rs.setPID(Integer.valueOf(server.get("PID")));
                    rs.setPort(Integer.valueOf(server.get("port")));
                    cluster.addRootServer(rs);
                    j++;
                }
                else if (type.equals("UpdateServer"))
                {
                    UpdateServerInfo ups = new UpdateServerInfo();
                    ups.setCurClusterRSIP(server.get("RSIP"));
                    ups.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                    ups.setIP(server.get("IP"));
                    ups.setIsDown(false);
                    ups.setMergePort(Integer.valueOf(server.get("mergePort")));
                    ups.setNIC(server.get("NIC"));
                    ups.setPID(Integer.valueOf(server.get("PID")));
                    ups.setServicePort(Integer.valueOf(server.get("servicePort")));
                    cluster.addUpdateServer(ups);
                }
                else if (type.equals("MergeServer"))
                {
                    MergeServerInfo ms = new MergeServerInfo();
                    ms.setCurClusterRSIP(server.get("RSIP"));
                    ms.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                    ms.setIP(server.get("IP"));
                    ms.setIsDown(false);
                    ms.setNIC(server.get("NIC"));
                    ms.setPID(Integer.valueOf(server.get("PID")));
                    ms.setListener(Boolean.valueOf(server.get("isListener")));
                    ms.setServicePort(Integer.valueOf(server.get("servicePort")));
                    ms.setMySQLPort(Integer.valueOf(server.get("MySQLPort")));
                    cluster.addMergeServer(ms);
                }
                else if (type.equals("ListenerMergeServer"))
                {
                    MergeServerInfo ms = new MergeServerInfo();
                    ms.setCurClusterRSIP(server.get("RSIP"));
                    ms.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                    ms.setIP(server.get("IP"));
                    ms.setIsDown(false);
                    ms.setNIC(server.get("NIC"));
                    ms.setPID(Integer.valueOf(server.get("PID")));
                    ms.setListener(Boolean.valueOf(server.get("isListener")));
                    ms.setServicePort(Integer.valueOf(server.get("servicePort")));
                    ms.setMySQLPort(Integer.valueOf(server.get("MySQLPort")));
                    cluster.addMergeServer(ms);
                }
                else if (type.equals("ChunkServer"))
                {
                    ChunkServerInfo cs = new ChunkServerInfo();
                    cs.setCurClusterRSIP(server.get("RSIP"));
                    cs.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                    cs.setIP(server.get("IP"));
                    cs.setIsDown(false);
                    cs.setNIC(server.get("NIC"));
                    cs.setPID(Integer.valueOf(server.get("PID")));
                    cs.setServicePort(Integer.valueOf(server.get("servicePort")));
                    cs.setAppName(server.get("appName"));
                    // System.out.println("--dd--appName:"+cs.getAppName());
                    cluster.addChunkServer(cs);
                }
            }
        }
        else
        {
            setAllServerDown();
        }
        // for(Cluster clusterx : clusters)
        // {
        // System.out.println("--dd--Clusterdep:"+getDeployPath());
        // System.out.println("--dd--ClusterID:"+clusterx.getClusterID());
        // System.out.println("3333："+clusterx.getAllAliveServer().size());
        // for(ServerInfo server
        // :clusterx.getAllAliveServer() )
        // {
        // System.out.println("3333xxx"+server.getIP()+"--"+server.getPID());
        // }
        // }
    }

    /**
     * 将新注册的ChunkServer或MergeServer的信息添加进全局变量
     * 
     * @param serverList
     */
    public static void addServer(List<Map<String, String>> serverList)
    {
        for (Map<String, String> server : serverList)
        {
            String type = server.get("serverType");
            if (type.equals("MergeServer"))
            {
                MergeServerInfo ms = new MergeServerInfo();
                ms.setCurClusterRSIP(server.get("RSIP"));
                ms.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                ms.setIP(server.get("IP"));
                ms.setIsDown(false);
                ms.setNIC(server.get("NIC"));
                ms.setPID(Integer.valueOf(server.get("PID")));
                ms.setListener(Boolean.valueOf(server.get("isListener")));
                ms.setServicePort(Integer.valueOf(server.get("servicePort")));
                ms.setMySQLPort(Integer.valueOf(server.get("MySQLPort")));
                for (Cluster cluster : clusters)
                {
                    if (cluster.getRandomRS().getCurClusterRSIP().equals(server.get("RSIP")))
                    {
                        cluster.addMergeServer(ms);
                    }
                }
            }
            else if (type.equals("ChunkServer"))
            {
                ChunkServerInfo cs = new ChunkServerInfo();
                cs.setCurClusterRSIP(server.get("RSIP"));
                cs.setCurClusterRSPort(Integer.valueOf(server.get("RSPort")));
                cs.setIP(server.get("IP"));
                cs.setIsDown(false);
                cs.setNIC(server.get("NIC"));
                cs.setPID(Integer.valueOf(server.get("PID")));
                cs.setServicePort(Integer.valueOf(server.get("servicePort")));
                cs.setAppName(server.get("appName"));
                for (Cluster cluster : clusters)
                {
                    if (cluster.getRandomRS().getCurClusterRSIP().equals(server.get("RSIP")))
                    {
                        cluster.addChunkServer(cs);
                    }
                }
            }
        }
    }

    /**
     * 将新注册的ChunkServer或MergeServer的信息添加进此类
     * 
     * @param ServerOperation
     *            ChunkServerInfo或MergeServerInfo对象
     * @param cluCategory 集群类型，master或slave
     */
    public static void addNewServerInfo(ServerInfo server, int clusterID)
    {
        // 确定集群角色
        int index = -1;
        for (int i = 0; i < 3; i++)
        {
            if (clusterID == clusters.get(i).getClusterID())
            {
                index = i;
            }
        }
        // 确定server类型
        String serverName = server.getClass().getName();
        if (serverName.endsWith("MergeServerInfo"))
        {
            clusters.get(index).addMergeServer((MergeServerInfo) server);
        }
        else if (serverName.endsWith("ChunkServerInfo"))
        {
            clusters.get(index).addChunkServer((ChunkServerInfo) server);
        }
        else if (serverName.endsWith("RootServerInfo"))
        {
            clusters.get(index).addRootServer((RootServerInfo) server);
        }
        else if (serverName.endsWith("UpdateServerInfo"))
        {
            clusters.get(index).addUpdateServer((UpdateServerInfo) server);
        }
    }

    /**
     * 将新注册的ChunkServer或MergeServer的信息添加进此类
     * 
     * @param ServerOperation
     *            ChunkServerInfo或MergeServerInfo对象
     * @param cluCategory 集群类型，master或slave
     */
    public static void addServerInfo(ServerInfo server, String cluCategory)
    {
        // 确定集群角色
        int clusterRole = cluCategory.equals("master") ? 1 : -1;
        int index = -1;
        if (clusterRole == -1)
        {
            List<Integer> indexList = indexOfAll(-1);
            index = new Random().nextInt(indexList.size());
        }
        else
        {
            index = indexOf(1);
        }

        // 确定server类型
        String serverName = server.getClass().getName();
        if (serverName.endsWith("MergeServerInfo"))
        {
            clusters.get(index).addMergeServer((MergeServerInfo) server);
        }
        else
        {
            clusters.get(index).addChunkServer((ChunkServerInfo) server);
        }
    }

    /**
     * 获取集群rs信息
     * 
     * @return
     */
    public static Map<Integer, String> getRSList()
    {
        Map<Integer, String> rsList = new HashMap<Integer, String>();
        RootServerInfo rs = null;

        for (Cluster cluster : clusters)
        {
            // System.out.println("getRSList----ClusterID:"
            // + cluster.getClusterID());
            rs = cluster.getRandomRS();
            if (rs != null)
            {
                rsList.put(cluster.getClusterID(), rs.getCurClusterRSIP());
            }
        }
        return rsList;

    }

    // public static void systemaa()
    // {
    // for (Cluster clusterx : clusters)
    // {
    // System.out.println("-----role:" +
    // clusterx.getClusterRole());
    // System.out.println("--systemaa--ClusterID:" +
    // clusterx.getClusterID());
    // //
    // System.out.println("systemaa：" +
    // clusterx.getAllServer().size());
    // for (ServerInfo server : clusterx.getAllServer())
    // {
    // System.out.println("systemaa" + server.getIP() + "--"
    // + server.getPID() + "--"
    // + server.isDown());
    // }
    // }
    // }

    /**
     * TODO 更新类中存储的集群信息
     * 
     * @return 更新成功则为true
     */
    public static boolean update(ServerInfo server)
    {
        for (Cluster cluster : clusters)
        {
            for (ServerInfo newServer : cluster.getAllServer())
            {
                if (newServer.getClass().getName().equals(server.getClass().getName())
                        && newServer.getIP().equals(server.getIP()))
                {
                    newServer.setIsDown(server.isDown());
                    newServer.setPID(server.getPID());
                    return true;
                }
            }
            ServerInfo newServer = cluster.getRandomRS();
            if (newServer.getCurClusterRSIP().equals(server.getCurClusterRSIP()))
            {
                int clusterID = cluster.getClusterID();
                addNewServerInfo(server, clusterID);
                return true;
            }
        }
        //
        // for (Cluster clusterx : clusters)
        // {
        // //
        // System.out.println("--update--Clusterdep:"+getDeployPath());
        // System.out.println("--update--ClusterID:" +
        // clusterx.getClusterID());
        // //
        // System.out.println("update："+clusterx.getAllServer().size());
        // for (ServerInfo server1 :
        // clusterx.getAllServer())
        // {
        // System.out.println("updateaaa" + server1.getIP()
        // + "--" + server1.getPID() + "--"
        // + server1.getClass().getName() + "---" +
        // server1.isDown());
        // }
        // }
        return false;
    }

    /**
     * 返回主集群RootServer对应的server信息
     * 
     * @return RootServerInfo对象
     */
    public static RootServerInfo getMasterRS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        RootServerInfo rs = clusters.get(index).getRandomRS();
        return rs == null ? null : rs;
    }

    /**
     * 返回主集群UpdateServer对应的server信息
     * 
     * @return UpdateServerInfo对象或null
     */
    public static UpdateServerInfo getMasterUPS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        UpdateServerInfo ups = cluster.getRandomUPS();
        return ups == null ? null : ups;
    }

    /**
     * 随机从主集群的所有MergeServer中选择一个，返回其server信息，找不到一个MS 则返回null
     * 
     * @return MergeServerInfo对象或null
     */
    public static MergeServerInfo getMasterMS()
    {

        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        MergeServerInfo ms = cluster.getRandomMS();
        return ms == null ? null : ms;
    }

    /**
     * 随机从主集群的所有ChunkServer中选择一个，返回其server信息，找不到一个CS 则返回null
     * 
     * @return ChunkServerInfo对象或null
     */
    public static ChunkServerInfo getMasterCS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        ChunkServerInfo cs = cluster.getRandomCS();
        return cs == null ? null : cs;
    }

    /**
     * 获取随机一个备集群的一个rs信息
     * 
     * @return 一个备集群中rs信息的列表
     */
    public static ServerInfo getSlaveRS()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        RootServerInfo rs = cluster.getRandomRS();
        return rs == null ? null : rs;
    }

    /**
     * 返回主集群UpdateServer对应的server信息
     * 
     * @return UpdateServerInfo对象或null
     */
    public static UpdateServerInfo getSlaveUPS()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        UpdateServerInfo ups = cluster.getRandomUPS();
        return ups == null ? null : ups;
    }

    /**
     * 随机从备集群的所有ChunkServer中选择一个，返回其server信息，找不到一个CS 则返回null
     * 
     * @return ChunkServerInfo对象或null
     */
    public static ChunkServerInfo getSlaveCS()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        ChunkServerInfo cs = cluster.getRandomCS();
        return cs == null ? null : cs;
    }

    /**
     * 随机从备集群的所有MergeServer中选择一个，返回其server信息，找不到一个MS 则返回null
     * 
     * @return MergeServerInfo对象或null
     */
    public static MergeServerInfo getSlaveMS()
    {

        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        MergeServerInfo ms = cluster.getRandomMS();
        return ms == null ? null : ms;
    }

    /**
     * 获取当前主集群所有未下线server的信息。可能有些server下线导致返回不全
     * 
     * @return server信息的列表
     */
    public static List<ServerInfo> getAliveMasterInfo()
    {
        List<ServerInfo> serverList = new ArrayList<ServerInfo>();

        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);

        // RS
        RootServerInfo rs = cluster.getAliveRandomRS();
        if (rs != null)
        {
            serverList.add(rs);
        }

        // UPS
        UpdateServerInfo ups = cluster.getAliveRandomUPS();
        if (ups != null)
        {
            serverList.add(ups);
        }

        // CS and MS
        serverList.addAll(cluster.getAllAliveCS());
        serverList.addAll(cluster.getAllAliveMS());

        return serverList;
    }

    /**
     * 返回主集群RootServer对应的server信息，如果此RS已下线则返回null
     * 
     * @return RootServerInfo对象或null
     */
    public static RootServerInfo getAliveMasterRS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        RootServerInfo rs = clusters.get(index).getAliveRandomRS();
        return rs == null ? null : rs;
    }

    /**
     * 返回主集群UpdateServer对应的server信息，此UPS下线则返回null
     * 
     * @return UpdateServerInfo对象或null
     */
    public static UpdateServerInfo getAliveMasterUPS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        UpdateServerInfo ups = cluster.getAliveRandomUPS();
        return ups == null ? null : ups;
    }

    /**
     * 随机从主集群的所有MergeServer中选择一个未下线的，返回其server信息，找不到一个未下线的MS
     * 则返回null
     * 
     * @return MergeServerInfo对象或null
     */
    public static MergeServerInfo getAliveMasterMS()
    {

        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        MergeServerInfo ms = cluster.getAliveRandomMS();
        return ms == null ? null : ms;
    }

    /**
     * 未下线的主集群的所有MergeServer中选择，返回其server信息， 则返回null
     * 
     * @return MergeServerInfo对象或null
     */
    public static List<MergeServerInfo> getAliveMasterMSs()
    {
        List<MergeServerInfo> msList = new ArrayList<MergeServerInfo>();
        // MergeServerInfo ms = null;

        List<Integer> indexList = indexOfAll(1);
        Cluster cluster = null;
        // for (int index : indexList)
        // {
        // cluster = clusters.get(index);
        // ms = cluster.getAliveRandomMS();
        // if (ms != null)
        // {
        // msList.add(ms);
        // }
        // }
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            for (MergeServerInfo ms : cluster.getAllAliveMS())
            {
                if (ms != null)
                {
                    msList.add(ms);
                }
            }
        }
        return msList;
    }

    /**
     * 返回主集群ChunkServerInfo对应的server信息，此CS下线则返回null
     * 
     * @return ChunkServerInfo对象或null
     */
    public static ChunkServerInfo getAliveMasterCS()
    {
        int index = indexOf(1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        ChunkServerInfo cs = cluster.getAliveRandomCS();
        return cs == null ? null : cs;
    }

    /**
     * 未下线的主集群的所有ChunkServer中选择，返回其server信息， 则返回null
     * 
     * @return MergeServerInfo对象或null
     */
    public static List<ChunkServerInfo> getAliveMasterCSs()
    {
        List<ChunkServerInfo> csList = new ArrayList<ChunkServerInfo>();

        List<Integer> indexList = indexOfAll(1);
        Cluster cluster = null;
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            for (ChunkServerInfo cs : cluster.getAllAliveCS())
            {
                if (cs != null)
                {
                    csList.add(cs);
                }
            }
        }
        return csList;
    }

    /**
     * 获取所有集群未下线的server信息
     * 
     * @return 所有集群未下线server信息的列表
     */
    public static List<ServerInfo> getAllAliveServerInfo()
    {
        List<ServerInfo> serverList = new ArrayList<ServerInfo>();
        for (Cluster ele : clusters)
        {
            serverList.addAll(ele.getAllAliveServer());
        }

        return serverList;
    }

    /**
     * 获取所有备集群未下线server的信息
     * 
     * @return 所有备集群未下线server信息的列表
     */
    public static List<ServerInfo> getAllAliveSlaveInfo()
    {
        List<ServerInfo> serverList = new ArrayList<ServerInfo>();
        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            serverList.addAll(clusters.get(index).getAllAliveServer());
        }

        return serverList;
    }

    /**
     * 获取随机一个备集群的所有未下线的server信息
     * 
     * @return 备集群中所有server信息的列表
     */
    public static List<ServerInfo> getAliveSlaveInfo()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        return clusters.get(index).getAllAliveServer();
    }

    /**
     * 获取随机一个备集群的一个未下线的rs信息
     * 
     * @return 一个备集群中rs信息的列表
     */
    public static ServerInfo getAliveSlaveRS()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        RootServerInfo rs = cluster.getAliveRandomRS();
        return rs == null ? null : rs;
    }

    /**
     * 获取随机一个备集群的一个未下线的rs信息
     * 
     * @return 一个备集群中rs信息的列表
     */
    public static ServerInfo getAliveSlaveUPS()
    {
        int index = indexOf(-1);
        if (index < 0)
        {
            return null;
        }
        Cluster cluster = clusters.get(index);
        UpdateServerInfo ups = cluster.getAliveRandomUPS();
        return ups == null ? null : ups;
    }

    /**
     * 返回所有备集群未下线RootServer对应的server信息， 列表为空则代表所有备集群的UPS都已下线
     * 
     * @return RootServerInfo对象列表
     */
    public static List<RootServerInfo> getAliveSlaveRSs()
    {
        List<RootServerInfo> rsList = new ArrayList<RootServerInfo>();
        RootServerInfo rs = null;

        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            rs = clusters.get(index).getAliveRandomRS();
            if (rs != null)
            {
                rsList.add(rs);
            }
        }

        return rsList;
    }

    /**
     * 返回所有备集群未下线UpdateServer对应的server信息，
     * 列表为空则代表所有备集群的UPS都已下线
     * 
     * @return UpdateServerInfo对象列表
     */
    public static List<UpdateServerInfo> getAliveSlaveUPSs()
    {
        List<UpdateServerInfo> upsList = new ArrayList<UpdateServerInfo>();
        UpdateServerInfo ups = null;

        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            ups = clusters.get(index).getAliveRandomUPS();
            if (ups != null)
            {
                upsList.add(ups);
            }
        }

        return upsList;
    }

    /**
     * 每个备集群中选出未下线的MergeServer，返回其server信息
     * 
     * @return MergeServerInfo对象列表
     */
    public static List<MergeServerInfo> getAliveSlaveMSs()
    {
        List<MergeServerInfo> msList = new ArrayList<MergeServerInfo>();

        List<Integer> indexList = indexOfAll(-1);
        Cluster cluster = null;
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            for (MergeServerInfo ms : cluster.getAllAliveMS())
            {
                if (ms != null)
                {
                    msList.add(ms);
                }
            }
        }

        return msList;
    }

    /**
     * 每个备集群中选出未下线的ChunkServerInfo，返回其server信息
     * 
     * @return ChunkServerInfo对象列表
     */
    public static List<ChunkServerInfo> getAliveSlaveCSs()
    {
        List<ChunkServerInfo> csList = new ArrayList<ChunkServerInfo>();

        List<Integer> indexList = indexOfAll(-1);
        Cluster cluster = null;
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            for (ChunkServerInfo cs : cluster.getAllAliveCS())
            {
                if (cs != null)
                {
                    csList.add(cs);
                }
            }
        }

        return csList;
    }

    /**
     * 根据clusterID返回cluster
     * 
     * @param clusterID
     * @return
     */
    public static Cluster getCluster(int clusterID)
    {
        for (Cluster cluster : clusters)
        {
            if (cluster.getClusterID() == clusterID)
            {
                return cluster;
            }
        }
        return null;
    }

    /**
     * 判断当前所有未下线的集群中是否存在主集群。
     * 
     * @return 存在主集群则返回true
     */
    public static boolean existMaster()
    {
        int index = indexOf(1);
        return index == -1 ? false : true;
    }

    /**
     * 将当前主集群的所有server的isDown置为true
     */
    public static void setMasterDown()
    {
        int index = indexOf(1);
        if (index >= 0)
        {
            Cluster cluster = clusters.get(index);
            cluster.setAllDown();
        }

    }

    /**
     * 将所有集群里server的isDown置为true
     */
    public static void setAllServerDown()
    {
        for (Cluster ele : clusters)
        {
            ele.setAllDown();
        }
    }

    /**
     * 将所有备集群的server里的isDown置为true
     */
    public static void setAllSlaveDown()
    {
        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            clusters.get(index).setAllDown();
        }
    }

    /**
     * 将一个备集群里指定server里的isDown置为true，输入为此备集群中未下线server信息的列表
     * 
     * @param serverList 备集群server信息列表
     */
    public static void setSlaveDown(List<ServerInfo> serverList)
    {
        Cluster cluster = null;
        String IP = serverList.get(0).getIP();
        int PID = serverList.get(0).getPID();

        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            if (cluster.contain(IP, PID))
            {
                cluster.setServerDown(serverList);
                break;
            }
        }
    }

    public static void setServerDown(List<ServerInfo> serverList)
    {
        Cluster cluster = null;
        String IP = serverList.get(0).getIP();
        int PID = serverList.get(0).getPID();

        List<Integer> indexList = indexOfAll(-1);
        for (int index : indexList)
        {
            cluster = clusters.get(index);
            if (cluster.contain(IP, PID))
            {
                cluster.setServerDown(serverList);
                break;
            }
        }
    }

    /**
     * 将此IP和PID对应的server所在的集群设为主集群
     * 
     * @param IP server的IP地址
     * @param PID server的进程号
     */
    public static void setMaster(String IP, int PID)
    {
        for (Cluster cluster : clusters)
        {
            if (cluster.contain(IP, PID))
            {
                clusters.get(indexOf(1)).setClusterRole(-1);
                cluster.setClusterRole(1);
                break;
            }
        }
    }

    /**
     * 根据输入的集群角色，返回集群所在列表的位置，对于备集群，返回的是随机一个备集群的位置，不是首个备集群
     * 的位置，如果找不到则返回-1
     * 
     * @param clusterRole 集群角色，1代表主集群，0代表初始化，-1代表备集群
     * @return 集群位置或-1
     */
    private static int indexOf(int clusterRole)
    {
        Cluster cluster = null;
        int index = -1;
        switch (clusterRole)
        {
        case -1:
            // 备集群
            List<Integer> indexList = new ArrayList<Integer>();
            for (int i = 0, len = clusters.size(); i < len; i++)
            {
                cluster = clusters.get(i);
                if (cluster.getClusterRole() == -1)
                {
                    indexList.add(i);
                }
            }

            // 随机选择一个备集群
            if (indexList.size() == 0)
            {
                return -1;
            }
            int position = new Random().nextInt(indexList.size());
            index = indexList.get(position);
            break;
        default:
            // 主集群或初始化状态
            boolean flag = false;
            for (int i = 0, len = clusters.size(); i < len; i++)
            {
                cluster = clusters.get(i);
                if (cluster.getClusterRole() == clusterRole)
                {
                    index = i;
                    flag = true;
                    break;
                }
            }
            if (!flag)
            {
                return -1;
            }
            break;
        }// end of switch(clusterRole)

        return index;
    }

    /**
     * 根据输入的集群角色，返回集群所在列表所有位置的列表
     * 
     * @param clusterRole 集群角色，1代表主集群，0代表初始化，-1代表备集群
     * @return 集群所有位置的列表
     */
    private static List<Integer> indexOfAll(int clusterRole)
    {
        Cluster cluster = null;
        List<Integer> indexList = new ArrayList<Integer>();

        for (int i = 0, len = clusters.size(); i < len; i++)
        {
            cluster = clusters.get(i);
            if (cluster.getClusterRole() == clusterRole)
            {
                indexList.add(i);
            }
        }

        return indexList;
    }

    public static void main(String[] args)
    {

    }

    public static String getDeployPath()
    {
        return deployPath;
    }

    public static void setDeployPath(String deployPath)
    {
        ClustersInfo.deployPath = deployPath;
    }

    public static String getUserName()
    {
        return userName;
    }

    public static void setUserName(String userName)
    {
        ClustersInfo.userName = userName;
    }

    public static String getPassword()
    {
        return password;
    }

    public static void setPassword(String password)
    {
        ClustersInfo.password = password;
    }

    public static int getConnectionPort()
    {
        return connectionPort;
    }

    public static void setConnectionPort(int connectionPort)
    {
        ClustersInfo.connectionPort = connectionPort;
    }

}
