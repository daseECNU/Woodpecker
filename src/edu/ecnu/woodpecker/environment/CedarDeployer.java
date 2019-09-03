package edu.ecnu.woodpecker.environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.alibaba.druid.sql.dialect.mysql.visitor.transform.FromSubqueryResolver;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import edu.ecnu.woodpecker.constant.CedarConstant;
import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ShellConstant;
import edu.ecnu.woodpecker.controller.CedarOperation;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.controller.clusterinfo.ClustersInfo;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.systemfunction.CedarSystemOperator;
import edu.ecnu.woodpecker.util.Log;
import edu.ecnu.woodpecker.util.Util;

public class CedarDeployer
{
    
    /**
     * 远程复制
     * 
     * @param ip
     * @return
     */
    public static boolean remoteReplicate(String ip)
    {
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        Date date = new Date();// 为获取当前系统时间
        String time = df.format(date);

        // 新建部署目录，在该目录下新建time文件夹
        String command = ShellConstant.MKDIR.replace("dirName", CedarDeployInfo.getDeployPath())
                + ShellConstant.OPENDIR.replace("dirName", CedarDeployInfo.getDeployPath() + ";") + ShellConstant.MKDIR.replace("dirName", time);
        Util.exec(ip, userName, password, connectionPort, command);

        StringBuilder scp = new StringBuilder(ShellConstant.SCP);
        int index = scp.indexOf("user");
        scp.replace(index, index + 4, userName);
        index = scp.indexOf("ip");
        scp.replace(index, index + 2, CedarCompileInfo.getMakeIP());
        index = scp.indexOf("path");
        scp.replace(index, index + 4, CedarCompileInfo.getMakePath());

        // 传递编译好的文件，完成后删除time文件夹
        command = ShellConstant.OPENDIR.replace("dirName", CedarDeployInfo.getDeployPath() + ";") + scp
                + ShellConstant.DELETE.replace("path", time) + ";";
        Util.scp(ip, connectionPort, userName, password, command, CedarCompileInfo.getMakeIP());

        Calendar nowTime = Calendar.getInstance();
        nowTime.add(Calendar.MINUTE, 3);

        // 等待复制完成
        while (true)
        {
            command = ShellConstant.OPENDIR.replace("dirName", CedarDeployInfo.getDeployPath() + ";") + ShellConstant.LS;
            String result = Util.exec(ip, userName, password, connectionPort, command);
            String[] ss = result.split(FileConstant.LINUX_LINE_FEED);
            int count = 0;
            for (String s : ss)
            {
                if (s.equals(time))
                    count++;
            }
            if (count == 0)
                return true;
            if (Calendar.getInstance().after(nowTime))
                return false;
        }
    }

    /**
     * 将编译后的CEDAR根据部署配置文件部署到相应的机器上
     * 
     * @param filePath 部署文件地址
     * @return true编译成功
     */
    public static boolean deployCEDAR()
    {

        if (!isCEDARexist())
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "CEDAR does not exist", LogLevelConstant.ERROR);
            return false;
        }
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();

            // 先查询该ip地址上所有的server，若存在则杀死
            //dealWithAliveServer(ip);
            dealWithLocalServer(ip);
        }
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();
            // 判断data等文件是否存在，若存在则删除
            if (!isDataDeleted(ip))
                return false;
        }
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();

            // 如果ip地址与编译的ip地址相同且编译路径和部署路径相同，不复制，否则复制
            if (ip.equals(CedarCompileInfo.getMakeIP()) && CedarCompileInfo.getMakePath().equals(CedarDeployInfo.getDeployPath()))
                Recorder.FunctionRecord(Log.getRecordMetadata(), ip + " exist CEDAR", LogLevelConstant.INFO);
            else
            {
                if (!remoteReplicate(ip))
                    return false;
            }
        }
        return true;
    }

    /**
     * 启动集群,集群启动成功后对全局对象ClustersInfo赋值
     * 
     * @return
     */
    public static boolean startCEDAR()
    {
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();

            // 先查询该ip地址上所有的server，若存在则杀死
            //dealWithAliveServer(ip);
            dealWithLocalServer(ip);
        }
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();
            // 判断data等文件是否存在，若存在则删除
            if (!isDataDeleted(ip))
            {
                return false;
            }
        }
        for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
        {
            String ip = (String) iter.next();

            // 新建data文件
            if (!createFolders(ip))
            {
                return false;
            }
        }

        // 起server
        if (!startCluster(CedarDeployInfo.getServerList()))
        {
            return false;
        }

        // 设主
        if (!setMaster(CedarDeployInfo.getServerList().get(0).getRSIP(), CedarDeployInfo.getRSservicePort()))
        {
            return false;
        }

        ClustersInfo.initialize(initializeCluster(CedarDeployInfo.getServerList()));
