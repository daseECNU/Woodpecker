package edu.ecnu.woodpecker.systemfunction;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.ecnu.woodpecker.constant.CedarConstant;
import edu.ecnu.woodpecker.constant.FileConstant;
import edu.ecnu.woodpecker.constant.LogLevelConstant;
import edu.ecnu.woodpecker.constant.ShellConstant;
import edu.ecnu.woodpecker.constant.SignConstant;
import edu.ecnu.woodpecker.controller.clusterinfo.ChunkServerInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.ClustersInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.MergeServerInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.RootServerInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.ServerInfo;
import edu.ecnu.woodpecker.controller.clusterinfo.UpdateServerInfo;
import edu.ecnu.woodpecker.log.Recorder;
import edu.ecnu.woodpecker.util.Log;
import edu.ecnu.woodpecker.util.Util;

public class CEDARServer
{

    /**
     * 
     * @param type
     * @param num
     * @return
     */
    public static String killServer(String type, int num)
    {
        CEDARCluster.update();
        String killServerList = "";
        if (type.equals("-master"))
        {
            // System.out.println("kill master server");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all servers of master",
                    LogLevelConstant.INFO);

            // 主集群所有server
            for (ServerInfo server : ClustersInfo.getAliveMasterInfo())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-slave_all"))
        {
            // System.out.println("kill all slave server");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all servers of all slaves",
                    LogLevelConstant.INFO);

            // 所有备集群所有server
            for (ServerInfo server : ClustersInfo.getAllAliveSlaveInfo())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-slave"))
        {
            // System.out.println("kill one slave server");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all servers of one slave",
                    LogLevelConstant.INFO);

            // 随机一个备集群所有server
            for (ServerInfo server : ClustersInfo.getAliveSlaveInfo())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-all"))
        {
            // System.out.println("kill all cluster server");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all servers of all clusters",
                    LogLevelConstant.INFO);

            // 所有集群所有server
            for (ServerInfo server : ClustersInfo.getAllAliveServerInfo())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-ups_master"))
        {
            // System.out.println("kill all ups on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill UPS on master",
                    LogLevelConstant.INFO);

            // 主集群UPS
            ServerInfo server = ClustersInfo.getAliveMasterUPS();
            if (killServer(server))
            {
                server.setPID(-1);
                server.setIsDown(true);
                killServerList += setKilledServer(server);
            }
        }
        else if (type.equals("-ups_slave"))
        {
            // System.out.println("kill one ups on slave ");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill one UPS of one slave ",
                    LogLevelConstant.INFO);

