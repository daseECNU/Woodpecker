package edu.ecnu.woodpecker.tools;

import java.util.Optional;
import java.util.stream.Stream;

import edu.ecnu.woodpecker.constant.ConfigConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.TestController;
import edu.ecnu.woodpecker.util.Util;

/**
 * Launch reelect for CEDAR, and return the new Master information
 *
 */
public class CedarReelect implements Initializable
{
    private int connectionPort;
    private String serverUserName = null;
    private String serverPassword = null;

    /**
     * CEDAR根目录, RS查询状态所需端口
     */
    private String cedarDirectory = null;
    private String[] RSIP = null;
    private int RSQueryPort;

    public CedarReelect()
    {
        connectionPort = 22;
    }
    
    /**
     * Reelect master in CEDAR
     * 
     * @throws Exception
     */
    private void reelect() throws Exception
    {
        String master = getCurrentMaster().orElseThrow(Exception::new);
        // Reelect
        StringBuilder reelect = new StringBuilder(80);
        reelect.append(cedarDirectory).append("/bin/rs_admin -r ").append(master).append(" -p ").append(RSQueryPort).append(" reelect");
        String info = Util.exec(master, serverUserName, serverPassword, connectionPort, reelect.toString());
        Stream<String> stream = Stream.of(info.split(FileConstant.WIN_LINE_FEED_STR));
        if (!stream.anyMatch(ele -> ele.contains("Okay")))
        {
            System.out.println("Reelect fail");
            System.exit(1);
        }
        System.out.println("Reelect succeed");
        Thread.sleep(10_000);
        master = getCurrentMaster().orElseThrow(Exception::new);
        System.out.println("New master is: " + master);
    }

    /**
     * Get master IP after reelect
     */
    private Optional<String> getCurrentMaster()
    {
        String getRoleTemplate = cedarDirectory + "/bin/rs_admin -r rs_ip -p rs_port get_obi_role";
        getRoleTemplate = getRoleTemplate.replace("rs_port", String.valueOf(RSQueryPort));
        long startTime = System.currentTimeMillis();
        // Timeout is 20 second
        while (System.currentTimeMillis() - startTime < 20_000)
        {
            for (String IP : RSIP)
            {
                String getRole = getRoleTemplate.replace("rs_ip", IP);
                String info = Util.exec(IP, serverUserName, serverPassword, connectionPort, getRole);
                Stream<String> stream = Stream.of(info.split(FileConstant.WIN_LINE_FEED_STR));
                if (stream.anyMatch(ele -> ele.contains(ConfigConstant.MASTER_UPPER)))
                    return Optional.of(IP);
            }
        }
        return Optional.empty();
    }
    
    public void start(boolean getCurrentMaster) throws Exception
    {
        // Set proxy server if exists
        TestController.initializeParameter();
        initialize(this, FileConstant.CEDAR_REELECT_CONFIG_PATH);
        if (getCurrentMaster)
        {
            String master = getCurrentMaster().orElseThrow(Exception::new);
            System.out.println("New master is: " + master);
        }
        else
            reelect();
    }

    public void setServerUserName(String serverUserName)
    {
        this.serverUserName = serverUserName;
    }

    public void setServerPassword(String serverPassword)
    {
        this.serverPassword = serverPassword;
    }

    public void setCedarDirectory(String cedarDirectory)
    {
        this.cedarDirectory = cedarDirectory;
    }

    public void setRSIP(String RSIP)
    {
        this.RSIP = Util.removeBlankElement(RSIP.split(SignConstant.COMMA_STR));
    }

    public void setRSQueryPort(String RSQueryPort)
    {
        this.RSQueryPort = Integer.parseInt(RSQueryPort);
    }

    public static void main(String[] args)
    {
        try
        {
            new CedarReelect().start(false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