//        CedarSystemOperator.sleep(30, "second");
//        if (CedarSystemOperator.isClusterAvailable() != 0)
//        {
//            return false;
//        }
        return true;
    }

    /**
     * 删除编译或部署期间产生的CEDAR文件
     * 
     * @return
     */
    public static boolean initializeCEDAR(String type)
    {
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();

        if (type.equals("compile"))
        {
            String command = ShellConstant.DELETE.replace("path", CedarCompileInfo.getMakePath()) + ShellConstant.EXECRESULT;

            // 删除编译期间产生的CEDAR文件
            String result = Util.exec(CedarCompileInfo.getMakeIP(), userName, password, connectionPort, command);

            for (String item : result.split(FileConstant.LINUX_LINE_FEED))
            {
                if (item.equals("exec_unsuccessful"))
                {

                    Recorder.FunctionRecord(Log.getRecordMetadata(), "Delete CEDAR's productions after compilation fail", LogLevelConstant.ERROR);
                    return false;
                }
            }
        }
        else if (type.equals("deploy"))
        {
            String command = ShellConstant.DELETE.replace("path", CedarDeployInfo.getDeployPath()) + ShellConstant.EXECRESULT;
            for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
            {
                String ip = (String) iter.next();

                // 判断文件是否被删除
                String result = Util.exec(ip, userName, password, connectionPort, command);

                for (String item : result.split(FileConstant.LINUX_LINE_FEED))
                {
                    if (item.equals("exec_unsuccessful"))
                    {

                        Recorder.FunctionRecord(Log.getRecordMetadata(), "Delete CEDAR's productions after deployment fail", LogLevelConstant.ERROR);
                        return false;
                    }
                }
            }
        }
        else
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "Initialize CEDAR type wrong", LogLevelConstant.ERROR);
            return false;
        }
        Recorder.FunctionRecord(Log.getRecordMetadata(), "Delete CEDAR during " + type + " successfully", LogLevelConstant.INFO);
        return true;
    }

    /**
     * 关闭集群 根据传入的type值，若正常关闭则关闭server，删除data文件 不然还需要将log文件转存
     * 
     * @param type
     * @return
     */
    public static boolean closeCEDAR(String type)
    {
        List<Map<String, String>> serverList = new ArrayList<Map<String, String>>();
        if (type.equals("normal"))
        {
            for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
            {
                String ip = (String) iter.next();

                // 先查询该ip地址上所有的server，若存在则杀死
                dealWithAliveServer(ip);

                ClustersInfo.initialize(serverList);
                // 判断data等文件是否存在，若存在则删除
                if (!isDataDeleted(ip))
                    return false;
            }
        }
        else if (type.equals("unnormal"))
        {
            for (Iterator<String> iter = CedarDeployInfo.getIPNIC().keySet().iterator(); iter.hasNext();)
            {
                String ip = (String) iter.next();

                // 先查询该ip地址上所有的server，若存在则杀死
                dealWithAliveServer(ip);

                ClustersInfo.initialize(serverList);
                // 日志转储
                if (!isLogArchived(ip))
                    return false;
                // 判断data等文件是否存在，若存在则删除
                if (!isDataDeleted(ip))
                    return false;
            }
        }
        else
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "Wrong close type", LogLevelConstant.ERROR);
            return false;
        }
        return true;
    }

    /**
     * 注册服务器
     * 
     * @param ip ip地址，在哪台机器上注册
     * @param serverType server类型 MergeServer/ChunkServer
     * @param clusterType cluster类型 master/slave
     * @param NIC 端口号
     */
    public static boolean registerServer(String ip, String serverType, String clusterType, String NIC)
    {
        List<MyServerInfo> rsList = new ArrayList<MyServerInfo>();
        List<MyServerInfo> list = new ArrayList<MyServerInfo>();
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();
        boolean NICexist = false;

        // 将RootServer添加到rsList
        for (MyServerInfo server : CedarDeployInfo.getServerList())
        {
            if (server.getServerType().equals(CedarConstant.ROOTSERVER))
            {
                rsList.add(server);
            }
        }

        int size = rsList.size();
        for (MyServerInfo server : CedarDeployInfo.getServerList())
        {
            if (server.getIP().equals(ip))
            {
                NICexist = true;
                if (server.getServerType().equals(serverType))
                {
                    Recorder.FunctionRecord(Log.getRecordMetadata(), "This server already exist", LogLevelConstant.ERROR);
                    return false;
                }
            }
        }
        for (MyServerInfo server : CedarDeployInfo.getServerList())
        {
            if (server.getIP().equals(ip))
            {
                for (int i = 0; i < rsList.size(); i++)
                {
                    if (server.getRSIP().equals(rsList.get(i).getRSIP()))
                    {
                        if (i == 0 && clusterType.equals(ConfigConstant.SLAVE_LOWER))
                        {
                            Recorder.FunctionRecord(Log.getRecordMetadata(), " This server conflict ", LogLevelConstant.ERROR);

                            return false;
                        }
                        else if (i != 0 && clusterType.equals(ConfigConstant.MASTER_LOWER))
                        {
                            Recorder.FunctionRecord(Log.getRecordMetadata(), " This server conflict ", LogLevelConstant.ERROR);

                            return false;
                        }
                    }
                }
            }
        }

        if (!NICexist)
        {
            // getNIC(ip,userName, password,connectionPort);
            CedarDeployInfo.addIPNIC(ip, NIC);
        }
        // 如果rsList的size为1表示该集群为单级群，没有备集群
        if (size == 1 && clusterType.equals(ConfigConstant.SLAVE_LOWER))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "This server is single cluster without slave cluster", LogLevelConstant.ERROR);
            return false;
        }

        // rsList的第一个为主集群
        else if (clusterType.equals(ConfigConstant.MASTER_LOWER))
        {
            if (serverType.equals(CedarConstant.MERGESERVER))
            {
                // 新建一个server将他添加到DeployInfo的ServerList中
                MyServerInfo newServer = new MyServerInfo();
                newServer.init(rsList.get(0).getRSIP(), ip, serverType);
                CedarDeployInfo.getServerList().add(newServer);
                list.add(newServer);

                // 起server
                if (!startMS(newServer, userName, password, connectionPort))
                {
                    return false;
                }
            }
            else if (serverType.equals(CedarConstant.CHUNKSERVER))
            {
                // 新建一个server将他添加到DeployInfo的ServerList中
                MyServerInfo newServer = new MyServerInfo();
                newServer.init(rsList.get(0).getRSIP(), ip, serverType);
                CedarDeployInfo.getServerList().add(newServer);
                list.add(newServer);

                // 起server
                if (!startCS(newServer, userName, password, connectionPort))
                {
                    return false;
                }
            }
        }
        else if (size != 1 && clusterType.equals(ConfigConstant.SLAVE_LOWER))
        {
            int i = randomNum(0, size);
            // //System.out.println("备集群" + i);
            if (serverType.equals(CedarConstant.MERGESERVER))
            {
                // 新建一个server将他添加到DeployInfo的ServerList中
                MyServerInfo newServer = new MyServerInfo();
                newServer.init(rsList.get(i).getRSIP(), ip, serverType);
                CedarDeployInfo.getServerList().add(newServer);
                list.add(newServer);

                // 起server
                if (!startMS(newServer, userName, password, connectionPort))
                {
                    return false;
                }
            }
            else if (serverType.equals(CedarConstant.CHUNKSERVER))
            {
                // 新建一个server将他添加到DeployInfo的ServerList中
                MyServerInfo newServer = new MyServerInfo();
                newServer.init(rsList.get(i).getRSIP(), ip, serverType);
                CedarDeployInfo.getServerList().add(newServer);
                list.add(newServer);

                // 起server
                if (!startCS(newServer, userName, password, connectionPort))
                {
                    return false;
                }
            }
        }
        ClustersInfo.addServer(initializeCluster(list));
        return true;
    }

    /**
     * @param IP
     * @return NIC's name of specified IP
     */
    public static String getNIC(String IP)
    {
        String user = ClustersInfo.getUserName();
        String password = ClustersInfo.getPassword();

        String NIC = null;
        String getNICcmd = "/sbin/ifconfig";
        String regularExpression = "^.*addr:\\s*" + IP + ".*$";

        BufferedReader reader = null;
        Channel channel = null;
        try
        {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, IP, 22);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(20000);
            session.connect();

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(getNICcmd);
            channel.connect();

            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                if (line.matches("^\\S.*"))
                {
                    // 网卡名
                    NIC = line.split("\\s+")[0];
                }

                if (line.matches(regularExpression))
                {
                    // 匹配到IP
                    return NIC.trim();
                }
            }
            session.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {}
            channel.disconnect();
        }
        return null;
    }

    /**
     * 初始化集群信息 在集群已经启动后想直接执行案例时调用该方法初始化集群信息
     * 
     * @param compileFilePath 编译文件路径
     * @param deployFilePath 部署文件路径
     * @param encoding 编码格式
     */
    public static boolean initializeCluster(String compileFilePath, String deployFilePath, String encoding)
    {
        CedarConfigInitializer.read(compileFilePath, encoding);
        CedarConfigInitializer.read(deployFilePath, encoding);
        String user = TestController.getServerUserName();
        String psw = TestController.getServerPassword();
        int port = CedarCompileInfo.getConnectionPort();
        CedarOperation.setDatabaseUser(CedarCompileInfo.getDatabaseUser());
        CedarOperation.setDatabasePassword(CedarCompileInfo.getDatabasePassword());
        String command = null;
        for (int i = 0; i < CedarDeployInfo.getServerList().size(); i++)
        {
            MyServerInfo server = CedarDeployInfo.getServerList().get(i);
//            StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//            int index = pgrep.indexOf("server");
//            pgrep.replace(index, index + 6, server.getServerType().toLowerCase());
//            index = pgrep.indexOf("user");
//            pgrep.replace(index, index + 4, user);
//            command = pgrep.toString();
//            if (server.getServerType().toLowerCase().equals("listenermergeserver"))
//            {
//                command = CedarConstant.PGREP.replace("server", "mergeserver");
//            }
            String pgrep = new String(CedarConstant.PGREP);
            String cmd = pgrep.replace("serverInformation", "./bin/"+server.getServerType().toLowerCase()+" -r "+server.getIP()+":"+CedarDeployInfo.getRSservicePort());
            if (!isServerStarted(server, user, psw, port, cmd))
            {
                Recorder.FunctionRecord(Log.getRecordMetadata(),
                        "Initialize cluster info without starting server unsuccessfully because one server start unsuccessfully", LogLevelConstant.ERROR);
                return false;
            }
        }
        ClustersInfo.initialize(initializeCluster(CedarDeployInfo.getServerList()));
        Recorder.FunctionRecord(Log.getRecordMetadata(), "Initialize cluster info without starting server", LogLevelConstant.INFO);
        if (CedarSystemOperator.isClusterAvailable() != 0)
        {
            return false;
        }
        return true;
    }

    /**
     * 将server信息存为List<Map<String,String>>类型
     * 
     * @param newServerList
     * @param list
     * @return
     */
    private static List<Map<String, String>> initializeCluster(List<MyServerInfo> list)
    {
        List<Map<String, String>> newServerList = new ArrayList<Map<String, String>>();
        for (MyServerInfo server : list)
        {
            String type = server.getServerType();
            Map<String, String> newServer = new HashMap<String, String>();
            newServer.put("serverType", server.getServerType());
            newServer.put("RSIP", server.getRSIP());
            newServer.put("IP", server.getIP());
            newServer.put("PID", String.valueOf(server.getPID()));
            newServer.put("NIC", CedarDeployInfo.getIPNIC().get(server.getIP()));
            newServer.put("RSPort", String.valueOf(CedarDeployInfo.getRSservicePort()));
            newServer.put("deployPath", CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()));
            newServer.put("userName", CedarCompiler.getUserName());
            newServer.put("password", CedarCompiler.getPassword());
            newServer.put("connectionPort", String.valueOf(CedarCompiler.getConnectionPort()));
            if (type.equals(CedarConstant.ROOTSERVER))
            {
                newServer.put("port", String.valueOf(CedarDeployInfo.getRSservicePort()));
            }
            else if (type.equals(CedarConstant.UPDATESERVER))
            {
                newServer.put("servicePort", String.valueOf(CedarDeployInfo.getUPSservicePort()));
                newServer.put("mergePort", String.valueOf(CedarDeployInfo.getUPSmergePort()));
            }
            else if (type.equals(CedarConstant.MERGESERVER))
            {
                newServer.put("servicePort", String.valueOf(CedarDeployInfo.getMSservicePort()));
                newServer.put("MySQLPort", String.valueOf(CedarDeployInfo.getMSMySQLPort()));
                newServer.put("isListener", "false");
            }
            else if (type.equals(CedarConstant.LISTENERMERGESERVER))
            {
                newServer.put("servicePort", String.valueOf(CedarDeployInfo.getLMSservicePort()));
                newServer.put("MySQLPort", String.valueOf(CedarDeployInfo.getLMSMySQLPort()));
                newServer.put("isListener", "true");
            }
            else if (type.equals(CedarConstant.CHUNKSERVER))
            {
                newServer.put("servicePort", String.valueOf(CedarDeployInfo.getCSservicePort()));
                newServer.put("appName", CedarDeployInfo.getAppName());
            }
            // //System.out.println("newServer:"+newServer.get("serverType")+"---"+newServer.get("RSIP"));
            newServerList.add(newServer);

        }
        return newServerList;
    }

    /**
     * 在begin和end之间生成随机整数
     * 
     * @param begin
     * @param end
     * @return
     */
    private static int randomNum(int begin, int end)
    {
        int rtnn = begin + (int) (Math.random() * (end - begin));
        if (rtnn == begin || rtnn == end)
        {
            return randomNum(begin, end);
        }
        return rtnn;
    }

    /**
     * 判断编译的cedar文件是否存在
     * 
     * @return
     */
    private static boolean isCEDARexist()
    {
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();
        String command = ShellConstant.OPENDIR.replace("dirName", CedarCompileInfo.getMakePath()) + ShellConstant.EXECRESULT;
        String result = Util.exec(CedarCompileInfo.getMakeIP(), userName, password, connectionPort, command);
        // System.out.println(CompileInfo.getMakeIP() +
        // command + result);
        for (String item : result.split(FileConstant.LINUX_LINE_FEED))
        {
            if (item.equals("exec_successful"))
                return true;
        }

        return false;
    }

    /**
     * 日志转存
     * 
     * @return
     */
    private static boolean isLogArchived(String ip)
    {
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");// 设置日期格式
        // new Date()为获取当前系统时间
        String time = df.format(new Date());

        // 新建目录
        String command = ShellConstant.MKDIR.replace("dirName", CedarDeployInfo.getLogPath() + "/" + CedarDeployInfo.getFileName() + "/" + time)
                + ShellConstant.MKDIR.replace("dirName",
                        CedarDeployInfo.getLogPath() + "/" + CedarDeployInfo.getFileName() + "/" + time + "/pyctest");
        Util.exec(ip, userName, password, connectionPort, command);

        // 日志转存
        StringBuilder scp = new StringBuilder(ShellConstant.SCP);
        int index = scp.indexOf("user");
        scp.replace(index, index + 4, userName);
        index = scp.indexOf("path");
        scp.replace(index, index + 4, CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + "/log");
        command = ShellConstant.OPENDIR.replace("dirName", CedarDeployInfo.getLogPath() + "/" + CedarDeployInfo.getFileName() + "/" + time + ";")
                + scp + ShellConstant.DELETE.replace("path", "pyctest");

        Util.scp(ip, connectionPort, userName, password, command, ip);

        // 判断日志是否转存完毕

        command = ShellConstant.OPENDIR.replace("dirname", CedarDeployInfo.getLogPath() + "/" + CedarDeployInfo.getFileName() + "/" + time + ";")
                + ShellConstant.LS;
        boolean isExist = false;
        Calendar nowTime = Calendar.getInstance();
        nowTime.add(Calendar.MINUTE, 3);
        while (true)
        {
            int count = 0;
            String result = Util.exec(ip, userName, password, connectionPort, command);
            for (String item : result.split(FileConstant.LINUX_LINE_FEED))
            {
                if (item.equals("pyctest"))
                {
                    count++;
                }
            }
            if (count == 0)
            {

                break;
            }
            if (Calendar.getInstance().after(nowTime))
            {
                isExist = true;
                break;
            }
        }
        if (isExist)
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "IP log archive unsuccessfully", LogLevelConstant.ERROR);
            return false;
        }
        Recorder.FunctionRecord(Log.getRecordMetadata(), "IP log archive successfully", LogLevelConstant.INFO);
        return true;
    }

    /**
     * 设主
     * 
     * @param rsip 主集群ip
     * @param RSservicePort 主集群port
     * @return
     */
    private static boolean setMaster(String ip, int port)
    {
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();

        StringBuilder setmaster = new StringBuilder(CedarConstant.SETMASTER);
        int index = setmaster.indexOf("ip");
        setmaster.replace(index, index + 2, ip);
        index = setmaster.indexOf("port");
        setmaster.replace(index, index + 4, String.valueOf(port));
        index = setmaster.indexOf("ip");
        setmaster.replace(index, index + 2, ip);
        index = setmaster.indexOf("port");
        setmaster.replace(index, index + 4, String.valueOf(port));
        String command = "sleep 15;" + ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + setmaster;
        // System.out.println(command);
        String result = Util.exec(ip, userName, password, connectionPort, command);
        int count = 0;
        for (String item : result.split(FileConstant.LINUX_LINE_FEED))
        {
            if (item.toUpperCase().equals("OKAY"))
            {
                count++;
            }
            if (count == 2)
            {
                Recorder.FunctionRecord(Log.getRecordMetadata(), ip + " set master successfully", LogLevelConstant.INFO);
                return true;
            }
        }

        Recorder.FunctionRecord(Log.getRecordMetadata(), ip + " set master unsuccessfully", LogLevelConstant.ERROR);
        return false;
    }

    /**
     * 起集群
     * 
     * @param serverList
     * @return
     */
    private static boolean startCluster(List<MyServerInfo> serverList)
    {
        List<MyServerInfo> rsList = new ArrayList<MyServerInfo>();
        String userName = CedarCompileInfo.getUserName();
        String password = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();

        // 将RootServer添加到rsList
        for (MyServerInfo server : serverList)
        {
            // System.out.println("serverlist" +
            // server.getRSIP() + server.getServerType());
            if (server.getServerType().equals(CedarConstant.ROOTSERVER))
            {
                rsList.add(server);
            }
        }

        // 起单级群rs
        if (rsList.size() == 1 && !startRSone(rsList, userName, password, connectionPort))
        {
            return false;
        }

        // 起三集群rs
        else if (rsList.size() != 1)
        {
            for (MyServerInfo server : serverList)
            {
                String serverType = server.getServerType();
                if (serverType.equals(CedarConstant.ROOTSERVER) && !startRSthree(rsList, userName, password, connectionPort, server))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.UPDATESERVER) && !startUPS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.MERGESERVER) && !startMS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.LISTENERMERGESERVER) && !startLMS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.CHUNKSERVER) && !startCS(server, userName, password, connectionPort))
                {
                    return false;
                }
            }
        }

        if (rsList.size() == 1)
        {
            for (MyServerInfo server : serverList)
            {
                String serverType = server.getServerType();
                if (serverType.equals(CedarConstant.UPDATESERVER) && !startUPS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.MERGESERVER) && !startMS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.LISTENERMERGESERVER) && !startLMS(server, userName, password, connectionPort))
                {
                    return false;
                }
                else if (serverType.equals(CedarConstant.CHUNKSERVER) && !startCS(server, userName, password, connectionPort))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 创建data文件和相关映射
     * 
     * @param ip
     * @return
     */
    private static boolean createFolders(String ip)
    {
        String current = getCurrentPath(CedarCompileInfo.getMakePath());
        String deploy = CedarDeployInfo.getDeployPath();
        String path = deploy + "/" + current;

        String command = ShellConstant.FORDISK + ShellConstant.MKDIR.replace("dirName", deploy + "/data/$disk/ups_data")
                + ShellConstant.DONE + ";" + ShellConstant.MKDIR.replace("dirName", path + "/data")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/rs")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/rs_commitlog")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_commitlog")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_wasmaster")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_commitpoint")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_data/raid0")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_data/raid2")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_data/raid1")
                + ShellConstant.MKDIR.replace("dirName", path + "/data/ups_data/raid3")
                + ShellConstant.LN.replace("path", deploy + "/data/1/ups_data " + path + "/data/ups_data/raid0/store0")
                + ShellConstant.LN.replace("path", deploy + "/data/2/ups_data " + path + "/data/ups_data/raid0/store1")
                + ShellConstant.LN.replace("path", deploy + "/data/3/ups_data " + path + "/data/ups_data/raid1/store0")
                + ShellConstant.LN.replace("path", deploy + "/data/4/ups_data " + path + "/data/ups_data/raid1/store1")
                + ShellConstant.LN.replace("path", deploy + "/data/5/ups_data " + path + "/data/ups_data/raid2/store0")
                + ShellConstant.LN.replace("path", deploy + "/data/6/ups_data " + path + "/data/ups_data/raid2/store1")
                + ShellConstant.LN.replace("path", deploy + "/data/7/ups_data " + path + "/data/ups_data/raid3/store0")
                + ShellConstant.LN.replace("path", deploy + "/data/8/ups_data " + path + "/data/ups_data/raid3/store1")
                + ShellConstant.FORDISK + ShellConstant.MKDIR.replace("dirName", deploy + "/data/$disk" + "/" + current + "/sstable")
                + ShellConstant.DONE + ";" + ShellConstant.FORDISK
                + ShellConstant.MKDIR.replace("dirName", deploy + "/data/$disk/obtest/sstable") + ShellConstant.DONE + ";"
                + ShellConstant.FORDISK + ShellConstant.LN.replace("path", deploy + "/data/$disk " + path + "/data/$disk")
                + ShellConstant.DONE + ShellConstant.EXECRESULT;

        String result = Util.exec(ip, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), command);
        // System.out.println(ip + command + result);
        for (String item : result.split(FileConstant.LINUX_LINE_FEED))
        {
            if (item.equals("exec_unsuccessful"))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据传入目录获得当前目录
     * 
     * @param path
     * @return
     */
    public static String getCurrentPath(String path)
    {
        String ss[] = path.split("/");
        String aa = ss[ss.length - 1];
        return aa;
    }

    /**
     * 判断data、log等数据是否删除成功
     * 
     * @param ip 该ip地址下
     * @return
     */
    private static boolean isDataDeleted(String ip)
    {
        String path = CedarDeployInfo.getDeployPath();
        String current = getCurrentPath(CedarCompileInfo.getMakePath());
        String command = ShellConstant.DELETE.replace("path", path + "/data;")
                + ShellConstant.DELETE.replace("path", path + "/" + current + "/data;")
                + ShellConstant.DELETE.replace("path", path + "/" + current + "/log;")
                + ShellConstant.DELETE.replace("path", path + "/" + current + "/run;")
                + ShellConstant.DELETE.replace("path", path + "/" + current + "/rs_admin.log") + ShellConstant.EXECRESULT;

        String result = Util.exec(ip, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), command);
        for (String item : result.split(FileConstant.LINUX_LINE_FEED))
        {
            if (item.equals("exec_unsuccessful"))
            {
                Recorder.FunctionRecord(Log.getRecordMetadata(), ip + " delete data unsuccessfully", LogLevelConstant.ERROR);
                return false;
            }

        }
        Recorder.FunctionRecord(Log.getRecordMetadata(), ip + " delete data successfully", LogLevelConstant.INFO);
        return true;
    }

    /**
     * 关闭该IP下该用户的所有cedar server
     * 
     * @param IP
     */
    private static void dealWithAliveServer(String IP)
    {
        String cmd = "kill -9 `pgrep 'rootserver|chunkserver|mergeserver|updateserver' -u " + CedarCompileInfo.getUserName() + "`";
        Util.exec(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all servers on " + IP + " successfully", LogLevelConstant.INFO);

    }

    
    /**
     * 关闭该IP下该用户下所属cedar目录中run文件中对应的cedar server
     * 
     * @param IP
     */
    private static void dealWithLocalServer(String IP)   //remain to improve
    {
        //kill old process which old port made
        String command="kill -9 `ps ux|grep 'serverInformation'|grep -v grep |awk '{print $2}'`";
        for(String ip:CedarConfigInitializer.getIPList())
        {
            String ipPort=ip+":"+CedarDeployInfo.getRSservicePort();
            String cmd = command.replace("serverInformation", ipPort);
            Util.exec(ip, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
        }
//        for(String string:CedarDeployer.startRootserver)
//        {
//            String[] stringSpilt=string.split(";");
//            String cmd = command.replace("serverInformation", stringSpilt[1]);
//            String executeIP = stringSpilt[0];
//            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
//        }
//        for(String string:CedarDeployer.startUpdateserver)
//        {
//            String[] stringSpilt=string.split(";");
//            String cmd = command.replace("serverInformation", stringSpilt[1]);
//            String executeIP = stringSpilt[0];
//            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
//        }
//        for(String string:CedarDeployer.startMergeserver)
//        {
//            String[] stringSpilt=string.split(";");
//            String cmd = command.replace("serverInformation", stringSpilt[1]);
//            String executeIP = stringSpilt[0];
//            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
//        }
//        for(String string:CedarDeployer.startChunkserver)
//        {
//            String[] stringSpilt=string.split(";");
//            String cmd = command.replace("serverInformation", stringSpilt[1]);
//            String executeIP = stringSpilt[0];
//            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), cmd);
//        }
        
        //kill all run file pid
        String KillRunPidCmd=null;
        for(String executeIP:CedarConfigInitializer.getIPList())
        {
            KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                    CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' rootserver.pid`";
            
            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
            KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                    CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' updateserver.pid`";
            
            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
            KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                    CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' chunkserver.pid`";
            
            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
            KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                    CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' mergeserver.pid`";
            
            Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill pid in run file on " + IP + " successfully", LogLevelConstant.INFO);
        }
    }
    
    /**
     * 启动ms
     * 
     * @param ms MergeServerInfo
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该ms启动成功
     */
    private static boolean startMS(MyServerInfo ms, String user, String psw, int port)
    {
        StringBuilder startMS = new StringBuilder(CedarConstant.STARTMS);
        int index = startMS.indexOf("ip");
        startMS.replace(index, index + 2, ms.getRSIP());
        index = startMS.indexOf("port");
        startMS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startMS.indexOf("servicePort");
        startMS.replace(index, index + 11, String.valueOf(CedarDeployInfo.getMSservicePort()));
        index = startMS.indexOf("MySQLPort");
        startMS.replace(index, index + 9, String.valueOf(CedarDeployInfo.getMSMySQLPort()));
        index = startMS.indexOf("NIC");
        startMS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(ms.getIP()));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startMS;
        // System.out.println(command);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start MS on " + ms.getIP() + " with RS IP " + ms.getRSIP(), LogLevelConstant.INFO);

        
        Util.exec(ms.getIP(), user, psw, port, command);
//        StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "mergeserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
//        // System.out.println(pgrep);
//        return isServerStarted(ms, user, psw, port, String.valueOf(pgrep));
        String pgrep = new String(CedarConstant.PGREP);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startMS).substring(0, String.valueOf(startMS).length()-1));
        return isServerStarted(ms, user, psw, port, cmd);
    }

    /**
     * 启动lms
     * 
     * @param ms MergeServer
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该ms启动成功
     */
    private static boolean startLMS(MyServerInfo lms, String user, String psw, int port)
    {
        StringBuilder startMS = new StringBuilder(CedarConstant.STARTLMS);
        int index = startMS.indexOf("ip");
        startMS.replace(index, index + 2, lms.getRSIP());
        index = startMS.indexOf("port");
        startMS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startMS.indexOf("servicePort");
        startMS.replace(index, index + 11, String.valueOf(CedarDeployInfo.getLMSservicePort()));
        index = startMS.indexOf("MySQLPort");
        startMS.replace(index, index + 9, String.valueOf(CedarDeployInfo.getLMSMySQLPort()));
        index = startMS.indexOf("NIC");
        startMS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(lms.getIP()));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startMS;
        // System.out.println(command);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start LMS on " + lms.getIP() + " with RS IP " + lms.getRSIP(), LogLevelConstant.INFO);

        Util.exec(lms.getIP(), user, psw, port, command);