            // 备集群一个UPS
            ServerInfo server = ClustersInfo.getAliveSlaveUPS();
            if (killServer(server))
            {
                server.setPID(-1);
                server.setIsDown(true);
                killServerList += setKilledServer(server);
            }

        }
        else if (type.equals("-ups_slave_all"))
        {
            // System.out.println("kill all ups on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all UPSs of all slaves",
                    LogLevelConstant.INFO);

            // 备集群所有UPS
            for (ServerInfo server : ClustersInfo.getAliveSlaveUPSs())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-ups_all"))
        {
            // System.out.println("kill all ups");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all UPSs of all clusters",
                    LogLevelConstant.INFO);

            // 所有集群UPS
            for (ServerInfo server : ClustersInfo.getAllAliveServerInfo())
            {
                if (server.getClass().getName().endsWith("UpdateServerInfo"))
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                    }
                }
            }
        }
        else if (type.equals("-rs_master"))
        {
            // System.out.println("kill rs on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill RS of master",
                    LogLevelConstant.INFO);

            // 主集群RS
            ServerInfo server = ClustersInfo.getAliveMasterRS();

            if (killServer(server))
            {
                server.setPID(-1);
                server.setIsDown(true);
                killServerList += setKilledServer(server);
            }
        }
        else if (type.equals("-rs_slave"))
        {
            // System.out.println("kill one rs on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill one RS of one slave",
                    LogLevelConstant.INFO);

            // 备集群一个RS
            ServerInfo server = ClustersInfo.getAliveSlaveRS();

            if (killServer(server))
            {
                server.setPID(-1);
                server.setIsDown(true);
                killServerList += setKilledServer(server);
            }
        }
        else if (type.equals("-rs_slave_all"))
        {
            // System.out.println("kill all rs on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all RSs of all slaves",
                    LogLevelConstant.INFO);

            // 备集群所有RS
            for (ServerInfo server : ClustersInfo.getAliveSlaveRSs())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }

            }
        }
        else if (type.equals("-rs_all"))
        {
            // System.out.println("kill all rs");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all RSs of all clusters",
                    LogLevelConstant.INFO);

            // 所有集群RS
            for (ServerInfo server : ClustersInfo.getAllAliveServerInfo())
            {
                if (server.getClass().getName().endsWith("RootServerInfo"))
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                    }
                }
            }
        }
        else if (type.equals("-lms_master"))
        {
            // System.out.println("kill all lms on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all LMSs of master",
                    LogLevelConstant.INFO);

            // 主集群LMS
            for (ServerInfo server : ClustersInfo.getAliveMasterMSs())
            {
                if (((MergeServerInfo) server).isListener())
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                    }
                }
            }
        }
        else if (type.equals("-lms_slave"))
        {
            // System.out.println("kill one lms on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill one LMS of one slave",
                    LogLevelConstant.INFO);

            // 一个备集群一个LMS
            for (ServerInfo server : ClustersInfo.getAliveSlaveMSs())
            {// dddd

                if (((MergeServerInfo) server).isListener())
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                        break;
                    }
                }
            }

            // //一个备集群中随机n个MS
            // int msnum = 0;
            // int MSnum = 0;
            // boolean satisfaction = true;
            // List<ServerInfo> serverList = null;
            // List<ServerInfo> newserverList = null;
            // serverList =
            // ClustersInfo.getAliveSlaveInfo();
            // for(ServerInfo server : serverList)
            // {
            // if(server.getClass().getName().endsWith("MergeServerInfo"))
            // {
            // MSnum++;
            // }
            // }
            // if(MSnum == num || MSnum > num)
            // {
            // System.out.println("该备集群满足");
            // for(ServerInfo server : serverList)
            // {
            // if(server.getClass().getName().endsWith("MergeServerInfo")&&killServer(server))
            // {
            // server.setPID(-1);
            // server.setIsDown(true);
            // killServerList += setKilledServer(server);
            // msnum++;
            // }
            // if(num == msnum)
            // {
            // break;
            // }
            // }
            // satisfaction = false;
            // }
            // while(satisfaction)
            // {
            // msnum =0;
            // newserverList =
            // ClustersInfo.getAliveSlaveInfo();
            // if(serverList != newserverList)
            // {
            // System.out.println("该备集群不同");
            // serverList = newserverList;
            // for(ServerInfo server : serverList)
            // {
            // if(server.getClass().getName().endsWith("MergeServerInfo")&&killServer(server))
            // {
            // server.setPID(-1);
            // server.setIsDown(true);
            // killServerList += setKilledServer(server);
            // msnum++;
            // }
            // if(num == msnum)
            // {
            // break;
            // }
            // }
            // break;
            // }
            // }
            // if(msnum < num)
            // {
            // System.out.print("该集群中的备备集群中没有"+num+"个ms");
            // }
            //
        }
        else if (type.equals("-lms_slave_all"))
        {
            // System.out.println("kill all lms on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all LMSs of all slaves",
                    LogLevelConstant.INFO);

            // 备集群所有LMS
            for (ServerInfo server : ClustersInfo.getAliveSlaveMSs())
            {

                if (((MergeServerInfo) server).isListener())
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                    }
                }
            }
        }
        else if (type.equals("-lms_all"))
        {
            // System.out.println("kill all lms");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all LMSs of all clusters",
                    LogLevelConstant.INFO);

            // 所有集群LMS
            for (ServerInfo server : ClustersInfo.getAllAliveServerInfo())
            {
                if (server.getClass().getName().endsWith("MergeServerInfo")
                        && ((MergeServerInfo) server).isListener())
                {
                    if (killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                    }
                }
            }
        }
        else if (type.equals("-ms_master"))
        {
            // System.out.println("kill " + num +
            // " ms on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill " + num + " ms on master",
                    LogLevelConstant.INFO);

            // 主集群随机n个MS
            for (int i = 0; i < num; i++)
            {
                ServerInfo server = ClustersInfo.getAliveMasterMS();
                if (server == null)
                {
                    // System.out.print("该主集群中没有ms");
                    Recorder.FunctionRecord(Log.getRecordMetadata(), "there are no " + num
                            + " ms on master", LogLevelConstant.ERROR);
                    break;
                }
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-ms_master_all"))
        {
            // System.out.println("kill all ms on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all ms on master",
                    LogLevelConstant.INFO);

            // 主集群所有MS
            for (ServerInfo server : ClustersInfo.getAliveMasterMSs())
            {
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-ms_slave_all"))
        {
            // System.out.println("kill " + num +
            // " ms on slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill " + num + " ms on all slave",
                    LogLevelConstant.INFO);

            // 所有备集群中随机n个MS
            for (int i = 0; i < num; i++)
            {
                ServerInfo server = null;
                if (ClustersInfo.getAliveSlaveMSs() != null)
                {
                    server = ClustersInfo.getAliveSlaveMSs().get(0);
                }
                else
                {
                    // System.out.print("所有备集群中没有" + num +
                    // "个ms");
                    // System.out.println("所有备集群中没有" + num +
                    // "个ms");
                    Recorder.FunctionRecord(Log.getRecordMetadata(), "all slave have on " + num
                            + " ms", LogLevelConstant.INFO);
                    break;
                }
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-ms_slave"))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill " + num + " ms on one slave",
                    LogLevelConstant.INFO);
            // 一个备集群中随机n个MS
            int msnum = 0;
            int MSnum = 0;
            boolean satisfaction = true;
            List<ServerInfo> serverList = null;
            List<ServerInfo> newserverList = null;
            serverList = ClustersInfo.getAliveSlaveInfo();
            for (ServerInfo server : serverList)
            {
                if (server.getClass().getName().endsWith("MergeServerInfo"))
                {
                    MSnum++;
                }
            }
            if (MSnum == num || MSnum > num)
            {
                // System.out.println("该备集群满足");
                for (ServerInfo server : serverList)
                {
                    if (server.getClass().getName().endsWith("MergeServerInfo")
                            && killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                        msnum++;
                    }
                    if (num == msnum)
                    {
                        break;
                    }
                }
                // System.out.println("kill" + num +
                // "个ms on slave");

                satisfaction = false;
            }
            while (satisfaction)
            {
                msnum = 0;
                newserverList = ClustersInfo.getAliveSlaveInfo();
                if (serverList != newserverList)
                {
                    // System.out.println("该备集群不同");
                    serverList = newserverList;
                    for (ServerInfo server : serverList)
                    {
                        if (server.getClass().getName().endsWith("MergeServerInfo")
                                && killServer(server))
                        {
                            server.setPID(-1);
                            server.setIsDown(true);
                            killServerList += setKilledServer(server);
                            msnum++;
                        }
                        if (num == msnum)
                        {
                            break;
                        }
                    }
                    break;
                }
            }
            if (msnum < num)
            {
                Recorder.FunctionRecord(Log.getRecordMetadata(), "this slave have on " + num
                        + " ms", LogLevelConstant.ERROR);

            }
        }
        else if (type.equals("-cs_master"))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill  " + num + " cs on master",
                    LogLevelConstant.INFO);
            // 主集群随机n个CS
            for (int i = 0; i < num; i++)
            {
                ServerInfo server = ClustersInfo.getAliveMasterCS();
                if (server == null)
                {
                    // System.out.print("该主集群中没有" + num +
                    // "个cs");
                    // System.out.println("所有备集群中没有" + num +
                    // "个cs");
                    Recorder.FunctionRecord(Log.getRecordMetadata(),
                            "master has no " + num + " cs", LogLevelConstant.ERROR);
                    break;
                }
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }

        }
        else if (type.equals("-cs_master_all"))
        {
            // System.out.println("kill all cs on master");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill all cs on master",
                    LogLevelConstant.INFO);

            // 主集群所有CS
            for (ServerInfo server : ClustersInfo.getAliveMasterCSs())
            {

                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        else if (type.equals("-cs_slave"))
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill " + num + " cs on one slave",
                    LogLevelConstant.INFO);
            // 一个备集群中随机n个CS
            int csnum = 0;
            int CSnum = 0;
            boolean satisfaction = true;
            List<ServerInfo> serverList = null;
            List<ServerInfo> newserverList = null;
            serverList = ClustersInfo.getAliveSlaveInfo();
            for (ServerInfo server : serverList)
            {
                if (server.getClass().getName().endsWith("ChunkServerInfo"))
                {
                    CSnum++;
                }
            }
            if (CSnum == num || CSnum > num)
            {
                // System.out.println("该备集群满足");
                for (ServerInfo server : serverList)
                {
                    if (server.getClass().getName().endsWith("ChunkServerInfo")
                            && killServer(server))
                    {
                        server.setPID(-1);
                        server.setIsDown(true);
                        killServerList += setKilledServer(server);
                        csnum++;
                    }
                    if (num == csnum)
                    {
                        break;
                    }
                }
                // System.out.println("kill" + num +
                // "个cs on slave");
                satisfaction = false;
            }
            while (satisfaction)
            {
                csnum = 0;
                newserverList = ClustersInfo.getAliveSlaveInfo();
                if (serverList != newserverList)
                {
                    // System.out.println("该备集群不同");
                    serverList = newserverList;
                    for (ServerInfo server : serverList)
                    {
                        if (server.getClass().getName().endsWith("ChunkServerInfo")
                                && killServer(server))
                        {
                            server.setPID(-1);
                            server.setIsDown(true);
                            killServerList += setKilledServer(server);
                            csnum++;
                        }
                        if (num == csnum)
                        {
                            break;
                        }
                    }
                    break;
                }
            }

            if (csnum < num)
            {
                // System.out.println("所有备集群中没有" + num +
                // "个cs");
                Recorder.FunctionRecord(Log.getRecordMetadata(), "this slave have on " + num
                        + " cs", LogLevelConstant.ERROR);

            }
        }
        else if (type.equals("-cs_slave_all"))
        {
            // System.out.println("kill " + num +
            // " cs on all slave");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "kill " + num + "  cs on all slave",
                    LogLevelConstant.INFO);
            // 所有备集群中随机n个CS
            for (int i = 0; i < num; i++)
            {
                ServerInfo server = null;
                if (ClustersInfo.getAliveSlaveCSs() != null)
                {
                    server = ClustersInfo.getAliveSlaveCSs().get(0);
                }
                else
                {
                    // System.out.println("all slave have no "
                    // + num + " cs ");
                    Recorder.FunctionRecord(Log.getRecordMetadata(), "all slave have no " + num
                            + "  cs ", LogLevelConstant.ERROR);
                    break;
                }
                if (killServer(server))
                {
                    server.setPID(-1);
                    server.setIsDown(true);
                    killServerList += setKilledServer(server);
                }
            }
        }
        // ClustersInfo.systemaa();
        return killServerList;
    }

    /**
     * 根据arg启某个被杀server
     * 
     * @param arg 是kill_server返回的字符串信息，存储有被杀server的信息。
     * @return 启动是否成功
     */
    public static boolean startServer(String arg)
    {
        if (arg == "")
        {
            Recorder.FunctionRecord(Log.getRecordMetadata(), "the input is null",
                    LogLevelConstant.ERROR);
            return false;
        }
        ServerInfo server = null;
        String type = null;
        for (String item : arg.split(SignConstant.SEMICOLON_STR))
        {
            String detail[] = item.split(SignConstant.COMMA_STR);
            type = detail[0];
            String command = null;
            String command1 = null;
            if (type.equals("RootServerInfo"))
            {
                server = new RootServerInfo();
                int num = Integer.valueOf(detail[8]);
                int role = Integer.valueOf(detail[6]) + 1;
                StringBuilder startRS = new StringBuilder(CedarConstant.STARTRSTHREE);
                int index = startRS.indexOf("ip");
                startRS.replace(index, index + 2, detail[1]);
                index = startRS.indexOf("port");
                startRS.replace(index, index + 4, detail[2]);
                index = startRS.indexOf("ip");
                startRS.replace(index, index + 2, detail[3]);
                index = startRS.indexOf("port");
                startRS.replace(index, index + 4, detail[7]);
                index = startRS.indexOf("iplist");
                index = startRS.indexOf("NIC");
                startRS.replace(index, index + 3, detail[4]);
                index = startRS.indexOf("num");
                startRS.replace(index, index + 3, String.valueOf(role));
                String iplistx = null;
                if (num != 1)
                {
                    iplistx = detail[9] + ":" + detail[7] + "@1#" + detail[10] + ":" + detail[7]
                            + "@2#" + detail[11] + ":" + detail[7] + "@3";

                }
                else
                {
                    iplistx = detail[9] + ":" + detail[7] + "@1 ";
                }
                startRS.replace(index, index + 6, iplistx);
                command = ShellConstant.OPENDIR.replace("dirName", detail[5] + ";") + startRS;
                ((RootServerInfo) server).setPort(Integer.valueOf(detail[2]));
//                StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//                index = pgrep.indexOf("server");
//                pgrep.replace(index, index + 6, "rootserver");
//                index = pgrep.indexOf("user");
//                pgrep.replace(index, index + 4, ClustersInfo.getUserName());
//                command1 = String.valueOf(pgrep);
                String pgrep = new String(CedarConstant.PGREP);
                command1 = pgrep.replace("serverInformation", String.valueOf(startRS).substring(0, String.valueOf(startRS).length()-1));
            }
            else if (type.equals("UpdateServerInfo"))
            {
                server = new UpdateServerInfo();
                StringBuilder startUPS = new StringBuilder(CedarConstant.STARTUPS);
                int index = startUPS.indexOf("ip");
                startUPS.replace(index, index + 2, detail[1]);
                index = startUPS.indexOf("port");
                startUPS.replace(index, index + 4, detail[2]);
                index = startUPS.indexOf("servicePort");
                startUPS.replace(index, index + 11, detail[6]);
                index = startUPS.indexOf("mergePort");
                startUPS.replace(index, index + 9, detail[7]);
                index = startUPS.indexOf("NIC");
                startUPS.replace(index, index + 3, detail[4]);
                command = ShellConstant.OPENDIR.replace("dirName", detail[5] + ";") + startUPS;

                ((UpdateServerInfo) server).setServicePort(Integer.valueOf(detail[6]));
                ((UpdateServerInfo) server).setMergePort(Integer.valueOf(detail[7]));
//                StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//                index = pgrep.indexOf("server");
//                pgrep.replace(index, index + 6, "updateserver");
//                index = pgrep.indexOf("user");
//                pgrep.replace(index, index + 4, ClustersInfo.getUserName());
//                command1 = String.valueOf(pgrep);
                String pgrep = new String(CedarConstant.PGREP);
                command1 = pgrep.replace("serverInformation", String.valueOf(startUPS).substring(0, String.valueOf(startUPS).length()-1));
            }
            else if (type.equals("MergeServerInfo"))
            {
                server = new MergeServerInfo();
                StringBuilder startMS = new StringBuilder(CedarConstant.STARTMS);
                int index = startMS.indexOf("ip");
                startMS.replace(index, index + 2, detail[1]);
                index = startMS.indexOf("port");
                startMS.replace(index, index + 4, detail[2]);
                index = startMS.indexOf("servicePort");
                startMS.replace(index, index + 11, detail[6]);
                index = startMS.indexOf("MySQLPort");
                startMS.replace(index, index + 9, detail[7]);
                index = startMS.indexOf("NIC");
                startMS.replace(index, index + 3, detail[4]);
                command = ShellConstant.OPENDIR.replace("dirName", detail[5] + ";") + startMS;

                ((MergeServerInfo) server).setListener(false);
                ((MergeServerInfo) server).setServicePort(Integer.valueOf(detail[6]));
                ((MergeServerInfo) server).setMySQLPort(Integer.valueOf(detail[7]));

                if (Boolean.valueOf(detail[8]))
                {
                    command += " -t lms";
                    ((MergeServerInfo) server).setListener(true);
                }
//                StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//                index = pgrep.indexOf("server");
//                pgrep.replace(index, index + 6, "mergeserver");
//                index = pgrep.indexOf("user");
//                pgrep.replace(index, index + 4, ClustersInfo.getUserName());
//                command1 = String.valueOf(pgrep);
                String pgrep = new String(CedarConstant.PGREP);
                command1 = pgrep.replace("serverInformation", String.valueOf(startMS).substring(0, String.valueOf(startMS).length()-1));
            }
            else if (type.equals("ChunkServerInfo"))
            {
                server = new ChunkServerInfo();
                StringBuilder startCS = new StringBuilder(CedarConstant.STARTCS);
                int index = startCS.indexOf("ip");
                startCS.replace(index, index + 2, detail[1]);
                index = startCS.indexOf("port");
                startCS.replace(index, index + 4, detail[2]);
                index = startCS.indexOf("servicePort");
                startCS.replace(index, index + 11, detail[7]);
                index = startCS.indexOf("appName");
                startCS.replace(index, index + 7, detail[6]);
                index = startCS.indexOf("NIC");
                startCS.replace(index, index + 3, detail[4]);
                command = ShellConstant.OPENDIR.replace("dirName", detail[5] + ";") + startCS;

                ((ChunkServerInfo) server).setAppName(detail[6]);
                ((ChunkServerInfo) server).setServicePort(Integer.valueOf(detail[7]));
//                StringBuilder pgrep = new StringBuilder(CedarConstant.PGREP);
//                index = pgrep.indexOf("server");
//                pgrep.replace(index, index + 6, "chunkserver");
//                index = pgrep.indexOf("user");
//                pgrep.replace(index, index + 4, ClustersInfo.getUserName());
//                command1 = String.valueOf(pgrep);
                String pgrep = new String(CedarConstant.PGREP);
                command1 = pgrep.replace("serverInformation", String.valueOf(startCS).substring(0, String.valueOf(startCS).length()-1));
            }

            // System.out.println("----command:"+command+"---host"+b[3]+ClustersInfo.getUserName()+ClustersInfo.getPassword()+
            // ClustersInfo.getConnectionPort());
            Util.exec(detail[3], ClustersInfo.getUserName(), ClustersInfo.getPassword(),
                    ClustersInfo.getConnectionPort(), command);
            server.setCurClusterRSIP(detail[1]);
            server.setCurClusterRSPort(Integer.valueOf(detail[2]));
            server.setIP(detail[3]);
            server.setNIC(detail[4]);
            if (isServerStarted(server, ClustersInfo.getUserName(), ClustersInfo.getPassword(),
                    ClustersInfo.getConnectionPort(), command1))
            {
                ClustersInfo.update(server);

                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                Recorder.FunctionRecord(Log.getRecordMetadata(),
                        "start " + type + " on " + server.getIP() + " unsuccessfully",
                        LogLevelConstant.ERROR);

                return false;
            }
        }
        Recorder.FunctionRecord(Log.getRecordMetadata(), "start " + type + " on " + server.getIP()
                + " successfully", LogLevelConstant.INFO);

        return true;
    }

    /**
     * 在指定ip上添加一个server，server类型由servertype决定
     * 
     * @param ip
     * @param servertype
     * @param NIC
     */
    public static void addServer(String ip, String servertype, String NIC)
    {
        CEDARCluster.update();
        String serverInfo = null;
        if (servertype.equals("-ms_master"))
        {
            serverInfo = "MergeServerInfo" + "," + ClustersInfo.getMasterRS().getCurClusterRSIP()
                    + "," + ClustersInfo.getMasterRS().getCurClusterRSPort() + "," + ip + "," + NIC
                    + "," + ClustersInfo.getDeployPath() + ","
                    + ClustersInfo.getMasterMS().getServicePort() + ","
                    + ClustersInfo.getMasterMS().getMySQLPort() + "," + "false" + "," + "master"
                    + ";";
        }
        else if (servertype.equals("-cs_master"))
        {
            serverInfo = "ChunkServerInfo" + "," + ClustersInfo.getMasterRS().getCurClusterRSIP()
                    + "," + ClustersInfo.getMasterRS().getCurClusterRSPort() + "," + ip + "," + NIC
                    + "," + ClustersInfo.getDeployPath() + ","
                    + ClustersInfo.getMasterCS().getAppName() + ","
                    + ClustersInfo.getMasterCS().getServicePort() + "," + "master" + ";";
        }
        else if (servertype.equals("-ms_slave"))
        {
            serverInfo = "MergeServerInfo" + "," + ClustersInfo.getSlaveRS().getCurClusterRSIP()
                    + "," + ClustersInfo.getSlaveRS().getCurClusterRSPort() + "," + ip + "," + NIC
                    + "," + ClustersInfo.getDeployPath() + ","
                    + ClustersInfo.getMasterMS().getServicePort() + ","
                    + ClustersInfo.getMasterMS().getMySQLPort() + "," + "false" + "," + "slave"
                    + ";";
        }
        else if (servertype.equals("-cs_slave"))
        {
            serverInfo = "ChunkServerInfo" + "," + ClustersInfo.getSlaveRS().getCurClusterRSIP()
                    + "," + ClustersInfo.getSlaveRS().getCurClusterRSPort() + "," + ip + "," + NIC
                    + "," + ClustersInfo.getDeployPath() + ","
                    + ClustersInfo.getMasterCS().getAppName() + ","
                    + ClustersInfo.getMasterCS().getServicePort() + "," + "slave" + ";";
        }
        // System.out.println("add-server"+serverInfo);
        startServer(serverInfo);
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
    private static boolean isServerStarted(ServerInfo server, String user, String psw, int port,
            String command)
    {
        String result = // "111";
        Util.exec(server.getIP(), user, psw, port, command);
        if (result.equals(""))
        {
            return false;
        }
        server.setPID(Integer.valueOf(result.replace(FileConstant.LINUX_LINE_FEED, "")));
        server.setIsDown(false);
        return true;
    }

    private static String setKilledServer(ServerInfo server)
    {
        String serverInfo = null;
        String type = server.getClass().getName();
        String aa[] = type.split("\\.");
        type = aa[aa.length - 1];
        // System.out.println("----type:"+type);
        serverInfo = type + "," + server.getCurClusterRSIP() + "," + server.getCurClusterRSPort()
                + "," + server.getIP() + "," + server.getNIC() + "," + ClustersInfo.getDeployPath();

        if (type.equals("RootServerInfo"))
        {
            serverInfo += "," + ((RootServerInfo) server).getClusterID() + ","
                    + ((RootServerInfo) server).getPort();
            Map<Integer, String> rsList = ClustersInfo.getRSList();
            serverInfo += "," + rsList.size();
            // int num=0,count=0;
            for (Iterator<Integer> iter = rsList.keySet().iterator(); iter.hasNext();)
            {
                int key = iter.next();
                serverInfo += "," + rsList.get(key);
                // num++;
                // if(server.getCurClusterRSIP().equals(rsList.get(key)))
                // {
                // count = num;
                // }
            }
            serverInfo += ";";
        }
        else if (type.equals("UpdateServerInfo"))
        {
            serverInfo += "," + ((UpdateServerInfo) server).getServicePort() + ","
                    + ((UpdateServerInfo) server).getMergePort() + ";";
        }
        else if (type.equals("MergeServerInfo"))
        {
            serverInfo += "," + ((MergeServerInfo) server).getServicePort() + ","
                    + ((MergeServerInfo) server).getMySQLPort() + ","
                    + ((MergeServerInfo) server).isListener() + ";";

        }
        else if (type.equals("ChunkServerInfo"))
        {
            serverInfo += "," + ((ChunkServerInfo) server).getAppName() + ","
                    + ((ChunkServerInfo) server).getServicePort() + ";";
        }
        // System.out.println("killserver信息：" + serverInfo);
        return serverInfo;
    }

    private static boolean killServer(ServerInfo server)
    {
        if (server == null)
        {
            // System.out.println("server为空");
            Recorder.FunctionRecord(Log.getRecordMetadata(), "the input is null",
                    LogLevelConstant.ERROR);
            return false;
        }
        String pid = String.valueOf(server.getPID());
        String ip = server.getIP();
        // String serverType = server.getClass().getName();
        String command = "kill -9 " + pid;

        Util.exec(ip, ClustersInfo.getUserName(), ClustersInfo.getPassword(),
                ClustersInfo.getConnectionPort(), command);
        // System.out.println("killserver on" + ip + " " +
        // command + " successfully");
        Recorder.FunctionRecord(Log.getRecordMetadata(), "kill server on " + ip + " " + command
                + " successfully", LogLevelConstant.INFO);

        return true;
    }
}
