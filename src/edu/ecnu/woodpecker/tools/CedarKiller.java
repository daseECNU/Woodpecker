package edu.ecnu.woodpecker.tools;

import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ShellConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.CedarOperation;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.environment.CedarCompileInfo;
import edu.ecnu.woodpecker.environment.CedarConfigInitializer;
import edu.ecnu.woodpecker.environment.CedarDeployer;
import edu.ecnu.woodpecker.executor.keyword.IndexOfProcessor;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.util.Log;
import edu.ecnu.woodpecker.util.Util;

/**
 * The tool to kill CEDAR servers
 * 
 */
public class CedarKiller implements Initializable
{
    /**
     * servers' IP and SSH port
     */
    private String IP = null;
    private int connectionPort = 22;

    private String serverUserName = null;
    private String serverPassword = null;

    /**
     * Kill servers
     */
    private void killCEDAR()
    {
//        String cmd = "kill -9 `pgrep 'rootserver|chunkserver|mergeserver|updateserver' -u " + serverUserName + "`";
//        edu.ecnu.woodpecker.util.Util.exec(IP, serverUserName, serverPassword, connectionPort, cmd);
        for (String group : TestController.getGroupSet())
        {
            String deployConfigFile = TestController.getTestEnvironmentConfigPath() + group + FileConstant.CONFIG_FILE_SUFFIX;
            CedarConfigInitializer.read(deployConfigFile, FileConstant.UTF_8);
            CedarConfigInitializer.read(TestController.getTestEnvironmentConfigPath()+"CEDAR_compile.conf", FileConstant.UTF_8);
            String KillRunPidCmd=null;
            for(String executeIP:CedarConfigInitializer.getIPList())
            {
                KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                        CedarConfigInitializer.getDeployPath().substring(1, CedarConfigInitializer.getDeployPath().length()-1) + "/" + CedarDeployer.getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' rootserver.pid`";
                
                Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
                KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                        CedarConfigInitializer.getDeployPath().substring(1, CedarConfigInitializer.getDeployPath().length()-1) + "/" + CedarDeployer.getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' updateserver.pid`";
               
                Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
                KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                        CedarConfigInitializer.getDeployPath().substring(1, CedarConfigInitializer.getDeployPath().length()-1) + "/" + CedarDeployer.getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' chunkserver.pid`";
               
                Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
                KillRunPidCmd = ShellConstant.OPENDIR.replace("dirName",
                        CedarConfigInitializer.getDeployPath().substring(1, CedarConfigInitializer.getDeployPath().length()-1) + "/" + CedarDeployer.getCurrentPath(CedarCompileInfo.getMakePath()) + "/run;") + "kill -9 `awk '{print $1}' mergeserver.pid`";
                
                Util.exec(executeIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), CedarCompileInfo.getConnectionPort(), KillRunPidCmd);
                Recorder.FunctionRecord(Log.getRecordMetadata(), "kill PID in run file on " + executeIP + " successfully", LogLevelConstant.INFO);
            }
        }  
    }

    public void start() throws Exception
    {
        TestController.initializeParameter();
        //initialize(this, FileConstant.CEDAR_KILLER_CONFIG_PATH);
//        String[] IPs = IP.split(SignConstant.COMMA_STR);
//        for (String ele : IPs)
//            killCEDAR(ele.trim());
        killCEDAR();
        //System.out.println("Off line all CEDAR servers belong to " + serverUserName + " in IP: " + IP);
    }

    public void setIP(String IP)
    {
        this.IP = IP;
    }

    public void setServerUserName(String serverUserName)
    {
        this.serverUserName = serverUserName;
    }

    public void setServerPassword(String serverPassword)
    {
        this.serverPassword = serverPassword;
    }

    /**
     * Tool's entry
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            new CedarKiller().start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