//        StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "mergeserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
//        // System.out.println(pgrep);
//        return isServerStarted(lms, user, psw, port, String.valueOf(pgrep));
        String pgrep = new String(CedarConstant.PGREP);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startMS).substring(0, String.valueOf(startMS).length()-1));
        return isServerStarted(lms, user, psw, port, cmd);
    }

    /**
     * 启动cs
     * 
     * @param cs ChunkServerInfo
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该cs启动成功
     */
    private static boolean startCS(MyServerInfo cs, String user, String psw, int port)
    {
        StringBuilder startCS = new StringBuilder(CedarConstant.STARTCS);
        int index = startCS.indexOf("ip");
        startCS.replace(index, index + 2, cs.getRSIP());
        index = startCS.indexOf("port");
        startCS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startCS.indexOf("servicePort");
        startCS.replace(index, index + 11, String.valueOf(CedarDeployInfo.getCSservicePort()));
        index = startCS.indexOf("appName");
        startCS.replace(index, index + 7, CedarDeployInfo.getAppName());
        index = startCS.indexOf("NIC");
        startCS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(cs.getIP()));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startCS;
        // System.out.println(command);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start CS on " + cs.getIP() + " with RS IP " + cs.getRSIP(), LogLevelConstant.INFO);

        Util.exec(cs.getIP(), user, psw, port, command);

