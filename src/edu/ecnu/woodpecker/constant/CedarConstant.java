package edu.ecnu.woodpecker.constant;

public class CedarConstant
{
    public final static String MERGE = "./ups_admin -a ip -p port -t major_freeze";
    public final static String REELECT = "./rs_admin -r ip -p port reelect ";
    public final static String ISMERGEDOWN = "./rs_admin -r ip -p port stat -o merge";
    public final static String GETROLE = "./rs_admin -r ip -p port get_obi_role";
    public final static String TRUNCATE = "./ups_admin -a ip -p port -t minor_freeze";
    public final static String GATHER = "./rs_admin -r ip -p port gather_statistics";
    public final static String ISGATHERDOWN = "./rs_admin -r ip -p port stat -o gather";

    public final static String COMPILESRC = "cd srcpath; make clean; touch svn_dist_version; sh build.sh init;"
            + "./configure --prefix=makepath  --with-release=yes --with-test-case=no; "
            + "make -j  core -C src/ && echo compile_cedar_successful ||echo compile_cedar_unsuccessful; "
            + "cd src/; make install && echo compile_cedar_successful ||echo compile_cedar_unsuccessful;";
    public final static String COMPILETOOLS = "cd ..; make -j core -C tools/ && echo compile_cedar_successful ||echo compile_cedar_unsuccessful;"
            + " cd tools/; make install && echo compile_cedar_successful ||echo compile_cedar_unsuccessful";

    /**
     * server
     */
    public final static String ROOTSERVER = "RootServer";
    public final static String MERGESERVER = "MergeServer";
    public final static String LISTENERMERGESERVER = "ListenerMergeServer";
    public final static String CHUNKSERVER = "ChunkServer";
    public final static String UPDATESERVER = "UpdateServer";

    /**
     * 启动server
     */
    public final static String STARTRSONE = "./bin/rootserver -r ip:port -R ip:port -i NIC -s ip:port@1 -C 1;";
    public final static String STARTRSTHREE = "./bin/rootserver -r ip:port -R ip:port -i NIC -s iplist -C num;";
    public final static String STARTUPS = "./bin/updateserver -r ip:port -p servicePort -m mergePort -i NIC;";
    public final static String STARTCS = "./bin/chunkserver -r ip:port -p servicePort -n appName -i NIC;";
    public final static String STARTMS = "./bin/mergeserver -r ip:port -p servicePort -z MySQLPort -i NIC;";
    public final static String STARTLMS = "./bin/mergeserver -r ip:port -p servicePort -z MySQLPort -i NIC -t lms;";

    /**
     * 判断server是否启动成功
     */
    //public final static String PGREP = "pgrep server -u  user ;";
    public final static String PGREP = "ps ux|grep 'serverInformation'|grep -v grep |awk '{print $2}'";
    /**
     * 设主
     */
    public final static String SETMASTER = "./bin/rs_admin -r  ip -p port set_obi_master_first;./bin/rs_admin -r ip -p port -t 60000000 boot_strap;";

}
