package edu.ecnu.woodpecker.tools;

import edu.ecnu.woodpecker.environment.CedarCompileInfo;
import edu.ecnu.woodpecker.util.Util;

public class AbnormalSimulation {

	/**
     * servers' IP and SSH port
     */
    private int connectionPort = 22;
    
	public void seizeCPU(String IP, int cores, int time)
	{
		String src = "/tools/AbnormalSimulation/seizeCPU.sh";
		String dst = "/home/"+CedarCompileInfo.getUserName()+"/seizeCPU.sh";
		Util.put(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, src, dst);
		
		String cmd = "sh " + dst + " " + cores + " " + time;
	    Util.exec(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, cmd);
	}
	public void seizeMEM(String IP, int size, int time)
	{
		String src = "/tools/AbnormalSimulation/seizeMEM";
		String dst = "/home/"+CedarCompileInfo.getUserName()+"/seizeMEM";
		Util.put(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, src, dst);
		
		String cmd = dst + " " + time + " " + size;
	    Util.exec(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, cmd);
	}
	public void seizeDISK(String IP, int IOPS, int size, int time)
	{
		String src = "/tools/AbnormalSimulation/seizeDISK.sh";
		String dst = "/home/"+CedarCompileInfo.getUserName()+"/seizeDISK.sh";
		Util.put(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, src, dst);
		
		String cmd = "sh " + dst + " " + IOPS +" " + size + " " + time;
	    Util.exec(IP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, cmd);
	}
	public void seizeNET(String transmitIP, int IOPS, String receiveIP, int size, int time)
	{
		String transmitsrc = "/tools/AbnormalSimulation/netClient.jar";
		String transmitdst = "/home/"+CedarCompileInfo.getUserName()+"/netClient.jar";
		Util.put(transmitIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, transmitsrc, transmitdst);
		
		String receivesrc = "~/tools/AbnormalSimulation/netServer.jar";
		String receivedst = "/home/"+CedarCompileInfo.getUserName()+"/netServer.jar";
		Util.put(receiveIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, receivesrc, receivedst);
		
		int port = 1234;
		String transmitcmd = "java -jar " + transmitdst + " " + receiveIP + " " + port + " " + IOPS + " " + size + " " + time;
		String receivecmd = "java -jar " + receivedst + " " + port + " " + (time + 5);
		Util.exec(receiveIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, transmitcmd);
	    Util.exec(transmitIP, CedarCompileInfo.getUserName(), CedarCompileInfo.getPassword(), connectionPort, receivecmd);
	}
}