//        StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "chunkserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
//        // System.out.println(pgrep);
//        return isServerStarted(cs, user, psw, port, String.valueOf(pgrep));
        String pgrep = new String(CedarConstant.PGREP);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startCS).substring(0, String.valueOf(startCS).length()-1));
        return isServerStarted(cs, user, psw, port, cmd);
    }

    /**
     * 启动ups
     * 
     * @param ups UpdateServerInfo
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该ups启动成功
     */
    private static boolean startUPS(MyServerInfo ups, String user, String psw, int port)
    {
        StringBuilder startUPS = new StringBuilder(CedarConstant.STARTUPS);
        int index = startUPS.indexOf("ip");
        startUPS.replace(index, index + 2, ups.getRSIP());
        index = startUPS.indexOf("port");
        startUPS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startUPS.indexOf("servicePort");
        startUPS.replace(index, index + 11, String.valueOf(CedarDeployInfo.getUPSservicePort()));
        index = startUPS.indexOf("mergePort");
        startUPS.replace(index, index + 9, String.valueOf(CedarDeployInfo.getUPSmergePort()));
        index = startUPS.indexOf("NIC");
        startUPS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(ups.getIP()));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startUPS;
        // System.out.println(command);

        Recorder.FunctionRecord(Log.getRecordMetadata(), "start UPS on " + ups.getIP() + " with RS IP " + ups.getRSIP(), LogLevelConstant.INFO);
    
        Util.exec(ups.getIP(), user, psw, port, command);
