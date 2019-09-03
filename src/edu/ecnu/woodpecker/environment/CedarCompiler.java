package edu.ecnu.woodpecker.environment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import edu.ecnu.woodpecker.constant.CedarConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ShellConstant;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.util.Log;
import edu.ecnu.woodpecker.util.Util;

public class CedarCompiler
{

    /**
     * 编译源码
     * 
     * @return 编译是否成功
     */
    public static boolean compileCEDAR()
    {

        String host = CedarCompileInfo.getSrcIP();
        String makeip = CedarCompileInfo.getMakeIP();
        String user = CedarCompileInfo.getUserName();
        String psw = CedarCompileInfo.getPassword();
        int port = CedarCompileInfo.getConnectionPort();
        String srcpath = CedarCompileInfo.getSrcPath();
        String makepath = CedarCompileInfo.getMakePath();
        String core = String.valueOf(CedarCompileInfo.getCore());

        StringBuilder command = new StringBuilder(CedarConstant.COMPILESRC);
        int index = command.indexOf("srcpath");
        command = command.replace(index, index + 7, srcpath);
        index = command.indexOf("makepath");
        command = command.replace(index, index + 8, makepath);
        index = command.indexOf("core");
        command = command.replace(index, index + 4, core);

        if (CedarCompileInfo.isCompileTools())
        {
            command.append(CedarConstant.COMPILETOOLS);
            index = command.indexOf("core");
            command = command.replace(index, index + 4, core);
        }
        // System.out.println(command);
        String result = Util.exec(host, user, psw, port, command.toString());
        int count = 0;
        for (String item : result.split(FileConstant.LINUX_LINE_FEED))
        {
            if (item.equals("compile_cedar_successful"))
            {
                count++;
            }
        }

        if (((CedarCompileInfo.isCompileTools() && count == 4) || (!CedarCompileInfo.isCompileTools() && count == 2))
                && ((makeip.equals(host) || (!makeip.equals(host) && isCompileFolderExist()))))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "Compile CEDAR successfully", LogLevelConstant.INFO);
            return true;
        }
        Recorder.FunctionRecord(Log.getRecordMetadata(), "Compile CEDAR unsuccessfully", LogLevelConstant.ERROR);
        return false;
    }

    public static String getPassword()
    {
        return CedarCompileInfo.getPassword();
    }

    public static String getUserName()
    {
        return CedarCompileInfo.getUserName();
    }

    public static int getConnectionPort()
    {
        return CedarCompileInfo.getConnectionPort();
    }

    private static boolean isCompileFolderExist()
    {
        String user = CedarCompileInfo.getUserName();
        String psw = CedarCompileInfo.getPassword();
        int connectionPort = CedarCompileInfo.getConnectionPort();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");// 设置日期格式
        // new Date()为获取当前系统时间
        String time = df.format(new Date());
        // System.out.println(time);

        // 判断编译ip与源码所在ip是否相同，不同则传输编译好的CERAR文件

        boolean isExist = false;
        if (!CedarCompileInfo.getSrcIP().equals(CedarCompileInfo.getMakeIP()))
        {

            String command = ShellConstant.DELETE.replace("path", CedarCompileInfo.getMakePath())
                    + ShellConstant.MKDIR.replace("dirName", getParentPath(CedarCompileInfo.getMakePath()))
                    + ShellConstant.OPENDIR.replace("dirName", getParentPath(CedarCompileInfo.getMakePath()) + ";")
                    + ShellConstant.MKDIR.replace("dirName", time);
            System.out.println("--------------" + command);
            Util.exec(CedarCompileInfo.getMakeIP(), user, psw, connectionPort, command);

            StringBuilder scp = new StringBuilder(ShellConstant.SCP);
            int index = scp.indexOf("user");
            scp.replace(index, index + 4, user);
            index = scp.indexOf("ip");
            scp.replace(index, index + 2, CedarCompileInfo.getSrcIP());
            index = scp.indexOf("path");
            scp.replace(index, index + 4, CedarCompileInfo.getMakePath());

            // 将编译好的CEDAR文件从源码所在ip复制到编译ip上
            command = ShellConstant.OPENDIR.replace("dirName", getParentPath(CedarCompileInfo.getMakePath()) + ";") + scp
                    + ShellConstant.DELETE.replace("path", time);

            System.out.println("isCompileFolderExist--command:" + command);
            Util.scp(CedarCompileInfo.getMakeIP(), connectionPort, user, psw, command, CedarCompileInfo.getSrcIP());

            Calendar nowTime = Calendar.getInstance();
            nowTime.add(Calendar.MINUTE, 3);
            // 判断复制是否完成
            while (true)
            {
                command = ShellConstant.OPENDIR.replace("dirName", getParentPath(CedarCompileInfo.getMakePath()) + ";") + ShellConstant.LS;
                String result = Util.exec(CedarCompileInfo.getMakeIP(), user, psw, connectionPort, command);

                int count = 0;
                for (String item : result.split(FileConstant.LINUX_LINE_FEED))
                {
                    if (item.equals(time))
                        count++;
                }
                if (count == 0)
                    break;
                if (Calendar.getInstance().after(nowTime))
                {
                    isExist = true;
                    break;
                }
            }
            // 删除源码所在ip地址下的编译好的CEDAR
            command = ShellConstant.DELETE.replace("path", CedarCompileInfo.getMakePath());
            System.out.println("remote" + command);
            Util.exec(CedarCompileInfo.getSrcIP(), user, psw, connectionPort, command);
        }
        if (isExist)
        {
            return false;
        }
        return true;
    }

    /**
     * 根据传入目录获得上级目录
     * 
     * @param path
     * @return
     */
    private static String getParentPath(String path)
    {
        String ss[] = path.split("/");
        String aa = "";
        for (int i = 0; i < ss.length - 1; i++)
        {
            aa += ss[i] + "/";
        }
        // System.out.println("parent:---"+aa);
        return aa;
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
        // isCompileCEDAR();
        isCompileFolderExist();
    }

}
