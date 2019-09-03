package edu.ecnu.woodpecker.systemfunction;

public class CedarSystemOperator
{
    /**
     * 暂停当前线程指定的时间
     * 
     * @param time 时间
     * @param timeUnit 时间单位
     */
    public static void sleep(int time, String timeUnit)
    {
        Shell.sleep(time, timeUnit);
    }

    /**
     * 表示在指定ip地址上执行shell命令，默认路径是用户主目录。
     * 
     * @param command shell命令
     * @param ip ip地址
     */
    public static void shellCommand(String command, String ip)
    {
        Shell.shellCommand(command, ip);
    }

    /**
     * 表示杀CEDAR的server，返回被杀server的信息 举例： 杀死主集群所有server type=master num随意 某个备集群中随机n个MS
     * type=ms_slave num=n
     * 
     * @param type server类型
     * @param num server数目
     * @return 被杀server的信息
     */
    public static String killServer(String type, int num)
    {
        return CEDARServer.killServer(type, num);
    }

    /**
     * 根据arg启某个被杀server
     * 
     * @param arg 是kill_server返回的字符串信息，存储有被杀server的信息。
     * @return 启动是否成功
     */
    public static boolean startServer(String arg)
    {
        return CEDARServer.startServer(arg);
    }

    /**
     * 在指定ip上添加一个server，server类型由servertype决定
     * 
     * @param ip
     * @param serverType 例ms_master
     * @param NIC 网卡名
     */
    public static void addServer(String ip, String serverType, String NIC)
    {
        CEDARServer.addServer(ip, serverType, NIC);
    }

    /**
     * 对主集群发送重新选举命令，进行换主选举。 发起选举后返回不需等待选举完成。
     * 
     * @return 发起重新选主是否成功
     */
    public static boolean reelect()
    {
        return CEDARCluster.reelect();
    }

    /**
     * 发起每日合并
     * 
     * @return 发起每日合并是否成功
     */
    public static boolean merge()
    {
        return CEDARCluster.merge();
    }

    /**
     * 表示集群合并是否完成，命令返回结果为整型数字
     * 
     * @return 0代表所有集群合并完成 1代表有一个备集群未合并完成，其余集群合并完 2代表两个备集群未合并完成，其余集群合并完
     *         3代表主集群未合并完成，其余备集群合并完 4代表一个备集群合并完成，其余集群未合并完成 5代表所有集群都未合并完成。
     */
    public static Integer isMergeDown()
    {
        return CEDARCluster.isMergeDown();
    }

    /**
     * 表示当前集群是否有主
     * 
     * @return
     */
    public static boolean existMaster()
    {
        return CEDARCluster.existMaster();
    }

    /**
     * 根据kill_server返回的字符串信息，设置该server所在集群为主集群
     * 
     * @param arg kill_server返回的字符串信息
     * @return 设置是否成功
     */
    public static boolean setMaster(String arg)
    {
        return CEDARCluster.setMaster(arg);
    }

    /**
     * 表示在n秒内等待所有集群合并完成，超出指定时间仍有集群未合并完成则返回错误，表示此case失败。
     * 
     * @param time 指定时间n秒
     * @return 正确或错误
     */
    public static boolean awaitMergeDone(int time)
    {
        return CEDARCluster.awaitMergeDone(time);
    }

    /**
     * 集群是否可服务，命令返回结果为整型数字 0代表所有集群可服务 1代表有一个备集群不可服务，其余集群可服务 2代表两个备集群不可服务，其余集群可服务
     * 3代表主集群不可服务，备集群都可服务 4代表一个备集群可服务，其余集群不可服务 5代表所有集群不可服务
     * 
     * @return
     */
    public static Integer isClusterAvailable()
    {
        return CEDARCluster.isClusterAvailable();
    }

    /**
     * 表示在n秒内等待所有集群可服务，超出指定时间仍有集群不可服务则返回错误，表示此case失败
     * 
     * @param time 指定时间n秒
     * @return
     */
    public static boolean awaitAvailable(int time)
    {
        return CEDARCluster.awaitAvailable(time);
    }

    /**
     * Start to gather statistical information
     * 
     * @return True when starting successfully
     */
    public static boolean gatherStatistics()
    {
        return CEDARCluster.gatherStatistics();
    }

    /**
     * 
     * @return True when gather is done
     */
    public static boolean isGatherDone()
    {
        return CEDARCluster.isGatherDone();
    }
}