//        StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "updateserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
//        // System.out.println(pgrep);
//        return isServerStarted(ups, user, psw, port, String.valueOf(pgrep));
        String pgrep = new String(CedarConstant.PGREP);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startUPS).substring(0, String.valueOf(startUPS).length()-1));
        return isServerStarted(ups, user, psw, port, cmd);
    }

    /**
     * 启动单级群的rs
     * 
     * @param rs RootServerInfo
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该rs启动成功
     */
    private static boolean startRSone(List<MyServerInfo> rslist, String user, String psw, int port)
    {

        StringBuilder startRS = new StringBuilder(CedarConstant.STARTRSONE);
        int index = startRS.indexOf("ip");
        startRS.replace(index, index + 2, rslist.get(0).getRSIP());
        index = startRS.indexOf("port");
        startRS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startRS.indexOf("ip");
        startRS.replace(index, index + 2, rslist.get(0).getRSIP());
        index = startRS.indexOf("port");
        startRS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startRS.indexOf("ip");
        startRS.replace(index, index + 2, rslist.get(0).getRSIP());
        index = startRS.indexOf("port");
        startRS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startRS.indexOf("NIC");
        startRS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(rslist.get(0).getIP()));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startRS;
        //System.out.println(command);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start single cluster's RS on " + rslist.get(0).getIP(), LogLevelConstant.INFO);

