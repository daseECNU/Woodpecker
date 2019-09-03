package edu.ecnu.woodpecker.environment;

public interface CedarEnvirOperation
{
    /**
     * 读取配置文件
     * 
     * @param filePath 编译配置文件地址+文件名 例："F:/compile.txt";
     * @param encoding 编码格式
     * @return 读取编译配置文件是否成功
     */
    public static boolean readCompileConf(String filePath, String encoding)
    {
        return CedarConfigInitializer.read(filePath, encoding);
    }

    /**
     * 编译CEDAR
     * 
     * @param filePath 编译配置文件地址+文件名 例："F:/compile.txt";
     * @param encoding 编码格式
     * @return 编译是否成功
     */
    public static boolean compileCEDAR()
    {
        // Configurate.read(filePath,encoding);
        return CedarCompiler.compileCEDAR();
    }

    /**
     * 部署CEDAR
     * 
     * @param filePath 编译部署文件地址+文件名 例："F:/1RS_1UPS_3MS_3CS.txt";
     * @param encoding 编码格式
     * @return 部署是否成功
     */
    public static boolean deployCEDAR(String filePath, String encoding)
    {
        boolean confCorrect = CedarConfigInitializer.read(filePath, encoding);
        return confCorrect ? CedarDeployer.deployCEDAR() : false;
    }

    /**
     * 传输编译好的CEDAR文件
     * 
     * @param ip 将编译好的CEDAR文件发送到该ip地址下
     * @return 传输是否成功
     */
    public static boolean remoteReplicate(String ip)
    {
        return CedarDeployer.remoteReplicate(ip);
    }

    /**
     * 根据配置文件起集群
     * 
     * @return 集群是否启动成功
     */
    public static boolean startCEDAR()
    {
        return CedarDeployer.startCEDAR();
    }

    /**
     * 关闭集群
     * 
     * @param type 集群关闭类型：normal/unnormal normal：正常关闭集群上所有server并删除日志等数据文件
     *            unnormal:正常关闭集群上所有server，将log文件转存后删除日志等数据文件
     * @return 集群是否关闭成功
     */
    public static boolean closeCEDAR(String type)
    {
        return CedarDeployer.closeCEDAR(type);
    }

    /**
     * 初始化集群
     * 
     * @param type 初始化集群类型：complie/deploy compile:删除集群中编译产生的CEDAR文件
     *            deploy：删除集群中部署产生的CEDAR文件
     * @return 初始化集群是否成功
     */
    public static boolean initializeCEDAR(String type)
    {
        return CedarDeployer.initializeCEDAR(type);
    }

    /**
     * 注册服务器
     * 
     * @param ip ip地址，在哪台机器上注册
     * @param serverType server类型 MergeServer/ChunkServer
     * @param clusterType cluster类型 master/slave
     * @param NIC 端口号
     * @return 注册服务器是否成功
     */
    public static boolean registerServer(String ip, String serverType, String clusterType,
            String NIC)
    {
        return CedarDeployer.registerServer(ip, serverType, clusterType, NIC);
    }

    /**
     * 获取NIC
     * 
     * @param ip ip地址
     */
    public static String getNIC(String ip)
    {
        return CedarDeployer.getNIC(ip);
    }

    /**
     * 初始化集群信息 在集群已经启动后想直接执行案例时调用该方法初始化集群信息
     * 
     * @param compileFilePath 编译文件路径
     * @param deployFilePath 部署文件路径
     * @param encoding 编码格式
     */
    public static boolean initializeCluster(String compileFilePath, String deployFilePath,
            String encoding)
    {
        return CedarDeployer.initializeCluster(compileFilePath, deployFilePath, encoding);
    }
}
