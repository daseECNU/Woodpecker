package edu.ecnu.woodpecker.environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import edu.ecnu.woodpecker.constant.CedarConstant;
import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.util.Log;


public class CedarConfigInitializer
{

    private static Map<String, String> IPNIC = new HashMap<String, String>();
    private static List<MyServerInfo> serverList = new ArrayList<MyServerInfo>();

    private static String content = null;
    private static String IP = null;
    private static String RSIP = null;
    
    private static String MS = "MS";
    private static String CS = "CS";
    private static String UPS = "UPS";
    private static String RS = "RS";
    private static String LMS = "LMS";

    private static List<String> ipList = new ArrayList<>();
    private static String deployPath=null;
    /**
     * 读取配置文件
     * 
     * @param filePath 文件路径
     * @param encoding 编码格式
     * @return 读取过程是否正确
     */
    public static boolean read(String filePath, String encoding)
    {
        CedarCompileInfo.setUserName(TestController.getServerUserName());
        CedarCompileInfo.setPassword(TestController.getServerPassword());

        File file = new File(filePath);
        // 判断文件是否存在
        if (file.isFile() && file.exists())
        {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding)))
            {
                getFileName(filePath);

                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null)
                {
                    if (lineTxt.matches("(\\s*#.*)|(\\s*)"))
                    {
                        // 注释行或空行
                        continue;
                    }

                    String confItemValue[] = lineTxt.split(SignConstant.ASSIGNMENT_STR);
                    String confItem = confItemValue[0].toLowerCase().trim();
                    content = confItemValue[1].trim();

                    if (confItem.equals(ConfigConstant.DATABASE_USER))
                    {
                        CedarCompileInfo.setDatabaseUser(content);
                    }
                    else if (confItem.equals(ConfigConstant.DATABASE_PASSWORD))
                    {
                        CedarCompileInfo.setDatabasePassword(content);
                    }
                    else if (confItem.equals(ConfigConstant.SRC_PATH))
                    {
                        CedarCompileInfo.setSrcPath(getContent(content));
                        CedarCompileInfo.setSrcIP(getIP(content));
                        CedarCompileInfo.setSrcPath(getAddress(content));
                    }
                    else if (confItem.equals(ConfigConstant.MAKE_PATH))
                    {
                        CedarCompileInfo.setMakePath(getContent(content));
                        CedarCompileInfo.setMakeIP(getIP(content));
                        CedarCompileInfo.setMakePath(getAddress(content));
                    }
                    else if (confItem.equals(ConfigConstant.CORE))
                    {
                        CedarCompileInfo.setCore(Integer.valueOf(content));
                    }
                    else if (confItem.equals(ConfigConstant.COMPILE_TOOLS))
                    {
                        CedarCompileInfo.setCompileTools(Boolean.valueOf(content));
                    }
                    else if (confItem.equals(ConfigConstant.CONNECTION_PORT))
                    {
                        CedarCompileInfo.setConnectionPort(Integer.valueOf(content));
                    }
                    else if (confItem.equals(ConfigConstant.DEPLOY_PATH))
                    {
                        deployPath=content;
                        CedarDeployInfo.setDeployPath(getContent(content));
                        
                    }
                    else if (confItem.equals(ConfigConstant.LOG_PATH))
                    {
                        CedarDeployInfo.setLogPath(getContent(content));
                    }
                    else if (confItem.equals(ConfigConstant.CEDAR_PORT))
                    {
                        setServerPort(content);
                    }
                    else if (confItem.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}"))
                    {
                        // 匹配IPv4地址
                        IP = confItem;
                        IPNIC.put(IP, getNIC(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword()));
                        addServer(content);
                        ipList.add(IP);
                    }
                    else
                    {
                        // 输入的配置文件内容格式错误，记日志
                        Recorder.FunctionRecord(Log.getRecordMetadata(), "Doesn't exist config item " + confItem, LogLevelConstant.ERROR);
                        return false;
                    }
                }
                CedarDeployInfo.setIPNIC(IPNIC);
                Recorder.FunctionRecord(Log.getRecordMetadata(), "Read configuration " + filePath + " successfully", LogLevelConstant.INFO);
                CedarDeployInfo.setAppName("obtest");
                return true;
            }
            catch (Exception e)
            {
                // 记录日志，读取配置文件出错
                Recorder.FunctionRecord(Log.getRecordMetadata(), "Read configuration " + filePath + "  unsuccessfully",
                        LogLevelConstant.ERROR);
                e.printStackTrace();
                return false;
            }
        } // end of if (file.isFile() && file.exists())
        else
        {
            // 记录日志，无法找到配置文件
            Recorder.FunctionRecord(Log.getRecordMetadata(), "Can't find the file:" + filePath, LogLevelConstant.ERROR);
            return false;
        }

    }

    /**
     * 获取文件名
     * 
     * @param filePath 文件路径
     */
    private static void getFileName(String filePath)
    {

        String item[] = filePath.split(FileConstant.FILE_SEPARATOR);
        item = item[item.length - 1].split("\\.");
        // item = item[item.length -
        // 1].split(SignConstant.DOT_STR);
        // System.out.println("文件名：" + item[0]);
        CedarDeployInfo.setFileName(item[0]);
    }

    /**
     * 根据"划分字符串，获得""之间的字符串
     * 
     * @param str
     * @return
     */
    private static String getContent(String str)
    {
        if (str.indexOf("\"") >= 0)
        {
            str = str.substring(str.indexOf("\"") + 1);
            if (str.indexOf("\"") >= 0)
            {
                content = str.substring(0, str.indexOf("\""));
            }
        }
        else
        {
            content = null;
        }
        return content;
    }

    /**
     * 根据@划分字符串，获得IP地址
     * 
     * @param str
     * @return
     */
    private static String getIP(String str)
    {

        return str.split(SignConstant.AT_STR)[0];
    }

    /**
     * 根据/划分字符串，获得(/home/user_name/)相对地址
     * 
     * @param str
     * @return
     */
    private static String getAddress(String str)
    {
        return str.split(SignConstant.AT_STR)[1];
    }
    
    public static List<String> getIPList()
    {
        return CedarConfigInitializer.ipList;
    }
    
    public static String getDeployPath()
    {
        return CedarConfigInitializer.deployPath;
    }
    /**
     * 解析配置文件，调用setServer往server列表中添加server，根据|划分字符
     * 
     * @param content server类型的组合
     */
    private static void addServer(String content)
    {
        String[] types = content.split(SignConstant.BAR);
        for (int i = 0; i < types.length; i++)
        {
            String serverType = types[i].trim().toUpperCase();
            if (serverType.equals(MS))
            {
                setServer(RSIP, IP, CedarConstant.MERGESERVER);
            }
            else if (serverType.equals(LMS))
            {
                setServer(RSIP, IP, CedarConstant.LISTENERMERGESERVER);
            }
            else if (serverType.equals(UPS))
            {
                setServer(RSIP, IP, CedarConstant.UPDATESERVER);
            }
            else if (serverType.equals(RS))
            {
                RSIP = IP;
                setServer(RSIP, IP, CedarConstant.ROOTSERVER);
            }
            else if (serverType.equals(CS))
            {
                setServer(RSIP, IP, CedarConstant.CHUNKSERVER);
            }
        }
    }

    /**
     * 获得server对应的端口号 根据;划分字符串str，得到a[] 再根据：划分字符串a[i],得到b[]
     * 匹配b[0]
     */
    private static void setServerPort(String str)
    {

        for (String item : str.split(SignConstant.SEMICOLON_STR))
        {
            String detail[] = item.split(SignConstant.COLON_STR);

            String type = detail[0].trim().toUpperCase();

            if (type.equals(MS))
            {
                String info[] = detail[1].trim().split(SignConstant.COMMA_STR);
                CedarDeployInfo.setMSservicePort(Integer.valueOf(info[0].trim()));
                CedarDeployInfo.setMSMySQLPort(Integer.valueOf(info[1].trim()));
            }
            else if (type.equals(LMS))
            {
                String info[] = detail[1].trim().split(SignConstant.COMMA_STR);
                CedarDeployInfo.setLMSservicePort(Integer.valueOf(info[0].trim()));
                CedarDeployInfo.setLMSMySQLPort(Integer.valueOf(info[1].trim()));
            }
            else if (type.equals(UPS))
            {
                String info[] = detail[1].trim().split(SignConstant.COMMA_STR);
                CedarDeployInfo.setUPSservicePort((Integer.valueOf(info[0].trim())));
                CedarDeployInfo.setUPSmergePort(Integer.valueOf(info[1].trim()));
            }
            else if (type.equals(CS))
            {
                CedarDeployInfo.setCSservicePort(Integer.valueOf(detail[1].trim()));
            }
            else if (type.equals(RS))
            {
                CedarDeployInfo.setRSservicePort(Integer.valueOf(detail[1].trim()));
            }
        }
    }

    /**
     * 新建server,插入serverlist
     * 
     * @param rsip
     * @param ip
     * @param type
     */
    private static void setServer(String rsip, String ip, String type)
    {
        MyServerInfo server = new MyServerInfo();
        server.init(rsip, ip, type);
        serverList.add(server);
        CedarDeployInfo.setServerList(serverList);
    }

    /**
     * 通过IP，用户名和密码获得对应的网卡名
     * 
     * @param IP
     * @param user
     * @param password
     * @return
     */
    private static String getNIC(String IP, String user, String password)
    {
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
            {
            }
            channel.disconnect();
        }
        return null;
    }

    
    
    /**
     * 单元测试用
     * 
     * @param args0
     */
    public static void main(String[] args0)
    {
        String filePath = "F:/compile.txt";
        CedarConfigInitializer.read(filePath, "GB2312");
        filePath = "F:/1RS_1UPS_3MS_3CS.txt";
        CedarConfigInitializer.read(filePath, "GB2312");
        System.out.println(CedarDeployInfo.getServerList().size());
        for (MyServerInfo server : CedarDeployInfo.getServerList())
        {
            System.out.println("RS IP: " + server.getRSIP() + "IP: " + server.getIP()
                    + "server type:" + server.getServerType());
        }
    }
}