//      //usage to kill cedar
//        CedarDeployer.startRootserver.add(rslist.get(0).getRSIP()+";"+command);
        
        
          Util.exec(rslist.get(0).getRSIP(), user, psw, port, command);
//        StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "rootserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
//        System.out.println(pgrep);
//        return isServerStarted(rslist.get(0), user, psw, port, String.valueOf(pgrep));
        String pgrep = new String(CedarConstant.PGREP);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startRS).substring(0, String.valueOf(startRS).length()-1));
        return isServerStarted(rslist.get(0), user, psw, port, cmd);
    }

    /**
     * 启动三级群的rs
     * 
     * @param rs RootServerInfo
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @return true代表该rs启动成功
     */
    private static boolean startRSthree(List<MyServerInfo> rslist, String user, String psw, int port, MyServerInfo rs)
    {
        int j = 0;
        for (int i = 0; i < rslist.size(); i++)
        {
            if (rs.getRSIP() == rslist.get(i).getRSIP())
            {
                j = i + 1;
            }
        }
        StringBuilder startRS = new StringBuilder(CedarConstant.STARTRSTHREE);
        int index = startRS.indexOf("ip");
        startRS.replace(index, index + 2, rslist.get(j - 1).getRSIP());
        index = startRS.indexOf("port");
        startRS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startRS.indexOf("ip");
        startRS.replace(index, index + 2, rslist.get(j - 1).getRSIP());
        index = startRS.indexOf("port");
        startRS.replace(index, index + 4, String.valueOf(CedarDeployInfo.getRSservicePort()));
        index = startRS.indexOf("iplist");
        String iplistx = rslist.get(0).getRSIP() + ":" + CedarDeployInfo.getRSservicePort() + "@1#" + rslist.get(1).getRSIP() + ":"
                + CedarDeployInfo.getRSservicePort() + "@2#" + rslist.get(2).getRSIP() + ":" + CedarDeployInfo.getRSservicePort() + "@3";
        startRS.replace(index, index + 6, iplistx);
        index = startRS.indexOf("NIC");
        startRS.replace(index, index + 3, CedarDeployInfo.getIPNIC().get(rslist.get(j-1).getIP()));
        index = startRS.indexOf("num");
        startRS.replace(index, index + 3, String.valueOf(j));
        String command = ShellConstant.OPENDIR.replace("dirName",
                CedarDeployInfo.getDeployPath() + "/" + getCurrentPath(CedarCompileInfo.getMakePath()) + ";") + startRS;
        // System.out.println(command);
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start three cluster's RS on " + rs.getIP(), LogLevelConstant.INFO);
       
        Util.exec(rs.getIP(), user, psw, port, command);

        String pgrep = new String(CedarConstant.PGREP);
//        index = pgrep.indexOf("server");
//        pgrep.replace(index, index + 6, "rootserver");
//        index = pgrep.indexOf("user");
//        pgrep.replace(index, index + 6, user);
        String cmd = pgrep.replace("serverInformation", String.valueOf(startRS).substring(0, String.valueOf(startRS).length()-1));
        // System.out.println(pgrep);
        if (!isServerStarted(rs, user, psw, port, cmd))
        {
            return false;
        }
        return true;
    }

    /**
     * 判断server是否启动成功 获取该类型server的进程号，若存在将该进程号赋值于该server
     * 
     * @param s ServerInfo(rs,ups,cs,ms)
     * @param user 用户名
     * @param psw 密码
     * @param port 端口号
     * @param command 执行语句
     * @return true代表启动server成功
     */
    private static boolean isServerStarted(MyServerInfo server, String user, String psw, int port, String command)
    {
        String result = Util.exec(server.getIP(), user, psw, port, command);
        if (result.equals(""))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "start server on " + server.getIP() + " unsuccessfully", LogLevelConstant.ERROR);
            return false;
        }
        server.setPID(Integer.valueOf(result.replace(FileConstant.LINUX_LINE_FEED, "")));
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start server on " + server.getIP() + " successfully", LogLevelConstant.INFO);

        return true;
    }

    /**
     * 单元测试用
     * 
     * @param args0
     */
    public static void main(String[] args0)
    {
        String filePath = "./config/CEDAR_compile.conf";
        CedarConfigInitializer.read(filePath, "GB2312");
        filePath = "./config/3RS_3UPS_3MS_3CS.conf";
        CedarConfigInitializer.read(filePath, "GB2312");
        // //System.out.println(DeployInfo.getServerList());
        ClustersInfo.initialize(initializeCluster(CedarDeployInfo.getServerList()));
        // remoteReplicate("10.11.1.195");
        // deployCEDAR();

        //
        // startCEDAR();
        // registerServer("10.11.1.197", MergeServer,
        // "master");
        // registerServer("10.11.1.198", MergeServer,
        // "master");
        // closeCEDAR("normal");
        // initializeCEDAR("deploy");
        // String command = "vi /proc/net/dev;";
        // String aa= Util.exec("10.11.1.199",
        // CompileInfo.getUserName(),
        // CompileInfo.getPassword(),
        // CompileInfo.getConnectionPort(), command);
        // //System.out.println("---"+aa+"---");
        // getNIC("10.11.1.192", CompileInfo.getUserName(),
        // CompileInfo.getPassword(),
        // CompileInfo.getConnectionPort());
        // String command="ls";
        // System.out.print("aaaa"+CompileInfo.getConnectionPort());
        // Util.exec("10.11.1.192",
        // CompileInfo.getUserName(),
        // CompileInfo.getPassword(),
        // CompileInfo.getConnectionPort(), command);
    }
}

class CedarDeployInfo
{
    /**
     * 部署文件名
     */
    private static String fileName = null;

    /**
     * cs中appname
     */
    private static String appName = null;

    /**
     * 日志转存路径
     */
    private static String logPath = null;
    /**
     * server的部署路径（绝对路径）
     */
    private static String deployPath = null;

    /**
     * rs端口号
     */
    private static int RSservicePort;

    /**
     * ups中 -p的端口号
     */
    private static int UPSservicePort;

    /**
     * ups中 -m的端口号
     */
    private static int UPSmergePort;

    /**
     * cs的端口号
     */
    private static int CSservicePort;

    /**
     * ms中 -p的端口号
     */
    private static int MSservicePort;

    /**
     * ms中 -z的端口号
     */
    private static int MSMySQLPort;

    /**
     * lms中 -p的端口号
     */
    private static int LMSservicePort;

    /**
     * lms中 -z的端口号
     */
    private static int LMSMySQLPort;

    /**
     * IP_NIC
     */
    private static Map<String, String> IPNIC;

    /**
     * server列表
     */
    private static List<MyServerInfo> serverList;

    public static String getDeployPath()
    {
        return deployPath;
    }

    public static void setDeployPath(String deployPath)
    {
        CedarDeployInfo.deployPath = deployPath;
    }

    public static int getRSservicePort()
    {
        return RSservicePort;
    }

    public static void setRSservicePort(int rSservicePort)
    {
        RSservicePort = rSservicePort;
    }

    public static int getUPSservicePort()
    {
        return UPSservicePort;
    }

    public static void setUPSservicePort(int uPSservicePort)
    {
        UPSservicePort = uPSservicePort;
    }

    public static int getUPSmergePort()
    {
        return UPSmergePort;
    }

    public static void setUPSmergePort(int uPSmergePort)
    {
        UPSmergePort = uPSmergePort;
    }

    public static int getCSservicePort()
    {
        return CSservicePort;
    }

    public static void setCSservicePort(int cSservicePort)
    {
        CSservicePort = cSservicePort;
    }

    public static int getMSservicePort()
    {
        return MSservicePort;
    }

    public static void setMSservicePort(int mSservicePort)
    {
        MSservicePort = mSservicePort;
    }

    public static int getMSMySQLPort()
    {
        return MSMySQLPort;
    }

    public static void setMSMySQLPort(int mSMySQLPort)
    {
        MSMySQLPort = mSMySQLPort;
    }

    public static int getLMSservicePort()
    {
        return LMSservicePort;
    }

    public static void setLMSservicePort(int lMSservicePort)
    {
        LMSservicePort = lMSservicePort;
    }

    public static int getLMSMySQLPort()
    {
        return LMSMySQLPort;
    }

    public static void setLMSMySQLPort(int lMSMySQLPort)
    {
        LMSMySQLPort = lMSMySQLPort;
    }

    public static Map<String, String> getIPNIC()
    {
        return IPNIC;
    }

    public static void setIPNIC(Map<String, String> iPNIC)
    {
        IPNIC = iPNIC;
    }

    public static void addIPNIC(String ip, String NIC)
    {
        IPNIC.put(ip, NIC);
    }

    public static List<MyServerInfo> getServerList()
    {
        return serverList;
    }

    public static void setServerList(List<MyServerInfo> serverList)
    {
        CedarDeployInfo.serverList = serverList;
    }

    public static String getFileName()
    {
        return fileName;
    }

    public static void setFileName(String fileName)
    {
        CedarDeployInfo.fileName = fileName;
    }

    public static String getAppName()
    {
        return appName;
    }

    public static void setAppName(String appName)
    {
        CedarDeployInfo.appName = appName;
    }

    public static String getLogPath()
    {
        return logPath;
    }

    public static void setLogPath(String logPath)
    {
        CedarDeployInfo.logPath = logPath;
    }

}

class MyServerInfo
{
    private String RSIP;
    private String IP;
    private String serverType;
    private int PID;

    public String getRSIP()
    {
        return RSIP;
    }

    public void setRSIP(String rSIP)
    {
        RSIP = rSIP;
    }

    public String getIP()
    {
        return IP;
    }

    public void setIP(String iP)
    {
        IP = iP;
    }

    public String getServerType()
    {
        return serverType;
    }

    public void setServerType(String serverType)
    {
        this.serverType = serverType;
    }

    public int getPID()
    {
        return PID;
    }

    public void setPID(int pID)
    {
        PID = pID;
    }

    public void init(String RSIP, String IP, String serverType)
    {
        this.RSIP = RSIP;
        this.IP = IP;
        this.serverType = serverType;
    }

}